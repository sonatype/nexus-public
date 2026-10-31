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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Spliterators;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobScanConcurrency;
import org.sonatype.nexus.blobstore.api.BlobStoreException;

import com.google.common.collect.AbstractIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters a listing of blob ids by evaluating a predicate on the caller's executor rather than inline.
 *
 * <p>
 * What the predicate does is the caller's business; the point is that it blocks. Reading each blob's attributes is the
 * case this exists for, so the vocabulary below calls one evaluation a read, but nothing here assumes that.
 *
 * <p>
 * Ids are emitted as their reads finish, not in listing order, so one slow read neither holds back verdicts already
 * decided nor leaves the executor idle. The source stays lazy, so a listing of any size is walked in bounded memory.
 *
 * <p>
 * A completion service reports that some read finished, never which one is stuck, so
 * {@link BlobScanConcurrency#readTimeout()} bounds the scan making no progress rather than any single read. Ids still
 * outstanding when it expires are retained, as are ids whose read threw: keeping one wrongly costs the caller a second
 * look, dropping one wrongly loses it silently.
 *
 * <p>
 * Single-consumer by design - only the consuming thread touches the source, the outstanding reads and the decided ids.
 * The close handler is the exception, and is safe because the stream is closed by the thread that consumed it.
 */
class ConcurrentBlobScan
{
  private static final Logger log = LoggerFactory.getLogger(ConcurrentBlobScan.class);

  private final Iterator<BlobId> source;

  private final Predicate<BlobId> retain;

  private final BlobScanConcurrency concurrency;

  private final CompletionService<Boolean> reads;

  /**
   * Insertion-ordered so that giving up on a stalled scan keeps blobs in listing order.
   */
  private final Map<Future<Boolean>, BlobId> outstanding = new LinkedHashMap<>();

  /**
   * Ids ready to emit. Needed because giving up on a stalled scan decides many ids at once, while the iterator hands
   * back one at a time.
   */
  private final Queue<BlobId> decided = new ArrayDeque<>();

  private ConcurrentBlobScan(
      final Stream<BlobId> blobIds,
      final Predicate<BlobId> retain,
      final BlobScanConcurrency concurrency)
  {
    this.source = blobIds.iterator();
    this.retain = retain;
    this.concurrency = concurrency;
    this.reads = new ExecutorCompletionService<>(concurrency.executor());
  }

  /**
   * Wraps the listing in a lazy stream that evaluates {@code retain} concurrently. Closing it cancels reads still
   * outstanding and closes the listing; the executor is the caller's and is left running.
   *
   * @return the retained ids, in the order their reads finished
   */
  static Stream<BlobId> filter(
      final Stream<BlobId> blobIds,
      final Predicate<BlobId> retain,
      final BlobScanConcurrency concurrency)
  {
    ConcurrentBlobScan scan = new ConcurrentBlobScan(blobIds, retain, concurrency);
    Iterator<BlobId> retained = new AbstractIterator<>()
    {
      @Override
      protected BlobId computeNext() {
        BlobId next = scan.nextRetained();
        return next == null ? endOfData() : next;
      }
    };

    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(retained, 0), false)
        .onClose(scan::cancelOutstanding)
        // The source holds the listing, which would otherwise be left open.
        .onClose(blobIds::close);
  }

  /**
   * Emits an already-decided id if there is one, otherwise tops the executor back up and waits for a read to land.
   *
   * @return the next retained id, or {@code null} once the listing is exhausted and nothing is outstanding
   */
  private BlobId nextRetained() {
    while (true) {
      if (!decided.isEmpty()) {
        return decided.poll();
      }
      submitUpToLimit();
      // Submitting can decide ids by itself, when the executor refuses a read
      if (!decided.isEmpty()) {
        continue;
      }
      if (outstanding.isEmpty()) {
        return null;
      }
      Future<Boolean> finished = awaitAnyRead();
      if (finished == null) {
        keepStalledReads();
        continue;
      }
      BlobId blobId = outstanding.remove(finished);
      if (retained(finished, blobId)) {
        return blobId;
      }
    }
  }

  /**
   * Submits from the listing until the configured number of reads is outstanding, or the listing runs out. An executor
   * that refuses a read retains the id instead: running it here would put the caller's blocking work on the thread
   * walking the listing, which is the stall this class exists to avoid.
   */
  private void submitUpToLimit() {
    while (outstanding.size() < concurrency.maxReadsInFlight() && source.hasNext()) {
      BlobId blobId = source.next();
      try {
        outstanding.put(reads.submit(() -> retain.test(blobId)), blobId);
      }
      catch (RejectedExecutionException e) {
        log.debug("Executor refused the scan read for blob {}, retaining it", blobId, e);
        decided.add(blobId);
        // Stop offering to an executor that is already refusing
        return;
      }
    }
  }

  /**
   * @return the next read to finish, or {@code null} if none finished before the timeout, which is a stalled scan
   *         rather than an error
   */
  private Future<Boolean> awaitAnyRead() {
    try {
      return reads.poll(concurrency.readTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BlobStoreException("Interrupted while reading blob attributes", e, null);
    }
  }

  /**
   * Unwraps a finished read without blocking, and without throwing: a read that produced no answer retains its id,
   * matching what a sequential scan does when the predicate fails.
   *
   * @return whether the id belongs in the scan
   */
  private boolean retained(final Future<Boolean> finished, final BlobId blobId) {
    try {
      return finished.get();
    }
    catch (CancellationException e) {
      // The stream was closed, so the scan is being abandoned and this blob no longer matters
      return false;
    }
    catch (ExecutionException e) {
      log.debug("Scan read failed for blob {}, retaining it", blobId, e);
      return true;
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BlobStoreException("Interrupted while reading blob attributes", e, null);
    }
  }

  /**
   * Retains every outstanding id and abandons its read, after a whole timeout passed with nothing finishing. There is
   * no way to tell which read is at fault, so the whole outstanding set is given up on together.
   */
  private void keepStalledReads() {
    // Interrupting is a request, so the reads may well still be running - say so rather than let the count read as
    // though the work had stopped
    log.warn("No scan read finished within {}, retaining the {} blob(s) still outstanding; their reads are abandoned"
        + " but may still be running", concurrency.readTimeout(), outstanding.size());
    outstanding.forEach((read, blobId) -> {
      read.cancel(true);
      decided.add(blobId);
    });
    outstanding.clear();
  }

  /**
   * Cancellation is the only signal a read can observe once submitted - CancelableHelper's flag is a ThreadLocal and
   * does not reach the executor's threads - so it is how an abandoned scan stops work already queued or running.
   */
  private void cancelOutstanding() {
    outstanding.keySet().forEach(read -> read.cancel(true));
  }
}
