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

import java.io.IOException;

import javax.annotation.Nullable;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.app.WebFilterPriority;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.authc.AuthRateLimitedEvent;
import org.sonatype.nexus.security.authc.AuthRateLimiterService;
import org.sonatype.nexus.security.authc.RateLimitResult;
import org.sonatype.nexus.security.authc.SsoDetector;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.SESSION_ENABLED;

/**
 * Session authentication filter for {@link SessionServlet}.
 *
 * Provides (very) basic {@code x-www-form-urlencoded} authentication support.
 *
 * @since 3.0
 */
@WebFilter(filterName = SessionAuthenticationFilter.NAME)
@Order(WebFilterPriority.AUTHENTICATION)
@Component
@ConditionalOnProperty(name = SESSION_ENABLED, havingValue = "true")
public class SessionAuthenticationFilter
    extends AuthenticatingFilter
{
  private static final Logger log = LoggerFactory.getLogger(SessionAuthenticationFilter.class);

  public static final String NAME = "nx-session-authc";

  public static final String P_USERNAME = "username";

  public static final String P_PASSWORD = "password";

  public static final String DELETE_METHOD = "DELETE";

  @Autowired(required = false)
  @Nullable
  private AuthRateLimiterService rateLimiterService;

  @Autowired(required = false)
  @Nullable
  private SsoDetector ssoDetector;

  @Autowired(required = false)
  @Nullable
  private EventManager eventManager;

  /**
   * Allow if authenticated or if logout request.
   */
  @Override
  protected boolean isAccessAllowed(
      final ServletRequest request,
      final ServletResponse response,
      final Object mappedValue)
  {
    Subject subject = getSubject(request, response);
    return subject.isAuthenticated() || isLogoutRequest(request);
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
    if (isLoginRequest(request, response)) {
      // Pre-authentication rate limit check: reject blocked users before hitting the auth backend.
      if (rateLimiterService != null && (ssoDetector == null || !ssoDetector.isSsoEnabled())) {
        String username = decodeBase64Param(request, P_USERNAME);
        if (username != null) {
          RateLimitResult limitResult = rateLimiterService.check(username);
          if (limitResult != null) {
            log.debug("Pre-auth rate limit blocking login attempt for user '{}'", username);
            if (eventManager != null) {
              String clientIp = WebUtils.toHttp(request).getRemoteAddr();
              eventManager.post(
                  new AuthRateLimitedEvent(username, limitResult.attemptCount(), limitResult.retryAfterSeconds(),
                      clientIp, "UI"));
            }
            try {
              HttpServletResponse httpResponse = WebUtils.toHttp(response);
              httpResponse.setHeader("Retry-After", String.valueOf(limitResult.retryAfterSeconds()));
              httpResponse.sendError(429, "Too many authentication attempts");
            }
            catch (IOException ex) {
              log.error("Failed to send 429 response", ex);
              denied(response);
            }
            return false;
          }
        }
      }

      log.trace("Attempting authentication");
      boolean authenticated = executeLogin(request, response);
      if (!authenticated) {
        log.trace("Access denied");
        denied(response);
      }
      return authenticated;
    }

    log.trace("Access denied");
    denied(response);
    return false;
  }

  @Override
  protected boolean isLoginRequest(final ServletRequest request, final ServletResponse response) {
    return (request instanceof HttpServletRequest) &&
        WebUtils.toHttp(request).getMethod().equalsIgnoreCase(POST_METHOD);
  }

  private boolean isLogoutRequest(final ServletRequest request) {
    return (request instanceof HttpServletRequest) &&
        WebUtils.toHttp(request).getMethod().equalsIgnoreCase(DELETE_METHOD);
  }

  private String decodeBase64Param(final ServletRequest request, final String name) {
    String encoded = WebUtils.getCleanParam(request, name);
    if (encoded != null) {
      return Strings2.decodeBase64(encoded);
    }
    return null;
  }

  @Override
  protected AuthenticationToken createToken(
      final ServletRequest request,
      final ServletResponse response) throws Exception
  {
    String username = decodeBase64Param(request, P_USERNAME);
    String password = decodeBase64Param(request, P_PASSWORD);
    return createToken(username, password, request, response);
  }

  @Override
  protected boolean onLoginSuccess(
      final AuthenticationToken token,
      final Subject subject,
      final ServletRequest request,
      final ServletResponse response) throws Exception
  {
    log.debug("Success: token={}, subject={}", token, subject);
    if (rateLimiterService != null) {
      // Use token principal to match the key used in checkAndRecord (onLoginFailure)
      rateLimiterService.recordSuccess(token.getPrincipal().toString());
    }
    return true;
  }

  @Override
  protected boolean onLoginFailure(
      final AuthenticationToken token,
      final AuthenticationException e,
      final ServletRequest request,
      final ServletResponse response)
  {
    log.debug("Failure: token={}", token, e);
    if (rateLimiterService != null && (ssoDetector == null || !ssoDetector.isSsoEnabled())) {
      // Record the failure to advance the backoff counter. The 429 response is handled by
      // the pre-auth check in onAccessDenied on subsequent attempts.
      String username = token.getPrincipal().toString();
      RateLimitResult limitResult = rateLimiterService.checkAndRecord(username);
      if (limitResult != null && eventManager != null) {
        String clientIp = WebUtils.toHttp(request).getRemoteAddr();
        eventManager.post(
            new AuthRateLimitedEvent(username, limitResult.attemptCount(), limitResult.retryAfterSeconds(),
                clientIp, "UI"));
      }
    }
    denied(response);
    return false;
  }
}
