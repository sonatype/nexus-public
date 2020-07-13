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
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
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
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Named
public class MavenContentGroupFacetImpl
    extends GroupFacetImpl
    implements MavenGroupFacet
{

  private final RepositoryMetadataMerger repositoryMetadataMerger;
  private final ArchetypeCatalogMerger archetypeCatalogMerger;

  @Inject
  public MavenContentGroupFacetImpl(
      final RepositoryManager repositoryManager,
      final ConstraintViolationFactory constraintViolationFactory,
      @Named(GroupType.NAME) final Type groupType)
  {
    super(repositoryManager, constraintViolationFactory, groupType);

    repositoryMetadataMerger = new RepositoryMetadataMerger();
    archetypeCatalogMerger = new ArchetypeCatalogMerger();
  }

  @Nullable
  @Override
  public Content getCached(final MavenPath mavenPath) throws IOException {
    checkMergeHandled(mavenPath);

    Optional<FluentAsset> fluentAsset = getRepository()
        .facet(ContentFacet.class)
        .assets()
        .path(mavenPath.getPath())
        .find();

    if(!fluentAsset.isPresent() || fluentAsset.get().isStale(cacheController)){
      return null;
    }

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
  private Content cache(final MavenPath mavenPath,
                        final TempBlob tempBlob,
                        final String contentType) throws IOException {

    AttributesMap attributesMap = new AttributesMap();
    maintainCacheInfo(attributesMap);
    mayAddETag(attributesMap, tempBlob.getHashes());

    Content content = null;
    try {
      content = new Content(getRepository().facet(MavenContentFacet.class)
          .put(mavenPath, new BlobPayload(tempBlob.getBlob(), contentType)));
    } catch (Exception e) {
      log.warn("Problem caching merged content {} : {}",
          getRepository().getName(), mavenPath.getPath(), e);
    }

    getRepository().facet(ContentFacet.class).assets().path(mavenPath.getPath()).find().ifPresent(FluentAsset::markAsStale);
    return content;
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
  private void mayAddETag(final AttributesMap attributesMap,
                          final Map<HashAlgorithm, HashCode> hashCodes) {
    if (attributesMap.contains(Content.CONTENT_ETAG)) {
      return;
    }
    HashCode sha1HashCode = hashCodes.get(HashAlgorithm.SHA1);
    if (sha1HashCode != null) {
      attributesMap.set(Content.CONTENT_ETAG, "{SHA1{" + sha1HashCode + "}}");
    }
  }
}
