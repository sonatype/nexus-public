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
package org.sonatype.nexus.orient.restore;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.orient.DatabaseRestorer;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.RESTORE;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.DATABASE_NAMES;

/**
 * Service that manages the RESTORE {@link org.sonatype.nexus.common.app.ManagedLifecycle.Phase}.
 *
 * Intended to require that restore for the schemas that are interdependent happens as a single unit.
 */
@Named
@ManagedLifecycle(phase = RESTORE)
@Singleton
public class RestoreServiceImpl
    extends StateGuardLifecycleSupport
    implements RestoreService
{
  private final DatabaseRestorer databaseRestorer;

  private final DatabaseManager databaseManager;

  @Inject
  public RestoreServiceImpl(final DatabaseRestorer databaseRestorer, final DatabaseManager databaseManager) {
    this.databaseRestorer = databaseRestorer;
    this.databaseManager = databaseManager;
  }

  @Override
  protected void doStart() throws Exception {
    Set<String> pending = new HashSet<>();
    for (String name: DATABASE_NAMES) {
      if (databaseRestorer.hasPendingRestore(name)) {
        log.debug("found pending restore for {}", name);
        pending.add(name);
      }
    }

    if (!pending.isEmpty() && !pending.equals(DATABASE_NAMES)) {
      throw new IllegalStateException("Found pending database restore files for " + pending +
          ", but some are missing; to restore you must have files for " + DATABASE_NAMES);
    }

    log.debug("passed joint restore check");
    // let this class create/and register instances for DATABASE_NAMES; will trigger create and restore (if necessary)
    DATABASE_NAMES.forEach(databaseManager::instance);
  }
}
