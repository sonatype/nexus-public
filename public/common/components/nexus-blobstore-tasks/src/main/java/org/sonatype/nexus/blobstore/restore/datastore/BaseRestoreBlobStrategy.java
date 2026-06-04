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
package org.sonatype.nexus.blobstore.restore.datastore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.handlers.LastDownloadedAttributeHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

/**
 * Provides the common logic for metadata restoration from a blob. Subclasses will implement the format-specific
 * restoration mechanisms
 *
 * @since 3.29
 */
public abstract class BaseRestoreBlobStrategy<T extends DataStoreRestoreBlobData>
    implements RestoreBlobStrategy
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String ASSET_PATH_PREFIX = "/";

  private final DryRunPrefix dryRunPrefix;

  private LastDownloadedAttributeHandler lastDownloadedAttributeHandler;

  protected BaseRestoreBlobStrategy(final DryRunPrefix dryRunPrefix) {
    this.dryRunPrefix = checkNotNull(dryRunPrefix);
  }

  @Autowired
  public void injectDependencies(final LastDownloadedAttributeHandler lastDownloadedAttributeHandler) {
    this.lastDownloadedAttributeHandler = checkNotNull(lastDownloadedAttributeHandler);
  }

  @Override
  public void restore(final Properties properties, final Blob blob, final BlobStore blobStore, final boolean isDryRun) {
    String logPrefix = isDryRun ? dryRunPrefix.get() : "";

    T restoreData = createRestoreData(properties, blob, blobStore);

    String repoName = restoreData.getRepository().getName();
    String blobName = restoreData.getBlobName();
    String blobStoreName = blobStore.getBlobStoreConfiguration().getName();

    if (!canAttemptRestore(restoreData)) {
      log.info("Skipping asset for blob store: {}, repository: {}, blob name: {}, blob id: {}", blobStoreName,
          repoName, blobName, blob.getId());
      return;
    }

    if (isDeleted(restoreData, blobStore)) {
      log.info("Skipping soft-deleted asset for blob store: {}, repository: {}, blob name: {}, blob id: {}",
          blobStoreName,
          repoName, blobName, blob.getId());
      return;
    }

    String assetPath = prependIfMissing(getAssetPath(restoreData), ASSET_PATH_PREFIX);

    try {
      ContentFacet contentFacet = restoreData.getRepository().facet(ContentFacet.class);
      String sourceBlobStoreName = resolveSourceBlobStoreName(restoreData.getRepository());
      OffsetDateTime lastDownloadedAttribute =
          lastDownloadedAttributeHandler.readLastDownloadedAttribute(blobStoreName, blob);
      if (lastDownloadedAttribute != null) {
        restoreData.setLastDownloaded(lastDownloadedAttribute);
      }

      Optional<FluentAsset> asset = contentFacet.assets().path(assetPath).find();
      if (asset.isPresent() && !shouldRestoreAsset(restoreData, asset.get(), blobStoreName, sourceBlobStoreName,
          contentFacet, logPrefix, repoName, blobName, isDryRun)) {
        return;
      }

      if (!isDryRun) {
        BlobId originalBlobId = blob.getId();
        createAssetFromData(restoreData);

        // Check if blob was re-ingested into a different blobstore
        Optional<FluentAsset> createdAsset = contentFacet.assets().path(assetPath).find();
        if (createdAsset.isPresent()) {
          FluentAsset fluentAsset = createdAsset.get();
          // try to apply lastDownloaded field to created asset
          if (restoreData.hasLastDownloaded()) {
            fluentAsset.lastDownloaded(restoreData.getLastDownloaded());
          }
          // Check if the asset's blob differs from the original - indicates re-ingest into correct blobstore
          softDeleteOriginalBlobIfReIngested(fluentAsset, originalBlobId, blobStore, sourceBlobStoreName, assetPath);
        }
      }

      log.info("{} Restored asset, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
          logPrefix, blobStoreName, repoName, assetPath, blobName, blob.getId());
    }
    catch (IOException ex) {
      log.debug("Error while restoring asset: blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
          blobStoreName, repoName, assetPath, blobName, blob.getId(), ex);
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Extracted to method for testability and clarity.
   */
  protected String resolveSourceBlobStoreName(final Repository repository) {
    return repository.getConfiguration()
        .getAttributes()
        .get(STORAGE)
        .get(BLOB_STORE_NAME)
        .toString();
  }

  /**
   * Determines if an existing asset should be restored based on:
   * 1. Whether the asset is orphaned
   * 2. Whether the blob needs to be re-ingested into the correct blobstore
   * 3. Whether the existing blob is newer than the one being restored
   *
   * @return true if restoration should proceed, false if we should skip
   */
  protected boolean shouldRestoreAsset(
      final T restoreData,
      final FluentAsset asset,
      final String blobStoreName,
      final String sourceBlobStoreName,
      final ContentFacet contentFacet,
      final String logPrefix,
      final String repoName,
      final String blobName,
      final boolean isDryRun)
  {
    asset.lastDownloaded().ifPresent(restoreData::setLastDownloaded);
    // Check if the asset has a blob that exists physically in the blobstore
    boolean assetHasPhysicalBlob = assetHasPhysicalBlob(asset, contentFacet);

    Blob blob = restoreData.getBlob();

    // Check if asset is orphaned - delete and restore
    if (shouldDeleteAsset(restoreData, asset)) {
      log.info(
          "{} Deleting asset as component is required but is not found, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
          logPrefix, blobStoreName, repoName, asset.path(), blobName, blob.getId());
      if (!isDryRun) {
        asset.delete();
      }
      return true;
    }

    // Check if existing asset has a newer or equal blob - skip only if blob exists physically
    if (!isRestoreDataMoreRecent(restoreData, asset)) {
      if (!assetHasPhysicalBlob) {
        // Asset has no physical blob - this means the blob was never properly attached
        // or was deleted. We should proceed with restoration, not skip.
        log.debug(
            "{} Asset has no physical blob, proceeding with restoration: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
            logPrefix, blobStoreName, repoName, asset.path(), blobName, blob.getId());
      }
      else {
        // Asset has a physical blob that is newer/equal - skip
        log.info(
            "Skipping as asset already exists with newer/equal blob, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
            blobStoreName, repoName, asset.path(), blobName, blob.getId());
        return false;
      }
    }

    // Check if blob is in the wrong blobstore - re-ingest regardless of timestamp
    if (!blobStoreName.equals(sourceBlobStoreName)) {
      log.info(
          "{} Deleting asset to re-ingest blob from wrong blobstore '{}' into correct blobstore '{}', repository: {}, path: {}, blob name: {}, blob id: {}",
          logPrefix, blobStoreName, sourceBlobStoreName, repoName, asset.path(), blobName, blob.getId());
      if (!isDryRun) {
        asset.delete();
      }
      return true;
    }

    // Asset has older blob - delete and restore
    log.info(
        "{} Deleting asset to restore more recent blob, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
        logPrefix, blobStoreName, repoName, asset.path(), blobName, blob.getId());
    if (!isDryRun) {
      asset.delete();
    }
    return true;
  }

  /**
   * Determines if metadata can be restored
   */
  protected abstract boolean canAttemptRestore(@Nonnull final T data);

  /**
   * Create the metadata asset
   */
  protected abstract void createAssetFromData(final T data) throws IOException;

  protected boolean shouldDeleteAsset(
      final T restoreData,
      final FluentAsset asset)
  {
    return isComponentRequired(restoreData)
        && isOrphanedAsset(restoreData, asset);
  }

  /**
   * Verifies if the asset has a blob that exists physically in the blobstore.
   * This check is needed to distinguish between:
   * - Asset has no blob reference (should proceed with restoration)
   * - Asset has a blob reference but it doesn't exist physically (should proceed with restoration)
   * - Asset has a blob reference that exists physically and is newer/equal (should skip)
   */
  private boolean assetHasPhysicalBlob(final FluentAsset asset, final ContentFacet contentFacet) {
    return asset
        .blob()
        .map(AssetBlob::blobRef)
        .map(blobRef -> {
          try {
            // get() with includeDeleted=true to include soft-deleted blobs
            return contentFacet.blobs().blob(blobRef).isPresent();
          }
          catch (Exception e) {
            log.debug("Error verifying blob {} exists in blobstore, assuming not present",
                blobRef.getBlobId(), e);
            return false;
          }
        })
        .orElse(false);
  }

  /**
   * Whether the restoreData's blob was created more recent than the asset's blob
   */
  protected boolean isRestoreDataMoreRecent(final T restoreData, final FluentAsset asset) {
    return asset
        .blob()
        .map(AssetBlob::blobCreated)
        .map(blobCreated -> {
          DateTime dateTime = restoreData.getBlob().getMetrics().getCreationTime();
          Instant instant = Instant.ofEpochMilli(dateTime.getMillis());
          OffsetDateTime restoredBlob = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
          return blobCreated.isBefore(restoredBlob);
        })
        .orElse(false);
  }

  /**
   * Whether restoreData's blob is marked as deleted (or blob attributes are missing)
   */
  protected boolean isDeleted(final T restoreData, final BlobStore blobStore) {
    BlobId blobId = restoreData.getBlob().getId();
    BlobAttributes blobAttributes = blobStore.getBlobAttributes(blobId);
    if (blobAttributes != null) {
      return blobAttributes.isDeleted();
    }
    return true;
  }

  /**
   * In cases when performing restore and a component has been deleted, it is possible for existing assets to become
   * orphaned during the restore process. In the context of the restore process, this method determines if an asset is
   * associated with the component found (using coordinates from the restored data) using the component's entity id.
   */
  private boolean isOrphanedAsset(
      final T data,
      final FluentAsset asset)
  {
    return !asset.component().isPresent();
  }

  /**
   * Return the string representation of the asset path
   */
  protected abstract String getAssetPath(@Nonnull final T data);

  /**
   * Create necessary data structure for the restore operation
   */
  protected abstract T createRestoreData(final Properties properties, final Blob blob, final BlobStore blobStore);

  /**
   * Determines if a component is required for the asset associated with the provided restore data.
   * https://issues.sonatype.org/browse/NEXUS-18350
   */
  protected abstract boolean isComponentRequired(final T data);

  /**
   * Soft-deletes the original blob if it was re-ingested into a different blobstore.
   * This happens when reconcile finds a blob in the wrong blobstore (e.g., after a partial repository move).
   */
  private void softDeleteOriginalBlobIfReIngested(
      final FluentAsset asset,
      final BlobId originalBlobId,
      final BlobStore sourceBlobStore,
      final String sourceBlobStoreName,
      final String assetPath)
  {
    asset.blob().ifPresent(assetBlob -> {
      BlobRef assetBlobRef;
      try {
        assetBlobRef = assetBlob.blobRef();
      }
      catch (UnsupportedOperationException e) {
        // blobRef() may not be supported by all blob implementations
        return;
      }
      if (assetBlobRef == null) {
        return;
      }
      // If the asset's blob is different from the original, the blob was re-ingested
      if (!originalBlobId.equals(assetBlobRef.getBlobId())) {
        log.info("Blob {} was re-ingested into blobstore '{}', soft-deleting original from '{}'",
            originalBlobId, assetBlobRef.getStore(), sourceBlobStoreName);
        // Mark the original blob as deleted
        BlobAttributes blobAttributes = sourceBlobStore.getBlobAttributes(originalBlobId);
        if (blobAttributes != null && !blobAttributes.isDeleted()) {
          sourceBlobStore.delete(originalBlobId, "Re-ingested into correct blobstore during reconcile");
        }
      }
    });
  }
}
