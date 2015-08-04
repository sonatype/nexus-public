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
package org.sonatype.scheduling;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.fail;

public class Utils
{

  public static void awaitTaskState(ScheduledTask<?> task, long timeout, TaskState... states) {
    Set<TaskState> stateSet = new HashSet<TaskState>(Arrays.asList(states));

    long start = System.currentTimeMillis();
    TaskState state = null;
    do {
      state = task.getTaskState();
      if (stateSet.contains(state)) {
        return;
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
        // ignored
      }
    }
    while (System.currentTimeMillis() - start <= timeout);
    fail("exceeded timeout while waiting for task " + task + " to transition from state " + state + " into "
        + stateSet);
  }

  public static void awaitZeroTaskCount(Scheduler scheduler, long timeout) {
    long start = System.currentTimeMillis();
    int n = 0;
    do {
      n = scheduler.getAllTasks().size();
      if (n <= 0) {
        return;
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
        // ignored
      }
    }
    while (System.currentTimeMillis() - start <= timeout);
    fail("exceeded timeout while waiting for task map to transition from count " + n + " into 0");
  }

}
