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
package org.sonatype.nexus.repository.p2.orient.upgrade;

/**
 * @since 3.28
 */
public class LegacyPathUtil
{
  private LegacyPathUtil() {
    // instance not allowed
  }

  private static final String HTTP_NXRM_PREFIX = "http/";

  private static final String HTTPS_NXRM_PREFIX = "https/";

  private static final String HTTP_URL_PREFIX = "http://";

  private static final String HTTPS_URL_PREFIX = "https://";

  /**
   * Convert a legacy URI encoded path back to a URI
   */
  public static String unescapePathToUri(final String path) {
    String resultPath = path;
    if (path.startsWith(HTTP_NXRM_PREFIX)) {
      resultPath = path.replaceFirst(HTTP_NXRM_PREFIX, HTTP_URL_PREFIX);
    }
    else if (path.startsWith(HTTPS_NXRM_PREFIX)) {
      resultPath = path.replaceFirst(HTTPS_NXRM_PREFIX, HTTPS_URL_PREFIX);
    }
    return resultPath;
  }
}
