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
package org.sonatype.nexus.internal.datastore;

import java.sql.Connection;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.common.upgrade.events.UpgradeEventSupport;
import org.sonatype.nexus.datastore.api.DataStoreManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Named
@Singleton
@ManagedLifecycle(phase = STORAGE)
public class DatabaseCheckImpl
    extends StateGuardLifecycleSupport
    implements DatabaseCheck, EventAware
{
  private final DataStoreManager dataStoreManager;

  private final boolean datastoreClustered;

  private DataSource dataSource;

  private MigrationVersion currentSchemaVersion;

  private boolean postgresql = false;

  @Inject
  public DatabaseCheckImpl(
      final DataStoreManager dataStoreManager,
      @Named(FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED) final boolean datastoreClustered)
  {
    this.dataStoreManager = checkNotNull(dataStoreManager);
    this.datastoreClustered = datastoreClustered;
  }

  @Override
  protected void doStart() throws Exception {
    dataSource = dataStoreManager.get(DEFAULT_DATASTORE_NAME)
        .orElseThrow(() -> new IllegalStateException("Missing DataStore named: " + DEFAULT_DATASTORE_NAME))
        .getDataSource();

    try (Connection con = dataSource.getConnection()) {
      postgresql = POSTGRE_SQL.equalsIgnoreCase(con.getMetaData().getDatabaseProductName());
    }
  }

  @Guarded(by = STARTED)
  @Override
  public boolean isPostgresql() {
    return postgresql;
  }

  @Override
  public boolean isAllowedByVersion(final Class<?> annotatedClass) {
    if (!datastoreClustered) {
      return true;
    }

    AvailabilityVersion availabilityVersion = annotatedClass.getAnnotation(AvailabilityVersion.class);
    if (availabilityVersion != null && isAllowed(availabilityVersion.from())) {
      return true;
    }
    if (availabilityVersion == null) {
      log.error("Missing database version specified for {}", annotatedClass);
    }

    log.debug("The database schema version is lower than the minimum required to enable {}", annotatedClass);
    return false;
  }

  @Override
  public boolean isAtLeast(final String version) {
    return isAllowed(version);
  }

  @Subscribe
  public void on(final UpgradeEventSupport event) {
    Optional<MigrationVersion> schemaVersion = event.getSchemaVersion()
        .map(MigrationVersion::fromVersion);
    if (schemaVersion.isPresent()) {
      currentSchemaVersion = schemaVersion.get();
    }
  }

  private boolean isAllowed(final String requiredVersion) {
    if (currentSchemaVersion == null) {
      currentSchemaVersion = getMigrationVersion(dataSource);
      if (currentSchemaVersion == null) {
        return true;
      }
    }

    return currentSchemaVersion.isAtLeast(requiredVersion);
  }

  @VisibleForTesting
  MigrationVersion getMigrationVersion(final DataSource dataSource) {
    if (dataSource == null) {
      log.warn("datasource has not been initialised");
      return null;
    }

    Flyway flyway = Flyway.configure()
        .dataSource(dataSource).load();

    MigrationInfo current = flyway.info().current();
    if (current != null) {
      return current.getVersion();
    }

    log.error("Could not determine database schema version");
    return null;
  }
}
