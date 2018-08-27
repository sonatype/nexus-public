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
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AntiCsrfFilterTest
  extends TestSupport
{
  private AntiCsrfFilter underTest;

  @Mock
  HttpServletRequest httpServletRequest;

  @Mock
  HttpServletResponse httpServletResponse;

  @Before
  public void setup() {
    underTest = new AntiCsrfFilter(true);
  }

  @Test
  public void testDisabledFilter() {
    underTest = new AntiCsrfFilter(false);
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
    verifyZeroInteractions(httpServletRequest);
    verifyZeroInteractions(httpServletResponse);
  }

  @Test
  public void testCsrfUnwarrantedRequest_ignoredUserAgent() {
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("notabrowser");
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
    verifyZeroInteractions(httpServletResponse);
  }

  @Test
  public void testCsrfCookieCreatedWhenNotAvailableInRequest() {
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/something");
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getContextPath()).thenReturn("something");
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(false));
    verify(httpServletResponse).addHeader(eq("Set-Cookie"), startsWith("NX-ANTI-CSRF-TOKEN"));
  }

  @Test
  public void testCsrfCookieNotCreatedWhenAvailableInRequest() {
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/something");
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getContextPath()).thenReturn("something");
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("NX-ANTI-CSRF-TOKEN", "avalue")});
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(false));
    verify(httpServletResponse, never()).addHeader(eq("Set-Cookie"), startsWith("NX-ANTI-CSRF-TOKEN"));
  }

  @Test
  public void testCookieCreatedAndCsrfCheckSkippedForFormPost() {
    //csrf check is skipped as it is done in the directnjine code
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/something");
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getContextPath()).thenReturn("something");
    when(httpServletRequest.getContentType()).thenReturn("multipart/form-data");
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
    verify(httpServletResponse).addHeader(eq("Set-Cookie"), startsWith("NX-ANTI-CSRF-TOKEN"));
  }

  @Test
  public void testCookieCreatedAndCsrfCheckSkippedForGetMethod() {
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/something");
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getContextPath()).thenReturn("something");
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
    verify(httpServletResponse).addHeader(eq("Set-Cookie"), startsWith("NX-ANTI-CSRF-TOKEN"));
  }

  @Test
  public void testCookieCreatedAndCsrfCheckSkippedForHeadMethod() {
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/something");
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.HEAD);
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getContextPath()).thenReturn("something");
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
    verify(httpServletResponse).addHeader(eq("Set-Cookie"), startsWith("NX-ANTI-CSRF-TOKEN"));
  }

  @Test
  public void testCookieCreatedAndCsrfCheckSkippedForMissingSessionAndReferrer() {
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/something");
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getContextPath()).thenReturn("something");
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
    verify(httpServletResponse).addHeader(eq("Set-Cookie"), startsWith("NX-ANTI-CSRF-TOKEN"));
  }

  @Test
  public void testCookieCreatedAndCsrfCheckPerformedButFails() {
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/something");
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getContextPath()).thenReturn("something");
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(false));
    verify(httpServletResponse).addHeader(eq("Set-Cookie"), startsWith("NX-ANTI-CSRF-TOKEN"));
  }

  @Test
  public void testCookieNotCreatedAndCsrfCheckPerformedAndPassesWithExistingCookie() {
    when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/something");
    when(httpServletRequest.getHeader("NX-ANTI-CSRF-TOKEN")).thenReturn("avalue");
    when(httpServletRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(httpServletRequest.getContextPath()).thenReturn("something");
    when(httpServletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("referrer");
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("NX-ANTI-CSRF-TOKEN", "avalue")});
    assertThat(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null), is(true));
    verify(httpServletResponse, never()).addHeader(eq("Set-Cookie"), startsWith("NX-ANTI-CSRF-TOKEN"));
  }
}
