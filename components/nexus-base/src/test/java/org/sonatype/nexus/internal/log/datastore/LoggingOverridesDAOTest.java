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
package org.sonatype.nexus.internal.log.datastore;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class LoggingOverridesDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(LoggingOverridesDAO.class);

  private DataSession<?> session;

  private LoggingOverridesDAO dao;

  private static final String FAKE_NAME = "funnyName";

  public static final String FAKE_LEVEL = LoggerLevel.DEBUG.toString();

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(LoggingOverridesDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testDAOOperations() {
    Continuation<LoggingOverridesData> emptyData = dao.readRecords(null);
    assertThat(emptyData.isEmpty(), is(true));

    dao.createRecord(FAKE_NAME, FAKE_LEVEL);
    Optional<LoggingOverridesData> initialRecord = dao.readRecords(null).stream().findFirst();

    assertThat(initialRecord.isPresent(), is(true));
    assertThat(initialRecord.get().getName(), is(FAKE_NAME));
    assertThat(initialRecord.get().getLevel(), is(FAKE_LEVEL));

    dao.deleteRecord(String.valueOf(initialRecord.get().id));
    Continuation<LoggingOverridesData> recordAfterDeletion = dao.readRecords(null);

    assertThat(recordAfterDeletion.isEmpty(), is(true));

    dao.createRecord(FAKE_NAME, FAKE_LEVEL);
    dao.createRecord(FAKE_NAME + "2", FAKE_LEVEL);
    dao.createRecord(FAKE_NAME + "3", FAKE_LEVEL);

    assertThat(dao.readRecords(null).size(), is(3));

    dao.deleteAllRecords();

    assertThat(dao.readRecords(null).size(), is(0));
  }
}
