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
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.scheduling.ClusteredTaskState;
import org.sonatype.nexus.scheduling.ClusteredTaskStateStore;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.events.TaskScheduledEvent;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Default {@link TaskScheduler} implementation.
 *
 * @since 3.0
 */
@Singleton
@Named
public class TaskSchedulerImpl
    extends ComponentSupport
    implements TaskScheduler
{
  private final EventManager eventManager;

  private final TaskFactory taskFactory;

  private final ClusteredTaskStateStore clusteredTaskStateStore;

  private final Provider<SchedulerSPI> scheduler;

  @Inject
  public TaskSchedulerImpl(final EventManager eventManager,
                           final TaskFactory taskFactory,
                           final ClusteredTaskStateStore clusteredTaskStateStore,
                           final Provider<SchedulerSPI> scheduler)
  {
    this.eventManager = checkNotNull(eventManager);
    this.taskFactory = checkNotNull(taskFactory);
    this.clusteredTaskStateStore = checkNotNull(clusteredTaskStateStore);
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  public TaskFactory getTaskFactory() {
    return taskFactory;
  }

  /**
   * Helper to ensure provided scheduler is non-null.
   */
  private SchedulerSPI getScheduler() {
    SchedulerSPI result = scheduler.get();
    checkState(result != null);
    return result;
  }

  @Override
  public ScheduleFactory getScheduleFactory() {
    ScheduleFactory result = getScheduler().scheduleFactory();
    checkState(result != null);
    return result;
  }

  @Override
  public int getRunningTaskCount() {
    return getScheduler().getRunningTaskCount();
  }

  @Override
  public int getExecutedTaskCount() {
    return getScheduler().getExecutedTaskCount();
  }

  @Override
  public TaskConfiguration createTaskConfigurationInstance(final String typeId) {
    checkNotNull(typeId);

    TaskDescriptor descriptor = taskFactory.findDescriptor(typeId);
    checkArgument(descriptor != null, "Missing descriptor for task with type-id: %s", typeId);

    TaskConfiguration config = new TaskConfiguration();
    descriptor.initializeConfiguration(config); // in case any hardcode values need to be inserted
    config.setId(UUID.randomUUID().toString());
    config.setTypeId(descriptor.getId());
    config.setTypeName(descriptor.getName());
    config.setName(descriptor.getName());
    config.setVisible(descriptor.isVisible());
    config.setRecoverable(descriptor.isRecoverable());

    return config;
  }

  @Override
  public TaskInfo submit(final TaskConfiguration config) {
    return scheduleTask(config, getScheduleFactory().now());
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
  public TaskInfo scheduleTask(final TaskConfiguration config, final Schedule schedule) {
    checkNotNull(config);
    checkNotNull(schedule);

    config.validate();

    Date now = new Date();
    if (config.getCreated() == null) {
      config.setCreated(now);
    }
    config.setUpdated(now);

    TaskInfo taskInfo = getScheduler().scheduleTask(config, schedule);

    log.info("Task {} scheduled: {}",
        taskInfo.getConfiguration().getTaskLogName(),
        taskInfo.getSchedule().getType()
    );

    eventManager.post(new TaskScheduledEvent(taskInfo));

    return taskInfo;
  }

  @Override
  public List<ClusteredTaskState> getClusteredTaskStateById(String taskId) {
    checkNotNull(taskId);
    return clusteredTaskStateStore.getClusteredState(taskId);
  }
}
