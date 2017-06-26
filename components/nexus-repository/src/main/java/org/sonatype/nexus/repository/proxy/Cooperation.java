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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.sonatype.goodies.common.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagateIfPossible;
import static java.lang.Boolean.TRUE;

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

  @FunctionalInterface
  public interface IOCall<T>
  {
    T call(boolean checkCache) throws IOException;
  }

  /**
   * @param passiveTimeout used when passively waiting for an initial download
   * @param activeTimeout used when actively waiting for a download dependency
   */
  public Cooperation(final Time passiveTimeout, final Time activeTimeout) {
    this.passiveTimeout = checkNotNull(passiveTimeout);
    this.activeTimeout = checkNotNull(activeTimeout);
  }

  /**
   * Requests cooperation with other threads that might already be downloading the same remote content.
   *
   * @param key uniquely identifies the content to be downloaded
   * @param fetch function that will download the remote content
   * @return remote content
   * @throws IOException
   */
  public T cooperate(final String key, final IOCall<T> fetch) throws IOException {

    // try and declare our interest in downloading the content
    CooperatingFuture<T> myFuture = new CooperatingFuture<>();
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

    // stagger timeout to preserve a minimum gap between download requests for the same content
    // (use shorter timeout if we're part of a download chain, to avoid accumulating undue delay)
    Time timeout = theirFuture.staggerTimeout(isDownloading() ? activeTimeout : passiveTimeout);

    log.debug("Attempt cooperative wait on {} for {}", key, timeout);
    try {
      // wait for the primary thread to finish its download
      return theirFuture.get(timeout.value(), timeout.unit());
    }
    catch (ExecutionException e) { // NOSONAR unwrap and report download errors
      propagateIfPossible(e.getCause(), IOException.class);
      throw new RuntimeException(e.getCause());
    }
    catch (CancellationException | InterruptedException | TimeoutException e) {
      log.debug("Abandoned cooperative wait on {} for {}", key, timeout, e);
    }

    // primary download thread might be stuck, so check cache before trying the download ourselves
    // (if there are download errors then don't disturb the primary thread, just let them propagate)
    return download(theirFuture, fetch, true);
  }

  private static boolean isDownloading() {
    return TRUE.equals(isDownloading.get());
  }

  /**
   * Attempt to download remote content, optionally checking local cache, storing any result in the given future.
   */
  private T download(final CooperatingFuture<T> future, final IOCall<T> fetch, final boolean checkCache)
      throws IOException
  {
    if (isDownloading()) {
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
  }
}
