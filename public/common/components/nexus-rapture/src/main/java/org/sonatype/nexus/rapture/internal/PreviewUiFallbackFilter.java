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
package org.sonatype.nexus.rapture.internal;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.WebFilterPriority.WEB_RESOURCES;

/**
 * SPA fallback filter for Preview UI path-based routing.
 *
 * Intercepts requests to /preview/* paths and forwards them to /index.html,
 * allowing the React router to handle client-side routing.
 *
 * This enables clean URLs like /preview/browse/welcome instead of
 * hash-based URLs like #preview/browse/welcome.
 *
 * @since 3.92.0
 */
@WebFilter(filterName = PreviewUiFallbackFilter.NAME, urlPatterns = "/preview/*")
@Order(WEB_RESOURCES - 100)
@Component
public class PreviewUiFallbackFilter
    implements Filter
{
  private static final Logger log = LoggerFactory.getLogger(PreviewUiFallbackFilter.class);

  public static final String NAME = "nx-preview-ui-fallback";

  private static final String ROOT_PATH = "/";

  @Override
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      String requestUri = httpRequest.getRequestURI();

      log.debug("Preview UI fallback: forwarding {} to {}", requestUri, ROOT_PATH);

      // Forward to root (/) for SPA routing - the React app handles all /preview/* routes
      request.getRequestDispatcher(ROOT_PATH).forward(request, response);
      return;
    }

    // Non-HTTP requests pass through
    chain.doFilter(request, response);
  }
}
