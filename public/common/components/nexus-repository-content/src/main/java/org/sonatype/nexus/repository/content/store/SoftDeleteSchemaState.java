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

import org.sonatype.nexus.common.app.softdelete.SoftDeleteSchemaVersions;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;

import com.google.common.eventbus.Subscribe;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.app.FeatureFlags.SOFT_DELETE_ENABLED_NAMED_VALUE;

/**
 * Shared, process-wide state for whether soft-delete read filtering is active.
 */
@Component
public class SoftDeleteSchemaState
    implements EventAware
{
  private final boolean featureEnabled;

  private final DatabaseCheck databaseCheck;

  // Don't access directly, use schemaReached()
  private volatile Boolean schemaReached;

  @Autowired
  public SoftDeleteSchemaState(
      @Value(SOFT_DELETE_ENABLED_NAMED_VALUE) final boolean featureEnabled,
      final DatabaseCheck databaseCheck)
  {
    this.featureEnabled = featureEnabled;
    this.databaseCheck = requireNonNull(databaseCheck);
  }

  /**
   * @return {@code true} when the feature flag is on and the soft-delete columns exist.
   */
  public boolean isSoftDeleteAvailable() {
    return featureEnabled && schemaReached();
  }

  private boolean schemaReached() {
    Boolean reached = schemaReached;
    if (reached == null) {
      reached = databaseCheck.isAtLeast(SoftDeleteSchemaVersions.CONTENT);
      schemaReached = reached;
    }
    return reached;
  }

  @Subscribe
  public void on(final UpgradeCompletedEvent event) {
    if (!featureEnabled) {
      return;
    }
    // The target schema version is monotonic, so once it reaches the anchor it stays reached.
    event.getSchemaVersion()
        .map(MigrationVersion::fromVersion)
        .ifPresent(version -> schemaReached = version.isAtLeast(SoftDeleteSchemaVersions.CONTENT));
  }
}
