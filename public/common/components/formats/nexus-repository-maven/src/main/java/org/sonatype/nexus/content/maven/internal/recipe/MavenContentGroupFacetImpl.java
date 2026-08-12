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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.MavenMimeRulesSource;
import org.sonatype.nexus.repository.maven.internal.group.ArchetypeCatalogMerger;
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet;
import org.sonatype.nexus.repository.maven.internal.group.RepositoryMetadataMerger;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.ByteStreams.toByteArray;
import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Maven2 specific implementation of {@link GroupFacetImpl} using the content store.
 *
 * @since 3.27
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MavenContentGroupFacetImpl
    extends GroupFacetImpl
    implements MavenGroupFacet, EventAware.Asynchronous
{
  private static final String PATH_PREFIX = "/";

  private final RepositoryMetadataMerger repositoryMetadataMerger;

  private final ArchetypeCatalogMerger archetypeCatalogMerger;

  // Sentinel meaning the cached value has not yet been computed or was invalidated by doUpdate().
  @VisibleForTesting
  static final int UNINITIALIZED = Integer.MIN_VALUE;

  // Cached minimum metadataMaxAge (seconds) across proxy leaf members.
  // UNINITIALIZED until first getCached() call (state is STARTED by then, so leafMembers() is safe).
  // Reset to UNINITIALIZED by doUpdate() so the next getCached() recomputes after member changes.
  @VisibleForTesting
  volatile int minProxyMetadataMaxAgeSeconds = UNINITIALIZED;

  @Autowired
  public MavenContentGroupFacetImpl(
      final RepositoryManager repositoryManager,
      final ConstraintViolationFactory constraintViolationFactory,
      @Qualifier(GroupType.NAME) final Type groupType,
      final RepositoryCacheInvalidationService repositoryCacheInvalidationService)
  {
    super(repositoryManager, constraintViolationFactory, groupType, repositoryCacheInvalidationService);

    this.repositoryMetadataMerger = new RepositoryMetadataMerger();
    this.archetypeCatalogMerger = new ArchetypeCatalogMerger();
  }

  @Override
  protected void doUpdate(final Configuration configuration) throws Exception {
    super.doUpdate(configuration);
    minProxyMetadataMaxAgeSeconds = UNINITIALIZED;
  }

  @Override
  protected void cleanupOrphanedGroupAssets(final Set<String> removedMemberNames) {
    log.info("Deleting ALL orphaned Maven assets for removed members : {}", removedMemberNames);

    try {
      ContentFacet contentFacet = getRepository().facet(ContentFacet.class);
      Continuation<FluentAsset> assetPage = contentFacet.assets().browse(Continuations.BROWSE_LIMIT, null);
      AtomicInteger deletedCount = new AtomicInteger(0);

      while (assetPage != null) {
        assetPage.forEach(asset -> {
          if (!asset.component().isPresent()) {
            try {
              asset.delete();
              log.debug("Deleted orphaned asset: {}", asset.path());
              deletedCount.incrementAndGet();
            }
            catch (Exception e) {
              log.warn("Failed to delete orphaned asset: {}", asset.path(), e);
            }
          }
        });

        try {
          String nextToken = assetPage.nextContinuationToken();
          assetPage = nextToken != null ? contentFacet.assets().browse(Continuations.BROWSE_LIMIT, nextToken) : null;
        }
        catch (IllegalStateException e) {
          assetPage = null;
        }
      }

      log.info("Deleted {} orphaned assets", deletedCount.get());
      log.info("Completed orphaned asset cleanup");
    }
    catch (Exception e) {
      log.error("Failed to cleanup orphaned Maven metadata after removing members", e);
    }
  }

  protected void scheduleBrowseNodeCleanup() {
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(1000);
        getRepository().optionalFacet(BrowseFacet.class).ifPresent(browseFacet -> {
          try {
            log.info("Trimming empty browse node folders after asset cleanup");
            browseFacet.trimBrowseNodes();
            log.info("Successfully trimmed empty browse node folders");
          }
          catch (Exception e) {
            log.warn("Failed to trim empty browse node folders", e);
          }
        });
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Browse node cleanup was interrupted", e);
      }
    });
  }

  @Nullable
  @Override
  public Content getCached(final MavenPath mavenPath) throws IOException {
    checkMergeHandled(mavenPath);

    final String path = prependIfMissing(mavenPath.getPath(), "/");

    log.trace("Checking cache for {}", path);

    Optional<FluentAsset> fluentAsset = getRepository()
        .facet(ContentFacet.class)
        .assets()
        .path(path)
        .find();

    if (!fluentAsset.isPresent()) {
      log.trace("cache miss for {}", path);
      return null;
    }

    // hashes are recalculated whenever metadata is merged, so they're always fresh
    FluentAsset asset = fluentAsset.get();
    Content content = asset.download();
    if (content.getSize() == 0L) {
      log.debug("Corrupted repository metadata: {}, source: {}", path, getRepository().getName());
      // rebuilt metadata process will be triggered for a group repository
      return null;
    }

    if (mavenPath.isHash()) {
      log.trace("Cache hit for hash {}", path);
      return new Content(content);
    }

    if (asset.isStale(cacheController)) {
      log.trace("Cache stale hit for {}", path);
      return null;
    }

    // The group's own cacheController has infinite TTL (-1) and never expires by age.
    // Honour the minimum metadataMaxAge of proxy members so cached merged metadata
    // is not served beyond the TTL that the proxies themselves would enforce.
    // Value is computed lazily here (state is STARTED at this point) and cached until doUpdate().
    int proxyTtl = minProxyMetadataMaxAgeSeconds;
    if (proxyTtl == UNINITIALIZED) {
      minProxyMetadataMaxAgeSeconds = proxyTtl = computeMinProxyMetadataMaxAgeSeconds();
    }
    if (proxyTtl >= 0 && asset.isStale(new CacheController(proxyTtl, null))) {
      log.trace("Cache stale for proxy member metadataMaxAge={}s: {}", proxyTtl, path);
      return null;
    }

    log.trace("Cache fresh hit for {}", path);
    return new Content(content);
  }

  @Nullable
  @Override
  public Content mergeAndCache(
      final MavenPath mavenPath,
      final Map<Repository, Response> responses) throws IOException
  {
    return merge(
        mavenPath,
        responses,
        this::createTempBlob,
        (tempBlob, contentType) -> {
          log.trace("Caching merged content");
          return cache(mavenPath, tempBlob, contentType);
        });
  }

  @Nullable
  @Override
  public Content mergeWithoutCaching(
      final MavenPath mavenPath,
      final Map<Repository, Response> responses) throws IOException
  {
    return merge(mavenPath, responses, Function.identity(), (in, contentType) -> {
      // load bytes in memory to make content re-usable; metadata shouldn't be too large
      // (don't include cache-related attributes since this content has not been cached)
      return new Content(new BytesPayload(toByteArray(in), contentType));
    });
  }

  /**
   * Attempts to cache the merged content, falling back to temporary uncached result if necessary.
   */
  private Content cache(
      final MavenPath mavenPath,
      final TempBlob tempBlob,
      final String contentType) throws IOException
  {
    try {
      Content content = new Content(getRepository().facet(MavenContentFacet.class)
          .put(mavenPath, new BlobPayload(tempBlob.getBlob(), contentType)));

      maintainCacheInfo(content.getAttributes());
      mayAddETag(content.getAttributes(), tempBlob.getHashes());

      for (Entry<HashAlgorithm, HashCode> entry : tempBlob.getHashes().entrySet()) {
        getRepository().facet(MavenContentFacet.class)
            .put(mavenPath.hash(entry.getKey()), new StringPayload(entry.getValue().toString(), TEXT_PLAIN));
      }

      getRepository().facet(ContentFacet.class)
          .assets()
          .path(prependIfMissing(mavenPath.getPath(), "/"))
          .find()
          .ifPresent(a -> a.markAsCached(content));

      return content;
    }
    catch (Exception e) {
      log.warn("Problem caching merged content {} : {}",
          getRepository().getName(), mavenPath.getPath(), e);
    }

    // Handle exception by forcing re-merge on next request and retrieving content from TempBlob
    getRepository().facet(ContentFacet.class)
        .assets()
        .path(prependIfMissing(mavenPath.getPath(), "/"))
        .find()
        .ifPresent(FluentAsset::markAsStale);

    try (InputStream in = tempBlob.get()) {
      // load bytes in memory before tempBlob vanishes; metadata shouldn't be too large
      // (don't include cache-related attributes since this content has not been cached)
      return new Content(new BytesPayload(toByteArray(in), contentType));
    }
  }

  private <T> T merge(
      final MetadataMerger merger,
      final MavenPath mavenPath,
      final LinkedHashMap<Repository, Content> contents,
      final Function<InputStream, T> streamFunction)
  {
    return new StreamCopier<>(
        outputStream -> merger.merge(outputStream, mavenPath, contents),
        streamFunction).read();
  }

  private TempBlob createTempBlob(final InputStream inputStream) {
    List<HashAlgorithm> hashAlgorithms = stream(HashType.values())
        .map(HashType::getHashAlgorithm)
        .collect(toList());

    return getRepository().facet(ContentFacet.class).blobs().ingest(inputStream, null, hashAlgorithms);
  }

  /**
   * Returns the minimum {@code metadataMaxAge} in seconds across all proxy leaf members,
   * or {@code -1} if no proxy member has a finite TTL. Used to enforce proxy TTLs on the
   * group's merged metadata cache, which otherwise has an infinite TTL.
   */
  @VisibleForTesting
  int computeMinProxyMetadataMaxAgeSeconds() {
    return leafMembers().stream()
        .flatMap(member -> member.optionalFacet(ProxyFacet.class).stream())
        .mapToInt(proxy -> (int) Math.min(
            proxy.getConfiguration().getMetadataMaxAge().getSeconds(), Integer.MAX_VALUE))
        .filter(seconds -> seconds >= 0)
        .min()
        .orElse(-1);
  }

  /**
   * Verifies that merge is handled.
   */
  private void checkMergeHandled(final MavenPath mavenPath) {
    checkArgument(
        getRepository().facet(MavenContentFacet.class).getMavenPathParser().isRepositoryMetadata(mavenPath)
            || mavenPath.main().getFileName().equals(Constants.ARCHETYPE_CATALOG_FILENAME),
        "Not handled by Maven2GroupFacet merge: %s",
        mavenPath);
  }

  @Nullable
  private <T extends Closeable> Content merge(
      final MavenPath mavenPath,
      final Map<Repository, Response> responses,
      final Function<InputStream, T> streamFunction,
      final ContentFunction<T> contentFunction) throws IOException
  {
    checkMergeHandled(mavenPath);
    // we do not cache subordinates/hashes, they are created as side-effect of cache
    checkArgument(!mavenPath.isSubordinate(), "Only main content handled, not hash or signature: %s", mavenPath);
    LinkedHashMap<Repository, Content> contents = Maps.newLinkedHashMap();
    for (Map.Entry<Repository, Response> entry : responses.entrySet()) {
      if (entry.getValue().getStatus().getCode() == HttpStatus.OK) {
        Response response = entry.getValue();
        if (response.getPayload() instanceof Content) {
          contents.put(entry.getKey(), (Content) response.getPayload());
        }
      }
    }

    if (contents.isEmpty()) {
      log.trace("No 200 OK responses to merge");
      return null;
    }

    T data = null;

    try {
      String contentType = null;
      if (getRepository().facet(MavenContentFacet.class).getMavenPathParser().isRepositoryMetadata(mavenPath)) {
        data = merge(repositoryMetadataMerger::merge, mavenPath, contents, streamFunction);
        contentType = MavenMimeRulesSource.METADATA_TYPE;
      }
      else if (mavenPath.getFileName().equals(Constants.ARCHETYPE_CATALOG_FILENAME)) {
        data = merge(archetypeCatalogMerger::merge, mavenPath, contents, streamFunction);
        contentType = ContentTypes.APPLICATION_XML;
      }

      if (data == null) {
        log.trace("No content resulted out of merge");
        return null;
      }

      return contentFunction.apply(data, contentType);
    }
    finally {
      if (data != null) {
        data.close();
      }
    }
  }

  /**
   * Adds {@link Content#CONTENT_ETAG} content attribute if not present. In case of hosted repositories, this is safe
   * and even good thing to do, as the content is hosted here only and NX is content authority.
   */
  private void mayAddETag(
      final AttributesMap attributesMap,
      final Map<HashAlgorithm, HashCode> hashCodes)
  {
    if (attributesMap.contains(Content.CONTENT_ETAG)) {
      return;
    }
    HashCode sha1HashCode = hashCodes.get(HashAlgorithm.SHA1);
    if (sha1HashCode != null) {
      attributesMap.set(Content.CONTENT_ETAG, "{SHA1{" + sha1HashCode + "}}");
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onAssetCreatedEvent(final AssetCreatedEvent event) {
    handleAssetEvent(event, false);
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
    event.getRepository().ifPresent(repository -> maybeEvict(repository, event.getAsset(), delete));
  }

  /**
   * Asset delete events may not have the Component, so we watch for deletions via components.
   * When a component is deleted from a member repository, we need to invalidate the cached
   * metadata in this group repository.
   *
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onComponentDeletedEvent(final ComponentDeletedEvent event) {
    org.sonatype.nexus.repository.content.Component component = event.getComponent();
    event.getRepository().ifPresent(repository -> {
      if (member(repository) && !ProxyType.NAME.equals(repository.getType().getValue())) {
        log.debug("Component deleted from member repository {}: {}", repository.getName(),
            component.toStringExternal());
        invalidateMetadataForComponent(component);
      }
    });
  }

  /**
   * Invalidates group metadata cache for the given component's Maven coordinates.
   * Extracts groupId, artifactId, and baseVersion from the component and marks
   * corresponding metadata paths as stale in the group repository.
   *
   * @param component the deleted component from a member repository
   */
  private void invalidateMetadataForComponent(final org.sonatype.nexus.repository.content.Component component) {
    try {
      // Extract Maven coordinates from component
      String groupId = component.namespace(); // namespace = groupId for Maven
      String artifactId = component.name(); // name = artifactId for Maven
      String baseVersion = component.attributes(Maven2Format.NAME).get(P_BASE_VERSION, String.class);

      if (StringUtils.isBlank(groupId) || StringUtils.isBlank(artifactId) || StringUtils.isBlank(baseVersion)) {
        log.debug("Unable to extract complete GAV from component: {}", component.toStringExternal());
        return;
      }

      log.debug("Invalidating group metadata for {}:{}:{}", groupId, artifactId, baseVersion);

      // Build metadata paths that need to be invalidated
      String groupPath = groupId.replace('.', '/');
      String artifactPath = groupPath + "/" + artifactId;

      ContentFacet contentFacet = getRepository().facet(ContentFacet.class);

      // Invalidate artifact-level metadata: groupId/artifactId/maven-metadata.xml
      // This metadata file lists all available versions for the artifact
      invalidateMetadataPath(contentFacet, artifactPath + "/maven-metadata.xml");

    }
    catch (Exception e) {
      log.warn("Failed to invalidate group metadata for component: {}", component.toStringExternal(), e);
    }
  }

  /**
   * Marks a specific metadata path as stale in the group repository cache.
   *
   * @param contentFacet the content facet for accessing assets
   * @param metadataPath the path to the metadata file (without leading slash)
   */
  private void invalidateMetadataPath(final ContentFacet contentFacet, final String metadataPath) {
    String pathWithSlash = prependIfMissing(metadataPath, PATH_PREFIX);
    contentFacet.assets()
        .path(pathWithSlash)
        .find()
        .ifPresent(asset -> {
          log.trace("Marking as stale: {}", pathWithSlash);
          asset.markAsStale();
        });
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onAssetPurgedEvent(final AssetPurgedEvent event) {
    event.getRepository().ifPresent(repository -> {
      for (int assetId : event.getAssetIds()) {
        repository.facet(ContentFacet.class)
            .assets()
            .find(new DetachedEntityId(valueOf(assetId)))
            .ifPresent(asset -> maybeEvict(repository, asset, true));
      }
    });
  }

  private void maybeEvict(final Repository repository, final Asset asset, final boolean delete) {
    log.trace("Maybe evicting memberRepo:{} assetPath:{} shouldDelete:{}", repository.getName(), asset.path(), delete);
    if (!asset.component().isPresent() && member(repository)) {
      final String path = asset.path();
      final MavenPath mavenPath = getRepository().facet(MavenContentFacet.class).getMavenPathParser().parsePath(path);
      // only trigger eviction on a fresh main metadata artifact (which may go on to evict its hashes)
      if (!mavenPath.isHash()) {
        if (delete) {
          try {
            getRepository().facet(MavenContentFacet.class).deleteWithHashes(mavenPath);
            return;
          }
          catch (Exception e) {
            log.warn("Problem deleting cached content {} : {}, will invalidate instead",
                getRepository().getName(), mavenPath.getPath(), e);
          }
        }
        getRepository().facet(ContentFacet.class)
            .assets()
            .path(prependIfMissing(mavenPath.main().getPath(), PATH_PREFIX))
            .find()
            .ifPresent(FluentAsset::markAsStale);
      }
    }
  }
}
