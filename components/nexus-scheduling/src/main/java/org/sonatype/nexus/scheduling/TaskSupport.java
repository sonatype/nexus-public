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

import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.logging.task.TaskLoggerFactory;
import org.sonatype.nexus.logging.task.TaskLoggerHelper;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

/**
 * Support for {@link Task} implementations.
 *
 * Subclasses may implement {@link Cancelable} interface if they are implemented to periodically check for
 * {@link #isCanceled()} or {@link CancelableHelper#checkCancellation()} methods.
 *
 * Task implementations should be {@code @Named} components but must not be {@code @Singletons}.
 *
 * @since 3.0
 */
public abstract class TaskSupport
    extends ComponentSupport
    implements Task
{
  private final TaskConfiguration configuration;

  private final AtomicBoolean canceledFlag;

  private final boolean taskLoggingEnabled;

  public TaskSupport() {
    this(true);
  }

  public TaskSupport(final boolean taskLoggingEnabled) {
    this.taskLoggingEnabled = taskLoggingEnabled;
    this.configuration = createTaskConfiguration();
    this.canceledFlag = new AtomicBoolean(false);
  }

  protected TaskConfiguration createTaskConfiguration() {
    return new TaskConfiguration();
  }

  protected TaskConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public TaskConfiguration taskConfiguration() {
    return new TaskConfiguration(configuration);
  }

  @Override
  public void configure(final TaskConfiguration configuration) {
    checkNotNull(configuration);

    configuration.validate();
    this.configuration.apply(configuration);

    String message = getMessage();
    if (!Strings.isNullOrEmpty(message)) {
      this.configuration.setMessage(message);
    }
  }

  @Override
  public String getId() {
    return getConfiguration().getId();
  }

  @Override
  public String getName() {
    return getConfiguration().getName();
  }

  /**
   * Install canceled flag and {@link #execute()}.
   */
  @Override
  public final Object call() throws Exception {
    startTaskLogging();
    CancelableHelper.set(canceledFlag);
    try {
      return execute();
    }
    catch (TaskInterruptedException e) {
      log.warn(TASK_LOG_ONLY, "Task '{}' was canceled", getMessage());
      throw e;
    }
    catch (Exception e) {
      log.error(TASK_LOG_ONLY, "Failed to run task '{}'", getMessage(), e);
      throw e;
    }
    finally {
      CancelableHelper.remove();
      finishTaskLogging();
    }
  }

  private void startTaskLogging() {
    if(taskLoggingEnabled) {
      TaskLoggerHelper.start(TaskLoggerFactory.create(this, log, configuration));
    }
  }

  private void finishTaskLogging() {
    if(taskLoggingEnabled) {
      TaskLoggerHelper.finish();
    }
  }

  /**
   * Execute task logic.
   */
  protected abstract Object execute() throws Exception;

  //
  // Cancelable; not directly implemented but here allow Cancelable to be used as a marker and provide impl
  //

  /**
   * Cancel this task.
   */
  public void cancel() {
    canceledFlag.set(true);
  }

  /**
   * Check if this task is canceled.
   */
  public boolean isCanceled() {
    return canceledFlag.get();
  }

  @Override
  public String toString() {
    return String.format("%s(id=%s, name=%s)", getClass().getSimpleName(), getId(), getName());
  }
}
