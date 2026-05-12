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

import org.sonatype.nexus.repository.search.sql.store.upgrade.task.CreateSearchTextPatternOpsIndexTaskDescriptor;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Schedules the {@link org.sonatype.nexus.repository.search.sql.store.upgrade.task.CreateSearchTextPatternOpsIndexTask}
 * to create text_pattern_ops indexes on search_components columns.
 */
@Component
public class SearchTextPatternOpsIndexesMigrationStep_2_118
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final UpgradeTaskScheduler upgradeTaskScheduler;

  @Autowired
  public SearchTextPatternOpsIndexesMigrationStep_2_118(final UpgradeTaskScheduler upgradeTaskScheduler) {
    this.upgradeTaskScheduler = checkNotNull(upgradeTaskScheduler);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.118");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    log.info("Scheduling text_pattern_ops index creation task");
    upgradeTaskScheduler.schedule(
        upgradeTaskScheduler.createTaskConfigurationInstance(CreateSearchTextPatternOpsIndexTaskDescriptor.TYPE_ID));
  }
}
