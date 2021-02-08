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
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

/**
 * Provides the common logic for metadata restoration from a blob. Subclasses will implement the format-specific
 * restoration mechanisms
 *
 * @since 3.29
 */
public abstract class BaseRestoreBlobStrategy<T extends RestoreBlobData>
    extends ComponentSupport
    implements RestoreBlobStrategy
{
  private static final String ASSET_PATH_PREFIX = "/";

  private final DryRunPrefix dryRunPrefix;

  protected BaseRestoreBlobStrategy(final DryRunPrefix dryRunPrefix)
  {
    this.dryRunPrefix = checkNotNull(dryRunPrefix);
  }

  @Override
  public void restore(final Properties properties, final Blob blob, final BlobStore blobStore, final boolean isDryRun)
  {
    String logPrefix = isDryRun ? dryRunPrefix.get() : "";

    T restoreData = createRestoreData(properties, blob, blobStore);

    String repoName = restoreData.getRepository().getName();
    String blobName = restoreData.getBlobName();
    String blobStoreName = blobStore.getBlobStoreConfiguration().getName();

    if (!canAttemptRestore(restoreData)) {
      log.debug("Skipping asset for blob store: {}, repository: {}, blob name: {}, blob id: {}", blobStoreName,
          repoName, blobName, blob.getId());
      return;
    }

    String assetPath = prependIfMissing(getAssetPath(restoreData), ASSET_PATH_PREFIX);

    try {
      Optional<FluentAsset> asset =
          restoreData.getRepository().facet(ContentFacet.class).assets().path(assetPath).find();

      if (asset.isPresent()) {
        FluentAsset fluentAsset = asset.get();
        if (shouldDeleteAsset(restoreData, fluentAsset)) {
          log.debug(
              "{} Deleting asset as component is required but is not found, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
              dryRunPrefix.get(), blobStoreName, repoName, fluentAsset.path(), blobName, blob.getId());
          if (!isDryRun) {
            fluentAsset.delete();
          }
        }
        else {
          log.debug(
              "Skipping as asset already exists, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
              blobStoreName, repoName, fluentAsset.path(), blobName, blob.getId());
          return;
        }
      }

      if (!isDryRun) {
        createAssetFromBlob(blob, restoreData);
      }

      log.info("{} Restored asset, blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
          logPrefix, blobStoreName, repoName, assetPath, blobName, blob.getId());
    }
    catch (Exception ex) {
      log.error("Error while restoring asset: blob store: {}, repository: {}, path: {}, blob name: {}, blob id: {}",
          blobStoreName, repoName, assetPath, blobName, blob.getId(), ex);
    }
  }

  /**
   * Determines if metadata can be restored
   */
  protected abstract boolean canAttemptRestore(@Nonnull final T data);

  /**
   * Create the metadata asset
   */
  protected abstract void createAssetFromBlob(final Blob assetBlob, final T data)
      throws IOException;

  protected boolean shouldDeleteAsset(
      final T restoreData,
      final FluentAsset asset)
  {
    return isComponentRequired(restoreData)
        && isOrphanedAsset(restoreData, asset);
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
}
