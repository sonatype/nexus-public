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

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseStatusDelayedExecutorTest
    extends TestSupport
{

  private static final int SLEEP_INTERVAL_MS = 25;

  private static final int MAX_RETRIES = 5;

  @Mock
  DatabaseIsWritableService databaseIsWritableService;

  DatabaseStatusDelayedExecutor statusDelayedExecutor;

  @Before
  public void setup() throws Exception {
    statusDelayedExecutor = new DatabaseStatusDelayedExecutor(databaseIsWritableService, 1, SLEEP_INTERVAL_MS,
        MAX_RETRIES);
    statusDelayedExecutor.start();
  }

  @Test
  public void ensureThatTaskEventuallyRuns() {
    when(databaseIsWritableService.isWritable()).thenReturn(false);

    Future<String> result = statusDelayedExecutor.submit(() -> "Done");

    await()
        .pollDelay(SLEEP_INTERVAL_MS / 2, MILLISECONDS)
        .atMost(2 * MAX_RETRIES * SLEEP_INTERVAL_MS, MILLISECONDS)
        .until(() -> result.isDone());

    verify(databaseIsWritableService, times(MAX_RETRIES)).isWritable();
  }

  @Test
  public void noWritableDelaysTask() {
    final AtomicInteger callCount = new AtomicInteger(0);
    when(databaseIsWritableService.isWritable()).thenAnswer(invocation -> callCount.incrementAndGet() > 4);

    Future<String> result = statusDelayedExecutor.submit(() -> "Done");

    await()
        .pollDelay(SLEEP_INTERVAL_MS / 2, MILLISECONDS)
        .atMost(2 * SLEEP_INTERVAL_MS, MILLISECONDS)
        .until(callCount::get, greaterThanOrEqualTo(1));

    assertThat(result.isDone(), is(false));

    await()
        .pollDelay(SLEEP_INTERVAL_MS / 2, MILLISECONDS)
        .atMost(10 * SLEEP_INTERVAL_MS, MILLISECONDS)
        .until(() -> result.isDone());

    assertThat(callCount.get(), is(5));
  }
}
