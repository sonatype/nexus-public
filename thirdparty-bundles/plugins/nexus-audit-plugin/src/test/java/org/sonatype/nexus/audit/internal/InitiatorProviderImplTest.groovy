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
package org.sonatype.nexus.audit.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.security.ClientInfo
import org.sonatype.nexus.security.ClientInfoProvider

import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Tests for {@link InitiatorProviderImpl}.
 */
class InitiatorProviderImplTest
    extends TestSupport
{
  @Mock
  ClientInfoProvider clientInfoProvider

  InitiatorProviderImpl underTest

  private static void reset() {
    ThreadContext.unbindSubject()
    ThreadContext.unbindSecurityManager()
  }

  private static Subject subject(final Object principal) {
    Subject subject = mock(Subject.class)
    when(subject.getPrincipal()).thenReturn(principal)
    return subject
  }

  @Before
  void setUp() {
    reset()

    underTest = new InitiatorProviderImpl(clientInfoProvider)
  }

  @After
  void tearDown() {
    reset()
  }

  @Test
  void 'prefer clientinfo when available'() {
    ClientInfo clientInfo = new ClientInfo('foo', '1.2.3.4', 'bar')
    when(clientInfoProvider.getCurrentThreadClientInfo()).thenReturn(clientInfo)

    def result = underTest.get()
    assert result == 'foo/1.2.3.4'
  }

  @Test
  void 'when clientinfo missing uses subject'() {
    ThreadContext.bind(subject('foo'))
    def result = underTest.get()
    assert result == 'foo'
  }
}
