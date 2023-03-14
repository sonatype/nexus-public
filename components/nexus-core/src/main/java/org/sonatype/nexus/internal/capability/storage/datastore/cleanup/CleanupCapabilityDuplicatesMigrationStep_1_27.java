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
package org.sonatype.nexus.internal.capability.storage.datastore.cleanup;

import java.sql.Connection;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Cleanup duplicate capabilities migration step. - do cleanup capabilities duplicates (if they exist) - create unique
 * index on capability_storage_item table
 */
@Named
@Singleton
public class CleanupCapabilityDuplicatesMigrationStep_1_27
    implements DatabaseMigrationStep
{
  private static final String ADD_INDEX =
      "CREATE UNIQUE INDEX IF NOT EXISTS uk_capability_storage_item_type_props ON capability_storage_item(type, properties)";

  private static final String ADD_CONSTRAINT =
      "ALTER TABLE capability_storage_item ADD CONSTRAINT IF NOT EXISTS uk_capability_storage_item_type_props UNIQUE (type, properties)";

  private final CleanupCapabilityDuplicatesService cleanupService;

  @Inject
  public CleanupCapabilityDuplicatesMigrationStep_1_27(final CleanupCapabilityDuplicatesService cleanupService) {
    this.cleanupService = checkNotNull(cleanupService);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.27");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    cleanupService.doCleanup();

    if (isPostgresql(connection)) {
      runStatement(connection, ADD_INDEX);
    }
    else {
      runStatement(connection, ADD_CONSTRAINT);
    }
  }
}
