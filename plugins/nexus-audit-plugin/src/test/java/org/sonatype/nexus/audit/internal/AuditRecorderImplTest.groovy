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
import org.sonatype.nexus.audit.AuditData
import org.sonatype.nexus.audit.AuditDataRecordedEvent
import org.sonatype.nexus.audit.AuditStore
import org.sonatype.nexus.audit.InitiatorProvider
import org.sonatype.nexus.common.event.EventBus
import org.sonatype.nexus.common.node.LocalNodeAccess

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyNoMoreInteractions
import static org.mockito.Mockito.verifyZeroInteractions
import static org.mockito.Mockito.when

/**
 * Tests for {@link AuditRecorderImpl}.
 */
class AuditRecorderImplTest
    extends TestSupport
{
  @Mock
  EventBus eventBus

  @Mock
  LocalNodeAccess localNodeAccess

  @Mock
  AuditStore auditStore

  @Mock
  InitiatorProvider initiatorProvider

  AuditRecorderImpl underTest

  private static final String initiator = 'test/1.2.3.4'

  private static final String nodeId = UUID.randomUUID().toString()

  @Before
  void setUp() {
    when(initiatorProvider.get()).thenReturn(initiator)
    when(localNodeAccess.getId()).thenReturn(nodeId)

    underTest = new AuditRecorderImpl(eventBus, localNodeAccess, auditStore, initiatorProvider)
    underTest.enabled = true
  }

  private static AuditData makeAuditData() {
    return new AuditData(
        domain: 'foo',
        type: 'bar',
        context: 'baz'
    )
  }

  @Test
  void 'no record stored if disabled'() {
    AuditData data = makeAuditData()
    underTest.enabled = false
    underTest.record(data)

    verifyZeroInteractions(auditStore)
  }

  @Test
  void 'defaults are filled in if missing'() {
    AuditData data = makeAuditData()
    underTest.record(data)

    def argument = ArgumentCaptor.forClass(AuditData.class)
    verify(auditStore).add(argument.capture())
    verifyNoMoreInteractions(auditStore)

    AuditData captured = argument.value
    assert captured.timestamp != null
    assert captured.nodeId == nodeId
    assert captured.initiator == initiator
  }

  @Test
  void 'event fired when data recorded'() {
    AuditData data = makeAuditData()
    underTest.record(data)

    def argument = ArgumentCaptor.forClass(Object.class)
    verify(eventBus).post(argument.capture())
    verifyNoMoreInteractions(eventBus)

    Object captured = argument.value
    assert captured instanceof AuditDataRecordedEvent
  }
}
