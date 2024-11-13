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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
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

  private final static String CHARVAR = "character varying";

  private final static String TEXT = "text";

  private final static String ONE_BILLION_CHARACTERS = "1000000000";

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
      assertThat(dao.getTableColumnType(qrtzJobDetailsTableName, columnName), equalToIgnoringCase(CHARVAR));
      assertThat(dao.getTableColumnType(quartzTriggersTableName, columnName), equalToIgnoringCase(CHARVAR));

      // h2 version 2.2.224 no longer supports 'text' data_type, but the max length
      // of character varying is set high for this column, so we are now testing the length as well
      assertThat(dao.getColumnCharacterLimit(qrtzJobDetailsTableName, columnName), is(ONE_BILLION_CHARACTERS));
      assertThat(dao.getColumnCharacterLimit(quartzTriggersTableName, columnName), is(ONE_BILLION_CHARACTERS));
    } else {
      assertThat(dao.getTableColumnType(qrtzJobDetailsTableName, columnName), equalToIgnoringCase(TEXT));
      assertThat(dao.getTableColumnType(quartzTriggersTableName, columnName), equalToIgnoringCase(TEXT));
    }
  }
}
