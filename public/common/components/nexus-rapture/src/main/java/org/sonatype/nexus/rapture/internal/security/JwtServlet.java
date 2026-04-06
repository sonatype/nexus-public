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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.JwtHelper;
import org.sonatype.nexus.security.authc.LoginEvent;
import org.sonatype.nexus.security.authc.LogoutEvent;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static java.util.Arrays.stream;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.NXSESSIONID_SECURE_COOKIE_NAMED_VALUE;
import static org.sonatype.nexus.rapture.internal.security.SessionServlet.SESSION_MP;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;
import static org.sonatype.nexus.security.JwtHelper.REALM;
import static org.sonatype.nexus.security.JwtHelper.USER;
import static org.sonatype.nexus.security.JwtHelper.USER_SESSION_ID;
import static org.sonatype.nexus.servlet.XFrameOptions.DENY;

/**
 * JWT servlet, to expose end-point for configuration of Shiro authentication filter to
 * establish a JWT.
 *
 * @since 3.38
 *
 * @see JwtAuthenticationFilter
 */
@WebServlet(SESSION_MP)
@Component
@Singleton
@ConditionalOnProperty(name = JWT_ENABLED, havingValue = "true")
public class JwtServlet
    extends HttpServlet
{
  private static final Logger log = LoggerFactory.getLogger(JwtServlet.class);

  private final String contextPath;

  private final EventManager eventManager;

  private final boolean cookieSecure;

  private final JwtHelper jwtHelper;

  private final JwtSessionRevocationService jwtSessionRevocationService;

  @Inject
  public JwtServlet(
      @Value("${nexus-context-path:#{null}}") final String contextPath,
      final EventManager eventManager,
      @Value(NXSESSIONID_SECURE_COOKIE_NAMED_VALUE) final boolean cookieSecure,
      final JwtHelper jwtHelper,
      final JwtSessionRevocationService jwtSessionRevocationService)
  {
    this.contextPath = contextPath;
    this.eventManager = eventManager;
    this.cookieSecure = cookieSecure;
    this.jwtHelper = jwtHelper;
    this.jwtSessionRevocationService = jwtSessionRevocationService;
  }

  /**
   * Create token.
   */
  @Override
  protected void doPost(
      final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException
  {
    Subject subject = SecurityUtils.getSubject();
    log.debug("Created token for user: {}", subject.getPrincipal());
    Optional<String> realmName = subject.getPrincipals().getRealmNames().stream().findFirst();
    realmName.ifPresent(realm -> eventManager.post(new LoginEvent(subject.getPrincipal().toString(), realm)));

    // sanity check
    checkState(subject.isAuthenticated());

    response.setStatus(SC_NO_CONTENT);

    // Silence warnings about "clickjacking" (even though it doesn't actually apply to API calls)
    response.setHeader(X_FRAME_OPTIONS, DENY);
  }

  /**
   * Delete token.
   */
  @Override
  protected void doDelete(
      final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException
  {
    Subject subject = SecurityUtils.getSubject();
    Object principal = subject.getPrincipal();

    if (principal == null) {
      log.warn("Subject principal is null during logout (likely OIDC flow), attempting JWT-based cleanup");
      handleNullPrincipalLogout(request, response);
      return;
    }

    String username = principal.toString();
    log.debug("Deleting token for user: {}", username);

    // Extract and revoke the JWT session before logging out
    revokeJwtSession(request, username);

    Optional<String> realmName = subject.getPrincipals().getRealmNames().stream().findFirst();
    realmName.ifPresent(realm -> eventManager.post(new LogoutEvent(username, realm)));
    subject.logout();

    // sanity check
    checkState(!subject.isAuthenticated());
    checkState(!subject.isRemembered());

    Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
    cookie.setPath(contextPath);
    cookie.setMaxAge(0);
    // see JwtHelper#createCookie
    cookie.setSecure(request.isSecure() && cookieSecure);
    response.addCookie(cookie);

    response.setStatus(SC_NO_CONTENT);

    // Silence warnings about "clickjacking" (even though it doesn't actually apply to API calls)
    response.setHeader(X_FRAME_OPTIONS, DENY);
  }

  /**
   * Extract the JWT from the request and revoke the session in the database.
   */
  private void revokeJwtSession(final HttpServletRequest request, final String username) {
    if (jwtHelper == null || jwtSessionRevocationService == null) {
      log.debug("JWT session revocation not available (jwtHelper or jwtSessionRevocationService is null)");
      return;
    }

    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return;
    }

    Optional<String> jwtToken = stream(cookies)
        .filter(cookie -> cookie.getName().equals(JWT_COOKIE_NAME))
        .map(Cookie::getValue)
        .filter(value -> !Strings2.isEmpty(value))
        .findFirst();

    if (jwtToken.isPresent()) {
      try {
        DecodedJWT decodedJwt = jwtHelper.verifyJwt(jwtToken.get());
        Claim userSessionIdClaim = decodedJwt.getClaim(USER_SESSION_ID);
        Claim realmClaim = decodedJwt.getClaim(REALM);

        if (!userSessionIdClaim.isNull() && !realmClaim.isNull()) {
          String userSessionId = userSessionIdClaim.asString();
          String userSource = realmClaim.asString();
          Date expiresAtDate = decodedJwt.getExpiresAt();
          if (expiresAtDate == null) {
            log.warn("JWT token for user {} does not have an expiration claim", username);
            return;
          }
          OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
              expiresAtDate.toInstant(),
              ZoneId.systemDefault());

          jwtSessionRevocationService.revokeSession(userSessionId, username, userSource, expiresAt);
          log.debug("JWT session revoked on logout: username={}, userSource={}, sessionId={}, expiresAt={}",
              username, userSource, userSessionId, expiresAt);
        }
        else {
          log.debug("JWT token missing required claims for user {} (userSessionId or realm), cannot revoke", username);
        }
      }
      catch (Exception e) {
        log.warn(
            "Could not verify JWT during logout for user {}: {}. Logout will proceed but token revocation skipped.",
            username, e.getMessage());
        // Continue with logout even if JWT verification fails
      }
    }
    else {
      log.debug("No JWT cookie found during logout for user {}", username);
    }
  }

  /**
   * Handle logout when Shiro principal is null (e.g., after OIDC logout filter).
   * Extracts user information from JWT cookie and performs cleanup.
   */
  private void handleNullPrincipalLogout(
      final HttpServletRequest request,
      final HttpServletResponse response)
  {
    // Attempt to logout Shiro session if it exists (may already be logged out by OIDC filter)
    try {
      Subject subject = SecurityUtils.getSubject();
      if (subject != null) {
        subject.logout();
      }
    }
    catch (Exception e) {
      log.debug("Could not logout Shiro subject (likely already logged out by OIDC filter): {}", e.getMessage());
    }
    // Still revoke the JWT session using data from the cookie itself
    revokeJwtSessionFromCookie(request);

    // Still clear the cookie on the client
    Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
    cookie.setPath(contextPath);
    cookie.setMaxAge(0);
    cookie.setSecure(request.isSecure() && cookieSecure);
    response.addCookie(cookie);

    response.setStatus(SC_NO_CONTENT);
    response.setHeader(X_FRAME_OPTIONS, DENY);
  }

  /**
   * Revoke JWT session using data from the JWT cookie itself.
   * This is used when the Shiro principal is null (e.g., after OIDC logout).
   */
  private void revokeJwtSessionFromCookie(final HttpServletRequest request) {
    if (jwtHelper == null || jwtSessionRevocationService == null) {
      return;
    }

    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return;
    }

    Optional<String> jwtToken = stream(cookies)
        .filter(cookie -> cookie.getName().equals(JWT_COOKIE_NAME))
        .map(Cookie::getValue)
        .filter(value -> !Strings2.isEmpty(value))
        .findFirst();

    if (jwtToken.isPresent()) {
      try {
        DecodedJWT decodedJwt = jwtHelper.verifyJwt(jwtToken.get());
        Claim userSessionIdClaim = decodedJwt.getClaim(USER_SESSION_ID);
        Claim realmClaim = decodedJwt.getClaim(REALM);
        Claim userClaim = decodedJwt.getClaim(USER);

        if (!userSessionIdClaim.isNull() && !realmClaim.isNull() && !userClaim.isNull()) {
          String userSessionId = userSessionIdClaim.asString();
          String username = userClaim.asString();
          String userSource = realmClaim.asString();
          Date expiresAtDate = decodedJwt.getExpiresAt();
          if (expiresAtDate == null) {
            log.warn("JWT token does not have an expiration claim, skipping revocation during null-principal logout");
            return;
          }
          OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
              expiresAtDate.toInstant(), ZoneId.systemDefault());

          jwtSessionRevocationService.revokeSession(userSessionId, username, userSource, expiresAt);
          log.debug("JWT session revoked during null-principal logout: username={}, sessionId={}",
              username, userSessionId);
        }
      }
      catch (Exception e) {
        log.warn("Could not verify JWT during null-principal logout: {}. Cookie will still be cleared.",
            e.getMessage());
      }
    }
  }
}
