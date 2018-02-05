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

import org.slf4j.Logger;

/**
 * Each task is executed in its own thread and its {@link TaskLogger} is stored in it here.
 *
 * @since 3.5
 */
public class TaskLoggerHelper
{
  private static final ThreadLocal<TaskLogger> context = new ThreadLocal<>();

  private TaskLoggerHelper() {
    throw new IllegalAccessError("Utility class");
  }

  public static void start(final TaskLogger taskLogger) {
    taskLogger.start();
    context.set(taskLogger);
  }

  public static TaskLogger get() {
    return context.get();
  }

  public static void finish() {
    TaskLogger taskLogger = get();
    if (taskLogger != null) {
      taskLogger.finish();
    }
    context.remove();
  }

  public static void progress(final TaskLoggingEvent event) {
    TaskLogger taskLogger = get();
    if (taskLogger != null) {
      taskLogger.progress(event);
    }
  }

  public static void progress(final Logger logger, final String message, Object... args) {
    progress(new TaskLoggingEvent(logger, message, args));
  }

  /**
   * @see TaskLogger#flush()
   */
  public static void flush() {
    TaskLogger taskLogger = get();
    if (taskLogger != null) {
      taskLogger.flush();
    }
  }
}
