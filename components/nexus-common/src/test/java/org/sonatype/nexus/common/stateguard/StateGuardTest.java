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
package org.sonatype.nexus.common.stateguard;

import java.util.function.Supplier;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.common.stateguard.StateGuardTest.State.DESTROYED;
import static org.sonatype.nexus.common.stateguard.StateGuardTest.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardTest.State.INITIALISED;
import static org.sonatype.nexus.common.stateguard.StateGuardTest.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardTest.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardTest.State.STOPPED;

/**
 * Tests for {@link StateGuard}.
 */
public class StateGuardTest
    extends TestSupport
{
  private StateGuard underTest;

  @Before
  public void setUp() {
    underTest = new StateGuard.Builder()
        .initial(NEW)
        .failure(FAILED)
        .create();

    assertThat(underTest.getCurrent(), is(NEW));
  }

  @Test
  public void testBasicTransition() throws Exception {
    TriggeredAction action = new TriggeredAction();
    underTest.transition(INITIALISED)
        .from(NEW)
        .run(action);

    assertThat(underTest.getCurrent(), is(INITIALISED));
    assertTrue(action.triggered);
  }

  @Test
  public void testNestedTransition() throws Exception {
    underTest.transition(INITIALISED)
        .from(NEW)
        .run(new NopAction());
    assertThat(underTest.getCurrent(), is(INITIALISED));

    underTest.transition(STARTED)
        .from(INITIALISED)
        .run(new NopAction());
    assertThat(underTest.getCurrent(), is(STARTED));

    TriggeredAction stoppedAction = new TriggeredAction();

    boolean[] triggered = new boolean[1];
    Action<?> destroyedAction = new Action<Void>()
    {
      @Override
      public Void run() throws Exception {
        if (underTest.is(STARTED)) {
          underTest.transition(STOPPED)
              .from(STARTED)
              .run(stoppedAction);
        }
        assertThat(underTest.getCurrent(), is(STOPPED));
        triggered[0] = true;
        return null;
      }
    };

    underTest.transition(DESTROYED)
        .run(destroyedAction);

    assertThat(underTest.getCurrent(), is(DESTROYED));
    assertTrue(stoppedAction.triggered);
    assertTrue(triggered[0]);
  }

  @Test
  public void testInvalidTransition() {
    TriggeredAction action = new TriggeredAction();

    Transition transition = underTest.transition(STARTED).from(INITIALISED);
    assertThrows(IllegalStateException.class, () -> transition.run(action));

    assertThat(underTest.getCurrent(), is(NEW));
    assertFalse(action.triggered);
  }

  /*
   * transition with action failing with exception
   */
  @Test
  public void testTransitionWithActionFailingWithException() {
    Transition transition = underTest.transition(INITIALISED).from(NEW);
    assertThrows(FailureException.class, () -> transition.run(new Action<Void>()
    {
      @Override
      public Void run() throws Exception {
        throw new FailureException();
      }
    }));

    assertThat(underTest.getCurrent(), is(FAILED));
  }

  @Test
  public void testTransitionWithActionIgnoringException() {
    Transition transition =
        underTest.transition(INITIALISED, false, new Class[]{FailureException.class}, true).from(NEW);
    Action<Void> action = new Action<Void>()
    {
      @Override
      public Void run() throws Exception {
        throw new FailureException();
      }
    };
    assertThrows(FailureException.class, () -> transition.run(action));

    assertThat(underTest.getCurrent(), is(INITIALISED));
  }

  @Test
  public void testTransition_withActionFailingWithError() {
    Transition transition = underTest.transition(INITIALISED).from(NEW);
    Action<Void> action = new Action<Void>()
    {
      @Override
      public Void run() throws Exception {
        throw new FailureError();
      }
    };
    assertThrows(FailureError.class, () -> transition.run(action));

    assertThat(underTest.getCurrent(), is(FAILED));
  }

  @Test
  public void testTransition_withMethodInvocationFailingWithException() {
    SimpleMethodInvocation invocation = invocation(FailureException::new);
    TransitionsInterceptor interceptor = new TransitionsInterceptor();
    assertThrows(FailureException.class, () -> interceptor.invoke(invocation));

    assertThat(underTest.getCurrent(), is(FAILED));
  }

  @Test
  public void testTransition_withMethodInvocationFailingWithError() {
    SimpleMethodInvocation invocation = invocation(FailureError::new);
    TransitionsInterceptor interceptor = new TransitionsInterceptor();
    assertThrows(FailureError.class, () -> interceptor.invoke(invocation));

    assertThat(underTest.getCurrent(), is(FAILED));
  }

  @Test
  public void testBasicGuard() throws Exception {
    TriggeredAction action = new TriggeredAction();
    underTest.guard(NEW)
        .run(action);

    assertThat(underTest.getCurrent(), is(NEW));
    assertTrue(action.triggered);
  }

  @Test
  public void testInvalidGuard() {
    TriggeredAction action = new TriggeredAction();

    Guard guard = underTest.guard(INITIALISED);
    assertThrows(IllegalStateException.class, () -> guard.run(action));

    assertThat(underTest.getCurrent(), is(NEW));
    assertFalse(action.triggered);
  }

  @Test
  public void testGuard_withMethodInvocationFailingWithException() {
    SimpleMethodInvocation invocation = invocation(FailureException::new);
    GuardedInterceptor interceptor = new GuardedInterceptor();
    assertThrows(FailureException.class, () -> interceptor.invoke(invocation));

    assertThat(underTest.getCurrent(), is(NEW));
  }

  @Test
  public void testGuard_withMethodInvocationFailingWithError() {
    SimpleMethodInvocation invocation = invocation(FailureError::new);
    GuardedInterceptor interceptor = new GuardedInterceptor();
    assertThrows(FailureError.class, () -> interceptor.invoke(invocation));

    assertThat(underTest.getCurrent(), is(NEW));
  }

  private SimpleMethodInvocation invocation(final Supplier<? extends Throwable> supplier) {
    try {
      return new SimpleMethodInvocation(new Target(supplier), Target.class.getMethod("go"), new Object[0]);
    }
    catch (NoSuchMethodException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  static class State
  {
    public static final String NEW = "NEW";

    public static final String INITIALISED = "INITIALISED";

    public static final String STARTED = "STARTED";

    public static final String STOPPED = "STOPPED";

    public static final String DESTROYED = "DESTROYED";

    public static final String FAILED = "FAILED";
  }

  private static class TriggeredAction
      implements Action<Void>
  {
    boolean triggered = false;

    @Override
    public Void run() throws Exception {
      triggered = true;
      return null;
    }
  }

  private static class NopAction
      implements Action<Void>
  {
    @Override
    public Void run() throws Exception {
      return null;
    }
  }

  private class Target
      implements StateGuardAware
  {
    Supplier<? extends Throwable> failureType;

    public Target(final Supplier<? extends Throwable> failureType) {
      this.failureType = failureType;
    }

    @Override
    public StateGuard getStateGuard() {
      return underTest;
    }

    @Guarded(by = NEW)
    @Transitions(from = NEW, to = INITIALISED)
    public void go() throws Throwable {
      if (failureType != null) {
        throw failureType.get();
      }
    }
  }

  private static class FailureException
      extends Exception
  {
    FailureException() {
      super("FAILED");
    }
  }

  private static class FailureError
      extends Error
  {
    FailureError() {
      super("FAILED");
    }
  }
}
