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

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.security.SecurityFilter.ATTR_USER_ID;
import static org.sonatype.nexus.security.SecurityFilter.ATTR_USER_PRINCIPAL;

/**
 * Nexus security filter providing HTTP BASIC authentication support.
 *
 * Knows about special handling needed for anonymous subjects.
 *
 * Does not create sessions.
 *
 * @since 3.0
 */
@Named
@Singleton
public class NexusBasicHttpAuthenticationFilter
    extends BasicHttpAuthenticationFilter
{
  public static final String NAME = "nx-basic-authc";

  protected final Logger log = LoggerFactory.getLogger(getClass());

  public NexusBasicHttpAuthenticationFilter() {
    setApplicationName("Sonatype Nexus");
  }

  /**
   * Always use permissive mode, which is needed for anonymous user support.
   */
  @Override
  protected boolean isPermissive(final Object mappedValue) {
    return true;
  }

  /**
   * Disable session creation for all BASIC auth requests.
   */
  @Override
  public boolean onPreHandle(final ServletRequest request, final ServletResponse response, final Object mappedValue)
      throws Exception
  {
    // Basic auth should never create sessions
    request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);

    return super.onPreHandle(request, response, mappedValue);
  }

  /**
   * Permissive {@link AuthorizationException} 401 and 403 handling.
   */
  @Override
  protected void cleanup(final ServletRequest request, final ServletResponse response, Exception failure)
      throws ServletException, IOException
  {
    // decode target exception
    Throwable cause = failure;
    if (cause instanceof ServletException) {
      cause = cause.getCause();
    }

    // special handling for authz failures due to permissive
    if (cause instanceof AuthorizationException) {
      // clear the failure
      failure = null;

      Subject subject = getSubject(request, response);
      boolean authenticated = subject.getPrincipal() != null && subject.isAuthenticated();

      if (authenticated) {
        // authenticated subject -> 403 forbidden
        WebUtils.toHttp(response).sendError(HttpServletResponse.SC_FORBIDDEN);
      }
      else {
        // unauthenticated subject -> 401 inform to authenticate
        try {
          // TODO: Should we build in browser detecting to avoid sending 401, should that be its own filter?

          onAccessDenied(request, response);
        }
        catch (Exception e) {
          failure = e;
        }
      }
    }

    super.cleanup(request, response, failure);
  }

  @Override
  protected boolean onLoginSuccess(AuthenticationToken token,
                                   Subject subject,
                                   ServletRequest request,
                                   ServletResponse response)
      throws Exception
  {
    if (request instanceof HttpServletRequest) {
      // Prefer the subject principal over the token's, as these could be different for token-based auth
      Object principal = subject.getPrincipal();
      if (principal == null) {
        principal = token.getPrincipal();
      }
      String userId = principal.toString();

      // Attach principal+userId to request so we can use that in the request-log
      request.setAttribute(ATTR_USER_PRINCIPAL, principal);
      request.setAttribute(ATTR_USER_ID, userId);
    }
    return super.onLoginSuccess(token, subject, request, response);
  }
}