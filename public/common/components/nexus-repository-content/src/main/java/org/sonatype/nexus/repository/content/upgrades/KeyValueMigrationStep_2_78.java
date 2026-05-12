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
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates key-value tables in Postgres to change the value column to TEXT from VARCHAR
 */
@Component
public class KeyValueMigrationStep_2_78
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Optional<String> version() {
    return Optional.of("2.78");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (isPostgresql(connection)) {
      try (Statement statment = connection.createStatement()) {
        for (String format : List.of("apt", "helm", "yum")) {
          log.info("Updating {}_key_value type", format);
          statment.execute("""
              ALTER TABLE IF EXISTS %s_key_value
              ALTER COLUMN value TYPE text;""".formatted(format));
        }
      }
    }
  }
}
