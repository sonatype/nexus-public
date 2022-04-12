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
package org.sonatype.nexus.quartz.internal.upgrades;

import java.sql.Connection;
import java.util.Optional;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Resolve issue when job can't be scheduled due to limited size of 'description' column
 *
 * @since 3.37
 */
@Named
public class JobDescriptionMigrationStep_1_3
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String QRTZ_JOB_DETAILS = "qrtz_job_details";

  private static final String QUARTZ_TRIGGERS = "qrtz_triggers";

  private static final String STATEMENT = "ALTER TABLE %s ALTER COLUMN description TYPE text";

  @Override
  public Optional<String> version() {
    return Optional.of("1.3");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, QRTZ_JOB_DETAILS) && tableExists(connection, QUARTZ_TRIGGERS)) {
      log.info("Changing 'description' column type from varchar(250) to text in table {}", QRTZ_JOB_DETAILS);
      runStatement(connection, String.format(STATEMENT, QRTZ_JOB_DETAILS));

      log.info("Changing 'description' column type from varchar(250) to text in table {}", QUARTZ_TRIGGERS);
      runStatement(connection, String.format(STATEMENT, QUARTZ_TRIGGERS));
    }
    else {
      log.warn("Table {} or {} doesn't exist, upgrade step {} will be skipped",
          QRTZ_JOB_DETAILS, QUARTZ_TRIGGERS, this.getClass().getSimpleName());
    }
  }
}
