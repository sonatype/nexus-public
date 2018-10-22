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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
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
 * Default {@link BlobStoreConfigurationStore} implementation.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class BlobStoreConfigurationStoreImpl
    extends StateGuardLifecycleSupport
    implements BlobStoreConfigurationStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final BlobStoreConfigurationEntityAdapter entityAdapter;

  @Inject
  public BlobStoreConfigurationStoreImpl(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                                         final BlobStoreConfigurationEntityAdapter entityAdapter)
  {
    this.databaseInstance = databaseInstance;
    this.entityAdapter = entityAdapter;
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public List<BlobStoreConfiguration> list() {
    return inTx(databaseInstance).call(db -> ImmutableList.copyOf(entityAdapter.browse(db)));
  }

  @Override
  @Guarded(by = STARTED)
  public void create(final BlobStoreConfiguration configuration) {
    checkNotNull(configuration);

    inTxRetry(databaseInstance).run(db -> entityAdapter.addEntity(db, configuration));
  }

  @Override
  @Guarded(by = STARTED)
  public void update(final BlobStoreConfiguration configuration) {
    checkNotNull(configuration);

    inTxRetry(databaseInstance).run(db -> entityAdapter.update(db, configuration));
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final BlobStoreConfiguration configuration) {
    checkNotNull(configuration);

    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteEntity(db, configuration));
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStoreConfiguration read(final String name) {
      return inTx(databaseInstance).call(db -> entityAdapter.getByName(db, name));
  }

  @Override
  @Guarded(by = STARTED)
  public Optional<BlobStoreConfiguration> findParent(final String name) {
    return inTx(databaseInstance).call(db -> entityAdapter.getParent(db, name));
  }
}
