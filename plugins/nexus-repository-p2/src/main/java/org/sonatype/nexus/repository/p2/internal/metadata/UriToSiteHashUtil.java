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
package org.sonatype.nexus.repository.p2.internal.metadata;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hashing;

/**
 * Normalizes and maps URIs to a hash.
 *
 * @since 3.28
 */
public class UriToSiteHashUtil
{
  private UriToSiteHashUtil() {
    // no instance allowed
  }

  public static String map(final String url) {
    return Hashing.sha256().hashString(url.endsWith("/") ? url : url + '/', StandardCharsets.UTF_8).toString();
  }

  public static String map(final URI url) {
    return map(url.toString());
  }
}
