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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.web.TemplateRenderer;
import org.sonatype.nexus.web.WebUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@code error.html} servlet to handle generic servlet error-page dispatched requests.
 *
 * @since 2.8
 */
@Named
@Singleton
public class ErrorPageServlet
    extends HttpServlet
{
  private final TemplateRenderer templateRenderer;

  private final WebUtils webUtils;

  @Inject
  public ErrorPageServlet(final TemplateRenderer templateRenderer,
                          final WebUtils webUtils)
  {
    this.templateRenderer = checkNotNull(templateRenderer);
    this.webUtils = checkNotNull(webUtils);
  }

  @Override
  protected void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    webUtils.addNoCacheResponseHeaders(response);

    Integer errorCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    String errorMessage = (String) request.getAttribute("javax.servlet.error.message");
    Throwable cause = (Throwable) request.getAttribute("javax.servlet.error.exception");

    // this happens if someone browses directly to the error page
    if (errorCode == null) {
      errorCode = 404;
      errorMessage = "Not found";
    }

    // error message must always be non-null when rendering
    if (errorMessage == null) {
      errorMessage = "Unknown error";
    }

    templateRenderer.renderErrorPage(
        request,
        response,
        errorCode,
        null, // default reason phrase will be used
        errorMessage,
        cause
    );
  }
}
