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
package org.sonatype.nexus.extdirect.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ExtDirectServlet}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExtDirectServletTest
{
  @Mock
  private ApplicationDirectories directories;

  @Mock
  private ExtDirectDispatcher extDirectDispatcher;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private ExtDirectServlet servlet;

  @Before
  public void setUp() {
    servlet = new ExtDirectServlet(directories, extDirectDispatcher, false);
  }

  @Test
  public void wrapRequest_normalizesNullPathInfoToEmpty() {
    when(request.getPathInfo()).thenReturn(null);
    assertThat(servlet.wrapRequest(request).getPathInfo(), is(""));
  }

  @Test
  public void wrapRequest_preservesNonNullPathInfo() {
    when(request.getPathInfo()).thenReturn("/some/path");
    assertThat(servlet.wrapRequest(request).getPathInfo(), is("/some/path"));
  }

  @Test
  public void doPost_setsXFrameOptionsHeader_whenPathInfoIsNull() throws Exception {
    when(request.getPathInfo()).thenReturn(null);
    when(request.getContentType()).thenReturn("application/json");

    assertThrows(IllegalStateException.class, () -> servlet.doPost(request, response));

    verify(response).setHeader("X-Frame-Options", "DENY");
  }
}
