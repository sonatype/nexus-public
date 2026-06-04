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
import java.util.Optional;

import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BlobRepositoryMismatchMigrationStep_2_106Test
{
  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  private BlobRepositoryMismatchMigrationStep_2_106 underTest;

  @Before
  public void setup() {
    underTest = new BlobRepositoryMismatchMigrationStep_2_106(upgradeTaskScheduler);
  }

  @Test
  public void testVersion() {
    Optional<String> version = underTest.version();

    assertThat(version).isPresent();
    assertThat(version.get()).isEqualTo("2.106");
  }

  @Test
  public void testMigrate() throws Exception {
    TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);
    when(upgradeTaskScheduler.createTaskConfigurationInstance("repository.blob.mismatch.task"))
        .thenReturn(taskConfiguration);

    underTest.migrate(mock(Connection.class));

    verify(upgradeTaskScheduler).createTaskConfigurationInstance("repository.blob.mismatch.task");
    verify(taskConfiguration).setString("repositoryName", "*");
    verify(upgradeTaskScheduler).schedule(taskConfiguration, false);
  }
}
