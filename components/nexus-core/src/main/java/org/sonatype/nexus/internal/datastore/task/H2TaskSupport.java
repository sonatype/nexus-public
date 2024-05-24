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
package org.sonatype.nexus.internal.datastore.task;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.scheduling.CancelableHelper;

import com.google.common.annotations.VisibleForTesting;

public class H2TaskSupport extends ComponentSupport
{
  private static final String EXPORT_INITIALISE_SQL
      = "ALTER TABLE \"PUBLIC\".\"NUGET_COMPONENT\" ALTER COLUMN CI_NAME VARCHAR NOT NULL SELECTIVITY 100;";

  private static final String EXPORT_SQL = "SCRIPT";

  private static final String EXPORT_RECOVERY_SQL
      = "ALTER TABLE \"PUBLIC\".\"NUGET_COMPONENT\" ALTER COLUMN CI_NAME VARCHAR AS LOWER(\"NAME\") SELECTIVITY 100;";

  static final int PROGRESS_UPDATE_THRESHOLD = 10_000;

  static final int PROGRESS_LOG_THRESHOLD = 500_000;

  public long exportDatabase(final Connection connection, final String location, final Consumer<String> progressConsumer)
      throws SqlScriptGenerationException {

    boolean autoCommit = false;
    try {
      autoCommit = getAndSetAutoCommit(connection);

      try (PreparedStatement scriptStmt = connection.prepareStatement(EXPORT_INITIALISE_SQL)) {
        scriptStmt.execute();
      }
      try (PreparedStatement scriptStmt = connection.prepareStatement(EXPORT_SQL)) {
        scriptStmt.execute();
        return processResults(scriptStmt.getResultSet(), location, progressConsumer);
      }
    }
    catch (SQLException ex) {
      throw new SqlScriptGenerationException("Script generation failed", ex);
    }
    finally {
      rollback(connection);
      resetAutoCommit(connection, autoCommit);
    }
  }

  @VisibleForTesting
  long processResults(final ResultSet resultSet, final String location, final Consumer<String> progressConsumer)
      throws SQLException {
    if (resultSet != null && location != null && !location.isEmpty()) {
      try (FileWriter writer = new FileWriter(location); BufferedWriter buffer = new BufferedWriter(writer)) {
        long linesWritten = writeLines(resultSet, progressConsumer, PROGRESS_UPDATE_THRESHOLD, PROGRESS_LOG_THRESHOLD, buffer);
        return linesWritten + 1;
      }
      catch (Exception ex) {
        throw new SqlScriptGenerationException("Script generation failed when writing data", ex);
      }
    }
    return 0;
  }

  @VisibleForTesting
  long writeLines(
      final ResultSet resultSet,
      final Consumer<String> progressConsumer,
      final int progressUpdateThreshold,
      final int progressLogThreshold,
      final BufferedWriter buffer) throws SQLException, IOException
  {
    long linesWritten = 0;
    while (resultSet.next()) {
      buffer.write(resultSet.getString(1)); // Always remember SQL columns are 1-indexed !
      buffer.newLine();
      if (++linesWritten % progressUpdateThreshold == 0) {
        updateProgress(progressConsumer, linesWritten);
      }
      if (linesWritten % progressLogThreshold == 0) {
        log.info("Exported {} lines", linesWritten);
      }
    }
    buffer.write(EXPORT_RECOVERY_SQL);
    buffer.newLine();
    buffer.flush();
    return linesWritten;
  }

  @VisibleForTesting
  void updateProgress(final Consumer<String> progressConsumer, final long linesWritten) {
    if (progressConsumer != null) {
      progressConsumer.accept(String.format("%d lines of SQL exported", linesWritten));
    }
    CancelableHelper.checkCancellation();
  }

  @VisibleForTesting
  void rollback(final Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException ex) {
      try (PreparedStatement scriptStmt = connection.prepareStatement(H2TaskSupport.EXPORT_RECOVERY_SQL)) {
        scriptStmt.execute();
      }
      catch (SQLException cex) {
        // If we hit this, there is no recovery
        log.error("Unable to rollback export initialisation. The database may need modification.", ex);
        try {
          connection.close();
        }
        catch (SQLException broken) {
          throw new RuntimeException("Unable to close database connection after failed rollback:", broken);
        }
      }
    }

  }

  @VisibleForTesting
  boolean getAndSetAutoCommit(final Connection connection) throws SQLException {
    boolean autoCommit = connection.getAutoCommit();
    connection.setAutoCommit(false);
    return autoCommit;
  }

  @VisibleForTesting
  void resetAutoCommit(final Connection connection, boolean autoCommit) {
    try {
      connection.setAutoCommit(autoCommit);
    } catch (SQLException ex) {
      // If we hit this, there is no recovery
      log.error("Unable to reset auto commit.", ex);
    }
  }
}
