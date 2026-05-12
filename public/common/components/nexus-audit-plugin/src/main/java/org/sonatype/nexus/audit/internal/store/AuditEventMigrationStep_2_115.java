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

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

/**
 * Creates audit_events table for queryable audit event storage.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class AuditEventMigrationStep_2_115
    implements DatabaseMigrationStep
{
  private static final String TABLE_NAME = "audit_events";

  @Override
  public Optional<String> version() {
    return Optional.of("2.115");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, TABLE_NAME)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, """
          CREATE INDEX IF NOT EXISTS idx_audit_events_domain_timestamp
            ON audit_events (domain, timestamp DESC)
          """);

      runStatement(connection, """
          CREATE INDEX IF NOT EXISTS idx_audit_events_type
            ON audit_events (type)
          """);

      runStatement(connection, """
          CREATE INDEX IF NOT EXISTS idx_audit_events_initiator
            ON audit_events (initiator)
          """);
    }
    else {
      runStatement(connection, """
          CREATE INDEX IF NOT EXISTS idx_audit_events_domain_timestamp
            ON audit_events (domain, timestamp DESC)
          """);

      runStatement(connection, """
          CREATE INDEX IF NOT EXISTS idx_audit_events_type
            ON audit_events (type)
          """);

      runStatement(connection, """
          CREATE INDEX IF NOT EXISTS idx_audit_events_initiator
            ON audit_events (initiator)
          """);
    }
  }
}
