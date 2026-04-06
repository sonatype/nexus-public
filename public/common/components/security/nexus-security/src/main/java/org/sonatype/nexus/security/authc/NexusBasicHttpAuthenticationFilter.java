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

import jakarta.inject.Singleton;

import org.sonatype.nexus.datastore.api.DataAccessException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationException;
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
import org.springframework.stereotype.Component;

/**
 * Nexus security filter providing HTTP BASIC authentication support.
 *
 * Knows about special handling needed for anonymous subjects.
 *
 * Does not create sessions.
 *
 * @since 3.0
 */
@WebFilter(filterName = NexusBasicHttpAuthenticationFilter.NAME)
@Component
@Singleton
public class NexusBasicHttpAuthenticationFilter
    extends BasicHttpAuthenticationFilter
{
  public static final String NAME = "nx-basic-authc";

  // This is the base64 encoded version of ":" i.e. empty credentials, required to allow anonymous v1 Docker search.
  private static final String EMPTY_CREDENTIALS = "Og==";

  /**
   * @since 3.1
   */
  public static final String BASIC_AUTH_REALM = "Sonatype Nexus Repository Manager";

  protected final Logger log = LoggerFactory.getLogger(getClass());

  public NexusBasicHttpAuthenticationFilter() {
    setApplicationName(BASIC_AUTH_REALM);
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
  public boolean onPreHandle(
      final ServletRequest request,
      final ServletResponse response,
      final Object mappedValue) throws Exception
  {
    // Basic auth should never create sessions; we do not want session overhead for non-user clients that supply
    // credentials
    request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);

    return super.onPreHandle(request, response, mappedValue);
  }

  /**
   * Permissive {@link AuthorizationException} 401 and 403 handling.
   */
  @Override
  protected void cleanup(
      final ServletRequest request,
      final ServletResponse response,
      Exception failure) throws ServletException, IOException
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

  /**
   * Override to catch infrastructure exceptions wrapped by Shiro's AbstractAuthenticator.
   */
  @Override
  protected boolean onLoginFailure(
      final AuthenticationToken token,
      final AuthenticationException e,
      final ServletRequest request,
      final ServletResponse response)
  {
    // Check if the cause chain contains DataAccessException
    // Limit depth to prevent infinite loops from circular cause chains
    Throwable cause = e;
    int depth = 0;
    final int maxDepth = 20;
    while (cause != null && depth < maxDepth) {
      if (cause instanceof DataAccessException) {
        log.warn("Infrastructure failure during authentication", cause);
        try {
          HttpServletResponse httpResponse = WebUtils.toHttp(response);
          httpResponse.setHeader("Retry-After", "60");
          httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
              "Service temporarily unavailable");
          return false;
        }
        catch (IOException ex) {
          log.error("Failed to send 503 response, falling back to normal authentication failure handling", ex);
          return super.onLoginFailure(token, e, request, response);
        }
      }
      cause = cause.getCause();
      depth++;
    }

    return super.onLoginFailure(token, e, request, response);
  }

  @Override
  protected boolean onLoginSuccess(
      final AuthenticationToken token,
      final Subject subject,
      final ServletRequest request,
      final ServletResponse response) throws Exception
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

  @Override
  protected boolean isLoginAttempt(final String authzHeader) {
    return !isEmptyCredentials(authzHeader) && super.isLoginAttempt(authzHeader);
  }

  private boolean isEmptyCredentials(final String authzHeader) {
    if (!authzHeader.toLowerCase().contains("basic ")) {
      return false;
    }

    String[] parts = authzHeader.split(" ");
    return parts.length > 1 && parts[1].equals(EMPTY_CREDENTIALS);
  }
}
