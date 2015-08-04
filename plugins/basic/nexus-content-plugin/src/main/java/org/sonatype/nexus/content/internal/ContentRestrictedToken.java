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
package org.sonatype.nexus.content.internal;

import javax.servlet.ServletRequest;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.HostAuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;

import static com.google.common.base.Preconditions.checkNotNull;

// FIXME: Sort out if this should implement RememberMe* muck or not

/**
 * {@link AuthenticationToken} used when access to {code}/content{code} is restricted.
 *
 * @see ContentAuthenticationFilter
 * @since 2.1
 */
public class ContentRestrictedToken
    implements /*RememberMeAuthenticationToken,*/ HostAuthenticationToken
{
  private final Object principal;

  private final char[] credentials;

  //private final boolean rememberMe;

  private final String host;

  private final ServletRequest request;

  public ContentRestrictedToken(final UsernamePasswordToken basis, final ServletRequest request) {
    checkNotNull(basis);
    this.principal = basis.getPrincipal();
    this.credentials = basis.getPassword();
    //this.rememberMe = basis.isRememberMe();
    this.host = basis.getHost();
    this.request = checkNotNull(request);
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  public String getUsername() {
    return principal != null ? principal.toString() : null;
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  //@Override
  //public boolean isRememberMe()
  //{
  //    return rememberMe;
  //}

  @Override
  public String getHost() {
    return host;
  }

  // NOTE: For now just expose the raw request, may want to limit what information/operations are exposed in the future

  public ServletRequest getRequest() {
    return request;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "principal=" + principal +
        //", rememberMe=" + rememberMe +
        ", host='" + host + '\'' +
        '}';
  }
}
