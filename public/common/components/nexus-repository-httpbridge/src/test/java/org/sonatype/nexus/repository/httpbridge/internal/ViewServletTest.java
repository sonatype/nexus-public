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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.repository.BadRequestException;
import org.sonatype.nexus.repository.httpbridge.internal.describe.Description;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionHelper;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionRenderer;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.CaptureLogsFor;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.TestLogAccessor;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.event.Level;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.logLevel;

/**
 * Tests for describe functionality of {@link ViewServlet}.
 */
@ExtendWith(LoggingExtension.class)
class ViewServletTest
    extends Test5Support
{
  @CaptureLogsFor(ViewServlet.class)
  TestLogAccessor log;

  @Mock
  private Request request;

  @Mock
  private ViewFacet facet;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private HttpServletResponse servletResponse;

  @Mock
  private DescriptionRenderer descriptionRenderer;

  @Mock(name = "facet-response", answer = RETURNS_DEEP_STUBS)
  private Response facetResponse;

  @Mock(name = "facet-exception", answer = RETURNS_DEEP_STUBS)
  private RuntimeException facetException;

  @Mock
  private HttpServletRequest httpServletRequest;

  private Parameters parameters;

  private DefaultHttpResponseSender defaultResponseSender;

  private ViewServlet underTest;

  @BeforeEach
  void setUp() throws Exception {
    defaultResponseSender = spy(new DefaultHttpResponseSender());

    lenient().when(descriptionRenderer.renderHtml(any(Description.class))).thenReturn("HTML");
    lenient().when(descriptionRenderer.renderJson(any(Description.class))).thenReturn("JSON");

    underTest = spy(new ViewServlet(mock(RepositoryManager.class),
        new HttpResponseSenderSelector(List.of(), defaultResponseSender),
        mock(DescriptionHelper.class),
        descriptionRenderer));

    lenient().when(request.getPath()).thenReturn("/test");

    parameters = new Parameters();
    lenient().when(request.getParameters()).thenReturn(parameters);

    BaseUrlHolder.set("http://placebo", "");
  }

  private void descriptionRequested(final String describe) {
    if (describe == null) {
      parameters.remove(ViewServlet.P_DESCRIBE);
    }
    else {
      parameters.set(ViewServlet.P_DESCRIBE, describe);
    }
  }

  @Test
  void describeRequestReturnsDescriptionResponse_HTML() throws Exception {
    descriptionRequested("HTML");
    facetThrowsException(false);

    underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse);

    verify(underTest).describe(request, facetResponse, null, "HTML");
    verify(underTest).send(eq(request), any(Response.class), eq(servletResponse));
    verify(servletResponse).setContentType(ContentTypes.TEXT_HTML);
  }

  @Test
  void describeRequestReturnsDescriptionResponse_JSON() throws Exception {
    descriptionRequested("JSON");
    facetThrowsException(false);

    underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse);

    verify(underTest).describe(request, facetResponse, null, "JSON");
    verify(underTest).send(eq(request), any(Response.class), eq(servletResponse));
    verify(servletResponse).setContentType(ContentTypes.APPLICATION_JSON);
  }

  @Test
  void facetExceptionsReturnedNormally() throws Exception {
    descriptionRequested(null);
    facetThrowsException(true);

    assertThrows(RuntimeException.class,
        () -> underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse));
  }

  @Test
  void facetExceptionsAreDescribed() throws Exception {
    descriptionRequested("HTML");
    facetThrowsException(true);

    underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse);

    // The exception got described
    verify(underTest).describe(request, null, facetException, "HTML");
    verify(underTest).send(eq(request), any(Response.class), eq(servletResponse));
  }

  @Test
  void return400BadRequestOnBadRequestException() throws Exception {
    String message = "message";
    when(httpServletRequest.getPathInfo()).thenThrow(new BadRequestException(message));
    underTest.service(httpServletRequest, servletResponse);
    verify(servletResponse).sendError(SC_BAD_REQUEST, message);
  }

  @Test
  void responseHasContentSecurityPolicy() throws Exception {
    underTest.service(httpServletRequest, servletResponse);

    assertThat(ViewServlet.SANDBOX, containsString("sandbox"));
    verify(servletResponse).setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, ViewServlet.SANDBOX);
  }

  @Test
  void responseHasXssProtectionDisabled() throws Exception {
    underTest.service(httpServletRequest, servletResponse);

    verify(servletResponse).setHeader(HttpHeaders.X_XSS_PROTECTION, "0");
  }

  @Test
  void testEofException() throws ServletException, IOException {
    when(httpServletRequest.getPathInfo()).thenReturn("maven-central/some/path");

    doThrow(new EofException()).when(defaultResponseSender).send(any(), any(), any());

    assertThrows(EofException.class, () -> underTest.service(httpServletRequest, servletResponse));

    // EofException is the underlying cause
    doThrow(new IllegalStateException(new EofException())).when(defaultResponseSender).send(any(), any(), any());
    assertThrows(IllegalStateException.class, () -> underTest.service(httpServletRequest, servletResponse));

    // EofException is suppressed
    IOException re = new IOException();
    re.addSuppressed(new EofException());
    doThrow(re).when(defaultResponseSender).send(any(), any(), any());
    assertThrows(IOException.class, () -> underTest.service(httpServletRequest, servletResponse));

    assertThat(log.logs(), not(hasItem(logLevel(Level.WARN))));
  }

  private void facetThrowsException(final boolean facetThrowsException) throws Exception {
    if (facetThrowsException) {
      when(facet.dispatch(request)).thenThrow(facetException);
    }
    else {
      when(facet.dispatch(request)).thenReturn(facetResponse);
    }
  }
}
