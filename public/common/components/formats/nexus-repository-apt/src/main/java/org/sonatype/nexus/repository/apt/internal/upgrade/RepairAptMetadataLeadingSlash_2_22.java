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
package org.sonatype.nexus.repository.apt.internal.upgrade;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Start task to repair Apt metadata adding leading slash to Filename
 */
@Component
public class RepairAptMetadataLeadingSlash_2_22
    implements DatabaseMigrationStep
{
  private final UpgradeTaskScheduler taskScheduler;

  @Autowired
  public RepairAptMetadataLeadingSlash_2_22(final UpgradeTaskScheduler taskScheduler) {
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.22");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    TaskConfiguration configuration =
        taskScheduler.createTaskConfigurationInstance(RepairAptMetadataLeadingSlashTaskDescriptor.TYPE_ID);
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, RepositoryTaskSupport.ALL_REPOSITORIES);
    taskScheduler.schedule(configuration);
  }
}
