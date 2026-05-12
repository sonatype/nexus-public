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

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Change node_id and parent_id to BIGINT to mitigate sequence exhaustion
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BrowseNodeMigrationStep_1_33
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final List<Format> formats;

  @Inject
  public BrowseNodeMigrationStep_1_33(final List<Format> formats) {
    this.formats = formats;
  }

  private static final String ALTER_NODE_ID = "ALTER TABLE %s_browse_node ALTER COLUMN node_id SET DATA TYPE BIGINT;";

  private static final String ALTER_PARENT_ID =
      "ALTER TABLE %s_browse_node ALTER COLUMN parent_id SET DATA TYPE BIGINT;";

  @Override
  public Optional<String> version() {
    return Optional.of("1.33");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    log.info("Running migration step over browse_node tables. " +
        "This operation may take a significant amount of time for large browse_node tables.");
    formats.forEach(format -> migrateFormat(connection, format));
    log.info("Completed migration step over browse_node tables for all formats.");
  }

  private void migrateFormat(final Connection connection, final Format format) {
    try {
      String formatName = format.getValue();
      log.info("Running migration step over {}_browse_node table, altering node_id to BIGINT.", formatName);
      try (PreparedStatement select = connection.prepareStatement(String.format(ALTER_NODE_ID, formatName))) {
        select.executeUpdate();
      }
      log.info("Running migration step over {}_browse_node table, altering parent_id to BIGINT.", formatName);
      try (PreparedStatement select = connection.prepareStatement(String.format(ALTER_PARENT_ID, formatName))) {
        select.executeUpdate();
      }
    }
    catch (SQLException e) {
      throw new IllegalStateException("Failed to apply browse_node id/parent datatype changes", e);
    }
  }
}
