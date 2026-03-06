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
package org.sonatype.nexus.blobstore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStoreMigrationStateProvider;
import org.sonatype.nexus.blobstore.api.HeavyBlobRef;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.stateguard.Guarded;

import org.springframework.util.StopWatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Support class for Cloud blob stores.
 *
 * @see CloudBlobPropertiesSupport
 * @since 3.37
 */
public abstract class CloudBlobStoreSupport<T extends AttributesLocation>
    extends BlobStoreSupport<T>
{
  protected final boolean loadFromDb;

  protected final BlobStoreMigrationStateProvider migrationStateProvider;

  protected CloudBlobStoreSupport(
      final BlobIdLocationResolver blobIdLocationResolver,
      final DryRunPrefix dryRunPrefix,
      final boolean loadFromDb,
      @Nullable final BlobStoreMigrationStateProvider migrationStateProvider)
  {
    super(blobIdLocationResolver, dryRunPrefix);
    this.loadFromDb = loadFromDb;
    this.migrationStateProvider = migrationStateProvider;
  }

  /**
   * Optimized get method for cloud blob stores that can load blob metadata from database.
   * When a HeavyBlobRef is provided and loadFromDb is enabled, this method skips reading
   * the blob properties file from cloud storage and instead uses the metadata from the database.
   *
   * @param blobRef the blob reference, potentially containing cached metadata
   * @return the blob, or null if not found
   */
  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobRef blobRef) {
    if (!(blobRef instanceof HeavyBlobRef) || !loadFromDb) {
      return get(blobRef.getBlobId());
    }
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("Loading blob from DB " + blobRef.getBlobId());
    BlobSupport blob = initializeBlob(blobRef.getBlobId());

    // Check if blob physically exists in cloud storage before returning
    // This check is ONLY performed during active migrations to allow the fallback mechanism to work (NEXUS-50554)
    // Outside of migrations, we skip this check to avoid the performance overhead of an extra cloud API call
    if (migrationStateProvider != null && migrationStateProvider.isMigrationActive()) {
      if (!blobExistsInStorage(blob.getId())) {
        log.debug("Blob {} not found in cloud storage during migration, returning null to allow fallback",
            blob.getId());
        return null;
      }
    }

    blob.refresh(Collections.emptyMap(), ((HeavyBlobRef) blobRef).getMetrics());
    stopWatch.stop();
    log.debug("elapsed time for Loading blob from DB {}: {} ms", blobRef.getBlobId(),
        stopWatch.getTotalTime(MILLISECONDS));
    return blob;
  }

  /**
   * Used to create {@link Blob} instances for the BlobStore, it will be used when bypassing storage for serving assets.
   */
  protected abstract BlobSupport initializeBlob(BlobId blobId);

  /**
   * Checks if a blob physically exists in cloud storage.
   * Used during blob store migration to determine if fallback to source blob store is needed.
   *
   * @param blobId the blob ID to check
   * @return true if the blob exists in cloud storage, false otherwise
   */
  protected abstract boolean blobExistsInStorage(BlobId blobId);

  protected abstract Blob writeBlobProperties(BlobId blobId, Map<String, String> headers);

  @Override
  protected BlobId getBlobId(final Map<String, String> headers, final BlobId assignedBlobId) {
    return super.getBlobId(removeTemporaryBlobHeaderIfPresent(headers), assignedBlobId);
  }

  @Override
  public final Blob makeBlobPermanent(final Blob blob, final Map<String, String> headers) {
    if (headers.containsKey(TEMPORARY_BLOB_HEADER)) {
      throw new IllegalArgumentException(
          String.format("Permanent blob headers must not contain entry with '%s' key.", TEMPORARY_BLOB_HEADER));
    }

    BlobId blobId = blob.getId();

    return Optional.ofNullable(get(blobId))
        .map(Blob::getHeaders)
        .filter(blobHeaders -> blobHeaders.containsKey(TEMPORARY_BLOB_HEADER))
        .map(__ -> {
          attachExternalMetadata(blobId, headers);
          return writeBlobProperties(blobId, headers);
        })
        // We were given a blob that was already made permanent, so we need to copy it instead.
        .orElseGet(() -> super.makeBlobPermanent(blob, headers));
  }

  /**
   * Attaches external metadata from the cloud blobstore and attach it to the blob headers.
   *
   * @param blobId the existing blob id
   * @param headers the existing blob headers
   */
  protected void attachExternalMetadata(final BlobId blobId, final Map<String, String> headers) {
    getExternalMetadata(blobId).ifPresent(externalMetadata -> {
      headers.put(EXTERNAL_ETAG_HEADER, externalMetadata.etag());
      headers.put(EXTERNAL_LAST_MODIFIED_HEADER, externalMetadata.lastModified().toString());
    });
  }

  @Override
  public boolean deleteIfTemp(final Blob blob) {
    if (isOwner(blob)) {
      Map<String, String> headers = blob.getHeaders();
      if (headers == null || headers.containsKey(TEMPORARY_BLOB_HEADER)) {
        return deleteHard(blob.getId());
      }
      log.atDebug()
          .addArgument(() -> blob.getId().asUniqueString())
          .log("Not deleting. Blob with id: {} is permanent.");
      return true;
    }
    return false;
  }

  public abstract Blob getBlobFromCache(final BlobId blobId);

  private Map<String, String> removeTemporaryBlobHeaderIfPresent(final Map<String, String> headers) {
    Map<String, String> headersCopy = headers;
    if (headersCopy.containsKey(TEMPORARY_BLOB_HEADER)) {
      headersCopy = new HashMap<>(headers);
      headersCopy.remove(TEMPORARY_BLOB_HEADER);
    }
    return headersCopy;
  }
}
