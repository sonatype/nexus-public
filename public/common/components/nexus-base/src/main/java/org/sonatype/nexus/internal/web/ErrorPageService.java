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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.bootstrap.jetty.NexusReasonPhraseCustomizer;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.common.template.TemplateThrowableAdapter;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.servlet.XFrameOptions;

import jakarta.annotation.Nullable;
import org.eclipse.jetty.ee10.servlet.ServletContextResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * Service to write a rendered HTML error page
 */
@Component
public class ErrorPageService
    extends PageServiceSupport
{
  private static final String TEMPLATE_RESOURCE = "errorPageHtml.vm";

  @Autowired
  public ErrorPageService(final TemplateHelper templateHelper, final XFrameOptions xFrameOptions) {
    super(templateHelper, xFrameOptions, TEMPLATE_RESOURCE);
  }

  /**
   * Write an HTML error page to the provided {@link HttpServletResponse}
   *
   * @param errorInfo a description of the error
   * @param request the inbound request
   * @param response the response to write to
   *
   * @throws IOException when a failure occurs writing to the response
   */
  public void writeErrorResponse(
      final ErrorInfo errorInfo,
      final HttpServletRequest request,
      final HttpServletResponse response) throws IOException
  {
    log.debug("Writing error response for {}", errorInfo);

    Integer errorCode = errorInfo.code;
    String errorMessage = errorInfo.message;

    // this happens if someone browses directly to the error page
    if (errorCode == null) {
      errorCode = SC_NOT_FOUND;
      errorMessage = "Not found";
    }

    // maintain custom status message when (re)setting the status code,
    // we can't use sendError because it doesn't allow custom html body
    if (errorMessage == null) {
      response.setStatus(errorCode);
      errorMessage = "Unknown error";
    }
    else {
      // NEXUS-46395: HttpServletResponse.setStatus(int, String) was removed in Jakarta
      // Servlet 6; only setStatus(int) remains. The custom HTTP/1.1 reason phrase that
      // Firewall depends on is carried via a sentinel response header and serialized on the wire by
      // NexusReasonPhraseCustomizer (STL-476).
      response.setStatus(errorCode);
      setJettyReason(response, errorMessage);
    }

    TemplateParameters params = templateHelper.parameters();
    params.set("errorCode", errorCode);
    params.set("errorName", Status.fromStatusCode(errorCode).getReasonPhrase());
    params.set("errorDescription", errorMessage);

    // add cause if ?debug enabled and there is an exception
    if (errorInfo.cause != null && ServletHelper.isDebug(request)) {
      params.set("errorCause", new TemplateThrowableAdapter(errorInfo.cause));
    }

    writeResponseWithoutCaching(params, request, response);
  }

  /**
   * Records the desired HTTP/1.1 reason phrase as a sentinel response header (read by
   * {@link NexusReasonPhraseCustomizer}) so it is serialized on the wire.
   * {@link ServletContextResponse#getServletContextResponse(jakarta.servlet.ServletResponse)}
   * walks any {@code ServletResponseWrapper} chain (covers {@code ShiroHttpServletResponse} and any
   * other wrappers added by Shiro / servlet filters) before returning the EE10 ServletContextResponse,
   * from which we can reach the core {@code org.eclipse.jetty.server.Response}.
   *
   * <p>
   * If the response is not a Jetty response (e.g. test harness with a mocked
   * {@code HttpServletResponse}, or a hypothetical non-Jetty deployment), the helper throws
   * {@link IllegalStateException}; we log at debug and let the response go out without a custom
   * reason phrase rather than failing the request.
   */
  private void setJettyReason(final HttpServletResponse response, final String errorMessage) {
    try {
      ServletContextResponse.getServletContextResponse(response)
          .getResponse()
          .getHeaders()
          .put(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER, errorMessage);
    }
    catch (IllegalStateException e) {
      log.debug("Could not unwrap response to set HTTP reason phrase: {}", e.toString());
    }
  }

  /**
   * @param code the status code
   * @param message a description of the problem to be rendered to the user
   * @param cause an optional cause, will only be rendered when debug is enabled and allowed
   */
  public static record ErrorInfo(Integer code, String message, @Nullable Throwable cause)
  {
    public ErrorInfo(final Integer code, final String message) {
      this(code, message, null);
    }
  }
}
