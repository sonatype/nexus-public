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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.io.Cooperation;
import org.sonatype.nexus.common.io.CooperationFactory;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.reverse;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.joda.time.DateTime.now;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.cache.CacheInfo.invalidateAsset;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.errorInputStream;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.findPackageRootAsset;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.savePackageRoot;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.toContent;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.REMOVE_DEFAULT_FIELDS_MATCHERS;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.rewriteTarballUrlMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmHandlers.packageId;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.mergeContents;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.parseContent;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.rewriteTarballUrl;
import static org.sonatype.nexus.repository.view.Content.applyToAsset;
import static org.sonatype.nexus.repository.view.Content.maintainLastModified;

/**
 * NPM specific implementation of {@link GroupFacetImpl} allowing for {@link Cooperation}, merging and caching.
 *
 * @since 3.15
 */
@Named
@Exposed
public class NpmGroupFacet
    extends GroupFacetImpl
{
  private final boolean mergeMetadata;

  @Nullable
  private CooperationFactory.Builder cooperationBuilder;

  @Nullable
  private Cooperation packageRootCooperation;

  @Inject
  public NpmGroupFacet(
      @Named("${nexus.npm.mergeGroupMetadata:-true}") final boolean mergeMetadata,
      final RepositoryManager repositoryManager,
      final ConstraintViolationFactory constraintViolationFactory,
      @Named(GroupType.NAME) final Type groupType)
  {
    super(repositoryManager, constraintViolationFactory, groupType);
    this.mergeMetadata = mergeMetadata;
  }

  @Inject
  protected void configureCooperation(
      final CooperationFactory cooperationFactory,
      @Named("${nexus.npm.packageRoot.cooperation.enabled:-true}") final boolean cooperationEnabled,
      @Named("${nexus.npm.packageRoot.cooperation.majorTimeout:-0s}") final Time majorTimeout,
      @Named("${nexus.npm.packageRoot.cooperation.minorTimeout:-30s}") final Time minorTimeout,
      @Named("${nexus.npm.packageRoot.cooperation.threadsPerKey:-100}") final int threadsPerKey)
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
      this.packageRootCooperation = cooperationBuilder.build(getRepository().getName() + ":packageRoot");
    }
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    buildCooperation();
  }

  /**
   * Build the NPM Package Root merging all the given responses into one. This method allows {@link Cooperation} to
   * work, meaning that multiple requests to the same group request path will join in returning the same result.
   *
   * @param responses {@link Map} of {@link Repository}s in a given group and the {@link Response}
   *                  for the associated {@link Repository}
   * @param context   {@link Context} of the current request to a Group Repository
   * @return Content
   * @throws IOException when unable to merge given responses in to a single package root
   */
  @Nullable
  public Content buildPackageRoot(final Map<Repository, Response> responses, final Context context) throws IOException {
    if (isNull(packageRootCooperation)) {
      return buildMergedPackageRoot(responses, context);
    }

    final String requestPath = getRequestPath(context);

    try {
      return packageRootCooperation.cooperate(requestPath, failover -> {

        if (failover) {
          // re-check cache when failing over to new thread
          Content latestContent = packageRootCooperation.join(() -> getFromCache(responses, context));
          if (nonNull(latestContent)) {
            return latestContent;
          }
        }

        return buildMergedPackageRoot(responses, context);
      });
    }
    catch (IOException e) {
      log.error("Unable to use Cooperation to merge {} for repository {}",
          requestPath, context.getRepository().getName(), e);
    }

    return null;
  }

  /**
   * Get {@link Content} wrapping the NPM Package root for the {@link Context} of the current request
   * to a Group Repository.
   *
   * @param responses {@link Map} of {@link Repository}s in a given group and the {@link Response}
   *                  for the associated {@link Repository}
   * @param context {@link Context} of the current request to a Group Repository
   * @return Content
   * @throws IOException if unable to load package root
   */
  @Nullable
  public NpmContent getFromCache(final Map<Repository, Response> responses, final Context context) throws IOException
  {
    Asset packageRootAsset = getPackageRootAssetFromCache(context);
    if (isNull(packageRootAsset)) {
      return null;
    }

    NpmContent npmContent = toContent(getRepository(), packageRootAsset);
    npmContent.fieldMatchers(rewriteTarballUrlMatcher(getRepository(), packageRootAsset.name()));
    npmContent.missingBlobInputStreamSupplier(
        (missingBlobException) -> buildMergedPackageRootOnMissingBlob(responses, context, missingBlobException));

    return !isStale(npmContent) ? npmContent : null;
  }

  /**
   * @see #getFromCache(Map, Context)
   */
  @Nullable
  public NpmContent getFromCache(final Context context) throws IOException
  {
    Asset packageRootAsset = getPackageRootAssetFromCache(context);
    if (isNull(packageRootAsset)) {
      return null;
    }

    NpmContent npmContent = toContent(getRepository(), packageRootAsset);
    npmContent.fieldMatchers(rewriteTarballUrlMatcher(getRepository(), packageRootAsset.name()));
    return !isStale(npmContent) ? npmContent : null;
  }

  @Nullable
  @TransactionalTouchBlob
  protected Asset getPackageRootAssetFromCache(final Context context) throws IOException {
    checkNotNull(context);

    StorageTx tx = UnitOfWork.currentTx();
    return findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId(matcherState(context)));
  }

  @Nullable
  @VisibleForTesting
  protected Content buildMergedPackageRoot(final Map<Repository, Response> responses, final Context context)
      throws IOException
  {
    checkNotNull(responses);
    checkNotNull(context);

    if (responses.isEmpty()) {
      log.debug("Unable to create package root for repository {}. Members had no metadata to merge.",
          context.getRepository().getName());

      return null;
    }

    List<Content> contents = responses
        .values().stream().map(response -> (Content) response.getPayload()).collect(toList());

    NestedAttributesMap result;
    NpmPackageId packageId = packageId(matcherState(context));

    if (shouldServeFirstResult(contents, packageId)) {
      result = parseContent(contents.get(0));
    }
    else {
      log.debug("Merging results from {} repositories", responses.size());

      // we make the last package the dominant one, by reversing the list
      reverse(contents);

      result = mergeContents(contents);
    }

    rewriteTarballUrl(context.getRepository().getName(), result);

    return saveToCache(packageId, result);
  }

  protected Content saveToCache(final NpmPackageId packageId, final NestedAttributesMap result) throws IOException {
    Asset packageRootAsset = savePackageRootToCache(packageId, result);
    return toContent(getRepository(), packageRootAsset).fieldMatchers(REMOVE_DEFAULT_FIELDS_MATCHERS);
  }

  @TransactionalStoreBlob
  protected Asset savePackageRootToCache(final NpmPackageId packageId, final NestedAttributesMap result)
      throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = getAsset(tx, packageId);
    AttributesMap contentAttributes = maintainLastModified(asset, null);
    maintainCacheInfo(contentAttributes);
    applyToAsset(asset, contentAttributes);

    savePackageRoot(tx, asset, result);

    return asset;
  }

  protected InputStream buildMergedPackageRootOnMissingBlob(final Map<Repository, Response> responses,
                                                            final Context context,
                                                            final MissingAssetBlobException e) throws IOException
  {
    if (log.isTraceEnabled()) {
      log.info("Missing blob {} containing cached metadata {}, deleting asset and triggering rebuild. Exception {}",
          e.getBlobRef(), e.getAsset(), e);
    }
    else {
      log.info("Missing blob {} containing cached metadata {}, deleting asset and triggering rebuild.",
          e.getBlobRef(), e.getAsset().name());
    }

    cleanupPackageRootAssetOnlyFromCache(e.getAsset());

    Content content = buildMergedPackageRoot(responses, context);
    // it should never be null, but we are being kind and return an error stream.
    return nonNull(content) ? content.openInputStream() :
        errorInputStream("Unable to retrieve merged package root on recovery for missing blob");
  }

  @Transactional
  protected void cleanupPackageRootAssetOnlyFromCache(final Asset packageRootAsset) {
    checkNotNull(packageRootAsset);

    StorageTx tx = UnitOfWork.currentTx();

    // Don't delete the blob because we already know it is missing
    tx.deleteAsset(packageRootAsset, false);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent deleted) {
    if (matchingEvent(deleted)) {
      invalidatePackageRoot(deleted);
    }
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetUpdatedEvent updated) {
    if (matchingEvent(updated) && hasBlobBeenUpdated(updated)) {
      invalidatePackageRoot(updated);
    }
  }

  @Nullable
  @Transactional(retryOn = ONeedRetryException.class, swallow = ORecordNotFoundException.class)
  protected void doInvalidate(final NpmPackageId packageId) {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset asset = findPackageRootAsset(tx, bucket, packageId);
    if (nonNull(asset) && invalidateAsset(asset)) {
      tx.saveAsset(asset);
    }
  }

  private Asset getAsset(final StorageTx tx, final NpmPackageId packageId) {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findPackageRootAsset(tx, bucket, packageId);

    if (isNull(asset)) {
      asset = tx.createAsset(bucket, getRepository().getFormat()).name(packageId.id());
    }

    return asset;
  }

  /**
   * @return True if we only have one result or if we have been asked not to merge metadata for non-scoped packages,
   * false otherwise.
   */
  @VisibleForTesting
  protected boolean shouldServeFirstResult(final List packages, final NpmPackageId packageId) {
    return packages.size() == 1 || (!mergeMetadata && isEmpty(packageId.scope()));
  }

  @Nullable
  private NestedAttributesMap getNestedAttributesMap(final Payload payload) {
    return nonNull(payload) ? ((Content) payload).getAttributes().get(NestedAttributesMap.class) : null;
  }

  private static TokenMatcher.State matcherState(final Context context) {
    return context.getAttributes().require(TokenMatcher.State.class);
  }

  private String getRequestPath(final Context context) {
    return context.getRequest().getPath().substring(1);
  }

  private boolean matchingEvent(final AssetEvent event) {
    return matchesRepository(event) && isNull(event.getComponentId());
  }

  private boolean matchesRepository(final AssetEvent event) {
    // only make DB changes on the originating node, as orient will also replicate those for us
    return event.isLocal() && member(event.getRepositoryName());
  }

  private void invalidatePackageRoot(final AssetEvent event) {
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      doInvalidate(NpmPackageId.parse(event.getAsset().name()));
    }
    finally {
      UnitOfWork.end();
    }
  }

  private boolean hasBlobBeenUpdated(final AssetUpdatedEvent updated) {
    DateTime blobUpdated = updated.getAsset().blobUpdated();
    DateTime oneMinuteAgo = now().minusMinutes(1);
    return isNull(blobUpdated) || blobUpdated.isAfter(oneMinuteAgo);
  }
}
