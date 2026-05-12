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
package org.sonatype.nexus.repository.search.sql.store.upgrade;

import java.sql.Connection;

import org.sonatype.nexus.repository.search.sql.store.upgrade.task.CleanOrphanedSearchRecordsTask;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemoveOrphanedSearchRecordsMigrationStep_2_76Test
{
  @Mock
  private Connection connection;

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  private RemoveOrphanedSearchRecordsMigrationStep_2_76 underTest;

  @BeforeEach
  void setUp() {
    underTest = spy(new RemoveOrphanedSearchRecordsMigrationStep_2_76(upgradeTaskScheduler));
  }

  @Test
  void testSchedulesTaskIfTablesExist() throws Exception {
    doReturn(true).when(underTest).tableExists(eq(connection), anyString());

    underTest.migrate(connection);

    verify(underTest).tableExists(eq(connection), eq("search_components"));
    verify(underTest).tableExists(eq(connection), eq("search_assets"));
    verify(upgradeTaskScheduler).schedule(
        upgradeTaskScheduler.createTaskConfigurationInstance(CleanOrphanedSearchRecordsTask.TYPE_ID));
  }

  @Test
  void testDoesNotScheduleTaskIfTablesDoNotExist() throws Exception {
    doReturn(false).when(underTest).tableExists(eq(connection), anyString());

    underTest.migrate(connection);

    verify(underTest).tableExists(eq(connection), eq("search_components"));
    verify(underTest).tableExists(eq(connection), eq("search_assets"));
    verify(upgradeTaskScheduler, never()).schedule(
        upgradeTaskScheduler.createTaskConfigurationInstance(CleanOrphanedSearchRecordsTask.TYPE_ID));
  }
}
