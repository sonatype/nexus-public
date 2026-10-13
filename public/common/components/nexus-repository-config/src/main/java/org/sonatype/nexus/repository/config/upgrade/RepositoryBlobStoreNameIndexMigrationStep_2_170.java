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
package org.sonatype.nexus.repository.config.upgrade;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Component;

/**
 * Adds the index backing {@code ConfigurationDAO.countByBlobStore()} -- the authoritative "is this blob store
 * used by any repository" check ({@code RepositoryManager.isBlobstoreUsed}) and the blob store list UI's usage
 * batch ({@code RepositoryManager.blobstoresInUse}). See NEXUS-54229.
 * <p>
 * The blob store name lives inside the {@code repository.attributes} JSON at {@code storage.blobStoreName}.
 * Only PostgreSQL can index a JSON-path expression; H2's {@code countByBlobStore} falls back to an unindexable
 * text scan, so there is nothing to create there.
 * <p>
 * Per NEXUS-49154, index creation lives in a migration step rather than MyBatis {@code createSchema()} to avoid
 * lock contention on startup.
 */
@Component
public class RepositoryBlobStoreNameIndexMigrationStep_2_170
    implements DatabaseMigrationStep
{
  @VisibleForTesting
  static final String TABLE_NAME = "repository";

  @VisibleForTesting
  static final String INDEX_NAME = "idx_repository_blob_store_name";

  @Override
  public Optional<String> version() {
    return Optional.of("2.170");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!isPostgresql(connection) || !tableExists(connection, TABLE_NAME)
        || indexExists(connection, TABLE_NAME, INDEX_NAME)) {
      return;
    }

    runStatement(connection, String.format(
        "CREATE INDEX IF NOT EXISTS %s ON %s ((attributes -> 'storage' ->> 'blobStoreName'))",
        INDEX_NAME, TABLE_NAME));
  }
}
