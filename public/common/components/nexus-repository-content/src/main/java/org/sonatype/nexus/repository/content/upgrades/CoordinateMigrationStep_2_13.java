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
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.repository.content.tasks.CreateComponentIndexTaskDescriptor;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

@Component
public class CoordinateMigrationStep_2_13
    implements DatabaseMigrationStep
{
  private static final Logger LOG = LoggerFactory.getLogger(CoordinateMigrationStep_2_13.class);

  private final UpgradeTaskScheduler upgradeTaskScheduler;

  @Autowired
  public CoordinateMigrationStep_2_13(final UpgradeTaskScheduler upgradeTaskScheduler) {
    this.upgradeTaskScheduler = checkNotNull(upgradeTaskScheduler);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.13");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    LOG.info("Scheduling CreateComponentIndexTask to run after upgrade is completed.");
    upgradeTaskScheduler.schedule(upgradeTaskScheduler.createTaskConfigurationInstance(
        CreateComponentIndexTaskDescriptor.TYPE_ID));
  }
}
