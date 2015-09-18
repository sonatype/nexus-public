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
package org.sonatype.nexus.repository.maven.internal.maven2;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPath.SignatureType;
import org.sonatype.nexus.repository.maven.MavenPathParser;

/**
 * Maven 2 path parser.
 *
 * @since 3.0
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2MavenPathParser
    implements MavenPathParser
{
  @Override
  public MavenPath parsePath(final String path) {
    final Coordinates coordinates = maven2LayoutedPathToCoordinates(path);
    return new MavenPath(path, coordinates);
  }

  @Override
  public boolean isRepositoryMetadata(final MavenPath path) {
    return path.main().getFileName().equals(Constants.METADATA_FILENAME);
  }

  /**
   * Tries to parse a path according to Maven2 layout spec, and extract the {@link Coordinates} out of it, if possible.
   * If path does not obeys Maven2 layout or is not an artifact path, {@code null} is returned.
   */
  @Nullable
  private Coordinates maven2LayoutedPathToCoordinates(String str) {
    try {
      str = str.startsWith("/") ? str.substring(1) : str;

      int vEndPos = str.lastIndexOf('/');
      if (vEndPos == -1) {
        return null;
      }

      int aEndPos = str.lastIndexOf('/', vEndPos - 1);
      if (aEndPos == -1) {
        return null;
      }

      int gEndPos = str.lastIndexOf('/', aEndPos - 1);
      if (gEndPos == -1) {
        return null;
      }

      final String groupId = str.substring(0, gEndPos).replace('/', '.');
      final String artifactId = str.substring(gEndPos + 1, aEndPos);
      final String baseVersion = str.substring(aEndPos + 1, vEndPos);
      final boolean snapshot = baseVersion.endsWith(Constants.SNAPSHOT_VERSION_SUFFIX);
      final String fileName = str.substring(vEndPos + 1);
      str = fileName;

      StringBuilder extSuffix = new StringBuilder();
      SignatureType signatureType = null;
      for (HashType hashType : HashType.values()) {
        if (str.endsWith("." + hashType.getExt())) {
          extSuffix.insert(0, "." + hashType.getExt());
          str = str.substring(0, str.length() - (hashType.getExt().length() + 1));
          break;
        }
      }

      for (SignatureType sType : SignatureType.values()) {
        if (str.endsWith("." + sType.getExt())) {
          extSuffix.insert(0, "." + sType.getExt());
          str = str.substring(0, str.length() - (sType.getExt().length() + 1));
          signatureType = sType;
        }
      }

      if (str.endsWith(Constants.METADATA_FILENAME)) {
        return null;
      }

      String version = baseVersion;
      Long timestamp = null;
      Integer buildNumber = null;
      String tail;
      if (snapshot) {
        int vSnapshotStart = artifactId.length() + baseVersion.length() - 10 + 3;
        version = str.substring(vSnapshotStart, vSnapshotStart + 8);
        if (Constants.SNAPSHOT_VERSION_SUFFIX.equals(version)) {
          version = baseVersion; // reset it
          int nTailPos = artifactId.length() + baseVersion.length() + 1;
          tail = str.substring(nTailPos);
        }
        else {
          final StringBuilder snapshotTimestampedVersion = new StringBuilder(version);
          snapshotTimestampedVersion.append(
              str.substring(vSnapshotStart + version.length(), vSnapshotStart + version.length() + 7)
          );

          try {
            timestamp = Constants.METADATA_DOTTED_TIMESTAMP.parseDateTime(
                snapshotTimestampedVersion.toString()).getMillis();
          }
          catch (IllegalArgumentException e) {
            // skip it
          }

          // add the dash between timestamp and buildNo
          snapshotTimestampedVersion.append('-');

          int buildNumberPos = vSnapshotStart + snapshotTimestampedVersion.length();
          final StringBuilder bnr = new StringBuilder();
          while (str.charAt(buildNumberPos) >= '0' && str.charAt(buildNumberPos) <= '9') {
            snapshotTimestampedVersion.append(str.charAt(buildNumberPos));
            bnr.append(str.charAt(buildNumberPos));
            buildNumberPos++;
          }
          if (bnr.length() == 0) {
            return null;
          }
          try {
            buildNumber = Integer.parseInt(bnr.toString());
          }
          catch (NumberFormatException e) {
            // skip it
          }
          int n = baseVersion.length() > 8 ? baseVersion.length() - 8 : 0;
          tail = str.substring(artifactId.length() + n + snapshotTimestampedVersion.length() + 1);
          version = baseVersion.substring(0, baseVersion.length() - 8) + snapshotTimestampedVersion;
        }
      }
      else {
        if (!fileName.startsWith(artifactId + "-" + baseVersion + ".")
            && !fileName.startsWith(artifactId + "-" + baseVersion + "-")) {
          // The path does not represents an artifact (filename does not match artifactId-version)!
          return null;
        }
        int nTailPos = artifactId.length() + baseVersion.length() + 1;
        tail = str.substring(nTailPos);
      }

      int nExtPos = tail.lastIndexOf('.');
      if (nExtPos == -1) {
        // NX-563: not allowing extensionless paths to be interpreted as artifact
        return null;
      }
      if (tail.endsWith(".tar.gz")) {
        nExtPos = nExtPos - 4;
      }

      final String ext = tail.substring(nExtPos + 1);
      final String classifier = tail.charAt(0) == '-' ? tail.substring(1, nExtPos) : null;

      return new Coordinates(
          snapshot,
          groupId,
          artifactId,
          version,
          timestamp,
          buildNumber,
          baseVersion,
          classifier,
          ext + extSuffix,
          signatureType
      );
    }
    catch (StringIndexOutOfBoundsException e) {
      return null;
    }
  }
}
