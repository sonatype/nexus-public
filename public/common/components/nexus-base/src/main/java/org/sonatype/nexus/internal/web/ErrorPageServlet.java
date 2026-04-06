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
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.app.WebFilterPriority;
import org.sonatype.nexus.internal.web.ErrorPageService.ErrorInfo;

import com.google.common.base.Throwables;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;

/**
 * An {@code error.html} servlet to handle generic servlet error-page dispatched requests.
 *
 * @since 2.8
 *
 * @see ErrorPageFilter
 */
// needs to be first-most when calculating order (some fudge room for edge-cases)
@Order(WebFilterPriority.WEB)
@WebServlet("/error.html")
@Component
@Singleton
public class ErrorPageServlet
    extends HttpServlet
{
  private static final Logger log = LoggerFactory.getLogger(ErrorPageServlet.class);

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

  private final ErrorPageService errorPageService;

  @Inject
  public ErrorPageServlet(final ErrorPageService errorPageService) {
    this.errorPageService = checkNotNull(errorPageService);
  }

  @SuppressWarnings("unused")
  @Override
  protected void service(
      final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException
  {
    String servletName = (String) request.getAttribute(ERROR_SERVLET_NAME);
    String requestUri = (String) request.getAttribute(ERROR_REQUEST_URI);
    Integer errorCode = (Integer) request.getAttribute(ERROR_STATUS_CODE);
    String errorMessage = (String) request.getAttribute(ERROR_MESSAGE);
    Class<?> causeType = (Class<?>) request.getAttribute(ERROR_EXCEPTION_TYPE);
    Throwable cause = (Throwable) request.getAttribute(ERROR_EXCEPTION);

    log.trace("Handling errorCode {} for {} uri {} message '{}'", errorCode, servletName, requestUri, errorMessage,
        cause);

    errorPageService.writeErrorResponse(new ErrorInfo(errorCode, errorMessage, cause), request, response);
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
    else if (isEofException(cause)) {
      log.trace("Client terminated connection", cause);
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

  /**
   * Jetty throws an EofException when the client terminates the connection and the application attempts to write to the
   * servlets stream.
   *
   * @see EofException
   */
  private static boolean isEofException(final Throwable e) {
    if (e instanceof EofException || Throwables.getRootCause(e) instanceof EofException) {
      return true;
    }
    return Stream.of(e.getSuppressed())
        .anyMatch(t -> t instanceof EofException);
  }
}
