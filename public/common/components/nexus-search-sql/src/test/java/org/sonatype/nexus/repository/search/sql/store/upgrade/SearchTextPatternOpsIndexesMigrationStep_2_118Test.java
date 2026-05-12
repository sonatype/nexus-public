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

import org.sonatype.nexus.repository.search.sql.store.upgrade.task.CreateSearchTextPatternOpsIndexTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SearchTextPatternOpsIndexesMigrationStep_2_118}
 */
@ExtendWith(MockitoExtension.class)
class SearchTextPatternOpsIndexesMigrationStep_2_118Test
{
  @Mock
  private Connection connection;

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @InjectMocks
  private SearchTextPatternOpsIndexesMigrationStep_2_118 underTest;

  @Test
  void testVersion() {
    assertTrue(underTest.version().isPresent(), "version should be present");
    assertEquals("2.118", underTest.version().get(), "version should be 2.118");
  }

  @Test
  void testMigrate_schedulesTask() throws Exception {
    TaskConfiguration mockTaskConfiguration = stubTaskConfiguration();

    underTest.migrate(connection);

    verify(upgradeTaskScheduler).createTaskConfigurationInstance(
        CreateSearchTextPatternOpsIndexTaskDescriptor.TYPE_ID);

    ArgumentCaptor<TaskConfiguration> configCaptor = ArgumentCaptor.forClass(TaskConfiguration.class);
    verify(upgradeTaskScheduler).schedule(configCaptor.capture());

    assertThat("Scheduled task configuration should match created configuration",
        configCaptor.getValue(),
        equalTo(mockTaskConfiguration));
  }

  @Test
  void testMigrate_calledOnce() throws Exception {
    stubTaskConfiguration();

    underTest.migrate(connection);

    verify(upgradeTaskScheduler)
        .createTaskConfigurationInstance(CreateSearchTextPatternOpsIndexTaskDescriptor.TYPE_ID);
    verify(upgradeTaskScheduler).schedule(any(TaskConfiguration.class));
  }

  private TaskConfiguration stubTaskConfiguration() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(upgradeTaskScheduler.createTaskConfigurationInstance(
        CreateSearchTextPatternOpsIndexTaskDescriptor.TYPE_ID))
            .thenReturn(config);
    return config;
  }
}
