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

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Constants;

/**
 * Utility class containing shared general methods for Maven metadata.
 *
 * @since 3.25
 */
public final class MetadataUtils
{
  private MetadataUtils() {
    //no op
  }

  /**
   * Builds a Maven path for the specified metadata.
   */
  public static MavenPath metadataPath(
      final String groupId,
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
}
