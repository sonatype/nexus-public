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
package org.sonatype.repository.conan.internal.common;

import java.util.List;

import org.sonatype.repository.conan.internal.ConanFacetUtils;

import com.google.common.collect.ImmutableList;

/**
 * @since 3.32
 */
public class ConanBrowseNodeGeneratorHelper
{
  private ConanBrowseNodeGeneratorHelper() {

  }

  public static List<String> assetSegment(final String path) {
    String[] split = path.split("/");
    int fileNameIndex = split.length - 1;
    int packageNameIndex;
    int packagesSegmentIndex;

    if (path.contains(ConanFacetUtils.PACKAGE_SNAPSHOT_IDENTIFIER)) {
      if (ConanFacetUtils.isPackageSnapshot(path)) {
        packageNameIndex = split.length - 1;
        packagesSegmentIndex = split.length - 2;
        return ImmutableList.of(split[packagesSegmentIndex], split[packageNameIndex]);
      }
      else {
        packageNameIndex = split.length - 2;
        packagesSegmentIndex = split.length - 3;
        return ImmutableList.of(split[packagesSegmentIndex], split[packageNameIndex], split[fileNameIndex]);
      }
    }
    return ImmutableList.of(split[fileNameIndex]);
  }
}
