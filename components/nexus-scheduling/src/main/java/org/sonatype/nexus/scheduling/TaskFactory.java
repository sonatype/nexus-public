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

/**
 * The factory for {@link Task} instances.
 */
public interface TaskFactory
{
  /**
   * Returns the list of task descriptors for all known tasks in system.
   */
  List<TaskDescriptor<?>> listTaskDescriptors();

  /**
   * Resolves the task descriptor by type ID of the task. Returns {@code null} if no task found for given task ID. This
   * "resolution" is laxed, in a way it will not work only for actual type ID (as reported by Task descriptor), but
   * also by task FQCN, Simple Class name and @Named value.
   */
  <T extends Task> TaskDescriptor<T> resolveTaskDescriptorByTypeId(String taskTypeId);

  /**
   * A factory for tasks based on passed in task configuration. Returns a configured task instance.
   *
   * @throws IllegalArgumentException if taskType carried by configuration is not a valid task type.
   */
  <T extends Task> T createTaskInstance(TaskConfiguration taskConfiguration)
      throws IllegalArgumentException;
}
