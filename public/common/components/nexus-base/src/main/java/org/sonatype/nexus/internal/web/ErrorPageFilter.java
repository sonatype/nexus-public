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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.app.WebFilterPriority;
import org.sonatype.nexus.security.authc.NexusAuthenticationException;
import org.sonatype.nexus.servlet.XFrameOptions;

import com.google.common.base.Throwables;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.authc.AuthenticationException;
import org.eclipse.jetty.io.EofException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 * Servlet filter to add error page rendering.
 *
 * @since 2.8
 *
 * @see ErrorPageServlet
 */
@Order(WebFilterPriority.WEB)
@WebFilter("/*")
@Component
public class ErrorPageFilter
    implements Filter
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final XFrameOptions xFrameOptions;

  @Autowired
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
      // NEXUS-49601: Rethrow EofException as IOException (unwrapped) so Jetty properly aborts the response.
      // If the exception is not thrown, Jetty's handleException() doesn't abort early and continues to
      // the COMPLETE state, which validates Content-Length vs bytes written, causing spurious
      // "Insufficient content written" errors when clients disconnect during large file transfers.
      if (e instanceof EofException) {
        log.debug("Client terminated connection", e);
        Throwables.propagateIfPossible(e, ServletException.class, IOException.class);
      }

      ErrorPageServlet.attachCause(request, e);
      if (resp.isCommitted()) {
        log.debug("Response is committed, cannot change status", e);
        return;
      }
      response.setHeader(X_FRAME_OPTIONS, xFrameOptions.getValueForPath(request.getPathInfo()));

      int errorCode = SC_INTERNAL_SERVER_ERROR;
      if (e instanceof AuthenticationException || e.getCause() instanceof AuthenticationException
          || e instanceof NexusAuthenticationException || e.getCause() instanceof NexusAuthenticationException) {
        errorCode = SC_UNAUTHORIZED;
      }
      response.sendError(errorCode);
    }
  }
}
