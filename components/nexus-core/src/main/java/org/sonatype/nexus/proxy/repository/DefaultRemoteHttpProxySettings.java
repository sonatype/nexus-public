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
package org.sonatype.nexus.proxy.repository;

import org.codehaus.plexus.util.StringUtils;

/**
 * Default {@link RemoteHttpProxySettings} implementation.
 *
 * @since 2.6
 */
public class DefaultRemoteHttpProxySettings
    implements RemoteHttpProxySettings
{

  private String hostname;

  private int port;

  private RemoteAuthenticationSettings proxyAuthentication;

  public boolean isEnabled() {
    return StringUtils.isNotBlank(getHostname()) && getPort() != 0;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public RemoteAuthenticationSettings getProxyAuthentication() {
    return proxyAuthentication;
  }

  public void setProxyAuthentication(RemoteAuthenticationSettings proxyAuthentication) {
    this.proxyAuthentication = proxyAuthentication;
  }

}
