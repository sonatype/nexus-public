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
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Disabled placeholder (NEXUS-54969).
 *
 * <p>
 * This step originally added the soft-delete columns ({@code deleted_on}, {@code deleted_by}) and a partial
 * index to the {@code repository} table (Logical Deletion Recovery, NEXUS-49147/NEXUS-51733). It is now a
 * no-op so the columns are not added: soft-delete is disabled at the store layer, and running the live
 * {@code ALTER TABLE} was part of the class of change that caused the "cached plan must not change result
 * type" (SQLState 0A000) failures during rollout.
 * </p>
 *
 * <p>
 * Safe for both database states. On a database where this step already ran, the columns remain (unused,
 * all-NULL). On a database where it never ran, the columns stay absent and
 * {@code ConfigurationStoreImpl} transparently falls back to its unfiltered repository reads (it catches
 * the "column deleted_on does not exist" error, see {@code SoftDeleteMigrationHelper}). The class stays
 * registered and keeps {@code version() = 2.168} so Flyway still matches any recorded history row;
 * re-enabling is a revert of this change.
 * </p>
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConfigurationSoftDeleteMigrationStep_2_168
    implements DatabaseMigrationStep
{
  @Override
  public Optional<String> version() {
    return Optional.of("2.168");
  }

  @Override
  public void migrate(final Connection connection) {
    // NEXUS-54969: disabled. Do not add deleted_on/deleted_by columns or the index to the repository table.
    // Idempotent no-op: already-migrated databases keep their (unused, NULL) columns; fresh databases never
    // get them and ConfigurationStoreImpl falls back to unfiltered repository reads.
  }
}
