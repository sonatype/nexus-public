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
package org.sonatype.nexus.scheduling.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.PeriodicJobService.PeriodicJob;

import com.jayway.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link PeriodicJobServiceImpl}
 */
public class PeriodicJobServiceImplTest
    extends TestSupport
{
  private PeriodicJobServiceImpl service;

  @Before
  public void setUp() {
    service = new PeriodicJobServiceImpl();
  }

  private boolean isRunning() {
    try {
      service.schedule(() -> {} , 60).cancel();
      return true;
    }
    catch (IllegalStateException | NullPointerException e) {
      return false;
    }
  }

  @Test
  public void numberIncrementingTask() throws Exception {
    service.startUsing();

    final AtomicInteger counter = new AtomicInteger();

    PeriodicJob schedule = service.schedule(counter::incrementAndGet, 1);

    Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> counter.get() > 0);

    schedule.cancel();

    service.stopUsing();
  }

  @Test
  public void testConditionalLifecycle() throws Exception {
    assertThat(isRunning(), is(false));
    service.startUsing();
    assertThat(isRunning(), is(true));
    service.startUsing();
    assertThat(isRunning(), is(true));
    service.stopUsing();
    assertThat(isRunning(), is(true));
    service.stopUsing();
    assertThat(isRunning(), is(false));
  }

  @Test(expected = IllegalStateException.class)
  public void testStopUsing_InvalidPrecondition() throws Exception {
    service.stopUsing();
  }
}
