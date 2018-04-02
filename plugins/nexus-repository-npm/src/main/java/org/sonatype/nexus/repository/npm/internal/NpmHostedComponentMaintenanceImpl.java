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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.util.Map.Entry;

import javax.inject.Named;

import org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * npm format specific hosted {@link ComponentMaintenance}.
 *
 * @since 3.0
 */
@Named
public class NpmHostedComponentMaintenanceImpl
    extends DefaultComponentMaintenanceImpl
{
  @Override
  @TransactionalDeleteBlob
  protected void deleteComponentTx(final EntityId componentId, final boolean deleteBlobs) {
    StorageTx tx = UnitOfWork.currentTx();
    Component component = tx.findComponentInBucket(componentId, tx.findBucket(getRepository()));
    if (component == null) {
      return;
    }
    tx.browseAssets(component).forEach(a -> deleteAssetTx(a, deleteBlobs));
  }

  /**
   * Deletes depending on what it is.
   */
  @TransactionalDeleteBlob
  protected void deleteAssetTx(final EntityId assetId, final boolean deleteBlob) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = tx.findAsset(assetId, tx.findBucket(getRepository()));
    if (asset == null) {
      return;
    }
    deleteAssetTx(asset, deleteBlob);
  }

  private void deleteAssetTx(final Asset asset, final boolean deleteBlob) {
    AssetKind assetKind = AssetKind.valueOf(asset.formatAttributes().get(P_ASSET_KIND, String.class));
    try {
      if (AssetKind.PACKAGE_ROOT == assetKind) {
        NpmPackageId packageId = NpmPackageId.parse(asset.name());
        deletePackageRoot(packageId, deleteBlob);
      }
      else if (AssetKind.TARBALL == assetKind) {
        NpmPackageId packageId = NpmPackageId.parse(asset.name().substring(0, asset.name().indexOf("/-/")));
        String tarballName = NpmMetadataUtils.extractTarballName(asset.name());
        deleteTarball(packageId, tarballName, deleteBlob);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deletes package and all related tarballs too.
   */
  private boolean deletePackageRoot(final NpmPackageId packageId, final boolean deleteBlob) throws IOException {
    return getRepository().facet(NpmHostedFacet.class).deletePackage(packageId, null, deleteBlob);
  }

  /**
   * Deletes tarball and updates package root.
   */
  private boolean deleteTarball(final NpmPackageId packageId, final String tarballName, final boolean deleteBlob)
      throws IOException
  {
    final StorageTx tx = UnitOfWork.currentTx();
    Asset packageRootAsset = NpmFacetUtils.findPackageRootAsset(
        tx, tx.findBucket(getRepository()), packageId
    );
    if (packageRootAsset == null) {
      return false;
    }
    NestedAttributesMap packageRoot = NpmFacetUtils.loadPackageRoot(tx, packageRootAsset);
    NestedAttributesMap version = NpmMetadataUtils.selectVersionByTarballName(packageRoot, tarballName);
    if (version == null) {
      return false;
    }
    packageRoot.child(NpmMetadataUtils.VERSIONS).remove(version.getKey());
    if (packageRoot.child(NpmMetadataUtils.VERSIONS).isEmpty()) {
      return getRepository().facet(NpmHostedFacet.class).deletePackage(packageId, null, deleteBlob);
    }
    else {
      NestedAttributesMap distTags = packageRoot.child(NpmMetadataUtils.DIST_TAGS);
      for (Entry<String, Object> distTag : distTags) {
        if (version.getKey().equals(distTag.getValue())) {
          distTags.remove(distTag.getKey());
        }
      }
      packageRoot.child(NpmMetadataUtils.TIME).remove(version.getKey());
      NpmMetadataUtils.maintainTime(packageRoot);
      NpmFacetUtils.savePackageRoot(UnitOfWork.currentTx(), packageRootAsset, packageRoot);
      return getRepository().facet(NpmHostedFacet.class).deleteTarball(packageId, tarballName, deleteBlob);
    }
  }
}
