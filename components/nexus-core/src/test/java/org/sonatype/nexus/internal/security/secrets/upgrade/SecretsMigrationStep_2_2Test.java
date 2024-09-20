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
package org.sonatype.nexus.internal.security.secrets.upgrade;

import java.sql.Connection;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.internal.security.secrets.SecretsDAO;
import org.sonatype.nexus.internal.security.secrets.task.SecretsMigrationTaskDescriptor;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class SecretsMigrationStep_2_2Test
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME)
      .access(SecretsDAO.class);

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  private SecretsMigrationStep_2_2 migrationStep;

  private DataSession<?> session;

  @Before
  public void setUp() {
    migrationStep = new SecretsMigrationStep_2_2(upgradeTaskScheduler);
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
  }

  @Test
  public void testMigrationSchedulesTask() throws Exception {
    try (Connection connection = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      migrationStep.migrate(connection);

      verify(upgradeTaskScheduler).createTaskConfigurationInstance(SecretsMigrationTaskDescriptor.TYPE_ID);
      verify(upgradeTaskScheduler).schedule(any());
    }
  }

  @After
  public void cleanup() {
    session.close();
  }
}
