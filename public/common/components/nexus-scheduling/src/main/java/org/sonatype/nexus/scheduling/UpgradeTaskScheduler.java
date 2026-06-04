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

/**
 * A helper for upgrades which require scheduling a task after Nexus starts
 */
public interface UpgradeTaskScheduler
{
  /**
   * Schedule a task to run after Nexus starts. The task will block the upgrade task queue until it completes,
   * preventing subsequent upgrade tasks from running concurrently.
   */
  void schedule(TaskConfiguration configuration);

  /**
   * Schedule a task to run after Nexus starts.
   *
   * <p>
   * Note: the default implementation delegates to {@link #schedule(TaskConfiguration)} and ignores the
   * {@code blockQueue} flag. Implementations that wish to honour the flag must override this method.
   * Calling this default with {@code blockQueue=false} will silently fall back to blocking behaviour.
   * </p>
   *
   * @param configuration the task configuration
   * @param blockQueue if {@code true}, the task blocks the upgrade task queue until it completes;
   *          if {@code false}, subsequent tasks in the queue are not held back by this task
   */
  default void schedule(TaskConfiguration configuration, boolean blockQueue) {
    schedule(configuration);
  }

  /**
   * Create a configuration for the given type-id.
   */
  TaskConfiguration createTaskConfigurationInstance(String typeId);
}
