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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * FIXME this class does nothing, yet referenced by 3 other classes atm to do 'something' make useful or remove
 */
public class WebSecurityUtil
{
  public static void setupWebContext(String sessionId) {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    HttpSession mockSession = mock(HttpSession.class);

    doReturn(sessionId).when(mockSession).getId();
    doReturn(null).when(mockRequest).getCookies();
    doReturn(mockSession).when(mockRequest).getSession();
    doReturn(mockSession).when(mockRequest).getSession(false);

    // we need to bind for the "web" impl of the RealmSecurityManager to work
    // TODO this method no longer exists on shiro! org.apache.shiro.web.util.WebUtils
    // WebUtils.bind( mockRequest );
    // WebUtils.bind( mockResponse );
  }
}
