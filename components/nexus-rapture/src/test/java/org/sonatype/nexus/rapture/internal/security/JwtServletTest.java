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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.SET_COOKIE;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;

public class JwtServletTest
    extends TestSupport
{
  @Mock
  private HttpServletRequest httpServletRequest;

  @Mock
  private HttpServletResponse httpServletResponse;

  @Mock
  private Subject subject;

  private JwtServlet underTest;

  @Before
  public void setup() {
    underTest = new JwtServlet("/");
    when(subject.isAuthenticated()).thenReturn(true);
    ThreadContext.bind(subject);
  }

  @After
  public void cleanup() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void testDoPost() throws Exception {
    underTest.doPost(httpServletRequest, httpServletResponse);

    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, "DENY");
  }

  @Test
  public void testDoDelete() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    Cookie cookie = new Cookie(JWT_COOKIE_NAME, "null");
    cookie.setPath("/");
    cookie.setMaxAge(0);

    verify(httpServletResponse).addCookie(any(Cookie.class));
    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, "DENY");
  }
}
