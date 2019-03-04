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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.httpbridge.HttpResponseSender;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.FORBIDDEN;

/**
 * Tests for {@link DefaultHttpResponseSender}.
 */
public class DefaultHttpResponseSenderTest
    extends TestSupport
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

  @Test
  public void customStatusMessageIsMaintainedWithPayload() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);

    Payload detailedReason = new StringPayload("Please authenticate and try again", "text/plain");

    Response response = new Response.Builder()
        .status(Status.failure(FORBIDDEN, "You can't see this"))
        .payload(detailedReason).build();

    underTest.send(request, response, httpServletResponse);

    verify(httpServletResponse).setStatus(403, "You can't see this");
  }

}
