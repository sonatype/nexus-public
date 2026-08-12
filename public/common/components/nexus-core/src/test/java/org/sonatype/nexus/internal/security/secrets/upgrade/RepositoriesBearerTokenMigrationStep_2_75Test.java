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

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.internal.security.secrets.SecretsDAO;
import org.sonatype.nexus.internal.security.secrets.task.RepositoriesBearerTokenConfigMigrationTaskDescriptor;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

class RepositoriesBearerTokenMigrationStep_2_75Test
    extends Test5Support
{
  @DataSessionConfiguration(daos = SecretsDAO.class)
  TestDataSessionSupplier dataSession;

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @InjectMocks
  private RepositoriesBearerTokenMigrationStep_2_75 migrationStep;

  @DatabaseTest
  void testMigrationSchedulesTaskWhenSecretsTableExists() throws Exception {
    try (Connection connection = dataSession.openConnection(DEFAULT_DATASTORE_NAME)) {
      migrationStep.migrate(connection);

      verify(upgradeTaskScheduler)
          .createTaskConfigurationInstance(RepositoriesBearerTokenConfigMigrationTaskDescriptor.TYPE_ID);
      verify(upgradeTaskScheduler).schedule(any());
    }
  }
}
