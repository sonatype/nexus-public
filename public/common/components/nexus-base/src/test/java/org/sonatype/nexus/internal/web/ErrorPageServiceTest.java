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

import org.sonatype.nexus.bootstrap.jetty.NexusReasonPhraseCustomizer;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.internal.web.ErrorPageService.ErrorInfo;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.servlet.XFrameOptions;

import org.eclipse.jetty.ee10.servlet.ServletContextResponse;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorPageServiceTest
{
  @Mock
  HttpServletRequest request;

  @Mock(answer = Answers.RETURNS_MOCKS)
  HttpServletResponse response;

  @Mock(answer = Answers.RETURNS_MOCKS)
  TemplateHelper templateHelper;

  @Mock
  XFrameOptions xFrameOptions;

  @Captor
  ArgumentCaptor<TemplateParameters> paramCaptor;

  @InjectMocks
  ErrorPageService underTest;

  // NEXUS-46395 / STL-476: HttpServletResponse.setStatus(int, String) was removed in Servlet 6,
  // and the production ErrorPageService now calls setStatus(int) only. The custom HTTP
  // reason phrase is set as a sentinel response header (read by NexusReasonPhraseCustomizer);
  // the propagation tests below exercise that path through the EE10 ServletContextResponse helper.
  @Test
  void testWriteErrorResponse() throws IOException {
    underTest.writeErrorResponse(new ErrorInfo(500, "Something Bad"), request, response);

    verify(response).setStatus(500);
    verify(templateHelper).render(any(), paramCaptor.capture());
    assertParameters(paramCaptor.getValue(), 500, "Internal Server Error", "Something Bad");
  }

  @Test
  void testWriteErrorResponse_noErrorMessage() throws IOException {
    underTest.writeErrorResponse(new ErrorInfo(500, null), request, response);

    verify(response).setStatus(500);
    verify(templateHelper).render(any(), paramCaptor.capture());
    assertParameters(paramCaptor.getValue(), 500, "Internal Server Error", "Unknown error");
  }

  @Test
  void testWriteErrorResponse_nullErrorCode() throws IOException {
    underTest.writeErrorResponse(new ErrorInfo(null, null), request, response);

    verify(response).setStatus(404);
    verify(templateHelper).render(any(), paramCaptor.capture());
    assertParameters(paramCaptor.getValue(), 404, "Not Found", "Not found");
  }

  /**
   * NEXUS-46395 / STL-476: the reason phrase the caller passes (e.g. "Quarantined" for the Firewall
   * block path) must be set as the sentinel response header read by NexusReasonPhraseCustomizer, so
   * it appears on the HTTP/1.1 status line. The EE10 unwrap helper
   * {@code ServletContextResponse.getServletContextResponse} walks through Shiro / wrapper chains to the
   * underlying Jetty response; we stub it here to assert the reason value flows through correctly.
   */
  @Test
  void testWriteErrorResponse_errorMessagePropagation() throws IOException {
    Response coreResponse = mock(Response.class);
    org.eclipse.jetty.http.HttpFields.Mutable coreHeaders = mock(org.eclipse.jetty.http.HttpFields.Mutable.class);
    when(coreResponse.getHeaders()).thenReturn(coreHeaders);
    ServletContextResponse contextResponse = mock(ServletContextResponse.class);
    when(contextResponse.getResponse()).thenReturn(coreResponse);

    try (MockedStatic<ServletContextResponse> staticMock = Mockito.mockStatic(ServletContextResponse.class)) {
      staticMock.when(() -> ServletContextResponse.getServletContextResponse(response))
          .thenReturn(contextResponse);

      underTest.writeErrorResponse(new ErrorInfo(403, "Quarantined"), request, response);
    }

    verify(response).setStatus(403);
    verify(coreHeaders).put(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER, "Quarantined");
  }

  /**
   * NEXUS-46395: when the response is not a Jetty response (test harness mocks, hypothetical
   * non-Jetty deployment) the unwrap helper throws {@link IllegalStateException}. The service
   * must log and continue, so the rendered error page still goes out — the only thing missing
   * is the (advisory) HTTP/1.1 reason phrase. Catches a future regression where someone
   * propagates the IllegalStateException and breaks every error page in test environments.
   */
  @Test
  void testWriteErrorResponse_nonJettyResponseDoesNotFail() throws IOException {
    try (MockedStatic<ServletContextResponse> staticMock = Mockito.mockStatic(ServletContextResponse.class)) {
      staticMock.when(() -> ServletContextResponse.getServletContextResponse(response))
          .thenThrow(new IllegalStateException("could not find ServletContextResponse for mock"));

      underTest.writeErrorResponse(new ErrorInfo(403, "Quarantined"), request, response);
    }

    verify(response).setStatus(403);
    verify(templateHelper).render(any(), paramCaptor.capture());
    assertParameters(paramCaptor.getValue(), 403, "Forbidden", "Quarantined");
  }

  @Test
  void testWriteErrorResponse_causeNoDebug() throws IOException {
    Exception error = new IllegalStateException();
    underTest.writeErrorResponse(new ErrorInfo(null, null, error), request, response);

    verify(templateHelper).render(any(), paramCaptor.capture());
    assertParameters(paramCaptor.getValue(), 404, "Not Found", "Not found");
  }

  @Test
  void testWriteErrorResponse_causeDebug() throws IOException {
    Exception error = new IllegalStateException();

    try (MockedStatic<ServletHelper> staticMock = Mockito.mockStatic(ServletHelper.class)) {
      staticMock.when(() -> ServletHelper.isDebug(request)).thenReturn(true);
      underTest.writeErrorResponse(new ErrorInfo(null, null, error), request, response);
    }

    verify(templateHelper).render(any(), paramCaptor.capture());
    TemplateParameters parameters = paramCaptor.getValue();
    verify(parameters).set(eq("errorCause"), any());
    assertParameters(parameters, 404, "Not Found", "Not found");
  }

  private static void assertParameters(
      final TemplateParameters actual,
      final int errorCode,
      final String errorName,
      final String errorMessage)
  {
    verify(actual).set("errorCode", errorCode);
    verify(actual).set("errorName", errorName);
    verify(actual).set("errorDescription", errorMessage);
    verifyNoMoreInteractions(actual);
  }
}
