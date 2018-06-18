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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.verification.VerificationMode;

import static org.apache.http.HttpHeaders.USER_AGENT;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
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
  private static final String PIPE_DELIMITED_MATCHING_PATTERN = "Apache-Maven.*|Apache Ivy.*";

  private static final String UNFORTUNATELY_SUPPORTED_COMMA_DELIMITED_MATCHING_PATTERN =
      "Apache-Maven.*\\s,\\sApache Ivy.*";

  private static final String NULL_USER_AGENT = null;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Mock
  javax.servlet.FilterChain filterChain;

  @Test
  public void httpOkResponse() throws Exception {
    verifyRequestNotExhausted(OK, PUT, "Apache-Maven.Foo");
  }

  @Test
  public void http400GetResponse() throws Exception {
    verifyRequestNotExhausted(BAD_REQUEST, GET, "Apache-Maven.Foo");
  }

  @Test
  public void http400PutResponse_NullUserAgent() throws Exception {
    verifyRequestNotExhausted(BAD_REQUEST, PUT, NULL_USER_AGENT);
  }

  @Test
  public void http400PutResponse_NonMavenOrIvyUserAgent() throws Exception {
    verifyRequestNotExhausted(BAD_REQUEST, PUT, "notmavenorivy");
  }

  @Test
  public void http400PutResponse_MavenUserAgent() throws Exception {
    verifyRequestExhausted(BAD_REQUEST, PUT, "Apache-Maven.Foo");
  }

  @Test
  public void http400PutResponse_IvyUserAgent() throws Exception {
    verifyRequestExhausted(BAD_REQUEST, PUT, "Apache Ivy.Foo");
  }

  private void verifyRequestNotExhausted(final Integer status, final String method, final String userAgent)
      throws Exception
  {
    verifyRequestExhaustionStatus(status, method, userAgent, never());
  }

  private void verifyRequestExhausted(final Integer status, final String method, final String userAgent)
      throws Exception
  {
    verifyRequestExhaustionStatus(status, method, userAgent, times(1));
  }

  private void verifyRequestExhaustionStatus(final Integer status, final String method, final String userAgent,
                                             final VerificationMode verificationMode) throws Exception
  {
    doVerifyRequestExhaustionStatus(status, method, userAgent, PIPE_DELIMITED_MATCHING_PATTERN, verificationMode);
    doVerifyRequestExhaustionStatus(
        status, method, userAgent, UNFORTUNATELY_SUPPORTED_COMMA_DELIMITED_MATCHING_PATTERN, verificationMode);
  }

  private void doVerifyRequestExhaustionStatus(final Integer status, final String method, final String userAgent,
                                               final String exhaustForAgents, final VerificationMode verificationMode)
      throws Exception
  {
    setupMockResponse(status, method, userAgent);
    new ExhaustRequestFilter(exhaustForAgents).doFilter(request, response, filterChain);
    verify(request, verificationMode).getInputStream();
  }

  private void setupMockResponse(final Integer status, final String method, final String userAgent) {
    reset(request, response);
    when(response.getStatus()).thenReturn(status);
    when(request.getMethod()).thenReturn(method);
    when(request.getHeader(eq(USER_AGENT))).thenReturn(userAgent);
  }
}
