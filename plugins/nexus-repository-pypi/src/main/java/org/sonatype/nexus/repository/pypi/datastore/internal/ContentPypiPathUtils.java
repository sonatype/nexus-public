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
package org.sonatype.nexus.repository.pypi.datastore.internal;

/**
 * @since 3.29
 */
public class ContentPypiPathUtils
{
  private static final String INDEX_PATH_PREFIX = "/simple/";

  private static final String PACKAGE_PATH_PREFIX = "/packages/";

  private ContentPypiPathUtils() {}

  /**
   * Builds a path to a package for a particular path.
   */
  public static String packagesPath(final String... parts) {
    String pkg = String.join("/", parts);

    return pkg.startsWith(PACKAGE_PATH_PREFIX) ? pkg : PACKAGE_PATH_PREFIX + pkg;
  }

  /**
   * Builds a path to the simple index.
   */
  public static String indexPath() {
    return INDEX_PATH_PREFIX;
  }

  /**
   * Builds a path to the simple index for a particular name.
   */
  public static String indexPath(final String name) {
    return INDEX_PATH_PREFIX + name + (name.endsWith("/") ? "" : "/");
  }

  /**
   * Builds a URI to the simple index.
   */
  public static String indexUriPath() {
    return INDEX_PATH_PREFIX.substring(1);
  }

  /**
   * Builds a URI to the simple index for a particular name.
   */
  public static String indexUriPath(final String name) {
    return buildIndexPath(name).substring(1);
  }

  private static String buildIndexPath(final String name) {
    return INDEX_PATH_PREFIX + name + (name.endsWith("/") ? "" : "/");
  }
}
