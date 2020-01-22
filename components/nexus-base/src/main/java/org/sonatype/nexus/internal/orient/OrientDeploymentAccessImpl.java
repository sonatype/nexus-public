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

package org.sonatype.nexus.internal.orient;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.DeploymentAccess;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.CONFIG;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Class used to identify a deployment uniquely and permanently.
 *
 * {@link DeploymentIdentifier#getId()} is generated at first run and not modifiable.
 *
 * @since 3.6.1
 */
@FeatureFlag(name = "nexus.orient.store.config")
@Named("orient")
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientDeploymentAccessImpl
    extends StateGuardLifecycleSupport
    implements DeploymentAccess
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final OrientDeploymentIdentifierEntityAdapter entityAdapter;

  private final NodeAccess nodeAccess;

  // cached local copy; initialized by #doStart
  private String id;

  @Inject
  public OrientDeploymentAccessImpl(@Named(CONFIG) final Provider<DatabaseInstance> databaseInstance,
                                    final NodeAccess nodeAccess)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = new OrientDeploymentIdentifierEntityAdapter();
    this.nodeAccess = nodeAccess;
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx tx = databaseInstance.get().connect()) {
      entityAdapter.register(tx);
    }

    this.id = inTxRetry(databaseInstance).call(db -> {
      DeploymentIdentifier identifier = entityAdapter.get(db);
      if (identifier == null) {
        identifier = new DeploymentIdentifier();
        identifier.setId(nodeAccess.getId());
        entityAdapter.set(db, identifier);
        log.info("Created new deployment identifier: {}", identifier);
      }
      return identifier.getId();
    });
  }

  @Override
  @Guarded(by = STARTED)
  public String getId() {
    return id;
  }

  @Override
  @Guarded(by = STARTED)
  public String getAlias() {
    return inTx(databaseInstance).call(db -> {
      DeploymentIdentifier identifier = entityAdapter.get(db);
      return identifier.getAlias();
    });
  }

  @Override
  @Guarded(by = STARTED)
  public void setAlias(final String newAlias) {
    inTxRetry(databaseInstance).run(db -> {
      DeploymentIdentifier identifier = entityAdapter.get(db);
      identifier.setAlias(newAlias);
      entityAdapter.set(db, identifier);
    });
  }
}
