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

/**
 * Descriptor for a {@link Task}.
 *
 * Descriptors should be named singleton components.
 *
 * @since 3.0
 */
public interface TaskDescriptor
{
  /**
   * Unique identifier for descriptor.
   */
  String getId();

  /**
   * Short descriptive name of task.
   */
  String getName();

  /**
   * The type of task.
   */
  Class<? extends Task> getType();

  /**
   * Task configuration fields.
   */
  List<FormField> getFormFields();

  /**
   * @since 3.27
   */
  TaskConfiguration createTaskConfiguration();

  /**
   * Directly manipulate task configuration before storing
   *
   * @since 3.2
   * @param configuration task's configuration
   */
  void initializeConfiguration(TaskConfiguration configuration);

  // TODO: Figure out some clearer terms to use for the following state flags:

  /**
   * Returns task visibility.
   *
   * Visible tasks are shown to users.
   *
   * May be overridden by {@link TaskConfiguration#setVisible(boolean)}.
   */
  boolean isVisible();

  /**
   * Returns task exposure.
   *
   * Exposed tasks are allowed to be created by users.
   */
  boolean isExposed();

  /**
   * Returns true if the job should be marked as recoverable
   *
   * @since 3.15
   */
  boolean isRecoverable();

  boolean allowConcurrentRun();

  boolean isReadOnlyUi();
}
