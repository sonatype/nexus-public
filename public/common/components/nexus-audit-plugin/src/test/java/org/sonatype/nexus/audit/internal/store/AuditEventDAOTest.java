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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Integration test for {@link AuditEventDAO#deleteOlderThan} running against the H2 datastore.
 * Complements the mocked unit tests in {@link AuditEventCleanupTaskTest} by exercising the
 * actual SQL, ensuring the strict {@code timestamp < cutoff} semantics and the {@code LIMIT}
 * behavior hold end-to-end.
 */
public class AuditEventDAOTest
{
  private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(AuditEventDAO.class);

  private DataSession<?> session;

  private AuditEventDAO dao;

  @Before
  public void setUp() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(AuditEventDAO.class);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void deletesOnlyRowsStrictlyOlderThanCutoff() {
    insertAt(T0.minusDays(2));
    insertAt(T0.minusDays(1));
    insertAt(T0); // exactly at cutoff — must NOT be deleted (strict <)
    insertAt(T0.plusDays(1));

    int deleted = dao.deleteOlderThan(T0, 100);

    assertThat(deleted, is(2));
    assertThat(dao.count(null, null, null, null, null, null), is(2));
  }

  @Test
  public void deletesNothingWhenAllRowsAreNewer() {
    insertAt(T0.plusDays(1));
    insertAt(T0.plusDays(2));

    int deleted = dao.deleteOlderThan(T0, 100);

    assertThat(deleted, is(0));
    assertThat(dao.count(null, null, null, null, null, null), is(2));
  }

  @Test
  public void batchSizeCapsRowsDeletedPerCall() {
    for (int i = 1; i <= 5; i++) {
      insertAt(T0.minusDays(i));
    }

    int deleted = dao.deleteOlderThan(T0, 2);

    assertThat(deleted, is(2));
    assertThat(dao.count(null, null, null, null, null, null), is(3));
  }

  @Test
  public void repeatedCallsPruneBacklogInBatches() {
    for (int i = 1; i <= 5; i++) {
      insertAt(T0.minusDays(i));
    }

    int total = 0;
    int batch;
    do {
      batch = dao.deleteOlderThan(T0, 2);
      total += batch;
    }
    while (batch > 0);

    assertThat(total, is(5));
    assertThat(dao.count(null, null, null, null, null, null), is(0));
  }

  private void insertAt(final OffsetDateTime timestamp) {
    AuditEventData data = new AuditEventData();
    data.setDomain("test.domain");
    data.setType("test");
    data.setContext("ctx");
    data.setTimestamp(timestamp);
    data.setInitiator("initiator");
    data.setNodeId("node-1");
    dao.insert(data);
  }
}
