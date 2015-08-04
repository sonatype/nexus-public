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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Local semaphore-based {@link ResourceLockMBean} implementation.
 */
final class LocalResourceLockMBean
    extends AbstractResourceLockMBean
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final ResourceLockFactory locks;

  // ----------------------------------------------------------------------
  // Constructor
  // ----------------------------------------------------------------------

  LocalResourceLockMBean(final ResourceLockFactory locks) {
    this.locks = locks;
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public String[] listResourceNames() {
    return locks.getResourceNames();
  }

  public String[] findOwningThreads(final String name) {
    if (!Arrays.asList(locks.getResourceNames()).contains(name)) {
      return new String[0]; // avoid creating new resource lock
    }
    final Thread[] owners = locks.getResourceLock(name).getOwners();
    final String[] ownerTIDs = new String[owners.length];
    for (int i = 0; i < owners.length; i++) {
      ownerTIDs[i] = Long.toString(owners[i].getId());
    }
    return ownerTIDs;
  }

  public String[] findWaitingThreads(final String name) {
    if (!Arrays.asList(locks.getResourceNames()).contains(name)) {
      return new String[0]; // avoid creating new resource lock
    }
    final Thread[] waiters = locks.getResourceLock(name).getWaiters();
    final String[] waiterTIDs = new String[waiters.length];
    for (int i = 0; i < waiters.length; i++) {
      waiterTIDs[i] = Long.toString(waiters[i].getId());
    }
    return waiterTIDs;
  }

  public String[] findOwnedResources(final String tid) {
    final long ownerId = Long.decode(tid).longValue();
    final List<String> names = new ArrayList<String>();
    for (final String n : locks.getResourceNames()) {
      for (final Thread t : locks.getResourceLock(n).getOwners()) {
        if (t.getId() == ownerId) {
          names.add(n);
        }
      }
    }
    return names.toArray(new String[names.size()]);
  }

  public String[] findWaitedResources(final String tid) {
    final long waiterId = Long.decode(tid).longValue();
    final List<String> names = new ArrayList<String>();
    for (final String n : locks.getResourceNames()) {
      for (final Thread t : locks.getResourceLock(n).getWaiters()) {
        if (t.getId() == waiterId) {
          names.add(n);
        }
      }
    }
    return names.toArray(new String[names.size()]);
  }

  public void releaseResource(final String name) {
    if (Arrays.asList(locks.getResourceNames()).contains(name)) {
      // forcibly unwind any current holds on this lock
      final ResourceLock lock = locks.getResourceLock(name);
      for (final Thread t : lock.getOwners()) {
        for (int i = lock.getSharedCount(t); i > 0; i--) {
          lock.unlockShared(t);
        }
        for (int i = lock.getExclusiveCount(t); i > 0; i--) {
          lock.unlockExclusive(t);
        }
      }
    }
  }
}
