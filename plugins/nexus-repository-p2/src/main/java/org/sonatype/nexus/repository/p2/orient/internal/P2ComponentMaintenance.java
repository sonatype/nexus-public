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
package org.sonatype.nexus.repository.p2.orient.internal;

import java.util.Collections;
import java.util.Set;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * P2 Component and asset removing implementation
 *
 * @since 3.28
 */
public class P2ComponentMaintenance
    extends DefaultComponentMaintenanceImpl
{
  /**
   * Deletes the asset and its component if it's the only asset in it.
   */
  @Override
  @Guarded(by = STARTED)
  @TransactionalDeleteBlob
  protected Set<String> deleteAssetTx(final EntityId assetId, final boolean deleteBlob) {
    StorageTx tx = UnitOfWork.currentTx();
    final Asset asset = tx.findAsset(assetId, tx.findBucket(getRepository()));
    if (asset == null) {
      return Collections.emptySet();
    }

    final EntityId componentId = asset.componentId();
    if (componentId == null) {
      // Assets without components should be deleted on their own
      return super.deleteAssetTx(assetId, deleteBlob);
    }

    final Component component = tx.findComponent(componentId);
    if (component == null) {
      return Collections.emptySet();
    }
    return deleteComponentTx(componentId, deleteBlob).getAssets();
  }
}
