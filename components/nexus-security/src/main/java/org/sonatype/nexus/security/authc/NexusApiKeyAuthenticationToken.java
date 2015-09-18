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
package org.sonatype.nexus.security.authc;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.HostAuthenticationToken;

/**
 * {@link AuthenticationToken} that contains credentials from a known API-Key.
 */
public class NexusApiKeyAuthenticationToken
    implements HostAuthenticationToken
{
  private Object principal;

  private final char[] credentials;

  private final String host;

  public NexusApiKeyAuthenticationToken(final Object principal, final char[] credentials, final String host) {
    this.principal = principal;
    this.credentials = credentials;
    this.host = host;
  }

  public Object getPrincipal() {
    return principal;
  }

  public Object getCredentials() {
    return credentials;
  }

  public String getHost() {
    return host;
  }

  /**
   * Assigns a new account identity to the current authentication token.
   */
  public void setPrincipal(final Object principal) {
    this.principal = principal;
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder(getClass().getName());
    buf.append(" - ").append(getPrincipal());
    if (host != null) {
      buf.append(" (").append(host).append(")");
    }
    return buf.toString();
  }
}
