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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Remove duplicate, unnecessary index
 * Add additional index for asset querying (similar to component)
 */
@Named
public class BrowseNodeMigrationStep_1_32
    extends ComponentSupport implements DatabaseMigrationStep
{
  private final List<Format> formats;

  @Inject
  public BrowseNodeMigrationStep_1_32(final List<Format> formats) {
    this.formats = formats;
  }

  private static final String DROP_INDEX = "DROP INDEX IF EXISTS idx_%s_browse_node_tree";

  private static final String CREATE_ASSET_INDEX = "CREATE INDEX IF NOT EXISTS " +
      "idx_%s_browse_node_asset_id ON %s_browse_node (asset_id);";

  @Override
  public Optional<String> version() {
    return Optional.of("1.32");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    formats.forEach(format -> migrateFormat(connection, format));
  }

  private void migrateFormat(final Connection connection, final Format format) {
    try {
      String formatName = format.getValue();
      try (PreparedStatement select = connection.prepareStatement(String.format(DROP_INDEX, formatName))) {
        select.executeUpdate();
      }
      try (PreparedStatement select = connection.prepareStatement(
          String.format(CREATE_ASSET_INDEX, formatName, formatName))) {
        select.executeUpdate();
      }
    }
    catch (SQLException e) {
      log.error("Failed to apply browse_node index changes", e);
      throw new RuntimeException(e);
    }
  }
}
