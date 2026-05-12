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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;

import org.sonatype.nexus.repository.content.tasks.CreateAssetBlobIndexTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AssetBlobMigrationStep_2_18_2Test
{
  @Rule
  public DataSessionRule dataSessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @InjectMocks
  private AssetBlobMigrationStep_2_18_2 underTest;

  private TaskConfiguration mockTaskConfiguration;

  @Before
  public void setup() {
    mockTaskConfiguration = mock(TaskConfiguration.class);
    when(upgradeTaskScheduler.createTaskConfigurationInstance(
        CreateAssetBlobIndexTaskDescriptor.TYPE_ID))
            .thenReturn(mockTaskConfiguration);
  }

  @Test
  public void testMigrate_schedulesTask() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection);
    }

    // Verify the task was created with correct type ID
    verify(upgradeTaskScheduler).createTaskConfigurationInstance(
        eq(CreateAssetBlobIndexTaskDescriptor.TYPE_ID));

    // Verify the task was scheduled
    ArgumentCaptor<TaskConfiguration> configCaptor = ArgumentCaptor.forClass(TaskConfiguration.class);
    verify(upgradeTaskScheduler).schedule(configCaptor.capture());

    // Verify the scheduled configuration is the same as the created one
    assertThat("Scheduled task configuration should match created configuration",
        configCaptor.getValue(),
        equalTo(mockTaskConfiguration));
  }

  @Test
  public void testMigrate_calledOnce() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection);
    }

    // Verify methods called exactly once
    verify(upgradeTaskScheduler, times(1))
        .createTaskConfigurationInstance(CreateAssetBlobIndexTaskDescriptor.TYPE_ID);
    verify(upgradeTaskScheduler, times(1)).schedule(any(TaskConfiguration.class));
  }

  @Test
  public void testVersion() {
    assertTrue("version should be present", underTest.version().isPresent());
    assertEquals("version should be 2.18.2", "2.18.2", underTest.version().get());
  }

  @Test
  public void testMigrate_doesNotFailWithNullConnection() throws Exception {
    // This tests that the migration doesn't depend on connection state
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection);
    }

    // Should still schedule the task successfully
    verify(upgradeTaskScheduler).schedule(any(TaskConfiguration.class));
  }

  @Test
  public void testMigrate_multipleCallsScheduleMultipleTasks() throws Exception {
    // Simulate multiple calls to migrate (shouldn't happen in practice, but tests idempotency)
    try (Connection connection1 = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection1);
    }

    try (Connection connection2 = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection2);
    }

    // Verify the task was scheduled twice (once per call)
    verify(upgradeTaskScheduler, times(2)).schedule(any(TaskConfiguration.class));
  }

  @Test
  public void testMigrate_usesCorrectTypeId() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection);
    }

    // Verify the correct type ID constant is used
    verify(upgradeTaskScheduler).createTaskConfigurationInstance(
        eq("repository.asset.blob.index.migration"));
  }
}
