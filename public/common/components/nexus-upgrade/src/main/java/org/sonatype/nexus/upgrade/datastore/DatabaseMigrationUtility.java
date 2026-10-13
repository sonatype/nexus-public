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
import org.springframework.stereotype.Component;

/**
 * Implements the schema-inspection helpers exposed as default methods on {@link DatabaseMigrationStep}, as a
 * testable object usable at runtime.
 *
 * @see DatabaseMigrationStep
 */
@Component
public class DatabaseMigrationUtility
{
  /*
   * Backs the schema-inspection default methods on {@link DatabaseMigrationStep}. Shared because this class
   * holds no state; collaborators outside this package inject the {@link Component} instead.
   */
  static final DatabaseMigrationUtility INSTANCE = new DatabaseMigrationUtility();

  public boolean isH2(final Connection conn) throws SQLException {
    return "H2".equals(conn.getMetaData().getDatabaseProductName());
  }

  public boolean isPostgresql(final Connection conn) throws SQLException {
    return "PostgreSQL".equals(conn.getMetaData().getDatabaseProductName());
  }

  public boolean tableExists(final Connection conn, final String tableName) throws SQLException {
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

  public boolean columnExists(
      final Connection conn,
      final String tableName,
      final String columnName) throws SQLException
  {
    if (isPostgresql(conn)) {
      return exists(conn, "SELECT 1 FROM pg_attribute" +
          " WHERE attrelid = to_regclass(?)" +
          "   AND UPPER(attname) = ?" +
          "   AND attnum > 0" +
          "   AND NOT attisdropped",
          tableName, upper(columnName));
    }
    return exists(conn,
        "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = ? AND UPPER(COLUMN_NAME) = ?",
        upper(tableName), upper(columnName));
  }

  public boolean constraintExists(
      final Connection conn,
      final String tableName,
      final String constraintName) throws SQLException
  {
    if (isPostgresql(conn)) {
      return exists(conn, "SELECT 1 FROM pg_constraint WHERE conrelid = to_regclass(?) AND UPPER(conname) = ?",
          tableName, upper(constraintName));
    }
    return exists(conn, "SELECT 1 FROM INFORMATION_SCHEMA.constraint_column_usage" +
        " WHERE UPPER(table_name) = ?" +
        "   AND UPPER(constraint_name) = ?",
        upper(tableName), upper(constraintName));
  }

  public boolean indexExists(
      final Connection conn,
      final String tableName,
      final String indexName) throws SQLException
  {
    if (isPostgresql(conn)) {
      return exists(conn, "SELECT 1 FROM pg_index i JOIN pg_class ic ON ic.oid = i.indexrelid" +
          " WHERE i.indrelid = to_regclass(?)" +
          "   AND UPPER(ic.relname) = ?",
          tableName, upper(indexName));
    }
    else if (isH2(conn)) {
      return exists(conn, "SELECT 1 FROM INFORMATION_SCHEMA.INDEXES" +
          " WHERE REGEXP_LIKE(UPPER(index_name), ?)" +
          "   AND UPPER(TABLE_NAME) = ?",
          upper(indexName) + DatabaseMigrationStep.H2_REGEX_SUFFIX, upper(tableName));
    }
    throw new UnsupportedOperationException();
  }

  public void createIndex(
      final Connection conn,
      final String indexName,
      final String tableName,
      final String columns) throws SQLException
  {
    String sql = "CREATE INDEX ";
    if (isPostgresql(conn)) {
      sql += "CONCURRENTLY ";
    }
    sql += "IF NOT EXISTS " + indexName + " ON " + tableName + " (" + columns + ")";
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.execute();
    }
  }

  public void dropIndex(final Connection conn, final String indexName) throws SQLException {
    String sql = "DROP INDEX IF EXISTS " + indexName;
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.execute();
    }
  }

  public String currentSchema(final Connection conn) throws SQLException {
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

  private boolean exists(final Connection conn, final String sql, final String... parameters) throws SQLException {
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setString(i + 1, parameters[i]);
      }
      try (ResultSet results = statement.executeQuery()) {
        return results.next();
      }
    }
  }

  private static String upper(final String value) {
    return value.toUpperCase(Locale.ENGLISH);
  }
}
