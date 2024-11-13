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
package org.sonatype.nexus.repository.content.internal;

import java.sql.Connection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Checks database integrity at application startup and configures automated repairs
 */
@Named
@Singleton
@ManagedLifecycle(phase = UPGRADE)
public class DatabaseIntegrityCheckService
    extends LifecycleSupport
{
  private final DataSessionSupplier dataSessionSupplier;

  private final List<DatabaseIntegrityChecker> databaseIntegrityCheckers;

  @Inject
  public DatabaseIntegrityCheckService(
      final DataSessionSupplier dataSessionSupplier,
      final List<DatabaseIntegrityChecker> databaseIntegrityCheckers)
  {
    this.dataSessionSupplier = checkNotNull(dataSessionSupplier);
    this.databaseIntegrityCheckers = checkNotNull(databaseIntegrityCheckers);
  }

  @Override
  protected void doStart() throws Exception {
    Connection connection = dataSessionSupplier.openConnection(DEFAULT_DATASTORE_NAME);
    for (DatabaseIntegrityChecker checker : databaseIntegrityCheckers) {
      checker.checkAndRepair(connection);
    }
  }
}
