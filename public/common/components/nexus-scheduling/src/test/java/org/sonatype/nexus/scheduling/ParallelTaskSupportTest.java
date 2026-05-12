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
package org.sonatype.nexus.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.sonatype.nexus.common.failure.MultipleFailures.MultipleFailuresException;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ParallelTaskSupport}
 */
@ExtendWith(MockitoExtension.class)
class ParallelTaskSupportTest

{
  @Mock
  SecurityManager securityManager;

  AtomicInteger counter = new AtomicInteger(0);

  @BeforeEach
  void setup() {
    ThreadContext.bind(securityManager);
    counter.set(0);
  }

  @AfterEach
  void teardown() {
    ThreadContext.unbindSecurityManager();
  }

  @Test
  void testExecute() throws Exception {
    TestParallelTask task = new TestParallelTask(5, 10, false);

    Object result = task.execute();

    assertThat(result, is("completed"));
    assertThat(counter.get(), is(10));
  }

  @Test
  void testExecute_singleException() {
    TestParallelTask task = new TestParallelTask(5, 5, true, 2);

    MultipleFailuresException exception = assertThrows(MultipleFailuresException.class, task::execute);

    // Should have processed all jobs even though one failed
    assertThat(counter.get(), greaterThanOrEqualTo(4));
    // Should have collected the exception
    assertThat(exception.getFailures(), hasSize(1));
    assertThat(exception.getFailures().get(0).getMessage(), containsString("Job 2 failed"));
  }

  @Test
  void testExecute_multipleExceptions() {
    TestParallelTask task = new TestParallelTask(5, 10, true, 2, 4, 6);

    MultipleFailuresException exception = assertThrows(MultipleFailuresException.class, task::execute);

    // Should have processed all jobs
    assertThat(counter.get(), greaterThanOrEqualTo(7));
    // Should have collected all 3 exceptions
    assertThat(exception.getFailures(), hasSize(3));
  }

  @Test
  void testExecute_max20Exceptions() {
    // Create 25 jobs that all throw exceptions
    int[] failingJobs = IntStream.range(0, 25).toArray();
    TestParallelTask task = new TestParallelTask(10, 25, true, failingJobs);

    MultipleFailuresException exception = assertThrows(MultipleFailuresException.class, task::execute);

    // Should only collect 20 exceptions, rest should be logged
    assertThat(exception.getFailures(), hasSize(20));
  }

  @Test
  void testExecute_resultExceptionPropagates() {
    TestParallelTask task = new TestParallelTask(5, 5, false)
    {
      @Override
      protected Object result() {
        throw new RuntimeException("result() failed");
      }
    };

    RuntimeException exception = assertThrows(RuntimeException.class, task::execute);
    assertThat(exception.getMessage(), containsString("result() failed"));
  }

  private class TestParallelTask
      extends ParallelTaskSupport
  {
    private final int jobCount;

    private final boolean throwExceptions;

    private final List<Integer> failingJobIndexes;

    TestParallelTask(
        final int concurrencyLimit,
        final int jobCount,
        final boolean throwExceptions,
        final int... failingJobIndexes)
    {
      super(concurrencyLimit);
      this.jobCount = jobCount;
      this.throwExceptions = throwExceptions;
      this.failingJobIndexes = new ArrayList<>();
      for (int index : failingJobIndexes) {
        this.failingJobIndexes.add(index);
      }
    }

    @Override
    protected Object result() {
      return "completed";
    }

    @Override
    protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress) {
      return IntStream.range(0, jobCount)
          .mapToObj(i -> (Runnable) () -> {
            counter.incrementAndGet();
            if (throwExceptions && failingJobIndexes.contains(i)) {
              throw new RuntimeException("Job " + i + " failed");
            }
          });
    }

    @Override
    public String getMessage() {
      return "";
    }
  }
}
