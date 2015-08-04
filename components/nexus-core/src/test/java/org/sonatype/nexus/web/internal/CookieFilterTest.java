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
package org.sonatype.nexus.web.internal;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CookieFilterTest
    extends TestSupport
{

  private static String COOKIE_1_INSECURE = "JSESSIONID=98a766bc-bc33-4b3c-9d9f-d3bb85b0cf00; Path=/nexus; HttpOnly";

  private static String COOKIE_2_INSECURE = "simple=cookie";

  private static String COOKIE_3_SECURE = "NXSESSIONID=98a766bc-bc33-4b3c-9d9f-d3bb85b0cf00; Path=/nexus; HttpOnly; Secure";

  private static String COOKIE_4_SECURE = "rememberMe=deleteMe; Path=/nexus; Secure; HttpOnly;";

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @Mock
  private FilterChain filterChain;

  @Mock
  private ServletResponse notInstanceOfHttpServletResponse;

  private CookieFilter cookieFilter;

  @Before
  public void setUp() throws Exception {
    this.cookieFilter = new CookieFilter();
  }

  @Test
  public void ifRequestIsSecureSetSecureCookieAttributeWhenMissing() throws Exception {
    when(request.isSecure()).thenReturn(true);
    when(response.getHeaders("Set-Cookie")).thenReturn(asList(COOKIE_1_INSECURE, COOKIE_2_INSECURE, COOKIE_3_SECURE,
        COOKIE_4_SECURE));

    cookieFilter.doFilter(request, response, filterChain);

    verify(response).setHeader("Set-Cookie", COOKIE_1_INSECURE + "; Secure");
    verify(response).addHeader("Set-Cookie", COOKIE_2_INSECURE + "; Secure");
    verify(response).addHeader("Set-Cookie", COOKIE_3_SECURE);
    verify(response).addHeader("Set-Cookie", COOKIE_4_SECURE);
  }

  @Test
  public void ifRequestIsNotSecureDoNotChangeCookieAttributes() throws Exception {
    when(request.isSecure()).thenReturn(false);
    when(response.getHeaders("Set-Cookie"))
        .thenReturn(asList(COOKIE_1_INSECURE, COOKIE_3_SECURE));

    cookieFilter.doFilter(request, response, filterChain);

    verifyZeroInteractions(response);
  }

  @Test
  public void ifNotInstanceOfHttpServletResponseDoNotProcessResponse() throws Exception {
    cookieFilter.doFilter(request, notInstanceOfHttpServletResponse, filterChain);

    verifyZeroInteractions(response);
  }

  @Test
  public void ifNoCookieHeadersDoNotChangeTheResponse() throws Exception {
    when(request.isSecure()).thenReturn(true);

    cookieFilter.doFilter(request, response, filterChain);

    verify(response, times(0)).setHeader(anyString(), anyString());
    verify(response, times(0)).addHeader(anyString(), anyString());
  }

}