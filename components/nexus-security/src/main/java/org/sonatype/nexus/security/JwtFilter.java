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

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.jwt.JwtVerificationException;

import org.apache.shiro.web.servlet.AdviceFilter;
import org.apache.shiro.web.util.WebUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;

/**
 * Filter to verify and refresh JWT cookie
 *
 * @since 3.38
 */
@Named
@Singleton
public class JwtFilter
    extends AdviceFilter
{
  public static final String NAME = "nx-jwt";

  private final JwtHelper jwtHelper;

  private final List<JwtRefreshExemption> jwtExemptPaths;

  @Inject
  public JwtFilter(final JwtHelper jwtHelper,
                   final List<JwtRefreshExemption> jwtExemptPaths) {
    this.jwtHelper = checkNotNull(jwtHelper);
    this.jwtExemptPaths = jwtExemptPaths;
  }

  @Override
  protected boolean preHandle(final ServletRequest request, final ServletResponse response) throws Exception {
    HttpServletRequest servletRequest = (HttpServletRequest) request;
    Cookie[] cookies = servletRequest.getCookies();

    if ((cookies != null) && !isExemptRequest(servletRequest)) {
      Optional<Cookie> jwtCookie = stream(cookies)
          .filter(cookie -> cookie.getName().equals(JWT_COOKIE_NAME))
          .findFirst();

      if (jwtCookie.isPresent()) {
        Cookie cookie = jwtCookie.get();
        String jwt = cookie.getValue();
        if (!Strings2.isEmpty(jwt)) {
          Cookie refreshedToken;
          try {
            refreshedToken = jwtHelper.verifyAndRefreshJwtCookie(jwt, request.isSecure());
          }
          catch (JwtVerificationException e) {
            // expire the cookie in case of any issues while JWT verification
            cookie.setValue("");
            cookie.setMaxAge(0);
            WebUtils.toHttp(response).addCookie(cookie);
            return false;
          }
          WebUtils.toHttp(response).addCookie(refreshedToken);
        }
      }
    }
    return true;
  }

  private boolean isExemptRequest(final HttpServletRequest request) {
    String requestPath = request.getServletPath();
    return jwtExemptPaths.stream()
        .map(JwtRefreshExemption::getPath)
        .anyMatch(requestPath::contains);
  }
}
