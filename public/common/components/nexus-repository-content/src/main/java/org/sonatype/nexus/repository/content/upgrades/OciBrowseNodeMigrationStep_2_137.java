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
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * OCI-specific migration step: Creates necessary indexes for OCI browse_node table.
 * Drops old index and creates new composite index for efficient browse operations.
 *
 * This migration step is specific to OCI format repository support (NEXUS-42721).
 * Adds necessary indexes for efficient OCI browse node queries.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OciBrowseNodeMigrationStep_2_137
    implements DatabaseMigrationStep
{
  private static final String OCI_FORMAT = "oci";

  private static final String OCI_BROWSE_NODE_TABLE = OCI_FORMAT + "_browse_node";

  private static final String DROP_INDEX = "DROP INDEX IF EXISTS idx_%s_browse_node_asset_id_component_id";

  private static final String CREATE_ASSET_INDEX =
      "CREATE INDEX IF NOT EXISTS idx_%s_browse_node_asset_id_component_id ON %s_browse_node (asset_id, component_id)";

  @Override
  public Optional<String> version() {
    return Optional.of("2.137");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, OCI_BROWSE_NODE_TABLE)) {
      return;
    }

    try (Statement st = connection.createStatement()) {
      st.execute(String.format(DROP_INDEX, OCI_FORMAT));
      st.execute(String.format(CREATE_ASSET_INDEX, OCI_FORMAT, OCI_FORMAT));
    }
  }
}
