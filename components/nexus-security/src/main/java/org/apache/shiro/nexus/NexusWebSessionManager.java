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
package org.apache.shiro.nexus;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.FeatureFlags;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionValidationScheduler;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.session.mgt.WebSessionManager;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link WebSessionManager}.
 *
 * This session manager predates the more recent JWT session management in org.sonatype.nexus.security. It's used
 * in for single node deployments typically, however it is not used for Pro deployments using SAML (HA or not).
 */
public class NexusWebSessionManager
    extends DefaultWebSessionManager
{
  private static final Logger log = LoggerFactory.getLogger(NexusWebSessionManager.class);

  private static final String DEFAULT_NEXUS_SESSION_COOKIE_NAME = "NXSESSIONID";

  private static final ThreadLocal<Boolean> requestIsHttps = ThreadLocal.withInitial(() -> Boolean.TRUE);

  @Inject
  public void configureProperties(
      @Named("${shiro.globalSessionTimeout:-" + DEFAULT_GLOBAL_SESSION_TIMEOUT + "}") final long globalSessionTimeout,
      @Named("${nexus.sessionCookieName:-" + DEFAULT_NEXUS_SESSION_COOKIE_NAME + "}") final String sessionCookieName,
      @Named("${nexus.session.enabled:-true}") final boolean sessionEnabled,
      @Named(FeatureFlags.NXSESSIONID_SECURE_COOKIE_NAMED) final boolean cookieSecure)
  {
    setGlobalSessionTimeout(globalSessionTimeout);
    log.info("Global session timeout: {} ms", getGlobalSessionTimeout());

    setSessionIdCookieEnabled(sessionEnabled);
    Cookie cookie = getSessionIdCookie();
    cookie.setName(sessionCookieName);
    cookie.setSecure(cookieSecure);
    log.info("Session-cookie prototype: name={}, secure={}", cookie.getName(), cookie.isSecure());
  }

  /**
   * Overrides the {@link #onStart(Session, SessionContext)} to first check to see if the request is coming
   * on a secure channel.
   *
   * @param session
   * @param context
   */
  @Override
  protected void onStart(final Session session, final SessionContext context) {
    if (WebUtils.isHttp((context))) {
      requestIsHttps.set(WebUtils.getHttpRequest(context).isSecure());
    }
    try {
      super.onStart(session, context);
    } finally {
      if (WebUtils.isHttp(context)) {
        requestIsHttps.remove();
      }
    }
  }

  /**
   * {@link #getSessionIdCookie()} in the parent returns a "template" cookie that is passed in as an argument.
   * It represents our ONLY injection point to set the Secure flag. If we blindly set the secure flag and the
   * request came in on a plain text HTTP only channel, the browser will refuse it. We need a way to know
   * if the cookie will be used on an HTTPS channel or not.
   *
   * @return the cookie template, including a value for {@link Cookie#isSecure()} appropriate for the request.
   */
  @Override
  public Cookie getSessionIdCookie() {
    Cookie cookie = super.getSessionIdCookie();
    boolean templateValue = cookie.isSecure();
    boolean requestIsSecure = requestIsHttps.get();
    log.trace("setting Secure flag on session cookie: systemValue={}, requestIsSecure={}",templateValue, requestIsSecure);
    cookie.setSecure(templateValue && requestIsSecure);
    return cookie;
  }

  /**
   * See https://issues.sonatype.org/browse/NEXUS-5727, https://issues.apache.org/jira/browse/SHIRO-443
   */
  @Override
  protected synchronized void enableSessionValidation() {
    final SessionValidationScheduler scheduler = getSessionValidationScheduler();
    if (scheduler == null) {
      super.enableSessionValidation();
    }
  }
}
