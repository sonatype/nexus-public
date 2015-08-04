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
package org.sonatype.sisu.locks;

/**
 * Reentrant lock for resources that support shared and/or exclusive access.
 */
public interface ResourceLock
{
  /**
   * Takes a shared resource lock for the given thread.
   */
  void lockShared(Thread thread);

  /**
   * Takes an exclusive resource lock for the given thread.
   */
  void lockExclusive(Thread thread);

  /**
   * Drops an exclusive resource lock for the given thread.
   */
  void unlockExclusive(Thread thread);

  /**
   * Drops a shared resource lock for the given thread.
   */
  void unlockShared(Thread thread);

  /**
   * @return Threads that currently hold locks on this resource
   */
  Thread[] getOwners();

  /**
   * @return Threads that are waiting for locks on this resource
   */
  Thread[] getWaiters();

  /**
   * @return Number of shared locks taken by the given thread
   */
  int getSharedCount(Thread thread);

  /**
   * @return Number of exclusive locks taken by the given thread
   */
  int getExclusiveCount(Thread thread);
}
