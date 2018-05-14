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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
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
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
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
  public Content mergeAndCache(final MavenPath mavenPath,
      final Map<Repository, Response> responses) throws IOException
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

    TempBlob tempBlob = null;

    try {
      String contentType = null;

      if (mavenFacet.getMavenPathParser().isRepositoryMetadata(mavenPath)) {
        tempBlob = merge(repositoryMetadataMerger::merge, mavenPath, contents);
        contentType = MavenMimeRulesSource.METADATA_TYPE;
      }
      else if (mavenPath.getFileName().equals(Constants.ARCHETYPE_CATALOG_FILENAME)) {
        tempBlob = merge(archetypeCatalogMerger::merge, mavenPath, contents);
        contentType = ContentTypes.APPLICATION_XML;
      }

      if (tempBlob == null) {
        log.trace("No content resulted out of merge");
        return null;
      }
      log.trace("Caching merged content");
      return cache(mavenPath, tempBlob, contentType);
    }
    finally {
      if(tempBlob != null) {
        tempBlob.close();
      }
    }
  }

  /**
   * Allows different merge methods to be used with the {@link StreamCopier}
   */
  interface MetadataMerger {
    void merge(OutputStream outputStream, MavenPath mavenPath, LinkedHashMap<Repository, Content> contents);
  }

  private TempBlob merge(final MetadataMerger merger, final MavenPath mavenPath, final LinkedHashMap<Repository, Content> contents) {
    return new StreamCopier<>(
        outputStream -> merger.merge(outputStream, mavenPath, contents),
        this::createTempBlob).read();
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
   * Caches the merged content and it's Maven2 format required sha1/md5 hashes along.
   */
  private Content cache(final MavenPath mavenPath,
                        final TempBlob tempBlob,
                        final String contentType) throws IOException {
    AttributesMap attributesMap = new AttributesMap();
    maintainCacheInfo(attributesMap);
    mayAddETag(attributesMap, tempBlob.getHashes());

    return MavenFacetUtils.putWithHashes(mavenFacet, mavenPath, tempBlob, contentType, attributesMap);
  }

  /**
   * Evicts the cached content and it's Maven2 format required sha1/md5 hashes along.
   */
  private void evictCache(final MavenPath mavenPath) throws IOException {
    MavenFacetUtils.deleteWithHashes(mavenFacet, mavenPath);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetEvent event) {
    // only make DB changes on the originating node, as orient will also replicate those for us
    if (event.isLocal() && event.getComponentId() == null && member(event.getRepositoryName())) {
      final String path = event.getAsset().name();
      final MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
      // group deletes path + path.hashes, but it should do only on content change in member
      if (!mavenPath.isHash()) {
        UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
        try {
          evictCache(mavenPath);
        }
        catch (IOException e) {
          log.warn("Could not evict merged content from {} cache at {}", getRepository().getName(),
              mavenPath.getPath(), e);
        }
        finally {
          UnitOfWork.end();
        }
      }
    }
  }
}
