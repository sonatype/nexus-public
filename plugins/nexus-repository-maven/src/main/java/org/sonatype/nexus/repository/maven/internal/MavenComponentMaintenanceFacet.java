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
package org.sonatype.nexus.repository.maven.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

/**
 * maven format specific hosted and proxy {@link ComponentMaintenance}.
 *
 * @since 3.15
 */
@Named
public class MavenComponentMaintenanceFacet
    extends DefaultComponentMaintenanceImpl
{
  @TransactionalDeleteBlob
  @Override
  protected Set<String> deleteAssetTx(final EntityId assetId, final boolean deleteBlob) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = tx.findAsset(assetId);
    if (asset == null) {
      return Collections.emptySet();
    }

    Set<String> deletedAssets = new HashSet<>(super.deleteAssetTx(assetId, deleteBlob));

    final EntityId componentId = asset.componentId();
    if (componentId != null) {
      Component component = tx.findComponent(componentId);
      if (component != null && !tx.browseAssets(component).iterator().hasNext()) {
        deletedAssets.addAll(deleteComponentTx(componentId, deleteBlob).getAssets());
      }
    }

    return deletedAssets;
  }
}
