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

import org.sonatype.nexus.proxy.access.Action;

/**
 * Repository item UID lock represents a global lock that is able to sync across multiple threads.
 *
 * @author cstamas
 */
public interface RepositoryItemUidLock
{
  /**
   * Locks this UID for a given action. Will perform lock upgrade is needed (read -> write). Lock upgrade (ie. create
   * action locking happens after read action already locked) happens, but does not happen atomically! Lock upgrade
   * is
   * actually release all (if any) read lock and then acquire write lock. Once you have exclusive lock, then only you
   * can be sure for unique access.
   */
  void lock(Action action);

  /**
   * Unlocks UID. It is the responsibility of caller to use lock/unlock properly (ie. boxing of calls).
   */
  void unlock();

  /**
   * Returns true if current thread has locks already on this UID lock.
   */
  boolean hasLocksHeld();
}
