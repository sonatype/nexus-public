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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.common.template.TemplateThrowableAdapter;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.servlet.XFrameOptions;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * An {@code error.html} servlet to handle generic servlet error-page dispatched requests.
 *
 * @since 2.8
 *
 * @see ErrorPageFilter
 */
@Named
@Singleton
public class ErrorPageServlet
    extends HttpServlet
{
  private static final Logger log = LoggerFactory.getLogger(ErrorPageServlet.class);

  private static final String TEMPLATE_RESOURCE = "errorPageHtml.vm";

  /**
   * @since 3.0
   */
  private static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";

  /**
   * @since 3.0
   */
  private static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";

  /**
   * @since 3.0
   */
  private static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";

  /**
   * @since 3.0
   */
  private static final String ERROR_MESSAGE = "javax.servlet.error.message";

  /**
   * @since 3.0
   */
  private static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

  /**
   * @since 3.0
   */
  private static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

  private final TemplateHelper templateHelper;

  private final XFrameOptions xFrameOptions;

  private final URL template;

  @Inject
  public ErrorPageServlet(final TemplateHelper templateHelper, final XFrameOptions xFrameOptions) {
    this.templateHelper = checkNotNull(templateHelper);
    this.xFrameOptions = checkNotNull(xFrameOptions);
    template = getClass().getResource(TEMPLATE_RESOURCE);
    checkNotNull(template);
  }

  @SuppressWarnings("unused")
  @Override
  protected void service(
      final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException
  {
    ServletHelper.addNoCacheResponseHeaders(response);

    String servletName = (String) request.getAttribute(ERROR_SERVLET_NAME);
    String requestUri = (String) request.getAttribute(ERROR_REQUEST_URI);
    Integer errorCode = (Integer) request.getAttribute(ERROR_STATUS_CODE);
    String errorMessage = (String) request.getAttribute(ERROR_MESSAGE);
    Class<?> causeType = (Class<?>) request.getAttribute(ERROR_EXCEPTION_TYPE);
    Throwable cause = (Throwable) request.getAttribute(ERROR_EXCEPTION);

    // this happens if someone browses directly to the error page
    if (errorCode == null) {
      errorCode = SC_NOT_FOUND;
      errorMessage = "Not found";
    }

    // maintain custom status message when (re)setting the status code,
    // we can't use sendError because it doesn't allow custom html body
    if (errorMessage == null) {
      response.setStatus(errorCode);
    }
    else {
      response.setStatus(errorCode, errorMessage);
    }

    response.setHeader(X_FRAME_OPTIONS, xFrameOptions.getValueForPath(request.getPathInfo()));
    response.setContentType("text/html");

    // ensure sanity of passed in strings which are used to render html content
    String errorDescription = errorMessage != null ? StringEscapeUtils.escapeHtml(errorMessage) : "Unknown error";

    TemplateParameters params = templateHelper.parameters();
    params.set("errorCode", errorCode);
    params.set("errorName", Status.fromStatusCode(errorCode).getReasonPhrase());
    params.set("errorDescription", errorDescription);

    // add cause if ?debug enabled and there is an exception
    if (cause != null && ServletHelper.isDebug(request)) {
      params.set("errorCause", new TemplateThrowableAdapter(cause));
    }

    String html = templateHelper.render(template, params);
    try (PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream()))) {
      out.println(html);
    }
  }

  /**
   * Attach exception details to request.
   *
   * @since 3.0
   */
  static void attachCause(final HttpServletRequest request, final Throwable cause) {
    if (isJavaLangError(cause)) {
      // Log java.lang.Error exceptions at error level
      log.error("Unexpected exception", getRootCause(cause));
    }
    else {
      log.debug("Attaching cause", cause);
    }
    request.setAttribute(ERROR_EXCEPTION_TYPE, cause.getClass());
    request.setAttribute(ERROR_EXCEPTION, cause);
  }

  private static boolean isJavaLangError(final Throwable e) {
    return getRootCause(e) instanceof Error;
  }

}
