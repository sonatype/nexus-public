/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.pypi.internal.orient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.io.Cooperation;
import org.sonatype.nexus.common.io.CooperationFactory;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.cache.CacheInfo.invalidateAsset;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.INDEX;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.INDEX_PATH_PREFIX;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.findAsset;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.toContent;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

/**
 * PyPi specific implementation of {@link GroupFacetImpl} allowing for {@link Cooperation}, merging and caching
 *
 * @since 3.15
 */
@Named
@Facet.Exposed
public class OrientPyPiGroupFacet
    extends GroupFacetImpl
{
  @Nullable
  private CooperationFactory.Builder cooperationBuilder;

  @Nullable
  private Cooperation indexRootCooperation;

  @Inject
  public OrientPyPiGroupFacet(final RepositoryManager repositoryManager,
                        final ConstraintViolationFactory constraintViolationFactory,
                        @Named(GroupType.NAME) final Type groupType)
  {
    super(repositoryManager, constraintViolationFactory, groupType);
  }

  @Inject
  protected void configureCooperation(
      final CooperationFactory cooperationFactory,
      @Named("${nexus.pypi.indexRoot.cooperation.enabled:-true}") final boolean cooperationEnabled,
      @Named("${nexus.pypi.indexRoot.cooperation.majorTimeout:-0s}") final Time majorTimeout,
      @Named("${nexus.pypi.indexRoot.cooperation.minorTimeout:-30s}") final Time minorTimeout,
      @Named("${nexus.pypi.indexRoot.cooperation.threadsPerKey:-100}") final int threadsPerKey)
  {
    if (cooperationEnabled) {
      this.cooperationBuilder = cooperationFactory.configure()
          .majorTimeout(majorTimeout)
          .minorTimeout(minorTimeout)
          .threadsPerKey(threadsPerKey);
    }
  }

  @VisibleForTesting
  void buildCooperation() {
    if (nonNull(cooperationBuilder)) {
      this.indexRootCooperation = cooperationBuilder.build(getRepository().getName() + ":indexRoot");
    }
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    buildCooperation();
  }

  /**
   * Build the PyPi Index Root merging all the given responses into one. This method allows {@link Cooperation} to
   * work, meaning that multiple requests to the same group request path will join in returning the same result.
   */
  public Content buildIndexRoot(final String name, final AssetKind assetKind, final Supplier<String> lazyMergeResult)
      throws IOException
  {
    if (isNull(indexRootCooperation)) {
      return buildMergedIndexRoot(name, lazyMergeResult, true);
    }

    try {
      return indexRootCooperation.cooperate(name, failover -> {

        if (failover) {
          // re-check cache when failing over to new thread
          Content latestContent = indexRootCooperation.join(() -> getFromCache(name, assetKind));
          if (nonNull(latestContent)) {
            return latestContent;
          }
        }

        return buildMergedIndexRoot(name, lazyMergeResult, true);
      });
    }
    catch (IOException e) {
      log.error("Unable to use Cooperation to merge {} for repository {}",
          name, getRepository().getName(), e);
    }

    return buildMergedIndexRoot(name, lazyMergeResult, false); // last resort, merge but don't cache
  }

  protected Content buildMergedIndexRoot(final String name, final Supplier<String> lazyMergeResult, boolean save)
      throws IOException
  {
    try {
      String html = lazyMergeResult.get();
      Content newContent = new Content(new StringPayload(html, ContentTypes.TEXT_HTML));
      return save ? saveToCache(name, newContent) : newContent;
    }
    catch (UncheckedIOException e) { // NOSONAR: unchecked wrapper, we're only interested in its cause
      throw e.getCause();
    }
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent event) {
    maybeInvalidateCache(event);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent event) {
    maybeInvalidateCache(event);
  }

  private void maybeInvalidateCache(final AssetEvent event) {
    if (event.isLocal() &&
        member(event.getRepositoryName()) &&
        ROOT_INDEX.name().equals(event.getAsset().formatAttributes().get(P_ASSET_KIND, String.class))) {
      invalidateCache(INDEX_PATH_PREFIX);
    }
  }

  private void invalidateCache(final String name) {
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      doInvalidateCache(name);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Transactional(retryOn = ONeedRetryException.class, swallow = ORecordNotFoundException.class)
  protected void doInvalidateCache(final String name) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = tx.findAssetWithProperty(P_NAME, name, tx.findBucket(getRepository()));
    if (asset != null && invalidateAsset(asset)) {
      log.info("Invalidating cached content {} from {}", name, getRepository().getName());
      tx.saveAsset(asset);
    }
  }

  @TransactionalTouchBlob
  public Content getFromCache(final String name, final AssetKind assetKind) {
    checkArgument(INDEX.equals(assetKind) || ROOT_INDEX.equals(assetKind), "Only index files are cached");

    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());

    Asset asset = findAsset(tx, bucket, name);
    if (asset == null) {
      return null;
    }

    return toContent(asset, tx.requireBlob(asset.blobRef()));
  }

  /**
   * Determines if the {@link Content} is stale, initially by assessing if there is
   * no lastModified time stamp associated with the content retrieved from cache then
   * checking if the {@link org.sonatype.nexus.repository.cache.CacheController} has been cleared.
   * Lastly, it should iterate over the members to see if any of the data has been modified since caching.
   * @param name of the cached asset
   * @param content of the asset
   * @param responses from all members
   * @return {@code true} if the content is considered stale.
   */
  public boolean isStale(final String name, final Content content, final Map<Repository, Response> responses) {
    DateTime cacheModified = extractLastModified(content);
    if (cacheModified == null || isStale(content)) {
      return true;
    }

    for (Entry<Repository, Response> response : responses.entrySet()) {
      Response responseValue = response.getValue();
      if (responseValue.getStatus().getCode() == HttpStatus.OK) {
        Content responseContent = (Content) responseValue.getPayload();

        if (responseContent != null) {
          DateTime memberLastModified = responseContent.getAttributes().get(CONTENT_LAST_MODIFIED, DateTime.class);
          if (memberLastModified == null || memberLastModified.isAfter(cacheModified)) {
            log.debug("Found stale content while fetching {} from repository {}", name, response.getKey().getName());
            return true;
          }
        }
      }
    }
    return false;
  }

  @TransactionalStoreBlob
  public Content saveToCache(final String name, final Content content) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = getAsset(tx, name);
    AttributesMap contentAttributes = Content.maintainLastModified(asset, null);
    contentAttributes.set(CacheInfo.class, cacheController.current());
    Content.applyToAsset(asset, contentAttributes);

    AssetBlob blob = updateAsset(tx, asset, content);

    Content response = new Content(new BlobPayload(blob.getBlob(), ContentTypes.TEXT_HTML));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, response.getAttributes());

    return response;
  }

  private DateTime extractLastModified(final Content content) {
    DateTime lastModified;
    if (content != null && content.getAttributes().contains(CONTENT_LAST_MODIFIED)) {
      lastModified = content.getAttributes().get(CONTENT_LAST_MODIFIED, DateTime.class);
    } else {
      lastModified = null;
    }
    return lastModified;
  }

  @TransactionalStoreBlob
  protected AssetBlob updateAsset(final StorageTx tx, final Asset asset, final Content content) throws IOException {
    AttributesMap contentAttributes = Content.maintainLastModified(asset, content.getAttributes());
    Content.applyToAsset(asset, contentAttributes);

    InputStream inputStream = content.openInputStream();
    AssetBlob blob = tx.setBlob(asset,
        asset.name(),
        () -> inputStream,
        HASH_ALGORITHMS,
        null,
        ContentTypes.TEXT_HTML,
        true);

    tx.saveAsset(asset);

    return blob;
  }

  @TransactionalTouchBlob
  protected Asset getAsset(final StorageTx tx, final String name) {
    final Bucket bucket = tx.findBucket(getRepository());
    Asset assets = findAsset(tx, bucket, name);
    if (assets == null) {
      assets = tx.createAsset(bucket, getRepository().getFormat()).name(name);
      assets.formatAttributes().set(P_ASSET_KIND, INDEX);
    }
    return assets;
  }
}
