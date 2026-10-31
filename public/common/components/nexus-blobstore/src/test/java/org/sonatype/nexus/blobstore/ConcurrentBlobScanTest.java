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

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobScanConcurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * Covers the concurrent scan on its own, without a blob store: the predicate stands in for the attribute read.
 */
class ConcurrentBlobScanTest
{
  private static final Duration GENEROUS = Duration.ofSeconds(30);

  private final ExecutorService executor = Executors.newFixedThreadPool(4);

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  private static BlobId blobId(final String name) {
    return new BlobId(name, null);
  }

  private static List<String> names(final Stream<BlobId> ids) {
    return ids.map(BlobId::asUniqueString).collect(toList());
  }

  private BlobScanConcurrency concurrency(final int maxReadsInFlight, final Duration readTimeout) {
    return new BlobScanConcurrency(executor, maxReadsInFlight, readTimeout);
  }

  @Test
  void retainsOnlyWhatThePredicateKeeps() {
    Stream<BlobId> source = Stream.of(blobId("keep-1"), blobId("drop"), blobId("keep-2"));
    Predicate<BlobId> retain = id -> id.asUniqueString().startsWith("keep");

    try (Stream<BlobId> scanned = ConcurrentBlobScan.filter(source, retain, concurrency(10, GENEROUS))) {
      assertThat(names(scanned), containsInAnyOrder("keep-1", "keep-2"));
    }
  }

  @Test
  void emitsFinishedReadsWhileAnEarlierOneIsStillBlocked() {
    CountDownLatch release = new CountDownLatch(1);
    Stream<BlobId> source = Stream.of(blobId("blocked"), blobId("free-1"), blobId("free-2"));
    Predicate<BlobId> retain = id -> {
      if ("blocked".equals(id.asUniqueString())) {
        awaitQuietly(release);
      }
      return true;
    };

    try (Stream<BlobId> scanned = ConcurrentBlobScan.filter(source, retain, concurrency(10, GENEROUS))) {
      Iterator<BlobId> ids = scanned.iterator();

      // The blocked read was submitted first and cannot finish yet, so anything emitted now proves completion order
      List<String> whileBlocked = List.of(ids.next().asUniqueString(), ids.next().asUniqueString());
      assertThat(whileBlocked, containsInAnyOrder("free-1", "free-2"));

      release.countDown();
      assertThat(ids.next().asUniqueString(), is("blocked"));
      assertThat(ids.hasNext(), is(false));
    }
    finally {
      release.countDown();
    }
  }

  @Test
  void keepsBlobsStillOutstandingWhenNothingFinishesInTime() {
    CountDownLatch neverReleased = new CountDownLatch(1);
    Stream<BlobId> source = Stream.of(blobId("wedged-1"), blobId("wedged-2"));
    Predicate<BlobId> retain = id -> {
      awaitQuietly(neverReleased);
      return false; // would be dropped if the read were ever believed
    };

    Duration readTimeout = Duration.ofMillis(200);
    long startNanos = System.nanoTime();
    try (Stream<BlobId> scanned = ConcurrentBlobScan.filter(source, retain, concurrency(10, readTimeout))) {
      // Giving up keeps them: a blob dropped in error goes unreconciled, one kept in error costs a second read
      assertThat(names(scanned), contains("wedged-1", "wedged-2"));
    }
    finally {
      neverReleased.countDown();
    }

    // One poll times out and both reads are given up together, so this should be about one readTimeout. The bound is
    // ten times that: loose enough for a busy machine, tight enough to fail if the scan waited on the reads instead.
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    assertThat(elapsedMillis, is(lessThan(readTimeout.toMillis() * 10)));
  }

  @Test
  void keepsBlobsWhoseReadFailed() {
    Stream<BlobId> source = Stream.of(blobId("boom"));
    Predicate<BlobId> retain = id -> {
      throw new IllegalStateException("attribute read failed");
    };

    try (Stream<BlobId> scanned = ConcurrentBlobScan.filter(source, retain, concurrency(10, GENEROUS))) {
      assertThat(names(scanned), contains("boom"));
    }
  }

  @Test
  void neverLeavesMoreReadsOutstandingThanConfigured() {
    int maxReadsInFlight = 3;
    AtomicInteger reading = new AtomicInteger();
    AtomicInteger highWaterMark = new AtomicInteger();

    Stream<BlobId> source = Stream.iterate(0, i -> i + 1).limit(40).map(i -> blobId("blob-" + i));
    Predicate<BlobId> retain = id -> {
      int concurrent = reading.incrementAndGet();
      highWaterMark.accumulateAndGet(concurrent, Math::max);
      // Held briefly so overlapping reads actually overlap, otherwise the cap is never exercised
      sleepQuietly();
      reading.decrementAndGet();
      return true;
    };

    try (Stream<BlobId> scanned = ConcurrentBlobScan.filter(source, retain, concurrency(maxReadsInFlight, GENEROUS))) {
      assertThat(names(scanned), hasSize(40));
    }
    // The executor has more threads than this, so anything above the cap would come from over-submitting
    assertThat(highWaterMark.get(), is(lessThanOrEqualTo(maxReadsInFlight)));
  }

  @Test
  void leavesTheCallersExecutorRunningAndClosesTheSource() {
    AtomicBoolean sourceClosed = new AtomicBoolean();
    Stream<BlobId> source = Stream.of(blobId("only")).onClose(() -> sourceClosed.set(true));

    try (Stream<BlobId> scanned = ConcurrentBlobScan.filter(source, id -> true, concurrency(10, GENEROUS))) {
      assertThat(names(scanned), contains("only"));
    }

    assertThat(sourceClosed.get(), is(true));
    assertThat(executor.isShutdown(), is(false));
  }

  @Test
  void stopsWithoutReadingAnythingForAnEmptyListing() {
    try (Stream<BlobId> scanned = ConcurrentBlobScan.filter(Stream.of(), id -> true, concurrency(10, GENEROUS))) {
      assertThat(names(scanned), is(empty()));
    }
  }

  private static void sleepQuietly() {
    try {
      Thread.sleep(5);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void awaitQuietly(final CountDownLatch latch) {
    try {
      latch.await(30, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
