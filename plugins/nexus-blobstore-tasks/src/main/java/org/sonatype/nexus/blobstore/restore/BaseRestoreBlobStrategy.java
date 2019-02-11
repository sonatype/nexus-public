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
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.common.hash.Hashes.hash;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

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
        if (shouldDeleteAsset(restoreData, blobData, path)) {
          log.debug(
              "Deleting asset as component is required but is not found, blob store: {}, repository: {}, path: {}, "
              + "blob name: {}, blob id: {}", blobStoreName, repoName, path, blobName, blob.getId());
          deleteAsset(blobData.getRepository(), path);
        }
        else {
          log.debug(
              "Skipping as asset already exists, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
              blobStoreName, repoName, path, blobName, blob.getId());
          return;
        }
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

  @TransactionalDeleteBlob
  protected void deleteAsset(final Repository repository, final String assetName) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = findAsset(repository, assetName);
    if (asset != null) {
      tx.deleteAsset(asset, false);
    }
  }

  /**
   * In cases when performing restore and a component has been deleted, it is possible
   * for existing assets to become orphaned during the restore process. In the context
   * of the restore process, this method determines if an asset is associated with the
   * component found (using coordinates from the restored data) using the component's entity id.
   */
  private boolean assetIsAssociatedWithComponent(final T data,
                                                 final Repository repository,
                                                 final String name) throws IOException
  {
    Optional<EntityId> componentEntityId = componentEntityId(data);
    return componentEntityId.isPresent() && componentEntityId.equals(assetComponentEntityId(repository, name));
  }

  @Transactional
  protected Asset findAsset(final Repository repository, final String name) {
    final StorageTx tx = UnitOfWork.currentTx();
    return tx.findAssetWithProperty(P_NAME, name, tx.findBucket(repository));
  }

  @Transactional
  protected Optional<Component> findComponent(final T data) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    return Optional.ofNullable(getComponentQuery(data))
        .map(q -> Iterables.getFirst(tx.findComponents(q, singletonList(getRepository(data))), null));
  }

  /**
   * Return a list of hashes to be calculated for the blob (defaulted to SHA1)
   */
  @Nonnull
  protected List<HashAlgorithm> getHashAlgorithms() {
    return newArrayList(HashAlgorithm.SHA1);
  }

  protected boolean shouldDeleteAsset(final T restoreData,
                                    final RestoreBlobData blobData,
                                    final String path) throws IOException
  {
    return componentRequired(restoreData)
        && !assetIsAssociatedWithComponent(restoreData, blobData.getRepository(), path);
  }

  private Optional<EntityId> assetComponentEntityId(final Repository repository, final String name) {
    return Optional.ofNullable(findAsset(repository, name))
        .map(Asset::componentId);
  }

  private Optional<EntityId> componentEntityId(final T data) throws IOException {
    return findComponent(data)
        .map(Component::getEntityMetadata)
        .map(EntityMetadata::getId);
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

  // ---- BEGIN ABSTRACT METHODS ---
  // These should probably be made abstract when done. Temporarily provided
  // 'default' (not to be confused with correct) implementation so the builds
  // and tests could complete without touching all implementing classes
  /**
   * Determines if a component is required for the asset associated with the
   * provided restore data.
   *
   * Defaults to false which effectively results in the logic in {@link #doRestore}
   * to proceed as before the introduction of additional checks for restoring the
   * when only components are missing.
   *
   * https://issues.sonatype.org/browse/NEXUS-18350
   */
  protected boolean componentRequired(final T data) throws IOException {
    return false;
  }

  /**
   * Provides a default query for retrieving a component based on the provided
   * restore data.
   *
   * NOTE: All logic using this method will be bypassed
   * unless a format specific implementation of this base class overrides
   * {@link #componentRequired}.
   */
  protected Query getComponentQuery(final T data) throws IOException {
    return null;
  }

  /**
   * Return the repository for the provided restore data
   */
  protected Repository getRepository(@Nonnull final T data) {
    return null;
  }
 // -- END ABSTRACT METHODS ---
  /**
   * Create the metadata asset
   */
  protected abstract void createAssetFromBlob(@Nonnull final AssetBlob assetBlob, @Nonnull final T data)
      throws IOException;

  @Override
  public void after(final boolean updateAssets, final Repository repository) {
    //no-op
  }
}
