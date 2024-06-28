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
package org.sonatype.nexus.repository.content.blobstore.metrics.migration;

import java.sql.Connection;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.blobstore.metrics.migration.FileBlobStoreMetricsMigrationTaskDescriptor.TYPE_ID;

/**
 * Manage {@link FileBlobStoreMetricsMigrationTask} task scheduling.
 */
@Named
@Singleton
public class FileBlobStoreMetricsMigrationStep
    extends RepeatableDatabaseMigrationStep
{
  private final UpgradeTaskScheduler upgradeTaskScheduler;

  @Inject
  public FileBlobStoreMetricsMigrationStep(
      final UpgradeTaskScheduler upgradeTaskScheduler)
  {
    this.upgradeTaskScheduler = checkNotNull(upgradeTaskScheduler);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    TaskConfiguration taskConfiguration = upgradeTaskScheduler.createTaskConfigurationInstance(TYPE_ID);
    upgradeTaskScheduler.schedule(taskConfiguration);
  }

  @Override
  public Integer getChecksum() {
    return 0;
  }
}
