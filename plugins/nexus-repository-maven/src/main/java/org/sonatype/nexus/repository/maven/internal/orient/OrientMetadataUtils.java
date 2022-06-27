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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.MavenMimeRulesSource;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.repository.cache.CacheInfo.extractFromAsset;
import static org.sonatype.nexus.repository.maven.internal.Constants.METADATA_FILENAME;

/**
 * Utility class containing shared (orient specific) methods for Maven metadata.
 *
 * @since 3.0
 */
public final class OrientMetadataUtils
{
  private static Logger log = LoggerFactory.getLogger(OrientMetadataUtils.class);

  private static final String METADATA_REBUILD_KEY = "forceRebuild";

  private OrientMetadataUtils() {
  }

  /**
   * True if content is available for a given path, false otherwise
   */
  public static boolean exists(final Repository repository, final MavenPath mavenPath) throws IOException {
    return repository.facet(OrientMavenFacet.class).exists(mavenPath);
  }

  /**
   * Reads content stored at given path as {@link Metadata}. Returns null if the content does not exist.
   */
  @Nullable
  public static Metadata read(final Repository repository, final MavenPath mavenPath) throws IOException {
    try {
      final Content content = repository.facet(OrientMavenFacet.class).get(mavenPath);
      if (content == null) {
        return null;
      }
      else {
        Metadata metadata = MavenModels.readMetadata(content.openInputStream());
        if (metadata == null) {
          log.warn("Corrupted metadata {} @ {}", repository.getName(), mavenPath.getPath());
        }
        return metadata;
      }
    }
    catch (BlobStoreException | MissingBlobException e) {
      if (log.isDebugEnabled()) {
        log.warn(String.format("Found missing blob for metadata path %s, will return null (not found) caused by:",
            mavenPath.getPath()), e);
      }
      else {
        log.warn("Found missing blob for metadata path {}, will return null (not found) caused by {}",
            mavenPath.getPath(), e.getMessage());
      }
      return null;
    }
  }

  /**
   * Writes passed in metadata as XML.
   */
  public static void write(final Repository repository, final MavenPath mavenPath, final Metadata metadata)
      throws IOException
  {
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    MavenModels.writeMetadata(buffer, metadata);
    final byte[] bytes = buffer.toByteArray();
    logGAMetadataBlobIfDebug(mavenPath, bytes);
    mavenFacet.put(mavenPath, new BytesPayload(bytes, MavenMimeRulesSource.METADATA_TYPE));
    final Map<HashAlgorithm, HashCode> hashCodes = mavenFacet.get(mavenPath).getAttributes()
        .require(Content.CONTENT_HASH_CODES_MAP, Content.T_CONTENT_HASH_CODES_MAP);
    checkState(hashCodes != null, "hashCodes");
    for (HashType hashType : HashType.values()) {
      MavenPath checksumPath = mavenPath.hash(hashType);
      HashCode hashCode = hashCodes.get(hashType.getHashAlgorithm());
      checkState(hashCode != null, "hashCode: type=%s", hashType);
      mavenFacet.put(checksumPath, new StringPayload(hashCode.toString(), Constants.CHECKSUM_CONTENT_TYPE));
    }
  }

  private static void logGAMetadataBlobIfDebug(
      final MavenPath mavenPath,
      final byte[] bytes)
  {
    if (log.isDebugEnabled()) {
      final String path = mavenPath.getPath();
      if (mavenPath.getFileName().equals(METADATA_FILENAME)
          && !path.endsWith("-SNAPSHOT/maven-metadata.xml")) {
        log.debug("Attempting to put GA metadata. Path {}, content = {}", path,
            new String(bytes, StandardCharsets.UTF_8));
      }
    }
  }

  /**
   * Deletes metadata.
   */
  public static Set<String> delete(final Repository repository, final MavenPath mavenPath) {
    checkNotNull(repository);
    checkNotNull(mavenPath);
    try {
      return MavenFacetUtils.deleteWithHashes(repository.facet(OrientMavenFacet.class), mavenPath);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *
   * @param repository
   * @param groupId
   * @param artifactId
   * @param baseVersion
   * @return Set of paths that were deleted. If asset was just marked for rebuild it will not be part of this set.
   * @throws IOException
   */
  public static Set<String> deleteAndAddRebuildFlag(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion) throws IOException
  {
    return Transactional.operation.throwing(IOException.class).withDb(repository.facet(StorageFacet.class).txSupplier())
        .call(() -> {
          Set<String> deletedPaths = Sets.newHashSet();
          final StorageTx tx = UnitOfWork.currentTx();
          Bucket bucket = tx.findBucket(repository);
          deletedPaths.addAll(repository.facet(OrientMavenFacet.class).maybeDeleteOrFlagToRebuildMetadata(bucket, groupId, artifactId, baseVersion));
          deletedPaths.addAll(repository.facet(OrientMavenFacet.class).maybeDeleteOrFlagToRebuildMetadata(bucket, groupId, artifactId));
          deletedPaths.addAll(repository.facet(OrientMavenFacet.class).maybeDeleteOrFlagToRebuildMetadata(bucket, groupId));
          return deletedPaths;
        });
  }

  public static void addRebuildFlag(final Asset metadataAsset) {
    metadataAsset.formatAttributes().set(METADATA_REBUILD_KEY, true);
  }

  public static void removeRebuildFlag(final Asset metadataAsset) {
    metadataAsset.formatAttributes().remove(METADATA_REBUILD_KEY);
  }

  /**
   * Checks whether or not the maven metadata should be rebuilt via its associated {@link Asset}
   *
   * Metadata should be rebuilt if either it has been manually marked via {@link OrientMetadataUtils#addRebuildFlag} or
   * if it has been invalidated via it's {@link CacheInfo}.
   */
  public static boolean requiresRebuild(final Asset metadataAsset) {
    requireNonNull(metadataAsset);

    if (TRUE.equals(metadataAsset.formatAttributes().get(METADATA_REBUILD_KEY, false))) {
      return true;
    }

    CacheInfo cacheInfo = extractFromAsset(metadataAsset);
    return cacheInfo != null && cacheInfo.isInvalidated();
  }
}
