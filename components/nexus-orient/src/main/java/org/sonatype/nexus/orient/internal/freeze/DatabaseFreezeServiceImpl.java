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
package org.sonatype.nexus.orient.internal.freeze;

import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeMergedEvent;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.DatabaseFrozenStateManager;

import com.google.common.eventbus.Subscribe;
import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration.ROLES;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Implementation of {@link DatabaseFreezeService}
 *
 * @since 3.2
 */
@Named
@ManagedLifecycle(phase = STORAGE)
@Singleton
public class DatabaseFreezeServiceImpl
    extends StateGuardLifecycleSupport
    implements DatabaseFreezeService, EventAware
{
  private final Set<Provider<DatabaseInstance>> providers;

  private final EventManager eventManager;

  private final DatabaseFrozenStateManager databaseFrozenStateManager;

  private final Provider<ODistributedServerManager> distributedServerManagerProvider;

  private boolean frozen;

  @Inject
  public DatabaseFreezeServiceImpl(final Set<Provider<DatabaseInstance>> providers,
                                   final EventManager eventManager,
                                   final DatabaseFrozenStateManager databaseFrozenStateManager,
                                   final Provider<OServer> server)
  {
    this.providers = checkNotNull(providers);
    this.eventManager = checkNotNull(eventManager);
    this.databaseFrozenStateManager = checkNotNull(databaseFrozenStateManager);
    checkNotNull(server);
    distributedServerManagerProvider = () -> server.get().getDistributedManager();
  }

  @Override
  protected synchronized void doStart() {
    if (databaseFrozenStateManager.get()) {
      log.info("Restoring database frozen state on startup");
      updateAndSaveFrozenState(true);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void freezeAllDatabases() {
    if (frozen) {
      log.info("Databases already frozen, skipping freeze command.");
      return;
    }
    log.info("Freezing all databases.");

    // post event to notify subscribers and update cluster in ha configurations
    // this will switch databases to replica mode
    eventManager.post(new DatabaseFreezeChangeEvent(true));
    // freeze the databases locally
    updateAndSaveFrozenState(true);
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void releaseAllDatabases() {
    if (!frozen) {
      log.info("Databases already released, skipping release command.");
      return;
    }

    log.info("Releasing all databases.");

    // release the databases locally
    updateAndSaveFrozenState(false);
    // post event to notify subscribers and update cluster in ha configurations
    // this will switch databases to master mode
    eventManager.post(new DatabaseFreezeChangeEvent(false));
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized boolean isFrozen() {
    return frozen;
  }

  @Override
  @Guarded(by = STARTED)
  public void checkUnfrozen() {
    checkUnfrozen("Database is frozen, unable to proceed.");
  }

  @Override
  @Guarded(by = STARTED)
  public void checkUnfrozen(final String message) {
    if (isFrozen()) {
      throw new OModificationOperationProhibitedException(message);
    }
  }

  @Subscribe
  public void onNodeMerged(final NodeMergedEvent event) {
    log.debug("Node merged with existing cluster: shared frozen state is {}", databaseFrozenStateManager.get());
    if (databaseFrozenStateManager.get()) {
      freezeAllDatabases();
    }
    else {
      releaseAllDatabases();
    }
  }

  /**
   * Updates the frozen state and create or remove marker file.
   */
  private void updateAndSaveFrozenState(final boolean newFrozenState) {
    log.debug("Updating frozen state to {}", newFrozenState);
    frozen = newFrozenState;

    if (!frozen) {
      // restore MASTER mode now to ensure unfrozen databases are actually writable
      setServerRole(ROLES.MASTER);
    }

    forEachFreezableDatabase(databaseInstance -> databaseInstance.setFrozen(frozen));

    if (frozen) {
      // enable REPLICA mode afterwards to ensure frozen databases reject writes for the right reason
      setServerRole(ROLES.REPLICA);
    }

    databaseFrozenStateManager.set(frozen);
  }

  private void forEachFreezableDatabase(final Consumer<DatabaseInstance> databaseInstanceConsumer) {
    providers.forEach(provider -> {
      DatabaseInstance databaseInstance = provider.get();
      try {
        databaseInstanceConsumer.accept(databaseInstance);
      }
      catch (Exception e) {
        log.error("Unable to process Database instance: {}", databaseInstance, e);
      }
    });
  }

  private void setServerRole(final ROLES serverRole) {
    ODistributedServerManager distributedServerManager = distributedServerManagerProvider.get();
    if (distributedServerManager == null) {
      log.debug("Skipping update of server role, databases not clustered");
      return;
    }
    for (String database : distributedServerManager.getMessageService().getDatabases()) {
      log.debug("Updating server role of {} database to {}", database, serverRole);
      distributedServerManager.executeInDistributedDatabaseLock(database, 0l, null, distributedConfiguration -> {
        distributedConfiguration.setServerRole("*", serverRole);
        return null;
      });
    }
  }
}
