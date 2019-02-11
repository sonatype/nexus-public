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
package org.sonatype.nexus.blobstore.restore.internal;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.BaseRestoreBlobStrategy;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.internal.NpmRestoreBlobData.NpmType;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.npm.NpmFacet;
import org.sonatype.nexus.repository.npm.repair.NpmRepairPackageRootComponent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.Query.Builder;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.npm.NpmCoordinateUtil.extractVersion;
import static org.sonatype.nexus.repository.npm.NpmCoordinateUtil.getPackageIdName;
import static org.sonatype.nexus.repository.npm.NpmCoordinateUtil.getPackageIdScope;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 3.6.1
 */
@Named("npm")
@Singleton
public class NpmRestoreBlobStrategy
    extends BaseRestoreBlobStrategy<NpmRestoreBlobData>
{
  private final NpmRepairPackageRootComponent npmRepairPackageRootComponent;

  @Inject
  public NpmRestoreBlobStrategy(final NodeAccess nodeAccess,
                                final RepositoryManager repositoryManager,
                                final BlobStoreManager blobStoreManager,
                                final DryRunPrefix dryRunPrefix,
                                final NpmRepairPackageRootComponent npmRepairPackageRootComponent)
  {
    super(nodeAccess, repositoryManager, blobStoreManager, dryRunPrefix);
    this.npmRepairPackageRootComponent = checkNotNull(npmRepairPackageRootComponent);
  }

  @Override
  protected NpmRestoreBlobData createRestoreData(final RestoreBlobData blobData) {
    return NpmRestoreBlobDataFactory.create(blobData);
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final NpmRestoreBlobData data) {
    Repository repository = data.getBlobData().getRepository();
    Optional<NpmFacet> npmFacet = repository.optionalFacet(NpmFacet.class);

    if (!npmFacet.isPresent()) {
      log.warn("Skipping as NPM Facet not found on repository: {}", repository.getName());
      return false;
    }

    return true;
  }

  @Override
  protected String getAssetPath(@Nonnull final NpmRestoreBlobData data) {
    return data.getBlobData().getBlobName();
  }

  @TransactionalTouchBlob
  @Override
  protected boolean assetExists(@Nonnull final NpmRestoreBlobData data) {
    NpmFacet facet = data.getBlobData().getRepository().facet(NpmFacet.class);
    Asset asset;

    switch (data.getType()) {
      case REPOSITORY_ROOT:
        asset = facet.findRepositoryRootAsset();
        break;
      case TARBALL:
        asset = facet.findTarballAsset(data.getPackageId(), data.getTarballName());
        break;
      case PACKAGE_ROOT:
        asset = facet.findPackageRootAsset(data.getPackageId());
        break;
      default: // all the cases are covered
        throw new IllegalStateException("Unexpected case encountered");
    }

    return asset != null;
  }

  @TransactionalStoreMetadata
  @Override
  protected void createAssetFromBlob(@Nonnull final AssetBlob assetBlob, @Nonnull final NpmRestoreBlobData data)
      throws IOException
  {
    NpmFacet facet = data.getBlobData().getRepository().facet(NpmFacet.class);

    switch (data.getType()) {
      case REPOSITORY_ROOT:
        facet.putRepositoryRoot(assetBlob, null);
        break;
      case TARBALL:
        facet.putTarball(data.getPackageId(), data.getTarballName(), assetBlob, null);
        break;
      case PACKAGE_ROOT:
        facet.putPackageRoot(data.getPackageId(), assetBlob, null);
        break;
      default: // all the cases are covered
        throw new IllegalStateException("Unexpected case encountered");
    }
  }

  protected boolean componentRequired(final NpmRestoreBlobData data) throws IOException {
    return data.getType() == NpmType.TARBALL;
  }

  protected Query getComponentQuery(final NpmRestoreBlobData data) {
    String version = extractVersion(data.getTarballName());

    String scope = getPackageIdScope(data.getPackageId());

    Builder builder = Query.builder().where(P_NAME).eq(getPackageIdName(data.getPackageId())).and(P_GROUP);

    builder = scope != null ? builder.eq(scope) : builder.isNull();

    return builder.and(P_VERSION).eq(version).build();
  }

  @Override
  protected boolean shouldDeleteAsset(final NpmRestoreBlobData restoreData,
                                      final RestoreBlobData blobData,
                                      final String path) throws IOException
  {
    NpmType type = restoreData.getType();
    return type == NpmType.REPOSITORY_ROOT || type == NpmType.PACKAGE_ROOT ||
        super.shouldDeleteAsset(restoreData, blobData, path);
  }

  protected Repository getRepository(@Nonnull final NpmRestoreBlobData data) {
    return data.getBlobData().getRepository();
  }

  @Override
  public void after(final boolean updateAssets, final Repository repository) {
    if (updateAssets) {
      npmRepairPackageRootComponent.repairRepository(repository);
    }
    else {
      log.info("Updating assets disabled so not running repair of npm package metadata");
    }
  }
}
