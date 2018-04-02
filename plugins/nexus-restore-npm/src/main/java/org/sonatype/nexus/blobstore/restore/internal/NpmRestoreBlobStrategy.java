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

import org.sonatype.nexus.repository.npm.NpmFacet;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.BaseRestoreBlobStrategy;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;

/**
 * @since 3.6.1
 */
@Named("npm")
@Singleton
public class NpmRestoreBlobStrategy
    extends BaseRestoreBlobStrategy<NpmRestoreBlobData>
{
  @Inject
  public NpmRestoreBlobStrategy(final NodeAccess nodeAccess,
                                final RepositoryManager repositoryManager,
                                final BlobStoreManager blobStoreManager,
                                final DryRunPrefix dryRunPrefix)
  {
    super(nodeAccess, repositoryManager, blobStoreManager, dryRunPrefix);
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
}
