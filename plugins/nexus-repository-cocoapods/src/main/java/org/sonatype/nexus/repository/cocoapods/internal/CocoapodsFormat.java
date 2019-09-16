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
package org.sonatype.nexus.repository.cocoapods.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;

/**
 * @since 3.next
 */
@Named(CocoapodsFormat.NAME)
@Singleton
public class CocoapodsFormat
    extends Format
{
  public static final String HTTP_NXRM_PREFIX = "http/";

  public static final String HTTPS_NXRM_PREFIX = "https/";

  public static final String HTTP_URL_PREFIX = "http://";

  public static final String HTTPS_URL_PREFIX = "https://";

  public static final String CDN_METADATA_PREFIX = "cdn_metadata/";

  public static final String NAME = "cocoapods";

  public static final String INVALID_SPEC_FILE_ERROR = "Invalid spec file: %s";

  public CocoapodsFormat() {
    super(NAME);
  }

  public static String escapeUriToPath(final String uri) {
    return uri.replace("://", "/");
  }

  public static String unescapePathToUri(final String path) {
    if (path.startsWith(HTTP_NXRM_PREFIX)) {
      return path.replaceFirst(HTTP_NXRM_PREFIX, HTTP_URL_PREFIX);
    }
    else if (path.startsWith(HTTPS_NXRM_PREFIX)) {
      return path.replaceFirst(HTTPS_NXRM_PREFIX, HTTPS_URL_PREFIX);
    }
    return path;
  }

  public static String removeInitialSlashFromPath(final String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }
}
