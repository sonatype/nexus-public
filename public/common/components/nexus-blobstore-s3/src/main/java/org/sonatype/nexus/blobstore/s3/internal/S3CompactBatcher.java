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
package org.sonatype.nexus.blobstore.s3.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Error;

/** Stateless bulk-delete helpers for {@link S3BlobStore#doCompactWithDeletedBlobIndexBatched}. */
final class S3CompactBatcher
{
  private static final Logger log = LoggerFactory.getLogger(S3CompactBatcher.class);

  // Backoff schedule for partial-failure retry (SlowDown inside 200 body — SDK adaptive can't see this).
  private static final long[] PARTIAL_RETRY_DELAYS_MS = {100L, 250L, 600L, 1500L, 3000L};

  // Per-attempt random jitter added to the backoff delay to desynchronise concurrent workers.
  private static final long PARTIAL_RETRY_JITTER_MS = 250L;

  // S3 transient error codes (safe to retry same keys — DeleteObject is idempotent).
  private static final String S3_ERR_SLOW_DOWN = "SlowDown";

  private static final String S3_ERR_SERVICE_UNAVAILABLE = "ServiceUnavailable";

  private static final String S3_ERR_INTERNAL_ERROR = "InternalError";

  private static final String S3_ERR_REQUEST_TIMEOUT = "RequestTimeout";

  // Object does not exist — treat as already deleted; safe to remove from soft-deleted index.
  private static final String S3_ERR_NO_SUCH_KEY = "NoSuchKey";

  private static final Set<String> RETRYABLE_ERROR_CODES =
      Set.of(S3_ERR_SLOW_DOWN, S3_ERR_SERVICE_UNAVAILABLE, S3_ERR_INTERNAL_ERROR, S3_ERR_REQUEST_TIMEOUT);

  private S3CompactBatcher() {
  }

  /** Keys for one logical blob plus its content size (captured at classify time for metric parity). */
  record BlobKey(BlobId blobId, String content, String attrs, String softDeletedAttrs, long contentSize)
  {
    BlobKey {
      Objects.requireNonNull(blobId, "blobId");
      Objects.requireNonNull(content, "content");
      Objects.requireNonNull(attrs, "attrs");
    }

    List<String> allKeys() {
      return softDeletedAttrs == null ? List.of(content, attrs) : List.of(content, attrs, softDeletedAttrs);
    }
  }

  /** Bulk-delete one batch; partial-failure-safe. Blobs with any failing key stay in the index. */
  static void flushBatch(
      final EncryptingS3Client s3,
      final String bucket,
      final SoftDeletedBlobIndex deletedBlobIndex,
      final List<BlobKey> chunk,
      final LongConsumer recordDeletion,
      final Consumer<BlobId> onSuccess)
  {
    if (chunk.isEmpty()) {
      return;
    }

    List<ObjectIdentifier> ids = new ArrayList<>(chunk.size() * 3);
    for (BlobKey bk : chunk) {
      for (String key : bk.allKeys()) {
        ids.add(ObjectIdentifier.builder().key(key).build());
      }
    }
    DeleteObjectsRequest req = DeleteObjectsRequest.builder()
        .bucket(bucket)
        .delete(Delete.builder().objects(ids).build())
        .build();

    int keyCount = ids.size();

    DeleteObjectsResponse resp;
    try {
      resp = s3.deleteObjects(req);
    }
    catch (Exception e) {
      // SDK exhausted retries; S3 may have partially processed the batch — remaining blobs retry next compact run.
      log.warn(
          "Compact: bulk DeleteObjects threw after SDK retries for batch of {} blobs ({} keys); "
              + "blobs remain in soft-deleted index for next compact. Cause: {}",
          chunk.size(), keyCount, e.getMessage(), e);
      return;
    }

    Set<String> failedKeys = new HashSet<>();
    if (resp.errors() != null && !resp.errors().isEmpty()) {
      // Split transient from permanent; transient keys retried in isolation, permanent ones stay for next compact.
      Set<String> permanentlyFailedKeys = new HashSet<>();
      List<String> retryableKeys = new ArrayList<>();
      for (S3Error err : resp.errors()) {
        if (RETRYABLE_ERROR_CODES.contains(err.code())) {
          retryableKeys.add(err.key());
        }
        else if (S3_ERR_NO_SUCH_KEY.equals(err.code())) {
          // Object already gone — treat as success; covers proxy optimistic records and prior partial runs.
          log.debug("Compact: key '{}' not found in S3 (NoSuchKey) — treating as already deleted", err.key());
        }
        else {
          permanentlyFailedKeys.add(err.key());
          log.debug("Compact: permanent S3 delete failure '{}': {} - {}",
              err.key(), err.code(), err.message());
        }
      }
      if (!permanentlyFailedKeys.isEmpty()) {
        S3Error first = resp.errors()
            .stream()
            .filter(e -> permanentlyFailedKeys.contains(e.key()))
            .findFirst()
            .orElse(resp.errors().get(0));
        log.warn("Compact: {} permanent DeleteObjects failure(s) of {} keys ({} blobs); first: '{}' {} - {}",
            permanentlyFailedKeys.size(), keyCount, chunk.size(),
            first.key(), first.code(), first.message());
      }
      failedKeys.addAll(permanentlyFailedKeys);

      if (!retryableKeys.isEmpty()) {
        log.info(
            "Compact: retrying {} throttled key(s) (of {} keys in batch, {} blobs) with backoff",
            retryableKeys.size(), keyCount, chunk.size());
        Set<String> stillFailed = retryThrottledKeys(s3, bucket, retryableKeys);
        failedKeys.addAll(stillFailed);
        if (!stillFailed.isEmpty()) {
          log.warn("Compact: {} key(s) still throttled after {} retries; staying in index for next compact",
              stillFailed.size(), PARTIAL_RETRY_DELAYS_MS.length);
        }
      }
    }

    // Collect blobs where every S3 key was deleted successfully.
    // Collection type required by SoftDeletedBlobIndex#deleteRecords (AJC weaver rejects List<BlobId>).
    Collection<BlobId> toRemove = new ArrayList<>(chunk.size());
    List<BlobKey> succeeded = new ArrayList<>(chunk.size());
    for (BlobKey bk : chunk) {
      if (bk.allKeys().stream().noneMatch(failedKeys::contains)) {
        toRemove.add(bk.blobId());
        succeeded.add(bk);
      }
    }

    if (!toRemove.isEmpty()) {
      try {
        deletedBlobIndex.deleteRecords(toRemove);
        // Fire callbacks only after DB removal confirmed.
        for (BlobKey bk : succeeded) {
          try {
            recordDeletion.accept(bk.contentSize());
          }
          catch (Exception e) {
            log.debug("Compact: recordDeletion threw for blob {}", bk.blobId(), e);
          }
          try {
            onSuccess.accept(bk.blobId());
          }
          catch (Exception e) {
            log.debug("Compact: onSuccess threw for {}", bk.blobId(), e);
          }
        }
      }
      catch (Exception e) {
        log.warn("Compact: bulk DB deleteRecords failed for {} blobs", toRemove.size(), e);
      }
    }
  }

  // Retry only throttled keys with exp backoff + jitter; blocks up to ~5.4s (intentional CallerRunsPolicy
  // backpressure).
  private static Set<String> retryThrottledKeys(
      final EncryptingS3Client s3,
      final String bucket,
      final List<String> throttledKeys)
  {
    List<String> pending = new ArrayList<>(throttledKeys);
    Set<String> stillFailed = new HashSet<>();
    for (int attempt = 0; attempt < PARTIAL_RETRY_DELAYS_MS.length && !pending.isEmpty(); attempt++) {
      long delay = PARTIAL_RETRY_DELAYS_MS[attempt] + ThreadLocalRandom.current().nextLong(PARTIAL_RETRY_JITTER_MS);
      try {
        Thread.sleep(delay);
      }
      catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        stillFailed.addAll(pending);
        return stillFailed;
      }

      List<ObjectIdentifier> retryIds = new ArrayList<>(pending.size());
      for (String key : pending) {
        retryIds.add(ObjectIdentifier.builder().key(key).build());
      }
      DeleteObjectsRequest retryReq = DeleteObjectsRequest.builder()
          .bucket(bucket)
          .delete(Delete.builder().objects(retryIds).build())
          .build();

      DeleteObjectsResponse retryResp;
      try {
        retryResp = s3.deleteObjects(retryReq);
      }
      catch (Exception e) {
        // Whole retry attempt threw — try again after next backoff, unless attempts exhausted.
        log.info("Compact: retry attempt {} threw for {} keys: {}", attempt + 1, pending.size(), e.getMessage(), e);
        continue;
      }

      List<String> nextPending = new ArrayList<>();
      int newPermanent = 0;
      S3Error firstNewPermanent = null;
      if (retryResp.errors() != null) {
        for (S3Error err : retryResp.errors()) {
          if (RETRYABLE_ERROR_CODES.contains(err.code())) {
            nextPending.add(err.key());
          }
          else {
            stillFailed.add(err.key());
            newPermanent++;
            if (firstNewPermanent == null) {
              firstNewPermanent = err;
            }
            log.debug("Compact: retry converted transient to permanent for '{}': {} - {}",
                err.key(), err.code(), err.message());
          }
        }
      }
      if (newPermanent > 0) {
        log.warn("Compact: {} key(s) became permanent failures on retry attempt {}; first: '{}' {} - {}",
            newPermanent, attempt + 1,
            firstNewPermanent.key(), firstNewPermanent.code(), firstNewPermanent.message());
      }
      pending = nextPending;
    }
    stillFailed.addAll(pending);
    return stillFailed;
  }

  // Groups by 6th '/': hour-level with a bucket prefix, minute-level without. Fewer than 6 slashes
  // (vol-chapter layout) falls back to 3rd '/' so Phase 3 workers stay busy on the default layout.
  static String hourPrefix(final String key) {
    if (key == null) {
      return "(null)";
    }
    int thirdSlash = -1;
    int slashCount = 0;
    for (int i = 0; i < key.length(); i++) {
      if (key.charAt(i) == '/') {
        slashCount++;
        if (slashCount == 3) {
          thirdSlash = i;
        }
        if (slashCount == 6) {
          return key.substring(0, i);
        }
      }
    }
    return thirdSlash >= 0 ? key.substring(0, thirdSlash) : key;
  }
}
