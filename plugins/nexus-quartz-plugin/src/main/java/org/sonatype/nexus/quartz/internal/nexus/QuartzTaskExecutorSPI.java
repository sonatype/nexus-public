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
package org.sonatype.nexus.quartz.internal.nexus;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.quartz.QuartzSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.spi.TaskExecutorSPI;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.eclipse.sisu.Priority;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.quartz.impl.matchers.KeyMatcher.keyEquals;
import static org.sonatype.nexus.quartz.internal.nexus.NexusTaskJobSupport.toTaskConfiguration;

/**
 * Quartz backed implementation of {@link TaskExecutorSPI}. It uses distinct group for NX tasks and relies on
 * {@link QuartzSupport}.
 *
 * @since 3.0
 */
@Singleton
@Named
@Priority(2000)
public class QuartzTaskExecutorSPI
    extends ComponentSupport
    implements TaskExecutorSPI
{
  /**
   * Group for job and trigger keys.
   */
  static final String QZ_NEXUS_GROUP = "nexus";

  private final EventBus eventBus;

  private final QuartzSupport quartzSupport;

  private final NexusScheduleConverter nexusScheduleConverter;

  @Inject
  public QuartzTaskExecutorSPI(final EventBus eventBus,
                               final QuartzSupport quartzSupport,
                               final NexusScheduleConverter nexusScheduleConverter)
  {
    this.eventBus = checkNotNull(eventBus);
    this.quartzSupport = checkNotNull(quartzSupport);
    this.nexusScheduleConverter = checkNotNull(nexusScheduleConverter);
  }

  @Override
  public TaskInfo getTaskById(final String id) {
    try {
      final NexusTaskInfo task = taskByNxTaskId(id);
      if (task != null && !task.isRemovedOrDone()) {
        return task;
      }
    }
    catch (IllegalStateException e) {
      // no listener found in taskByKey, means no job exists
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
    return null;
  }

  @Override
  public List<TaskInfo> listsTasks() {
    try {
      final List<TaskInfo> result = Lists.newArrayList();
      final Map<JobKey, NexusTaskInfo> allTasks = allTasks();
      for (NexusTaskInfo nexusTaskInfo : allTasks.values()) {
        if (!nexusTaskInfo.isRemovedOrDone()) {
          result.add(nexusTaskInfo);
        }
      }
      return result;
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }


  @Override
  public TaskInfo scheduleTask(final TaskConfiguration taskConfiguration,
                               final Schedule schedule)
  {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(QuartzSupport.class.getClassLoader());
    try {
      final JobKey jobKey = JobKey.jobKey(UUID.randomUUID().toString(), QZ_NEXUS_GROUP);
      // get trigger, but use identity of jobKey
      // This is only for simplicity, as is not a requirement: NX job:triggers are 1:1 so tying them as this is ok
      // ! create the trigger before eventual TaskInfo remove bellow to avoid task removal in case of an invalid trigger
      final Trigger trigger = nexusScheduleConverter.toTrigger(schedule)
          .withIdentity(jobKey.getName(), jobKey.getGroup()).build();

      final NexusTaskInfo old = taskByNxTaskId(taskConfiguration.getId());
      if (old != null && !old.remove()) {
        // this is update but old task could not be removed: ie. running a non-cancelable task
        throw new IllegalArgumentException("Task could not be updated: running and not cancelable?");
      }
      log.debug("Task {} : {} scheduled with key: {} ", taskConfiguration.getId(),
          taskConfiguration.getTaskLogName(), jobKey.getName());
      final JobDataMap jobDataMap = new JobDataMap(taskConfiguration.asMap());
      final JobDetail jobDetail = JobBuilder.newJob(NexusTaskJobSupport.class).withIdentity(jobKey)
          .withDescription(taskConfiguration.getName()).usingJobData(jobDataMap).build();

      // register job specific listener with initial state
      final NexusTaskInfo nexusTaskInfo = initializeTaskState(jobDetail, trigger);
      // schedule the job
      quartzSupport.getScheduler().scheduleJob(jobDetail, trigger);
      // enabled
      if (!taskConfiguration.isEnabled()) {
        quartzSupport.getScheduler().pauseJob(jobKey);
      }
      return nexusTaskInfo;
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }

  @Override
  public TaskInfo rescheduleTask(final String id, final Schedule schedule) {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(QuartzSupport.class.getClassLoader());
    try {
      final NexusTaskInfo task = taskByNxTaskId(id);
      if (task == null) {
        return null;
      }
      checkState(!task.isRemovedOrDone(), "Done task cannot be rescheduled");
      log.debug("Task {} : {} rescheduled {} -> {} ",
          task.getJobKey().getName(),
          task.getConfiguration().getTaskLogName(),
          task.getSchedule(), schedule);
      final Trigger trigger = nexusScheduleConverter.toTrigger(schedule)
          .withIdentity(task.getJobKey().getName(), task.getJobKey().getGroup()).forJob(task.getJobKey()).build();
      quartzSupport.getScheduler().rescheduleJob(trigger.getKey(), trigger);
      // update TaskInfo, but only if it's WAITING, as running one will pick up the change by job listener when done
      task.setNexusTaskStateIfInState(
          State.WAITING,
          new NexusTaskState(
              task.getConfiguration(),
              schedule,
              trigger.getFireTimeAfter(new Date())
          ),
          task.getNexusTaskFuture()
      );
      return task;
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }

  @Override
  public int getRunningTaskCount() {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(QuartzSupport.class.getClassLoader());
    try {
      return quartzSupport.getScheduler().getCurrentlyExecutingJobs().size();
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }

  // ==

  /**
   * Creates and registers a {@link NexusTaskJobListener} for given task with initial state and returns it's {@link
   * NexusTaskInfo}.
   */
  NexusTaskInfo initializeTaskState(final JobDetail jobDetail, final Trigger trigger) throws SchedulerException {
    final Date now = new Date();
    final TaskConfiguration taskConfiguration = toTaskConfiguration(jobDetail.getJobDataMap());
    final Schedule schedule = nexusScheduleConverter.toSchedule(trigger);
    final NexusTaskState nexusTaskState = new NexusTaskState(
        taskConfiguration,
        schedule,
        trigger.getFireTimeAfter(now)
    );

    NexusTaskFuture future = null;
    if (schedule instanceof Now) {
      future = new NexusTaskFuture(
          this,
          jobDetail.getKey(),
          taskConfiguration.getTaskLogName(),
          now,
          schedule
      );
    }

    final NexusTaskJobListener nexusTaskJobListener = new NexusTaskJobListener<>(
        eventBus,
        this,
        jobDetail.getKey(),
        nexusScheduleConverter,
        nexusTaskState,
        future
    );

    quartzSupport.getScheduler().getListenerManager()
        .addJobListener(nexusTaskJobListener, keyEquals(jobDetail.getKey()));
    return nexusTaskJobListener.getNexusTaskInfo();
  }

  Map<JobKey, NexusTaskInfo> allTasks() throws SchedulerException {
    final Map<JobKey, NexusTaskInfo> result = Maps.newHashMap();
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(QuartzSupport.class.getClassLoader());
    try {
      final Set<JobKey> jobKeys = quartzSupport.getScheduler().getJobKeys(jobGroupEquals(QZ_NEXUS_GROUP));
      for (JobKey jobKey : jobKeys) {
        final NexusTaskJobListener<?> nexusTaskJobListener = (NexusTaskJobListener<?>) quartzSupport.getScheduler()
            .getListenerManager().getJobListener(NexusTaskJobListener.listenerName(jobKey));
        if (nexusTaskJobListener != null) {
          result.put(jobKey, nexusTaskJobListener.getNexusTaskInfo());
        }
      }
      return result;
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }

  NexusTaskInfo taskByNxTaskId(final String id) throws SchedulerException {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(QuartzSupport.class.getClassLoader());
    try {
      final Map<JobKey, NexusTaskInfo> allTasks = allTasks();
      for (NexusTaskInfo task : allTasks.values()) {
        if (task.getId().equals(id)) {
          return task;
        }
      }
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
    return null;
  }

  boolean cancelJob(final JobKey jobKey) {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(QuartzSupport.class.getClassLoader());
    try {
      checkNotNull(jobKey);
      return quartzSupport.cancelJob(jobKey);
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }

  void runNow(final JobKey jobKey) throws TaskRemovedException, SchedulerException {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(QuartzSupport.class.getClassLoader());
    try {
      // triggering with dataMap from "now" trigger as it contains metadata for back-conversion
      // in listener
      quartzSupport.getScheduler().triggerJob(
          jobKey,
          nexusScheduleConverter.toTrigger(new Now()).build().getJobDataMap()
      );
    }
    catch (JobPersistenceException e) {
      throw new TaskRemovedException(jobKey.getName(), e);
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }

  boolean removeTask(final JobKey jobKey) {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(QuartzSupport.class.getClassLoader());
    try {
      quartzSupport.getScheduler().getListenerManager().removeJobListener(NexusTaskJobListener.listenerName(jobKey));
      return quartzSupport.getScheduler().unscheduleJob(new TriggerKey(jobKey.getName(), jobKey.getGroup()));
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }
}
