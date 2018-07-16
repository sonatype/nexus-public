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
package org.sonatype.nexus.repository.maven.internal.group;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.MavenFacetUtils;
import org.sonatype.nexus.repository.maven.internal.MavenMimeRulesSource;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.MissingBlobException;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.maven.internal.MavenFacetUtils.findAsset;
import static org.sonatype.nexus.repository.maven.internal.MavenFacetUtils.mayAddETag;

/**
 * Maven2 specific implementation of {@link GroupFacetImpl}: metadata merge and archetype catalog merge is handled.
 *
 * @since 3.0
 */
@Named
@Facet.Exposed
public class MavenGroupFacet
    extends GroupFacetImpl
{
  private final RepositoryMetadataMerger repositoryMetadataMerger;

  private final ArchetypeCatalogMerger archetypeCatalogMerger;

  private MavenFacet mavenFacet;

  @Inject
  public MavenGroupFacet(final RepositoryManager repositoryManager,
                         final ConstraintViolationFactory constraintViolationFactory,
                         @Named(GroupType.NAME) final Type groupType)
  {
    super(repositoryManager, constraintViolationFactory, groupType);
    this.repositoryMetadataMerger = new RepositoryMetadataMerger();
    this.archetypeCatalogMerger = new ArchetypeCatalogMerger();
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    this.mavenFacet = facet(MavenFacet.class);
  }

  /**
   * Fetches cached content if exists, or {@code null}.
   */
  @Nullable
  public Content getCached(final MavenPath mavenPath) throws IOException
  {
    checkMergeHandled(mavenPath);
    Content content = mavenFacet.get(mavenPath);
    if (mavenPath.isHash()) {
      return content; // hashes are recalculated whenever metadata is merged, so they're always fresh
    }
    return !isStale(content) ? content : null;
  }

  /**
   * Merges and caches and returns the merged metadata. Returns {@code null} if no usable response was in passed in
   * map.
   */
  @Nullable
  public Content mergeAndCache(final MavenPath mavenPath, final Map<Repository, Response> responses)
      throws IOException
  {
    return merge(mavenPath, responses, this::createTempBlob, (tempBlob, contentType) -> {
      log.trace("Caching merged content");
      return cache(mavenPath, tempBlob, contentType);
    });
  }

  /**
   * Merges the metadata but doesn't cache it. Returns {@code null} if no usable response was in passed in map.
   *
   * @since 3.13
   */
  @Nullable
  public Content mergeWithoutCaching(final MavenPath mavenPath, final Map<Repository, Response> responses)
      throws IOException
  {
    return merge(mavenPath, responses, Function.identity(), (in, contentType) -> {
      // load bytes in memory to make content re-usable; metadata shouldn't be too large
      // (don't include cache-related attributes since this content has not been cached)
      return new Content(new BytesPayload(toByteArray(in), contentType));
    });
  }

  @FunctionalInterface
  private interface ContentFunction<T>
  {
    Content apply(T data, String contentType) throws IOException;
  }

  @Nullable
  private <T extends Closeable> Content merge(final MavenPath mavenPath,
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
      if (mavenFacet.getMavenPathParser().isRepositoryMetadata(mavenPath)) {
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
   * Allows different merge methods to be used with the {@link StreamCopier}
   */
  interface MetadataMerger {
    void merge(OutputStream outputStream, MavenPath mavenPath, LinkedHashMap<Repository, Content> contents);
  }

  private <T> T merge(final MetadataMerger merger,
                      final MavenPath mavenPath,
                      final LinkedHashMap<Repository, Content> contents,
                      final Function<InputStream, T> streamFunction)
  {
    return new StreamCopier<>(
        outputStream -> merger.merge(outputStream, mavenPath, contents),
        streamFunction).read();
  }

  private TempBlob createTempBlob(final InputStream inputStream) {
    StorageFacet storageFacet = getRepository().facet(StorageFacet.class);
    List<HashAlgorithm> hashAlgorithms = stream(HashType.values())
        .map(HashType::getHashAlgorithm)
        .collect(toList());
    return storageFacet.createTempBlob(inputStream, hashAlgorithms);
  }

  /**
   * Verifies that merge is handled.
   */
  private void checkMergeHandled(final MavenPath mavenPath) {
    checkArgument(
        mavenFacet.getMavenPathParser().isRepositoryMetadata(mavenPath)
            || mavenPath.getFileName().equals(Constants.ARCHETYPE_CATALOG_FILENAME),
        "Not handled by Maven2GroupFacet merge: %s",
        mavenPath
    );
  }

  /**
   * Attempts to cache the merged content, falling back to temporary uncached result if necessary.
   */
  private Content cache(final MavenPath mavenPath,
                        final TempBlob tempBlob,
                        final String contentType) throws IOException {

    AttributesMap attributesMap = new AttributesMap();
    maintainCacheInfo(attributesMap);
    mayAddETag(attributesMap, tempBlob.getHashes());

    try {
      return doCache(mavenPath, tempBlob, contentType, attributesMap);
    }
    catch (ONeedRetryException | ORecordDuplicatedException | MissingBlobException e) {
      log.debug("Conflict caching merged content {} : {}",
          getRepository().getName(), mavenPath.getPath(), e);
    }
    catch (Exception e) {
      log.warn("Problem caching merged content {} : {}",
          getRepository().getName(), mavenPath.getPath(), e);
    }

    invalidatePath(mavenPath); // sanity: force re-merge on next request

    try (InputStream in = tempBlob.get()) {
      // load bytes in memory before tempBlob vanishes; metadata shouldn't be too large
      // (don't include cache-related attributes since this content has not been cached)
      return new Content(new BytesPayload(toByteArray(in), contentType));
    }
  }

  /**
   * Caches the merged content and its Maven2 format required sha1/md5 hashes along.
   *
   * Declare this method as transactional before calling the main facet as we want to
   * create the merged metadata as well as its Maven2 hashes in a single transaction.
   * We also want to avoid retrying if another thread concurrently works on the same
   * metadata path.
   */
  @Transactional
  protected Content doCache(final MavenPath mavenPath,
                            final TempBlob tempBlob,
                            final String contentType,
                            final AttributesMap attributesMap)
      throws IOException
  {
    return MavenFacetUtils.putWithHashes(mavenFacet, mavenPath, tempBlob, contentType, attributesMap);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetEvent event) {
    // only make DB changes on the originating node, as orient will also replicate those for us
    if (event.isLocal() && event.getComponentId() == null && member(event.getRepositoryName())) {
      final String path = event.getAsset().name();
      final MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
      // only trigger eviction on main metadata artifact (which may go on to evict its hashes)
      if (!mavenPath.isHash()) {
        UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
        try {
          evictCache(mavenPath, event instanceof AssetDeletedEvent);
        }
        finally {
          UnitOfWork.end();
        }
      }
    }
  }

  /**
   * Evicts the cached content and potentially its Maven2 format required sha1/md5 hashes.
   */
  private void evictCache(final MavenPath mavenPath, final boolean delete) {
    if (delete) {
      try {
        deletePath(mavenPath);
        return; // no need to invalidate
      }
      catch (ONeedRetryException | MissingBlobException e) {
        log.debug("Conflict deleting cached content {} : {}, will invalidate instead",
            getRepository().getName(), mavenPath.getPath(), e);
      }
      catch (Exception e) {
        log.warn("Problem deleting cached content {} : {}, will invalidate instead",
            getRepository().getName(), mavenPath.getPath(), e);
      }
    }

    invalidatePath(mavenPath);
  }

  /**
   * Attempts to delete previously cached content for the given {@link MavenPath}.
   *
   * Declare this method as transactional before calling the main facet as we want to
   * override the default semantics and let retry exceptions propagate to the caller.
   * We also want to avoid retrying if another thread concurrently works on the same
   * metadata path.
   */
  @Transactional(swallow = ORecordNotFoundException.class)
  protected void deletePath(final MavenPath mavenPath) throws IOException {
    MavenFacetUtils.deleteWithHashes(mavenFacet, mavenPath);
  }

  /**
   * Invalidates the previously cached content for the given {@link MavenPath}.
   *
   * Note: in the current maven-group design hashes are always considered 'fresh',
   * so we don't mark them as stale. Instead they will be automatically refreshed
   * when the main content is next requested.
   *
   * @see #getCached(MavenPath)
   */
  private void invalidatePath(final MavenPath mavenPath) {
    try {
      doInvalidate(mavenPath);
    }
    catch (Exception e) {
      log.warn("Problem invalidating cached content {} : {}",
          getRepository().getName(), mavenPath.getPath(), e);
    }
  }

  /**
   * Tries to invalidate the current main cached asset for the given {@link MavenPath}.
   */
  @Transactional(retryOn = ONeedRetryException.class, swallow = ORecordNotFoundException.class)
  protected void doInvalidate(final MavenPath mavenPath) {
    StorageTx tx = UnitOfWork.currentTx();
    final Asset asset = findAsset(tx, tx.findBucket(getRepository()), mavenPath.main());
    if (asset != null && CacheInfo.invalidateAsset(asset)) {
      tx.saveAsset(asset);
    }
  }
}
