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
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.npm.NpmFacet;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind;
import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexInvalidatedEvent;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.npm.NpmCoordinateUtil.extractVersion;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.PACKAGE_ROOT;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.REPOSITORY_ROOT;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.TARBALL;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.REPOSITORY_ROOT_ASSET;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.getOrCreateTarballComponent;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.tarballAssetName;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * @since 3.6.1
 */
@Named
public class NpmFacetImpl
    extends FacetSupport
    implements NpmFacet
{
  private final NpmPackageParser npmPackageParser;

  @Inject
  public NpmFacetImpl(final NpmPackageParser npmPackageParser) {
    this.npmPackageParser = checkNotNull(npmPackageParser);
  }

  @Nullable
  @Override
  public Asset findRepositoryRootAsset() {
    final StorageTx tx = UnitOfWork.currentTx();
    return NpmFacetUtils.findRepositoryRootAsset(tx, tx.findBucket(getRepository()));
  }

  @Nullable
  @Override
  public Asset putRepositoryRoot(final AssetBlob assetBlob, @Nullable final AttributesMap contentAttributes)
      throws IOException
  {
    final Repository repository = getRepository();
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    Asset asset = NpmFacetUtils.findRepositoryRootAsset(tx, bucket);

    if (asset == null) {
      asset = tx.createAsset(bucket, repository.getFormat()).name(REPOSITORY_ROOT_ASSET);
      getEventManager().post(new NpmSearchIndexInvalidatedEvent(repository));
      saveAsset(tx, asset, assetBlob, REPOSITORY_ROOT, contentAttributes);
    }

    return asset;
  }

  @Nullable
  @Override
  public Asset findPackageRootAsset(final String packageId) {
    final StorageTx tx = UnitOfWork.currentTx();
    return NpmFacetUtils.findPackageRootAsset(tx, tx.findBucket(getRepository()), NpmPackageId.parse(packageId));
  }

  @Nullable
  @Override
  public Asset putPackageRoot(final String packageId,
                              final AssetBlob assetBlob,
                              @Nullable final AttributesMap contentAttributes)
      throws IOException
  {
    final Repository repository = getRepository();
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    Asset asset = NpmFacetUtils.findPackageRootAsset(tx, bucket, NpmPackageId.parse(packageId));

    if (asset == null) {
      asset = tx.createAsset(bucket, repository.getFormat()).name(packageId);
      saveAsset(tx, asset, assetBlob, PACKAGE_ROOT, contentAttributes);
    }

    return asset;
  }

  @Nullable
  @Override
  public Asset findTarballAsset(final String packageId,
                                final String tarballName)
  {
    final StorageTx tx = UnitOfWork.currentTx();
    return NpmFacetUtils
        .findTarballAsset(tx, tx.findBucket(getRepository()), NpmPackageId.parse(packageId), tarballName);
  }

  @Nullable
  @Override
  public Asset putTarball(final String packageId,
                          final String tarballName,
                          final AssetBlob assetBlob,
                          @Nullable final AttributesMap contentAttributes) throws IOException
  {
    final Repository repository = getRepository();
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    NpmPackageId npmPackageId = NpmPackageId.parse(packageId);
    Asset asset = NpmFacetUtils.findTarballAsset(tx, bucket, npmPackageId, tarballName);

    if (asset == null) {
      String version = extractVersion(tarballName);
      Component tarballComponent = getOrCreateTarballComponent(tx, repository, npmPackageId, version);
      asset = tx.firstAsset(tarballComponent);

      if (asset == null) {
        asset = tx.createAsset(bucket, tarballComponent).name(tarballAssetName(npmPackageId, tarballName));
      }
    }

    maybeExtractFormatAttributes(tx, packageId, asset, assetBlob);
    saveAsset(tx, asset, assetBlob, TARBALL, contentAttributes);

    return asset;
  }

  private void saveAsset(final StorageTx tx,
                         final Asset asset,
                         final AssetBlob assetBlob,
                         final AssetKind kind,
                         @Nullable final AttributesMap contentAttributes)
  {
    asset.formatAttributes().set(P_ASSET_KIND, kind.name());
    tx.attachBlob(asset, assetBlob);
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    tx.saveAsset(asset);
  }

  private void maybeExtractFormatAttributes(final StorageTx tx,
                                            final String packageId,
                                            final Asset asset,
                                            final AssetBlob assetBlob)
  {
    Blob blob = tx.requireBlob(assetBlob.getBlobRef());
    Map<String, Object> formatAttributes = npmPackageParser.parsePackageJson(blob::getInputStream);
    if (formatAttributes.isEmpty()) {
      log.warn("No format attributes found in package.json for npm package ID {}, will not be searchable", packageId);
    }
    else {
      NpmFormatAttributesExtractor formatAttributesExtractor = new NpmFormatAttributesExtractor(formatAttributes);
      formatAttributesExtractor.copyFormatAttributes(asset);
    }
  }
}
