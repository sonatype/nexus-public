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
package org.sonatype.nexus.audit.internal.store;

import java.util.Date;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditDataRecordedEvent;
import org.sonatype.nexus.common.event.EventHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AuditEventPersistingSubscriberTest
{
  @Mock
  private AuditEventStore auditEventStore;

  @Captor
  private ArgumentCaptor<AuditEventData> dataCaptor;

  private AuditEventPersistingSubscriber subscriber;

  @Before
  public void setUp() {
    subscriber = new AuditEventPersistingSubscriber(auditEventStore);
  }

  @After
  public void tearDown() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testPersistsAuditEvent() {
    AuditData auditData = new AuditData();
    auditData.setDomain("protect.firewall");
    auditData.setType("protection-level-changed");
    auditData.setContext("maven-proxy");
    auditData.setInitiator("admin");
    auditData.setNodeId("node-1");
    auditData.setTimestamp(new Date(1L));
    auditData.setAttributes(Map.of("from", "none", "to", "quarantine"));

    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(false);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }

    verify(auditEventStore).insert(dataCaptor.capture());
    AuditEventData persisted = dataCaptor.getValue();
    assertThat(persisted.getDomain(), is("protect.firewall"));
    assertThat(persisted.getType(), is("protection-level-changed"));
    assertThat(persisted.getContext(), is("maven-proxy"));
    assertThat(persisted.getInitiator(), is("admin"));
    assertThat(persisted.getTimestamp(), is(notNullValue()));
  }

  @Test
  public void testSkipsWhenReplicating() {
    AuditData auditData = new AuditData();
    auditData.setDomain("protect.firewall");
    auditData.setType("protection-level-changed");
    auditData.setContext("maven-proxy");
    auditData.setInitiator("admin");
    auditData.setAttributes(Map.of("from", "none", "to", "quarantine"));

    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(true);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }

    verifyNoInteractions(auditEventStore);
  }
}
