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
package org.sonatype.nexus.thread;

import java.util.concurrent.Future;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DatabaseStatusDelayedExecutorTest
    extends TestSupport
{
  private static final int THREAD_POOL_SIZE = 1;

  private DatabaseStatusDelayedExecutor statusDelayedExecutor;

  @Before
  public void setup() throws Exception {
    statusDelayedExecutor = new DatabaseStatusDelayedExecutor(THREAD_POOL_SIZE);
    statusDelayedExecutor.start();
  }

  @Test
  public void ensureThatTaskRuns() throws Exception {
    Future<String> result = statusDelayedExecutor.submit(() -> "Done");

    await()
        .atMost(1000, MILLISECONDS)
        .until(() -> result.isDone());

    assertThat(result.get(), is("Done"));
  }

  @Test
  public void tasksExecuteImmediately() throws Exception {
    Future<String> result = statusDelayedExecutor.submit(() -> "Completed");

    await()
        .atMost(500, MILLISECONDS)
        .until(result::isDone);

    assertThat(result.get(), is("Completed"));
  }
}
