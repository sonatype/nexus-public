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
package org.sonatype.nexus.repository.content.search.upgrade;

import java.sql.Connection;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_TABLE_SEARCH_NAMED;

/**
 * For SQL Search, change the column created to last_updated and trigger a re-index
 */
@Named
public class SqlSearchMigrationStep_1_29
    extends SearchIndexUpgrade
{
  private final String RENAME_QUERY = "ALTER TABLE search_components RENAME COLUMN component_created TO last_modified;";

  private final String NOT_NULL_QUERY = "ALTER TABLE search_components ALTER COLUMN last_modified DROP NOT NULL;";

  private final String TABLE = "search_components";

  private final String COMPONENT_CREATED = "component_created";

  private final String LAST_MODIFIED = "last_modified";

  private final boolean sqlSearchEnabled;

  @Inject
  public SqlSearchMigrationStep_1_29(@Named(DATASTORE_TABLE_SEARCH_NAMED) final boolean sqlSearchEnabled) {
    this.sqlSearchEnabled = sqlSearchEnabled;
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.29");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // We only need to do this for PostgreSQL, and the query will fail if the column exists
    if (isPostgresql(connection) && columnExists(connection, TABLE, COMPONENT_CREATED)) {
      log.info(String.format("Renaming column %s to %s", COMPONENT_CREATED, LAST_MODIFIED));
      runStatement(connection, RENAME_QUERY);

      log.info(String.format("Dropping null constraint"));
      runStatement(connection, NOT_NULL_QUERY);
    }

    // We only need to trigger migration if the user is using SQL Search
    if (sqlSearchEnabled) {
      super.migrate(connection);
    }
  }
}
