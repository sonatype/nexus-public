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
package org.sonatype.nexus.repository.cocoapods.internal.pod;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.sonatype.nexus.repository.view.Context;

import static org.sonatype.nexus.repository.cocoapods.internal.CocoapodsFormat.removeInitialSlashFromPath;

/**
 * @since 3.20
 */
public class PodsUtils
{
  public static final String HTTP_NXRM_PREFIX = "http/";

  public static final String HTTPS_NXRM_PREFIX = "https/";

  public static final String HTTP_URL_PREFIX = "http://";

  public static final String HTTPS_URL_PREFIX = "https://";

  private PodsUtils() {}

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

  public static String extractPodPath(final Context context) {
    return attachParametersToPath(
        removeInitialSlashFromPath(context.getRequest().getPath()),
        context.getRequest().getParameters().entries());
  }

  private static String attachParametersToPath(final String path, final Collection<Entry<String, String>> parameters) {
    String paramStr = parameters.stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("&"));
    return StringUtils.isBlank(paramStr) ? path : path + "?" + paramStr;
  }
}
