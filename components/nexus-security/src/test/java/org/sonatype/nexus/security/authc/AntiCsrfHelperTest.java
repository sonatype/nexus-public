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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.net.HttpHeaders;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AntiCsrfHelperTest
  extends TestSupport
{
  private static final String SESSION_COOKIE = "not-default-sessonid";

  private AntiCsrfHelper underTest;

  @Mock
  HttpServletRequest httpServletRequest;

  @Before
  public void setup() {
    underTest = new AntiCsrfHelper(true, SESSION_COOKIE);
  }

  /*
   * Test that an exception is not thrown when the CSRF protection is disabled.
   */
  @Test
  public void testRequireValidToken_Disabled() {
    underTest = new AntiCsrfHelper(false, SESSION_COOKIE);
    underTest.requireValidToken(httpServletRequest, "a-token");
    verifyZeroInteractions(httpServletRequest);
  }

  /*
   * Requests with a referrer set but no token are invalid.
   */
  @Test(expected = UnauthorizedException.class)
  public void testRequireValidToken_Referrer_NoToken() {
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    underTest.requireValidToken(httpServletRequest, null);
  }

  /*
   * Requests with a session but no token are invalid.
   */
  @Test(expected = UnauthorizedException.class)
  public void testRequireValidToken_Session_NoToken() {
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[] { new Cookie(SESSION_COOKIE, "avalue") });
    underTest.requireValidToken(httpServletRequest, null);
  }

  /*
   * Requests without a session and without a referrer are likely non-browser clients.
   */
  @Test
  public void testRequireValidToken_NoSessionAndNoReferrer() {
    underTest.requireValidToken(httpServletRequest, null);
  }

  /*
   * Requests with a session and matching tokens are valid.
   */
  @Test
  public void testRequireValidToken() {
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie(AntiCsrfHelper.ANTI_CSRF_TOKEN_NAME, "a-value") });
    underTest.requireValidToken(httpServletRequest, "a-value");
  }

  /*
   * Requests with a session and mismatch tokens are invalid.
   */
  @Test(expected = UnauthorizedException.class)
  public void testRequireValidToken_tokenMismatch() {
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie(AntiCsrfHelper.ANTI_CSRF_TOKEN_NAME, "a-value") });
    underTest.requireValidToken(httpServletRequest, "a-different-value");
  }

  /*
   * Requests with a session and mismatch tokens are invalid.
   */
  @Test(expected = UnauthorizedException.class)
  public void testRequireValidToken_missingCookie() {
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    underTest.requireValidToken(httpServletRequest, "a-different-value");
  }

  /*
   * Requests with a session and mismatch tokens are invalid.
   */
  @Test(expected = UnauthorizedException.class)
  public void testRequireValidToken_missingToken() {
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie(AntiCsrfHelper.ANTI_CSRF_TOKEN_NAME, "a-value") });
    underTest.requireValidToken(httpServletRequest, null);
  }

  /*
   * Test that the filter passes requests when disabled
   */
  @Test
  public void testIsAccessAllowed_Disabled() {
    underTest = new AntiCsrfHelper(false, SESSION_COOKIE);
    assertThat(underTest.isAccessAllowed(httpServletRequest), is(true));
    verifyZeroInteractions(httpServletRequest);
  }

  /*
   * Test that the filter allows requests with 'safe' HTTP methods without a token
   */
  @Test
  public void testIsAccessAllowed_SafeMethodsAllowed() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.HEAD);
    assertThat(underTest.isAccessAllowed(httpServletRequest), is(true));

    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);
    assertThat(underTest.isAccessAllowed(httpServletRequest), is(true));
  }

  /*
   * Test the behaviours for session & referrer. Requests without a session or referrer should be from non-browser
   * clients.
   */
  @Test
  public void testIsAccessAllowed_NoSessionAndNoReferrer() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    assertThat(underTest.isAccessAllowed(httpServletRequest), is(false));

    reset(httpServletRequest);
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[] { new Cookie(SESSION_COOKIE, "avalue") });
    assertThat(underTest.isAccessAllowed(httpServletRequest), is(false));

    reset(httpServletRequest);
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    assertThat(underTest.isAccessAllowed(httpServletRequest), is(true));
  }

  /*
   * Test that a request with a valid CSRF token is allowed
   */
  @Test
  public void testIsAccessAllowed_ValidCsrfToken() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    when(httpServletRequest.getHeader("NX-ANTI-CSRF-TOKEN")).thenReturn("avalue");
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie("NX-ANTI-CSRF-TOKEN", "avalue"), new Cookie(SESSION_COOKIE, "avalue") });

    assertThat(underTest.isAccessAllowed(httpServletRequest), is(true));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(2)).getCookies();
  }

  /*
   * Test that a request missing a CSRF cookie but with a header is rejected
   */
  @Test
  public void testIsAccessAllowed_MissingCsrfCookie() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getHeader("NX-ANTI-CSRF-TOKEN")).thenReturn("avalue");

    assertThat(underTest.isAccessAllowed(httpServletRequest), is(false));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(2)).getCookies();
  }

  /*
   * Test that a request missing a CSRF header but with a cookie is rejected
   */
  @Test
  public void testIsAccessAllowed_MissingCsrfHeader() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    // NX-ANTI-CSRF-TOKEN not set
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie("NX-ANTI-CSRF-TOKEN", "avalue"), new Cookie(SESSION_COOKIE, "avalue") });

    assertThat(underTest.isAccessAllowed(httpServletRequest), is(false));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(2)).getCookies();
  }

  /*
   * Test that a request with mismatched CSRF tokens is rejected
   */
  @Test
  public void testIsAccessAllowed_MismatchedCsrfToken() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getHeader("NX-ANTI-CSRF-TOKEN")).thenReturn("some-value");
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie("NX-ANTI-CSRF-TOKEN", "some-other-value"),
            new Cookie(SESSION_COOKIE, "avalue") });

    assertThat(underTest.isAccessAllowed(httpServletRequest), is(false));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(2)).getCookies();
  }
}
