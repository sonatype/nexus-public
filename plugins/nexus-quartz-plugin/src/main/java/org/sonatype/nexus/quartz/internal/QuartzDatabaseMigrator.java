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
package org.sonatype.nexus.quartz.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;

/**
 * Migrator of Quartz database.
 *
 * @since 3.0
 */
@Singleton
@Named
public class QuartzDatabaseMigrator
    extends ComponentSupport
{
  private static final String CREATE_VERSION_HISTORY = "CREATE TABLE IF NOT EXISTS VERSION_HISTORY(VERSION INT PRIMARY KEY, DATE_APPLIED TIMESTAMP)";

  private static final String INSERT_VERSION_HISTORY = "INSERT INTO VERSION_HISTORY (VERSION, DATE_APPLIED) VALUES (?, NOW())";

  private static final String SELECT_LATEST_VERSION = "SELECT MAX(VERSION) FROM VERSION_HISTORY";

  /**
   * Perform migration steps if needed to keep Quartz database up to date. Connection will be closed when migration
   * done.
   */
  public void migrateAll(Connection connection, List<QuartzDatabaseMigration> migrations) throws SQLException {
    log.debug("Starting Quartz DB migration...");
    try {
      for (QuartzDatabaseMigration migration : migrations) {

        // Find the current version in the database
        int databaseCurrentVersion = getCurrentVersion(connection);
        if (migration.getVersion() <= databaseCurrentVersion) {
          continue;
        }

        log.info("Starting Quartz DB migration to version {}", migration.getVersion());
        try {
          migration.migrate(connection);
          log.debug("Completed Quartz DB migration to version {}", migration.getVersion());
          updateCurrentVersion(connection, migration.getVersion());
        }
        catch (Exception e) {
          log.error("Error while migrating Quartz DB to version {}", migration.getVersion(), e);
          Throwables.propagate(e);
        }
      }
    }
    finally {
      connection.close();
    }
    log.debug("Completed Quartz DB migration");
  }

  private int getCurrentVersion(Connection connection) throws SQLException {
    // Make sure the version history table actually exists
    final PreparedStatement createTable = connection.prepareStatement(CREATE_VERSION_HISTORY);
    try {
      createTable.execute();
    }
    finally {
      createTable.close();
    }

    final PreparedStatement queryCurrentVersion = connection.prepareStatement(SELECT_LATEST_VERSION);
    try {
      final ResultSet resultSet = queryCurrentVersion.executeQuery();
      if (!resultSet.next()) {
        return -1;
      }
      final int anInt = resultSet.getInt(1);
      if (resultSet.wasNull()) {
        return -1;
      }
      return anInt;
    }
    finally {
      queryCurrentVersion.close();
    }
  }

  private void updateCurrentVersion(Connection connection, int version) throws SQLException {
    final PreparedStatement update = connection.prepareStatement(INSERT_VERSION_HISTORY);
    try {
      update.setInt(1, version);
      update.execute();
    }
    finally {
      update.close();
    }
  }
}