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

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataAccessException;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@WebFilter(filterName = NexusBasicHttpAuthenticationFilter.NAME)
@Component
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

  @Autowired(required = false)
  @Nullable
  private AuthRateLimiterService rateLimiterService;

  @Autowired(required = false)
  @Nullable
  private EventManager eventManager;

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
   * Override to apply rate limiting after a confirmed credential failure and to handle
   * infrastructure exceptions.
   *
   * <p>
   * Order of checks:
   * <ol>
   * <li>Infrastructure failure ({@link DataAccessException}) → 503, counter not incremented.</li>
   * <li>Rate limit check ({@link AuthRateLimiterService#checkAndRecord}) → 429 if threshold exceeded.</li>
   * <li>Delegate to super → 401.</li>
   * </ol>
   *
   * <p>
   * Placing the rate limit check here (post-auth) ensures that correct credentials always
   * reach Shiro and can reset the counter via {@link #onLoginSuccess}.
   */
  @Override
  protected boolean onLoginFailure(
      final AuthenticationToken token,
      final AuthenticationException e,
      final ServletRequest request,
      final ServletResponse response)
  {
    // 1. Infrastructure failures — do not count toward rate limit
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

    // 2. Rate limiting — applied after confirmed credential failure
    if (rateLimiterService != null) {
      String username = token.getPrincipal().toString();
      RateLimitResult limitResult = rateLimiterService.checkAndRecord(username);
      if (limitResult != null) {
        log.debug("Rate limiting login attempt for user '{}'", username);
        if (eventManager != null) {
          // IP is captured for audit trail only; rate-limit decisions are username-only (OWASP-aligned)
          String clientIp = WebUtils.toHttp(request).getRemoteAddr();
          eventManager.post(new AuthRateLimitedEvent(username, limitResult.attemptCount(),
              limitResult.retryAfterSeconds(), clientIp, "BASIC"));
        }
        try {
          HttpServletResponse httpResponse = WebUtils.toHttp(response);
          httpResponse.setHeader("Retry-After", String.valueOf(limitResult.retryAfterSeconds()));
          httpResponse.sendError(429, "Too many authentication attempts");
        }
        catch (IOException ex) {
          log.error("Failed to send 429 response", ex);
        }
        return false;
      }
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
      // Prefer the subject principal over the token's for request-log attributes,
      // as these could be different for token-based auth (e.g. LDAP mapped username)
      Object principal = subject.getPrincipal();
      if (principal == null) {
        principal = token.getPrincipal();
      }

      // Attach principal+userId to request so we can use that in the request-log
      request.setAttribute(ATTR_USER_PRINCIPAL, principal);
      request.setAttribute(ATTR_USER_ID, principal.toString());

      if (rateLimiterService != null) {
        // Use the token principal (raw username from Authorization header) to match
        // the key used in checkAndRecord, which also reads from the Authorization header
        rateLimiterService.recordSuccess(token.getPrincipal().toString());
      }
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
