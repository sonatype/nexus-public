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
 * Tests that {@link MDCAwareCallable} propagates the submitting thread's Shiro {@link ThreadContext}
 * to the managed worker thread. Shiro 2.x changed ThreadContext from an InheritableThreadLocal to a
 * plain ThreadLocal, so worker threads no longer inherit the SecurityManager/Subject automatically;
 * MDCAwareCallable must carry it across explicitly.
 */
public class MDCAwareCallableTest
{
  private final SecurityManager securityManager = new DefaultSecurityManager();

  @After
  public void tearDown() {
    ThreadContext.remove();
  }

  @Test
  public void propagatesThreadContextToWorkerThreadAndClearsAfterCall() throws Exception {
    // bind a SecurityManager on the submitting thread, then build the callable (captures context here)
    ThreadContext.bind(securityManager);
    AtomicReference<SecurityManager> seenDuringCall = new AtomicReference<>();
    MDCAwareCallable<String> callable = new MDCAwareCallable<>(() -> {
      seenDuringCall.set(ThreadContext.getSecurityManager());
      return "result";
    });

    // execute on a separate (worker) thread which does NOT inherit the submitter's ThreadContext
    AtomicReference<SecurityManager> seenAfterCall = new AtomicReference<>();
    AtomicReference<String> returned = new AtomicReference<>();
    Thread worker = new Thread(() -> {
      try {
        returned.set(callable.call());
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      seenAfterCall.set(ThreadContext.getSecurityManager());
    });
    worker.start();
    worker.join();

    assertSame("worker must see the submitting thread's SecurityManager during call",
        securityManager, seenDuringCall.get());
    assertSame("call must return the delegate result", "result", returned.get());
    assertNull("worker ThreadContext must be cleared after call to avoid pooled-thread leakage",
        seenAfterCall.get());
  }

  @Test
  public void preservesWorkerPreExistingContext() throws Exception {
    // A worker that already carries its own Subject/SecurityManager (e.g. the fixed TASK_SUBJECT bound
    // by NexusExecutorService.forFixedSubject via subject.associateWith) must NOT have it replaced by
    // the submitting thread's captured context — otherwise the task runs as the wrong subject.
    ThreadContext.bind(securityManager);
    AtomicReference<SecurityManager> seenDuringCall = new AtomicReference<>();
    MDCAwareCallable<String> callable = new MDCAwareCallable<>(() -> {
      seenDuringCall.set(ThreadContext.getSecurityManager());
      return "ok";
    });

    SecurityManager workerExisting = new DefaultSecurityManager();
    AtomicReference<SecurityManager> seenAfterCall = new AtomicReference<>();
    Thread worker = new Thread(() -> {
      ThreadContext.bind(workerExisting);
      try {
        callable.call();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      seenAfterCall.set(ThreadContext.getSecurityManager());
    });
    worker.start();
    worker.join();

    assertSame("worker's pre-existing fixed Subject must not be overridden during the call",
        workerExisting, seenDuringCall.get());
    assertSame("worker's pre-existing SecurityManager must remain bound after the call",
        workerExisting, seenAfterCall.get());
  }

  @Test
  public void runsDelegateWhenSubmitterHadNoThreadBoundContext() throws Exception {
    // Nothing bound on the submitting thread's ThreadContext, so there is nothing to capture and the
    // boundContext=false path is taken: the worker must run the delegate without binding any context.
    // A VM-static SecurityManager (as production sets at startup) lets MDC subject-resolution succeed
    // without a thread binding; ThreadContext itself stays empty so getResources() captures nothing.
    SecurityUtils.setSecurityManager(securityManager);
    try {
      AtomicBoolean called = new AtomicBoolean(false);
      MDCAwareCallable<String> callable = new MDCAwareCallable<>(() -> {
        called.set(true);
        return "done";
      });

      AtomicReference<String> returned = new AtomicReference<>();
      AtomicReference<SecurityManager> seenAfterCall = new AtomicReference<>();
      Thread worker = new Thread(() -> {
        try {
          returned.set(callable.call());
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
        seenAfterCall.set(ThreadContext.getSecurityManager());
      });
      worker.start();
      worker.join();

      assertTrue("delegate must run when no thread-bound context was captured", called.get());
      assertSame("call must return the delegate result", "done", returned.get());
      assertNull("worker must not be left with a thread-bound SecurityManager", seenAfterCall.get());
    }
    finally {
      SecurityUtils.setSecurityManager(null);
    }
  }
}
