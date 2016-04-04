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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

/**
 * Session servlet, to expose end-point for configuration of Shiro authentication filter to
 * establish a user session.
 *
 * @since 3.0
 *
 * @see SessionAuthenticationFilter
 */
@Named
@Singleton
public class SessionServlet
  extends HttpServlet
{
  private static final Logger log = LoggerFactory.getLogger(SessionServlet.class);

  /**
   * Create session.
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    Subject subject = SecurityUtils.getSubject();
    log.info("Created session for user: {}", subject.getPrincipal());

    // sanity check
    checkState(subject.isAuthenticated());
    checkState(subject.getSession(false) != null);

    response.setStatus(SC_NO_CONTENT);
  }

  /**
   * Delete session.
   */
  @Override
  protected void doDelete(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    Subject subject = SecurityUtils.getSubject();
    log.info("Deleting session for user: {}", subject.getPrincipal());
    subject.logout();

    // sanity check
    checkState(!subject.isAuthenticated());
    checkState(!subject.isRemembered());
    checkState(subject.getSession(false) == null);

    response.setStatus(SC_NO_CONTENT);
  }
}
