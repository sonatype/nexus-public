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
package org.sonatype.nexus.repository.content.store;

import org.sonatype.nexus.common.app.trash.TrashSchemaVersions;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;

import com.google.common.eventbus.Subscribe;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.app.FeatureFlags.TRASH_ENABLED_NAMED_VALUE;

/**
 * Shared, process-wide state for whether trash read filtering is active.
 */
@Component
public class TrashSchemaState
    implements EventAware
{
  private final boolean featureEnabled;

  private final DatabaseCheck databaseCheck;

  // Don't access directly, use schemaReached()
  private volatile Boolean schemaReached;

  @Autowired
  public TrashSchemaState(
      @Value(TRASH_ENABLED_NAMED_VALUE) final boolean featureEnabled,
      final DatabaseCheck databaseCheck)
  {
    this.featureEnabled = featureEnabled;
    this.databaseCheck = requireNonNull(databaseCheck);
  }

  /**
   * @return {@code true} when the feature flag is on and the trash columns exist.
   *         <p>
   *         Gates behavior that should follow the runtime feature flag: delete-routing (whether a delete goes to Trash
   *         or is permanent) and Trash UI/REST exposure. Do NOT use this for read/insert filtering -- see
   *         {@link #isSchemaReached()}.
   */
  public boolean isTrashAvailable() {
    return featureEnabled && schemaReached();
  }

  /**
   * @return {@code true} when the trash columns exist (schema migrated), independent of the feature flag.
   *         <p>
   *         Read/insert filtering (the {@code deleted_on IS NULL} predicate) keys on this, NOT on
   *         {@link #isTrashAvailable()}: once content has been Trashed, turning the feature flag off must never
   *         resurrect it, so the read filter has to persist as long as the columns exist (NEXUS-55233). On an instance
   *         that migrated the columns but never enabled Trash, nothing was ever soft-deleted, so every
   *         {@code deleted_on} is NULL and the filter is a harmless no-op.
   */
  public boolean isSchemaReached() {
    return schemaReached();
  }

  private boolean schemaReached() {
    // Lazy, lock-free init: the field is volatile for visibility, not atomicity, so two threads racing on the
    // first call may both run the databaseCheck. That is harmless -- isAtLeast is a cheap read-only probe and both
    // threads store the same value -- so we accept the rare double-invocation rather than pay for locking.
    Boolean reached = schemaReached;
    if (reached == null) {
      reached = databaseCheck.isAtLeast(TrashSchemaVersions.CONTENT);
      schemaReached = reached;
    }
    return reached;
  }

  @Subscribe
  public void on(final UpgradeCompletedEvent event) {
    // Refresh regardless of the feature flag: read filtering keys on schema presence (isSchemaReached), so a
    // node must start hiding Trashed content as soon as the columns exist even with the flag off -- e.g. a ZDU
    // node that received this event after starting pre-migration (NEXUS-55233).
    // Schema presence is monotonic: once the columns exist they never go away. Enforce that here rather than
    // trusting event ordering -- a lagging/partial ZDU event carrying a version below the anchor must never flip a
    // node that already reached the anchor back to "not reached" and momentarily resurface Trashed content.
    if (Boolean.TRUE.equals(schemaReached)) {
      return;
    }
    event.getSchemaVersion()
        .map(MigrationVersion::fromVersion)
        .filter(version -> version.isAtLeast(TrashSchemaVersions.CONTENT))
        .ifPresent(version -> schemaReached = true);
  }
}
