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
import java.sql.PreparedStatement;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.springframework.stereotype.Component;

/**
 * Migration step which removes the defunct Nexus 2-to-3 migration capability.
 *
 * The migration capability was owned by the now-removed nexus-legacy-2-to-3-migration-plugin module.
 * Any instance that ever configured the wizard will have orphaned rows in capability_storage_item
 * that cause DefaultCapabilityRegistry to log "Cannot sync capability {} - unknown type {}" on
 * every sync.
 */
@Component
public class RemoveNexus2MigrationCapabilityMigrationStep_2_164
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Optional<String> version() {
    return Optional.of("2.164");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, "capability_storage_item")) {
      try (PreparedStatement statement =
          connection.prepareStatement("DELETE FROM capability_storage_item WHERE type = ?")) {
        statement.setString(1, "migration");
        int deleted = statement.executeUpdate();
        log.info("Removed {} orphaned Nexus 2-to-3 migration capability rows", deleted);
      }
    }
  }
}
