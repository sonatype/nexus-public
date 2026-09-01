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

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditDataRecordedEvent;
import org.sonatype.nexus.common.event.EventHelper;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.clearInvocations;
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

  @Test
  void testBlobStoreEventFiltering() {
    assertEventPersisted(createBlobstoreAuditData("created"));
    assertEventPersisted(createBlobstoreAuditData("updated"));
    assertEventPersisted(createBlobstoreAuditData("deleted"));

    assertEventFiltered(createBlobstoreAuditData("started"));
    assertEventFiltered(createBlobstoreAuditData("stopped"));
  }

  @Test
  void testRepositoryEventFiltering() {
    assertEventPersisted(createRepositoryAuditData("created"));
    assertEventPersisted(createRepositoryAuditData("updated"));
    assertEventPersisted(createRepositoryAuditData("deleted"));
    assertEventPersisted(createRepositoryAuditData("cacheInvalidated"));
    assertEventPersisted(createRepositoryAuditData("autoBlockStatus"));

    assertEventFiltered(createRepositoryAuditData("started"));
    assertEventFiltered(createRepositoryAuditData("restored"));
    assertEventFiltered(createRepositoryAuditData("destroyed"));
    assertEventFiltered(createRepositoryAuditData("loaded"));
    assertEventFiltered(createRepositoryAuditData("stopped"));
  }

  // ===== Task lifecycle event filtering tests =====

  @Test
  void testSkipsTaskStartedEvent() {
    assertEventFiltered(createTaskAuditData("started"));
  }

  @Test
  void testSkipsTaskFinishedEvent() {
    assertEventFiltered(createTaskAuditData("finished"));
  }

  @Test
  void testSkipsTaskFailedEvent() {
    assertEventFiltered(createTaskAuditData("failed"));
  }

  @Test
  void testSkipsTaskCancelRequestedEvent() {
    assertEventFiltered(createTaskAuditData("cancel-requested"));
  }

  @Test
  void testSkipsTaskCanceledEvent() {
    assertEventFiltered(createTaskAuditData("canceled"));
  }

  @Test
  void testSkipsTaskBlockedEvent() {
    assertEventFiltered(createTaskAuditData("blocked"));
  }

  @Test
  void testSkipsUnregisteredTaskEventType() {
    // TaskStartedRunningEvent has no registerType(...) call in TaskAuditor, so it falls through
    // to Strings2.lower(class.simpleName). The allowlist must skip any type not in {scheduled, deleted}.
    assertEventFiltered(createTaskAuditData("taskstartedrunningevent"));
  }

  // ===== Task configuration events should be persisted =====

  @Test
  void testPersistsTaskScheduledEvent() {
    assertEventPersisted(createTaskAuditData("scheduled"));
  }

  @Test
  void testPersistsTaskDeletedEvent() {
    assertEventPersisted(createTaskAuditData("deleted"));
  }

  // ===== Null-type edge case =====

  @Test
  void testSkipsEventWithNullType() {
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
  void testPersistsFilteredTypeFromOtherDomain() {
    // Verify that "started" type is not only filtered a different domain
    AuditData auditData = createRepositoryAuditData("started");
    auditData.setDomain("some-other-domain");

    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(false);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }

    verify(auditEventStore).insert(dataCaptor.capture());
    AuditEventData persisted = dataCaptor.getValue();
    assertThat(persisted.getDomain(), is("some-other-domain"));
    assertThat(persisted.getType(), is("started"));
  }

  // ===== Helper methods =====

  private static AuditData createBlobstoreAuditData(final String type) {
    AuditData auditData = new AuditData();
    auditData.setDomain("blobstore");
    auditData.setType(type);
    auditData.setContext("default");
    auditData.setInitiator("admin");
    auditData.setNodeId("node-1");
    auditData.setTimestamp(new Date());
    auditData.setAttributes(Map.of());
    return auditData;
  }

  private static AuditData createRepositoryAuditData(final String type) {
    AuditData auditData = new AuditData();
    auditData.setDomain("repository");
    auditData.setType(type);
    auditData.setContext("maven-central");
    auditData.setInitiator("admin");
    auditData.setNodeId("node-1");
    auditData.setTimestamp(new Date());
    auditData.setAttributes(Map.of());
    return auditData;
  }

  private static AuditData createTaskAuditData(final String type) {
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

  private void assertEventPersisted(final AuditData auditData) {
    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(false);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }
    verify(auditEventStore).insert(dataCaptor.capture());
    AuditEventData persisted = dataCaptor.getValue();
    assertThat(persisted.getDomain(), is(auditData.getDomain()));
    assertThat(persisted.getType(), is(auditData.getType()));
    clearInvocations(auditEventStore);
  }

  private void assertEventFiltered(final AuditData auditData) {
    try (MockedStatic<EventHelper> eh = Mockito.mockStatic(EventHelper.class)) {
      eh.when(EventHelper::isReplicating).thenReturn(false);
      subscriber.on(new AuditDataRecordedEvent(auditData));
    }
    verifyNoInteractions(auditEventStore);
  }
}
