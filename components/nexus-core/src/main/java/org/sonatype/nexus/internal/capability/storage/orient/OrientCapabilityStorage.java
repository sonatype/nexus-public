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
package org.sonatype.nexus.internal.capability.storage.orient;

import java.util.Map;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorage;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Orient {@link CapabilityStorage} implementation.
 *
 * @since 3.0
 */
@Named("orient")
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientCapabilityStorage
    extends StateGuardLifecycleSupport
    implements CapabilityStorage
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final OrientCapabilityStorageItemEntityAdapter entityAdapter;

  @Inject
  public OrientCapabilityStorage(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                                 final OrientCapabilityStorageItemEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  private CapabilityIdentity identity(final CapabilityStorageItem item) {
    checkEntityType(item);
    return OrientCapabilityStorageItem.identity(EntityHelper.id((OrientCapabilityStorageItem) item));
  }

  @Override
  @Guarded(by = STARTED)
  public CapabilityIdentity add(final CapabilityStorageItem item) {
    checkEntityType(item);
    inTxRetry(databaseInstance).run(db -> entityAdapter.addEntity(db, (OrientCapabilityStorageItem) item));
    return identity(item);
  }

  @Override
  @Guarded(by = STARTED)
  public boolean update(final CapabilityIdentity id, final CapabilityStorageItem item) {
    checkEntityType(item);
    return inTxRetry(databaseInstance).call(db -> entityAdapter.edit(db, id.toString(), (OrientCapabilityStorageItem) item));
  }

  @Override
  @Guarded(by = STARTED)
  public boolean remove(final CapabilityIdentity id) {
    return inTxRetry(databaseInstance).call(db -> entityAdapter.delete(db, id.toString()));
  }

  @Override
  @Guarded(by = STARTED)
  public Map<CapabilityIdentity, CapabilityStorageItem> getAll() {
    return inTx(databaseInstance)
        .call(db -> Maps.uniqueIndex(transform(entityAdapter.browse(db), e -> e), this::identity));
  }

  @Override
  public CapabilityStorageItem newStorageItem(final int version, final String type, final boolean enabled,
                                              final String notes, final Map<String, String> properties)
  {
    return new OrientCapabilityStorageItem(version, type, enabled, notes, properties);
  }

  private static void checkEntityType(final CapabilityStorageItem item) {
    checkArgument(item instanceof OrientCapabilityStorageItem,
        "CapabilityStorageItem does not match the backing implementation");
  }
}
