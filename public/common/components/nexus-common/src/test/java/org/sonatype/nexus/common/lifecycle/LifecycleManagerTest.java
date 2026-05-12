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
package org.sonatype.nexus.common.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.common.failure.MultipleFailures.MultipleFailuresException;
import org.sonatype.nexus.common.lifecycle.LifecycleSupport.State;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link LifecycleManager}.
 */
public class LifecycleManagerTest
{
  private static void assertState(final LifecycleSupport lifecycle, final State state) {
    assertTrue(lifecycle.is(state));
  }

  @Test
  public void addStartStopRemove() throws Exception {
    LifecycleManager underTest = new LifecycleManager();
    LifecycleSupport foo = new LifecycleSupport();
    LifecycleSupport bar = new LifecycleSupport();

    assertState(underTest, State.NEW);
    assertState(foo, State.NEW);
    assertState(bar, State.NEW);

    underTest.add(foo);
    assertThat(underTest.size(), is(1));

    underTest.add(bar);
    assertThat(underTest.size(), is(2));

    underTest.start();
    assertState(underTest, State.STARTED);
    assertState(foo, State.STARTED);
    assertState(bar, State.STARTED);

    underTest.stop();
    assertState(underTest, State.STOPPED);
    assertState(foo, State.STOPPED);
    assertState(bar, State.STOPPED);

    underTest.remove(foo);
    assertThat(underTest.size(), is(1));

    underTest.remove(bar);
    assertThat(underTest.size(), is(0));
  }

  @Test
  public void startStopOrdering() throws Exception {
    final List<LifecycleSupport> started = new ArrayList<>();
    final List<LifecycleSupport> stopped = new ArrayList<>();

    LifecycleManager underTest = new LifecycleManager();
    LifecycleSupport foo = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        started.add(this);
      }

      @Override
      protected void doStop() throws Exception {
        stopped.add(this);
      }
    };
    Lifecycle bar = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        started.add(this);
      }

      @Override
      protected void doStop() throws Exception {
        stopped.add(this);
      }
    };

    underTest.add(foo, bar);

    assertThat(started.size(), is(0));
    assertThat(stopped.size(), is(0));

    underTest.start();
    assertThat(started.size(), is(2));
    assertThat(started, contains(foo, bar));

    underTest.stop();
    assertThat(stopped.size(), is(2));
    assertThat(stopped, contains(bar, foo));
  }

  @Test
  public void startWithSingleFailure() throws Exception {
    LifecycleManager underTest = new LifecycleManager();
    LifecycleSupport foo = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        throw new TestException();
      }
    };
    LifecycleSupport bar = new LifecycleSupport();

    underTest.add(foo, bar);

    try {
      underTest.start();
    }
    catch (MultipleFailuresException e) {
      List<Throwable> failures = e.getFailures();
      assertThat(failures.size(), is(1));
      assertThat(failures.get(0), instanceOf(TestException.class));

      assertState(foo, State.FAILED);
      assertState(bar, State.STARTED);
    }
  }

  @Test
  public void startWithMultipleFailures() throws Exception {
    LifecycleManager underTest = new LifecycleManager();
    LifecycleSupport foo = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        throw new TestException();
      }
    };
    LifecycleSupport bar = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        throw new TestError();
      }
    };

    underTest.add(foo, bar);

    try {
      underTest.start();
    }
    catch (MultipleFailuresException e) {
      List<Throwable> failures = e.getFailures();
      assertThat(failures.size(), is(2));
      assertThat(failures.get(0), instanceOf(TestException.class));
      assertThat(failures.get(1), instanceOf(TestError.class));

      assertState(foo, State.FAILED);
      assertState(bar, State.FAILED);
    }
  }

  @Test
  public void stopWithSingleFailure() throws Exception {
    LifecycleManager underTest = new LifecycleManager();
    LifecycleSupport foo = new LifecycleSupport()
    {
      @Override
      protected void doStop() throws Exception {
        throw new TestException();
      }
    };
    LifecycleSupport bar = new LifecycleSupport();

    underTest.add(foo, bar);

    underTest.start();

    try {
      underTest.stop();
    }
    catch (MultipleFailuresException e) {
      List<Throwable> failures = e.getFailures();
      assertThat(failures.size(), is(1));
      assertThat(failures.get(0), instanceOf(TestException.class));

      assertState(foo, State.FAILED);
      assertState(bar, State.STOPPED);
    }
  }

  @Test
  public void stopWithMultipleFailures() throws Exception {
    LifecycleManager underTest = new LifecycleManager();
    LifecycleSupport foo = new LifecycleSupport()
    {
      @Override
      protected void doStop() throws Exception {
        throw new TestException();
      }
    };
    LifecycleSupport bar = new LifecycleSupport()
    {
      @Override
      protected void doStop() throws Exception {
        throw new TestError();
      }
    };

    underTest.add(foo, bar);

    underTest.start();

    try {
      underTest.stop();
    }
    catch (MultipleFailuresException e) {
      List<Throwable> failures = e.getFailures();
      assertThat(failures.size(), is(2));
      assertThat(failures.get(0), instanceOf(TestError.class));
      assertThat(failures.get(1), instanceOf(TestException.class));

      assertState(foo, State.FAILED);
      assertState(bar, State.FAILED);
    }
  }
}
