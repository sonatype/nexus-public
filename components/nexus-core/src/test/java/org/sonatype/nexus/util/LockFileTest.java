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
package org.sonatype.nexus.util;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LockFileTest
    extends TestSupport
{
  /**
   * Simple test where two locks attempt to lock same file.
   */
  @Test
  public void twoLocks()
      throws Exception
  {
    final File lockFile = util.createTempFile();

    final LockFile lf1 = new LockFile(lockFile, "lf1".getBytes());
    final LockFile lf2 = new LockFile(lockFile, "lf2".getBytes());

    try {
      // lf1 will obtain lock, lf2 should fail
      assertThat(lf1.lock(), is(true));
      assertThat(lf2.lock(), is(false));

      // repeating same step should be idempotent
      assertThat(lf1.lock(), is(true));
      assertThat(lf2.lock(), is(false));

      // verify who holds the lock
      assertThat(lf1.readBytes(), equalTo(lf1.getPayload()));
    }
    finally {
      // cleanup
      lf1.release();
      lf2.release();
    }
  }

  /**
   * Test where two locks are "passing over" to each other the lock ownership.
   */
  @Test
  public void twoLocksPassingOver()
      throws Exception
  {
    final File lockFile = util.createTempFile();

    final LockFile lf1 = new LockFile(lockFile, "lf1".getBytes());
    final LockFile lf2 = new LockFile(lockFile, "lf2".getBytes());
    try {
      // lf1 will obtain lock, lf2 should fail
      assertThat(lf1.lock(), is(true));
      assertThat(lf2.lock(), is(false));

      // verify who holds the lock
      assertThat(lf1.readBytes(), equalTo(lf1.getPayload()));

      // pass over the lock
      lf1.release();
      assertThat(lf2.lock(), is(true));

      // verify who holds the lock
      assertThat(lf2.readBytes(), equalTo(lf2.getPayload()));

      // repeating same step should be idempotent
      assertThat(lf1.lock(), is(false));
      assertThat(lf2.lock(), is(true));

      // verify who holds the lock
      assertThat(lf2.readBytes(), equalTo(lf2.getPayload()));
    }
    finally {
      // cleanup
      lf1.release();
      lf2.release();
    }
  }

  /**
   * Multiple threads performing multiple synchronized attempts to lock same FileLock. As many locks should be obtained
   * as many attempts are made (thread count is irrelevant), as only one should win per each attempt.
   */
  @Test
  public void concurrencyTest() throws Exception {
    final File lockFile = util.createTempFile();
    final int threadNumber = 10;
    final int attempts = 100;
    final CyclicBarrier barrier = new CyclicBarrier(threadNumber);
    final ExecutorService ex = Executors.newCachedThreadPool();
    final List<FileLockerRunnable> flrs = Lists.newArrayList();
    for (int i = 1; i <= threadNumber; i++) {
      final FileLockerRunnable flr = new FileLockerRunnable(lockFile, String.valueOf(i), barrier, attempts);
      flrs.add(flr);
      ex.submit(flr); // due to barrier they will wait until all ready to start
    }

    ex.shutdown();
    // Any exceptions thrown by the FileLockerRunnables will lead to a timeout because of the cyclic barrier
    boolean timelyCompletion = ex.awaitTermination(10L, TimeUnit.SECONDS);

    if (!timelyCompletion) {
      log("Test execution timed out.");
    }

    int totalLocksHappened = 0;

    for(FileLockerRunnable flr : flrs){
      // If a runnable encountered an exception, use it to fail the test
      if (flr.getCaughtException() != null) {
        throw new Exception(flr.getCaughtException());
      }
      totalLocksHappened+= flr.getLocked();
    }
    assertThat(totalLocksHappened, equalTo(attempts));
  }

  public static class FileLockerRunnable
      implements Callable<Integer>
  {
    private final LockFile lockFile;

    private final CyclicBarrier barrier;

    private final int attempts;

    private int locked;

    private Exception caughtException;

    public FileLockerRunnable(final File lockFile, String name,
                              final CyclicBarrier barrier, final int attempts)
    {
      this.lockFile = new LockFile(lockFile, name.getBytes(Charset.forName("UTF-8")));
      this.barrier = barrier;
      this.attempts = attempts;
    }

    public int getLocked() {
      return locked;
    }

    public Exception getCaughtException() { return caughtException; }

    @Override
    public Integer call() throws Exception {
      for (int i = 0; i < attempts; i++) {
        barrier.await(); // start together
        try {
          if (lockFile.lock()) {
            locked++;
            // verify that content of the file equals to our payload, as we own the lock
            assertThat(lockFile.readBytes(), equalTo(lockFile.getPayload()));
          }
          barrier.await(); // wait for others to attempt, and the one won should hold the lock during that
        }
        catch (Exception e) {
          // If an exception gets thrown, hang on to it so that it can be reported by the test
          // Unconventional, but it seems the cyclic barrier rules out normal approach of getting the exception from Future.get()
          this.caughtException = e;
        }
        finally {
          lockFile.release(); // the one locked should release for next attempt
        }
      }
      return locked;
    }
  }
}
