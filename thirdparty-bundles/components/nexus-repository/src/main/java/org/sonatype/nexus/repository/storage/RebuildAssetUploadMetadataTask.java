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
package org.sonatype.nexus.repository.storage;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndexCursor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;

/**
 * Fix asset metadata, based on information from the associated blob.
 *
 * @since 3.6.1
 */
@Named
public class RebuildAssetUploadMetadataTask
    extends TaskSupport
{
  private final AssetStore assetStore;

  private final BlobStoreManager blobStoreManager;

  private final int limit;

  @Inject
  public RebuildAssetUploadMetadataTask(final AssetStore assetStore,
                                        final BlobStoreManager blobStoreManager,
                                        final RebuildAssetUploadMetadataConfiguration configuration)
  {
    this.assetStore = checkNotNull(assetStore);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.limit = checkNotNull(configuration).getPageSize();
  }

  @Override
  public String getMessage() {
    return "Rebuild asset upload metadata";
  }

  @Override
  protected Object execute() {
    long totalAssets = assetStore.countAssets(null);
    long processedAssets = 0;

    OIndexCursor assetCursor = assetStore.getIndex(AssetEntityAdapter.I_BUCKET_COMPONENT_NAME).cursor();
    ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
    List<Entry<OCompositeKey, EntityId>> assets = assetStore.getNextPage(assetCursor, limit);

    if (!Iterables.isEmpty(assets) && !Strings2.isBlank(assetStore.getById(assets.get(0).getValue()).createdBy())) {
      return null;
    }

    while (assets != null && !assets.isEmpty()) {
      checkContinuation();

      Iterable<EntityId> assetIds = assets.stream().map(Entry::getValue).collect(toList());

      Collection<Asset> assetsToUpdate = stream(assetStore.getByIds(assetIds))
          .filter(asset -> Strings2.isEmpty(asset.createdBy()))
          .filter(asset -> asset.blobRef() != null).map(asset -> {
            BlobStore blobStore = blobStoreManager.get(asset.blobRef().getStore());
            Blob blob = blobStore.get(asset.blobRef().getBlobId());
            if (blob != null) {
              asset.createdBy(blob.getHeaders().get(BlobStore.CREATED_BY_HEADER));
              asset.createdByIp(blob.getHeaders().get(BlobStore.CREATED_BY_IP_HEADER));
              asset.blobCreated(blob.getMetrics().getCreationTime());
            }

            return asset;
          }).collect(toList());

      assetStore.save(assetsToUpdate);

      processedAssets += size(assetsToUpdate);
      progressLogger.info("{} / {} asset upload metadata processed in {} ms", processedAssets, totalAssets, progressLogger.getElapsed());

      assets = assetStore.getNextPage(assetCursor, limit);
    }

    progressLogger.flush();

    return null;
  }

  private void checkContinuation() {
    if (isCanceled()) {
      throw new TaskInterruptedException("Rebuilding asset upload metadata was cancelled", true);
    }
  }
}
