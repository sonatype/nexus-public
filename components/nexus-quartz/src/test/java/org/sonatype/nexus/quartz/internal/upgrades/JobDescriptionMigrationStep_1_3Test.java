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
package org.sonatype.nexus.quartz.internal.upgrades;

import java.sql.Connection;
import java.util.Locale;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.quartz.internal.datastore.QuartzDAO;
import org.sonatype.nexus.quartz.internal.datastore.QuartzJobDataTypeHandler;
import org.sonatype.nexus.quartz.internal.datastore.QuartzTestDAO;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Unit tests for {@link JobDescriptionMigrationStep_1_3} class
 */
@Category(SQLTestGroup.class)
public class JobDescriptionMigrationStep_1_3Test
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule()
      .handle(new QuartzJobDataTypeHandler())
      .access(QuartzDAO.class)
      .access(QuartzTestDAO.class);

  private DataSession<?> session;

  private QuartzTestDAO dao;

  private JobDescriptionMigrationStep_1_3 upgradeStep;

  private final static String TEXT = "TEXT";

  @Before
  public void setup() {
    upgradeStep = new JobDescriptionMigrationStep_1_3();
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(QuartzTestDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testMigration() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      upgradeStep.migrate(conn);
    }

    String qrtzJobDetailsTableName = "qrtz_job_details";
    String quartzTriggersTableName = "qrtz_triggers";
    String columnName = "description";
    if ("H2".equals(session.sqlDialect())) {
      qrtzJobDetailsTableName = qrtzJobDetailsTableName.toUpperCase(Locale.ROOT);
      quartzTriggersTableName = quartzTriggersTableName.toUpperCase(Locale.ROOT);
      columnName = columnName.toUpperCase(Locale.ROOT);
    }

    assertThat(TEXT.equalsIgnoreCase(dao.getTableColumnType(qrtzJobDetailsTableName, columnName))).isTrue();
    assertThat(TEXT.equalsIgnoreCase(dao.getTableColumnType(quartzTriggersTableName, columnName))).isTrue();
  }
}
