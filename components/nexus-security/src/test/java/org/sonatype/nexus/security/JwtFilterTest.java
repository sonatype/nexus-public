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
package org.sonatype.nexus.security;

import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;

/**
 * Test for {@link JwtFilter}
 */
public class JwtFilterTest
    extends TestSupport
{
  private static final String OLD_JWT = "old-jwt";

  private static final String NEW_JWT = "new-jwt";

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @Mock
  private JwtHelper jwtHelper;

  private JwtFilter jwtFilter;

  @Before
  public void setupFilter() {
    this.jwtFilter = new JwtFilter(jwtHelper);
  }

  @Test
  public void testPreHandle_successfulRefresh() throws Exception {
    Cookie oldCookie = makeCookie(OLD_JWT);
    Cookie newCookie = makeCookie(NEW_JWT);
    Cookie[] cookies = new Cookie[] {oldCookie};

    when(jwtHelper.verifyAndRefreshJwtCookie(OLD_JWT)).thenReturn(Optional.of(newCookie));
    when(request.getCookies()).thenReturn(cookies);

    jwtFilter.preHandle(request, response);

    verify(response).addCookie(newCookie);
  }

  @Test
  public void testPreHandle_invalidJwt() throws Exception {
    Cookie oldCookie = makeCookie(OLD_JWT);
    Cookie[] cookies = new Cookie[] {oldCookie};

    when(jwtHelper.verifyAndRefreshJwtCookie(OLD_JWT)).thenReturn(Optional.empty());
    when(request.getCookies()).thenReturn(cookies);

    jwtFilter.preHandle(request, response);

    verifyZeroInteractions(response);
  }

  @Test
  public void testPreHandle_noJwtCookie() throws Exception {
    Cookie[] cookies = new Cookie[] {};
    when(request.getCookies()).thenReturn(cookies);

    jwtFilter.preHandle(request, response);

    verifyZeroInteractions(response);
  }

  private Cookie makeCookie(final String jwt) {
    Cookie cookie = new Cookie(JWT_COOKIE_NAME, jwt);
    cookie.setMaxAge(300);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    return cookie;
  }
}
