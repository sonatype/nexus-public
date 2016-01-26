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
package org.sonatype.nexus.internal.capability.storage;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Orient {@link CapabilityStorage} implementation.
 *
 * @since 3.0
 */
@Named("orient")
@Singleton
public class OrientCapabilityStorage
    extends LifecycleSupport
    implements CapabilityStorage
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final CapabilityStorageItemEntityAdapter entityAdapter;

  @Inject
  public OrientCapabilityStorage(@Named("config") final Provider<DatabaseInstance> databaseInstance,
                                 final CapabilityStorageItemEntityAdapter entityAdapter)
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

  /**
   * Open a database connection using the pool.
   */
  private ODatabaseDocumentTx openDb() {
    ensureStarted();
    return databaseInstance.get().acquire();
  }

  private CapabilityIdentity identity(final CapabilityStorageItem item) {
    EntityId id = EntityHelper.id(item);
    return new CapabilityIdentity(id.getValue());
  }

  @Override
  public CapabilityIdentity add(final CapabilityStorageItem item) throws IOException {
    try (ODatabaseDocumentTx db = openDb()) {
      entityAdapter.addEntity(db, item);
    }

    return identity(item);
  }

  @Override
  public boolean update(final CapabilityIdentity id, final CapabilityStorageItem item) throws IOException {
    try (ODatabaseDocumentTx db = openDb()) {
      return entityAdapter.edit(db, id.toString(), item);
    }
  }

  @Override
  public boolean remove(final CapabilityIdentity id) throws IOException {
    try (ODatabaseDocumentTx db = openDb()) {
      return entityAdapter.delete(db, id.toString());
    }
  }

  @Override
  public Map<CapabilityIdentity, CapabilityStorageItem> getAll() throws IOException {
    Map<CapabilityIdentity, CapabilityStorageItem> items = Maps.newHashMap();

    try (ODatabaseDocumentTx db = openDb()) {
      for (CapabilityStorageItem item : entityAdapter.browse.execute(db)) {
        items.put(identity(item), item);
      }
    }

    return items;
  }
}
