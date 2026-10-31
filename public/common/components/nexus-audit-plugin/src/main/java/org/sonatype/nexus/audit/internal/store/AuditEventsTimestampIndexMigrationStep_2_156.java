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
 * Adds {@code idx_audit_events_timestamp} on {@code audit_events(timestamp DESC)} so the Preview UI Audit
 * page — which always issues {@code ORDER BY timestamp DESC LIMIT N} and by default filters to
 * {@code timestamp >= now() - interval '30 days'} — hits an index scan instead of a full sequential scan
 * with a top-N sort.
 */
@Component
public class AuditEventsTimestampIndexMigrationStep_2_156
    implements DatabaseMigrationStep
{
  private static final String TABLE_NAME = "audit_events";

  private static final String INDEX_NAME = "idx_audit_events_timestamp";

  @Override
  public Optional<String> version() {
    return Optional.of("2.156");
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
    if (indexExists(connection, TABLE_NAME, INDEX_NAME)) {
      return;
    }
    if (isPostgresql(connection)) {
      runStatement(connection, String.format(
          "CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON %s (timestamp DESC)", INDEX_NAME, TABLE_NAME));
    }
    else if (isH2(connection)) {
      runStatement(connection, String.format(
          "CREATE INDEX IF NOT EXISTS %s ON %s (timestamp DESC)", INDEX_NAME, TABLE_NAME));
    }
  }
}
