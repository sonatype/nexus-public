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
package org.sonatype.nexus.repository.apt.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.AptFacet;
import org.sonatype.nexus.repository.apt.AptRestoreFacet;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.apt.internal.debian.Utils;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.apt.internal.debian.Utils.isDebPackageContentType;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 3.17
 */
@Named
public class AptRestoreFacetImpl
    extends FacetSupport
    implements AptRestoreFacet
{
  @Override
  @TransactionalStoreBlob
  public Content restore(final AssetBlob assetBlob, final String path) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset;
    AptFacet aptFacet = facet(AptFacet.class);
    if (isDebPackageContentType(path)) {
      ControlFile controlFile = AptPackageParser.getDebControlFile(assetBlob.getBlob());
      asset = aptFacet.findOrCreateDebAsset(tx, path, new PackageInfo(controlFile));
    }
    else {
      asset = aptFacet.findOrCreateMetadataAsset(tx, path);
    }

    tx.attachBlob(asset, assetBlob);
    Content.applyToAsset(asset, Content.maintainLastModified(asset, new AttributesMap()));
    tx.saveAsset(asset);
    return FacetHelper.toContent(asset, assetBlob.getBlob());
  }

  @Override
  @TransactionalTouchBlob
  public boolean assetExists(final String path) {
    final StorageTx tx = UnitOfWork.currentTx();
    return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository())) != null;
  }

  @Override
  public Query getComponentQuery(final Blob blob) throws IOException {
    final InputStream inputStream = blob.getInputStream();
    final PackageInfo packageInfo = new PackageInfo(AptPackageParser.parsePackage(() -> inputStream));
    return Query.builder()
        .where(P_NAME).eq(packageInfo.getPackageName())
        .and(P_VERSION).eq(packageInfo.getVersion())
        .and(P_GROUP).eq(packageInfo.getArchitecture())
        .build();
  }

  @Override
  public boolean componentRequired(final String name) {
   return Utils.isDebPackageContentType(name);
  }
}
