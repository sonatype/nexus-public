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
package org.sonatype.nexus.orient.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.hash.HashCode;

/**
 * Maven facet, present on all Maven repositories.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface OrientMavenFacet
    extends Facet, MavenFacet
{
  // HTTP operations

  @Nullable
  Content get(MavenPath path) throws IOException;

  Content put(MavenPath path, Payload payload) throws IOException;

  Content put(MavenPath path,
              Path sourceFile,
              String contentType,
              AttributesMap contentAttributes,
              Map<HashAlgorithm, HashCode> hashes,
              long size) throws IOException;

  /**
   * Puts an artifact held in a temporary blob.
   * @since 3.1
   */
  Content put(MavenPath path, TempBlob blob, String contentType, AttributesMap contentAttributes) throws IOException;

  Set<String> delete(MavenPath... paths) throws IOException;

  Set<String> deleteAssetOnly(MavenPath... paths) throws IOException;

  /**
   * @since 3.4
   */
  Asset put(MavenPath path, AssetBlob assetBlob, AttributesMap contentAttributes) throws IOException;

  /**
   * @since 3.27
   * @param the tuple of group Id, artifact Id, and base Version
   * @return paths there were deleted
   */
  Set<String> maybeDeleteOrFlagToRebuildMetadata(
      Bucket bucket,
      String groupId,
      String artifactId,
      String baseVersion) throws IOException;

  /**
   * @since 3.27
   * @param the tuple of group Id, and artifact Id
   * @return paths there were deleted
   */
  default Set<String> maybeDeleteOrFlagToRebuildMetadata(
      final Bucket bucket,
      final String groupId,
      final String artifactId) throws IOException
  {
    return maybeDeleteOrFlagToRebuildMetadata(bucket, groupId, artifactId, null);
  }

  /**
   * @since 3.27
   * @param the group Id
   * @return paths there were deleted
   */
  default Set<String> maybeDeleteOrFlagToRebuildMetadata(final Bucket bucket, final String groupId)
      throws IOException{
    return maybeDeleteOrFlagToRebuildMetadata(bucket, groupId, null, null);
  }
}
