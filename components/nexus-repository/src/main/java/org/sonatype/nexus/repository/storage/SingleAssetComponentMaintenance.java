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

import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.common.concur.ONeedRetryException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * A component maintenance facet that assumes that Components have the same lifecycle as their
 * single Assets.
 *
 * @since 3.0
 */
@Named
public class SingleAssetComponentMaintenance
    extends DefaultComponentMaintenanceImpl
{
  /**
   * Deletes both the asset and its component.
   */
  @Override
  @Guarded(by = STARTED)
  public void deleteAsset(final EntityId assetId) {
    checkNotNull(assetId);
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      deleteAssetTx(assetId);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Transactional(retryOn = ONeedRetryException.class)
  protected void deleteAssetTx(final EntityId assetId) {
    StorageTx tx = UnitOfWork.currentTransaction();
    final Asset asset = tx.findAsset(assetId, tx.getBucket());
    final EntityId componentId = asset.componentId();
    if (componentId == null) {
      // Assets without components should be deleted on their own
      super.deleteAssetTx(assetId);
    }
    else {
      // Otherwise, delete the comopnent, which in turn cascades down to the asset
      deleteComponent(tx, componentId);
    }
  }

  private void deleteComponent(final StorageTx tx, final EntityId componentId) {
    final Component component = tx.findComponent(componentId, tx.getBucket());
    log.info("Deleting component: {}", component);
    tx.deleteComponent(component); // This in turn cascades back down to the asset
  }
}
