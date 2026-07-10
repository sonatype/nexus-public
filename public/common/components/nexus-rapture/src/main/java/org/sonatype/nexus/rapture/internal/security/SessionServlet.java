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
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.authc.LoginEvent;
import org.sonatype.nexus.security.authc.LogoutEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static jakarta.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.sonatype.nexus.common.app.FeatureFlags.SESSION_ENABLED;
import static org.sonatype.nexus.servlet.XFrameOptions.DENY;

/**
 * Session servlet, to expose end-point for configuration of Shiro authentication filter to
 * establish a user session.
 *
 * @since 3.0
 *
 * @see SessionAuthenticationFilter
 */
@WebServlet(SessionServlet.SESSION_MP)
@Component
@ConditionalOnProperty(name = SESSION_ENABLED, havingValue = "true")
public class SessionServlet
    extends HttpServlet
{
  public static final String SESSION_MP = "/service/rapture/session";

  private static final Logger log = LoggerFactory.getLogger(SessionServlet.class);

  private final EventManager eventManager;

  @Autowired
  public SessionServlet(final EventManager eventManager) {
    this.eventManager = eventManager;
  }

  /**
   * Create session.
   */
  @Override
  protected void doPost(
      final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException
  {
    Subject subject = SecurityUtils.getSubject();
    log.info("Created session for user: {}", subject.getPrincipal());
    Optional<String> realmName = subject.getPrincipals().getRealmNames().stream().findFirst();
    realmName.ifPresent(realm -> eventManager.post(new LoginEvent(subject.getPrincipal().toString(), realm)));

    // sanity check
    checkState(subject.isAuthenticated());
    checkState(subject.getSession(false) != null);

    response.setStatus(SC_NO_CONTENT);

    // Silence warnings about "clickjacking" (even though it doesn't actually apply to API calls)
    response.setHeader(X_FRAME_OPTIONS, DENY);
  }

  /**
   * Delete session.
   */
  @Override
  protected void doDelete(
      final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException
  {
    Subject subject = SecurityUtils.getSubject();

    // Handle the case where session has already timed out (user is anonymous)
    // This prevents NPE when getPrincipal() or getPrincipals() returns null
    if (subject.getPrincipal() == null) {
      log.info("Session already expired or user not authenticated");
      response.setStatus(SC_NO_CONTENT);
      response.setHeader(X_FRAME_OPTIONS, DENY);
      return;
    }

    log.info("Deleting session for user: {}", subject.getPrincipal());
    Optional<String> realmName = subject.getPrincipals().getRealmNames().stream().findFirst();
    realmName.ifPresent(realm -> eventManager.post(new LogoutEvent(subject.getPrincipal().toString(), realm)));
    subject.logout();

    // sanity check
    checkState(!subject.isAuthenticated());
    checkState(!subject.isRemembered());
    checkState(subject.getSession(false) == null);

    response.setStatus(SC_NO_CONTENT);

    // Silence warnings about "clickjacking" (even though it doesn't actually apply to API calls)
    response.setHeader(X_FRAME_OPTIONS, DENY);
  }
}
