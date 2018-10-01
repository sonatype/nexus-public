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
package org.sonatype.nexus.cleanup.internal.storage.orient;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Orient specific storage for Cleanup Policies
 *
 * @since 3.14
 */
@Named("orient")
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientCleanupPolicyStorage
    extends StateGuardLifecycleSupport
    implements CleanupPolicyStorage
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final OrientCleanupPolicyEntityAdapter entityAdapter;

  @Inject
  public OrientCleanupPolicyStorage(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
      final OrientCleanupPolicyEntityAdapter entityAdapter)
  {
    this.databaseInstance = databaseInstance;
    this.entityAdapter = entityAdapter;
  }

  @Override
  protected void doStart() {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public CleanupPolicy add(final CleanupPolicy item) {
    return inTxRetry(databaseInstance).call(db -> entityAdapter.readEntity(entityAdapter.addEntity(db, item)));
  }

  @Override
  @Guarded(by = STARTED)
  public CleanupPolicy update(final CleanupPolicy cleanupPolicy) {
    return inTxRetry(databaseInstance)
        .call(db -> entityAdapter.readEntity(entityAdapter.editEntity(db, cleanupPolicy)));
  }

  @Override
  @Guarded(by = STARTED)
  public void remove(final CleanupPolicy cleanupPolicy) {
    checkNotNull(cleanupPolicy);

    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteEntity(db, cleanupPolicy));
  }

  @Override
  @Guarded(by = STARTED)
  public CleanupPolicy get(final String cleanupPolicyName) {
    return inTx(databaseInstance).call(db -> entityAdapter.get(db, cleanupPolicyName));
  }

  @Override
  @Guarded(by = STARTED)
  public List<CleanupPolicy> getAll() {
    return inTx(databaseInstance).call(db -> ImmutableList.copyOf(entityAdapter.browse(db)));
  }

  @Override
  @Guarded(by = STARTED)
  public boolean exists(final String cleanupPolicyName) {
    return inTx(databaseInstance).call(db -> entityAdapter.exists(db, cleanupPolicyName));
  }

  @Override
  public List<CleanupPolicy> getAllByFormat(final String format) {
    return inTx(databaseInstance).call(db -> ImmutableList.copyOf(entityAdapter.browseByFormat(db, format)));
  }
}
