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
 * Factory API for managing various kinds of resource locks.
 */
public interface ResourceLockFactory
{
  /**
   * Returns the {@link ResourceLock} associated with the given resource name.
   *
   * @param name The lock name
   * @return Named resource lock
   */
  ResourceLock getResourceLock(String name);

  /**
   * Returns all resource names associated with active {@link ResourceLock}s.
   *
   * @return Resource names
   */
  String[] getResourceNames();

  /**
   * Shuts down the lock factory and cleans up any allocated resources/threads.
   */
  void shutdown();
}
