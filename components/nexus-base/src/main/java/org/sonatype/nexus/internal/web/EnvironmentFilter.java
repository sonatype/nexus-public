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
package org.sonatype.nexus.internal.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.security.UserIdMdcHelper;

import org.eclipse.sisu.Hidden;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.SERVER;
import static com.google.common.net.HttpHeaders.X_CONTENT_TYPE_OPTIONS;

/**
 * Sets up the basic environment for web-requests.
 *
 * @since 3.0
 */
@Named
@Hidden // hide from DynamicFilterChainManager because we statically install it in WebModule
@Singleton
public class EnvironmentFilter
    extends ComponentSupport
    implements Filter
{
  private final String serverBanner;

  private final String serverHeader;

  private final BaseUrlManager baseUrlManager;

  @Inject
  public EnvironmentFilter(
      final ApplicationVersion applicationVersion,
      final BaseUrlManager baseUrlManager)
  {
    // cache "Server" header value
    checkNotNull(applicationVersion);

    this.serverBanner = String.format("Sonatype Nexus %s %s",
        applicationVersion.getEdition(),
        applicationVersion.getVersion());

    this.serverHeader = String.format("Nexus/%s (%s)",
        applicationVersion.getVersion(),
        applicationVersion.getEdition());

    this.baseUrlManager = checkNotNull(baseUrlManager);
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    filterConfig.getServletContext().setAttribute("nexus-banner", serverBanner);
  }

  @Override
  public void destroy() {
    // ignore
  }

  @Override
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    // start with default unknown user-id in MDC
    UserIdMdcHelper.unknown();

    // detect base-url
    baseUrlManager.detectAndHoldUrl();

    // fill in default response headers
    defaultHeaders((HttpServletResponse) response);

    try {
      chain.doFilter(request, response);
    }
    finally {
      // unset user-id MDC
      UserIdMdcHelper.unset();
    }
  }

  /**
   * Add default headers to servlet response.
   */
  private void defaultHeaders(final HttpServletResponse response) {
    response.setHeader(SERVER, serverHeader);

    // NEXUS-5023 disable IE for sniffing into response content
    response.setHeader(X_CONTENT_TYPE_OPTIONS, "nosniff");
  }
}
