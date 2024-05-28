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
package org.sonatype.nexus.repository.db.datastore;

import java.sql.Connection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.db.DatabaseCheck;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Named
@Singleton
@FeatureFlag(name = DATASTORE_ENABLED)
@ManagedLifecycle(phase = STORAGE)
public class DatabaseCheckImpl
    extends StateGuardLifecycleSupport
    implements DatabaseCheck
{
  private final DataSessionSupplier sessionSupplier;

  private boolean postgresql = false;

  @Inject
  public DatabaseCheckImpl(final DataSessionSupplier sessionSupplier)
  {
    this.sessionSupplier = requireNonNull(sessionSupplier);
  }

  @Override
  protected void doStart() throws Exception {
    try (Connection con = sessionSupplier.openConnection(DEFAULT_DATASTORE_NAME)) {
      postgresql = POSTGRE_SQL.equalsIgnoreCase(con.getMetaData().getDatabaseProductName());
    }
  }

  @Guarded(by = STARTED)
  @Override
  public boolean isPostgresql() {
    return postgresql;
  }
}
