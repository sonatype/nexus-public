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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;
import org.sonatype.nexus.ssl.spi.KeyStoreStorageManager;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Implementation of {@link KeyStoreStorageManager} for the SSL trust store. Uses OrientDB as backing storage to
 * facilitate distribution of data across cluster.
 * 
 * @since 3.1
 */
@Named(KeyStoreManagerImpl.NAME)
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
public class KeyStoreStorageManagerImpl
    extends StateGuardLifecycleSupport
    implements KeyStoreStorageManager
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final KeyStoreDataEntityAdapter entityAdapter;

  private final EventManager eventManager;

  private final Collection<OrientKeyStoreStorage> storages = new ConcurrentLinkedQueue<>();

  @Inject
  public KeyStoreStorageManagerImpl(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                                    final KeyStoreDataEntityAdapter entityAdapter,
                                    final EventManager eventManager)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  protected void doStop() throws Exception {
    storages.forEach(eventManager::unregister);
    storages.clear();
  }

  @Override
  @Guarded(by = STARTED)
  public KeyStoreStorage createStorage(final String keyStoreName) {
    checkNotNull(keyStoreName);
    OrientKeyStoreStorage storage = new OrientKeyStoreStorage(this, KeyStoreManagerImpl.NAME + '/' + keyStoreName);
    eventManager.register(storage);
    storages.add(storage);
    return storage;
  }

  @Guarded(by = STARTED)
  @Nullable
  public KeyStoreData load(final String keyStoreName) {
    checkNotNull(keyStoreName);
    return inTx(databaseInstance).call(db -> entityAdapter.load(db, keyStoreName));
  }

  @Guarded(by = STARTED)
  public void save(final KeyStoreData entity) {
    checkNotNull(entity);
    inTxRetry(databaseInstance).run(db -> entityAdapter.save(db, entity));
  }
}
