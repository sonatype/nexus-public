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
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Migration step to create an index for idx_soft_deleted_blobs_by_source_blob_store_name_record_id
 */
@Named
public class SoftDeletedBlobsByBlobStoreIndexMigrationStep_2_7
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private final String ADD_INDEX_STATEMENT =
      "CREATE INDEX IF NOT EXISTS idx_soft_deleted_blobs_by_source_blob_store_name_record_id"
          + " ON soft_deleted_blobs (source_blob_store_name, record_id);";

  @Override
  public Integer getChecksum() {
    return Objects.hash(ADD_INDEX_STATEMENT);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.7");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    long startTime = System.currentTimeMillis();
    log.info("Creating index idx_soft_deleted_blobs_by_source_blob_store_name_record_id");

    try(Statement statement = connection.createStatement()) {
      statement.execute(ADD_INDEX_STATEMENT);
    }

    log.info("Index idx_soft_deleted_blobs_by_source_blob_store_name_record_id created successfully in {} seconds.",
        (System.currentTimeMillis() - startTime) * 0.001d);
  }
}
