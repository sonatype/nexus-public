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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.pypi.PyPiFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.copyAttributes;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findAsset;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findComponent;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractFilenameFromPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractNameFromFilename;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractVersionFromFilename;
import static org.sonatype.nexus.repository.pypi.internal.PyPiInfoUtils.extractMetadata;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.isIndexPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.isPackagePath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.isRootIndexPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.normalizeName;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * @since 3.14
 */
@Named
public class PyPiFacetImpl
    extends FacetSupport
    implements PyPiFacet
{
  private final AssetEntityAdapter assetEntityAdapter;

  @Inject
  public PyPiFacetImpl(final AssetEntityAdapter assetEntityAdapter) {
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
  }

  @Override
  public boolean assetExists(final String name) {
    final StorageTx tx = UnitOfWork.currentTx();
    return assetEntityAdapter.exists(tx.getDb(), name, tx.findBucket(getRepository()));
  }

  @Override
  @Nullable
  public Asset put(final String path, final AssetBlob assetBlob) throws IOException {
    if (isPackagePath(path)) {
      return putPackage(path, assetBlob);
    }
    else if (isRootIndexPath(path)) {
      log.info("Not repairing root index");
      return null;
    }
    else if (isIndexPath(path)) {
      return putIndex(path, assetBlob);
    }
    else {
      log.warn("Path does not represent a PyPI package or index: {}", path);
      return null;
    }
  }

  @Override
  public Asset putPackage(final String path, final AssetBlob assetBlob) throws IOException {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());

    Asset asset = findAsset(tx, bucket, path);

    if (asset == null) {
      final String filename = extractFilenameFromPath(path);

      Map<String, String> attributes;
      try (InputStream is = assetBlob.getBlob().getInputStream()) {
        attributes = extractMetadata(is);
      }

      if (!attributes.containsKey(P_NAME)) {
        log.debug("No name found in metadata for {}, extracting from filename.", filename);
        attributes.put(P_NAME, normalizeName(extractNameFromFilename(filename)));
      }
      if (!attributes.containsKey(P_VERSION)) {
        log.debug("No version found in metadata for {}, extracting from filename.", filename);
        attributes.put(P_VERSION, extractVersionFromFilename(filename));
      }

      final String name = attributes.get(P_NAME);
      final String version = attributes.get(P_VERSION);

      Component  component = findComponent(tx, getRepository(), name, version);
      if (component == null) {
        component = tx.createComponent(bucket, getRepository().getFormat());
        component.name(name);
        component.version(version);
        component.formatAttributes().set(P_SUMMARY, attributes.get(P_SUMMARY));
        tx.saveComponent(component);
      }
      asset = tx.createAsset(bucket, component);
      asset.name(path);
      copyAttributes(asset, attributes);
      saveAsset(tx, asset, assetBlob, AssetKind.PACKAGE);
    }

    return asset;
  }

  @Override
  public Asset putIndex(final String path, final AssetBlob assetBlob) {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());

    Asset asset = findAsset(tx, bucket, path);

    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(path);
      saveAsset(tx, asset, assetBlob, AssetKind.INDEX);
    }

    return asset;
  }

  private void saveAsset(final StorageTx tx, final Asset asset, final AssetBlob assetBlob, final AssetKind kind) {
    asset.formatAttributes().set(P_ASSET_KIND, kind);
    tx.attachBlob(asset, assetBlob);
    DateTime blobCreationTime = assetBlob.getBlob().getMetrics().getCreationTime();
    asset.blobCreated(blobCreationTime);
    asset.blobUpdated(blobCreationTime);
    tx.saveAsset(asset);
  }
}
