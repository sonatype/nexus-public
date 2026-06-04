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
package org.sonatype.nexus.repository.content.search.capability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;

import org.sonatype.nexus.repository.content.search.upgrade.SearchIndexUpgrade;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.springframework.stereotype.Component;

/**
 * Migration step which removes the defunct SearchConfigurationCapability
 */
@Component
public class SearchCapabilityStep_2_16
    extends SearchIndexUpgrade
    implements DatabaseMigrationStep
{
  @Override
  public Optional<String> version() {
    return Optional.of("2.16");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (this.tableExists(connection, "capability_storage_item")) {
      try (PreparedStatement statement =
          connection.prepareStatement("DELETE FROM capability_storage_item WHERE type = ?")) {
        statement.setString(1, "nexus.search.configuration");
        statement.execute();
      }
    }

    if (this.tableExists(connection, "search_components")) {
      // trigger re-index
      super.migrate(connection);
    }
  }
}
