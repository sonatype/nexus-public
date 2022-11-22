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
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.MavenMimeRulesSource;
import org.sonatype.nexus.repository.maven.internal.group.ArchetypeCatalogMerger;
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet;
import org.sonatype.nexus.repository.maven.internal.group.RepositoryMetadataMerger;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

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
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;

/**
 * Maven2 specific implementation of {@link GroupFacetImpl} using the content store.
 *
 * @since 3.27
 */
@Named
public class MavenContentGroupFacetImpl
    extends GroupFacetImpl
    implements MavenGroupFacet, EventAware.Asynchronous
{
  private static final String PATH_PREFIX = "/";
  private final RepositoryMetadataMerger repositoryMetadataMerger;

  private final ArchetypeCatalogMerger archetypeCatalogMerger;

  @Inject
  public MavenContentGroupFacetImpl(
      final RepositoryManager repositoryManager,
      final ConstraintViolationFactory constraintViolationFactory,
      @Named(GroupType.NAME) final Type groupType,
      RepositoryCacheInvalidationService repositoryCacheInvalidationService)
  {
    super(repositoryManager, constraintViolationFactory, groupType, repositoryCacheInvalidationService);

    repositoryMetadataMerger = new RepositoryMetadataMerger();
    archetypeCatalogMerger = new ArchetypeCatalogMerger();
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
    if (mavenPath.isHash()) {
      log.trace("Cache hit for hash {}", path);
      return new Content(fluentAsset.get().download());
    }

    if (fluentAsset.get().isStale(cacheController)) {
      log.trace("Cache stale hit for {}", path);
      return null;
    }

    log.trace("Cache fresh hit for {}", path);
    return new Content(fluentAsset.get().download());
  }

  @Nullable
  @Override
  public Content mergeAndCache(
      final MavenPath mavenPath, final Map<Repository, Response> responses) throws IOException
  {
    return merge(
        mavenPath,
        responses,
        this::createTempBlob,
        (tempBlob, contentType) -> {
          log.trace("Caching merged content");
          return cache(mavenPath, tempBlob, contentType);
        }
    );
  }

  @Nullable
  @Override
  public Content mergeWithoutCaching(
      final MavenPath mavenPath, final Map<Repository, Response> responses) throws IOException
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

      getRepository().facet(ContentFacet.class).assets().path(prependIfMissing(mavenPath.getPath(), "/")).find()
          .ifPresent(a -> a.markAsCached(content));

      return content;
    }
    catch (Exception e) {
      log.warn("Problem caching merged content {} : {}",
          getRepository().getName(), mavenPath.getPath(), e);
    }

    // Handle exception by forcing re-merge on next request and retrieving content from TempBlob
    getRepository().facet(ContentFacet.class).assets().path(prependIfMissing(mavenPath.getPath(), "/")).find()
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
   * Verifies that merge is handled.
   */
  private void checkMergeHandled(final MavenPath mavenPath) {
    checkArgument(
        getRepository().facet(MavenContentFacet.class).getMavenPathParser().isRepositoryMetadata(mavenPath)
            || mavenPath.getFileName().equals(Constants.ARCHETYPE_CATALOG_FILENAME),
        "Not handled by Maven2GroupFacet merge: %s",
        mavenPath
    );
  }

  @Nullable
  private <T extends Closeable> Content merge(
      final MavenPath mavenPath,
      final Map<Repository, Response> responses,
      final Function<InputStream, T> streamFunction,
      final ContentFunction<T> contentFunction)
      throws IOException
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
        getRepository().facet(ContentFacet.class).assets()
            .path(prependIfMissing(mavenPath.main().getPath(), PATH_PREFIX))
            .find()
            .ifPresent(FluentAsset::markAsStale);
      }
    }
  }
}
