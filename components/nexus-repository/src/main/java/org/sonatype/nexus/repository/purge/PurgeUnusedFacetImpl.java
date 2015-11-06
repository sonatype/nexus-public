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
package org.sonatype.nexus.repository.purge;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_BUCKET;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_LAST_ACCESSED;

/**
 * {@link PurgeUnusedFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class PurgeUnusedFacetImpl
    extends FacetSupport
    implements PurgeUnusedFacet
{
  private final BucketEntityAdapter bucketEntityAdapter;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final AssetEntityAdapter assetEntityAdapter;

  @Inject
  public PurgeUnusedFacetImpl(final BucketEntityAdapter bucketEntityAdapter,
                              final ComponentEntityAdapter componentEntityAdapter,
                              final AssetEntityAdapter assetEntityAdapter)
  {
    this.bucketEntityAdapter = checkNotNull(bucketEntityAdapter);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
  }

  @Override
  @Guarded(by = STARTED)
  public void purgeUnused(final int numberOfDays) {
    checkArgument(numberOfDays > 0, "Number of days must be greater then zero");
    log.info("Purging unused components from repository {}", getRepository().getName());
    try (StorageTx tx = facet(StorageFacet.class).txSupplier().get()) {
      Date olderThan = DateTime.now().minusDays(numberOfDays).toDateMidnight().toDate();
      deleteUnusedComponents(tx, olderThan);
      deleteUnusedAssets(tx, olderThan);
    }
  }

  /**
   * Delete all unused components.
   */
  private void deleteUnusedComponents(final StorageTx tx, final Date olderThan) {
    tx.begin();
    Iterable<Component> components = findUnusedComponents(tx, olderThan);
    for (Component component : components) {
      log.debug("Deleting unused component {}", component);
      tx.deleteComponent(component);
    }
    tx.commit();
  }

  /**
   * Delete all unused assets.
   */
  private void deleteUnusedAssets(final StorageTx tx, final Date olderThan) {
    tx.begin();
    Iterable<Asset> assets = findUnusedAssets(tx, olderThan);
    for (Asset asset : assets) {
      log.debug("Deleting unused asset {}", asset);
      tx.deleteAsset(asset);
    }
    tx.commit();
  }

  /**
   * Find all components that were last accessed before specified date. Date when a component was last accessed is the
   * last time an asset of that component was last accessed.
   */
  private Iterable<Component> findUnusedComponents(final StorageTx tx, final Date olderThan) {
    String sql = String.format(
        "SELECT FROM (SELECT %s, MAX(%s) AS lastAccessed FROM %s WHERE %s=:bucket AND %s IS NOT NULL GROUP BY %s) WHERE lastAccessed < :olderThan",
        P_COMPONENT, P_LAST_ACCESSED, assetEntityAdapter.getTypeName(), P_BUCKET, P_COMPONENT, P_COMPONENT
    );
    Map<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("bucket", bucketEntityAdapter.recordIdentity(tx.findBucket(getRepository())));
    sqlParams.put("olderThan", olderThan);

    return Iterables.transform(tx.browse(sql, sqlParams), new Function<ODocument, Component>()
    {
      @Nullable
      @Override
      public Component apply(final ODocument input) {
        ORID componentId = input.field(P_COMPONENT, ORID.class);
        return tx.findComponent(new AttachedEntityId(componentEntityAdapter, componentId), tx.findBucket(getRepository()));
      }
    });
  }

  /**
   * Find all assets without component that were last accessed before specified date.
   */
  private Iterable<Asset> findUnusedAssets(final StorageTx tx, final Date olderThan) {
    String whereClause = String.format("%s IS NULL AND %s < :olderThan", P_COMPONENT, P_LAST_ACCESSED);
    Map<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("olderThan", olderThan);

    return tx.findAssets(whereClause, sqlParams, ImmutableList.of(getRepository()), null);
  }
}
