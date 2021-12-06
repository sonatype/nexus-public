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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.security.JwtHelper;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;

/**
 * JWT authentication filter for {@link JwtServlet}.
 *
 * @since 3.next
 */
@Named
@Singleton
@FeatureFlag(name = JWT_ENABLED)
public class JwtAuthenticationFilter
    extends SessionAuthenticationFilter
{
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  public static final String NAME = "nx-jwt-authc";

  private final JwtHelper jwtHelper;

  private final String contextPath;

  @Inject
  public JwtAuthenticationFilter(final JwtHelper jwtHelper,
                                 @Named("${nexus-context-path}") final String contextPath) {
    this.jwtHelper = checkNotNull(jwtHelper);
    this.contextPath = checkNotNull(contextPath);
  }

  @Override
  protected boolean onLoginSuccess(final AuthenticationToken token,
                                   final Subject subject,
                                   final ServletRequest request,
                                   final ServletResponse response)
      throws Exception
  {
    log.debug("Success: token={}, subject={}", token, subject);
    String jwtToken = jwtHelper.createJwt(subject);
    int expiry = jwtHelper.getExpiryInSec();

    Cookie cookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    cookie.setMaxAge(expiry);
    cookie.setPath(contextPath);
    cookie.setHttpOnly(true);
    ((HttpServletResponse)response).addCookie(cookie);
    return true;
  }
}
