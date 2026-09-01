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
package org.sonatype.nexus.upgrade.datastore.internal.steps;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Creates the {@code hf_xet_presigned_url_store} table for HuggingFace XET protocol support.
 *
 * <p>
 * This table stores short-lived presigned URLs used in the XET protocol for cross-node
 * deployments. Required for cloud deployments where multiple Nexus instances serve
 * requests behind a load balancer (NEXUS-48685).
 *
 * <p>
 * Table schema:
 * <ul>
 * <li>{@code token} - VARCHAR(64) PRIMARY KEY - UUID string</li>
 * <li>{@code presigned_url} - TEXT NOT NULL - AWS presigned URL</li>
 * <li>{@code url_range} - VARCHAR(64) - bytes=start-end range (sufficient: longest realistic
 * value is {@code bytes=0-999999999999999} = 22 chars; 64 chars handles theoretical
 * multi-petabyte xorbs with ample margin)</li>
 * <li>{@code expires_at_millis} - BIGINT NOT NULL - expiry timestamp</li>
 * <li>{@code created_at_millis} - BIGINT NOT NULL - creation timestamp</li>
 * </ul>
 */
@Component
public class HuggingFaceXetPresignedUrlStoreMigrationStep_2_163
    implements DatabaseMigrationStep
{
  private static final String TABLE_NAME = "hf_xet_presigned_url_store";

  @Override
  public Optional<String> version() {
    return Optional.of("2.163");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    createTable(connection);
    createIndex(connection);
  }

  private void createTable(final Connection connection) throws SQLException {
    if (tableExists(connection, TABLE_NAME)) {
      return;
    }

    if (isPostgresql(connection)) {
      // NEXUS-49154 / CLAUDE.md: wrap CREATE TABLE in a DO block with duplicate_table
      // catch so concurrent migrations on multi-node startups don't deadlock. CREATE TABLE
      // only raises duplicate_table on races; duplicate_object is for constraint/index/type
      // name collisions and cannot come out of this statement, so it is not caught here.
      runStatement(connection, String.format("""
          DO $$
          BEGIN
            BEGIN
              CREATE TABLE IF NOT EXISTS %s (
                token              VARCHAR(64) NOT NULL,
                presigned_url      TEXT NOT NULL,
                url_range          VARCHAR(64),
                expires_at_millis  BIGINT NOT NULL,
                created_at_millis  BIGINT NOT NULL,
                CONSTRAINT pk_%s PRIMARY KEY (token)
              );
            EXCEPTION
              WHEN duplicate_table THEN NULL;
            END;
          END $$;
          """, TABLE_NAME, TABLE_NAME));
    }
    else {
      // H2 syntax
      runStatement(connection, String.format("""
          CREATE TABLE IF NOT EXISTS %s (
            token              VARCHAR(64) NOT NULL,
            presigned_url      CLOB NOT NULL,
            url_range          VARCHAR(64),
            expires_at_millis  BIGINT NOT NULL,
            created_at_millis  BIGINT NOT NULL,
            CONSTRAINT pk_%s PRIMARY KEY (token)
          )
          """, TABLE_NAME, TABLE_NAME));
    }
  }

  private void createIndex(final Connection connection) throws SQLException {
    String indexName = "idx_" + TABLE_NAME + "_expires";

    if (indexExists(connection, TABLE_NAME, indexName)) {
      return;
    }

    if (isPostgresql(connection)) {
      // Use DO block for PostgreSQL to avoid concurrent creation issues. CREATE INDEX
      // races surface as duplicate_object (index/constraint name collision), never as
      // duplicate_table — so only duplicate_object is caught here.
      runStatement(connection, String.format("""
          DO $$
          BEGIN
            BEGIN
              CREATE INDEX IF NOT EXISTS %s ON %s (expires_at_millis);
            EXCEPTION
              WHEN duplicate_object THEN NULL;
            END;
          END $$;
          """, indexName, TABLE_NAME));
    }
    else {
      // H2 syntax
      runStatement(connection, String.format(
          "CREATE INDEX IF NOT EXISTS %s ON %s (expires_at_millis)",
          indexName, TABLE_NAME));
    }
  }
}
