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

import org.sonatype.nexus.common.lifecycle.LifecycleSupport.State;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link LifecycleSupport}.
 */
public class LifecycleSupportTest
{
  private static void assertState(final LifecycleSupport lifecycle, final State state) {
    assertTrue(lifecycle.is(state));
    final boolean[] resultHolder = new boolean[1];
    Thread asyncAssert = new Thread(new Runnable()
    {
      @Override
      public void run() {
        resultHolder[0] = lifecycle.is(state);
      }
    });
    try {
      asyncAssert.start();
      asyncAssert.join(2_000);
      assertTrue(resultHolder[0]);
    }
    catch (Exception e) {
      fail("Unable to check state asynchronously" + e);
    }
  }

  @Test
  public void startStopStartStop() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport();

    assertState(underTest, State.NEW);

    underTest.start();
    assertState(underTest, State.STARTED);

    underTest.stop();
    assertState(underTest, State.STOPPED);

    underTest.start();
    assertState(underTest, State.STARTED);

    underTest.stop();
    assertState(underTest, State.STOPPED);
  }

  @Test
  public void stopBeforeStartDisallowed() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport();

    assertState(underTest, State.NEW);

    try {
      underTest.stop();
    }
    catch (IllegalStateException e) {
      // expected
    }

    assertState(underTest, State.NEW);
  }

  @Test
  public void startAfterStartDisallowed() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport();

    assertState(underTest, State.NEW);

    underTest.start();

    assertState(underTest, State.STARTED);

    try {
      underTest.start();
    }
    catch (IllegalStateException e) {
      // expected
    }

    assertState(underTest, State.STARTED);
  }

  @Test
  public void stopAfterStopDisallowed() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport();

    assertState(underTest, State.NEW);

    underTest.start();

    assertState(underTest, State.STARTED);

    underTest.stop();

    assertState(underTest, State.STOPPED);

    try {
      underTest.stop();
    }
    catch (IllegalStateException e) {
      // expected
    }

    assertState(underTest, State.STOPPED);
  }

  @Test
  public void startAfterFailureDisallowed() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        throw new TestException();
      }
    };

    assertState(underTest, State.NEW);

    try {
      underTest.start();
    }
    catch (TestException e) {
      // expected
    }

    assertState(underTest, State.FAILED);

    try {
      underTest.start();
    }
    catch (IllegalStateException e) {
      // expected
    }

    assertState(underTest, State.FAILED);
  }

  @Test
  public void stopAfterFailureDisallowed() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        throw new TestException();
      }
    };

    assertState(underTest, State.NEW);

    try {
      underTest.start();
    }
    catch (TestException e) {
      // expected
    }

    assertState(underTest, State.FAILED);

    try {
      underTest.stop();
    }
    catch (IllegalStateException e) {
      // expected
    }

    assertState(underTest, State.FAILED);
  }

  @Test
  public void startException() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        throw new TestException();
      }
    };

    try {
      underTest.start();
      fail();
    }
    catch (TestException e) {
      // expected
    }

    assertState(underTest, State.FAILED);
  }

  @Test
  public void startError() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport()
    {
      @Override
      protected void doStart() throws Exception {
        throw new TestError();
      }
    };

    try {
      underTest.start();
      fail();
    }
    catch (TestError e) {
      // expected
    }

    assertState(underTest, State.FAILED);
  }

  @Test
  public void stopException() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport()
    {
      @Override
      protected void doStop() throws Exception {
        throw new TestException();
      }
    };

    underTest.start();
    try {
      underTest.stop();
      fail();
    }
    catch (TestException e) {
      // expected
    }

    assertState(underTest, State.FAILED);
  }

  @Test
  public void stopError() throws Exception {
    LifecycleSupport underTest = new LifecycleSupport()
    {
      @Override
      protected void doStop() throws Exception {
        throw new TestError();
      }
    };

    underTest.start();
    try {
      underTest.stop();
      fail();
    }
    catch (TestError e) {
      // expected
    }

    assertState(underTest, State.FAILED);
  }
}
