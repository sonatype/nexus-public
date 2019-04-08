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
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LastShutdownTimeService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.orient.DatabaseStatusDelayedExecutor;
import org.sonatype.nexus.quartz.internal.orient.JobCreatedEvent;
import org.sonatype.nexus.quartz.internal.orient.JobDeletedEvent;
import org.sonatype.nexus.quartz.internal.orient.JobUpdatedEvent;
import org.sonatype.nexus.quartz.internal.orient.TriggerCreatedEvent;
import org.sonatype.nexus.quartz.internal.orient.TriggerDeletedEvent;
import org.sonatype.nexus.quartz.internal.orient.TriggerUpdatedEvent;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskFuture;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJob;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.EndState;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.DefaultThreadExecutor;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.spi.JobFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.ThreadExecutor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.filterKeys;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.quartz.impl.matchers.KeyMatcher.keyEquals;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskJob.configurationOf;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskJob.updateJobData;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener.listenerName;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskState.LAST_RUN_STATE_END_STATE;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskState.getLastRunState;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskState.hasLastRunState;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskState.setLastRunState;
import static org.sonatype.nexus.scheduling.TaskDescriptorSupport.LIMIT_NODE_KEY;
import static org.sonatype.nexus.scheduling.TaskInfo.EndState.INTERRUPTED;

/**
 * Quartz {@link SchedulerSPI}.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class QuartzSchedulerSPI
    extends StateGuardLifecycleSupport
    implements SchedulerSPI, EventAware
{
  private static final String SCHEDULER_NAME = "nexus";

  private static final String GROUP_NAME = "nexus";

  private static final Set<String> INHERITED_CONFIG_KEYS = ImmutableSet.of(LIMIT_NODE_KEY);

  private final EventManager eventManager;

  private final NodeAccess nodeAccess;

  private final Provider<JobStore> jobStoreProvider;

  private final JobFactory jobFactory;

  private final int threadPoolSize;

  private final ScheduleFactory scheduleFactory;

  private final QuartzTriggerConverter triggerConverter;

  private final LastShutdownTimeService lastShutdownTimeService;

  private final DatabaseStatusDelayedExecutor delayedExecutor;

  private final boolean recoverInterruptedJobs;

  private Scheduler scheduler;

  private QuartzScheduler quartzScheduler;

  private boolean active;

  @SuppressWarnings("squid:S00107") //suppress constructor parameter count
  @Inject
  public QuartzSchedulerSPI(final EventManager eventManager,
                            final NodeAccess nodeAccess,
                            final Provider<JobStore> jobStoreProvider,
                            final JobFactory jobFactory,
                            final LastShutdownTimeService lastShutdownTimeService,
                            final DatabaseStatusDelayedExecutor delayedExecutor,
                            @Named("${nexus.quartz.poolSize:-20}") final int threadPoolSize,
                            @Named("${nexus.quartz.recoverInterruptedJobs:-true}") final boolean recoverInterruptedJobs)
  {
    this.eventManager = checkNotNull(eventManager);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.jobStoreProvider = checkNotNull(jobStoreProvider);
    this.jobFactory = checkNotNull(jobFactory);
    this.lastShutdownTimeService = checkNotNull(lastShutdownTimeService);
    this.recoverInterruptedJobs = recoverInterruptedJobs;
    this.delayedExecutor = checkNotNull(delayedExecutor);

    checkArgument(threadPoolSize > 0, "Invalid thread-pool size: %s", threadPoolSize);
    this.threadPoolSize = threadPoolSize;
    log.info("Thread-pool size: {}", threadPoolSize);

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
    scheduler = createScheduler();

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
    delayedExecutor.execute(() -> {
      final Optional<Date> lastShutdownTime = lastShutdownTimeService.estimateLastShutdownTime();
      forEachNexusJob((Trigger trigger, JobDetail jobDetail) -> {
        try {
          updateLastRunStateInfo(jobDetail, lastShutdownTime);
        }
        catch (SchedulerException e) {
          log.error("Error updating last run state for {}", jobDetail.getKey(), e);
        }
      });

      forEachNexusJob((Trigger trigger, JobDetail jobDetail) -> {
        try {
          attachJobListener(jobDetail, trigger);
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

  private void forEachNexusJob(BiConsumer<Trigger, JobDetail> consumer) {
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
   * then the {@link EndState} is set to interrupted
   *
   * @param nexusLastRunTime - approximate time at which the last instance of nexus was shutdown
   */
  private void updateLastRunStateInfo(final JobDetail jobDetail, Optional<Date> nexusLastRunTime)
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

      if (!hasLastRunState(taskConfig) || getLastRunState(taskConfig).getRunStarted().before(latestFire)) {
        long estimatedDuration = Math.max(nexusLastRunTime.orElse(latestFire).getTime() - latestFire.getTime(), 0);
        setLastRunState(taskConfig, EndState.INTERRUPTED, latestFire, estimatedDuration);

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

  /**
   * Create a new {@link Scheduler} and set to stand-by mode.
   */
  private Scheduler createScheduler() throws SchedulerException {
    // ensure executed threads have TCCL set
    ThreadExecutor threadExecutor = new DefaultThreadExecutor()
    {
      @Override
      public void execute(final Thread thread) {
        thread.setContextClassLoader(QuartzSchedulerSPI.class.getClassLoader());
        super.execute(thread);
      }
    };

    // create Scheduler (implicitly registers it with repository)
    DirectSchedulerFactory.getInstance().createScheduler(
        SCHEDULER_NAME,
        nodeAccess.getId(), // instance-id
        new QuartzThreadPool(threadPoolSize),
        threadExecutor,
        jobStoreProvider.get(),
        null, // scheduler plugin-map
        null, // rmi-registry host
        0,    // rmi-registry port
        -1,   // idle-wait time
        -1,   // db-failure retry-interval
        true, // jmx-export
        null, // custom jmx object-name, lets use the default
        1,    // max batch-size
        0L    // batch time-window
    );
    Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler(SCHEDULER_NAME);
    scheduler.setJobFactory(jobFactory);

    // re-logging with version, as by default we limit quartz logging to WARN, hiding its default version logging
    log.info("Quartz Scheduler v{}", scheduler.getMetaData().getVersion());

    scheduler.standby();

    return scheduler;
  }

  @Override
  protected void doStop() throws Exception {
    // shutdown and unregister the scheduler instance
    scheduler.shutdown();
    SchedulerRepository.getInstance().remove(SCHEDULER_NAME);
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
    if (!active && !scheduler.isInStandbyMode()) {
      scheduler.standby();
      log.info("Scheduler put into stand-by mode");
    }
    else if (active && scheduler.isInStandbyMode()) {
      scheduler.start();
      log.info("Scheduler put into ready mode");
    }
  }

  //
  // JobListeners
  //

  /**
   * Attach {@link QuartzTaskJobListener} to job.
   */
  private QuartzTaskJobListener attachJobListener(final JobDetail jobDetail,
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
    if (schedule instanceof Now) {
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
   * Returns listener for given job, or null if not found.
   */
  @Nullable
  private QuartzTaskJobListener findJobListener(final JobKey jobKey) throws SchedulerException {
    String name = listenerName(jobKey);
    return (QuartzTaskJobListener) scheduler.getListenerManager().getJobListener(name);
  }

  private void updateJobListener(final JobDetail jobDetail) throws SchedulerException {
    QuartzTaskJobListener toBeUpdated = findJobListener(jobDetail.getKey());
    if (toBeUpdated != null) {
      QuartzTaskInfo taskInfo = toBeUpdated.getTaskInfo();
      taskInfo.setNexusTaskStateIfInState(
          TaskInfo.State.WAITING,
          new QuartzTaskState(
              taskInfo.getConfiguration().apply(configurationOf(jobDetail)),
              taskInfo.getSchedule(),
              taskInfo.getCurrentState().getNextRun()
          ),
          taskInfo.getTaskFuture()
      );
    }
  }

  private void updateJobListener(final Trigger trigger) throws SchedulerException {
    QuartzTaskJobListener toBeUpdated = findJobListener(trigger.getJobKey());
    if (toBeUpdated != null) {
      QuartzTaskInfo taskInfo = toBeUpdated.getTaskInfo();
      taskInfo.setNexusTaskStateIfInState(
          TaskInfo.State.WAITING,
          new QuartzTaskState(
              taskInfo.getConfiguration(),
              triggerConverter.convert(trigger),
              trigger.getFireTimeAfter(new Date())
          ),
          taskInfo.getTaskFuture()
      );
    }
  }

  private void removeJobListener(final JobKey jobKey) throws SchedulerException {
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
  public TaskInfo scheduleTask(final TaskConfiguration config,
                               final Schedule schedule)
  {
    checkState(!EventHelper.isReplicating(), "Replication in progress");

    try (TcclBlock tccl = TcclBlock.begin(this)) {
      // check for existing task with same id
      QuartzTaskInfo old = findTaskById(config.getId());

      if (old != null) {

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

        // update TaskInfo, but only if it's WAITING, as running one will pick up the change by job listener when done
        old.setNexusTaskStateIfInState(
            TaskInfo.State.WAITING,
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
      else {
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
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
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
  private Map<JobKey, QuartzTaskInfo> allTasks() throws SchedulerException {
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
          log.warn("Job missing listener; omitting from results: {}", jobKey);
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

        Trigger trigger = scheduler.getTrigger(triggerKey(jobKey.getName(), jobKey.getGroup()));
        if (trigger == null) {
          log.error("Missing trigger for key: {}", jobKey);
          continue;
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
  private QuartzTaskInfo findTaskById(final String id) throws SchedulerException {
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
          TaskInfo.State.RUNNING,
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

  @Subscribe
  public void on(JobCreatedEvent event) {
    if (!event.isLocal() && isStarted()) {
      JobDetail jobDetail = event.getJob().getValue();

      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
      quartzScheduler.notifySchedulerListenersJobAdded(jobDetail);
    }
  }

  @Subscribe
  public void on(JobUpdatedEvent event) throws SchedulerException {
    if (!event.isLocal() && isStarted()) {
      JobDetail jobDetail = event.getJob().getValue();

      updateJobListener(jobDetail);

      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
      quartzScheduler.notifySchedulerListenersJobAdded(jobDetail);
    }
  }

  @Subscribe
  public void on(JobDeletedEvent event) throws SchedulerException {
    if (!event.isLocal() && isStarted()) {
      JobDetail jobDetail = event.getJob().getValue();

      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
      quartzScheduler.notifySchedulerListenersJobDeleted(jobDetail.getKey());

      removeJobListener(jobDetail.getKey());
    }
  }

  @Subscribe
  public void on(TriggerCreatedEvent event) throws SchedulerException {
    if (!event.isLocal() && isStarted()) {
      Trigger trigger = event.getTrigger().getValue();
      if (!isRunNow(trigger)) {

        attachJobListener(jobStoreProvider.get().retrieveJob(trigger.getJobKey()), trigger);

        // simulate signals Quartz would have sent
        quartzScheduler.getSchedulerSignaler().signalSchedulingChange(getNextFireMillis(trigger));
        quartzScheduler.notifySchedulerListenersSchduled(trigger);
      }
      else if (isLimitedToThisNode(trigger)) {
        // special "run-now" task which was created on a different node to where it will run
        // when this happens we ping the scheduler to make sure it runs as soon as possible
        quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
        quartzScheduler.notifySchedulerListenersSchduled(trigger);
      }
    }
  }

  @Subscribe
  public void on(TriggerUpdatedEvent event) throws SchedulerException {
    if (!event.isLocal() && isStarted()) {
      Trigger trigger = event.getTrigger().getValue();
      if (!isRunNow(trigger)) {

        updateJobListener(trigger);

        // simulate signals Quartz would have sent
        quartzScheduler.getSchedulerSignaler().signalSchedulingChange(getNextFireMillis(trigger));
        quartzScheduler.notifySchedulerListenersUnscheduled(trigger.getKey());
        quartzScheduler.notifySchedulerListenersSchduled(trigger);
      }
    }
  }

  @Subscribe
  public void on(TriggerDeletedEvent event) throws SchedulerException {
    if (!event.isLocal() && isStarted()) {
      Trigger trigger = event.getTrigger().getValue();
      if (!isRunNow(trigger)) {

        // simulate signals Quartz would have sent
        quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
        quartzScheduler.notifySchedulerListenersUnscheduled(trigger.getKey());

        removeJobListener(trigger.getJobKey());
      }
    }
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

  private boolean isLimitedToThisNode(final Trigger trigger) {
    // can skip isClustered check because this method is only called when in HA mode
    return nodeAccess.getId().equals(trigger.getJobDataMap().getString(LIMIT_NODE_KEY));
  }

  private static boolean isRunNow(final Trigger trigger) {
    return Now.TYPE.equals(trigger.getJobDataMap().getString(Schedule.SCHEDULE_TYPE));
  }

  private static boolean isInterruptedJob(final JobDetail jobDetail) {
    return INTERRUPTED.name().equals(jobDetail.getJobDataMap().getString(LAST_RUN_STATE_END_STATE));
  }

  private static long getNextFireMillis(final Trigger trigger) {
    Date nextFireTime = trigger.getNextFireTime();
    return nextFireTime != null ? nextFireTime.getTime() : 0L;
  }
}
