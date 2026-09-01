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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
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
    return "H2".equals(conn.getMetaData().getDatabaseProductName());
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
    return "PostgreSQL".equals(conn.getMetaData().getDatabaseProductName());
  }

  default boolean tableExists(final Connection conn, final String tableName) throws SQLException {
    if (isH2(conn)) {
      try (PreparedStatement statement =
          conn.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?")) {
        statement.setString(1, tableName.toUpperCase());
        try (ResultSet results = statement.executeQuery()) {
          return results.next();
        }
      }
    }
    else if (isPostgresql(conn)) {
      try (PreparedStatement statement = conn.prepareStatement("SELECT to_regclass(?);")) {
        statement.setString(1, tableName);
        try (ResultSet results = statement.executeQuery()) {
          if (!results.next()) {
            return false;
          }
          Object oid = results.getObject(1);
          return oid != null;
        }
      }
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  default boolean columnExists(
      final Connection conn,
      final String tableName,
      final String columnName) throws SQLException
  {
    String sql = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = ? AND UPPER(COLUMN_NAME) = ?";
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, tableName.toUpperCase());
      statement.setString(2, columnName.toUpperCase());
      try (ResultSet results = statement.executeQuery()) {
        return results.next();
      }
    }
  }

  default boolean constraintExists(
      final Connection conn,
      final String tableName,
      final String constraintName) throws SQLException
  {
    String sql = "SELECT * FROM INFORMATION_SCHEMA.constraint_column_usage " +
        " WHERE UPPER(table_name) = ?" +
        "   AND UPPER(constraint_name) = ?";
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, tableName.toUpperCase());
      statement.setString(2, constraintName.toUpperCase());
      try (ResultSet results = statement.executeQuery()) {
        return results.next();
      }
    }
  }

  default boolean indexExists(final Connection conn, final String indexName) throws SQLException {
    if (isPostgresql(conn)) {
      String currentSchema = currentSchema(conn);
      String sql = "SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS" +
          " WHERE UPPER(constraint_name) = ?" +
          "   AND UPPER(constraint_schema) = ?";

      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setString(1, indexName.toUpperCase());
        statement.setString(2, currentSchema.toUpperCase());
        try (ResultSet results = statement.executeQuery()) {
          return results.next();
        }
      }
    }
    else if (isH2(conn)) {
      String sql = "SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS" +
          " WHERE UPPER(constraint_name) = ?";
      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setString(1, indexName.toUpperCase());
        try (ResultSet results = statement.executeQuery()) {
          return results.next();
        }
      }
    }
    throw new UnsupportedOperationException();
  }

  default boolean indexExists(
      final Connection conn,
      final String tableName,
      final String indexName) throws SQLException
  {
    if (isPostgresql(conn)) {
      String currentSchema = currentSchema(conn);
      String sql = "SELECT * FROM pg_indexes WHERE UPPER(tablename) = ? AND UPPER(indexname) = ?";

      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setString(1, tableName.toUpperCase(Locale.ENGLISH));
        statement.setString(2, indexName.toUpperCase(Locale.ENGLISH));
        try (ResultSet results = statement.executeQuery()) {
          return results.next();
        }
      }
    }
    else if (isH2(conn)) {
      String sql =
          "SELECT * FROM information_schema.indexes WHERE REGEXP_LIKE(UPPER(index_name), ?) AND UPPER(TABLE_NAME) = ?";
      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setString(1, (indexName.toUpperCase(Locale.ENGLISH) + H2_REGEX_SUFFIX));
        statement.setString(2, tableName.toUpperCase(Locale.ENGLISH));
        try (ResultSet results = statement.executeQuery()) {
          return results.next();
        }
      }
    }
    throw new UnsupportedOperationException();
  }

  default String currentSchema(final Connection conn) throws SQLException {
    String sql = "select current_schema()";
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      try (ResultSet results = statement.executeQuery()) {
        if (results.next()) {
          return results.getString(1);
        }
      }
    }
    throw new IllegalStateException("Unable to determine current database schema");
  }
}
