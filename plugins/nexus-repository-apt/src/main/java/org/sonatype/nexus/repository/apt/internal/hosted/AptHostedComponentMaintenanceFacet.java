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
package org.sonatype.nexus.repository.apt.internal.hosted;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.apt.internal.hosted.AptHostedFacet.AssetAction;
import org.sonatype.nexus.repository.apt.internal.hosted.AptHostedFacet.AssetChange;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.common.concur.ONeedRetryException;

import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * @since 3.next
 */
@Named
public class AptHostedComponentMaintenanceFacet
    extends DefaultComponentMaintenanceImpl
{
  @Transactional(retryOn = ONeedRetryException.class)
  @Override
  protected Set<String> deleteAssetTx(final EntityId assetId, final boolean deleteBlobs) {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = tx.findAsset(assetId, bucket);

    if (asset == null) {
      return Collections.emptySet();
    }

    String assetKind = asset.formatAttributes().get(P_ASSET_KIND, String.class);
    Set<String> result = super.deleteAssetTx(assetId, deleteBlobs);
    if ("DEB".equals(assetKind)) {
      try {
        getRepository().facet(AptHostedFacet.class)
            .rebuildIndexes(Collections.singletonList(new AptHostedFacet.AssetChange(AssetAction.REMOVED, asset)));
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    if (asset.componentId() != null) {
      Component component = tx.findComponentInBucket(asset.componentId(), bucket);

      if (!tx.browseAssets(component).iterator().hasNext()) {
        log.debug("Deleting component: {}", component);
        tx.deleteComponent(component, deleteBlobs);
      }
    }

    return result;
  }

  @TransactionalDeleteBlob
  @Override
  protected DeletionResult deleteComponentTx(final EntityId componentId, final boolean deleteBlobs) {
    StorageTx tx = UnitOfWork.currentTx();
    Component component = tx.findComponentInBucket(componentId, tx.findBucket(getRepository()));

    if (component == null) {
      return new DeletionResult(null, Collections.emptySet());
    }

    Iterable<Asset> assets = tx.browseAssets(component);
    List<AssetChange> changes = new ArrayList<>();
    for (Asset asset : assets) {
      changes.add(new AssetChange(AssetAction.REMOVED, asset));
    }

    log.debug("Deleting component: {}", component.toStringExternal());
    DeletionResult result = new DeletionResult(component, tx.deleteComponent(component, deleteBlobs));
    try {
      getRepository().facet(AptHostedFacet.class).rebuildIndexes(changes);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return result;
  }
}
