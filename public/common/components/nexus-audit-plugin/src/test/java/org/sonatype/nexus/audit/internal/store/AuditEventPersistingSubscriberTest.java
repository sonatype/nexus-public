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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditDataRecordedEvent;
import org.sonatype.nexus.common.event.EventHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link AuditEventPersistingSubscriber}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuditEventPersistingSubscriberTest
{
  @Mock
  private AuditEventStore auditEventStore;

  @Captor
  private ArgumentCaptor<AuditEventData> dataCaptor;

  private AuditEventPersistingSubscriber subscriber;

  @BeforeEach
  void setUp() {
    subscriber = new AuditEventPersistingSubscriber(auditEventStore);
  }

  @AfterEach
  void tearDown() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  void testPersistsAuditEvent() {
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
  void testSkipsWhenReplicating() {
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

  // ===== Task lifecycle event filtering tests =====

  @Test
  void testSkipsTaskStartedEvent() {
    assertTaskLifecycleEventFiltered("started");
  }

  @Test
  void testSkipsTaskFinishedEvent() {
    assertTaskLifecycleEventFiltered("finished");
  }

  @Test
  void testSkipsTaskFailedEvent() {
    assertTaskLifecycleEventFiltered("failed");
  }

  @Test
  void testSkipsTaskCancelRequestedEvent() {
    assertTaskLifecycleEventFiltered("cancel-requested");
  }

  @Test
  void testSkipsTaskCanceledEvent() {
    assertTaskLifecycleEventFiltered("canceled");
  }

  @Test
  void testSkipsTaskBlockedEvent() {
    assertTaskLifecycleEventFiltered("blocked");
  }

  @Test
  void testSkipsUnregisteredTaskEventType() {
    // TaskStartedRunningEvent has no registerType(...) call in TaskAuditor, so it falls through
    // to Strings2.lower(class.simpleName). The allowlist must skip any type not in {scheduled, deleted}.
    assertTaskLifecycleEventFiltered("taskstartedrunningevent");
  }

  private void assertTaskLifecycleEventFiltered(String type) {
    AuditData auditData = createTaskAuditData(type);
    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(false);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }
    verifyNoInteractions(auditEventStore);
  }

  // ===== Task configuration events should be persisted =====

  @Test
  void testPersistsTaskScheduledEvent() {
    assertTaskConfigurationEventPersisted("scheduled");
  }

  @Test
  void testPersistsTaskDeletedEvent() {
    assertTaskConfigurationEventPersisted("deleted");
  }

  private void assertTaskConfigurationEventPersisted(String type) {
    AuditData auditData = createTaskAuditData(type);
    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(false);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }
    verify(auditEventStore).insert(dataCaptor.capture());
    AuditEventData persisted = dataCaptor.getValue();
    assertThat(persisted.getDomain(), is("tasks"));
    assertThat(persisted.getType(), is(type));
  }

  // ===== Null-type edge case =====

  @Test
  void testSkipsTaskEventWithNullType() {
    // A null type in a policed domain cannot be in the allowlist, so it is filtered.
    AuditData auditData = new AuditData();
    auditData.setDomain("tasks");
    auditData.setType(null);
    auditData.setContext("repository.cleanup");
    auditData.setInitiator("admin");
    auditData.setTimestamp(new Date());

    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(false);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }

    verifyNoInteractions(auditEventStore);
  }

  // ===== Non-task domains should not be affected by filtering =====

  @Test
  void testPersistsNonTaskEventWithLifecycleTypeName() {
    // Verify that "started" type is only filtered for tasks domain
    AuditData auditData = new AuditData();
    auditData.setDomain("repository");
    auditData.setType("started"); // Same type name as task lifecycle event
    auditData.setContext("maven-releases");
    auditData.setInitiator("admin");
    auditData.setTimestamp(new Date());

    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(false);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }

    verify(auditEventStore).insert(dataCaptor.capture());
    AuditEventData persisted = dataCaptor.getValue();
    assertThat(persisted.getDomain(), is("repository"));
    assertThat(persisted.getType(), is("started"));
  }

  // ===== Helper methods =====

  private AuditData createTaskAuditData(String type) {
    AuditData auditData = new AuditData();
    auditData.setDomain("tasks");
    auditData.setType(type);
    auditData.setContext("repository.cleanup");
    auditData.setInitiator("admin");
    auditData.setNodeId("node-1");
    auditData.setTimestamp(new Date());
    auditData.setAttributes(Map.of(
        "schedule", "manual",
        "currentState", "RUNNING",
        "lastRunState", "OK"));
    return auditData;
  }
}
