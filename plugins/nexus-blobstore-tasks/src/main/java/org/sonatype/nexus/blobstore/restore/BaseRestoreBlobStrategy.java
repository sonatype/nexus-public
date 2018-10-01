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
package org.sonatype.nexus.blobstore.restore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.common.hash.Hashes.hash;

/**
 * Provides the common logic for metadata restoration from a blob. Subclasses will implement the format-specific
 * restoration mechanisms
 *
 * @since 3.6.1
 */
public abstract class BaseRestoreBlobStrategy<T>
    extends ComponentSupport
    implements RestoreBlobStrategy
{
  private NodeAccess nodeAccess;

  private RepositoryManager repositoryManager;

  private BlobStoreManager blobStoreManager;

  private DryRunPrefix dryRunPrefix;

  public BaseRestoreBlobStrategy(final NodeAccess nodeAccess,
                                 final RepositoryManager repositoryManager,
                                 final BlobStoreManager blobStoreManager,
                                 final DryRunPrefix dryRunPrefix)
  {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.dryRunPrefix = checkNotNull(dryRunPrefix);
  }

  @Override
  public void restore(final Properties properties,
                      final Blob blob,
                      final String blobStoreName,
                      final boolean isDryRun)
  {
    RestoreBlobData blobData = new RestoreBlobData(blob, properties, blobStoreName, repositoryManager);
    Optional<StorageFacet> storageFacet = blobData.getRepository().optionalFacet(StorageFacet.class);
    T restoreData = createRestoreData(blobData);

    if (storageFacet.isPresent() && canAttemptRestore(restoreData)) {
      doRestore(storageFacet.get(), blobData, restoreData, isDryRun);
    }
    else {
      log.debug("Skipping asset, blob store: {}, repository: {}, blob name: {}, blob id: {}",
          blobStoreName, blobData.getRepository().getName(), blobData.getBlobName(), blob.getId());
    }
  }

  private void doRestore(StorageFacet storageFacet, RestoreBlobData blobData, T restoreData, boolean isDryRun) {
    String logPrefix = isDryRun ? dryRunPrefix.get() : "";
    String path = getAssetPath(restoreData);
    String blobStoreName = blobData.getBlobStoreName();
    String repoName = blobData.getRepository().getName();
    String blobName = blobData.getBlobName();
    Blob blob = blobData.getBlob();

    UnitOfWork.begin(storageFacet.txSupplier());
    try {
      if (assetExists(restoreData)) {
        log.debug(
            "Skipping as asset already exists, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
            blobStoreName, repoName, path, blobName, blob.getId());
        return;
      }

      if (!isDryRun) {
        doCreateAssetFromBlob(blobData, restoreData, blob);
      }

      log.info("{}Restored asset, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
          logPrefix, blobStoreName, repoName, path, blobName, blob.getId());
    }
    catch (Exception e) {
      log.error("Error while restoring asset: blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
          blobStoreName, repoName, path, blobName, blob.getId(), e);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @TransactionalStoreMetadata
  protected void doCreateAssetFromBlob(final RestoreBlobData blobData,
                                       final T restoreData,
                                       final Blob blob) throws IOException
  {
    List<HashAlgorithm> hashTypes = getHashAlgorithms();

    AssetBlob assetBlob = new AssetBlob(nodeAccess,
        blobStoreManager.get(blobData.getBlobStoreName()),
        blobStore -> blob,
        blobData.getProperty(HEADER_PREFIX + CONTENT_TYPE_HEADER),
        hash(hashTypes, blob.getInputStream()), true);

    createAssetFromBlob(assetBlob, restoreData);
  }

  /**
   * Return a list of hashes to be calculated for the blob (defaulted to SHA1)
   */
  @Nonnull
  protected List<HashAlgorithm> getHashAlgorithms() {
    return newArrayList(HashAlgorithm.SHA1);
  }

  /**
   * Create necessary data structure for the restore operation
   */
  protected abstract T createRestoreData(RestoreBlobData blobData);

  /**
   * Determines if metadata can be restored
   */
  protected abstract boolean canAttemptRestore(@Nonnull final T data);

  /**
   * Return the string representation of the asset path
   */
  protected abstract String getAssetPath(@Nonnull final T data);

  /**
   * Determine if the asset already exists
   */
  protected abstract boolean assetExists(@Nonnull final T data) throws IOException;

  /**
   * Create the metadata asset
   */
  protected abstract void createAssetFromBlob(@Nonnull final AssetBlob assetBlob, @Nonnull final T data)
      throws IOException;

  @Override
  public void after(final boolean updateAssets) {
    //no-op    
  }
}
