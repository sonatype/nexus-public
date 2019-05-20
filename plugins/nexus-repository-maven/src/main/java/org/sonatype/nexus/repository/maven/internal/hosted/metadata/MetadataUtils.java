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
package org.sonatype.nexus.repository.maven.internal.hosted.metadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.MavenFacetUtils;
import org.sonatype.nexus.repository.maven.internal.MavenMimeRulesSource;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.hash.HashCode;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Utility class containing shared methods for Maven metadata.
 *
 * @since 3.0
 */
public final class MetadataUtils
{
  private static Logger log = LoggerFactory.getLogger(MetadataUtils.class);

  private MetadataUtils() {
  }

  /**
   * Builds a Maven path for the specified metadata.
   */
  public static MavenPath metadataPath(final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(groupId.replace('.', '/'));
    if (artifactId != null) {
      sb.append("/").append(artifactId);
      if (baseVersion != null) {
        sb.append("/").append(baseVersion);
      }
    }
    sb.append("/").append(Constants.METADATA_FILENAME);
    return new MavenPath(sb.toString(), null);
  }

  /**
   * True if content is available for a given path, false otherwise
   */
  public static boolean exists(final Repository repository, final MavenPath mavenPath) throws IOException {
    return repository.facet(MavenFacet.class).get(mavenPath) != null;
  }

  /**
   * Reads content stored at given path as {@link Metadata}. Returns null if the content does not exist.
   */
  @Nullable
  public static Metadata read(final Repository repository, final MavenPath mavenPath) throws IOException {
    final Content content = repository.facet(MavenFacet.class).get(mavenPath);
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

  /**
   * Writes passed in metadata as XML.
   */
  public static void write(final Repository repository, final MavenPath mavenPath, final Metadata metadata)
      throws IOException
  {
    MavenFacet mavenFacet = repository.facet(MavenFacet.class);
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    MavenModels.writeMetadata(buffer, metadata);
    mavenFacet.put(mavenPath, new BytesPayload(buffer.toByteArray(),
        MavenMimeRulesSource.METADATA_TYPE));
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

  /**
   * Deletes metadata.
   */
  public static void delete(final Repository repository, final MavenPath mavenPath) {
    checkNotNull(repository);
    checkNotNull(mavenPath);
    try {
      MavenFacetUtils.deleteWithHashes(repository.facet(MavenFacet.class), mavenPath);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
