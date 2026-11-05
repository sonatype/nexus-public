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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.common.app.trash.TrashSchemaVersions;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Re-adds the trash columns ({@code deleted_on}, {@code deleted_by}) to every existing
 * {@code {format}_asset} and {@code {format}_component}.
 *
 * Some customers may have run this as 2.172 but customers using non-public schema may have skipped
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SoftDeleteColumnsMigrationStep_2_177
    implements DatabaseMigrationStep
{
  public static final String VERSION = TrashSchemaVersions.CONTENT;

  private static final Logger log = LoggerFactory.getLogger(SoftDeleteColumnsMigrationStep_2_177.class);

  @Override
  public Optional<String> version() {
    return Optional.of(VERSION);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    List<String> tables = new ArrayList<>();
    tables.addAll(findTablesBySuffix(connection, "_asset"));
    tables.addAll(findTablesBySuffix(connection, "_component"));

    if (tables.isEmpty()) {
      log.info("No content tables found, skipping trash column reintroduction");
      return;
    }

    log.info("Ensuring trash columns on {} content tables", tables.size());
    for (String tableName : tables) {
      if (!isSafeIdentifier(tableName)) {
        log.warn("Skipping table with unsafe name: {}", tableName);
        continue;
      }
      addColumn(connection, tableName, "deleted_on", "TIMESTAMP WITH TIME ZONE");
      addColumn(connection, tableName, "deleted_by", "VARCHAR(200)");
    }
  }

  private void addColumn(
      final Connection connection,
      final String tableName,
      final String column,
      final String type) throws Exception
  {
    if (columnExists(connection, tableName, column)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, String.format("""
          DO $$
          BEGIN
            BEGIN
              ALTER TABLE %s ADD COLUMN %s %s NULL;
            EXCEPTION
              WHEN duplicate_column THEN NULL;
            END;
          END $$;
          """, tableName, column, type));
    }
    else if (isH2(connection)) {
      runStatement(connection,
          String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s %s NULL", tableName, column, type));
    }
  }
}
