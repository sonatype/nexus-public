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
package org.sonatype.nexus.internal.web;

import java.util.Collections;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HeaderPatternFilter}
 *
 */
public class HeaderPatternFilterTest
    extends TestSupport
{

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Mock
  FilterConfig filterConfig;

  @Mock
  javax.servlet.FilterChain filterChain;

  HeaderPatternFilter filter;

  @Before
  public void setUp() throws Exception {
    filter = new HeaderPatternFilter();
    when(filterConfig.getInitParameter(anyString())).thenReturn(null);
    filter.init(filterConfig);
  }

  @Test
  public void testFilter_badValue() throws Exception {
    testHeaderValue("><script>alert(document.domain)</script>");
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testFilter_otherBadValue() throws Exception {
    testHeaderValue("not a legit hostname");
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testFilter_goodValue() throws Exception {
    testHeaderValue("example.com");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void testFilter_localhost() throws Exception {
    testHeaderValue("localhost");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void testFilter_nonroutableIp() throws Exception {
    testHeaderValue("10.0.0.1");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void testFilter_localhostWithPort() throws Exception {
    testHeaderValue("localhost:8080");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void testFilter_goodValueWithPort() throws Exception {
    testHeaderValue("example.com:8080");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void testFilter_nonroutableWithPort() throws Exception {
    testHeaderValue("10.0.0.1:8080");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void testFilter_ipv6() throws Exception {
    testHeaderValue("[1762:0:0:0:0:B03:1:AF18]");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void testFilter_ipv6WithPort() throws Exception {
    testHeaderValue("[1762:0:0:0:0:B03:1:AF18]:8080");
    verify(filterChain).doFilter(request, response);
  }

  private void testHeaderValue(String headerValue) throws Exception {
    when(request.getHeaders("Host")).thenReturn(Collections.enumeration(Collections.singleton(headerValue)));
    filter.doFilter(request, response, filterChain);
  }

}
