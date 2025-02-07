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
package org.sonatype.nexus.repository.maven.internal;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPath.SignatureType;
import org.sonatype.nexus.repository.maven.MavenPathParser;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven 2 path parser.
 *
 * @since 3.0
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2MavenPathParser
    extends ComponentSupport
    implements MavenPathParser
{
  private static final String TAR_EXT_PREFIX = ".tar";

  private static final String CPIO_EXT_PREFIX = ".cpio";

  // The extension supported for Coca-Cola
  // https://issues.sonatype.org/browse/NEXUS-24098
  private static final String NK_OS_EXT = ".nk.os";

  @Nonnull
  @Override
  public MavenPath parsePath(final String path) {
    return parsePath(path, true);
  }

  @Nonnull
  @Override
  public MavenPath parsePath(final String path, final boolean caseSensitive) {
    checkNotNull(path);
    String pathWithoutLeadingSlash = path;
    if (path.startsWith("/")) {
      pathWithoutLeadingSlash = path.substring(1);
    }
    final Coordinates coordinates = maven2LayoutedPathToCoordinates(pathWithoutLeadingSlash, caseSensitive);
    return new MavenPath(pathWithoutLeadingSlash, coordinates);
  }

  @Override
  public boolean isRepositoryMetadata(final MavenPath path) {
    return path.main().getFileName().equals(Constants.METADATA_FILENAME);
  }

  @Override
  public boolean isRepositoryIndex(final MavenPath path) {
    return path.getPath().equals(Constants.INDEX_MAIN_CHUNK_FILE_PATH) ||
        path.getPath().equals(Constants.INDEX_PROPERTY_FILE_PATH);
  }

  /**
   * Tries to parse a path according to Maven2 layout spec, and extract the {@link Coordinates} out of it, if possible.
   * If path does not obeys Maven2 layout or is not an artifact path, {@code null} is returned.
   */
  @Nullable
  private Coordinates maven2LayoutedPathToCoordinates(final String pathString, final boolean caseSensitive) {
    String str = pathString;
    try {
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

      for (HashType hashType : HashType.values()) {
        if (str.endsWith("." + hashType.getExt())) {
          extSuffix.insert(0, "." + hashType.getExt());
          str = str.substring(0, str.length() - (hashType.getExt().length() + 1));
          break;
        }
      }

      if (str.endsWith(Constants.METADATA_FILENAME)) {
        return null;
      }

      String version = baseVersion;
      Long timestamp = null;
      Integer buildNumber = null;
      String tail = null;
      if (snapshot) {
        int vSnapshotStart =
            artifactId.length() + 1 + baseVersion.length() - Constants.SNAPSHOT_VERSION_SUFFIX.length();
        version = str.substring(vSnapshotStart, vSnapshotStart + Constants.SNAPSHOT_VERSION_SUFFIX.length());

        if (Constants.SNAPSHOT_VERSION_SUFFIX.equals(version)) {
          int vTimestampStart = vSnapshotStart + version.length() + 1;

          //this would be expected in most cases
          version = baseVersion; // reset it
          tail = str.substring(artifactId.length() + baseVersion.length() + 1);

          //check if we have something hokey like SNAPSHOT-20180101.121212
          if (str.length() > vTimestampStart + Constants.DOTTED_TIMESTAMP_VERSION_FORMAT.length()) {
            try { //NOSONAR not extracting to method as many variables external to the method need to be updated
              Constants.METADATA_DOTTED_TIMESTAMP.parseDateTime(
                  str.substring(vTimestampStart, vTimestampStart + Constants.DOTTED_TIMESTAMP_VERSION_FORMAT.length()))
                  .getMillis();
              version = str.substring(vTimestampStart, vTimestampStart + Constants.SNAPSHOT_VERSION_SUFFIX.length());
              vSnapshotStart = vTimestampStart;
              tail = null;
            }
            catch (IllegalArgumentException e) { //NOSONAR
              //usually expected
            }
          }
        }

        if (tail == null) {
          final StringBuilder snapshotTimestampedVersion = new StringBuilder(version);
          snapshotTimestampedVersion.append(str.substring(vSnapshotStart + version.length(),
              vSnapshotStart + version.length() + Constants.SNAPSHOT_VERSION_SUFFIX.length() - 1));

          try {
            timestamp = Constants.METADATA_DOTTED_TIMESTAMP.parseDateTime(
                snapshotTimestampedVersion.toString()).getMillis();
          }
          catch (IllegalArgumentException e) {
            log.trace("metadata dotted timestamp failed parsing to millis {}", snapshotTimestampedVersion.toString());
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
          try {
            buildNumber = Integer.parseInt(bnr.toString());
          }
          catch (NumberFormatException e) {
            log.trace("build number failed parsing {}", bnr);
          }
          tail = str.substring(vSnapshotStart + snapshotTimestampedVersion.length());
          version = baseVersion.substring(0, baseVersion.length() - Constants.SNAPSHOT_VERSION_SUFFIX.length())
              + snapshotTimestampedVersion;
        }
      }
      else {
        String fileNameStr = fileName;
        String artifactStr = artifactId + "-" + baseVersion;

        if (!caseSensitive) {
          fileNameStr = fileNameStr.toLowerCase(Locale.ROOT);
          artifactStr = artifactStr.toLowerCase(Locale.ROOT);
        }

        if (!fileNameStr.startsWith(artifactStr) || "-.".indexOf(fileNameStr.charAt(artifactStr.length())) == -1) {
          // The path does not represent an artifact (filename does not match artifactId-version[-.])!
          return null;
        }

        int nTailPos = artifactId.length() + baseVersion.length() + 1;
        tail = str.substring(nTailPos);
      }

      int nExtPos = getExceptionPos(tail);
      if (nExtPos == -1) {
        // NX-563: not allowing extensionless paths to be interpreted as artifact
        return null;
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

  private int getExceptionPos(final String tail) {
    int nExtPos = tail.lastIndexOf('.');
    if (nExtPos == -1) {
      return -1;
    }

    String tailWithoutExt = tail.substring(0, nExtPos);

    if (tailWithoutExt.endsWith(TAR_EXT_PREFIX)) {
      nExtPos -= TAR_EXT_PREFIX.length();
    }
    else if (tailWithoutExt.endsWith(CPIO_EXT_PREFIX)) {
      nExtPos -= CPIO_EXT_PREFIX.length();
    }
    else if (tail.endsWith(NK_OS_EXT)){
      nExtPos = tail.length() - NK_OS_EXT.length();
    }

    return nExtPos;
  }
}
