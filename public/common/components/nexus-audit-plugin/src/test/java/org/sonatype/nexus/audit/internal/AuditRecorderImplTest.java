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
package org.sonatype.nexus.audit.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditDataRecordedEvent;
import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditRecorderImplTest
{
  private static final String INITIATOR = "test/1.2.3.4";

  private static final String NODE_ID = UUID.randomUUID().toString();

  @Mock
  private EventManager eventManager;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private InitiatorProvider initiatorProvider;

  @Captor
  private ArgumentCaptor<AuditDataRecordedEvent> eventCaptor;

  @InjectMocks
  private AuditRecorderImpl underTest;

  @BeforeEach
  void setUp() {
    when(initiatorProvider.get()).thenReturn(INITIATOR);
    when(nodeAccess.getId()).thenReturn(NODE_ID);
    underTest.setEnabled(true);
  }

  private static AuditData makeAuditData() {
    AuditData auditData = new AuditData();
    auditData.setDomain("foo");
    auditData.setType("bar");
    auditData.setContext("baz");
    return auditData;
  }

  @Test
  void testNoRecordStoredIfDisabled() {
    AuditData data = makeAuditData();
    underTest.setEnabled(false);
    underTest.record(data);

    verifyNoInteractions(eventManager);
  }

  @Test
  void testDefaultsAreFilledInIfMissing() {
    AuditData data = makeAuditData();
    underTest.record(data);

    verify(eventManager).post(eventCaptor.capture());
    verifyNoMoreInteractions(eventManager);

    AuditDataRecordedEvent captured = eventCaptor.getValue();
    assertThat(captured.getData().getTimestamp(), notNullValue());
    assertThat(captured.getData().getNodeId(), is(NODE_ID));
    assertThat(captured.getData().getInitiator(), is(INITIATOR));
  }

  @Test
  void testUnknownIsReplacedIfPrincipalIsPresent() {
    when(initiatorProvider.get()).thenReturn("*UNKNOWN/1.2.3.4");

    AuditData data = makeAuditData();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("principal", "someuser");
    data.setAttributes(attributes);
    underTest.record(data);

    verify(eventManager).post(eventCaptor.capture());
    verifyNoMoreInteractions(eventManager);

    AuditDataRecordedEvent captured = eventCaptor.getValue();
    assertThat(captured.getData().getTimestamp(), notNullValue());
    assertThat(captured.getData().getNodeId(), is(NODE_ID));
    assertThat(captured.getData().getInitiator(), is("someuser/1.2.3.4"));
  }

  @Test
  void testEventFiredWhenDataRecorded() {
    AuditData data = makeAuditData();
    underTest.record(data);

    verify(eventManager).post(eventCaptor.capture());
    verifyNoMoreInteractions(eventManager);

    assertThat(eventCaptor.getValue(), notNullValue());
  }

  @Test
  void testRecordThrowsNpeWhenDomainIsNull() {
    AuditData data = new AuditData();
    data.setType("bar");
    data.setContext("baz");
    // domain intentionally left null

    assertThrows(NullPointerException.class, () -> underTest.record(data));
    verifyNoInteractions(eventManager);
  }

  @Test
  void testRecordThrowsNpeWhenTypeIsNull() {
    AuditData data = new AuditData();
    data.setDomain("foo");
    data.setContext("baz");
    // type intentionally left null

    assertThrows(NullPointerException.class, () -> underTest.record(data));
    verifyNoInteractions(eventManager);
  }

  @Test
  void testRecordThrowsNpeEvenWhenDisabled() {
    // Precondition must fire before the enabled gate
    underTest.setEnabled(false);
    AuditData data = new AuditData();
    // domain and type both null

    assertThrows(NullPointerException.class, () -> underTest.record(data));
    verifyNoInteractions(eventManager);
  }
}
