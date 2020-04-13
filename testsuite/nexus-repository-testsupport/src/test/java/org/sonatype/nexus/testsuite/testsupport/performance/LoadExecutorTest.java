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
package org.sonatype.nexus.testsuite.testsupport.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Test;

/**
 * Tests {@link LoadExecutor}
 */
public class LoadExecutorTest
{
  @Test(expected = IllegalStateException.class)
  public void taskExceptionsArePropagated() throws Exception {
    final List<Callable<?>> tasks = new ArrayList<>();
    tasks.add(new Callable<Void>()
    {
      @Override
      public Void call() throws Exception {
        throw new IllegalStateException("expected");
      }
    });

    new LoadExecutor(tasks, 1, 10).callTasks();
  }

  @Test(expected = AssertionError.class)
  public void taskAssertionErrorsArePropagated() throws Exception {
    final List<Callable<?>> tasks = new ArrayList<>();
    tasks.add(new Callable<Void>()
    {
      @Override
      public Void call() throws Exception {
        throw new AssertionError("expected");
      }
    });

    new LoadExecutor(tasks, 1, 10).callTasks();
  }

  @SuppressWarnings("java:S2699") // sonar wants assertions, but in this case seems best to let an exception bubble up
  @Test
  public void noFailureTestDoesActuallyStop() throws Exception {
    final List<Callable<?>> tasks = new ArrayList<>();
    tasks.add(new Callable<Void>()
    {
      @Override
      public Void call() throws Exception {
        // This task is always successful
        return null;
      }
    });

    new LoadExecutor(tasks, 1, 5).callTasks();
  }
}
