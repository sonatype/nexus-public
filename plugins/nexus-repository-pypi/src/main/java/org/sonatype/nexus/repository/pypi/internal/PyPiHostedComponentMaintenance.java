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

import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

/**
 * A component maintenance facet that removes the component once no assets exist for it. Used for PyPI since we have to
 * create a component once the first asset with a new name and version exists, but no component-like structure exists
 * in PyPI.
 *
 * @since 3.1
 */
@Named
public class PyPiHostedComponentMaintenance
    extends DefaultComponentMaintenanceImpl
{
  /**
   * Deletes the asset. If the associated component has no additional assets, then the component is also deleted.
   */
  @TransactionalDeleteBlob
  protected void deleteAssetTx(final EntityId assetId, final boolean deleteBlob) {
    StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());
    final Asset asset = tx.findAsset(assetId, bucket);
    if (asset == null) {
      return;
    }
    super.deleteAssetTx(assetId, deleteBlob);

    final EntityId componentId = asset.componentId();
    if (componentId == null) {
      return;
    }
    final Component component = tx.findComponentInBucket(componentId, bucket);
    if (component == null || tx.browseAssets(component).iterator().hasNext()) {
      return;
    }
    deleteComponentTx(componentId, deleteBlob);
  }
}
