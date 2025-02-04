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
package org.sonatype.nexus.blobstore.file.upgrades;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Updates PK for soft_deleted_blobs table
 */
@Named
public class SoftDeletedBlobsMigrationStep_1_19
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String CREATE_INDEX =
      "ALTER TABLE soft_deleted_blobs ADD CONSTRAINT pk_soft_deleted_blobs_record_id PRIMARY KEY (record_id)";

  private static final String DROP_INDEX =
      "ALTER TABLE soft_deleted_blobs DROP CONSTRAINT IF EXISTS pk_soft_deleted_blobs_blob_id";

  private static final String H2 = "" +
      "SELECT * " +
      "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
      "WHERE TABLE_NAME = 'SOFT_DELETED_BLOBS' AND CONSTRAINT_NAME = 'PK_SOFT_DELETED_BLOBS_BLOB_ID'";

  private static final String PSQL = "" +
      "SELECT * " +
      "FROM information_schema.table_constraints " +
      "WHERE table_name = 'soft_deleted_blobs' AND constraint_name = 'pk_soft_deleted_blobs_blob_id'";

  @Override
  public Optional<String> version() {
    return Optional.of("1.19");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, "soft_deleted_blobs") && isPKConstraintOutdated(connection)) {
      runStatement(connection, DROP_INDEX);
      runStatement(connection, CREATE_INDEX);
      log.info("PK for table 'soft_deleted_blobs' has changed from 'blob_id' to 'record_id'");
    }
  }

  private boolean isPKConstraintOutdated(final Connection connection) throws SQLException {
    String oldConstraintPresentCheckQuery;
    if (isH2(connection)) {
      oldConstraintPresentCheckQuery = H2;
    }
    else if (isPostgresql(connection)) {
      oldConstraintPresentCheckQuery = PSQL;
    }
    else {
      throw new SQLException("Unexpected DB engine type");
    }

    try (PreparedStatement stmt = connection.prepareStatement(oldConstraintPresentCheckQuery)) {
      return stmt.executeQuery().next();
    }
  }
}
