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
package org.sonatype.nexus.logging.task;

/**
 * @since 3.5
 */
public interface TaskLogger
{
  // id used in discriminator. See logback.xml
  String LOGBACK_TASK_DISCRIMINATOR_ID = "taskIdAndDate";

  // constant for MDC use
  String TASK_LOG_ONLY_MDC = "TASK_LOG_ONLY_MDC";

  // constant for MDC use
  String TASK_LOG_WITH_PROGRESS_MDC = "TASK_LOG_WITH_PROGRESS_MDC";

  /**
   * Required to start the task logging. See {@link TaskLoggerHelper#start(TaskLogger)}
   */
  void start();

  /**
   * Required to close out the task logging. This involves cleaning up MDC and ThreadLocal variables. See {@link
   * TaskLoggerHelper#finish()}
   */
  void finish();

  /**
   * Log a progress event, which are always logged to the task log, but only periodically to the nexus.log
   *
   * @param event log event containing progress
   */
  void progress(TaskLoggingEvent event);

  /**
   * Flush any pending progress messages so they are logged immediately
   */
  void flush();
}
