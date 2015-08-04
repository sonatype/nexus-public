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
package org.sonatype.nexus.proxy.maven.routing.internal.task;

import java.util.concurrent.Executors;

import org.sonatype.nexus.proxy.maven.routing.internal.task.executor.ConstrainedExecutor;
import org.sonatype.nexus.proxy.maven.routing.internal.task.executor.ConstrainedExecutorImpl;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ConstrainedExecutorImplTest
    extends TestSupport
{
  @Test
  public void smoke()
      throws Exception
  {
    final ConstrainedExecutor ci = new ConstrainedExecutorImpl(Executors.newFixedThreadPool(2));

    final TestWorker tw = new TestWorker("1", 5);
    ci.mayExecute("foo", tw);
    tw.waitForExecuted();
    assertThat(tw.isJobDone(), equalTo(true));
    assertThat(tw.isExecuting(), equalTo(false));
    assertThat(tw.isCanceled(), equalTo(false));
    assertThat(ci.getStatistics().getCurrentlyRunningJobKeys(), is(empty()));
  }

  @Test
  public void manyMayExecuteAndOnlyOneDoesIt()
      throws Exception
  {
    final ConstrainedExecutor ci = new ConstrainedExecutorImpl(Executors.newFixedThreadPool(2));

    final TestWorker tw1 = new TestWorker("1", 100);
    final TestWorker tw2 = new TestWorker("2", 10);
    final TestWorker tw3 = new TestWorker("3", 10);
    final TestWorker tw4 = new TestWorker("4", 10);
    final TestWorker tw5 = new TestWorker("5", 10);
    assertThat(ci.mayExecute("foo", tw1), is(true));
    assertThat(ci.mayExecute("foo", tw2), is(false));
    assertThat(ci.mayExecute("foo", tw3), is(false));
    assertThat(ci.mayExecute("foo", tw4), is(false));
    assertThat(ci.mayExecute("foo", tw5), is(false));

    tw1.waitForExecuted();

    // did the job
    assertThat(tw1.isJobDone(), equalTo(true));
    assertThat(tw1.isExecuting(), equalTo(false));
    assertThat(tw1.isExecuted(), equalTo(true));
    assertThat(tw1.isCanceled(), equalTo(false));

    // was "forgotten", not even invoked
    assertThat(tw2.isJobDone(), equalTo(false));
    assertThat(tw2.isExecuting(), equalTo(false));
    assertThat(tw2.isExecuted(), equalTo(false));
    assertThat(tw2.isCanceled(), equalTo(false));

    // was "forgotten", not even invoked
    assertThat(tw3.isJobDone(), equalTo(false));
    assertThat(tw3.isExecuting(), equalTo(false));
    assertThat(tw3.isExecuted(), equalTo(false));
    assertThat(tw3.isCanceled(), equalTo(false));

    // was "forgotten", not even invoked
    assertThat(tw4.isJobDone(), equalTo(false));
    assertThat(tw4.isExecuting(), equalTo(false));
    assertThat(tw4.isExecuted(), equalTo(false));
    assertThat(tw4.isCanceled(), equalTo(false));

    // was "forgotten", not even invoked
    assertThat(tw5.isJobDone(), equalTo(false));
    assertThat(tw5.isExecuting(), equalTo(false));
    assertThat(tw5.isExecuted(), equalTo(false));
    assertThat(tw5.isCanceled(), equalTo(false));

    assertThat(ci.getStatistics().getCurrentlyRunningJobKeys(), is(empty()));
  }

  @Test
  public void manyMustExecuteAndOnlyOneDoesIt()
      throws Exception
  {
    final ConstrainedExecutor ci = new ConstrainedExecutorImpl(Executors.newFixedThreadPool(2));

    final TestWorker tw1 = new TestWorker("1", 50000);
    final TestWorker tw2 = new TestWorker("2", 50000);
    final TestWorker tw3 = new TestWorker("3", 50000);
    final TestWorker tw4 = new TestWorker("4", 50000);
    final TestWorker tw5 = new TestWorker("5", 10);

    assertThat(ci.mustExecute("foo", tw1), equalTo(false));
    Thread.sleep(150); // give some time to worker to start
    assertThat(ci.mustExecute("foo", tw2), equalTo(true));
    Thread.sleep(150); // give some time to worker to start
    assertThat(ci.mustExecute("foo", tw3), equalTo(true));
    Thread.sleep(150); // give some time to worker to start
    assertThat(ci.mustExecute("foo", tw4), equalTo(true));
    Thread.sleep(150); // give some time to worker to start
    assertThat(ci.mustExecute("foo", tw5), equalTo(true));

    tw5.waitForExecuted();

    // here, due to concurrency, all other workers
    // except tw5 might not even be executed, as
    // they were canceled before execution
    // as new mustExecute invocation happened
    // so, don't test the isExecuted only
    // on tw5

    // was canceled when running
    assertThat(tw1.isJobDone(), equalTo(false));
    assertThat(tw1.isExecuting(), equalTo(false));
    // assertThat( tw1.isExecuted(), equalTo( true ) );
    assertThat(tw1.isCanceled(), equalTo(true));

    // was canceled when running
    assertThat(tw2.isJobDone(), equalTo(false));
    assertThat(tw2.isExecuting(), equalTo(false));
    // assertThat( tw2.isExecuted(), equalTo( true ) );
    assertThat(tw2.isCanceled(), equalTo(true));

    // was canceled when running
    assertThat(tw3.isJobDone(), equalTo(false));
    assertThat(tw3.isExecuting(), equalTo(false));
    // assertThat( tw3.isExecuted(), equalTo( true ) );
    assertThat(tw3.isCanceled(), equalTo(true));

    // was canceled when running
    assertThat(tw4.isJobDone(), equalTo(false));
    assertThat(tw4.isExecuting(), equalTo(false));
    // assertThat( tw4.isExecuted(), equalTo( true ) );
    assertThat(tw4.isCanceled(), equalTo(true));

    // was not canceled and did it
    assertThat(tw5.isJobDone(), equalTo(true));
    assertThat(tw5.isExecuting(), equalTo(false));
    assertThat(tw5.isExecuted(), equalTo(true));
    assertThat(tw5.isCanceled(), equalTo(false));

    assertThat(ci.getStatistics().getCurrentlyRunningJobKeys(), is(empty()));
  }

  @Test
  public void cancellAll()
      throws Exception
  {
    final ConstrainedExecutor ci = new ConstrainedExecutorImpl(Executors.newFixedThreadPool(5));

    final TestWorker tw1 = new TestWorker("1", 50000);
    final TestWorker tw2 = new TestWorker("2", 50000);
    final TestWorker tw3 = new TestWorker("3", 50000);
    final TestWorker tw4 = new TestWorker("4", 50000);
    final TestWorker tw5 = new TestWorker("5", 50000);

    assertThat(ci.mayExecute("foo1", tw1), equalTo(true));
    assertThat(ci.mayExecute("foo2", tw2), equalTo(true));
    assertThat(ci.mayExecute("foo3", tw3), equalTo(true));
    assertThat(ci.mayExecute("foo4", tw4), equalTo(true));
    assertThat(ci.mayExecute("foo5", tw5), equalTo(true));

    // give time to executor to start them
    Thread.sleep(1000);

    assertThat(ci.hasRunningWithKey("foo1"), is(true));
    assertThat(ci.hasRunningWithKey("foo3"), is(true));
    assertThat(ci.hasRunningWithKey("fooX"), is(false));

    ci.cancelAllJobs();

    tw1.waitForExecuted();
    tw2.waitForExecuted();
    tw3.waitForExecuted();
    tw4.waitForExecuted();
    tw5.waitForExecuted();

    // was canceled when running
    assertThat(tw1.isJobDone(), equalTo(false));
    assertThat(tw1.isExecuting(), equalTo(false));
    assertThat(tw1.isExecuted(), equalTo(true));
    assertThat(tw1.isCanceled(), equalTo(true));

    // was canceled when running
    assertThat(tw2.isJobDone(), equalTo(false));
    assertThat(tw2.isExecuting(), equalTo(false));
    assertThat(tw2.isExecuted(), equalTo(true));
    assertThat(tw2.isCanceled(), equalTo(true));

    // was canceled when running
    assertThat(tw3.isJobDone(), equalTo(false));
    assertThat(tw3.isExecuting(), equalTo(false));
    assertThat(tw3.isExecuted(), equalTo(true));
    assertThat(tw3.isCanceled(), equalTo(true));

    // was canceled when running
    assertThat(tw4.isJobDone(), equalTo(false));
    assertThat(tw4.isExecuting(), equalTo(false));
    assertThat(tw4.isExecuted(), equalTo(true));
    assertThat(tw4.isCanceled(), equalTo(true));

    // was not canceled and did it
    assertThat(tw5.isJobDone(), equalTo(false));
    assertThat(tw5.isExecuting(), equalTo(false));
    assertThat(tw5.isExecuted(), equalTo(true));
    assertThat(tw5.isCanceled(), equalTo(true));

    assertThat(ci.getStatistics().getCurrentlyRunningJobKeys(), is(empty()));
  }

  // ==

  private static class TestWorker
      extends CancelableRunnableSupport
  {
    private final int cycles;

    private volatile boolean executing;

    private volatile boolean executed;

    private volatile boolean jobDone;

    protected TestWorker(final String name, final int cycles) {
      super(null, name);
      this.cycles = cycles;
      this.executing = false;
      this.executed = false;
      this.jobDone = false;
    }

    public boolean isJobDone() {
      return jobDone;
    }

    public boolean isExecuting() {
      return executing;
    }

    public boolean isExecuted() {
      return executed;
    }

    public void waitForExecuted()
        throws InterruptedException
    {
      while (!isExecuted()) {
        Thread.sleep(500);
      }
    }

    @Override
    protected void doRun()
        throws Exception
    {
      try {
        executing = true;
        // the work loop
        for (int i = 0; i < cycles; i++) {
          CancelableUtil.checkInterruption();
          Thread.sleep(50);
        }
        jobDone = true;
      }
      finally {
        executing = false;
        executed = true;
      }
    }
  }
}
