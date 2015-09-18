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
package org.sonatype.nexus.scheduling.internal;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.spi.TaskExecutorSPI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The Nexus task executor, that relies on SPI provider to execute tasks.
 *
 * @since 3.0
 */
@Singleton
@Named
public class DefaultTaskScheduler
    extends ComponentSupport
    implements TaskScheduler
{
  private final TaskFactory taskFactory;

  private final Provider<TaskExecutorSPI> schedulerProvider;

  @Inject
  public DefaultTaskScheduler(final TaskFactory taskFactory,
                              final Provider<TaskExecutorSPI> schedulerProvider)
  {
    this.taskFactory = checkNotNull(taskFactory);
    this.schedulerProvider = checkNotNull(schedulerProvider);
  }

  // ==

  /**
   * Returns the actual SPI present in system.
   */
  private TaskExecutorSPI getScheduler() {
    final TaskExecutorSPI provider = schedulerProvider.get();
    if (provider == null) {
      throw new IllegalStateException("No scheduler present in system!");
    }
    return provider;
  }

  // ==

  @Override
  public List<TaskDescriptor<?>> listTaskDescriptors() {
    return taskFactory.listTaskDescriptors();
  }

  @Override
  public TaskConfiguration createTaskConfigurationInstance(final Class<? extends Task> taskType)
      throws IllegalArgumentException
  {
    checkNotNull(taskType);
    return createTaskConfigurationInstance(taskType.getName());
  }

  @Override
  public TaskConfiguration createTaskConfigurationInstance(final String taskType) throws IllegalArgumentException {
    checkNotNull(taskType);
    final TaskDescriptor<?> taskDescriptor = taskFactory.resolveTaskDescriptorByTypeId(taskType);
    checkArgument(taskDescriptor != null, "Unknown taskType: '%s'", taskType);
    return createTaskConfigurationInstanceFromDescriptor(taskDescriptor);
  }

  @Override
  public <T extends Task> T createTaskInstance(final TaskConfiguration taskConfiguration)
      throws IllegalArgumentException
  {
    checkNotNull(taskConfiguration);
    return taskFactory.createTaskInstance(taskConfiguration);
  }

  @Override
  public TaskInfo submit(final TaskConfiguration taskConfiguration) {
    return scheduleTask(taskConfiguration, new Now());
  }

  @Override
  public TaskInfo getTaskById(final String id) {
    checkNotNull(id);
    return getScheduler().getTaskById(id);
  }

  @Override
  public List<TaskInfo> listsTasks() {
    return getScheduler().listsTasks();
  }

  @Override
  public TaskInfo scheduleTask(final TaskConfiguration taskConfiguration, final Schedule schedule) {
    checkNotNull(taskConfiguration);
    taskConfiguration.validate();
    checkNotNull(schedule);
    final Date now = new Date();
    if (taskConfiguration.getCreated() == null) {
      taskConfiguration.setCreated(now);
    }
    taskConfiguration.setUpdated(now);
    final TaskInfo taskInfo = getScheduler().scheduleTask(taskConfiguration, schedule);
    log.info("Task {} scheduled: {}", taskInfo.getConfiguration().getTaskLogName(), taskInfo.getSchedule().getType());
    return taskInfo;
  }

  @Override
  public TaskInfo rescheduleTask(final String id, final Schedule schedule) {
    checkNotNull(id);
    checkNotNull(schedule);
    final TaskInfo taskInfo =  getScheduler().rescheduleTask(id, schedule);
    if (taskInfo != null) {
      log.info("Task {} rescheduled: {}", taskInfo.getConfiguration().getTaskLogName(),
          taskInfo.getSchedule().getType());
    }
    return taskInfo;
  }

  /**
   * Creates configuration from descriptor.
   */
  private TaskConfiguration createTaskConfigurationInstanceFromDescriptor(final TaskDescriptor taskDescriptor)
      throws IllegalArgumentException
  {
    log.debug("Creating task configuration for task descriptor: {}", taskDescriptor.getId());
    final TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setId(generateId());
    taskConfiguration.setTypeId(taskDescriptor.getId());
    taskConfiguration.setTypeName(taskDescriptor.getName());
    taskConfiguration.setName(taskDescriptor.getName());
    taskConfiguration.setVisible(taskDescriptor.isVisible());
    return taskConfiguration;
  }

  /**
   * Creates a unique ID for the task.
   */
  private String generateId()
  {
    // TODO: revisit some possible alternative?
    return UUID.randomUUID().toString();
  }

  // ==

  @Override
  public int getRunningTaskCount() {
    return getScheduler().getRunningTaskCount();
  }

  @Override
  @Deprecated
  public void killAll() {
    // TODO: nop, used in UTs only
  }
}
