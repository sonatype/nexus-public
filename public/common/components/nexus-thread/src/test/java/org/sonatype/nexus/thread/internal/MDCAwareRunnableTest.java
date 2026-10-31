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
package org.sonatype.nexus.thread.internal;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests that {@link MDCAwareRunnable} propagates the submitting thread's Shiro {@link ThreadContext}
 * to the managed worker thread. Shiro 2.x changed ThreadContext from an InheritableThreadLocal to a
 * plain ThreadLocal, so worker threads no longer inherit the SecurityManager/Subject automatically;
 * MDCAwareRunnable must carry it across explicitly.
 */
public class MDCAwareRunnableTest
{
  private final SecurityManager securityManager = new DefaultSecurityManager();

  @After
  public void tearDown() {
    ThreadContext.remove();
  }

  @Test
  public void propagatesThreadContextToWorkerThreadAndClearsAfterRun() throws Exception {
    // bind a SecurityManager on the submitting thread, then build the runnable (captures context here)
    ThreadContext.bind(securityManager);
    AtomicReference<SecurityManager> seenDuringRun = new AtomicReference<>();
    MDCAwareRunnable runnable =
        new MDCAwareRunnable(() -> seenDuringRun.set(ThreadContext.getSecurityManager()));

    // execute on a separate (worker) thread which does NOT inherit the submitter's ThreadContext
    AtomicReference<SecurityManager> seenAfterRun = new AtomicReference<>();
    Thread worker = new Thread(() -> {
      runnable.run();
      seenAfterRun.set(ThreadContext.getSecurityManager());
    });
    worker.start();
    worker.join();

    assertSame("worker thread must see the submitting thread's SecurityManager during run",
        securityManager, seenDuringRun.get());
    assertNull("worker ThreadContext must be cleared after run to avoid pooled-thread leakage",
        seenAfterRun.get());
  }

  @Test
  public void preservesWorkerPreExistingContext() throws Exception {
    // A worker that already carries its own Subject/SecurityManager (e.g. the fixed TASK_SUBJECT bound
    // by NexusExecutorService.forFixedSubject via subject.associateWith) must NOT have it replaced by
    // the submitting thread's captured context — otherwise the task runs as the wrong subject.
    ThreadContext.bind(securityManager);
    AtomicReference<SecurityManager> seenDuringRun = new AtomicReference<>();
    MDCAwareRunnable runnable =
        new MDCAwareRunnable(() -> seenDuringRun.set(ThreadContext.getSecurityManager()));

    SecurityManager workerExisting = new DefaultSecurityManager();
    AtomicReference<SecurityManager> seenAfterRun = new AtomicReference<>();
    Thread worker = new Thread(() -> {
      ThreadContext.bind(workerExisting);
      runnable.run();
      seenAfterRun.set(ThreadContext.getSecurityManager());
    });
    worker.start();
    worker.join();

    assertSame("worker's pre-existing fixed Subject must not be overridden during the run",
        workerExisting, seenDuringRun.get());
    assertSame("worker's pre-existing SecurityManager must remain bound after the run",
        workerExisting, seenAfterRun.get());
  }

  @Test
  public void runsDelegateWhenSubmitterHadNoThreadBoundContext() throws Exception {
    // Nothing bound on the submitting thread's ThreadContext, so there is nothing to capture and the
    // boundContext=false path is taken: the worker must run the delegate without binding any context.
    // A VM-static SecurityManager (as production sets at startup) lets MDC subject-resolution succeed
    // without a thread binding; ThreadContext itself stays empty so getResources() captures nothing.
    SecurityUtils.setSecurityManager(securityManager);
    try {
      AtomicBoolean ran = new AtomicBoolean(false);
      AtomicReference<SecurityManager> seenAfterRun = new AtomicReference<>();
      MDCAwareRunnable runnable = new MDCAwareRunnable(() -> ran.set(true));

      Thread worker = new Thread(() -> {
        runnable.run();
        seenAfterRun.set(ThreadContext.getSecurityManager());
      });
      worker.start();
      worker.join();

      assertTrue("delegate must run when no thread-bound context was captured", ran.get());
      assertNull("worker must not be left with a thread-bound SecurityManager", seenAfterRun.get());
    }
    finally {
      SecurityUtils.setSecurityManager(null);
    }
  }
}
