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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_LAST_ACCESSED;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;

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
  private final ComponentEntityAdapter componentEntityAdapter;

  @Inject
  public PurgeUnusedFacetImpl(final ComponentEntityAdapter componentEntityAdapter)
  {
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
  }

  @Override
  @Guarded(by = STARTED)
  public void purgeUnused(final int numberOfDays) {
    checkArgument(numberOfDays > 0, "Number of days must be greater then zero");
    log.info("Purging unused components from repository {}", getRepository().getName());

    Date olderThan = DateTime.now().minusDays(numberOfDays).withTimeAtStartOfDay().toDate();

    UnitOfWork.beginBatch(facet(StorageFacet.class).txSupplier());
    try {
      deleteUnusedComponents(olderThan);
      deleteUnusedAssets(olderThan);
    }
    finally {
      UnitOfWork.end();
    }
  }

  /**
   * Delete all unused components.
   */
  @TransactionalDeleteBlob
  protected void deleteUnusedComponents(final Date olderThan) {
    StorageTx tx = UnitOfWork.currentTx();

    for (Component component : findUnusedComponents(tx, olderThan)) {
      log.debug("Deleting unused component {}", component);
      tx.deleteComponent(component); // TODO: commit in batches
    }
  }

  /**
   * Delete all unused assets.
   */
  @TransactionalDeleteBlob
  protected void deleteUnusedAssets(final Date olderThan) {
    StorageTx tx = UnitOfWork.currentTx();

    for (Asset asset : findUnusedAssets(tx, olderThan)) {
      log.debug("Deleting unused asset {}", asset);
      tx.deleteAsset(asset); // TODO: commit in batches
    }
  }

  /**
   * Find all components that were last accessed before specified date. Date when a component was last accessed is the
   * last time an asset of that component was last accessed.
   */
  private Iterable<Component> findUnusedComponents(final StorageTx tx, final Date olderThan) {
    final Bucket bucket = tx.findBucket(getRepository());

    String sql = String.format(
        "SELECT FROM (SELECT %s, MAX(%s) AS lastAccessed FROM asset WHERE %s=:bucket AND %s IS NOT NULL GROUP BY %s) WHERE lastAccessed < :olderThan",
        P_COMPONENT, P_LAST_ACCESSED, P_BUCKET, P_COMPONENT, P_COMPONENT
    );

    Map<String, Object> sqlParams = ImmutableMap.of(
        "bucket", AttachedEntityHelper.id(bucket),
        "olderThan", olderThan
    );

    return Iterables.transform(tx.browse(sql, sqlParams),
        (doc) -> componentEntityAdapter.readEntity(doc.field(P_COMPONENT)));
  }

  /**
   * Find all assets without component that were last accessed before specified date.
   */
  private Iterable<Asset> findUnusedAssets(final StorageTx tx, final Date olderThan) {
    String whereClause = String.format("%s IS NULL AND %s < :olderThan", P_COMPONENT, P_LAST_ACCESSED);
    Map<String, Object> sqlParams = ImmutableMap.of("olderThan", olderThan);

    return tx.findAssets(whereClause, sqlParams, ImmutableList.of(getRepository()), null);
  }
}
