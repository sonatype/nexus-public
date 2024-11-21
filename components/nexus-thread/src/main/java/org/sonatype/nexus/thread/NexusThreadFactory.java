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
package org.sonatype.nexus.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Nexus {@link ThreadFactory}.
 */
public class NexusThreadFactory
    implements ThreadFactory
{
  private static final AtomicInteger poolNumber = new AtomicInteger(1);

  private final AtomicInteger threadNumber = new AtomicInteger(1);

  private final String namePrefix;

  private final ThreadGroup schedulerThreadGroup;

  private final boolean deamonThread;

  private int threadPriority;

  public NexusThreadFactory(String poolId, String threadGroupName) {
    this(poolId, threadGroupName, Thread.NORM_PRIORITY);
  }

  public NexusThreadFactory(final String poolId, final String threadGroupName, final int threadPriority) {
    this(poolId, threadGroupName, threadPriority, false);
  }

  public NexusThreadFactory(
      final String poolId,
      final String threadGroupName,
      final int threadPriority,
      final boolean daemonThread)
  {
    int poolNum = poolNumber.getAndIncrement();
    this.schedulerThreadGroup = new ThreadGroup(threadGroupName + " #" + poolNum);
    this.namePrefix = poolId + "-" + poolNum + "-thread-";
    this.deamonThread = daemonThread;
    this.threadPriority = threadPriority;
  }

  public Thread newThread(final Runnable r) {
    final Thread result = new Thread(schedulerThreadGroup, r, namePrefix + threadNumber.getAndIncrement());
    result.setDaemon(this.deamonThread);
    result.setPriority(this.threadPriority);
    return result;
  }
}
