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
 * Sets NOT NULL constraint on normalized_version column for {format}_component tables.
 *
 * Related to NEXUS-49154 - Migration created to allow removal of ALTER TABLE
 * from MyBatis createSchema() which could cause lock contention on startup.
 *
 * Only applies to PostgreSQL as H2 doesn't need this constraint.
 *
 * @since 3.87
 */
@Component
public class ComponentNormalizedVersionConstraintMigrationStep_2_69
    implements DatabaseMigrationStep
{
  private final List<Format> formats;

  @Autowired
  public ComponentNormalizedVersionConstraintMigrationStep_2_69(final List<Format> formats) {
    this.formats = formats;
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.69");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // Only PostgreSQL needs this constraint
    if (!isPostgresql(connection)) {
      return;
    }

    for (Format format : formats) {
      String tableName = format.getValue() + "_component";
      if (tableExists(connection, tableName)) {
        setNormalizedVersionNotNull(connection, tableName);
      }
    }
  }

  private void setNormalizedVersionNotNull(
      final Connection connection,
      final String tableName) throws SQLException
  {
    // Only add NOT NULL constraint if all rows have normalized_version populated
    // This matches the logic that was in ComponentDAO.xml createSchema
    runStatement(connection, String.format("""
        DO $$
        BEGIN
          IF NOT EXISTS (SELECT 1 FROM %s WHERE normalized_version IS NULL) THEN
            BEGIN
              ALTER TABLE %s ALTER COLUMN normalized_version SET NOT NULL;
            EXCEPTION
              WHEN others THEN NULL;
            END;
          END IF;
        END $$;
        """, tableName, tableName));
  }
}
