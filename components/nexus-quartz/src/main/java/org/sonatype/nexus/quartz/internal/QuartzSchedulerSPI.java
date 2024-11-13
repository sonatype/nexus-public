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
package org.sonatype.nexus.quartz.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LastShutdownTimeService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskFuture;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJob;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskState;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.schedule.Cron;
import org.sonatype.nexus.scheduling.schedule.Manual;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;
import org.sonatype.nexus.thread.DatabaseStatusDelayedExecutor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.core.QuartzScheduler;
import org.quartz.spi.JobStore;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.filterKeys;
import static java.util.Collections.emptyMap;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.quartz.impl.matchers.KeyMatcher.keyEquals;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener.listenerName;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskUtils.configurationOf;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskUtils.updateJobData;
import static org.sonatype.nexus.scheduling.TaskConfiguration.LAST_RUN_STATE_END_STATE;
import static org.sonatype.nexus.scheduling.TaskDescriptorSupport.LIMIT_NODE_KEY;
import static org.sonatype.nexus.scheduling.TaskState.INTERRUPTED;
import static org.sonatype.nexus.scheduling.TaskState.RUNNING;
import static org.sonatype.nexus.scheduling.schedule.Schedule.SCHEDULE_START_AT;
import static org.sonatype.nexus.scheduling.schedule.Schedule.stringToDate;

/**
 * Quartz {@link SchedulerSPI}.
 *
 * @since 3.0
 */
@ManagedLifecycle(phase = SERVICES)
public abstract class QuartzSchedulerSPI
    extends StateGuardLifecycleSupport
    implements SchedulerSPI
{
  public static final String MISSING_TRIGGER_RECOVERY = ".missingTriggerRecovery";

  protected static final String GROUP_NAME = "nexus";

  private static final Set<String> INHERITED_CONFIG_KEYS = ImmutableSet.of(LIMIT_NODE_KEY);

  protected final EventManager eventManager;

  private final NodeAccess nodeAccess;

  protected final Provider<JobStore> jobStoreProvider;

  protected final ScheduleFactory scheduleFactory;

  private final Provider<Scheduler> schedulerProvider;

  protected final QuartzTriggerConverter triggerConverter;

  private final LastShutdownTimeService lastShutdownTimeService;

  private final DatabaseStatusDelayedExecutor delayedExecutor;

  private final boolean recoverInterruptedJobs;

  protected Scheduler scheduler;

  protected QuartzScheduler quartzScheduler;

  private boolean active;

  @SuppressWarnings("squid:S00107") //suppress constructor parameter count
  @Inject
  public QuartzSchedulerSPI(final EventManager eventManager,
                            final NodeAccess nodeAccess,
                            final Provider<JobStore> jobStoreProvider,
                            final Provider<Scheduler> schedulerProvider,
                            final LastShutdownTimeService lastShutdownTimeService,
                            final DatabaseStatusDelayedExecutor delayedExecutor,
                            @Named("${nexus.quartz.recoverInterruptedJobs:-true}") final boolean recoverInterruptedJobs)
  {
    this.eventManager = checkNotNull(eventManager);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.jobStoreProvider = checkNotNull(jobStoreProvider);
    this.schedulerProvider = checkNotNull(schedulerProvider);
    this.lastShutdownTimeService = checkNotNull(lastShutdownTimeService);
    this.recoverInterruptedJobs = recoverInterruptedJobs;
    this.delayedExecutor = checkNotNull(delayedExecutor);

    this.scheduleFactory = new QuartzScheduleFactory();
    this.triggerConverter = new QuartzTriggerConverter(this.scheduleFactory);

    // FIXME: sort out with refinement to lifecycle below
    this.active = true;
  }

  public QuartzTriggerConverter triggerConverter() {
    return triggerConverter;
  }

  @VisibleForTesting
  Scheduler getScheduler() {
    return scheduler;
  }

  //
  // Lifecycle
  //
  @Override
  protected void doStart() throws Exception {
    // create new scheduler
    scheduler = schedulerProvider.get();

    try {
      // access internal scheduler to simulate signals for remote updates
      Field schedField = scheduler.getClass().getDeclaredField("sched");
      schedField.setAccessible(true);
      quartzScheduler = (QuartzScheduler) schedField.get(scheduler);
    }
    catch (Exception | LinkageError e) {
      log.error("Cannot find QuartzScheduler", e);
      throw e;
    }

    // re-attach listeners right after scheduler is available
    reattachListeners();
  }

  private void reattachListeners() {
    final Optional<Date> lastShutdownTime = lastShutdownTimeService.estimateLastShutdownTime();
    forEachNexusJob((final Trigger trigger, final JobDetail jobDetail) -> {
      try {
        updateLastRunStateInfo(jobDetail, lastShutdownTime);
      }
      catch (SchedulerException e) {
        log.error("Error updating last run state for {}", jobDetail.getKey(), e);
      }
    });

    forEachNexusJob((final Trigger trigger, final JobDetail jobDetail) -> {
      try {
        stubJobListener(jobDetail);
      }
      catch (SchedulerException e) {
        log.error("Error attaching job listener to {}", jobDetail.getKey(), e);
      }
    });

    delayedExecutor.execute(() -> {
      forEachNexusJob((final Trigger trigger, final JobDetail jobDetail) -> {
        try {
          updateJobListener(trigger);
        }
        catch (SchedulerException e) {
          log.error("Error attaching job listener to {}", jobDetail.getKey(), e);
        }
      });

      if (recoverInterruptedJobs) {
        forEachNexusJob(this::recoverJob);
      }
    });
  }

  private void forEachNexusJob(final BiConsumer<Trigger, JobDetail> consumer) {
    try {
      for (Entry<Trigger, JobDetail> entry : getNexusJobs().entrySet()) {
        consumer.accept(entry.getKey(), entry.getValue());
      }
    }
    catch (SchedulerException e) {
      log.error("Error getting jobs to process", e);
    }
  }

  @VisibleForTesting
  void recoverJob(final Trigger trigger, final JobDetail jobDetail) {
    if (shouldRecoverJob(trigger, jobDetail)) {
      try {
        Trigger newTrigger = newTrigger()
            .usingJobData(trigger.getJobDataMap())
            .withDescription("Recovery of " + trigger.getDescription())
            .forJob(jobDetail)
            .startNow()
            .build();
        log.info("Recovering job {}", newTrigger.getJobKey());
        scheduler.scheduleJob(newTrigger);
      }
      catch (SchedulerException e) {
        log.error("Failed to recover job {}", trigger.getJobKey(), e);
      }
    }
  }

  private static Boolean shouldRecoverJob(final Trigger trigger, final JobDetail jobDetail) {
    return (jobDetail.requestsRecovery() && isInterruptedJob(jobDetail)) || isRunNow(trigger);
  }

  /**
   * Checks the last run time against its last trigger fire time.
   * If the trigger's last fire time doesn't match with the jobs last fire time,
   * then the {@link TaskState} is set to interrupted
   *
   * @param nexusLastRunTime - approximate time at which the last instance of nexus was shutdown
   */
  protected void updateLastRunStateInfo(final JobDetail jobDetail, final Optional<Date> nexusLastRunTime)
      throws SchedulerException
  {
    Optional<Date> latestFireWrapper = scheduler.getTriggersOfJob(jobDetail.getKey()).stream()
        .filter(Objects::nonNull)
        .map(Trigger::getPreviousFireTime)
        .filter(Objects::nonNull)
        .max(Date::compareTo);

    if (latestFireWrapper.isPresent()) {
      TaskConfiguration taskConfig = configurationOf(jobDetail);
      Date latestFire = latestFireWrapper.get();

      if (!taskConfig.hasLastRunState() || taskConfig.getLastRunState().getRunStarted().before(latestFire)) {
        long estimatedDuration = Math.max(nexusLastRunTime.orElse(latestFire).getTime() - latestFire.getTime(), 0);
        taskConfig.setLastRunState(INTERRUPTED, latestFire, estimatedDuration);

        log.warn("Updating lastRunState to interrupted for jobKey {} taskConfig: {}", jobDetail.getKey(), taskConfig);
        try {
          updateJobData(jobDetail, taskConfig);
          scheduler.addJob(jobDetail, true, true);
        }
        catch (RuntimeException e) {
          log.warn("Problem updating lastRunState to interrupted for jobKey {}", jobDetail.getKey(), e);
        }
      }
    }
  }

  @Override
  protected void doStop() throws Exception {
    scheduler = null;
  }

  // TODO: Simplify active/pause/resume bits here
  @Override
  public void pause() {
    try {
      setActive(false);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void resume() {
    try {
      setActive(true);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  private void setActive(final boolean started) throws SchedulerException {
    this.active = started;
    if (isStarted()) {
      applyActive();
    }
  }

  private void applyActive() throws SchedulerException {
    try (TcclBlock tccl = TcclBlock.begin(this)) {
      if (!active && !scheduler.isInStandbyMode()) {
        scheduler.standby();
        log.info("Scheduler put into stand-by mode");
      }
      else if (active && scheduler.isInStandbyMode()) {
        scheduler.start();
        log.info("Scheduler put into ready mode");
      }
    }
  }

  //
  // JobListeners
  //

  /**
   * Schedules a manually executable trigger for a job missing a trigger and adds marker for health check reporting
   */
  private Trigger scheduleJobWithManualTrigger(final JobKey jobKey,
                                               final JobDetail jobDetail,
                                               final TriggerKey triggerKey) throws SchedulerException
  {
    log.error("Missing trigger for key: {}", jobKey);
    Trigger trigger = triggerConverter.convert(new Manual())
        .usingJobData(jobDetail.getJobDataMap())
        .usingJobData(MISSING_TRIGGER_RECOVERY, jobKey.getName())
        .withIdentity(triggerKey)
        .withDescription(jobDetail.getDescription())
        .forJob(jobDetail)
        .build();
    log.info("Rescheduling job '{}' with manual trigger", jobDetail.getDescription());
    scheduler.scheduleJob(trigger);
    return trigger;
  }

  /**
   * Attach {@link QuartzTaskJobListener} to job.
   */
  protected QuartzTaskJobListener attachJobListener(
      final JobDetail jobDetail,
      final Trigger trigger) throws SchedulerException
  {
    log.debug("Initializing task-state: jobDetail={}, trigger={}", jobDetail, trigger);

    Date now = new Date();
    TaskConfiguration taskConfiguration = configurationOf(jobDetail);
    Schedule schedule = triggerConverter.convert(trigger);
    QuartzTaskState taskState = new QuartzTaskState(
        taskConfiguration,
        schedule,
        trigger.getFireTimeAfter(now)
    );

    QuartzTaskFuture future = null;
    if (scheduler.getTriggerState(trigger.getKey()) == TriggerState.BLOCKED) {
      // This is a task already running
      future = new QuartzTaskFuture(
          this,
          jobDetail.getKey(),
          taskConfiguration.getTaskLogName(),
          trigger.getStartTime(), // TODO verify this
          schedule,
          null
      );
    }
    else if (schedule instanceof Now) {
      future = new QuartzTaskFuture(
          this,
          jobDetail.getKey(),
          taskConfiguration.getTaskLogName(),
          now,
          schedule,
          null
      );
    }

    QuartzTaskJobListener listener = new QuartzTaskJobListener(
        listenerName(jobDetail.getKey()),
        eventManager,
        this,
        new QuartzTaskInfo(eventManager, this, jobDetail.getKey(), taskState, future)
    );

    scheduler.getListenerManager().addJobListener(listener, keyEquals(jobDetail.getKey()));

    return listener;
  }

  /**
   * Creates a stub of a {@link QuartzTaskJobListener} attached to the job with scheduling unset.
   * See NEXUS-18983
   */
  private QuartzTaskJobListener stubJobListener(final JobDetail jobDetail) throws SchedulerException {
    log.debug("Stubbing task-state: jobDetail={}", jobDetail);

    TaskConfiguration taskConfiguration = configurationOf(jobDetail);
    Schedule schedule = scheduleFactory.manual();
    QuartzTaskState taskState = new QuartzTaskState(taskConfiguration, schedule, null);

    QuartzTaskJobListener listener = new QuartzTaskJobListener(listenerName(jobDetail.getKey()), eventManager, this,
        new QuartzTaskInfo(eventManager, this, jobDetail.getKey(), taskState, null));

    scheduler.getListenerManager().addJobListener(listener, keyEquals(jobDetail.getKey()));

    return listener;
  }

  /**
   * Returns listener for given job, or null if not found.
   */
  @Nullable
  private QuartzTaskJobListener findJobListener(final JobKey jobKey) throws SchedulerException {
    String name = listenerName(jobKey);
    return (QuartzTaskJobListener) scheduler.getListenerManager().getJobListener(name);
  }

  protected void updateJobListener(final JobDetail jobDetail) throws SchedulerException {
    QuartzTaskJobListener toBeUpdated = findJobListener(jobDetail.getKey());
    if (toBeUpdated != null) {
      QuartzTaskInfo taskInfo = toBeUpdated.getTaskInfo();
      taskInfo.setNexusTaskStateIfWaiting(
          new QuartzTaskState(
              taskInfo.getConfiguration().apply(configurationOf(jobDetail)),
              taskInfo.getSchedule(),
              taskInfo.getCurrentState().getNextRun()
          ),
          taskInfo.getTaskFuture()
      );
    }
  }

  protected void updateJobListener(final Trigger trigger) throws SchedulerException {
    QuartzTaskJobListener toBeUpdated = findJobListener(trigger.getJobKey());
    if (toBeUpdated != null) {
      QuartzTaskInfo taskInfo = toBeUpdated.getTaskInfo();
      taskInfo.setNexusTaskStateIfWaiting(
          new QuartzTaskState(
              taskInfo.getConfiguration(),
              triggerConverter.convert(trigger),
              trigger.getFireTimeAfter(new Date())
          ),
          taskInfo.getTaskFuture()
      );
    }
  }

  protected void removeJobListener(final JobKey jobKey) throws SchedulerException {
    String name = listenerName(jobKey);
    QuartzTaskJobListener toBeRemoved = (QuartzTaskJobListener) scheduler.getListenerManager().getJobListener(name);
    if (toBeRemoved != null) {
      // ensure future is done
      QuartzTaskFuture future = toBeRemoved.getTaskInfo().getTaskFuture();
      if (future != null && !future.isDone()) {
        future.doCancel();
      }
      scheduler.getListenerManager().removeJobListener(name);
    }
  }

  //
  // SchedulerSPI
  //

  @Override
  @Guarded(by = STARTED)
  public ScheduleFactory scheduleFactory() {
    return scheduleFactory;
  }

  @Override
  @Guarded(by = STARTED)
  public String renderStatusMessage() {
    StringBuilder buff = new StringBuilder();

    SchedulerMetaData metaData;
    try {
      metaData = scheduler.getMetaData();
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }

    if (metaData.isShutdown()) {
      buff.append("Shutdown");
    }
    else {
      if (metaData.getRunningSince() != null) {
        buff.append("Started");
      }
      else {
        buff.append("Stopped");
      }
      if (metaData.isInStandbyMode()) {
        buff.append("; Stand-by");
      }
    }
    return buff.toString();
  }

  @Override
  @Guarded(by = STARTED)
  public String renderDetailMessage() {
    try {
      return scheduler.getMetaData().getSummary();
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Nullable
  @Guarded(by = STARTED)
  public TaskInfo getTaskById(final String id) {
    try {
      QuartzTaskInfo task = findTaskById(id);
      if (task != null && !task.isRemovedOrDone()) {
        return task;
      }
    }
    catch (IllegalStateException e) {
      // no listener found in taskByKey, means no job exists
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public List<TaskInfo> listsTasks() {
    try {
      // returns all tasks which are NOT removed or done
      return allTasks().values().stream()
          .filter((task) -> !task.isRemovedOrDone())
          .collect(Collectors.toList());
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public List<String> getMissingTriggerDescriptions() {
    try {
      try (TcclBlock tccl = TcclBlock.begin(this)) {
        Set<JobKey> jobKeys = scheduler.getJobKeys(jobGroupEquals(GROUP_NAME));
        List<String> missingJobDescriptions = new ArrayList<>();
        for (JobKey jobKey : jobKeys) {
          Trigger trigger = scheduler.getTrigger(triggerKey(jobKey.getName(), jobKey.getGroup()));
          if (trigger.getJobDataMap().containsKey(MISSING_TRIGGER_RECOVERY)) {
            missingJobDescriptions.add(trigger.getDescription());
          }
        }
        return missingJobDescriptions;
      }
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public TaskInfo scheduleTask(final TaskConfiguration config,
                               final Schedule schedule)
  {
    checkState(!EventHelper.isReplicating(), "Replication in progress");

    try (TcclBlock tccl = TcclBlock.begin(this)) {
      // check for existing task with same id
      QuartzTaskInfo old = findTaskById(config.getId());

      if (old != null) {
        return updateJob(old, config, schedule);
      }
      else {
        return createNewJob(config, schedule);
      }
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  protected QuartzTaskInfo createNewJob(
      final TaskConfiguration config,
      final Schedule schedule) throws SchedulerException
  {
    // Use always new jobKey, as if THIS task reschedules THIS/itself, "new" should not interfere with "this"
    // Currently only healthcheck does this, by rescheduling itself
    JobKey jobKey = JobKey.jobKey(UUID.randomUUID().toString(), GROUP_NAME);

    // get trigger, but use identity of jobKey
    // This is only for simplicity, as is not a requirement: NX job:triggers are 1:1 so tying them as this is ok
    // ! create the trigger before eventual TaskInfo remove bellow to avoid task removal in case of an invalid trigger
    JobDetail jobDetail = buildJob(config, jobKey);
    Trigger trigger = buildTrigger(schedule, jobDetail);

    log.debug("Task {} : {} scheduled with key: {} and schedule: {}",
        config.getId(),
        config.getTaskLogName(),
        jobKey.getName(),
        schedule
    );

    // register job specific listener with initial state
    QuartzTaskJobListener listener = attachJobListener(jobDetail, trigger);

    scheduler.scheduleJob(jobDetail, trigger);

    if (!config.isEnabled()) {
      scheduler.pauseJob(jobKey);
    }

    return listener.getTaskInfo();
  }

  protected QuartzTaskInfo updateJob(
      final QuartzTaskInfo old,
      final TaskConfiguration config,
      final Schedule schedule) throws SchedulerException
  {
    checkState(!(old.getSchedule() instanceof Now), "Run 'now' task cannot be rescheduled");
    checkState(!old.isRemovedOrDone(), "Done task cannot be rescheduled");
    QuartzTaskFuture future = old.getTaskFuture();
    if (future != null) { // is running
      checkState(!(schedule instanceof Now), "Running task cannot be rescheduled with 'now'");
    }

    log.debug("Task {} : {} rescheduled {} -> {} ",
        old.getJobKey().getName(),
        old.getConfiguration().getTaskLogName(),
        old.getSchedule(),
        schedule
    );

    JobDetail jobDetail = buildJob(config, old.getJobKey());
    Trigger trigger = buildTrigger(schedule, jobDetail);

    scheduler.addJob(jobDetail, true, true);
    scheduler.rescheduleJob(trigger.getKey(), trigger);

    JobDataMap jobData = trigger.getJobDataMap();
    String type = jobData.getString(Schedule.SCHEDULE_TYPE);

    if (Cron.TYPE.equals(type)) {
      verifyCron(jobData);
    }

      // update TaskInfo, but only if it's WAITING, as running one will pick up the change by job listener when done
    old.setNexusTaskStateIfWaiting(
        new QuartzTaskState(
            config,
            schedule,
            trigger.getFireTimeAfter(new Date())
        ),
        future
    );

    if (!config.isEnabled()) {
      scheduler.pauseJob(old.getJobKey());
    }
    else {
      scheduler.resumeJob(old.getJobKey());
    }

    return old;
  }

  private void verifyCron(final JobDataMap jobData) throws SchedulerException {
    Date startAt = stringToDate(jobData.getString(SCHEDULE_START_AT));
    String cronExpression = jobData.getString(Cron.SCHEDULE_CRON_EXPRESSION);
    try {
      scheduleFactory.cron(startAt, cronExpression);
    }
    catch (Exception e) {
      throw new SchedulerException(e);
    }
  }

  private JobDetail buildJob(final TaskConfiguration config, final JobKey jobKey) {
    return JobBuilder.newJob(QuartzTaskJob.class)
        .withIdentity(jobKey)
        .withDescription(config.getName())
        .requestRecovery(config.isRecoverable())
        .usingJobData(new JobDataMap(config.asMap()))
        .build();
  }

  private Trigger buildTrigger(final Schedule schedule, final JobDetail jobDetail) {
    return ensureStartsInTheFuture(triggerConverter.convert(schedule)
        .withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
        .withDescription(jobDetail.getDescription())
        .usingJobData(new JobDataMap(filterKeys(jobDetail.getJobDataMap(), INHERITED_CONFIG_KEYS::contains)))
        .build());
  }

  @Override
  @Guarded(by = STARTED)
  public int getRunningTaskCount() {
    try (TcclBlock tccl = TcclBlock.begin(this)) {
      return scheduler.getCurrentlyExecutingJobs().size();
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public int getExecutedTaskCount() {
    return quartzScheduler.numJobsExecuted();
  }

  //
  // Internal
  //

  /**
   * Ensure that trigger start date is not in the past.
   */
  private Trigger ensureStartsInTheFuture(final Trigger trigger) {
    Date now = new Date();
    if (trigger.getStartTime().before(now)) {
      Date fireTimeAfter = trigger.getFireTimeAfter(now);
      if (fireTimeAfter != null) {
        return trigger.getTriggerBuilder().startAt(fireTimeAfter).build();
      }
    }
    return trigger;
  }

  /**
   * Returns all tasks for the {@link #GROUP_NAME} group, which also have attached job-listeners.
   */
  protected Map<JobKey, QuartzTaskInfo> allTasks() throws SchedulerException {
    try (TcclBlock tccl = TcclBlock.begin(this)) {
      Map<JobKey, QuartzTaskInfo> result = new HashMap<>();

      Set<JobKey> jobKeys = scheduler.getJobKeys(jobGroupEquals(GROUP_NAME));
      for (JobKey jobKey : jobKeys) {
        QuartzTaskJobListener listener = findJobListener(jobKey);
        if (listener != null) {
          result.put(jobKey, listener.getTaskInfo());
        }
        else {
          // TODO: Sort out if this is normal or edge-case indicative of a bug or not
          log.debug("Job missing listener; omitting from results: {}", jobKey);
        }
      }

      return result;
    }
  }

  private Map<Trigger, JobDetail> getNexusJobs() throws SchedulerException {
    Map<Trigger, JobDetail> nexusJobs = new HashMap<>();
    try (TcclBlock tccl = TcclBlock.begin(this)) {
      Set<JobKey> jobKeys = scheduler.getJobKeys(jobGroupEquals(GROUP_NAME));
      for (JobKey jobKey : jobKeys) {
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        if (jobDetail == null) {
          log.error("Missing job-detail for key: {}", jobKey);
          continue;
        }

        TriggerKey triggerKey = triggerKey(jobKey.getName(), jobKey.getGroup());
        Trigger trigger = scheduler.getTrigger(triggerKey);
        if (trigger == null) {
          trigger = scheduleJobWithManualTrigger(jobKey, jobDetail, triggerKey);
        }

        nexusJobs.put(trigger, jobDetail);
      }
    }
    return nexusJobs;
  }

  /**
   * Returns task-info for given identifier, or null.
   */
  @Nullable
  @VisibleForTesting
  protected QuartzTaskInfo findTaskById(final String id) throws SchedulerException {
    try (TcclBlock tccl = TcclBlock.begin(this)) {
      return allTasks().values().stream()
          .filter((task) -> task.getId().equals(id))
          .findFirst()
          .orElse(null);
    }
  }

  /**
   * Used by {@link QuartzTaskFuture#cancel(boolean)}.
   */
  @Guarded(by = STARTED)
  public boolean cancelJob(final JobKey jobKey) {
    checkNotNull(jobKey);

    try (TcclBlock tccl = TcclBlock.begin(this)) {
      return scheduler.interrupt(jobKey);
    }
    catch (UnableToInterruptJobException e) {
      log.debug("Unable to interrupt job with key: {}", jobKey, e);
    }
    return false;
  }

  /**
   * Used by {@link QuartzTaskInfo#runNow()}.
   */
  @Guarded(by = STARTED)
  public void runNow(final String triggerSource,
                     final JobKey jobKey,
                     final QuartzTaskInfo taskInfo,
                     final QuartzTaskState taskState)
      throws TaskRemovedException, SchedulerException
  {
    checkState(active, "Cannot run tasks while scheduler is paused");

    TaskConfiguration config = taskState.getConfiguration();

    // avoid marking local state as running if task is limited to run on a different node
    if (!isLimitedToAnotherNode(config)) {
      taskInfo.setNexusTaskState(
          RUNNING,
          taskState,
          new QuartzTaskFuture(
              this,
              jobKey,
              config.getTaskLogName(),
              new Date(),
              scheduleFactory().now(),
              triggerSource
          )
      );
    }

    try (TcclBlock tccl = TcclBlock.begin(this)) {
      // triggering with dataMap from "now" trigger as it contains metadata for back-conversion in listener
      JobDataMap triggerDetail = triggerConverter.convert(scheduleFactory().now()).build().getJobDataMap();
      triggerDetail.putAll(filterKeys(config.asMap(), INHERITED_CONFIG_KEYS::contains));
      scheduler.triggerJob(jobKey, triggerDetail);
    }
    catch (JobPersistenceException e) {
      throw new TaskRemovedException(jobKey.getName(), e);
    }
  }

  /**
   * Used by {@link QuartzTaskInfo#remove()}.
   */
  @Guarded(by = STARTED)
  public boolean removeTask(final JobKey jobKey) {
    try (TcclBlock tccl = TcclBlock.begin(this)) {
      boolean result = false;
      List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
      for (Trigger trigger : triggers) {
        result = scheduler.unscheduleJob(trigger.getKey()) || result;
      }
      removeJobListener(jobKey);
      return result;
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Guarded(by = STARTED)
  @Override
  public boolean cancel(final String id, final boolean mayInterruptIfRunning) {
    return Optional.ofNullable(id)
        .map(this::getTaskById)
        .map(TaskInfo::getCurrentState)
        .map(CurrentState::getFuture)
        .map(f -> f.cancel(mayInterruptIfRunning))
        .orElse(false);
  }

  @Nullable
  @Override
  public TaskInfo getTaskByTypeId(final String typeId) {
    return getTaskByTypeId(typeId, emptyMap());
  }

  @Nullable
  @Override
  public TaskInfo getTaskByTypeId(final String typeId, final Map<String, String> config) {
    checkNotNull(typeId);
    checkNotNull(config);
    return listsTasks().stream()
        .filter(t -> typeId.equals(t.getTypeId()))
        .filter(matchConfig(config))
        .findFirst()
        .orElse(null);
  }

  @Override
  public boolean findAndSubmit(final String typeId) {
    return findAndSubmit(typeId, emptyMap());
  }

  public boolean findWaitingTask(final String typeId, final Map<String, String> config) {
    TaskInfo taskInfo = getTaskByTypeId(typeId, config);
    if (taskInfo == null) {
      return false;
    } else {
      return taskInfo.getCurrentState().getState().isWaiting() || taskInfo.getCurrentState().getState().isRunning();
    }
  }

  @Override
  public boolean findAndSubmit(final String typeId, final Map<String, String> config) {
    checkNotNull(typeId);
    checkNotNull(config);
    TaskInfo taskInfo = getTaskByTypeId(typeId, config);
    if (taskInfo == null) {
      return false;
    }
    else {
      try {
        if (!taskInfo.getCurrentState().getState().isRunning()) {
          taskInfo.runNow();
        }
      }
      catch (TaskRemovedException e) {
        log.error("Unable to submit task: {}", taskInfo, e);
      }
      return true;
    }
  }

  private Predicate<TaskInfo> matchConfig(final Map<String, String> config) {
    return t -> {
      TaskConfiguration tc = t.getConfiguration();
      return config.entrySet().stream()
          .filter(e -> e.getKey() != null)
          .filter(e -> e.getValue() != null)
          .allMatch(e -> e.getValue().equals(tc.getString(e.getKey())));
    };
  }


  /**
   * See {@link QuartzTaskInfo#runNow(String)}
   *
   * @since 3.2
   */
  public boolean isLimitedToAnotherNode(final TaskConfiguration config) {
    if (nodeAccess.isClustered() && config.containsKey(LIMIT_NODE_KEY)) {
      String limitedNodeId = config.getString(LIMIT_NODE_KEY);
      checkState(!Strings2.isBlank(limitedNodeId),
          "Task '%s' is not configured for HA", config.getName());
      checkState(nodeAccess.getMemberIds().contains(limitedNodeId),
          "Task '%s' uses node %s which is not a member of this cluster", config.getName(), limitedNodeId);
      return !nodeAccess.getId().equals(limitedNodeId);
    }
    return false;
  }

  protected boolean isLimitedToThisNode(final Trigger trigger) {
    // can skip isClustered check because this method is only called when in HA mode
    return nodeAccess.getId().equals(trigger.getJobDataMap().getString(LIMIT_NODE_KEY));
  }

  protected static boolean isRunNow(final Trigger trigger) {
    return Now.TYPE.equals(trigger.getJobDataMap().getString(Schedule.SCHEDULE_TYPE));
  }

  private static boolean isInterruptedJob(final JobDetail jobDetail) {
    return INTERRUPTED.name().equals(jobDetail.getJobDataMap().getString(LAST_RUN_STATE_END_STATE));
  }

  protected static long getNextFireMillis(final Trigger trigger) {
    Date nextFireTime = trigger.getNextFireTime();
    return nextFireTime != null ? nextFireTime.getTime() : 0L;
  }
}
