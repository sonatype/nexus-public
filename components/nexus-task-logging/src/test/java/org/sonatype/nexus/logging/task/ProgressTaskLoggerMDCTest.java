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
package org.sonatype.nexus.logging.task;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Special test to cover MDC thread specifics. See NEXUS-14432 and https://logback.qos.ch/manual/mdc.html#managedThreads
 * (tldr: logback with MDC and thread pools doesn't copy the MDC values so we have to do it manually)
 */
public class ProgressTaskLoggerMDCTest
    extends TestSupport
{
  @Mock
  private Logger mockLogger;

  @Test
  public void testMDCCopy() throws InterruptedException {
    String mainThread = Thread.currentThread().getName();

    // put something into MDC in current thread
    MDC.put("foo", "bar");

    // since we are testing with threads, we need to track that the inner thread ran through
    AtomicBoolean tested = new AtomicBoolean(false);

    // create progress task logger with 1ms start delay (i.e. start immediately). Interval is not relevant for this
    // test.
    ProgressTaskLogger progressTaskLogger = new ProgressTaskLogger(mockLogger, 1, 60000, TimeUnit.MILLISECONDS)
    {
      void logProgress() {
        super.logProgress();
        assertThat(mainThread, not(equalTo(Thread.currentThread().getName()))); // verify we are in a different thread
        assertThat(MDC.get("foo"), equalTo("bar"));
        tested.set(true);
      }
    };

    // store a progress message before start so it is there immediately
    progressTaskLogger.progress(new TaskLoggingEvent(mockLogger, "test message"));

    progressTaskLogger.start();

    Thread.sleep(100); // wait for the execution to complete

    // assert that the test ran
    assertTrue(tested.get());

    progressTaskLogger.finish();
  }
}
