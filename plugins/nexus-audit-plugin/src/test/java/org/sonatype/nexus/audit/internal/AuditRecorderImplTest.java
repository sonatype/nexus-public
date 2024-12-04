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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditDataRecordedEvent;
import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AuditRecorderImplTest
    extends TestSupport
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

  @Before
  public void setUp() {
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
  public void testNoRecordStoredIfDisabled() {
    AuditData data = makeAuditData();
    underTest.setEnabled(false);
    underTest.record(data);

    verifyNoInteractions(eventManager);
  }

  @Test
  public void testDefaultsAreFilledInIfMissing() {
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
  public void testUnknownIsReplacedIfPrincipalIsPresent() {
    when(initiatorProvider.get()).thenReturn("*UNKNOWN/1.2.3.4");

    AuditData data = makeAuditData();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("principal", "someuser");
    data.setAttributes(attributes);
    underTest.record(data);

    verify(eventManager).post(eventCaptor.capture());
    verifyNoMoreInteractions(eventManager);

    AuditDataRecordedEvent captured =  eventCaptor.getValue();
    assertThat(captured.getData().getTimestamp(), notNullValue());
    assertThat(captured.getData().getNodeId(), is(NODE_ID));
    assertThat(captured.getData().getInitiator(), is("someuser/1.2.3.4"));
  }

  @Test
  public void testEventFiredWhenDataRecorded() {
    AuditData data = makeAuditData();
    underTest.record(data);

    verify(eventManager).post(eventCaptor.capture());
    verifyNoMoreInteractions(eventManager);

    assertThat(eventCaptor.getValue(), notNullValue());
  }
}
