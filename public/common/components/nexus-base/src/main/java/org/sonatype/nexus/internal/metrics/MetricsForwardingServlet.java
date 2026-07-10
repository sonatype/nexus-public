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
package org.sonatype.nexus.internal.metrics;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;

import org.sonatype.nexus.common.app.WebFilterPriority;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static jakarta.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;

/**
 * Forwards requests to the moved metrics resources
 */
@Order(WebFilterPriority.WEB)
@Component
@WebServlet(MetricsForwardingServlet.PATH)
public class MetricsForwardingServlet
    extends HttpServlet
{
  private static final String PREFIX = "/service/metrics/";

  private static final int PREFIX_LENGTH = PREFIX.length();

  public static final String PATH = PREFIX + "*";

  @Override
  protected void doGet(
      final HttpServletRequest req,
      final HttpServletResponse resp) throws ServletException, IOException
  {
    resp.setHeader(HttpHeaders.LOCATION, computePath(req));
    resp.setStatus(SC_MOVED_PERMANENTLY);
  }

  private static String computePath(final HttpServletRequest req) {
    return switch (req.getRequestURI().substring(PREFIX_LENGTH)) {
      case "healthcheck" -> req.getContextPath() + "/service/rest/v1/status/check";
      default -> req.getRequestURI().replace("/service/", "/service/rest/");
    };
  }
}
