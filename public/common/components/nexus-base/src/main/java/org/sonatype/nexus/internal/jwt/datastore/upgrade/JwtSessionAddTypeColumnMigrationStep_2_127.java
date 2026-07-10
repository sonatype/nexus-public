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
package org.sonatype.nexus.internal.jwt.datastore.upgrade;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Adds the {@code type} discriminator column to the {@code jwt_session} table.
 *
 * The column splits rows into two kinds: {@code SESSION} (per-session revocations from logout)
 * and {@code USER_INVALIDATION} (per-user cutoffs recorded on password change, consulted by
 * {@code JwtSecurityFilter} against each JWT's {@code iat}). See NEXUS-52579.
 */
@Component
public class JwtSessionAddTypeColumnMigrationStep_2_127
    implements DatabaseMigrationStep
{
  private static final String TABLE_NAME = "jwt_session";

  private static final String COLUMN_NAME = "type";

  @Override
  public Optional<String> version() {
    return Optional.of("2.127");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, TABLE_NAME)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, """
          DO $$
          BEGIN
            BEGIN
              ALTER TABLE jwt_session ADD COLUMN type VARCHAR(32) NOT NULL DEFAULT 'SESSION';
            EXCEPTION
              WHEN duplicate_column THEN NULL;
            END;
          END $$;
          """);
    }
    else if (isH2(connection) && !columnExists(connection, TABLE_NAME, COLUMN_NAME)) {
      runStatement(connection,
          "ALTER TABLE jwt_session ADD COLUMN type VARCHAR(32) NOT NULL DEFAULT 'SESSION'");
    }
  }
}
