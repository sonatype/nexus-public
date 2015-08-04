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
package org.sonatype.nexus.web;

import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link WebUtils}.
 */
public class WebUtilsTest
  extends TestSupport
{
  private WebUtils underTest;

  @Before
  public void setUp() throws Exception {
    SystemStatus systemStatus = mock(SystemStatus.class);
    doReturn("version").when(systemStatus).getVersion();

    ApplicationStatusSource applicationStatusSource = mock(ApplicationStatusSource.class);
    doReturn(systemStatus).when(applicationStatusSource).getSystemStatus();

    underTest = new WebUtils(applicationStatusSource);
  }

  @Test
  public void testEquipResponseWithStandardHeaders() {
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    underTest.equipResponseWithStandardHeaders(response);

    verify(response).setHeader("Server", "Nexus/version");
    verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
    verify(response).setHeader("X-Content-Type-Options", "nosniff");

    verifyNoMoreInteractions(response);
  }
}
