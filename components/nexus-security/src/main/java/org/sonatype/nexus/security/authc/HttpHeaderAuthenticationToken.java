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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link AuthenticationToken} with a principal extracted from an HTTP header.
 *
 * The principal is equal to HTTP header value.
 *
 * @since 2.7
 */
public class HttpHeaderAuthenticationToken
    implements HostAuthenticationToken
{
  private final String headerName;

  private final String headerValue;

  private final String host;

  public HttpHeaderAuthenticationToken(final String headerName, final String headerValue, final String host) {
    this.headerName = checkNotNull(headerName);
    this.headerValue = checkNotNull(headerValue);
    this.host = host;
  }

  @Override
  public String getPrincipal() {
    return headerValue;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public String getHost() {
    return host;
  }

  public String getHeaderName() {
    return headerName;
  }

  public String getHeaderValue() {
    return headerValue;
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
