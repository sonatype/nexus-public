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
import java.util.Optional;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.search.sql.store.upgrade.task.ReIndexSearchFilterByPatternTask.FILTER_CONDITION_FIELD_ID;
import static org.sonatype.nexus.repository.search.sql.store.upgrade.task.ReIndexSearchFilterByPatternTask.TYPE_ID;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Schedules a reindex task which filters components that contains underscores for namespace or version fields.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class ReIndexSearchRecordsWithUnderscoreMigrationStep_2_105
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private final TaskScheduler taskScheduler;

  private final UpgradeTaskScheduler upgradeTaskScheduler;

  private final boolean haEnabled;

  @Autowired
  public ReIndexSearchRecordsWithUnderscoreMigrationStep_2_105(
      TaskScheduler taskScheduler,
      UpgradeTaskScheduler upgradeTaskScheduler,
      @Value(FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE) final boolean haEnabled)
  {
    this.haEnabled = haEnabled;
    this.taskScheduler = checkNotNull(taskScheduler);
    this.upgradeTaskScheduler = checkNotNull(upgradeTaskScheduler);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.105");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (haEnabled) {
      scheduleReindexTaskForRecordsWithUnderscore();
    }
    else {
      log.debug("HA not enabled no need to schedule the reindex with filter task");
    }
  }

  private void scheduleReindexTaskForRecordsWithUnderscore() {
    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(TYPE_ID);
    taskConfiguration.setString(FILTER_CONDITION_FIELD_ID, "namespace LIKE '%\\_%' OR version LIKE '%\\_%'");
    upgradeTaskScheduler.schedule(taskConfiguration);
    log.debug("Scheduled reindex with filter task");
  }
}
