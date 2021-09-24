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
package org.sonatype.nexus.orient.raw.internal;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.orient.raw.RawContentFacet;
import org.sonatype.nexus.repository.raw.RawCoordinatesHelper;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.OrientReplicationFacetSupport;
import org.sonatype.nexus.repository.storage.ReplicationFacet;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * A {@link ReplicationFacet} for raw content running with orient.
 *
 * @since 3.31
 */
@Named
public class OrientRawReplicationFacet
    extends OrientReplicationFacetSupport
{
  @Override
  public void doReplicate(final String path,
                        final AssetBlob assetBlob,
                        final NestedAttributesMap assetAttributes,
                        final NestedAttributesMap componentAttributes) {
    StorageFacet storageFacet = facet(StorageFacet.class);
    UnitOfWork.begin(storageFacet.txSupplier());
    try {
      putPreservingAllAttributes(path, assetBlob, assetAttributes);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @TransactionalStoreBlob
  protected void putPreservingAllAttributes(final String path, final AssetBlob assetBlob, @Nullable final AttributesMap contentAttributes) {
    RawContentFacet rawContentFacet = facet(RawContentFacet.class);
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = rawContentFacet.getOrCreateAsset(getRepository(), path, RawCoordinatesHelper.getGroup(path), path);
    tx.attachBlob(asset, assetBlob);
    asset.attributes((NestedAttributesMap) contentAttributes);
    tx.saveAsset(asset);
  }

  @Override
  public boolean doReplicateDelete(final String path) {
    Asset asset;
    StorageFacet storageFacet = facet(StorageFacet.class);
    UnitOfWork.begin(storageFacet.txSupplier());
    try {
      asset = findAssetTransactional(path);
      if (asset == null) {
        log.debug("Skipping replication delete with asset {} as it doesn't exist.", path);
        return false;
      }
    }
    finally {
      UnitOfWork.end();
    }

    log.debug("Replicating delete to asset {}", path);
    return !getRepository().facet(ComponentMaintenance.class).deleteAsset(asset.getEntityMetadata().getId())
        .isEmpty();
  }

  @Transactional
  protected Asset findAssetTransactional(final String path) {
    StorageTx tx = UnitOfWork.currentTx();
    return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository()));
  }
}
