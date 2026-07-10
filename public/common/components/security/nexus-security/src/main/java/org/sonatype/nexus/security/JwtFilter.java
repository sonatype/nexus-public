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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;
import org.sonatype.nexus.security.jwt.JwtVerificationException;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;
import static org.sonatype.nexus.security.JwtHelper.USER;
import static org.sonatype.nexus.security.JwtHelper.USER_SESSION_ID;

/**
 * Filter to verify and refresh JWT cookie
 *
 * @since 3.38
 */
@ConditionalOnProperty(name = JWT_ENABLED, havingValue = "true")
@WebFilter(filterName = JwtFilter.NAME)
@Component
public class JwtFilter
    extends AdviceFilter
{
  public static final String NAME = "nx-jwt";

  private final JwtHelper jwtHelper;

  private final JwtSessionRevocationService jwtSessionRevocationService;

  @Autowired
  public JwtFilter(
      final JwtHelper jwtHelper,
      final JwtSessionRevocationService jwtSessionRevocationService)
  {
    this.jwtHelper = checkNotNull(jwtHelper);
    this.jwtSessionRevocationService = checkNotNull(jwtSessionRevocationService);
  }

  @Override
  protected boolean preHandle(final ServletRequest request, final ServletResponse response) throws Exception {
    HttpServletRequest servletRequest = (HttpServletRequest) request;
    Cookie[] cookies = servletRequest.getCookies();

    if (cookies != null) {
      Optional<Cookie> jwtCookie = stream(cookies)
          .filter(cookie -> cookie.getName().equals(JWT_COOKIE_NAME))
          .findFirst();

      if (jwtCookie.isPresent()) {
        Cookie cookie = jwtCookie.get();
        String jwt = cookie.getValue();
        if (!Strings2.isEmpty(jwt)) {
          DecodedJWT decoded;
          try {
            decoded = jwtHelper.verifyJwt(jwt);
          }
          catch (JwtVerificationException e) {
            // expire the cookie in case of any issues while JWT verification
            expireCookie(cookie, response);
            return false;
          }

          // Do not refresh a revoked session's JWT. Clearing here keeps the browser's
          // stored cookie from advancing its iat past a user-invalidation cutoff.
          if (isRevokedOrInvalidated(decoded)) {
            expireCookie(cookie, response);
            return false;
          }

          WebUtils.toHttp(response).addCookie(jwtHelper.refreshJwtCookie(decoded, request.isSecure()));
        }
      }
    }
    return true;
  }

  private boolean isRevokedOrInvalidated(final DecodedJWT decoded) {
    Claim sessionIdClaim = decoded.getClaim(USER_SESSION_ID);
    if (sessionIdClaim != null && !sessionIdClaim.isNull()
        && jwtSessionRevocationService.isRevoked(sessionIdClaim.asString())) {
      return true;
    }

    Claim userClaim = decoded.getClaim(USER);
    Date issuedAt = decoded.getIssuedAt();
    if (userClaim == null || userClaim.isNull() || issuedAt == null) {
      return false;
    }

    OffsetDateTime iat = issuedAt.toInstant().atOffset(ZoneOffset.UTC);
    return jwtSessionRevocationService.isUserInvalidatedAfter(userClaim.asString(), iat);
  }

  private static void expireCookie(final Cookie cookie, final ServletResponse response) {
    cookie.setValue("");
    cookie.setMaxAge(0);
    WebUtils.toHttp(response).addCookie(cookie);
  }
}
