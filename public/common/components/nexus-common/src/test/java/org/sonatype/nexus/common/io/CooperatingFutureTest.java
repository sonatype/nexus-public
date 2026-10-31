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
package org.sonatype.nexus.common.io;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.sonatype.nexus.common.io.CooperationFactorySupport.Config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CooperatingFuture}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CooperatingFutureTest
{
  @Mock
  private Config config;

  private Logger logger;

  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void setUp() {
    logger = (Logger) LoggerFactory.getLogger(CooperatingFuture.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
    logger.setLevel(Level.DEBUG);
  }

  @After
  public void tearDown() {
    logger.detachAppender(listAppender);
  }

  @Test
  public void downloadTimeoutsAreStaggered() {
    CooperatingFuture<String> cooperatingFuture = new CooperatingFuture<>("testKey", config);

    Random random = new Random();
    long[] downloadTimeMillis = new long[10];
    long expectedGap = 200;

    downloadTimeMillis[0] = System.currentTimeMillis(); // first download
    for (int i = 1; i < downloadTimeMillis.length; i++) {

      // random sleep representing some client-side work
      LockSupport.parkNanos(Duration.ofMillis(random.nextInt((int) expectedGap)).toNanos());

      // staggered sleep should bring us close to the expected gap
      LockSupport.parkNanos(cooperatingFuture.staggerTimeout(Duration.ofMillis(expectedGap)).toNanos());

      downloadTimeMillis[i] = System.currentTimeMillis(); // next download
    }

    for (int i = 1; i < downloadTimeMillis.length; i++) {
      long actualGap = downloadTimeMillis[i] - downloadTimeMillis[i - 1];
      assertThat(actualGap, allOf(greaterThanOrEqualTo(expectedGap - 25), lessThanOrEqualTo(expectedGap + 75)));
    }
  }

  @Test
  public void threadCooperationLimitExceededWarnLogFiresOncePerFuture() throws Exception {
    int limit = 2;
    when(config.threadsPerKey()).thenReturn(limit);

    // Create a testable subclass that exposes the internal methods
    TestableCooperatingFuture future = new TestableCooperatingFuture("testKey", config);

    // Set threadCount to the limit
    future.setThreadCount(limit);

    // First cooperation attempt beyond limit should throw and log warning
    try {
      future.simulateCooperationFailure();
      throw new AssertionError("Expected CooperationException");
    }
    catch (CooperationException e) {
      // Expected
      assertThat(e.getMessage(), containsString("Thread cooperation maxed"));
    }

    // Verify warning was logged once
    assertThat(listAppender.list.stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .filter(event -> event.getFormattedMessage().contains("Thread cooperation limit"))
        .count(), equalTo(1L));

    // Second cooperation attempt should also throw but NOT log warning again (one-time)
    try {
      future.simulateCooperationFailure();
      throw new AssertionError("Expected CooperationException");
    }
    catch (CooperationException e) {
      // Expected
    }

    // Verify warning was still only logged once
    assertThat(listAppender.list.stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .filter(event -> event.getFormattedMessage().contains("Thread cooperation limit"))
        .count(), equalTo(1L));
  }

  @Test
  public void debugLogFiresOnEachLimitExceeded() throws Exception {
    int limit = 2;
    when(config.threadsPerKey()).thenReturn(limit);

    TestableCooperatingFuture future = new TestableCooperatingFuture("testKey", config);
    future.setThreadCount(limit);

    // First failure
    try {
      future.simulateCooperationFailure();
    }
    catch (CooperationException expected) {
    }

    // Second failure
    try {
      future.simulateCooperationFailure();
    }
    catch (CooperationException expected) {
    }

    // Debug log should have fired twice (one per failure)
    assertThat(listAppender.list.stream()
        .filter(event -> event.getLevel() == Level.DEBUG)
        .filter(event -> event.getFormattedMessage().contains("Thread cooperation maxed"))
        .count(), equalTo(2L));
  }

  /**
   * Testable subclass that allows manipulating internal state.
   */
  private static class TestableCooperatingFuture
      extends CooperatingFuture<String>
  {
    TestableCooperatingFuture(final String requestKey, final Config config) {
      super(requestKey, config);
    }

    void setThreadCount(final int count) throws Exception {
      // Use reflection to set the thread count for testing
      Field field = CooperatingFuture.class.getDeclaredField("threadCount");
      field.setAccessible(true);
      AtomicInteger atomicCount = (AtomicInteger) field.get(this);
      atomicCount.set(count);
    }

    void simulateCooperationFailure() throws Exception {
      // Use reflection to call the private increaseCooperation method
      Method method = CooperatingFuture.class.getDeclaredMethod("increaseCooperation");
      method.setAccessible(true);
      try {
        method.invoke(this);
      }
      catch (InvocationTargetException e) {
        if (e.getCause() instanceof CooperationException) {
          // Simulate what cooperate() does: log warning then re-throw
          Method logMethod = CooperatingFuture.class.getDeclaredMethod("logLimitExceededWarning");
          logMethod.setAccessible(true);
          logMethod.invoke(this);
          throw (CooperationException) e.getCause();
        }
        throw e;
      }
    }
  }
}
