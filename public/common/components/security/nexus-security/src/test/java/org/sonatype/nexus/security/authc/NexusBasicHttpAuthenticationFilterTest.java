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
package org.sonatype.nexus.security.authc;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.datastore.api.DataAccessException;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class NexusBasicHttpAuthenticationFilterTest
{
  private NexusBasicHttpAuthenticationFilter filter;

  private HttpServletRequest request;

  private HttpServletResponse response;

  @Before
  public void setUp() {
    filter = new NexusBasicHttpAuthenticationFilter();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
  }

  @Test
  public void testInfrastructureExceptionReturns503() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("admin", "admin123");

    // Create wrapped database exception (as it would come from Shiro)
    DataAccessException dataAccessException = new DataAccessException("Database unavailable");
    AuthenticationException shiroException =
        new AuthenticationException("Authentication failed", dataAccessException);

    // Call onLoginFailure
    boolean result = filter.onLoginFailure(token, shiroException, request, response);

    // Verify Retry-After header was set
    verify(response).setHeader(eq("Retry-After"), eq("60"));
    // Verify 503 was sent
    verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        eq("Service temporarily unavailable"));
    // Verify method returned false (authentication failed, don't continue)
    assertThat(result, is(false));
  }

  @Test
  public void testNormalAuthenticationFailureReturns401() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("admin", "wrongpassword");

    // Normal authentication exception (wrong credentials)
    AuthenticationException shiroException =
        new AuthenticationException("Authentication failed",
            new IncorrectCredentialsException());

    // Call onLoginFailure - should delegate to super (which returns false, allowing Shiro to handle 401)
    boolean result = filter.onLoginFailure(token, shiroException, request, response);

    // Verify 503 was NOT sent
    verify(response, never()).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        eq("Service temporarily unavailable"));
    // Result should be false (super's behavior), which allows Shiro to send 401
    assertThat(result, is(false));
  }

  @Test
  public void testInfrastructureExceptionDeepInCauseChain() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("admin", "admin123");

    // Create a deeper cause chain to test walking
    DataAccessException dataAccessException = new DataAccessException("Database unavailable");
    RuntimeException wrapperException = new RuntimeException("Wrapper", dataAccessException);
    AuthenticationException shiroException =
        new AuthenticationException("Authentication failed", wrapperException);

    // Call onLoginFailure
    boolean result = filter.onLoginFailure(token, shiroException, request, response);

    // Verify Retry-After header was set
    verify(response).setHeader(eq("Retry-After"), eq("60"));
    // Verify 503 was sent (even though exception is deeper in chain)
    verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        eq("Service temporarily unavailable"));
    assertThat(result, is(false));
  }

  @Test
  public void testInfrastructureExceptionFallbackOnIOException() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("admin", "admin123");

    // Create wrapped database exception
    DataAccessException dataAccessException = new DataAccessException("Database unavailable");
    AuthenticationException shiroException =
        new AuthenticationException("Authentication failed", dataAccessException);

    // Mock sendError to throw IOException
    doThrow(new IOException("Network error")).when(response)
        .sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE), eq("Service temporarily unavailable"));

    // Call onLoginFailure
    boolean result = filter.onLoginFailure(token, shiroException, request, response);

    // Verify 503 was attempted
    verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        eq("Service temporarily unavailable"));
    // Result should be false (super's fallback behavior)
    assertThat(result, is(false));
  }
}
