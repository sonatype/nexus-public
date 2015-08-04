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

import org.sonatype.nexus.client.internal.util.Check;

/**
 * @since 2.1
 */
public class ProxyInfo
{

  private final Protocol proxyProtocol;

  private final String proxyHost;

  private final int proxyPort;

  private final AuthenticationInfo proxyAuthentication;

  public ProxyInfo(final Protocol proxyProtocol, final String proxyHost, final int proxyPort,
                   final AuthenticationInfo proxyAuthentication)
  {
    this.proxyProtocol = Check.notNull(proxyProtocol, Protocol.class);
    this.proxyHost = Check.notBlank(proxyHost, "proxyHost");
    this.proxyPort =
        Check.argument(proxyPort > 0 && proxyPort < 65536, proxyPort,
            "proxyPort out of boundaries (0 < proxyPort < 65536)!");
    this.proxyAuthentication = proxyAuthentication;
  }

  public Protocol getProxyProtocol() {
    return proxyProtocol;
  }

  public String getProxyHost() {
    return proxyHost;
  }

  public int getProxyPort() {
    return proxyPort;
  }

  public AuthenticationInfo getProxyAuthentication() {
    return proxyAuthentication;
  }

  @Override
  public String toString() {
    return "ProxyInfo[proxyProtocol=" + proxyProtocol + ", proxyHost=" + proxyHost + ", proxyPort=" + proxyPort
        + ", proxyAuthentication=" + proxyAuthentication + "]";
  }

}
