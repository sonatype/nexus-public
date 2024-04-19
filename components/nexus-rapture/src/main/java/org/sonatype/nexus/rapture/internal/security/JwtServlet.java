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
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.authc.LoginEvent;
import org.sonatype.nexus.security.authc.LogoutEvent;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.NXSESSIONID_SECURE_COOKIE_NAMED;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;
import static org.sonatype.nexus.servlet.XFrameOptions.DENY;

/**
 * JWT servlet, to expose end-point for configuration of Shiro authentication filter to
 * establish a JWT.
 *
 * @since 3.38
 *
 * @see JwtAuthenticationFilter
 */
@Named
@Singleton
@FeatureFlag(name = JWT_ENABLED)
public class JwtServlet
    extends HttpServlet
{
  private static final Logger log = LoggerFactory.getLogger(JwtServlet.class);

  private final String contextPath;

  private final EventManager eventManager;

  private final boolean cookieSecure;

  public JwtServlet(final String contextPath, final EventManager eventManager) {
    this(contextPath, eventManager, false);
  }
  @Inject
  public JwtServlet(@Named("${nexus-context-path}") final String contextPath, final EventManager eventManager,
                    @Named(NXSESSIONID_SECURE_COOKIE_NAMED) final boolean cookieSecure) {
    this.contextPath = contextPath;
    this.eventManager = eventManager;
    this.cookieSecure = cookieSecure;
  }

  /**
   * Create token.
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
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
  protected void doDelete(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    Subject subject = SecurityUtils.getSubject();
    log.debug("Deleting token for user: {}", subject.getPrincipal());
    Optional<String> realmName = subject.getPrincipals().getRealmNames().stream().findFirst();
    realmName.ifPresent(realm -> eventManager.post(new LogoutEvent(subject.getPrincipal().toString(), realm)));
    subject.logout();

    // sanity check
    checkState(!subject.isAuthenticated());
    checkState(!subject.isRemembered());

    Cookie cookie = new Cookie(JWT_COOKIE_NAME, "null");
    cookie.setPath(contextPath);
    cookie.setMaxAge(0);
    // see JwtHelper#createCookie
    cookie.setSecure(request.isSecure() && cookieSecure);
    response.addCookie(cookie);

    response.setStatus(SC_NO_CONTENT);

    // Silence warnings about "clickjacking" (even though it doesn't actually apply to API calls)
    response.setHeader(X_FRAME_OPTIONS, DENY);
  }
}
