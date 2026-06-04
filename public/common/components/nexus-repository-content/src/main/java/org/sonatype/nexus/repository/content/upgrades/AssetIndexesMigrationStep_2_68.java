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

import org.springframework.stereotype.Component;

/**
 * Adds H2-only indexes to {format}_asset tables for existing databases.
 *
 * Related to NEXUS-49154 - Migration created to allow removal of CREATE INDEX
 * from MyBatis createSchema() which could cause lock contention on startup.
 *
 * Note: PostgreSQL indexes are handled by AssetTableIndexesMigrationStep_2_32
 *
 * @since 3.87
 */
@Component
public class AssetIndexesMigrationStep_2_68
    implements DatabaseMigrationStep
{
  private final List<Format> formats;

  @Autowired
  public AssetIndexesMigrationStep_2_68(final List<Format> formats) {
    this.formats = formats;
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.68");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // Only H2 needs these indexes created here
    // PostgreSQL indexes were created in AssetTableIndexesMigrationStep_2_32
    if (!isH2(connection)) {
      return;
    }

    for (Format format : formats) {
      String tableName = format.getValue() + "_asset";
      if (tableExists(connection, tableName)) {
        createIndexes(connection, format.getValue(), tableName);
      }
    }
  }

  private void createIndexes(
      final Connection connection,
      final String format,
      final String tableName) throws SQLException
  {
    createIndexIfNotExists(connection, "idx_" + format + "_asset_kind",
        tableName, "kind");

    createIndexIfNotExists(connection, "idx_" + format + "_asset_component",
        tableName, "component_id");

    createIndexIfNotExists(connection, "idx_" + format + "_asset_blob",
        tableName, "asset_blob_id");

    createIndexIfNotExists(connection, "idx_" + format + "_asset_last_downloaded",
        tableName, "last_downloaded");

    createIndexIfNotExists(connection, "idx_" + format + "_asset_path",
        tableName, "path");
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

    runStatement(connection,
        String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s)", indexName, tableName, columns));
  }
}
