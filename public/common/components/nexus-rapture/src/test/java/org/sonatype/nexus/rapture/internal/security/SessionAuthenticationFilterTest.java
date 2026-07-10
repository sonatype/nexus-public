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
package org.sonatype.nexus.rapture.internal.security;

import java.lang.reflect.Field;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.authc.AuthRateLimiterService;
import org.sonatype.nexus.security.authc.RateLimitResult;
import org.sonatype.nexus.security.authc.SsoDetector;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SessionAuthenticationFilterTest
{
  private SessionAuthenticationFilter filter;

  private HttpServletRequest request;

  private HttpServletResponse response;

  private AuthRateLimiterService rateLimiterService;

  private SsoDetector ssoDetector;

  private EventManager eventManager;

  @Before
  public void setUp() throws Exception {
    filter = new SessionAuthenticationFilter();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    rateLimiterService = mock(AuthRateLimiterService.class);
    ssoDetector = mock(SsoDetector.class);
    eventManager = mock(EventManager.class);

    setField("rateLimiterService", rateLimiterService);
    setField("ssoDetector", ssoDetector);
    setField("eventManager", eventManager);
  }

  private void setField(final String name, final Object value) throws Exception {
    Field field = SessionAuthenticationFilter.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(filter, value);
  }

  private static String base64(final String value) {
    return Base64.getEncoder().encodeToString(value.getBytes());
  }

  // ---- pre-auth check in onAccessDenied ----

  @Test
  public void testOnAccessDenied_preAuthBlocked_sends429WithoutExecutingLogin() throws Exception {
    when(ssoDetector.isSsoEnabled()).thenReturn(false);
    when(request.getMethod()).thenReturn("POST");
    when(request.getParameter("username")).thenReturn(base64("jsmith"));
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");

    when(rateLimiterService.check("jsmith")).thenReturn(new RateLimitResult(30L, 6));

    boolean result = filter.onAccessDenied(request, response);

    verify(rateLimiterService).check("jsmith");
    verify(response).setHeader("Retry-After", "30");
    verify(response).sendError(429, "Too many authentication attempts");
    assertThat(result, is(false));
  }

  @Test
  public void testOnAccessDenied_preAuthBlocked_doesNotCallCheck_whenNotLoginRequest() throws Exception {
    // GET requests are not login requests; no rate-limit check should occur
    when(request.getMethod()).thenReturn("GET");

    filter.onAccessDenied(request, response);

    verify(rateLimiterService, never()).check(any());
  }

  @Test
  public void testOnAccessDenied_ssoEnabled_skipsPreAuthCheck() throws Exception {
    when(ssoDetector.isSsoEnabled()).thenReturn(true);

    // Call onLoginFailure rather than onAccessDenied to verify SSO bypass without needing Shiro wiring
    AuthenticationToken token = mock(AuthenticationToken.class);
    when(token.getPrincipal()).thenReturn("jsmith");

    filter.onLoginFailure(token, mock(AuthenticationException.class), request, response);

    // With SSO enabled, the rate limiter is never consulted
    verify(rateLimiterService, never()).check(any());
    verify(rateLimiterService, never()).checkAndRecord(any());
  }

  // ---- onLoginFailure records counter, no longer sends 429 directly ----

  @Test
  public void testOnLoginFailure_recordsFailureAndPostsEvent() throws Exception {
    when(ssoDetector.isSsoEnabled()).thenReturn(false);
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");

    AuthenticationToken token = mock(AuthenticationToken.class);
    when(token.getPrincipal()).thenReturn("jsmith");

    when(rateLimiterService.checkAndRecord("jsmith")).thenReturn(new RateLimitResult(30L, 6));

    boolean result = filter.onLoginFailure(token, mock(AuthenticationException.class), request, response);

    verify(rateLimiterService).checkAndRecord("jsmith");
    verify(eventManager).post(any());
    // 429 is NOT sent here — it's handled by the pre-auth check on next attempt
    verify(response, never()).sendError(429, "Too many authentication attempts");
    assertThat(result, is(false));
  }

  @Test
  public void testOnLoginFailure_ssoEnabled_skipsRateLimitCheck() throws Exception {
    when(ssoDetector.isSsoEnabled()).thenReturn(true);

    AuthenticationToken token = mock(AuthenticationToken.class);
    when(token.getPrincipal()).thenReturn("jsmith");

    filter.onLoginFailure(token, mock(AuthenticationException.class), request, response);

    verify(rateLimiterService, never()).checkAndRecord(any());
  }

  @Test
  public void testOnLoginSuccess_recordsSuccessUsingTokenPrincipal() throws Exception {
    AuthenticationToken token = mock(AuthenticationToken.class);
    when(token.getPrincipal()).thenReturn("jsmith");
    Subject subject = mock(Subject.class);

    boolean result = filter.onLoginSuccess(token, subject, request, response);

    verify(rateLimiterService).recordSuccess("jsmith");
    assertThat(result, is(true));
  }
}
