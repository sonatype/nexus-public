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
package org.sonatype.nexus.quartz.internal.datastore.upgrades;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Adds indexes to Quartz Scheduler tables for existing databases.
 *
 * Related to NEXUS-49154 - Migration created to allow removal of CREATE INDEX
 * from MyBatis createSchema() which could cause lock contention on startup.
 *
 * Creates 18 indexes across qrtz_job_details, qrtz_triggers, and qrtz_fired_triggers tables.
 * These are standard Quartz scheduler indexes required for performant job scheduling.
 *
 * @since 3.87
 */
@Component
public class QuartzIndexesMigrationStep_2_40
    implements DatabaseMigrationStep
{
  @Override
  public Optional<String> version() {
    return Optional.of("2.40");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // Check if main tables exist before creating indexes
    if (tableExists(connection, "qrtz_job_details")) {
      createJobDetailsIndexes(connection);
    }
    if (tableExists(connection, "qrtz_triggers")) {
      createTriggersIndexes(connection);
    }
    if (tableExists(connection, "qrtz_fired_triggers")) {
      createFiredTriggersIndexes(connection);
    }
  }

  private void createJobDetailsIndexes(final Connection connection) throws SQLException {
    createIndexIfNotExists(connection, "idx_qrtz_j_req_recovery",
        "qrtz_job_details", "sched_name, requests_recovery");
    createIndexIfNotExists(connection, "idx_qrtz_j_grp",
        "qrtz_job_details", "sched_name, job_group");
  }

  private void createTriggersIndexes(final Connection connection) throws SQLException {
    createIndexIfNotExists(connection, "idx_qrtz_t_j",
        "qrtz_triggers", "sched_name, job_name, job_group");
    createIndexIfNotExists(connection, "idx_qrtz_t_jg",
        "qrtz_triggers", "sched_name, job_group");
    createIndexIfNotExists(connection, "idx_qrtz_t_c",
        "qrtz_triggers", "sched_name, calendar_name");
    createIndexIfNotExists(connection, "idx_qrtz_t_g",
        "qrtz_triggers", "sched_name, trigger_group");
    createIndexIfNotExists(connection, "idx_qrtz_t_state",
        "qrtz_triggers", "sched_name, trigger_state");
    createIndexIfNotExists(connection, "idx_qrtz_t_n_state",
        "qrtz_triggers", "sched_name, trigger_name, trigger_group, trigger_state");
    createIndexIfNotExists(connection, "idx_qrtz_t_n_g_state",
        "qrtz_triggers", "sched_name, trigger_group, trigger_state");
    createIndexIfNotExists(connection, "idx_qrtz_t_next_fire_time",
        "qrtz_triggers", "sched_name, next_fire_time");
    createIndexIfNotExists(connection, "idx_qrtz_t_nft_st",
        "qrtz_triggers", "sched_name, trigger_state, next_fire_time");
    createIndexIfNotExists(connection, "idx_qrtz_t_nft_misfire",
        "qrtz_triggers", "sched_name, misfire_instr, next_fire_time");
    createIndexIfNotExists(connection, "idx_qrtz_t_nft_st_misfire",
        "qrtz_triggers", "sched_name, misfire_instr, next_fire_time, trigger_state");
    createIndexIfNotExists(connection, "idx_qrtz_t_nft_st_misfire_grp",
        "qrtz_triggers", "sched_name, misfire_instr, next_fire_time, trigger_group, trigger_state");
  }

  private void createFiredTriggersIndexes(final Connection connection) throws SQLException {
    createIndexIfNotExists(connection, "idx_qrtz_ft_trig_inst_name",
        "qrtz_fired_triggers", "sched_name, instance_name");
    createIndexIfNotExists(connection, "idx_qrtz_ft_inst_job_req_rcvry",
        "qrtz_fired_triggers", "sched_name, instance_name, requests_recovery");
    createIndexIfNotExists(connection, "idx_qrtz_ft_j_g",
        "qrtz_fired_triggers", "sched_name, job_name, job_group");
    createIndexIfNotExists(connection, "idx_qrtz_ft_jg",
        "qrtz_fired_triggers", "sched_name, job_group");
    createIndexIfNotExists(connection, "idx_qrtz_ft_t_g",
        "qrtz_fired_triggers", "sched_name, trigger_name, trigger_group");
    createIndexIfNotExists(connection, "idx_qrtz_ft_tg",
        "qrtz_fired_triggers", "sched_name, trigger_group");
  }

  private void createIndexIfNotExists(
      final Connection connection,
      final String indexName,
      final String tableName,
      final String columns) throws SQLException
  {
    if (indexExists(connection, tableName, indexName)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, String.format("""
          DO $$
          BEGIN
            BEGIN
              CREATE INDEX %s ON %s (%s);
            EXCEPTION
              WHEN duplicate_table THEN NULL;
            END;
          END $$;
          """, indexName, tableName, columns));
    }
    else if (isH2(connection)) {
      runStatement(connection,
          String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s)", indexName, tableName, columns));
    }
  }
}
