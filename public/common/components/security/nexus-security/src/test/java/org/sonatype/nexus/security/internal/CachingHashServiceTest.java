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
package org.sonatype.nexus.security.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.crypto.hash.SimpleHashProvider;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link CachingHashService}.
 */
public class CachingHashServiceTest
{
  private static final String ALGORITHM = "SHA-512";

  private static final int ITERATIONS = 1024;

  // Fixed salt so a fixed source hashes deterministically (no random public-salt generation).
  private static final byte[] SALT = "0123456789abcdef".getBytes();

  private final CachingHashService underTest = new CachingHashService();

  private static HashRequest request(final String algorithm, final String source) {
    return new HashRequest.Builder()
        .setAlgorithmName(algorithm)
        .setSource(source.getBytes())
        .setSalt(SALT)
        .addParameter(SimpleHashProvider.Parameters.PARAMETER_ITERATIONS, ITERATIONS)
        .build();
  }

  @Test
  public void computesHashForKnownAlgorithm() {
    Hash hash = underTest.computeHash(request(ALGORITHM, "s3cr3t"));

    assertThat(hash, is(notNullValue()));
    assertThat(hash.getAlgorithmName(), is(ALGORITHM));
    assertThat(hash.getIterations(), is(ITERATIONS));
  }

  @Test
  public void identicalRequestsProduceIdenticalHashes() {
    String first = underTest.computeHash(request(ALGORITHM, "s3cr3t")).toBase64();
    String second = underTest.computeHash(request(ALGORITHM, "s3cr3t")).toBase64();

    assertThat(second, is(first));
  }

  @Test
  public void emptySourceReturnsNull() {
    HashRequest emptySource = new HashRequest.Builder()
        .setAlgorithmName(ALGORITHM)
        .setSource(new byte[0])
        .build();

    assertThat(underTest.computeHash(emptySource), is(nullValue()));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unknownAlgorithmThrows() {
    underTest.computeHash(request("NO-SUCH-ALGO", "s3cr3t"));
  }

  /**
   * The whole point of this class is to resolve {@link org.apache.shiro.crypto.hash.HashSpi} providers ONCE instead of
   * re-running a {@code ServiceLoader} scan on every hash. {@link CountingHashSpi} (registered via
   * {@code META-INF/services}) counts its instantiations, so we can assert discovery happens once per instance and
   * never again per {@code computeHash}. (Assumes sequential test execution, which is the module default.)
   */
  @Test
  public void providersAreResolvedOncePerInstanceNotPerComputeHash() {
    int before = CountingHashSpi.instantiations();
    CachingHashService service = new CachingHashService();
    int afterConstruction = CountingHashSpi.instantiations();

    for (int i = 0; i < 50; i++) {
      service.computeHash(request(ALGORITHM, "s3cr3t"));
    }
    int afterHashing = CountingHashSpi.instantiations();

    // Construction drains the ServiceLoader exactly once (one CountingHashSpi instantiated)...
    assertThat(afterConstruction - before, is(1));
    // ...and computeHash performs no further provider discovery.
    assertThat(afterHashing, is(afterConstruction));
  }

  /**
   * Exercises the hot path the way credential verification does: many threads resolving and computing the same hash
   * concurrently. The provider index is materialised once at construction, so this must never throw and every thread
   * must observe the identical result (the shared {@code ServiceLoader} of the previous implementation was not safe
   * for this concurrent use).
   */
  @Test
  public void concurrentComputeHashIsConsistent() throws Exception {
    int threads = 16;
    int perThread = 100;
    String expected = underTest.computeHash(request(ALGORITHM, "s3cr3t")).toBase64();

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      CountDownLatch start = new CountDownLatch(1);
      Set<String> results = ConcurrentHashMap.newKeySet();
      CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();

      List<Future<?>> futures = new ArrayList<>();
      for (int t = 0; t < threads; t++) {
        Callable<Void> task = () -> {
          start.await();
          for (int i = 0; i < perThread; i++) {
            results.add(underTest.computeHash(request(ALGORITHM, "s3cr3t")).toBase64());
          }
          return null;
        };
        futures.add(pool.submit(task));
      }

      start.countDown();
      for (Future<?> future : futures) {
        try {
          future.get(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
          failures.add(e.getCause() != null ? e.getCause() : e);
        }
      }

      if (!failures.isEmpty()) {
        StringWriter trace = new StringWriter();
        try (PrintWriter out = new PrintWriter(trace)) {
          failures.forEach(failure -> failure.printStackTrace(out));
        }
        fail(failures.size() + " concurrent computeHash call(s) failed:\n" + trace);
      }
      assertThat(results, hasSize(1));
      assertThat(results.iterator().next(), is(expected));
    }
    finally {
      pool.shutdownNow();
    }
  }
}
