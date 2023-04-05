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
package org.sonatype.nexus.internal.log.overrides.datastore;

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

/**
 * {@link LoggingOverridesDAO} tests
 */
@Category(SQLTestGroup.class)
public class LoggingOverridesDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(LoggingOverridesDAO.class);

  private DataSession<?> session;

  private LoggingOverridesDAO dao;

  private static final String FAKE_NAME = "funnyName";

  private static final String FAKE_LEVEL = LoggerLevel.DEBUG.toString();

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
  public void testBrowse() {
    Continuation<LoggingOverridesData> emptyData = dao.readRecords(null);
    assertThat(emptyData.isEmpty(), is(true));

    LoggingOverridesData recordOne = new LoggingOverridesData();
    recordOne.setName(FAKE_NAME + "_ONE");
    recordOne.setLevel(FAKE_LEVEL);
    dao.createRecord(recordOne);

    LoggingOverridesData recordTwo = new LoggingOverridesData();
    recordTwo.setName(FAKE_NAME + "_TWO");
    recordTwo.setLevel(FAKE_LEVEL);
    dao.createRecord(recordTwo);

    LoggingOverridesData recordThree = new LoggingOverridesData();
    recordThree.setName(FAKE_NAME + "_THREE");
    recordThree.setLevel(FAKE_LEVEL);
    dao.createRecord(recordThree);

    Continuation<LoggingOverridesData> records = dao.readRecords(null);
    assertThat(records.size(), is(3));
  }

  @Test
  public void testCreateRecord() {
    LoggingOverridesData data = new LoggingOverridesData();
    data.setName(FAKE_NAME);
    data.setLevel(FAKE_LEVEL);

    dao.createRecord(data);
    Optional<LoggingOverridesData> record = dao.readRecords(null).stream().findFirst();
    assertThat(record.isPresent(), is(true));
    assertThat(record.get().getName(), is(FAKE_NAME));
    assertThat(record.get().getLevel(), is(FAKE_LEVEL));
  }

  @Test
  public void testUpdate() {
    LoggingOverridesData data = new LoggingOverridesData();
    data.setName(FAKE_NAME);
    data.setLevel(FAKE_LEVEL);
    dao.createRecord(data);

    Optional<LoggingOverridesData> initial = dao.readRecords(null).stream().findFirst();
    assertThat(initial.isPresent(), is(true));

    data.setLevel(LoggerLevel.INFO.toString());
    dao.updateRecord(data);

    Optional<LoggingOverridesData> updated = dao.readRecords(null).stream().findFirst();
    assertThat(updated.isPresent(), is(true));
    assertThat(updated.get().getLevel(), is(LoggerLevel.INFO.toString()));
  }

  @Test
  public void testDeleteByName() {
    LoggingOverridesData dataOne = new LoggingOverridesData();
    dataOne.setName(FAKE_NAME + "_ONE");
    dataOne.setLevel(FAKE_LEVEL);
    dao.createRecord(dataOne);

    LoggingOverridesData dataTwo = new LoggingOverridesData();
    dataTwo.setName(FAKE_NAME + "_TWO");
    dataTwo.setLevel(FAKE_LEVEL);
    dao.createRecord(dataTwo);

    Continuation<LoggingOverridesData> records = dao.readRecords(null);
    assertThat(records.size(), is(2));

    dao.deleteRecord(FAKE_NAME + "_ONE");
    records = dao.readRecords(null);
    assertThat(records.size(), is(1));

    dao.deleteRecord(FAKE_NAME + "_TWO");
    records = dao.readRecords(null);
    assertThat(records.size(), is(0));
  }
}
