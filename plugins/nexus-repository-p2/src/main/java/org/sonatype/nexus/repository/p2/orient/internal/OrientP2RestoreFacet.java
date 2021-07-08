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

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.metadata.P2Attributes;
import org.sonatype.nexus.repository.p2.internal.util.P2PathUtils;
import org.sonatype.nexus.repository.p2.internal.util.P2TempBlobUtils;
import org.sonatype.nexus.repository.p2.orient.P2Facet;
import org.sonatype.nexus.repository.p2.orient.P2RestoreFacet;
import org.sonatype.nexus.repository.p2.orient.internal.util.OrientP2PathUtils;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 3.28
 */
@Named
@Priority(Integer.MAX_VALUE)
public class OrientP2RestoreFacet
    extends FacetSupport
    implements P2RestoreFacet
{
  private final P2TempBlobUtils p2TempBlobUtils;

  @Inject
  public OrientP2RestoreFacet(final P2TempBlobUtils p2TempBlobUtils) {
    this.p2TempBlobUtils = p2TempBlobUtils;
  }

  @Override
  @TransactionalTouchBlob
  public void restore(final AssetBlob assetBlob, final String path) {
    StorageTx tx = UnitOfWork.currentTx();
    P2Facet facet = facet(P2Facet.class);

    Asset asset;
    if (componentRequired(path)) {
      P2Attributes attributes = P2Attributes.builder().build();
      try {
        attributes = getComponentAttributes(assetBlob.getBlob(), path);
      }
      catch (IOException e) {
        log.error("Exception of extracting components attributes from blob {}", assetBlob);
      }

      Component component = facet.findOrCreateComponent(tx, attributes);
      asset = facet.findOrCreateAsset(tx, component, path, attributes);
    }
    else {
      asset = facet.findOrCreateAsset(tx, path);
    }
    tx.attachBlob(asset, assetBlob);

    Content.applyToAsset(asset, Content.maintainLastModified(asset, new AttributesMap()));
    tx.saveAsset(asset);
  }

  @Override
  @TransactionalTouchBlob
  public boolean assetExists(final String path) {
    final StorageTx tx = UnitOfWork.currentTx();
    return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository())) != null;
  }

  @Override
  public boolean componentRequired(final String name) {
    AssetKind assetKind = facet(P2Facet.class).getAssetKind(name);

    return CacheControllerHolder.CONTENT.equals(assetKind.getCacheType());
  }

  @Override
  public Query getComponentQuery(final Blob blob, final String blobName, final String blobStoreName)
      throws IOException
  {
    P2Attributes attributes = getComponentAttributes(blob, blobName);

    return Query.builder().where(P_NAME).eq(attributes.getComponentName())
        .and(P_VERSION).eq(attributes.getComponentVersion()).build();
  }

  private P2Attributes getComponentAttributes(final Blob blob, final String blobName)
      throws IOException
  {
    P2Attributes attributes;
    AssetKind assetKind = P2PathUtils.getAssetKind(blobName);
    if (AssetKind.BINARY_BUNDLE == assetKind) {
      attributes = P2PathUtils.getBinaryAttributesFromBlobName(blobName);
    }
    else {
      StorageFacet storageFacet = facet(StorageFacet.class);
      attributes = OrientP2PathUtils.getPackageAttributesFromBlob(storageFacet, p2TempBlobUtils, blob, blobName);
    }

    return attributes;
  }
}
