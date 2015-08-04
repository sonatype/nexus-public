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
package org.sonatype.scheduling;

/**
 * Manage the storage and loading of ScheduledTask objects
 */
public interface TaskConfigManager
{
  /**
   * Add a new scheduled task
   */
  <T> void addTask(ScheduledTask<T> task);

  /**
   * Remove an existing scheduled task
   */
  <T> void removeTask(ScheduledTask<T> task);

  /**
   * Create and start all tasks, usually done once upon starting system (to start tasks that should be recurring)
   */
  void initializeTasks(Scheduler scheduler);

  /**
   * A factory for tasks.
   */
  SchedulerTask<?> createTaskInstance(String taskType)
      throws IllegalArgumentException;

  /**
   * A factory for tasks.
   */
  <T> T createTaskInstance(Class<T> taskType)
      throws IllegalArgumentException;
}
