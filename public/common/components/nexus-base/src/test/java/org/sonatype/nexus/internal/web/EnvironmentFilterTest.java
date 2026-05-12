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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.BaseUrlManager;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static com.google.common.net.HttpHeaders.SERVER;
import static com.google.common.net.HttpHeaders.STRICT_TRANSPORT_SECURITY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.internal.web.EnvironmentFilter.STS_VALUE;

@ExtendWith(MockitoExtension.class)
class EnvironmentFilterTest
{
  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private BaseUrlManager baseUrlManager;

  @BeforeEach
  void setup() {
    lenient().when(applicationVersion.getVersion()).thenReturn("3.0.0");
    lenient().when(applicationVersion.getEdition()).thenReturn("CORE");
  }

  @Test
  void doFilter_headerDisabled() throws ServletException, IOException {
    EnvironmentFilter filter = new EnvironmentFilter(applicationVersion, baseUrlManager, false);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(response, never()).setHeader(eq(SERVER), anyString());
    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_ContentSecurityPolicy_is_set() throws ServletException, IOException {
    EnvironmentFilter filter = new EnvironmentFilter(applicationVersion, baseUrlManager, true);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    // It is important that headers are set *before* the filter chain not after. We may overwrite the CSP later, and
    // headers may not be set once the server starts writing the response
    InOrder ordered = inOrder(response, chain);

    ordered.verify(response).setHeader(eq(CONTENT_SECURITY_POLICY), anyString());
    ordered.verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_StrictTransportSecurity_for_https() throws ServletException, IOException {
    EnvironmentFilter filter = new EnvironmentFilter(applicationVersion, baseUrlManager, true);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getScheme()).thenReturn("https");
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(response).setHeader(eq(CONTENT_SECURITY_POLICY), anyString());
    verify(response).setHeader(STRICT_TRANSPORT_SECURITY, STS_VALUE);
    verify(chain).doFilter(request, response);
  }
}
