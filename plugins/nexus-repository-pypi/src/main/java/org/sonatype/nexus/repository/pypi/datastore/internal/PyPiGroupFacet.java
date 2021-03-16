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
package org.sonatype.nexus.repository.pypi.datastore.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.io.Cooperation;
import org.sonatype.nexus.common.io.CooperationFactory;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.datastore.PypiContentFacet;
import org.sonatype.nexus.repository.pypi.AssetKind;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.pypi.AssetKind.INDEX;
import static org.sonatype.nexus.repository.pypi.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiStorageUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

/**
 * PyPi specific implementation of {@link GroupFacetImpl} allowing for {@link Cooperation}, merging and caching
 *
 * @since 3.29
 */
@Named
@Exposed
public class PyPiGroupFacet
    extends GroupFacetImpl
    implements EventAware.Asynchronous
{
  @Nullable
  private CooperationFactory.Builder cooperationBuilder;

  @Nullable
  private Cooperation indexRootCooperation;

  private PypiContentFacet contentFacet;

  @Inject
  public PyPiGroupFacet(
      final RepositoryManager repositoryManager,
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
    contentFacet = facet(PypiContentFacet.class);
    buildCooperation();
  }

  /**
   * Build the PyPi Index Root merging all the given responses into one. This method allows {@link Cooperation} to work,
   * meaning that multiple requests to the same group request path will join in returning the same result.
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

  protected Content buildMergedIndexRoot(final String name, final Supplier<String> lazyMergeResult, final boolean save)
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
  @AllowConcurrentEvents
  public void onAssetUploadedEvent(final AssetUploadedEvent event) {
    handleAssetEvent(event, false);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onAssetDeletedEvent(final AssetDeletedEvent event) {
    handleAssetEvent(event, true);
  }

  private void handleAssetEvent(final AssetEvent event, final boolean delete) {
    event.getRepository().ifPresent(repository -> {
      if (member(repository) && ROOT_INDEX.name().equals(event.getAsset().kind())) {
        doInvalidateCache(event.getAsset(), delete);
      }
    });
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onAssetPurgedEvent(final AssetPurgedEvent event) {
    event.getRepository().ifPresent(repository -> {
      if (!member(repository)) {
        return;
      }
      ContentFacet contentFacet = repository.facet(ContentFacet.class);
      for (int assetId : event.getAssetIds()) {
        contentFacet.assets()
        .find(new DetachedEntityId(valueOf(assetId)))
        .ifPresent(asset -> doInvalidateCache(asset, true));
      }
    });
  }

  protected void doInvalidateCache(final Asset asset, final boolean delete) {
    log.info("Invalidating cached content {} from {}", asset.path(), getRepository().getName());
    Optional<FluentAsset> fluentAssetOpt = getRepository().facet(PypiContentFacet.class).getAsset(asset.path());
    fluentAssetOpt.ifPresent(fluentAsset -> {
      if (delete) {
        fluentAsset.delete();
      }
      fluentAsset.markAsStale();
    });
  }

  public Content getFromCache(final String name, final AssetKind assetKind) {
    checkArgument(INDEX.equals(assetKind) || ROOT_INDEX.equals(assetKind), "Only index files are cached");
    return contentFacet.getAsset(name)
        .map(FluentAsset::download)
        .orElse(null);
  }

  /**
   * Determines if the {@link Content} is stale, initially by assessing if there is no lastModified time stamp
   * associated with the content retrieved from cache then checking if the {@link org.sonatype.nexus.repository.cache.CacheController}
   * has been cleared. Lastly, it should iterate over the members to see if any of the data has been modified since
   * caching.
   *
   * @param name      of the cached asset
   * @param content   of the asset
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

  public Content saveToCache(final String name, final Content content) {
    try (TempBlob tempBlob = contentFacet.blobs().ingest(content, HASH_ALGORITHMS)) {
      return contentFacet.assets()
          .path(name)
          .kind(INDEX.name())
          .blob(tempBlob)
          .save()
          .markAsCached(content)
          .download();
    }
  }

  private DateTime extractLastModified(final Content content) {
    DateTime lastModified;
    if (content != null && content.getAttributes().contains(CONTENT_LAST_MODIFIED)) {
      lastModified = content.getAttributes().get(CONTENT_LAST_MODIFIED, DateTime.class);
    }
    else {
      lastModified = null;
    }
    return lastModified;
  }
}
