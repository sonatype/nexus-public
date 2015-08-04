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
package org.sonatype.nexus.client.rest;

import java.net.MalformedURLException;
import java.net.URL;

import org.sonatype.nexus.client.internal.util.Check;

/**
 * @since 2.1
 */
public class BaseUrl
{

  private final Protocol protocol;

  private final String host;

  private final int port;

  private final String path;

  public BaseUrl(final Protocol protocol, final String host, final int port, final String path) {
    this.protocol = Check.notNull(protocol, Protocol.class);
    this.host = Check.notBlank(host, "host");
    this.port = Check.argument(port > 0 && port < 65536, port, "Port out of boundaries (0 < port < 65536)!");
    String fixedPath = path;
    if (!fixedPath.endsWith("/")) {
      fixedPath = fixedPath + "/";
    }
    this.path = fixedPath;
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getPath() {
    return path;
  }

  // ==

  public String toUrl() {
    return getProtocol().name().toLowerCase() + "://" + getHost() + ":" + getPort() + getPath();
  }

  // ==

  @Override
  public String toString() {
    return toUrl();
  }

  // ==

  public static BaseUrl baseUrlFrom(final String url)
      throws MalformedURLException
  {
    return baseUrlFrom(new URL(Check.notBlank(url, "URL")));
  }

  public static BaseUrl baseUrlFrom(final URL url) {
    Check.notNull(url, URL.class);
    final Protocol protocol = Protocol.valueOf(url.getProtocol().toUpperCase());
    final String host = url.getHost();
    final int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
    String fixedPath = url.getPath();
    if (!fixedPath.startsWith("/")) {
      fixedPath = "/" + fixedPath;
    }
    if (!fixedPath.endsWith("/")) {
      fixedPath = fixedPath + "/";
    }
    return new BaseUrl(protocol, host, port, fixedPath);
  }
}
