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
package org.sonatype.nexus.web.internal;

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

import org.sonatype.nexus.web.ErrorStatusException;
import org.sonatype.nexus.web.TemplateRenderer;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * Servlet filter to add error page rendering.
 *
 * @since 2.8
 */
@Named
@Singleton
public class ErrorPageFilter
    extends ComponentSupport
    implements Filter
{
  private final TemplateRenderer templateRenderer;

  @Inject
  public ErrorPageFilter(final TemplateRenderer templateRenderer) {
    this.templateRenderer = checkNotNull(templateRenderer);
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
  public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
      throws IOException, ServletException
  {
    final HttpServletRequest request = (HttpServletRequest) req;
    final HttpServletResponse response = (HttpServletResponse) resp;
    try {
      chain.doFilter(req, response);
    }
    catch (ErrorStatusException e) {
      // send for direct rendering, everything is prepared
      templateRenderer.renderErrorPage(
          request,
          response,
          e.getResponseCode(),
          e.getReasonPhrase(),
          messageOf(e),
          e.getCause()
      );
    }
    catch (IOException e) {
      // IOException handling, do not leak information nor render error page
      log.error("Internal error", e);
      response.setStatus(SC_INTERNAL_SERVER_ERROR);
    }
    catch (Exception e) {
      // runtime and servlet exceptions will end here (thrown probably by some non-nexus filter or servlet)
      log.warn("Unexpected exception", e);
      templateRenderer.renderErrorPage(
          request,
          response,
          SC_INTERNAL_SERVER_ERROR,
          null, // default reason phrase will be used
          messageOf(e),
          e
      );
    }
  }

  /**
   * Returns the message of given throwable, or if message is null will toString throwable.
   */
  private static String messageOf(final Throwable cause) {
    String message = cause.getMessage();
    if (message == null) {
      return cause.toString();
    }
    return message;
  }
}
