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
package org.sonatype.nexus.self.hosted.blobstore.s3.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Our S3 blob store implementation previously used the S3 expiration policy, this schedules compact tasks
 */
@Component
public class ScheduleS3CompactTasksMigrationStep
    implements RepeatableDatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String TABLE = "blob_store_configuration";

  private static final String SELECT_COUNT = """
      SELECT count(*)
        FROM %s
       WHERE type = ?;
      """.formatted(TABLE);

  private final UpgradeTaskScheduler upgradeTaskScheduler;

  @Autowired
  public ScheduleS3CompactTasksMigrationStep(final UpgradeTaskScheduler upgradeTaskScheduler) {
    this.upgradeTaskScheduler = checkNotNull(upgradeTaskScheduler);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, TABLE)) {
      try (PreparedStatement statement = connection.prepareStatement(SELECT_COUNT)) {
        statement.setString(1, S3BlobStore.TYPE);
        ResultSet results = statement.executeQuery();
        if (!results.next()) {
          log.debug("No results");
          return;
        }
        if (results.getInt(1) > 0) {
          upgradeTaskScheduler.schedule(
              upgradeTaskScheduler.createTaskConfigurationInstance(ScheduleS3CompactTasksTaskDescriptor.TYPE_ID));
        }
      }
    }
  }

  @Override
  public Integer getChecksum() {
    return 1;
  }
}
