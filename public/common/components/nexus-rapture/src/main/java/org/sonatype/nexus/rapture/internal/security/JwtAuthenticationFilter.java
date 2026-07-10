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

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.app.WebFilterPriority;
import org.sonatype.nexus.security.JwtHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * JWT authentication filter for {@link JwtServlet}.
 *
 * @since 3.38
 */
@WebFilter(filterName = JwtAuthenticationFilter.NAME)
@Order(WebFilterPriority.AUTHENTICATION)
@Component
@ConditionalOnProperty(name = JWT_ENABLED, havingValue = "true")
public class JwtAuthenticationFilter
    extends SessionAuthenticationFilter
{
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  public static final String NAME = "nx-jwt-authc";

  private final JwtHelper jwtHelper;

  @Autowired
  public JwtAuthenticationFilter(final JwtHelper jwtHelper) {
    this.jwtHelper = checkNotNull(jwtHelper);
  }

  @Override
  protected boolean onLoginSuccess(
      final AuthenticationToken token,
      final Subject subject,
      final ServletRequest request,
      final ServletResponse response) throws Exception
  {
    log.debug("Success: token={}, subject={}", token, subject);
    Cookie cookie = jwtHelper.createJwtCookie(subject, request.isSecure());
    ((HttpServletResponse) response).addCookie(cookie);
    return true;
  }
}
