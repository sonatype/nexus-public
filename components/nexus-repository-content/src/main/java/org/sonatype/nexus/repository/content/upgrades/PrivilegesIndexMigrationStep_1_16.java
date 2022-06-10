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
import java.sql.Statement;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Creates an index by name for the security privilege table
 *
 * @since 3.next
 */
@Named
@Singleton
public class PrivilegesIndexMigrationStep_1_16
    extends ComponentSupport
    implements DatabaseMigrationStep

{
  private static final String CREATE_INDEX_STATEMENT = "CREATE INDEX IF NOT EXISTS %s ON %s";

  private static final String PRIVILEGE_BY_NAME_IDX_NAME = "idx_privilege_by_name";
  private static final String PRIVILEGE_BY_NAME_IDX_METADATA = "privilege(name)";

  @Override
  public Optional<String> version() {
    return Optional.of("1.16");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    try (Statement statement = connection.createStatement()) {
        createIndex(statement , PRIVILEGE_BY_NAME_IDX_NAME , PRIVILEGE_BY_NAME_IDX_METADATA);
    }
  }

  /**
   * Creates an index with the given parameters
   * @param statement a {@link Statement} object to execute the statement
   * @param indexName the name of the index
   * @param indexMetadata the metadata of the index
   * @throws SQLException if not able to create the expected index
   */
  private void createIndex(Statement statement , String indexName , String indexMetadata) throws SQLException {
    statement.execute(String.format(CREATE_INDEX_STATEMENT , indexName , indexMetadata));
  }
}
