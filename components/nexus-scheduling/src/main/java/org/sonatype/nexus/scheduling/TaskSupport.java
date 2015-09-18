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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.scheduling.CancelableSupport.CancelableFlagHolder;
import org.sonatype.nexus.scheduling.TaskInfo.State;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link Task} implementations. Subclasses may implement {@link Cancelable} interface if they are
 * implemented to periodically check for {@link #isCanceled()} or {@link CancelableSupport#checkCancellation()}
 * methods. Task implementations should be {@code @Named} components but must not be {@code @Singletons}.
 *
 * @since 3.0
 */
public abstract class TaskSupport
    extends ComponentSupport
    implements Task
{
  private final TaskConfiguration configuration;

  private final CancelableFlagHolder cancelableFlagHolder;

  public TaskSupport() {
    this.configuration = createTaskConfiguration();
    this.cancelableFlagHolder = new CancelableFlagHolder();
  }

  protected TaskConfiguration createTaskConfiguration() {
    return new TaskConfiguration();
  }

  protected TaskConfiguration getConfiguration() { return configuration; }

  // == NexusTask

  public TaskConfiguration taskConfiguration() {
    return new TaskConfiguration(configuration);
  }

  @Override
  public void configure(final TaskConfiguration configuration) throws IllegalArgumentException {
    checkNotNull(configuration);
    configuration.validate();
    this.configuration.apply(configuration);
    final String message = getMessage();
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
   * Returns running tasks having same type as this task.
   */
  @Override
  public List<TaskInfo> isBlockedBy(final List<TaskInfo> runningTasks) {
    return Lists.newArrayList(Iterables.filter(runningTasks, new Predicate<TaskInfo>()
    {
      @Override
      public boolean apply(final TaskInfo taskInfo) {
        // blockedBy: running tasks of same type as me
        return State.RUNNING == taskInfo.getCurrentState().getState()
            && getConfiguration().getTypeId().equals(taskInfo.getConfiguration().getTypeId());
      }
    }));
  }

  @Override
  public final Object call() throws Exception {
    MDC.put(TaskSupport.class.getSimpleName(), getClass().getSimpleName());
    CancelableSupport.setCurrent(cancelableFlagHolder);
    try {
      return execute();
    }
    finally {
      CancelableSupport.setCurrent(null);
      MDC.remove(TaskSupport.class.getSimpleName());
    }
  }

  // == Cancelable (default implementations, used when Cancelable iface implemented)

  public void cancel() {
    cancelableFlagHolder.cancel();
  }

  public boolean isCanceled() {
    return cancelableFlagHolder.isCanceled();
  }

  // == Internal

  /**
   * Where the job is done.
   */
  protected abstract Object execute() throws Exception;

  // ==

  @Override
  public String toString() {
    return String.format("%s(id=%s, name=%s)", getClass().getSimpleName(), getId(), getName());
  }
}
