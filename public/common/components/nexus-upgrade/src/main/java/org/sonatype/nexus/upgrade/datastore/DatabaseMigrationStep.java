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
import java.util.Optional;

/**
 * @since 3.29
 */
public interface DatabaseMigrationStep
{
  String H2_REGEX_SUFFIX = "[_[0-9]*]?";

  /**
   * The version this step migrates the database to. Migrations returning an empty value will only run when the checksum
   * changes.
   */
  Optional<String> version();

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
    return DatabaseMigrationUtility.INSTANCE.isH2(conn);
  }

  /**
   * Binds a JSON value to a prepared-statement parameter using the dialect-correct type: H2 stores JSON(B)
   * as a {@code byte[]}, PostgreSQL as a UTF-8 {@code String}. Delegates to the shared
   * {@link JsonParameterBinder} (the single source of truth also used by
   * {@code UpgradeConfigStoreSupport#setJsonParameter}) so the two dialect bindings cannot drift — a
   * wrong-dialect binding silently corrupts the stored JSON.
   */
  default void setJsonParameter(
      final PreparedStatement statement,
      final int index,
      final byte[] json,
      final boolean isH2) throws SQLException
  {
    JsonParameterBinder.setJsonParameter(statement, index, json, isH2);
  }

  default boolean isPostgresql(final Connection conn) throws SQLException {
    return DatabaseMigrationUtility.INSTANCE.isPostgresql(conn);
  }

  default boolean tableExists(final Connection conn, final String tableName) throws SQLException {
    return DatabaseMigrationUtility.INSTANCE.tableExists(conn, tableName);
  }

  /**
   * Whether {@code columnName} exists on the table {@code tableName} resolves to.
   */
  default boolean columnExists(
      final Connection conn,
      final String tableName,
      final String columnName) throws SQLException
  {
    return DatabaseMigrationUtility.INSTANCE.columnExists(conn, tableName, columnName);
  }

  /**
   * Whether {@code constraintName} exists on the table {@code tableName} resolves to.
   */
  default boolean constraintExists(
      final Connection conn,
      final String tableName,
      final String constraintName) throws SQLException
  {
    return DatabaseMigrationUtility.INSTANCE.constraintExists(conn, tableName, constraintName);
  }

  /**
   * Whether an index named {@code indexName} exists on the table {@code tableName} resolves to.
   */
  default boolean indexExists(
      final Connection conn,
      final String tableName,
      final String indexName) throws SQLException
  {
    return DatabaseMigrationUtility.INSTANCE.indexExists(conn, tableName, indexName);
  }

  default String currentSchema(final Connection conn) throws SQLException {
    return DatabaseMigrationUtility.INSTANCE.currentSchema(conn);
  }
}
