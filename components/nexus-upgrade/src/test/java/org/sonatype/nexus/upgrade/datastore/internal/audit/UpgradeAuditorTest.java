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
package org.sonatype.nexus.upgrade.datastore.internal.audit;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;
import org.sonatype.nexus.common.upgrade.events.UpgradeFailedEvent;
import org.sonatype.nexus.common.upgrade.events.UpgradeStartedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.COMPLETED;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.DOMAIN;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.FAILED;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.MESSAGE;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.MIGRATIONS;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.NEXUS_VERSION;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.NODE_IDS;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.SCHEMA_VERSION;
import static org.sonatype.nexus.upgrade.datastore.internal.audit.UpgradeAuditor.STARTED;

public class UpgradeAuditorTest
    extends TestSupport
{
  private static final String V3_99 = "3.99";

  private static final String ERROR_MSG = "Something unfortunate occurred";

  private static final String FOO_1_38 = "Foo_1.38";

  private static final String V1_38 = "1.38";

  private static final String V1_37 = "1.37";

  private static final String USER = "timmy";

  @Mock
  private AuditRecorder recorder;

  @Mock
  private ApplicationVersion applicationVersion;

  @Captor
  private ArgumentCaptor<AuditData> auditDataCaptor;

  private UpgradeAuditor underTest;

  @Before
  public void setup() {
    when(applicationVersion.getVersion()).thenReturn(V3_99);
    underTest = new UpgradeAuditor(applicationVersion);
    underTest.setAuditRecorder(() -> recorder);

    when(recorder.isEnabled()).thenReturn(true);
  }

  @Test
  public void testOn_UpgradeCompletedEvent() {
    Collection<String> nodeIds = Collections.singletonList("my-node-id");
    underTest.on(new UpgradeCompletedEvent(USER, V1_38, nodeIds, FOO_1_38));

    verify(recorder).record(auditDataCaptor.capture());

    AuditData recorded = auditDataCaptor.getValue();
    assertAuditDataWithNodes(recorded, COMPLETED, USER, V1_38, V3_99, FOO_1_38, nodeIds);
  }

  @Test
  public void testOn_UpgradeFailedEvent() {
    underTest.on(new UpgradeFailedEvent(USER, V1_38, ERROR_MSG));

    verify(recorder).record(auditDataCaptor.capture());

    AuditData recorded = auditDataCaptor.getValue();
    assertAuditDataWithError(recorded, FAILED, USER, V1_38, V3_99, ERROR_MSG);
  }

  @Test
  public void testOn_UpgradeStartedEvent() {
    underTest.on(new UpgradeStartedEvent(USER, V1_37, FOO_1_38));

    verify(recorder).record(auditDataCaptor.capture());

    AuditData recorded = auditDataCaptor.getValue();
    assertAuditDataWithMigration(recorded, STARTED, USER, V1_37, V3_99, FOO_1_38);
  }

  /*
   * The schema version can be null when migration during the first Nexus start
   */
  @Test
  public void testOn_UpgradeStartedEvent_nullSchema() {
    underTest.on(new UpgradeStartedEvent(USER, null, FOO_1_38));

    verify(recorder).record(auditDataCaptor.capture());

    AuditData recorded = auditDataCaptor.getValue();
    assertAuditDataWithMigration(recorded, STARTED, USER, null, V3_99, FOO_1_38);
  }

  /*
   * The user can be null when an upgrade occurs during startup
   */
  @Test
  public void testOn_nullUser() {
    underTest.on(new UpgradeStartedEvent(null, V1_37, FOO_1_38));

    verify(recorder).record(auditDataCaptor.capture());

    AuditData recorded = auditDataCaptor.getValue();
    assertAuditDataWithMigration(recorded, STARTED, "system", V1_37, V3_99, FOO_1_38);
  }

  private static void assertAuditData(
      final AuditData actual,
      final String type,
      final String user,
      final String schemaVersion,
      final String nexusVersion)
  {
    assertThat(actual.getContext(), is("system"));
    assertThat(actual.getDomain(), is(DOMAIN));
    assertThat(actual.getInitiator(), is(user));
    assertThat(actual.getType(), is(type));

    Map<String, Object> actualAttributes = actual.getAttributes();
    assertThat(actualAttributes.get(SCHEMA_VERSION), is(schemaVersion));
    assertThat(actualAttributes.get(NEXUS_VERSION), is(nexusVersion));
  }

  private static void assertAuditDataWithMigration(
      final AuditData actual,
      final String type,
      final String user,
      final String schemaVersion,
      final String nexusVersion,
      final String migrations)
  {
    assertAuditData(actual, type, user, schemaVersion, nexusVersion);
    assertThat(((String[]) (actual.getAttributes().get(MIGRATIONS)))[0], is(migrations));
  }

  private static void assertAuditDataWithNodes(
      final AuditData actual,
      final String type,
      final String user,
      final String schemaVersion,
      final String nexusVersion,
      final String migrations,
      final Collection<String> nodeIds)
  {
    assertAuditDataWithMigration(actual, type, user, schemaVersion, nexusVersion, migrations);
    assertThat(actual.getAttributes().get(NODE_IDS), is(nodeIds));
  }

  private static void assertAuditDataWithError(
      final AuditData actual,
      final String type,
      final String user,
      final String schemaVersion,
      final String nexusVersion,
      final String error)
  {
    assertAuditData(actual, type, user, schemaVersion, nexusVersion);
    assertThat(actual.getAttributes().get(MESSAGE), is(error));
  }
}
