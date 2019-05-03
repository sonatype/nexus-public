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
package org.sonatype.nexus.orient;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.internal.MemoryDatabaseManager;
import org.sonatype.nexus.orient.testsupport.internal.MinimalDatabaseServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.Thread.State.WAITING;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Exercises partitioned and non-partitioned {@link DatabasePool}s.
 */
public class DatabasePoolTest
    extends TestSupport
{
  private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

  private final MinimalDatabaseServer server = new MinimalDatabaseServer();

  private final DatabaseManagerSupport manager = new MemoryDatabaseManager();

  @Before
  public void setUp() throws Exception {
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void settingBothConnectionLimitsToZeroTriggersException() throws Exception {
    manager.setMaxConnections(0);
    manager.setMaxConnectionsPerCore(0);
    try {
      manager.start();
      manager.instance("test");
      manager.pool("test");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
          is("Either nexus.orient.maxConnectionsPerCore or nexus.orient.maxConnections must be positive"));
    }
    finally {
      manager.stop();
    }
  }

  @Test
  public void settingBothConnectionLimitsNegativeTriggersException() throws Exception {
    manager.setMaxConnections(Integer.MIN_VALUE);
    manager.setMaxConnectionsPerCore(Integer.MIN_VALUE);
    try {
      manager.start();
      manager.instance("test");
      manager.pool("test");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
          is("Either nexus.orient.maxConnectionsPerCore or nexus.orient.maxConnections must be positive"));
    }
    finally {
      manager.stop();
    }
  }

  @Test
  public void exceedingConnectionLimitBlocks() throws Exception {
    int numWorkers = 20;
    int maximumPoolSize = 10;

    manager.setMaxConnections(maximumPoolSize);
    manager.setMaxConnectionsPerCore(-1);
    try {
      manager.start();
      manager.instance("test");
      DatabasePool pool = manager.pool("test");

      List<Thread> workers = generateWorkers(pool, numWorkers);

      workers.forEach(Thread::start);

      // wait for pool to reach its maximum size
      await().until(pool::getPoolSize, is(maximumPoolSize));

      // check all the other threads are waiting
      await().until(() -> countWaitingThreads(workers), is(numWorkers - maximumPoolSize));
    }
    finally {
      manager.stop();
    }
  }

  @Test
  public void exceedingPerCoreConnectionLimitBlocks() throws Exception {
    int numWorkers = NUM_CORES * 8; // set high to try and hit all (hashed) partitions
    int perCoreLimit = 2; // deliberately set low as we want to test overflow condition

    // one partition per-core, plus one overflow partition
    int maximumPoolSize = (NUM_CORES + 1) * perCoreLimit;

    manager.setMaxConnections(-1);
    manager.setMaxConnectionsPerCore(perCoreLimit);
    try {
      manager.start();
      manager.instance("test");
      DatabasePool pool = manager.pool("test");

      List<Thread> workers = generateWorkers(pool, numWorkers);

      workers.forEach(Thread::start);

      // wait for pool to reach its maximum size
      await().until(pool::getPoolSize, is(maximumPoolSize));

      // check all the other threads are waiting; allow for the fact that a few connections
      // could still be available since hash distribution may not fully cover all partitions
      // depending on the number of cores

      int expectedWaiterCount = numWorkers - (maximumPoolSize - pool.getAvailableCount());
      await().until(() -> countWaitingThreads(workers), is(expectedWaiterCount));
    }
    finally {
      manager.stop();
    }
  }

  private List<Thread> generateWorkers(DatabasePool pool, int numWorkers) {
    return generate(() -> (Runnable) () -> pool.acquire())
        .limit(numWorkers)
        .map(Thread::new)
        .collect(toList());
  }

  private int countWaitingThreads(List<Thread> workers) {
    return (int) workers.stream()
        .map(Thread::getState)
        .filter(WAITING::equals)
        .count();
  }
}
