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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.servlet.XFrameOptions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class ErrorPageFilterTest
    extends TestSupport
{
  private ErrorPageFilter underTest;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @Before
  public void setup() {
    XFrameOptions xFrameOptions = new XFrameOptions(true);
    underTest = new ErrorPageFilter(xFrameOptions);
  }

  @Test
  public void testDoFilter_properXframeOptions() throws Exception {
    doThrow(new IOException("test")).when(filterChain).doFilter(any(), any());

    underTest.doFilter(request, response, filterChain);

    verify(response).setHeader(X_FRAME_OPTIONS, "DENY");
  }
}
