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
package org.sonatype.security.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.sonatype.security.AbstractSecurityTest;

import org.easymock.EasyMock;

import static org.easymock.EasyMock.replay;

public abstract class AbstractWebSecurityTest
    extends AbstractSecurityTest
{
  protected void setupLoginContext(String sessionId) {
    HttpServletRequest mockRequest = EasyMock.createNiceMock(HttpServletRequest.class);
    HttpServletResponse mockResponse = EasyMock.createNiceMock(HttpServletResponse.class);
    HttpSession mockSession = EasyMock.createNiceMock(HttpSession.class);

    EasyMock.expect(mockSession.getId()).andReturn(sessionId).anyTimes();
    EasyMock.expect(mockRequest.getCookies()).andReturn(null).anyTimes();
    EasyMock.expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
    EasyMock.expect(mockRequest.getSession(false)).andReturn(mockSession).anyTimes();
    replay(mockSession);
    replay(mockRequest);

    // // we need to bind for the "web" impl of the PlexusSecurityManager to work
    // WebUtils.bind( mockRequest );
    // WebUtils.bind( mockResponse );
  }
}
