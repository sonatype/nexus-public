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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.internal.web.ErrorPageService.ErrorInfo;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.servlet.XFrameOptions;

import org.apache.shiro.web.servlet.ShiroHttpServletResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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

  @Test
  void testWriteErrorResponse() throws IOException {
    underTest.writeErrorResponse(new ErrorInfo(500, "Something Bad"), request, response);

    verify(response).setStatus(500, "Something Bad");
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

    verify(response).setStatus(404, "Not found");
    verify(templateHelper).render(any(), paramCaptor.capture());
    assertParameters(paramCaptor.getValue(), 404, "Not Found", "Not found");
  }

  @Test
  void testWriteErrorResponse_errorMessagePropagation() throws IOException {
    org.eclipse.jetty.ee8.nested.Response jettyResponse = mock(Answers.RETURNS_MOCKS);

    underTest.writeErrorResponse(new ErrorInfo(403, "Quarantined"), request, jettyResponse);

    verify(jettyResponse).setStatusWithReason(403, "Quarantined");
  }

  @Test
  void testWriteErrorResponse_wrappedErrorMessagePropagation() throws IOException {
    ShiroHttpServletResponse shiroResponse = mock(Answers.RETURNS_MOCKS);
    org.eclipse.jetty.ee8.nested.Response jettyResponse = mock();
    when(shiroResponse.getResponse()).thenReturn(jettyResponse);

    underTest.writeErrorResponse(new ErrorInfo(403, "Quarantined"), request, shiroResponse);

    verify(shiroResponse).getResponse();
    verify(jettyResponse).setStatusWithReason(403, "Quarantined");
  }

  @Test
  void testWriteErrorResponse_nestedWrappedErrorMessagePropagation() throws IOException {
    ShiroHttpServletResponse shiroResponse = mock(Answers.RETURNS_MOCKS);
    HttpServletResponseWrapper wrappedResponse = mock();
    when(shiroResponse.getResponse()).thenReturn(wrappedResponse);
    org.eclipse.jetty.ee8.nested.Response jettyResponse = mock();
    when(wrappedResponse.getResponse()).thenReturn(jettyResponse);

    underTest.writeErrorResponse(new ErrorInfo(403, "Quarantined"), request, shiroResponse);

    verify(shiroResponse).getResponse();
    verify(wrappedResponse).getResponse();
    verify(jettyResponse).setStatusWithReason(403, "Quarantined");
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
