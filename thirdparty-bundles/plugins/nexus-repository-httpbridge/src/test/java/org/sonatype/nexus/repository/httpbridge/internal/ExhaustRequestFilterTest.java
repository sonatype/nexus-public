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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.apache.http.HttpHeaders.USER_AGENT;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;

/**
 * Tests for {@link ExhaustRequestFilter}
 */
public class ExhaustRequestFilterTest extends TestSupport
{
  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Mock
  javax.servlet.FilterChain filterChain;

  ExhaustRequestFilter filter;

  @Before
  public void setUp() throws Exception {
    filter = new ExhaustRequestFilter("Apache-Maven.*");
  }

  @Test
  public void httpOkResponse() throws Exception {
    setupMockResponse(OK, null, null);

    filter.doFilter(request, response, filterChain);

    verifyRequestNotExhausted();
  }

  @Test
  public void http400GetResponse() throws Exception {
    setupMockResponse(BAD_REQUEST, GET, null);

    filter.doFilter(request, response, filterChain);

    verifyRequestNotExhausted();
  }

  @Test
  public void http400PutResponse_NullUserAgent() throws Exception {
    setupMockResponse(BAD_REQUEST, PUT, null);

    filter.doFilter(request, response, filterChain);

    verifyRequestNotExhausted();
  }

  @Test
  public void http400PutResponse_NonMavenUserAgent() throws Exception {
    setupMockResponse(BAD_REQUEST, PUT, "notmaven");

    filter.doFilter(request, response, filterChain);

    verifyRequestNotExhausted();
  }

  @Test
  public void http400PutResponse_MavenUserAgent() throws Exception {
    setupMockResponse(BAD_REQUEST, PUT, "Apache-Maven.Foo");

    filter.doFilter(request, response, filterChain);

    verifyRequestExhausted();
  }

  private void setupMockResponse(final Integer status, final String method, final String userAgent) {
    when(response.getStatus()).thenReturn(status);
    when(request.getMethod()).thenReturn(method);
    when(request.getHeader(eq(USER_AGENT))).thenReturn(userAgent);
  }

  private void verifyRequestNotExhausted() throws IOException {
    verify(request, never()).getInputStream();
  }

  private void verifyRequestExhausted() throws IOException {
    verify(request).getInputStream();
  }
}
