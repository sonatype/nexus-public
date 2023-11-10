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

import java.util.Arrays;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;

import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SessionServletTest
    extends TestSupport
{
  @Mock
  private HttpServletRequest httpServletRequest;

  @Mock
  private HttpServletResponse httpServletResponse;

  @Mock
  private Subject subject;

  @Mock
  private EventManager eventManager;

  private SessionServlet underTest;

  @Before
  public void setup() {
    underTest = new SessionServlet(eventManager);

    PrincipalCollection principalCollection = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principalCollection);
    when(principalCollection.getRealmNames()).thenReturn(new HashSet<>(Arrays.asList("realm")));
    when(subject.getPrincipal()).thenReturn("someuser");

    when(subject.isAuthenticated()).thenReturn(true);
    when(subject.getSession(false)).thenReturn(mock(Session.class));
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
    when(subject.getSession(false)).thenReturn(null);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, "DENY");
  }
}
