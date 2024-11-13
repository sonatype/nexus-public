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
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Duplicates the default methods from DatabaseMigrationStep in order to make a more testable object at runtime
 * @see DatabaseMigrationStep
 */
@Named
@Singleton
public class DatabaseMigrationUtility
{
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

  public boolean columnExists(final Connection conn, final String tableName, final String columnName)
      throws SQLException
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

  public boolean indexExists(final Connection conn, final String indexName)
      throws SQLException
  {
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

  public String currentSchema(final Connection conn)
      throws SQLException
  {
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
