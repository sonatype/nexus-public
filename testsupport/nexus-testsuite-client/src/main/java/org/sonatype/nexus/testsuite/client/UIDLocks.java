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
package org.sonatype.nexus.testsuite.client;

/**
 * UID Locks Nexus Client Subsystem.
 *
 * @since 2.2
 */
public interface UIDLocks
{

  /**
   * Locks a repository item for specified action
   *
   * @param repository containing the item to be locked
   * @param path       of item to be locked
   * @param lockType   on of read/create/delete/update
   */
  void lock(String repository, String path, LockType lockType);

  /**
   * Unlocks a repository item.
   *
   * @param repository containing the item to be unlocked
   * @param path       of item to be unlocked
   */
  void unlock(String repository, String path);

  /**
   * Allowed locking types.
   */
  public enum LockType
  {

    read, create, update, delete;

  }

}
