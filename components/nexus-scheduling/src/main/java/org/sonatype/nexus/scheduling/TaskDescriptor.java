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

import org.sonatype.nexus.formfields.FormField;

import com.google.common.base.Predicate;

/**
 * Task descriptor that makes task visible in UI and user manageable. Task that is meant for end users to use over UI
 * should have descriptors too. Still, a Task implementation does NOT have to have descriptor. One notable difference
 * is, that running task that has descriptor is also by default visible in UI (grid), while task without descriptor is
 * by default not visible in UI (grid). Visibility may be overridden by using {@link
 * TaskConfiguration#setVisible(boolean)} method. Descriptors should be singleton components.
 *
 * @since 3.0
 */
public interface TaskDescriptor<T extends Task>
{
  /**
   * The "type ID" of task. This ID is used in UI solely.
   */
  String getId();

  /**
   * Short, descriptive name of task.
   */
  String getName();

  /**
   * The actual type of task.
   */
  Class<T> getType();

  /**
   * UI elements of the task.
   */
  List<FormField> formFields();

  /**
   * Are task <strong>instances</strong> visible in UI (grid, when scheduled)?
   */
  boolean isVisible();

  /**
   * Is the task <strong>type</strong> exposed over UI and available for users to create instances of it, schedule it?
   */
  boolean isExposed();

  /**
   * Returns the predicate to filter for tasks based on this type.
   */
  Predicate<TaskInfo> predicate();

  /**
   * Filters the supplied list for tasks of this type. This is just a handy method to free caller to cope with
   * filtering, uses Guava filtering with predicate from {@link #predicate()}, returns a new list with filtered
   * elements.
   */
  List<TaskInfo> filter(List<TaskInfo> tasks);
}
