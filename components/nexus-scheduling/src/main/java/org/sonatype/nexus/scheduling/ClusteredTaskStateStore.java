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
package org.sonatype.nexus.scheduling;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Keeps track of task states within a clustered environment. Each node advertises local task states which the store
 * distributes across the cluster for all nodes to query.
 * 
 * @since 3.1
 */
public interface ClusteredTaskStateStore
{
  /**
   * Updates the store with the current state of the specified task.
   */
  void setLocalState(TaskInfo taskInfo);

  /**
   * Removes the state of the specified task from the store.
   */
  void removeClusteredState(String taskId);

  /**
   * Returns the state of a given task across the nodes in a clustered environment or {@code null} if clustering isn't
   * enabled.
   */
  @Nullable
  List<ClusteredTaskState> getClusteredState(String taskId);
}
