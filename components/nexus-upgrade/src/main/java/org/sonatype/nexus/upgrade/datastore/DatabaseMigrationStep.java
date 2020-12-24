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
package org.sonatype.nexus.upgrade.datastore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @since 3.29
 */
public interface DatabaseMigrationStep
{
  /**
   * The version this step migrates the database to
   */
  String version();

  /**
   * Perform the migration step. The provided connection should not be closed.
   */
  void migrate(Connection connection) throws Exception;

  /**
   * Indicates whether the migration can occur inside a transaction.
   */
  default boolean canExecuteInTransaction() {
    return true;
  }

  default Integer getChecksum() {
    return null;
  }

  /**
   * Runs the given SQL, returns the number of rows updated or -1 if the result is a set.
   */
  default int runStatement(final Connection connection, final String sql) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      if (stmt.execute()) {
        return -1;
      }
      return stmt.getUpdateCount();
    }
  }

  default boolean isH2(final Connection conn) throws SQLException {
    return "H2".equals(conn.getMetaData().getDatabaseProductName());
  }

  default boolean isPostgresql(final Connection conn) throws SQLException {
    return "PostgreSQL".equals(conn.getMetaData().getDatabaseProductName());
  }
}
