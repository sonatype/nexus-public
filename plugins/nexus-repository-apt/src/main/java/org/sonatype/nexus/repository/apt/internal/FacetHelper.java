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

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.apt.AptFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.ContentSpecifier;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.apt.internal.PackageName.PACKAGES;
import static org.sonatype.nexus.repository.apt.internal.PackageName.PACKAGES_BZ2;
import static org.sonatype.nexus.repository.apt.internal.PackageName.PACKAGES_GZ;
import static org.sonatype.nexus.repository.apt.internal.PackageName.PACKAGES_XZ;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.*;

/**
 * @since 3.next
 */
public class FacetHelper
{
  public static final List<HashAlgorithm> hashAlgorithms = Arrays.asList(MD5, SHA1, SHA256);

  private static final String RELEASE_PATH = "dists/%s/%s";

  private static final String PACKAGE_PATH = "dists/%s/%s/binary-%s/%s";

  private static final String ASSET_PATH = "pool/%s/%s/%s";

  public static List<ContentSpecifier> getReleaseIndexSpecifiers(AptFacet facet) {
    if (facet.isFlat()) {
      return Arrays.asList(
          new ContentSpecifier(RELEASE, SnapshotItem.Role.RELEASE_INDEX),
          new ContentSpecifier(RELEASE_GPG, SnapshotItem.Role.RELEASE_SIG),
          new ContentSpecifier(INRELEASE, SnapshotItem.Role.RELEASE_INLINE_INDEX));
    }
    else {
      String dist = facet.getDistribution();

      return Arrays.asList(
          new ContentSpecifier(String.format(RELEASE_PATH, dist, RELEASE), SnapshotItem.Role.RELEASE_INDEX),
          new ContentSpecifier(String.format(RELEASE_PATH, dist, RELEASE_GPG), SnapshotItem.Role.RELEASE_SIG),
          new ContentSpecifier(String.format(RELEASE_PATH, dist, INRELEASE), SnapshotItem.Role.RELEASE_INLINE_INDEX));
    }
  }

  public static List<ContentSpecifier> getReleasePackageIndexes(AptFacet facet, String component, String arch) {
    if (facet.isFlat()) {
      return Arrays.asList(
          new ContentSpecifier(PACKAGES, SnapshotItem.Role.PACKAGE_INDEX_RAW),
          new ContentSpecifier(PACKAGES_GZ, SnapshotItem.Role.PACKAGE_INDEX_GZ),
          new ContentSpecifier(PACKAGES_BZ2, SnapshotItem.Role.PACKAGE_INDEX_BZ2),
          new ContentSpecifier(PACKAGES_XZ, SnapshotItem.Role.PACKAGE_INDEX_XZ));
    }
    else {
      String dist = facet.getDistribution();

      return Arrays.asList(
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

  public static Content toContent(final Asset asset, final Blob blob) {
    final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, hashAlgorithms, content.getAttributes());
    return content;
  }

  public static String buildAssetName(String packageName, String version, String architecture) {
    return packageName + "_" + version + "_" + architecture + ".deb";
  }

  public static String buildAssetPath(final String packageName, final String version, final String architecture) {
    return String.format(ASSET_PATH, packageName.substring(0, 1), packageName,
        buildAssetName(packageName, version, architecture));
  }

  private FacetHelper() {
    //empty
  }
}
