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
package org.sonatype.nexus.internal.security.apikey.upgrade;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Adds creator tracking columns to api_key_v2 table for admin token operations.
 *
 * Related to NEXUS-50483 - Admin User Token Management
 * Related to NEXUS-50484 - Admin Token Auditing and Governance
 */
@Component
public class ApiKeyCreatorTrackingMigrationStep_2_108
    implements DatabaseMigrationStep
{
  private static final String TABLE_NAME = "api_key_v2";

  @Override
  public Optional<String> version() {
    return Optional.of("2.108");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, TABLE_NAME)) {
      addCreatorTrackingColumns(connection);
    }
  }

  private void addCreatorTrackingColumns(final Connection connection) throws Exception {
    if (isPostgresql(connection)) {
      // Use DO block with exception handling to protect against concurrent execution (NEXUS-49154 pattern)
      runStatement(connection, """
          DO $$
          BEGIN
            BEGIN
              ALTER TABLE api_key_v2 ADD COLUMN created_by_user_id VARCHAR(200);
            EXCEPTION
              WHEN duplicate_column THEN NULL;
            END;
            BEGIN
              ALTER TABLE api_key_v2 ADD COLUMN created_by_realm VARCHAR(200);
            EXCEPTION
              WHEN duplicate_column THEN NULL;
            END;
          END $$;
          """);
    }
    else if (isH2(connection)) {
      // H2 supports IF NOT EXISTS natively
      if (!columnExists(connection, TABLE_NAME, "created_by_user_id")) {
        runStatement(connection,
            "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS created_by_user_id VARCHAR(200)");
      }
      if (!columnExists(connection, TABLE_NAME, "created_by_realm")) {
        runStatement(connection,
            "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS created_by_realm VARCHAR(200)");
      }
    }
  }
}
