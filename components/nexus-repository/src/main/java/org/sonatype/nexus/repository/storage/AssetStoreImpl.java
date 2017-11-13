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
package org.sonatype.nexus.repository.storage;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.entity.AttachedEntityId;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.entity.EntityHelper.hasMetadata;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * @since 3.6
 */
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
@Named
public class AssetStoreImpl
    extends StateGuardLifecycleSupport
    implements AssetStore, Lifecycle
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final AssetEntityAdapter entityAdapter;

  @Inject
  public AssetStoreImpl(@Named("component") final Provider<DatabaseInstance> databaseInstance,
                        final AssetEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  @Guarded(by = STARTED)
  public Asset getById(final EntityId id) {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return entityAdapter.read(db, id);
    }
  }

  @Override
  public Iterable<Asset> getByIds(final Iterable<EntityId> ids) {
    return inTxRetry(databaseInstance).call(db -> entityAdapter.transform(entityAdapter.documents(db, ids)));
  }

  @Override
  @Guarded(by = STARTED)
  public long countAssets(@Nullable final Iterable<Bucket> buckets) {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return entityAdapter.countByQuery(db, null, null, buckets, null);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public OIndex<?> getIndex(final String indexName) {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return db.getMetadata().getIndexManager().getIndex(indexName);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public <T> List<Entry<T, EntityId>> getNextPage(final OIndexCursor cursor, final int limit) {
    List<Entry<T, EntityId>> page = new ArrayList<>(limit);

    // For reasons unknown Orient needs the connection despite the code not using it
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      cursor.setPrefetchSize(limit);
      while (page.size() < limit) {
        Entry<Object, OIdentifiable> entry = cursor.nextEntry();
        if (entry == null) {
          break;
        }

        @SuppressWarnings("unchecked")
        T key = (T) entry.getKey();
        EntityId value = new AttachedEntityId(entityAdapter, entry.getValue().getIdentity());
        page.add(new SimpleEntry<>(key, value));
      }
    }

    return page;
  }

  @Override
  @Guarded(by = STARTED)
  public Asset save(Asset asset) {
    if (hasMetadata(asset)) {
      inTxRetry(databaseInstance).run(db -> entityAdapter.editEntity(db, asset));
      return asset;
    }
    else {
      return inTxRetry(databaseInstance).call(db -> entityAdapter.readEntity(entityAdapter.addEntity(db, asset)));
    }
  }

  @Override
  public void save(final Iterable<Asset> assets) {
    inTxRetry(databaseInstance).run(db -> {
      assets.forEach(asset -> {
        if (hasMetadata(asset)) {
          entityAdapter.editEntity(db, asset);
        }
        else {
          entityAdapter.addEntity(db, asset);
        }
      });
    });
  }
}
