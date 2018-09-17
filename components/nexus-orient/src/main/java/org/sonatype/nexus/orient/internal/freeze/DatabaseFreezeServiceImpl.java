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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.node.NodeMergedEvent;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.DatabaseFrozenStateManager;
import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType;
import org.sonatype.nexus.orient.freeze.ReadOnlyState;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.privilege.ApplicationPermission;

import org.joda.time.DateTimeZone;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration.ROLES;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType.SYSTEM;
import static org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType.USER_INITIATED;

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
    implements DatabaseFreezeService, EventAware, EventAware.Asynchronous
{

  public static final String SERVER_NAME = "*";

  static final ApplicationPermission READ_ONLY_PERMISSION = new ApplicationPermission(SERVER_NAME, Arrays.asList("read"));

  private final Set<Provider<DatabaseInstance>> providers;

  private final EventManager eventManager;

  private final DatabaseFrozenStateManager databaseFrozenStateManager;

  private final Provider<ODistributedServerManager> distributedServerManagerProvider;

  private final NodeAccess nodeAccess;

  private final SecurityHelper securityHelper;

  @Inject
  public DatabaseFreezeServiceImpl(final Set<Provider<DatabaseInstance>> providers,
                                   final EventManager eventManager,
                                   final DatabaseFrozenStateManager databaseFrozenStateManager,
                                   final Provider<OServer> server,
                                   final NodeAccess nodeAccess,
                                   final SecurityHelper securityHelper)
  {
    this.providers = checkNotNull(providers);
    this.eventManager = checkNotNull(eventManager);
    this.databaseFrozenStateManager = checkNotNull(databaseFrozenStateManager);
    checkNotNull(server);
    distributedServerManagerProvider = () -> server.get().getDistributedManager();
    this.nodeAccess = checkNotNull(nodeAccess);
    this.securityHelper = securityHelper;
  }

  @Override
  protected void doStart() {
    // catch up to other hosts in the cluster on startup
    List<FreezeRequest> state = databaseFrozenStateManager.getState();
    if (!state.isEmpty()) {
      refreezeOnStartup(state);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public boolean isFrozen() {
    if (nodeAccess.isClustered()) {
      ODistributedServerManager serverManager = distributedServerManagerProvider.get();
      Map<String, ROLES> roles = new HashMap<>();
      for (String database : serverManager.getMessageService().getDatabases()) {
        ODistributedConfiguration configuration = serverManager.getDatabaseConfiguration(database);
        roles.put(database, configuration.getServerRole(SERVER_NAME));
      }
      return roles.values().stream().allMatch(r -> ROLES.REPLICA.equals(r));
    } else {
      return providers.stream().allMatch(provider -> provider.get().isFrozen());
    }
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

  @Override
  @Guarded(by = STARTED)
  public synchronized FreezeRequest requestFreeze(final InitiatorType type, final String initiatorId) {
    // reject new user initiated requests if any existing requests are present
    if (InitiatorType.USER_INITIATED.equals(type) && !getState().isEmpty()) {
      log.warn("rejecting {} request for {} as 1 or more requests are already present in {}", type, initiatorId, getState());
      return null;
    }
    boolean frozenStatePrior = isFrozen();
    FreezeRequest request = new FreezeRequest(type, initiatorId);
    if (nodeAccess.isClustered()) {
      request.setNodeId(nodeAccess.getId());
    }
    FreezeRequest result = databaseFrozenStateManager.add(request);
    if (result != null && !frozenStatePrior) {
      eventManager.post(new DatabaseFreezeChangeEvent(true));
      freezeLocalDatabases();
    }
    return result;
  }

  @Override
  @Guarded(by = STARTED)
  public List<FreezeRequest> getState() {
    return Collections.unmodifiableList(databaseFrozenStateManager.getState());
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized boolean releaseRequest(final FreezeRequest request) {
    boolean result = databaseFrozenStateManager.remove(request);
    if (result && getState().isEmpty()) {
      releaseLocalDatabases();
      eventManager.post(new DatabaseFreezeChangeEvent(false));
    }
    if (!result) {
      log.error("failed to release {}; freeze request state {}", request, getState());
    }
    return result;
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized boolean releaseUserInitiatedIfPresent() {
    Optional<FreezeRequest> request = getState().stream()
        .filter(it -> InitiatorType.USER_INITIATED.equals(it.getInitiatorType()))
        .findAny();
    if (request.isPresent()) {
      return releaseRequest(request.get());
    }
    return false;
  }

  @Override
  public synchronized List<FreezeRequest> releaseAllRequests() {
    List<FreezeRequest> requests = getState();
    for (FreezeRequest request: requests) {
      releaseRequest(request);
    }
    return requests;
  }

  @Override
  public synchronized void freezeLocalDatabases() {
    if (nodeAccess.isClustered()) {
      setServerRole(ROLES.REPLICA);
    } else {
      forEachFreezableDatabase(databaseInstance -> databaseInstance.setFrozen(true));
    }
  }

  @Override
  public synchronized void releaseLocalDatabases() {
    if (nodeAccess.isClustered()) {
      setServerRole(ROLES.MASTER);
    } else {
      forEachFreezableDatabase(databaseInstance -> databaseInstance.setFrozen(false));
    }
  }

  @Override
  public ReadOnlyState getReadOnlyState() {
    return new DefaultReadOnlyState(getState(),
        securityHelper.allPermitted(READ_ONLY_PERMISSION));
  }

  /**
   * Check {@link DatabaseFrozenStateManager#getState()} to see if cluster state is represents a state change
   * different from our local status.
   *
   * @param event event signalling a node has been added to the cluster
   */
  @Subscribe
  public void onNodeMerged(final NodeMergedEvent event) {
    log.info("Node merged with existing cluster; shared frozen state is {}, local frozen state is={}",
        databaseFrozenStateManager.getState(),
        isFrozen());

    if (!isFrozen() && !getState().isEmpty()) {
      // freeze if we aren't frozen and the state suggests we should be
      freezeLocalDatabases();
    }
    else if(isFrozen() && getState().isEmpty()) {
      // release if we're frozen and the state suggests we shouldn't be
      releaseLocalDatabases();
    }
    else {
      log.info("no action taken for {}", event);
    }
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
    for (String database : distributedServerManager.getMessageService().getDatabases()) {
      log.info("Updating server role of {} database to {}", database, serverRole);
      distributedServerManager.executeInDistributedDatabaseLock(database, 0l, null, distributedConfiguration -> {
        distributedConfiguration.setServerRole(SERVER_NAME, serverRole);
        log.info("Updated server role of {} database to {}", database, serverRole);
        return null;
      });
    }
  }

  @VisibleForTesting
  void refreezeOnStartup(final List<FreezeRequest> state) {
    log.info("Restoring database frozen state on startup");
    Map<InitiatorType, List<FreezeRequest>> requestsByInitiator =
        state.stream().collect(groupingBy(FreezeRequest::getInitiatorType));
    for (FreezeRequest request : state) {
      log.warn("Database was frozen by {} process '{}' at {}",
          request.getInitiatorType(), request.getInitiatorId(),
          request.getTimestamp().withZone(DateTimeZone.getDefault()));
    }
    if (nodeAccess.isClustered()) {
      log.warn("Databases must be unfrozen manually");
      freezeLocalDatabases();
    }
    else {
      // in non-clustered mode, system requests are not restored on startup
      for (FreezeRequest request : requestsByInitiator.getOrDefault(SYSTEM, emptyList())) {
        log.warn("Discarding freeze request by {} process '{}'", request.getInitiatorType(), request.getInitiatorId());
        databaseFrozenStateManager.remove(request);
      }
      if (!requestsByInitiator.getOrDefault(USER_INITIATED, emptyList()).isEmpty()) {
        log.warn("Databases must be unfrozen manually");
        freezeLocalDatabases();
      }
    }
  }
}
