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
package org.sonatype.nexus.bootstrap.jetty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DockerSubdomainRepositoryMapping
{
  private static final Map<String, String> map = new ConcurrentHashMap<>();

  private DockerSubdomainRepositoryMapping() {
    // empty
  }

  public static String get(final String hostHeader) {
    int subdomainLength = hostHeader.indexOf(".");
    if (subdomainLength != -1) {
      String subdomain = hostHeader.substring(0, subdomainLength);
      return map.get(subdomain);
    }
    return null;
  }

  public static void put(final String subdomain, final String repositoryName) {
    map.put(subdomain, repositoryName);
  }

  public static void remove(final String subdomain) {
    map.remove(subdomain);
  }
}
