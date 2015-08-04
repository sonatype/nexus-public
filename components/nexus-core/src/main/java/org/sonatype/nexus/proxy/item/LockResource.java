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
package org.sonatype.nexus.proxy.item;

/**
 * Before you continue using this class, stop and read carefully it's description. This "smart" lock is NOT an improved
 * implementation of Java's ReentrantReadWriteLock in any way. It does NOT implement the unsupported atomic
 * lock-upgrade. All this class allows is following pattern:
 *
 * <pre>
 *   ...
 *   lock.lockShared();
 *   try {
 *     ...
 *     lock.lockExclusively();
 *     try {
 *       ...
 *     } finally {
 *       lock.unlock();
 *     }
 *     ...
 *   } finally {
 *     lock.unlock();
 *   }
 * </pre>
 *
 * @author cstamas
 */
interface LockResource
{
  /**
   * Acquires a shared lock. Blocks until successful.
   */
  void lockShared();

  /**
   * Acquires an exclusive lock. Blocks until successful.
   */
  void lockExclusively();

  /**
   * Unlocks the last acquired lock.
   */
  void unlock();

  /**
   * Returns true if the caller thread owns locks of any kinds on this lock.
   */
  boolean hasLocksHeld();
}
