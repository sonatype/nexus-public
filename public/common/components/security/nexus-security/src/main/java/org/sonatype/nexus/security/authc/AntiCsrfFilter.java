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
package org.sonatype.nexus.security.authc;

import java.io.IOException;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This is an anti cross-site request forgery (CSRF / XSRF) protection using a cookie-to-header token approach.
 *
 * @since 3.13
 */
@WebFilter(filterName = AntiCsrfFilter.NAME)
@Component
public class AntiCsrfFilter
    extends AuthenticationFilter
{
  public static final String NAME = "nx-anticsrf-authc";

  private static final Logger log = LoggerFactory.getLogger(AntiCsrfFilter.class);

  private final AntiCsrfHelper csrfHelper;

  @Autowired
  public AntiCsrfFilter(final AntiCsrfHelper csrfHelper) {
    this.csrfHelper = csrfHelper;
  }

  @Override
  public boolean isEnabled() {
    return csrfHelper.isEnabled();
  }

  @Override
  protected boolean isAccessAllowed(
      final ServletRequest request,
      final ServletResponse response,
      final Object mappedValue)
  {
    return csrfHelper.isAccessAllowed((HttpServletRequest) request);
  }

  @Override
  protected boolean onAccessDenied(final ServletRequest request, final ServletResponse response) throws IOException {
    log.debug("Rejecting request from {} due to invalid cross-site request forgery token", request.getRemoteAddr());

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    httpResponse.setContentType("text/plain");
    httpResponse.getWriter().print(AntiCsrfHelper.ERROR_MESSAGE_TOKEN_MISMATCH);

    return false;
  }
}
