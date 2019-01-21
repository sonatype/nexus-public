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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AntiCsrfFilterTest
  extends TestSupport
{
  private AntiCsrfFilter underTest;

  @Mock
  private AntiCsrfHelper antiCrsfHelper;

  @Mock
  private PrintWriter printWriter;

  @Mock
  HttpServletRequest httpServletRequest;

  @Mock
  HttpServletResponse httpServletResponse;

  @Before
  public void setup() throws IOException {
    underTest = new AntiCsrfFilter(antiCrsfHelper);
    when(httpServletResponse.getWriter()).thenReturn(printWriter);
  }

  @Test
  public void testOnAccessDenied() throws IOException {
    assertFalse(underTest.onAccessDenied(httpServletRequest, httpServletResponse));
    verify(httpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(httpServletResponse).setContentType("text/plain");
    verify(printWriter).print(AntiCsrfHelper.ERROR_MESSAGE_TOKEN_MISMATCH);
  }

  @Test
  public void testIsEnabled() {
    when(antiCrsfHelper.isEnabled()).thenReturn(true);
    assertTrue(underTest.isEnabled());

    when(antiCrsfHelper.isEnabled()).thenReturn(false);
    assertFalse(underTest.isEnabled());
  }

  @Test
  public void testIsAccessAllowed() {
    when(antiCrsfHelper.isAccessAllowed(httpServletRequest)).thenReturn(true);
    assertTrue(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null));

    when(antiCrsfHelper.isAccessAllowed(httpServletRequest)).thenReturn(false);
    assertFalse(underTest.isAccessAllowed(httpServletRequest, httpServletResponse, null));
  }
}
