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
import java.util.Set;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Adds composite index for retain N cleanup queries on all retain-enabled format component tables.
 *
 * Index covers the full retain query access pattern: repository_id, namespace, name, normalized_version DESC.
 * This allows efficient ORDER BY + OFFSET without sort-on-disk.
 *
 * Related to NEXUS-52740 - Cleanup Policy Enhancements (Retain N for all formats).
 */
@Component
public class RetainNCompositeIndexMigrationStep_2_144
    implements DatabaseMigrationStep
{
  private static final Set<String> RETAIN_FORMATS = Set.of(
      "maven2", "docker", "npm", "pypi", "go", "helm", "nuget",
      "yum", "rubygems", "terraform", "swift", "apt", "pub");

  private final List<Format> formats;

  @Autowired
  public RetainNCompositeIndexMigrationStep_2_144(final List<Format> formats) {
    this.formats = formats;
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.144");
  }

  @Override
  public boolean canExecuteInTransaction() {
    return false; // this is required to use CONCURRENTLY in the CREATE INDEX for postgresql
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    for (Format format : formats) {
      String formatName = format.getValue();
      if (!RETAIN_FORMATS.contains(formatName)) {
        continue;
      }
      String tableName = formatName + "_component";
      if (tableExists(connection, tableName)) {
        createRetainIndex(connection, formatName, tableName);
      }
    }
  }

  private void createRetainIndex(
      final Connection connection,
      final String format,
      final String tableName) throws SQLException
  {
    String indexName = "idx_" + format + "_component_retain";
    if (indexExists(connection, tableName, indexName)) {
      return;
    }
    if (isPostgresql(connection)) {
      runStatement(connection,
          String.format(
              "CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON %s (repository_id, namespace, name, normalized_version DESC)",
              indexName, tableName));
    }
    else if (isH2(connection)) {
      runStatement(connection,
          String.format("CREATE INDEX IF NOT EXISTS %s ON %s (repository_id, namespace, name, normalized_version DESC)",
              indexName, tableName));
    }
  }
}
