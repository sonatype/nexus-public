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
package org.sonatype.nexus.repository.apt.internal;

import java.util.List;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.ContentSpecifier;
import org.sonatype.nexus.repository.browse.node.BrowsePath;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.apt.internal.PackageName.PACKAGES;
import static org.sonatype.nexus.repository.apt.internal.PackageName.PACKAGES_BZ2;
import static org.sonatype.nexus.repository.apt.internal.PackageName.PACKAGES_GZ;
import static org.sonatype.nexus.repository.apt.internal.PackageName.PACKAGES_XZ;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;

/**
 * Apt helper class
 *
 * @since 3.31
 */
public class AptFacetHelper
{
  public static final List<HashAlgorithm> hashAlgorithms = ImmutableList.of(MD5, SHA1, SHA256);

  private static final String RELEASE_PATH = "dists/%s/%s";

  private static final String PACKAGE_PATH = "dists/%s/%s/binary-%s/%s";

  private static final String ASSET_PATH = "pool/%s/%s/%s";

  private static final String MISSED_VALUE_MESSAGE = "Control file doesn't contain '%s' field.";

  private static final String PACKAGE = "Package";

  private static final String VERSION = "Version";

  private static final String ARCHITECTURE = "Architecture";

  /**
   * Returns list of the release indexes specifiers
   *
   * @param isFlat - Type of the repository. Flat repository doesn't have distribution
   * @param dist   - Apt package distribution
   * @return - list of the release indexes specifiers
   */
  public static List<ContentSpecifier> getReleaseIndexSpecifiers(boolean isFlat, final String dist) {
    if (isFlat) {
      return ImmutableList.of(
          new ContentSpecifier(RELEASE, SnapshotItem.Role.RELEASE_INDEX),
          new ContentSpecifier(RELEASE_GPG, SnapshotItem.Role.RELEASE_SIG),
          new ContentSpecifier(INRELEASE, SnapshotItem.Role.RELEASE_INLINE_INDEX));
    }
    else {
      return ImmutableList.of(
          new ContentSpecifier(String.format(RELEASE_PATH, dist, RELEASE), SnapshotItem.Role.RELEASE_INDEX),
          new ContentSpecifier(String.format(RELEASE_PATH, dist, RELEASE_GPG), SnapshotItem.Role.RELEASE_SIG),
          new ContentSpecifier(String.format(RELEASE_PATH, dist, INRELEASE), SnapshotItem.Role.RELEASE_INLINE_INDEX));
    }
  }

  /**
   * Returns list of the release package indexes
   *
   * @param isFlat    - Type of the repository. Flat repository doesn't have distribution
   * @param dist      - Apt package distribution
   * @param component - Apt Component
   * @param arch      - Package type of architecture. e.g. amd64
   * @return - list of the release package indexes
   */
  public static List<ContentSpecifier> getReleasePackageIndexes(boolean isFlat, final String dist,
                                                                final String component,
                                                                final String arch)
  {
    if (isFlat) {
      return ImmutableList.of(
          new ContentSpecifier(PACKAGES, SnapshotItem.Role.PACKAGE_INDEX_RAW),
          new ContentSpecifier(PACKAGES_GZ, SnapshotItem.Role.PACKAGE_INDEX_GZ),
          new ContentSpecifier(PACKAGES_BZ2, SnapshotItem.Role.PACKAGE_INDEX_BZ2),
          new ContentSpecifier(PACKAGES_XZ, SnapshotItem.Role.PACKAGE_INDEX_XZ));
    }
    else {
      return ImmutableList.of(
          new ContentSpecifier(String.format(PACKAGE_PATH, dist, component, arch, PACKAGES),
              SnapshotItem.Role.PACKAGE_INDEX_RAW),
          new ContentSpecifier(String.format(PACKAGE_PATH, dist, component, arch, PACKAGES_GZ),
              SnapshotItem.Role.PACKAGE_INDEX_GZ),
          new ContentSpecifier(String.format(PACKAGE_PATH, dist, component, arch, PACKAGES_BZ2),
              SnapshotItem.Role.PACKAGE_INDEX_BZ2),
          new ContentSpecifier(String.format(PACKAGE_PATH, dist, component, arch, PACKAGES_XZ),
              SnapshotItem.Role.PACKAGE_INDEX_XZ));
    }
  }

  /**
   * Returns asset name base on the provided parameters
   *
   * @param packageName  - Debian package name e.g. 'nano'
   * @param version      - Package version e.g. '2.9.3-2'
   * @param architecture - Package architecture e.g. 'amd64'
   * @return - e.g. 'nano_2.9.3-2_amd64.deb'
   */
  public static String buildAssetName(final String packageName, final String version, final String architecture) {
    return packageName + "_" + version + "_" + architecture + ".deb";
  }

  /**
   * Returns asset path with asset name base on the provided parameters
   *
   * @param packageName  - Debian package name e.g. 'nano'
   * @param version      - Package version e.g. '2.9.3-2'
   * @param architecture - Package architecture e.g. 'amd64'
   * @return - e.g. 'pool/n/nano/nano_2.9.3-2_amd64.deb'
   */
  public static String buildAssetPath(final String packageName, final String version, final String architecture) {
    return String.format(ASSET_PATH, packageName.substring(0, 1), packageName,
        buildAssetName(packageName, version, architecture));
  }

  /**
   * Returns asset path with asset name base on the provided parameters
   *
   * @param controlFile - is a representation of Debian control file content.
   * @return - e.g. 'pool/n/nano/nano_2.9.3-2_amd64.deb'
   */
  public static String buildAssetPath(final ControlFile controlFile) {
    String name = getValueFromControlFile(controlFile, PACKAGE);
    String version = getValueFromControlFile(controlFile, VERSION);
    String architecture = getValueFromControlFile(controlFile, ARCHITECTURE);
    return buildAssetPath(name, version, architecture);
  }

  private static String getValueFromControlFile(final ControlFile controlFile, final String fieldName) {
    return controlFile.getField(fieldName).map(f -> f.value)
        .orElseThrow(() -> new IllegalStateException(String.format(MISSED_VALUE_MESSAGE, fieldName)));
  }

  /**
   * Returns path with appended string on the beginning
   *
   * @param path - Any path e.g. 'some/path/example'
   * @return - e.g. '/some/path/example'
   */
  public static String normalizeAssetPath(String path) {
    return StringUtils.prependIfMissing(path, BrowsePath.SLASH);
  }

  private AptFacetHelper() {
    //empty
  }
}
