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
package org.sonatype.repository.conan.internal;

import java.util.List;

import org.sonatype.nexus.common.hash.HashAlgorithm;

import com.google.common.collect.ImmutableList;

import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;

/**
 * @since 3.32
 */
public class ConanFacetUtils
{
  public static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(SHA1, SHA256);

  public static final String PACKAGE_SNAPSHOT_IDENTIFIER = "packages";

  private ConanFacetUtils() {
    // nop, utility class
  }

  public static boolean isPackageSnapshot(String path) {
    String[] args = path.split("/");
    if (args.length > 2) {
      String expectPackage = args[args.length - 2];
      return PACKAGE_SNAPSHOT_IDENTIFIER.equals(expectPackage);
    }
    else {
      return false;
    }
  }

  public static boolean isDigest(String path) {
    return path.endsWith(AssetKind.DIGEST.getFilename());
  }
}
