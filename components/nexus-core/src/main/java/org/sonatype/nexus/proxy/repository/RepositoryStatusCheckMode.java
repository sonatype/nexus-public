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
package org.sonatype.nexus.proxy.repository;

public enum RepositoryStatusCheckMode
{
  /**
   * Nexus will always update the remote status (and autoblock and auto-unblock proxy mode if neded) of all reposes
   * if
   * it is in service. Warning: in this mode, even unused Nexus will do HEAD/GET requests against remote reposes
   * (will
   * produce network traffic) on regular time intervals! To stop checking it, put the repository out of service or
   * change the RepositoryStatusCheckMode to some other mode.
   */
  ALWAYS,

  /**
   * Nexus will ping only those remote hosts that became unavailable and caused a Nexus repository to became
   * AUTO_BLOCKED. It will check the status of remote repo as long until it becomes available (and will auto-unblock
   * it) OR until the user blocks it manually (ie. it is a known fact that a remote repo is down for maintenance for
   * a
   * while) or puts out of service.
   */
  AUTO_BLOCKED_ONLY,

  /**
   * Nexus will never try to discover remote statuses (and never unblock AUTO_BLOCKED) repositories. Users must
   * manage proxy modes manually.
   */
  NEVER;
}
