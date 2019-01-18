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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.INDEX;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findAsset;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.toContent;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.INDEX_PATH_PREFIX;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

/**
 * PyPi specific implementation of {@link GroupFacetImpl} merging and caching index
 *
 * @since 3.15
 */
@Named
@Facet.Exposed
public class PyPiGroupFacet
    extends GroupFacetImpl
{
  @Inject
  public PyPiGroupFacet(final RepositoryManager repositoryManager,
                        final ConstraintViolationFactory constraintViolationFactory,
                        @Named(GroupType.NAME) final Type groupType)
  {
    super(repositoryManager, constraintViolationFactory, groupType);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent event) {
    maybeDeleteFromCache(event);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent event) {
    maybeDeleteFromCache(event);
  }

  private void maybeDeleteFromCache(final AssetEvent event) {
    if (event.isLocal() &&
        member(event.getRepositoryName()) &&
        ROOT_INDEX.name().equals(event.getAsset().formatAttributes().get(P_ASSET_KIND, String.class))) {
      deleteFromCache(INDEX_PATH_PREFIX);
    }
  }

  private void deleteFromCache(final String name) {
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      doDeleteFromCache(name);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @TransactionalDeleteBlob
  protected void doDeleteFromCache(final String name) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = tx.findAssetWithProperty(P_NAME, name, tx.findBucket(getRepository()));
    if (asset != null) {
      log.info("Deleting cached content {} from {}", name, getRepository().getName());
      tx.deleteAsset(asset);
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
  public Payload saveToCache(final String name, final Content content) throws IOException {
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
