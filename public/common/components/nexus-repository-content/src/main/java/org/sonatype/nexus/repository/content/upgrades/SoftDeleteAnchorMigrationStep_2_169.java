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
 * No-op anchor migration whose only purpose is to bump the recorded schema version to
 * {@value #VERSION}. Stores consult {@code DatabaseCheck.isAtLeast(VERSION)} to gate every
 * read query that references the soft-delete columns ({@code deleted_on}, {@code deleted_by})
 * on the dynamic-format tables ({@code {format}_component}, {@code {format}_asset}).
 *
 * <p>
 * <strong>Why an anchor migration?</strong> The column work for the dynamic-format tables is
 * performed by the repeatable soft-delete steps ({@code ComponentSoftDeleteRepeatableMigrationStep},
 * {@code AssetSoftDeleteRepeatableMigrationStep}). This anchor sits after them so the read-query
 * gating in {@code ComponentStore} / {@code AssetStore} / {@code BrowseNodeStore} has a single
 * concrete version to test against via {@code DatabaseCheck.isAtLeast}.
 *
 * <p>
 * <strong>Why this is ZDU-safe:</strong>
 * <ul>
 * <li>Flyway runs all versioned migrations (including this one) in a single {@code migrate()}
 * invocation, with the column-adding steps ordered before this anchor.</li>
 * <li>{@link org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent} fires exactly once,
 * <em>after</em> {@code migrate()} returns. Only then does
 * {@code DatabaseCheckImpl}'s cache invalidate.</li>
 * <li>Until the event fires, the cached {@code isAtLeast("2.169")} entry is still its
 * pre-migration value (false). Queries continue to omit the {@code deleted_on} predicate
 * during this window.</li>
 * <li>Once the event fires, cache invalidates atomically; the next call sees both
 * {@code isAtLeast("2.169") == true} <em>and</em> the columns present. There is no race
 * window where {@code isAtLeast} flips to true while columns are still missing.</li>
 * </ul>
 *
 * <p>
 * <strong>Why no DDL here:</strong> This deliberately avoids
 * {@code ALTER TABLE ADD COLUMN IF NOT EXISTS} at boot. That class of statement caused NEXUS-49154
 * — an exclusive table lock taken before the {@code IF NOT EXISTS} check led to sev-1 outages on
 * heavily-written tables. The column work belongs to the dedicated versioned migrations.
 *
 * <p>
 * Part of the Logical Deletion Recovery initiative (NEXUS-49147 / NEXUS-51733).
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SoftDeleteAnchorMigrationStep_2_169
    implements DatabaseMigrationStep
{
  /**
   * Version anchor for the dynamic-format soft-delete column work. Consumers reference this via
   * {@code DatabaseCheck.isAtLeast(SoftDeleteAnchorMigrationStep_2_169.VERSION)}.
   */
  public static final String VERSION = "2.169";

  @Override
  public Optional<String> version() {
    return Optional.of(VERSION);
  }

  @Override
  public void migrate(final Connection connection) {
    // Intentionally empty. See class Javadoc.
  }
}
