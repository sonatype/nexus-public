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
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStoreMigrationStateProvider;
import org.sonatype.nexus.blobstore.api.HeavyBlobRef;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.stateguard.Guarded;

import com.google.common.base.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  // nexus.blobstore.cloud.existenceCheck.enabled: lets support skip the per-read existence check
  // without a patch release, independent of loadFromDb (which also disables the unrelated
  // DB-metadata fast path). The check still runs while a repository move is active, because the
  // move relies on get() returning null to fall back to the blob's old location. Defaults to true
  // so a store built directly (e.g. in a test) keeps the check.
  private volatile boolean existenceCheckEnabled = true;

  // The existence check runs on every read, so a persistent backend error (throttling, expired
  // credentials, timeouts) would otherwise log one WARN per download.
  private final RateLimitedWarn existenceCheckWarnings =
      new RateLimitedWarn(log, TimeUnit.MINUTES.toNanos(1), Ticker.systemTicker());

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

  @Autowired
  public void setExistenceCheckEnabled(
      @Value("${nexus.blobstore.cloud.existenceCheck.enabled:true}") final boolean enabled)
  {
    this.existenceCheckEnabled = enabled;
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
    if (shouldCheckExistence() && !timedBlobExistsInStorage(blob.getId())) {
      log.debug("Blob {} not found in cloud storage, returning null to allow fallback", blob.getId());
      return null;
    }

    blob.refresh(Collections.emptyMap(), ((HeavyBlobRef) blobRef).getMetrics());
    stopWatch.stop();
    log.debug("elapsed time for Loading blob from DB {}: {} ms", blobRef.getBlobId(),
        stopWatch.getTotalTime(MILLISECONDS));
    return blob;
  }

  /**
   * The check runs on every read unless disabled by {@code nexus.blobstore.cloud.existenceCheck.enabled=false}, and
   * even then while a repository move is active. With it off, a genuinely missing blob is only detected later, when
   * the content is opened.
   */
  private boolean shouldCheckExistence() {
    return existenceCheckEnabled || (migrationStateProvider != null && migrationStateProvider.isMigrationActive());
  }

  /**
   * Times the existence check, so what it costs can be read from the {@code blobExistsInStorage} timer of each store
   * type (for example {@code org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.blobExistsInStorage.timer}).
   */
  private boolean timedBlobExistsInStorage(final BlobId blobId) {
    long start = System.nanoTime();
    try {
      return blobExistsInStorage(blobId);
    }
    finally {
      updateTimer("blobExistsInStorage", System.nanoTime() - start);
    }
  }

  /**
   * Used to create {@link Blob} instances for the BlobStore, it will be used when bypassing storage for serving assets.
   */
  protected abstract BlobSupport initializeBlob(BlobId blobId);

  /**
   * Checks if a blob physically exists in cloud storage.
   * Called from {@link #get(BlobRef)} for every {@code HeavyBlobRef} read, not just during an active
   * "Change repository blob store" task.
   *
   * @param blobId the blob ID to check
   * @return true if the blob exists in cloud storage, false otherwise
   */
  protected abstract boolean blobExistsInStorage(BlobId blobId);

  /**
   * Logs a failed existence check at WARN at most once a minute per blob store; the rest go to DEBUG with the
   * full exception.
   */
  protected void warnExistenceCheckFailed(final BlobId blobId, final String backend, final Exception e) {
    existenceCheckWarnings.warn(e, "Error checking blob existence for {} in {}: {}", blobId, backend,
        describeWithCause(e));
  }

  /**
   * Formats an exception for a log line without the full stack trace, but without losing a
   * wrapped cause either (plain {@code Throwable#toString()} only shows the outer exception,
   * which can hide the real error behind a backend SDK's own wrapper exception type).
   */
  private static String describeWithCause(final Throwable t) {
    Throwable cause = t.getCause();
    return cause == null || cause == t ? t.toString() : t + " (caused by: " + cause + ")";
  }

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
