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
package org.sonatype.nexus.repository.r.internal.util;

import java.util.regex.Pattern;

import org.sonatype.nexus.repository.r.AssetKind;
import org.sonatype.nexus.repository.view.Context;

import org.apache.commons.lang3.StringUtils;

import static org.sonatype.nexus.repository.r.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.r.AssetKind.RDS_METADATA;

/**
 * Utility methods for working with R routes and paths.
 *
 * @since 3.28
 */
public final class RPathUtils
{
  public static final Pattern PATTERN_PACKAGES_EXTENSION = Pattern.compile(".*PACKAGES.*");

  public static final Pattern PATTERN_METADATA_RDS_EXTENSION = Pattern.compile(".+.rds");

  public static final Pattern PATTERN_ARCHIVE_EXTENSION = Pattern.compile(".+(.zip|.tgz|.tar.gz)");

  public static final Pattern PATTERN_PATH = Pattern.compile(".+/.+/.+");

  public static final String PACKAGES_GZ_FILENAME = "PACKAGES.gz";

  /**
   * Builds a path to an asset for a particular path and filename.
   */
  public static String buildPath(final String path, final String filename) {
    return StringUtils.appendIfMissing(path, "/") + filename;
  }

  /**
   * Extracts full path from {@link Context}
   */
  public static String extractRequestPath(final Context context) {
    return removeInitialSlashFromPath(context.getRequest().getPath());
  }

  /**
   * Removes slash if path starts with it
   */
  public static String removeInitialSlashFromPath(final String path) {
    return StringUtils.stripStart(path, "/");
  }

  /**
   * Returns base path of the package (without filename)
   */
  public static String getBasePath(final String path) {
    final int pathEndCharIndex = StringUtils.stripEnd(path, "/").lastIndexOf('/');
    return path.substring(0, pathEndCharIndex);
  }

  /**
   * Determines asset kind by it's path
   */
  public static AssetKind getAssetKind(final String path) {
    AssetKind assetKind = ARCHIVE;
    if (PATTERN_PACKAGES_EXTENSION.matcher(path).matches()) {
      assetKind = PACKAGES;
    }
    else if (PATTERN_METADATA_RDS_EXTENSION.matcher(path).matches()) {
      assetKind = RDS_METADATA;
    }

    return assetKind;
  }

  /**
   * Determines if it's a valid extension for archive
   */
  public static boolean isValidArchiveExtension(final String nameWithExtension) {
    return PATTERN_ARCHIVE_EXTENSION.matcher(nameWithExtension).matches();
  }

  /**
   * Determines if it's a valid repository path
   */
  public static boolean isValidRepoPath(final String path) {
    return PATTERN_PATH.matcher(path).matches();
  }

  private RPathUtils() {
    // empty
  }
}
