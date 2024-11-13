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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.scheduling.events.TaskScheduledEvent;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.app.FeatureFlags.CHANGE_REPO_BLOBSTORE_TASK_ENABLED_NAMED;

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

  protected static final String REPO_MOVE_TYPE_ID = "repository.move";

  private final EventManager eventManager;

  private final TaskFactory taskFactory;

  private final Provider<SchedulerSPI> scheduler;

  @Inject
  @Named(CHANGE_REPO_BLOBSTORE_TASK_ENABLED_NAMED)
  protected boolean changeRepoBlobstoreTaskEnabled;

  @Inject
  public TaskSchedulerImpl(final EventManager eventManager,
                           final TaskFactory taskFactory,
                           final Provider<SchedulerSPI> scheduler)
  {
    this.eventManager = checkNotNull(eventManager);
    this.taskFactory = checkNotNull(taskFactory);
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

    TaskConfiguration config = descriptor.createTaskConfiguration();
    descriptor.initializeConfiguration(config); // in case any hardcode values need to be inserted
    config.setId(UUID.randomUUID().toString());
    config.setTypeId(descriptor.getId());
    config.setTypeName(descriptor.getName());
    config.setName(descriptor.getName());
    config.setVisible(descriptor.isVisible());
    config.setRecoverable(descriptor.isRecoverable());
    config.setExposed(descriptor.isExposed());

    return config;
  }

  @Override
  public TaskInfo submit(final TaskConfiguration config) {
    return scheduleTask(config, getScheduleFactory().now());
  }

  @Override
  public TaskInfo getTaskById(final String id) {
    checkNotNull(id);
    TaskInfo taskInfo = getScheduler().getTaskById(id);
    if (null != taskInfo && includeRepoMoveTask(taskInfo)) {
      return taskInfo;
    }
    return null;
  }

  @Override
  public List<TaskInfo> listsTasks() {
    return getScheduler().listsTasks()
        .stream()
        .filter(this::includeRepoMoveTask)
        .collect(Collectors.toList());
  }

  private boolean includeRepoMoveTask(TaskInfo taskInfo) {
    return (changeRepoBlobstoreTaskEnabled || !taskInfo.getTypeId().equals(REPO_MOVE_TYPE_ID));
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
  public ExternalTaskState toExternalTaskState(final TaskInfo taskInfo) {
    return new ExternalTaskState(taskInfo);
  }

  @Override
  public boolean cancel(final String id, final boolean mayInterruptIfRunning) {
    return getScheduler().cancel(id, mayInterruptIfRunning);
  }

  @Nullable
  @Override
  public TaskInfo getTaskByTypeId(final String typeId) {
    return getScheduler().getTaskByTypeId(typeId);
  }

  @Nullable
  @Override
  public TaskInfo getTaskByTypeId(final String typeId, final Map<String, String> config) {
    return getScheduler().getTaskByTypeId(typeId, config);
  }

  @Override
  public boolean findAndSubmit(final String typeId) {
    return getScheduler().findAndSubmit(typeId);
  }

  @Override
  public boolean findWaitingTask(final String typeId, Map<String, String> config) {
    return getScheduler().findWaitingTask(typeId, config);
  }

  @Override
  public boolean findAndSubmit(final String typeId, final Map<String, String> config) {
    return getScheduler().findAndSubmit(typeId, config);
  }
}
