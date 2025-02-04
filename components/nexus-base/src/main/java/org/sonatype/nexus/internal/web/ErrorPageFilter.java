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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.servlet.XFrameOptions;

import org.eclipse.sisu.Hidden;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * Servlet filter to add error page rendering.
 *
 * @since 2.8
 *
 * @see ErrorPageServlet
 */
@Named
@Hidden // hide from DynamicFilterChainManager because we statically install it in WebModule
@Singleton
public class ErrorPageFilter
    extends ComponentSupport
    implements Filter
{
  private final XFrameOptions xFrameOptions;

  @Inject
  public ErrorPageFilter(final XFrameOptions xFrameOptions) {
    this.xFrameOptions = checkNotNull(xFrameOptions);
  }

  @Override
  public void init(final FilterConfig config) throws ServletException {
    // ignore
  }

  @Override
  public void destroy() {
    // ignore
  }

  @Override
  public void doFilter(
      final ServletRequest req,
      final ServletResponse resp,
      final FilterChain chain) throws IOException, ServletException
  {
    final HttpServletRequest request = (HttpServletRequest) req;
    final HttpServletResponse response = (HttpServletResponse) resp;

    // Delegate any exceptions to the ErrorPageServlet via standard sendError servlet api
    // Custom handling here to avoid logging from Jetty implementation
    try {
      chain.doFilter(request, response);
    }
    catch (Exception e) {
      ErrorPageServlet.attachCause(request, e);
      if (resp.isCommitted()) {
        log.debug("Response is committed, cannot change status", e);
        return;
      }
      response.setHeader(X_FRAME_OPTIONS, xFrameOptions.getValueForPath(request.getPathInfo()));
      response.sendError(SC_INTERNAL_SERVER_ERROR);
    }
  }
}
