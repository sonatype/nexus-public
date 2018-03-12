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
package org.sonatype.nexus.repository.proxy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.sonatype.goodies.common.Time;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagateIfPossible;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toMap;

/**
 * Manages cooperation between multiple threads to avoid duplicated I/O.
 *
 * @since 3.4
 */
public class Cooperation<T>
{
  private static final Logger log = LoggerFactory.getLogger(Cooperation.class);

  private static final ThreadLocal<Boolean> isDownloading = new ThreadLocal<>();

  private final ConcurrentMap<String, CooperatingFuture<T>> futureValues = new ConcurrentHashMap<>();

  private final Time passiveTimeout;

  private final Time activeTimeout;

  private final int threadsPerKey;

  @FunctionalInterface
  public interface IOCall<T>
  {
    T call(boolean checkCache) throws IOException;
  }

  /**
   * @param passiveTimeout used when passively waiting for an initial download
   * @param activeTimeout used when actively waiting for a download dependency
   * @param threadsPerKey maximum threads that can wait for the same download
   */
  public Cooperation(final Time passiveTimeout, final Time activeTimeout, final int threadsPerKey) {
    this.passiveTimeout = checkNotNull(passiveTimeout);
    this.activeTimeout = checkNotNull(activeTimeout);
    this.threadsPerKey = threadsPerKey;
  }

  /**
   * Requests cooperation with other threads that might already be downloading the same remote content.
   *
   * @param key uniquely identifies the content to be downloaded
   * @param fetch function that will download the remote content
   *
   * @return remote content
   *
   * @throws IOException when download fails
   * @throws CooperationException when cooperation fails
   */
  public T cooperate(final String key, final IOCall<T> fetch) throws IOException {

    // try and declare our interest in downloading the content
    CooperatingFuture<T> myFuture = new CooperatingFuture<>(key);
    CooperatingFuture<T> theirFuture = futureValues.putIfAbsent(key, myFuture);

    if (theirFuture == null) {
      // no-one else is downloading the content, go-ahead
      try {
        return download(myFuture, fetch, false);
      }
      catch (Exception | Error e) { // NOSONAR report all download errors to cooperating threads
        myFuture.completeExceptionally(e);
        throw e;
      }
      finally {
        futureValues.remove(key, myFuture);
      }
    }

    theirFuture.increaseCooperation(threadsPerKey);
    try {
      if (currentThreadAlreadyDownloading()) {
        // this is a dependency; use shorter timeout and be prepared to download in parallel
        // (just in case the thread we're waiting on ends up waiting on our primary content)
        return waitForDownload(theirFuture, fetch, activeTimeout, true);
      }
      else {
        // waiting for primary content; use longer timeout and avoid downloading in parallel
        return waitForDownload(theirFuture, fetch, passiveTimeout, false);
      }
    }
    finally {
      theirFuture.decreaseCooperation();
    }
  }

  /**
   * Cooperatively waits for the download thread; may resort to downloading in parallel if allowed.
   *
   * @param future shared future that request threads cooperate on
   * @param fetch function that will download the remote content
   * @param initialTimeout how long to wait for the download if we were the only thread waiting
   * @param downloadOnTimeout whether to download in parallel if original thread takes too long
   *
   * @return remote content
   *
   * @throws IOException when download fails
   * @throws CooperationException when cooperation fails
   */
  private T waitForDownload(final CooperatingFuture<T> future, // NOSONAR
                            final IOCall<T> fetch,
                            final Time initialTimeout,
                            final boolean downloadOnTimeout)
      throws IOException
  {
    try {
      if (initialTimeout.value() <= 0) {
        log.debug("Attempt cooperative wait on {}", future);
        return future.get(); // wait indefinitely
      }

      Time timeout = initialTimeout;
      if (downloadOnTimeout) {
        timeout = future.staggerTimeout(timeout); // preserve minimum gap between parallel downloads
      }

      log.debug("Attempt cooperative wait on {} for {}", future, timeout);
      return future.get(timeout.value(), timeout.unit());
    }
    catch (ExecutionException e) { // NOSONAR unwrap and report download errors
      log.debug("Cooperative wait failed on {}", future, e.getCause());
      propagateIfPossible(e.getCause(), IOException.class);
      throw new IOException("Cooperative wait failed on " + future, e.getCause());
    }
    catch (CancellationException | InterruptedException e) {
      log.debug("Cooperative wait cancelled on {}", future, e);
      throw new CooperationException("Cooperative wait cancelled on " + future);
    }
    catch (TimeoutException e) {
      log.debug("Cooperative wait timed out on {}", future, e);

      if (downloadOnTimeout) {
        return download(future, fetch, true); // go remote in case original download thread is stuck
      }

      throw new CooperationException("Cooperative wait timed out on " + future);
    }
  }

  /**
   * Is the current thread flagged as already downloading some other content?
   *
   * This can happen when a download dependency, such as an index file, is needed to complete the initial request.
   */
  private static boolean currentThreadAlreadyDownloading() {
    return TRUE.equals(isDownloading.get());
  }

  /**
   * Attempt to download remote content, optionally checking local cache, storing any result in the given future.
   */
  private T download(final CooperatingFuture<T> future, final IOCall<T> fetch, final boolean checkCache)
      throws IOException
  {
    if (currentThreadAlreadyDownloading()) {
      return future.download(fetch, checkCache);
    }
    try {
      isDownloading.set(TRUE);
      return future.download(fetch, checkCache);
    }
    finally {
      isDownloading.remove();
    }
  }

  /**
   * {@link CompletableFuture} that keeps track of the last staggered time to
   * preserve a minimum gap between download requests for the same content.
   */
  static class CooperatingFuture<T>
      extends CompletableFuture<T>
  {
    private final AtomicLong staggerTimeMillis = new AtomicLong(System.currentTimeMillis());

    private final AtomicInteger threadCount = new AtomicInteger(1);

    private final String key;

    CooperatingFuture(final String key) {
      this.key = checkNotNull(key);
    }

    /**
     * Increases the cooperation count by one.
     *
     * @throws CooperationException if increasing the count would breach the given limit.
     */
    public void increaseCooperation(final int limit) {
      // try to avoid depleting entire request pool with waiting threads
      threadCount.getAndUpdate(count -> {
        if (count >= limit) {
          log.debug("Thread cooperation maxed for {}", this);
          throw new CooperationException("Thread cooperation maxed for " + this);
        }
        return count + 1;
      });
    }

    /**
     * Decreases the cooperation count by one.
     */
    public void decreaseCooperation() {
      threadCount.decrementAndGet();
    }

    /**
     * @return staggered timeout that makes sure waiting threads don't all wake-up at the same time
     */
    public Time staggerTimeout(final Time gap) {
      long currentTimeMillis = System.currentTimeMillis();

      // atomically progress the staggered time
      long prevTimeMillis, nextTimeMillis;
      do {
        prevTimeMillis = staggerTimeMillis.get();
        nextTimeMillis = Math.max(prevTimeMillis + gap.toMillis(), currentTimeMillis);
      }
      while (!staggerTimeMillis.compareAndSet(prevTimeMillis, nextTimeMillis));

      return Time.millis(nextTimeMillis - currentTimeMillis);
    }

    /**
     * Fluent method that downloads content, stores it in this future, before finally returning it.
     */
    public T download(final IOCall<T> fetch, final boolean checkCache) throws IOException {
      T value = fetch.call(checkCache);
      complete(value);
      return value;
    }

    @Override
    public String toString() {
      return key + " (" + threadCount.get() + " threads cooperating)";
    }

    @VisibleForTesting
    String getKey() {
      return key;
    }

    @VisibleForTesting
    int getThreadCount() {
      return threadCount.get();
    }
  }

  /**
   * @return number of threads cooperating per request-key.
   */
  @VisibleForTesting
  Map<String, Integer> getThreadCountPerKey() {
    return futureValues.values().stream().collect(
        toMap(CooperatingFuture::getKey, CooperatingFuture::getThreadCount));
  }
}
