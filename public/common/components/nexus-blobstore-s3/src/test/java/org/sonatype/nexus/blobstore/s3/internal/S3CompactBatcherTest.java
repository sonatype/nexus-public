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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobIndex;
import org.sonatype.nexus.blobstore.s3.internal.S3CompactBatcher.BlobKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class S3CompactBatcherTest
{
  private static final LongConsumer NOOP_LONG = bytes -> {
  };

  private static final Consumer<BlobId> NOOP_ON_SUCCESS = id -> {
  };

  private EncryptingS3Client s3;

  private SoftDeletedBlobIndex index;

  @BeforeEach
  void setUp() {
    s3 = mock(EncryptingS3Client.class);
    index = mock(SoftDeletedBlobIndex.class);
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder().build());
  }

  @Test
  void emptyChunkIsNoOp() {
    S3CompactBatcher.flushBatch(s3, "bucket", index, List.of(), NOOP_LONG, NOOP_ON_SUCCESS);
    verify(s3, never()).deleteObjects(any(DeleteObjectsRequest.class));
    verify(index, never()).deleteRecords(any());
  }

  @Test
  void happyPathDeletesAllKeysAndRemovesAllIndexRecords() {
    List<BlobKey> chunk = blobKeys(3, /* withSoftDel */ true);

    S3CompactBatcher.flushBatch(s3, "bucket", index, chunk, NOOP_LONG, NOOP_ON_SUCCESS);

    ArgumentCaptor<DeleteObjectsRequest> req = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
    verify(s3, times(1)).deleteObjects(req.capture());
    assertThat(req.getValue().bucket()).isEqualTo("bucket");
    assertThat(req.getValue().delete().objects()).hasSize(9); // 3 blobs × 3 keys

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<BlobId>> ids = ArgumentCaptor.forClass((Class) Collection.class);
    verify(index, times(1)).deleteRecords(ids.capture());
    assertThat(ids.getValue()).hasSize(3);
  }

  @Test
  void blobsWithNullSoftDeleteCopyEmitTwoKeys() {
    BlobKey twoKey = new BlobKey(new BlobId("b0"), "content/b0", "attrs/b0", null, 0L);

    S3CompactBatcher.flushBatch(s3, "bucket", index, List.of(twoKey), NOOP_LONG, NOOP_ON_SUCCESS);

    ArgumentCaptor<DeleteObjectsRequest> req = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
    verify(s3).deleteObjects(req.capture());
    assertThat(req.getValue().delete().objects()).hasSize(2);
  }

  @Test
  void partialFailureKeepsFailedBlobInIndex() {
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(S3Error.builder().key("content/b0").code("AccessDenied").message("denied").build())
            .build());

    List<BlobKey> chunk = blobKeys(2, true);
    S3CompactBatcher.flushBatch(s3, "bucket", index, chunk, NOOP_LONG, NOOP_ON_SUCCESS);

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<BlobId>> ids = ArgumentCaptor.forClass((Class) Collection.class);
    verify(index).deleteRecords(ids.capture());
    assertThat(ids.getValue()).extracting(BlobId::toString).containsExactly("b1");
  }

  @Test
  void wholeBatchS3ExceptionLeavesIndexUntouched() {
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenThrow(S3Exception.builder().message("ServiceUnavailable").build());

    S3CompactBatcher.flushBatch(s3, "bucket", index, blobKeys(1, false), NOOP_LONG, NOOP_ON_SUCCESS);

    verify(index, never()).deleteRecords(any());
  }

  @Test
  void dbExceptionDoesNotPropagate() {
    doThrow(new RuntimeException("db down")).when(index).deleteRecords(any());

    // Must not throw — the S3 keys are already deleted and the next compact will catch up.
    S3CompactBatcher.flushBatch(s3, "bucket", index, blobKeys(1, false), NOOP_LONG, NOOP_ON_SUCCESS);
  }

  @Test
  void wholeChunkOfFailuresRemovesNothing() {
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(
                S3Error.builder().key("content/b0").code("AccessDenied").message("x").build(),
                S3Error.builder().key("content/b1").code("AccessDenied").message("x").build())
            .build());

    S3CompactBatcher.flushBatch(s3, "bucket", index, blobKeys(2, false), NOOP_LONG, NOOP_ON_SUCCESS);

    verify(index, never()).deleteRecords(any());
  }

  @Test
  void recordDeletionSumsContentSizeOfSuccessfulBlobsOnly() {
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(S3Error.builder().key("content/b1").code("AccessDenied").message("x").build())
            .build());

    AtomicLong recorded = new AtomicLong();
    List<BlobKey> chunk = List.of(
        new BlobKey(new BlobId("b0"), "content/b0", "attrs/b0", null, 100L),
        new BlobKey(new BlobId("b1"), "content/b1", "attrs/b1", null, 999L), // fails
        new BlobKey(new BlobId("b2"), "content/b2", "attrs/b2", null, 50L));

    S3CompactBatcher.flushBatch(s3, "bucket", index, chunk, recorded::addAndGet, NOOP_ON_SUCCESS);

    assertThat(recorded.get()).isEqualTo(150L); // b0 + b2 only — b1 failed
  }

  @Test
  void recordDeletionFiresOncePerSuccessfulBlob() {
    // Regression test for NEXUS-52267 branch-test finding: aggregating recordDeletion per-batch
    // caused blobCount to underreport by ~300× because recordDeletion(long) decrements the count
    // by exactly 1 regardless of the size argument. Fix: call it once per successfully-deleted blob.
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(S3Error.builder().key("content/b1").code("AccessDenied").message("x").build())
            .build());

    java.util.concurrent.atomic.AtomicInteger invocationCount = new java.util.concurrent.atomic.AtomicInteger();
    List<BlobKey> chunk = List.of(
        new BlobKey(new BlobId("b0"), "content/b0", "attrs/b0", null, 100L),
        new BlobKey(new BlobId("b1"), "content/b1", "attrs/b1", null, 999L), // fails
        new BlobKey(new BlobId("b2"), "content/b2", "attrs/b2", null, 50L));

    S3CompactBatcher.flushBatch(s3, "bucket", index, chunk, bytes -> invocationCount.incrementAndGet(),
        NOOP_ON_SUCCESS);

    assertThat(invocationCount.get()).isEqualTo(2); // b0 and b2 succeeded — 2 separate calls, not 1 aggregated
  }

  @Test
  void recordDeletionSkippedWhenAllBlobsFail() {
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(S3Error.builder().key("content/b0").code("AccessDenied").message("x").build())
            .build());

    AtomicLong recorded = new AtomicLong();
    BlobKey only = new BlobKey(new BlobId("b0"), "content/b0", "attrs/b0", null, 200L);

    S3CompactBatcher.flushBatch(s3, "bucket", index, List.of(only), recorded::addAndGet, NOOP_ON_SUCCESS);

    assertThat(recorded.get()).isZero();
  }

  @Test
  void onSuccessFiresOncePerFullyDeletedBlob() {
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(S3Error.builder().key("content/b1").code("AccessDenied").message("x").build())
            .build());

    List<BlobId> invalidated = new ArrayList<>();
    List<BlobKey> chunk = blobKeys(3, false);

    S3CompactBatcher.flushBatch(s3, "bucket", index, chunk, NOOP_LONG, invalidated::add);

    assertThat(invalidated).extracting(BlobId::toString).containsExactly("b0", "b2");
  }

  @Test
  void partialFailureWithSlowDownRetriesOnlyFailedKeys() {
    // First attempt: b1's content key hits SlowDown; retry succeeds → all 3 blobs cleaned.
    // Verifies (a) retry fires only for the throttled key (not the whole batch), and (b) blob b1
    // ultimately removed from index once retry succeeds.
    DeleteObjectsResponse firstAttempt = DeleteObjectsResponse.builder()
        .errors(S3Error.builder().key("content/b1").code("SlowDown").message("Please reduce your request rate").build())
        .build();
    DeleteObjectsResponse retrySucceeds = DeleteObjectsResponse.builder().build();
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(firstAttempt, retrySucceeds);

    S3CompactBatcher.flushBatch(s3, "bucket", index, blobKeys(3, false), NOOP_LONG, NOOP_ON_SUCCESS);

    // Two S3 calls: initial + one retry attempt.
    ArgumentCaptor<DeleteObjectsRequest> reqs = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
    verify(s3, times(2)).deleteObjects(reqs.capture());
    // Initial batch had 6 keys (3 blobs × 2 keys each — no softDeletedAttrs).
    assertThat(reqs.getAllValues().get(0).delete().objects()).hasSize(6);
    // Retry batch had ONLY the failed key.
    assertThat(reqs.getAllValues().get(1).delete().objects()).hasSize(1);
    assertThat(reqs.getAllValues().get(1).delete().objects().get(0).key()).isEqualTo("content/b1");

    // All 3 blobs eventually removed from soft-deleted index.
    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<BlobId>> ids = ArgumentCaptor.forClass((Class) Collection.class);
    verify(index).deleteRecords(ids.capture());
    assertThat(ids.getValue()).hasSize(3);
  }

  @Test
  void slowDownExceedsMaxRetriesLeavesFailedBlobInIndex() {
    // Every attempt returns SlowDown → retry exhausted → b1 stays in the soft-deleted index.
    // b0 and b2 succeeded on the initial attempt and are removed.
    DeleteObjectsResponse alwaysSlowDown = DeleteObjectsResponse.builder()
        .errors(S3Error.builder().key("content/b1").code("SlowDown").message("throttled").build())
        .build();
    when(s3.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(alwaysSlowDown);

    S3CompactBatcher.flushBatch(s3, "bucket", index, blobKeys(3, false), NOOP_LONG, NOOP_ON_SUCCESS);

    // Initial call + 5 retries = 6 total attempts.
    verify(s3, times(6)).deleteObjects(any(DeleteObjectsRequest.class));

    // Only b0 and b2 removed; b1 permanently failed for this run.
    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<BlobId>> ids = ArgumentCaptor.forClass((Class) Collection.class);
    verify(index).deleteRecords(ids.capture());
    assertThat(ids.getValue()).extracting(BlobId::toString).containsExactly("b0", "b2");
  }

  @Test
  void nonRetryableErrorCodeIsNotRetried() {
    // AccessDenied is permanent — no retry, blob stays failed on first pass.
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(S3Error.builder().key("content/b1").code("AccessDenied").message("denied").build())
            .build());

    S3CompactBatcher.flushBatch(s3, "bucket", index, blobKeys(3, false), NOOP_LONG, NOOP_ON_SUCCESS);

    // Exactly 1 S3 call — no retry for a non-transient code.
    verify(s3, times(1)).deleteObjects(any(DeleteObjectsRequest.class));

    // b0 and b2 removed; b1 permanently failed.
    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<BlobId>> ids = ArgumentCaptor.forClass((Class) Collection.class);
    verify(index).deleteRecords(ids.capture());
    assertThat(ids.getValue()).extracting(BlobId::toString).containsExactly("b0", "b2");
  }

  @Test
  void interruptedRetrySleepLeavesRemainingBlobsInIndex() throws InterruptedException {
    // Interrupt the worker thread during retry sleep; pending blob stays in the soft-deleted index.
    CountDownLatch firstCallMade = new CountDownLatch(1);
    DeleteObjectsResponse slowDown = DeleteObjectsResponse.builder()
        .errors(S3Error.builder().key("content/b1").code("SlowDown").message("throttled").build())
        .build();
    doAnswer(inv -> {
      firstCallMade.countDown();
      return slowDown;
    }).when(s3).deleteObjects(any(DeleteObjectsRequest.class));

    Thread runner = new Thread(() -> S3CompactBatcher.flushBatch(
        s3, "bucket", index, blobKeys(3, false), NOOP_LONG, NOOP_ON_SUCCESS));
    runner.start();
    // Wait until the first S3 call completes — worker is now entering Thread.sleep in retry.
    assertThat(firstCallMade.await(5, TimeUnit.SECONDS)).isTrue();
    runner.interrupt();
    runner.join(5_000);
    assertThat(runner.isAlive()).isFalse();

    // b0 and b2 succeeded on the initial call; b1 stays in index because retry was interrupted.
    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<BlobId>> ids = ArgumentCaptor.forClass((Class) Collection.class);
    verify(index).deleteRecords(ids.capture());
    assertThat(ids.getValue()).extracting(BlobId::toString).containsExactly("b0", "b2");
  }

  @Test
  void onSuccessCallbackExceptionDoesNotAbortRemaining() {
    Consumer<BlobId> throwsForB1 = id -> {
      if ("b1".equals(id.toString())) {
        throw new RuntimeException("boom");
      }
    };

    S3CompactBatcher.flushBatch(s3, "bucket", index, blobKeys(3, false), NOOP_LONG, throwsForB1);

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<BlobId>> ids = ArgumentCaptor.forClass((Class) Collection.class);
    verify(index).deleteRecords(ids.capture());
    assertThat(ids.getValue()).hasSize(3); // all three still removed from index despite callback throw
  }

  @Test
  void noSuchKeyTreatedAsSuccessAndBlobRemovedFromIndex() {
    // S3 reports NoSuchKey for the content key — object already deleted by a prior partial compact run.
    // The blob should be removed from the soft-deleted index so it isn't retried indefinitely.
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(S3Error.builder().key("content/b0").code("NoSuchKey").message("Not Found").build())
            .build());

    BlobKey twoKey = new BlobKey(new BlobId("b0"), "content/b0", "attrs/b0", null, 100L);
    AtomicLong recorded = new AtomicLong();
    S3CompactBatcher.flushBatch(s3, "bucket", index, List.of(twoKey), recorded::addAndGet, NOOP_ON_SUCCESS);

    // Blob must be removed from index even though S3 reported NoSuchKey.
    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<BlobId>> ids = ArgumentCaptor.forClass((Class) Collection.class);
    verify(index).deleteRecords(ids.capture());
    assertThat(ids.getValue()).extracting(BlobId::toString).containsExactly("b0");
    // Metric should also fire once deletion is confirmed.
    assertThat(recorded.get()).isEqualTo(100L);
  }

  @Test
  void noSuchKeyBlobWithZeroSizeStillFiresRecordDeletion() {
    // Blob classified with contentSize=0 (attrs absent at classify time — NoSuchKey path).
    // recordDeletion must fire once so the blob count decrements even with no size captured.
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder()
            .errors(
                S3Error.builder().key("content/b0").code("NoSuchKey").message("Not Found").build(),
                S3Error.builder().key("attrs/b0").code("NoSuchKey").message("Not Found").build())
            .build());

    java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger();
    BlobKey zeroSize = new BlobKey(new BlobId("b0"), "content/b0", "attrs/b0", null, 0L);
    S3CompactBatcher.flushBatch(s3, "bucket", index, List.of(zeroSize), bytes -> callCount.incrementAndGet(),
        NOOP_ON_SUCCESS);

    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test
  void dbFailureAfterS3SuccessDoesNotFireMetric() {
    // S3 delete succeeds but DB deleteRecords throws — metric must NOT fire (avoids double-decrement
    // on the next compact run when the blob is re-processed via NoSuchKey path).
    doThrow(new RuntimeException("db down")).when(index).deleteRecords(any());

    AtomicLong recorded = new AtomicLong();
    S3CompactBatcher.flushBatch(s3, "bucket", index, blobKeys(1, false), recorded::addAndGet, NOOP_ON_SUCCESS);

    assertThat(recorded.get()).isZero();
  }

  @Nested
  class HourPrefix
  {
    @Test
    void extractsHourSegmentFromWellFormedKey() {
      assertThat(S3CompactBatcher.hourPrefix("mystore/content/2026/07/08/04/abc123.bytes"))
          .isEqualTo("mystore/content/2026/07/08/04");
    }

    @Test
    void nullKeyReturnsPlaceholder() {
      assertThat(S3CompactBatcher.hourPrefix(null)).isEqualTo("(null)");
    }

    @Test
    void shortKeyWithFewerThanSixSlashesReturnsWholeKey() {
      assertThat(S3CompactBatcher.hourPrefix("store/content/uuid.bytes"))
          .isEqualTo("store/content/uuid.bytes");
    }

    @Test
    void longKeyWithFewerThanThreeSlashesReturnsWholeKey() {
      // 51-char key with only 1 slash — fewer than 3 slashes, return full key
      String key = "a".repeat(25) + "/" + "b".repeat(25);
      assertThat(S3CompactBatcher.hourPrefix(key)).isEqualTo(key);
    }

    @Test
    void twoSlashKeyReturnsWholeKey() {
      // e.g. TemporaryLocationStrategy without bucket prefix: content/tmp/{uuid}
      String key = "content/tmp/abc-def-uuid.bytes";
      assertThat(S3CompactBatcher.hourPrefix(key)).isEqualTo(key);
    }

    @Test
    void keyWithExactlySixSlashesReturnsCorrectPrefix() {
      // Minimal valid path: single-char segments
      assertThat(S3CompactBatcher.hourPrefix("s/c/Y/M/D/H/uuid"))
          .isEqualTo("s/c/Y/M/D/H");
    }

    @Test
    void dateBlobWithoutBucketPrefixGroupsAtMinuteLevel() {
      assertThat(S3CompactBatcher.hourPrefix("content/2026/07/08/04/23/abc-uuid.bytes"))
          .isEqualTo("content/2026/07/08/04/23");
    }

    @Test
    void volChapterKeyGroupsAtThirdSlash() {
      String key = "content/vol-01/chap-01/a-very-long-blob-id-123456789.bytes";
      assertThat(S3CompactBatcher.hourPrefix(key)).isEqualTo("content/vol-01/chap-01");
    }
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private static List<BlobKey> blobKeys(int n, boolean withSoftDel) {
    return java.util.stream.IntStream.range(0, n)
        .mapToObj(i -> new BlobKey(new BlobId("b" + i),
            "content/b" + i, "attrs/b" + i, withSoftDel ? "softdel/b" + i : null, 0L))
        .collect(toList());
  }
}
