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
package org.sonatype.nexus.internal.status;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.status.StatusHealthCheckException;
import org.sonatype.nexus.common.status.StatusHealthCheckStore;
import org.sonatype.nexus.internal.status.StatusHealthCheckEntityAdapter.NodeHealthCheck;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Orient {@link StatusHealthCheckStore} implementation.
 *
 * @since 3.15
 */
@Named
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class StatusHealthCheckStoreImpl
    extends StateGuardLifecycleSupport
    implements StatusHealthCheckStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final StatusHealthCheckEntityAdapter entityAdapter;

  private final NodeAccess nodeAccess;

  @Inject
  public StatusHealthCheckStoreImpl(
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> databaseInstance,
      final StatusHealthCheckEntityAdapter entityAdapter,
      final NodeAccess nodeAccess)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  protected void doStart() {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void markHealthCheckTime() throws StatusHealthCheckException {

    String nodeId = nodeAccess.getId();

    try {
      inTxRetry(databaseInstance).run(db -> {
        NodeHealthCheck nhc = entityAdapter.read(db, nodeId);

        if (nhc == null) {
          nhc = entityAdapter.newEntity();
          nhc.nodeId = nodeId;
          nhc.lastHealthCheck = new Date();
          entityAdapter.addEntity(db, nhc);
        }
        else {
          nhc.lastHealthCheck = new Date();
          entityAdapter.editEntity(db, nhc);
        }
      });
    }
    catch (Exception e) {
      throw new StatusHealthCheckException("Unable to update health check time for node: " + nodeId, e);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void checkReadHealth() throws StatusHealthCheckException {

    String nodeId = nodeAccess.getId();

    try {
      inTxRetry(databaseInstance).run(db -> entityAdapter.read(db, nodeId));
    }
    catch (Exception e) {
      throw new StatusHealthCheckException("Unable to read health check time for node: " + nodeId, e);
    }
  }
}
