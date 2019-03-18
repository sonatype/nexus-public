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
package org.sonatype.nexus.repository.pypi.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;

/**
 * A component maintenance facet that removes the component once no assets exist for it. Used for PyPI since we have to
 * create a component once the first asset with a new name and version exists, but no component-like structure exists
 * in PyPI.
 *
 * @since 3.1
 */
@Named
public class PyPiComponentMaintenance
    extends DefaultComponentMaintenanceImpl
{
  /**
   * Deletes the asset. If the associated component has no additional assets, then the component is also deleted.
   */
  @TransactionalDeleteBlob
  protected Set<String> deleteAssetTx(final EntityId assetId, final boolean deleteBlob) {
    StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());
    final Asset asset = tx.findAsset(assetId, bucket);
    if (asset == null) {
      return Collections.emptySet();
    }
    Set<String> deletedAssets = new HashSet<>();
    deletedAssets.addAll(super.deleteAssetTx(assetId, deleteBlob));

    final EntityId componentId = asset.componentId();
    if (componentId != null) {
      deleteRootIndex();
      
      // We are deleting a package and therefore need to remove the associated index as it is no longer valid
      deleteCachedIndex(asset.formatAttributes().get(P_NAME, String.class));
      
      final Component component = tx.findComponentInBucket(componentId, bucket);
      if (component != null && !tx.browseAssets(component).iterator().hasNext()) {
        deletedAssets.addAll(deleteComponentTx(componentId, deleteBlob).getAssets());
      }
    }

    return deletedAssets;
  }

  /**
   * Deletes the root AND package index if a component has been deleted
   */
  @TransactionalDeleteBlob
  @Override
  protected DeletionResult deleteComponentTx(final EntityId componentId, final boolean deleteBlobs) {
    StorageTx tx = UnitOfWork.currentTx();
    Component component = tx.findComponentInBucket(componentId, tx.findBucket(getRepository()));
    if (component == null) {
      return new DeletionResult(null, Collections.emptySet());
    }

    deleteRootIndex();

    deleteCachedIndex(component.name());

    log.debug("Deleting component: {}", component.toStringExternal());
    return new DeletionResult(component, tx.deleteComponent(component, deleteBlobs));
  }

  private void deleteRootIndex() {
    PyPiIndexFacet facet = getRepository().facet(PyPiIndexFacet.class);
    facet.deleteRootIndex();
  }

  private void deleteCachedIndex(final String assetName) {
    PyPiIndexFacet facet = getRepository().facet(PyPiIndexFacet.class);
    facet.deleteIndex(assetName);
  }
}
