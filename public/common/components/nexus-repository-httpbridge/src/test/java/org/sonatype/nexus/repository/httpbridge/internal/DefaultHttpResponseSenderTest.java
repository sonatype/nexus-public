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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.httpbridge.HttpResponseSender;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import org.apache.shiro.web.servlet.ShiroHttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletApiResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DefaultHttpResponseSender}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultHttpResponseSenderTest
{
  private static final byte[] TEST_CONTENT = "TEST CONTENT".getBytes(StandardCharsets.UTF_8);

  private final HttpResponseSender underTest = new DefaultHttpResponseSender();

  @Mock
  private Request request;

  @Mock
  private Payload payload;

  @Spy
  private InputStream input = new ByteArrayInputStream(TEST_CONTENT);

  @Mock
  private HttpServletResponse httpServletResponse;

  @Mock
  private ServletOutputStream output;

  @Before
  public void setUp() throws Exception {
    when(request.getHeaders()).thenReturn(new Headers());
    when(payload.openInputStream()).thenReturn(input);
    when(httpServletResponse.getOutputStream()).thenReturn(output);
  }

  @Test
  public void payloadClosedAfterNullRequest() throws Exception {

    underTest.send(null, HttpResponses.ok(payload), httpServletResponse);

    InOrder order = inOrder(payload, input);

    order.verify(payload).getContentType();
    order.verify(payload, atLeastOnce()).getSize();
    order.verify(payload).close();

    order.verifyNoMoreInteractions();
  }

  @Test
  public void payloadClosedAfterHEAD() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.HEAD);

    underTest.send(request, HttpResponses.ok(payload), httpServletResponse);

    InOrder order = inOrder(payload, input);

    order.verify(payload).getContentType();
    order.verify(payload, atLeastOnce()).getSize();
    order.verify(payload).close();

    order.verifyNoMoreInteractions();
  }

  @Test
  public void payloadClosedAfterGET() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);

    underTest.send(request, HttpResponses.ok(payload), httpServletResponse);

    InOrder order = inOrder(payload, input);

    order.verify(payload).getContentType();
    order.verify(payload, atLeastOnce()).getSize();
    order.verify(payload).openInputStream();
    order.verify(input).close();
    order.verify(payload).close();

    order.verifyNoMoreInteractions();
  }

  @Test
  public void payloadClosedAfterError() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);

    doThrow(new IOException("Dropped")).when(payload).copy(input, output);

    try {
      underTest.send(request, HttpResponses.ok(payload), httpServletResponse);
      fail("Expected IOException");
    }
    catch (IOException e) {
      assertThat(e.getMessage(), is("Dropped"));
    }

    InOrder order = inOrder(payload, input);

    order.verify(payload).getContentType();
    order.verify(payload, atLeastOnce()).getSize();
    order.verify(payload).openInputStream();
    order.verify(input).close();
    order.verify(payload).close();

    order.verifyNoMoreInteractions();
  }

  @Test
  public void customStatusMessageIsMaintained() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);

    underTest.send(request, HttpResponses.forbidden("You can't see this"), httpServletResponse);

    verify(httpServletResponse).sendError(403, "You can't see this");
  }

  /**
   * NEXUS-46395: when an error response carries a payload (e.g. Repository Firewall
   * quarantine bodies returning 403/409 with an RFC 9457 / format-specific report),
   * the {@code sendError} path is bypassed. We must still forward the custom HTTP/1.1
   * reason phrase to Jetty's core {@link org.eclipse.jetty.server.Response#setReason}
   * so HTTP/1.1 clients (notably Maven 3.9.x) can read it off the status line.
   */
  @Test
  public void customStatusMessageWithPayloadIsForwardedToCoreJettyResponse() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);

    ServletApiResponse servletApiResponse = org.mockito.Mockito.mock(ServletApiResponse.class);
    org.eclipse.jetty.server.Response coreResponse =
        org.mockito.Mockito.mock(org.eclipse.jetty.server.Response.class);
    when(servletApiResponse.getStatus()).thenReturn(403);
    when(servletApiResponse.getOutputStream()).thenReturn(output);
    when(servletApiResponse.getResponse()).thenReturn(coreResponse);

    Response response = new Response.Builder()
        .status(Status.failure(403, "package-quarantined"))
        .payload(payload)
        .build();

    underTest.send(request, response, servletApiResponse);

    verify(servletApiResponse).setStatus(403);
    verify(coreResponse).setReason("package-quarantined");
  }

  /**
   * NEXUS-53114: in production the servlet response handed to
   * {@link DefaultHttpResponseSender} is wrapped by Shiro's
   * {@link ShiroHttpServletResponse}. Verify the unwrap path traverses Shiro and
   * still forwards the custom reason phrase to the core Jetty
   * {@link org.eclipse.jetty.server.Response}.
   */
  @Test
  public void customStatusMessageWithPayload_unwrapsThroughShiro() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);

    ServletApiResponse servletApiResponse = org.mockito.Mockito.mock(ServletApiResponse.class);
    org.eclipse.jetty.server.Response coreResponse =
        org.mockito.Mockito.mock(org.eclipse.jetty.server.Response.class);
    when(servletApiResponse.getResponse()).thenReturn(coreResponse);

    // ShiroHttpServletResponse is a concrete class that wraps an HttpServletResponse.
    // Build a real instance so the production-shape unwrap (ShiroHttpServletResponse
    // -> HttpServletResponseWrapper(s) -> ServletApiResponse) is exercised end-to-end.
    ShiroHttpServletResponse shiroResponse =
        new ShiroHttpServletResponse(servletApiResponse, null, null);
    when(servletApiResponse.getOutputStream()).thenReturn(output);

    Response response = new Response.Builder()
        .status(Status.failure(403, "package-quarantined"))
        .payload(payload)
        .build();

    underTest.send(request, response, shiroResponse);

    // setStatus is called on the outer ShiroHttpServletResponse, which delegates to
    // the wrapped servletApiResponse.
    verify(servletApiResponse).setStatus(403);
    verify(coreResponse).setReason("package-quarantined");
  }
}
