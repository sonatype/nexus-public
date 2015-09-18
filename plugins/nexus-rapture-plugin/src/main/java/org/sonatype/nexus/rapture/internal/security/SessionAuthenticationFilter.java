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
package org.sonatype.nexus.rapture.internal.security;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.text.Strings2;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session authentication filter for {@link SessionServlet}.
 *
 * Provides (very) basic {@code x-www-form-urlencoded} authentication support.
 *
 * @since 3.0
 */
@Named
@Singleton
public class SessionAuthenticationFilter
    extends AuthenticatingFilter
{
  private static final Logger log = LoggerFactory.getLogger(SessionAuthenticationFilter.class);

  public static final String NAME = "nx-session-authc";

  public static final String P_USERNAME = "username";

  public static final String P_PASSWORD = "password";

  public static final String P_REMEMBER_ME = "rememberMe";

  public static final String DELETE_METHOD = "DELETE";

  /**
   * Allow if authenticated or if logout request.
   */
  @Override
  protected boolean isAccessAllowed(final ServletRequest request,
                                    final ServletResponse response,
                                    final Object mappedValue)
  {
    Subject subject = getSubject(request, response);
    return subject.isAuthenticated() || isLogoutRequest(request, response);
  }

  /**
   * Fill in denied response.
   */
  private void denied(final ServletResponse response) {
    if (response instanceof HttpServletResponse) {
      // using 403 here as 401 implies use of "WWW-Authenticate" scheme which is not employed here
      WebUtils.toHttp(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  @Override
  protected boolean onAccessDenied(final ServletRequest request, final ServletResponse response) throws Exception {
    boolean authenticated = false;
    if (isLoginRequest(request, response)) {
      log.trace("Attempting authentication");
      authenticated = executeLogin(request, response);
    }

    if (!authenticated) {
      log.trace("Access denied");
      denied(response);
    }

    return authenticated;
  }

  @Override
  protected boolean isLoginRequest(final ServletRequest request, final ServletResponse response) {
    return (request instanceof HttpServletRequest) &&
        WebUtils.toHttp(request).getMethod().equalsIgnoreCase(POST_METHOD);
  }

  private boolean isLogoutRequest(final ServletRequest request, final ServletResponse response) {
    return (request instanceof HttpServletRequest) &&
        WebUtils.toHttp(request).getMethod().equalsIgnoreCase(DELETE_METHOD);
  }

  @Override
  protected boolean isRememberMe(final ServletRequest request) {
    // TODO: Allow feature to be disabled globally by property, probably here is a good place?
    return WebUtils.isTrue(request, P_REMEMBER_ME);
  }

  private String decodeBase64Param(final ServletRequest request, final String name) {
    String encoded = WebUtils.getCleanParam(request, name);
    if (encoded != null) {
      return Strings2.decodeBase64(encoded);
    }
    return null;
  }

  @Override
  protected AuthenticationToken createToken(final ServletRequest request, final ServletResponse response)
      throws Exception
  {
    String username = decodeBase64Param(request, P_USERNAME);
    String password = decodeBase64Param(request, P_PASSWORD);
    return createToken(username, password, request, response);
  }

  @Override
  protected boolean onLoginSuccess(final AuthenticationToken token,
                                   final Subject subject,
                                   final ServletRequest request,
                                   final ServletResponse response)
      throws Exception
  {
    log.debug("Success: token={}, subject={}", token, subject);
    return true;
  }

  @Override
  protected boolean onLoginFailure(final AuthenticationToken token,
                                   final AuthenticationException e,
                                   final ServletRequest request,
                                   final ServletResponse response)
  {
    log.debug("Failure: token={}", token, e);
    denied(response);
    return false;
  }
}
