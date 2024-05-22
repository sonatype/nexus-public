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
package org.sonatype.nexus.quartz.internal.datastore;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskUtils;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.spi.TaskResultState;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;
import org.sonatype.nexus.transaction.Transactional;

import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.quartz.TriggerKey.triggerKey;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskUtils.updateJobData;

@Named
@Singleton
public class TaskResultStateStoreImpl
    extends ConfigStoreSupport<QuartzDAO>
    implements TaskResultStateStore
{
  protected Scheduler scheduler;

  @Inject
  public TaskResultStateStoreImpl(final DataSessionSupplier sessionSupplier, final Scheduler scheduler) {
    super(sessionSupplier);
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  public Optional<TaskResultState> getState(final TaskInfo taskInfo) {
    return Optional.ofNullable(taskInfo)
        .map(QuartzTaskInfo.class::cast)
        .map(QuartzTaskInfo::getJobKey)
        .map(JobKey::getName)
        .flatMap(this::getQuartzState)
        .map(Collections::singletonList)
        .map(jobStates -> aggregate(taskInfo, jobStates));
  }

  @Override
  public void updateJobDataMap(final TaskInfo taskInfo) {
    String jobName = ((QuartzTaskInfo) taskInfo).getJobKey().getName();
    JobDataMap jobDataMap = new JobDataMap();
    updateJobData(jobDataMap, taskInfo.getConfiguration());

    // Don't persist for any tasks that may run when frozen, as exceptions may occur
    if (!taskInfo.getConfiguration().getBoolean(TaskConfiguration.RUN_WHEN_FROZEN, false)) {
      doUpdateJobDataMap(jobName, jobDataMap);
    }
  }

  @Transactional
  protected List<QuartzTaskStateData> getQuartzStates() {
    return dao().getStates();
  }

  @Transactional
  protected Optional<QuartzTaskStateData> getQuartzState(final String taskId) {
    return dao().getState(taskId);
  }

  @Transactional
  protected void doUpdateJobDataMap(final String jobName, final JobDataMap jobDataMap) {
    QuartzTaskStateData quartzTaskStateData = new QuartzTaskStateData();
    quartzTaskStateData.setJobName(jobName);
    quartzTaskStateData.setJobData(jobDataMap);
    dao().updateJobDataMap(quartzTaskStateData);
  }

  private TaskResultState aggregate(final TaskInfo taskInfo, final List<QuartzTaskStateData> jobStates) {
    TaskConfiguration taskConfiguration = jobStates.stream()
        .map(QuartzTaskStateData::getJobData)
        .filter(Objects::nonNull)
        .map(QuartzTaskUtils::configurationOf)
        .filter(Objects::nonNull)
        .findAny()
        .orElse(null);

    if (taskConfiguration == null) {
      return null;
    }

    Date nextFireTime = null;
    JobKey jobKey = ((QuartzTaskInfo) taskInfo).getJobKey();
    try {
      nextFireTime = Optional.ofNullable(scheduler.getTrigger(triggerKey(jobKey.getName(), jobKey.getGroup())))
          .map(Trigger::getNextFireTime)
          .orElseGet(taskInfo.getCurrentState()::getNextRun);
    }
    catch (SchedulerException e) {
      log.debug("An error occurred finding the next fire time for {}", taskConfiguration.getId(), e);
      nextFireTime = taskInfo.getCurrentState().getNextRun();
    }

    boolean running = jobStates.stream()
        .map(QuartzTaskStateData::getState)
        .anyMatch("EXECUTING"::equals);

    return new TaskResultState(taskConfiguration.getId(), running ? TaskState.RUNNING : TaskState.WAITING,
        nextFireTime, taskConfiguration.getLastRunState(), taskConfiguration.getProgress());
  }
}
