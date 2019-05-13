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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind;
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
  protected DeletionResult deleteComponentTx(final EntityId componentId, final boolean deleteBlobs) {
    StorageTx tx = UnitOfWork.currentTx();
    Component component = tx.findComponentInBucket(componentId, tx.findBucket(getRepository()));
    if (component == null) {
      return new DeletionResult(null, Collections.emptySet());
    }
    Set<String> deletedAssets = new HashSet<>();
    tx.browseAssets(component).forEach(a -> deletedAssets.addAll(deleteAssetTx(a, deleteBlobs)));
    return new DeletionResult(component, deletedAssets);
  }

  /**
   * Deletes depending on what it is.
   */
  @Override
  @TransactionalDeleteBlob
  protected Set<String> deleteAssetTx(final EntityId assetId, final boolean deleteBlob) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = tx.findAsset(assetId, tx.findBucket(getRepository()));
    if (asset == null) {
      return Collections.emptySet();
    }
    return deleteAssetTx(asset, deleteBlob);
  }

  private Set<String> deleteAssetTx(final Asset asset, final boolean deleteBlob) {
    AssetKind assetKind = AssetKind.valueOf(asset.formatAttributes().get(P_ASSET_KIND, String.class));
    Set<String> deletedAssets = new HashSet<>();
    try {
      if (AssetKind.PACKAGE_ROOT == assetKind) {
        NpmPackageId packageId = NpmPackageId.parse(asset.name());
        deletedAssets.addAll(deletePackageRoot(packageId, deleteBlob));
      }
      else if (AssetKind.TARBALL == assetKind) {
        NpmPackageId packageId = NpmPackageId.parse(asset.name().substring(0, asset.name().indexOf("/-/")));
        String tarballName = NpmMetadataUtils.extractTarballName(asset.name());
        deletedAssets.addAll(deleteTarball(packageId, tarballName, deleteBlob));

        EntityId componentId = asset.componentId();
        if (componentId != null) {
          StorageTx tx = UnitOfWork.currentTx();
          Component component = tx.findComponent(componentId);
          if (component != null && !tx.browseAssets(component).iterator().hasNext()) {
            deletedAssets.addAll(deleteComponentTx(componentId, deleteBlob).getAssets());
          }
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return deletedAssets;
  }

  /**
   * Deletes package and all related tarballs too.
   */
  private Set<String> deletePackageRoot(final NpmPackageId packageId, final boolean deleteBlob) throws IOException {
    return getRepository().facet(NpmHostedFacet.class).deletePackage(packageId, null, deleteBlob);
  }

  /**
   * Deletes tarball and updates package root.
   */
  private Set<String> deleteTarball(final NpmPackageId packageId, final String tarballName, final boolean deleteBlob)
      throws IOException
  {
    final StorageTx tx = UnitOfWork.currentTx();
    Asset packageRootAsset = NpmFacetUtils.findPackageRootAsset(
        tx, tx.findBucket(getRepository()), packageId
    );
    if (packageRootAsset == null) {
      return Collections.emptySet();
    }
    NestedAttributesMap packageRoot = NpmFacetUtils.loadPackageRoot(tx, packageRootAsset);
    NestedAttributesMap version = NpmMetadataUtils.selectVersionByTarballName(packageRoot, tarballName);
    if (version == null) {
      return Collections.emptySet();
    }
    packageRoot.child(NpmMetadataUtils.VERSIONS).remove(version.getKey());
    if (packageRoot.child(NpmMetadataUtils.VERSIONS).isEmpty()) {
      return getRepository().facet(NpmHostedFacet.class).deletePackage(packageId, null, deleteBlob);
    }
    else {
      Iterator<Entry<String, Object>> distTags = packageRoot.child(NpmMetadataUtils.DIST_TAGS).iterator();
      while (distTags.hasNext()) {
        Entry<String, Object> distTag = distTags.next();
        if (version.getKey().equals(distTag.getValue())) {
          distTags.remove();
        }
      }
      packageRoot.child(NpmMetadataUtils.TIME).remove(version.getKey());
      NpmMetadataUtils.maintainTime(packageRoot);
      NpmFacetUtils.savePackageRoot(UnitOfWork.currentTx(), packageRootAsset, packageRoot);
      return getRepository().facet(NpmHostedFacet.class).deleteTarball(packageId, tarballName, deleteBlob);
    }
  }
}
