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
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.net.HttpHeaders;
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

public class AntiCsrfFilterTest
  extends TestSupport
{
  private static final String SESSION_COOKIE = "not-default-sessonid";

  private static final String BROWSER_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";

  private static final String CLIENT_UA = "";

  private AntiCsrfFilter underTest;

  @Mock
  HttpServletRequest httpServletRequest;

  @Mock
  HttpServletResponse httpServletResponse;

  @Before
  public void setup() {
    underTest = new AntiCsrfFilter(true, SESSION_COOKIE);
  }

  /*
   * Test that the filter passes requests when disabled
   */
  @Test
  public void testDisabledFilter() {
    underTest = new AntiCsrfFilter(false, SESSION_COOKIE);
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
    verifyZeroInteractions(httpServletRequest);
    verifyZeroInteractions(httpServletResponse);
  }

  /*
   * Test that the filter allows requests with 'safe' HTTP methods without a token
   */
  @Test
  public void testSafeMethodsAllowed() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.HEAD);
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));

    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
  }

  /*
   * Test that the filter does not reject requests without a session and no referrer header set
   */
  @Test
  public void testNoSessionAndNoReferrer() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(BROWSER_UA);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(false));

    reset(httpServletRequest);
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(BROWSER_UA);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[] { new Cookie(SESSION_COOKIE, "avalue") });
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(false));

    reset(httpServletRequest);
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(BROWSER_UA);
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
  }

  /*
   * Test that a request with a valid CSRF token is allowed
   */
  @Test
  public void testValidCsrfToken() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("http://localhost:8081");
    when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(BROWSER_UA);
    when(httpServletRequest.getHeader("NX-ANTI-CSRF-TOKEN")).thenReturn("avalue");
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie("NX-ANTI-CSRF-TOKEN", "avalue"), new Cookie(SESSION_COOKIE, "avalue") });

    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(2)).getCookies();
  }

  /*
   * Test that a request missing a CSRF cookie but with a non browser User-Agent is Accepted
   */
  @Test
  public void testNonBrowserUserAgent() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(CLIENT_UA);

    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(0)).getCookies();
  }

  /*
   * Test that a request missing a CSRF cookie but with a header is rejected
   */
  @Test
  public void testMissingCsrfCookie() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getHeader("NX-ANTI-CSRF-TOKEN")).thenReturn("avalue");
    when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(BROWSER_UA);

    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(false));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(2)).getCookies();
  }

  /*
   * Test that a request missing a CSRF header but with a cookie is rejected
   */
  @Test
  public void testMissingCsrfHeader() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(BROWSER_UA);
    // NX-ANTI-CSRF-TOKEN not set
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie("NX-ANTI-CSRF-TOKEN", "avalue"), new Cookie(SESSION_COOKIE, "avalue") });

    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(false));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(2)).getCookies();
  }

  /*
   * Test that a request with mismatched CSRF tokens is rejected
   */
  @Test
  public void testMismatchedCsrfToken() {
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getHeader("NX-ANTI-CSRF-TOKEN")).thenReturn("some-value");
    when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(BROWSER_UA);
    when(httpServletRequest.getCookies())
        .thenReturn(new Cookie[] { new Cookie("NX-ANTI-CSRF-TOKEN", "some-other-value"),
            new Cookie(SESSION_COOKIE, "avalue") });

    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(false));

    // simple validation, we expect the code to access the cookies twice
    verify(httpServletRequest, times(2)).getCookies();
  }
}
