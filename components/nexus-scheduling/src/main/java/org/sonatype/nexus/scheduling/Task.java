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

import java.util.concurrent.Callable;

/**
 * The main interface for all Tasks used in Nexus.
 *
 * All implementations should keep their configuration (if any) in the corresponding {@link TaskConfiguration} object,
 * due to persistence implications.  Or, the task might source it's configuration from other (injected) component.
 * Implementations <em>must not be marked as singletons</em>, container must provide new instance for each execution.
 *
 * @since 3.0
 */
public interface Task
    extends Callable<Object>
{
  /**
   * Returns the copy of the task configuration.
   *
   * Modifying this map has no effect on effective configuration of the task.
   */
  TaskConfiguration taskConfiguration();

  /**
   * Configures the task.
   *
   * @throws IllegalArgumentException if passed in configuration is invalid.
   */
  void configure(TaskConfiguration taskConfiguration);

  /**
   * Returns a unique ID of the task instance.
   *
   * Shorthand method for {@link TaskConfiguration#getId()}.
   */
  String getId();

  /**
   * Returns a descriptive name of the task instance, usually set by user.
   */
  String getName();

  /**
   * Returns short generated message of current task's instance work.
   *
   * This message is based on task configuration and same typed tasks might emit different messages depending
   * on configuration.
   */
  String getMessage();
}
