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
package org.sonatype.nexus.security;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;
import org.sonatype.nexus.security.jwt.JwtVerificationException;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.subject.WebSubject;
import org.apache.shiro.web.subject.support.WebDelegatingSubject;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;
import static org.sonatype.nexus.security.JwtHelper.REALM;
import static org.sonatype.nexus.security.JwtHelper.USER;
import static org.sonatype.nexus.security.JwtHelper.USER_SESSION_ID;

/**
 * JWT security filter.
 *
 * @since 3.38
 */
@WebFilter("/*")
@ConditionalOnProperty(value = JWT_ENABLED, havingValue = "true")
public class JwtSecurityFilter
    extends SecurityFilter
{
  private final JwtHelper jwtHelper;

  private final JwtSessionRevocationService jwtSessionRevocationService;

  private final AuditRecorder auditRecorder;

  private static final Logger log = LoggerFactory.getLogger(JwtSecurityFilter.class);

  @Autowired
  public JwtSecurityFilter(
      final WebSecurityManager webSecurityManager,
      final FilterChainResolver filterChainResolver,
      final JwtHelper jwtHelper,
      final JwtSessionRevocationService jwtSessionRevocationService,
      final AuditRecorder auditRecorder)
  {
    super(webSecurityManager, filterChainResolver);
    this.jwtHelper = checkNotNull(jwtHelper);
    this.jwtSessionRevocationService = checkNotNull(jwtSessionRevocationService);
    this.auditRecorder = checkNotNull(auditRecorder);
  }

  @Override
  protected WebSubject createSubject(final ServletRequest request, final ServletResponse response) {
    Cookie[] cookies = ((HttpServletRequest) request).getCookies();

    if (cookies != null) {
      Optional<Cookie> jwtCookie = stream(cookies)
          .filter(cookie -> cookie.getName().equals(JWT_COOKIE_NAME))
          .findFirst();

      if (jwtCookie.isPresent()) {
        Cookie cookie = jwtCookie.get();

        SimpleSession session = new SimpleSession(request.getRemoteHost());
        DecodedJWT decodedJwt;
        String jwt = cookie.getValue();
        if (!Strings2.isEmpty(jwt)) {
          try {
            decodedJwt = jwtHelper.verifyJwt(jwt);
          }
          catch (JwtVerificationException e) {
            log.debug("Expire and reset the JWT cookie due to the error: {}", e.getMessage());
            cookie.setValue("");
            cookie.setMaxAge(0);
            WebUtils.toHttp(response).addCookie(cookie);

            return super.createSubject(request, response);
          }

          // Check if the session has been revoked (logged out)
          Claim userSessionIdClaim = decodedJwt.getClaim(USER_SESSION_ID);
          if (!userSessionIdClaim.isNull()) {
            String userSessionId = userSessionIdClaim.asString();
            if (jwtSessionRevocationService.isRevoked(userSessionId)) {
              Claim userClaim = decodedJwt.getClaim(USER);
              String username = userClaim != null ? userClaim.asString() : "unknown";
              String remoteAddr = request.getRemoteAddr();
              String remoteHost = request.getRemoteHost();

              log.warn("SECURITY: Attempt to use revoked JWT token detected and blocked. " +
                  "Username: {}, Session ID: {}, Remote IP: {}, Remote Host: {}",
                  username, userSessionId, remoteAddr, remoteHost);

              // Record audit event for revoked token usage attempt
              recordRevokedTokenAudit(username, userSessionId, remoteAddr, remoteHost);

              cookie.setValue("");
              cookie.setMaxAge(0);
              WebUtils.toHttp(response).addCookie(cookie);

              return super.createSubject(request, response);
            }
          }

          Claim user = decodedJwt.getClaim(USER);
          Claim realm = decodedJwt.getClaim(REALM);

          // Handle case where user or realm claims are missing/null
          // Check both if claim is null and if asString() returns null
          String username = user != null && !user.isNull() ? user.asString() : null;
          String realmName = realm != null && !realm.isNull() ? realm.asString() : null;

          if (username == null || realmName == null) {
            log.debug("Invalid JWT token: missing user or realm claim");
            cookie.setValue("");
            cookie.setMaxAge(0);
            WebUtils.toHttp(response).addCookie(cookie);
            return super.createSubject(request, response);
          }

          PrincipalCollection principals = new SimplePrincipalCollection(username, realmName);

          session.setTimeout(TimeUnit.SECONDS.toMillis(jwtHelper.getExpirySeconds()));
          session.setAttribute(JWT_COOKIE_NAME, jwt);

          return new WebDelegatingSubject(
              principals,
              true,
              request.getRemoteHost(),
              session,
              true,
              request,
              response,
              getSecurityManager());
        }
      }
    }
    return super.createSubject(request, response);
  }

  /**
   * Record audit event for attempted use of a revoked JWT token.
   * This creates a permanent audit trail of security events.
   */
  private void recordRevokedTokenAudit(
      final String username,
      final String sessionId,
      final String remoteAddr,
      final String remoteHost)
  {
    if (auditRecorder.isEnabled()) {
      AuditData data = new AuditData();
      data.setDomain("security.jwt");
      data.setType("revoked-token-attempt");
      data.setContext(username);
      data.setTimestamp(new Date());
      data.setInitiator(username + "/" + remoteAddr);

      data.getAttributes().put("username", username);
      data.getAttributes().put("sessionId", sessionId);
      data.getAttributes().put("remoteAddr", remoteAddr);
      data.getAttributes().put("remoteHost", remoteHost);

      auditRecorder.record(data);
    }
  }
}
