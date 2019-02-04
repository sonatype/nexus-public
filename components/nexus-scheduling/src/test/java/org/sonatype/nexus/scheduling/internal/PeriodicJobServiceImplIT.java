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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link PeriodicJobServiceImpl}
 */
public class PeriodicJobServiceImplIT
    extends TestSupport
{
  PeriodicJobServiceImpl underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new PeriodicJobServiceImpl();
    underTest.start();
  }

  @Test
  public void numberIncrementingTask() throws Exception {
    final AtomicInteger counter = new AtomicInteger();

    PeriodicJob schedule = underTest.schedule(() -> {
      counter.incrementAndGet();
    }, 1);

    Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> counter.get() > 0);

    schedule.cancel();
  }

  @After
  public void stop() throws Exception {
    if (underTest != null) {
      underTest.stop();
    }
  }
}
