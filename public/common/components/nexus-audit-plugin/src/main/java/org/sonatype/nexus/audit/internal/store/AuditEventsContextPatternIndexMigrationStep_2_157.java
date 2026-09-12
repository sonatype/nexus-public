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
package org.sonatype.nexus.audit.internal.store;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Replaces the plain-btree {@code idx_audit_events_context} with a text_pattern_ops btree so
 * {@code context LIKE 'X:%'} in the audit-log query is index-usable regardless of the database
 * collation. On PostgreSQL under non-C collations (e.g. {@code en_US.utf8}) a plain btree cannot
 * serve {@code LIKE 'prefix%'}; text_pattern_ops uses byte-wise ordering, so it can. H2's default
 * text ordering is already codepoint-based, so the H2 branch is a no-op.
 */
@Component
public class AuditEventsContextPatternIndexMigrationStep_2_157
    implements DatabaseMigrationStep
{
  private static final String TABLE_NAME = "audit_events";

  private static final String OLD_INDEX_NAME = "idx_audit_events_context";

  private static final String NEW_INDEX_NAME = "idx_audit_events_context_pattern";

  @Override
  public Optional<String> version() {
    return Optional.of("2.157");
  }

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, TABLE_NAME)) {
      return;
    }
    if (isPostgresql(connection)) {
      runStatement(connection, String.format(
          "CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON %s (context text_pattern_ops)",
          NEW_INDEX_NAME, TABLE_NAME));
      runStatement(connection, String.format(
          "DROP INDEX CONCURRENTLY IF EXISTS %s", OLD_INDEX_NAME));
    }
  }
}
