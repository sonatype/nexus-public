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
package org.sonatype.nexus.restlet1x.internal;

import java.util.Collections;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.noelios.restlet.Engine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RestletHeaderFilterTest
    extends TestSupport
{

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @Mock
  private ServletResponse notInstanceOfHttpServletResponse;

  private RestletHeaderFilter filter;

  @Before
  public void setUp() throws Exception {
    this.filter = new RestletHeaderFilter();
  }

  @Test
  public void noServerHeader() throws Exception {
    when(response.getHeaders("Server")).thenReturn(Collections.EMPTY_LIST);

    filter.preHandle(request, response);

    verify(response).setHeader("Server", Engine.VERSION_HEADER);
  }

  @Test
  public void oneServerHeader() throws Exception {
    when(response.getHeaders("Server")).thenReturn(asList("special/5.0.0"));

    filter.preHandle(request, response);

    verify(response).setHeader("Server", "special/5.0.0 " + Engine.VERSION_HEADER);
  }

  @Test
  public void multipleServerHeaders() throws Exception {
    when(response.getHeaders("Server")).thenReturn(asList("special/5.0.0", "server2/v1"));

    filter.preHandle(request, response);

    verify(response).setHeader("Server", "special/5.0.0 server2/v1 " + Engine.VERSION_HEADER);
  }

  @Test
  public void ifNotInstanceOfHttpServletResponseDoNotProcessResponse() throws Exception {
    filter.preHandle(request, notInstanceOfHttpServletResponse);

    verifyZeroInteractions(response);
  }

}