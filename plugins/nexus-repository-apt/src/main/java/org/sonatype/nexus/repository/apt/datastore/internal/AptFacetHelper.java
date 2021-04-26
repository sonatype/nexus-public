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
package org.sonatype.nexus.repository.apt.datastore.internal;

import java.util.List;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.browse.node.BrowsePath;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;

/**
 * Apt helper class
 *
 * @since 3.next
 */
public class AptFacetHelper
{
  public static final List<HashAlgorithm> hashAlgorithms = ImmutableList.of(MD5, SHA1, SHA256);

  private static final String ASSET_PATH = "pool/%s/%s/%s";

  /**
   * Returns asset name base on the provided parameters
   *
   * @param packageName - Debian package name e.g. 'nano'
   * @param version - Package version e.g. '2.9.3-2'
   * @param architecture - Package architecture e.g. 'amd64'
   * @return - e.g. 'nano_2.9.3-2_amd64.deb'
   */
  public static String buildAssetName(final String packageName, final String version, final String architecture) {
    return packageName + "_" + version + "_" + architecture + ".deb";
  }

  /**
   * Returns asset path with asset name base on the provided parameters
   *
   * @param packageName - Debian package name e.g. 'nano'
   * @param version - Package version e.g. '2.9.3-2'
   * @param architecture - Package architecture e.g. 'amd64'
   * @return - e.g. 'pool/n/nano/nano_2.9.3-2_amd64.deb'
   */
  public static String buildAssetPath(final String packageName, final String version, final String architecture) {
    return String.format(ASSET_PATH, packageName.substring(0, 1), packageName,
        buildAssetName(packageName, version, architecture));
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
