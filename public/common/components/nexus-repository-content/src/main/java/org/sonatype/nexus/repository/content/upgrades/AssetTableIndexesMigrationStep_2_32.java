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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static java.util.Objects.requireNonNull;
import org.springframework.stereotype.Component;

/**
 * Adds indexes to {format}_asset tables for performance.
 *
 * Indexes: kind, component_id, asset_blob_id, last_downloaded, path
 *
 * Related to NEXUS-49154 - Migration created to move CREATE INDEX statements
 * from MyBatis createSchema() to migrations, avoiding repeated index checks on startup.
 *
 * @since 3.87
 */
@Component
public class AssetTableIndexesMigrationStep_2_32
    implements DatabaseMigrationStep
{
  private final List<Format> formats;

  @Autowired
  public AssetTableIndexesMigrationStep_2_32(final List<Format> formats) {
    this.formats = requireNonNull(formats);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.32");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    for (Format format : formats) {
      String tableName = format.getValue() + "_asset";
      String formatValue = format.getValue();

      if (tableExists(connection, tableName)) {
        createIndexIfNotExists(connection, "idx_" + formatValue + "_asset_kind",
            tableName, "kind");
        createIndexIfNotExists(connection, "idx_" + formatValue + "_asset_component",
            tableName, "component_id");
        createIndexIfNotExists(connection, "idx_" + formatValue + "_asset_blob",
            tableName, "asset_blob_id");
        createIndexIfNotExists(connection, "idx_" + formatValue + "_asset_last_downloaded",
            tableName, "last_downloaded");
        createIndexIfNotExists(connection, "idx_" + formatValue + "_asset_path",
            tableName, "path");
      }
    }
  }

  private void createIndexIfNotExists(
      final Connection connection,
      final String indexName,
      final String tableName,
      final String columnName) throws SQLException
  {
    if (!indexExists(connection, indexName)) {
      runStatement(connection, String.format(
          "CREATE INDEX IF NOT EXISTS %s ON %s (%s)",
          indexName, tableName, columnName));
    }
  }
}
