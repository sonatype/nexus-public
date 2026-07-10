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
import java.io.PrintWriter;
import java.net.URL;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.servlet.XFrameOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;

/**
 * Support service to write a rendered HTML page
 */
public abstract class PageServiceSupport
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final TemplateHelper templateHelper;

  private final XFrameOptions xFrameOptions;

  private final URL template;

  protected PageServiceSupport(
      final TemplateHelper templateHelper,
      final XFrameOptions xFrameOptions,
      final String templateResource)
  {
    this.templateHelper = checkNotNull(templateHelper);
    this.xFrameOptions = checkNotNull(xFrameOptions);

    template = getClass().getResource(templateResource);
    checkNotNull(template);
  }

  /**
   * Writes a response with a header specifying caches should be disabled.
   *
   * @param params
   * @param request
   * @param response
   * @throws IOException
   */
  protected void writeResponseWithoutCaching(
      final TemplateParameters params,
      final HttpServletRequest request,
      final HttpServletResponse response) throws IOException
  {
    ServletHelper.addNoCacheResponseHeaders(response);
    writeResponseWithCaching(params, request, response);
  }

  /**
   * Writes a response and allows caching of the page.
   *
   * @param params the parameters to render
   * @param request the http request
   * @param response the http response
   * @throws IOException
   */
  protected void writeResponseWithCaching(
      final TemplateParameters params,
      final HttpServletRequest request,
      final HttpServletResponse response) throws IOException
  {
    writeHeaders(request, response);

    String html = templateHelper.render(template, params);

    try (PrintWriter out = response.getWriter()) {
      out.println(html);
    }
  }

  private void writeHeaders(final HttpServletRequest request, final HttpServletResponse response) {
    log.trace("Writing headers");
    response.setHeader(X_FRAME_OPTIONS, xFrameOptions.getValueForPath(request.getPathInfo()));
    response.setContentType("text/html");
  }
}
