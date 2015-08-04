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

import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.eclipse.sisu.inject.Logs;
import org.eclipse.sisu.inject.Weak;

/**
 * Abstract {@link ResourceLockFactory} implementation; associates names with {@link ResourceLock}s.
 */
abstract class AbstractResourceLockFactory
    implements ResourceLockFactory
{
  // ----------------------------------------------------------------------
  // Constants
  // ----------------------------------------------------------------------

  protected static final String JMX_DOMAIN = "org.sonatype.sisu";

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  /*
   * TODO: investigate combining this with the thread-counts map by using a MultiMap?
   *
   * TODO: stop GC from reclaiming locks if they're still locked, but not referenced?
   */
  private final ConcurrentMap<String, ResourceLock> resourceLocks = Weak.concurrentValues();

  private ObjectName jmxName;

  // ----------------------------------------------------------------------
  // Constructor
  // ----------------------------------------------------------------------

  AbstractResourceLockFactory(final boolean jmxEnabled) {
    if (jmxEnabled) {
      try {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        final String hash = String.format("0x%08X", new Integer(System.identityHashCode(this)));
        jmxName = ObjectName.getInstance(JMX_DOMAIN, properties("type", category(), "hash", hash));

        server.registerMBean(new LocalResourceLockMBean(this), jmxName);
      }
      catch (final Exception e) {
        Logs.warn("Problem registering LocksMBean for: <>", this, e);
      }
    }
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public final ResourceLock getResourceLock(final String name) {
    ResourceLock lock = resourceLocks.get(name);
    if (null == lock) {
      final ResourceLock oldLock = resourceLocks.putIfAbsent(name, lock = createResourceLock(name));
      if (null != oldLock) {
        return oldLock;
      }
    }
    return lock;
  }

  public final String[] getResourceNames() {
    return resourceLocks.keySet().toArray(new String[0]);
  }

  public void shutdown() {
    if (null != jmxName) {
      try {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(jmxName);
      }
      catch (final Exception e) {
        Logs.warn("Problem unregistering LocksMBean for: <>", this, e);
      }
    }
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  /**
   * @return JMX category for this {@link ResourceLockFactory}
   */
  protected abstract String category();

  /**
   * @return Lock associated with the given resource name
   */
  protected abstract ResourceLock createResourceLock(final String name);

  /**
   * @return Hashtable from key,value,key,value,... sequence
   */
  protected static final Hashtable<String, String> properties(final String... keyValues) {
    final Hashtable<String, String> properties = new Hashtable<String, String>();
    for (int i = 0; i < keyValues.length; ) {
      properties.put(keyValues[i++], keyValues[i++]);
    }
    return properties;
  }
}
