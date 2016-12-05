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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.thread.TcclBlock;
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
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
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
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.quartz.impl.matchers.KeyMatcher.keyEquals;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener.listenerName;
import static org.sonatype.nexus.scheduling.TaskDescriptorSupport.LIMIT_NODE_KEY;

/**
 * Quartz {@link SchedulerSPI}.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class QuartzSchedulerSPI
    extends LifecycleSupport
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

  private Scheduler scheduler;

  private QuartzScheduler quartzScheduler;

  private boolean active;

  @Inject
  public QuartzSchedulerSPI(final EventManager eventManager,
                            final NodeAccess nodeAccess,
                            final Provider<JobStore> jobStoreProvider,
                            final JobFactory jobFactory,
                            @Named("${nexus.quartz.poolSize:-20}") final int threadPoolSize)
      throws Exception
  {
    this.eventManager = checkNotNull(eventManager);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.jobStoreProvider = checkNotNull(jobStoreProvider);
    this.jobFactory = checkNotNull(jobFactory);

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
    reattachJobListeners();
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
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void resume() {
    try {
      setActive(true);
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
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
   * Re-attach listeners to all existing jobs.
   */
  private void reattachJobListeners() throws SchedulerException {
    log.debug("Re-attaching listeners to jobs");

    // Install job supporting listeners for each NX task being scheduled
    Set<JobKey> jobKeys = scheduler.getJobKeys(jobGroupEquals(QuartzSchedulerSPI.GROUP_NAME));
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

      attachJobListener(jobDetail, trigger);
    }
  }

  /**
   * Attach {@link QuartzTaskJobListener} to job.
   */
  private QuartzTaskJobListener attachJobListener(final JobDetail jobDetail,
                                                  final Trigger trigger) throws SchedulerException
  {
    log.debug("Initializing task-state: jobDetail={}, trigger={}", jobDetail, trigger);

    Date now = new Date();
    TaskConfiguration taskConfiguration = QuartzTaskJob.configurationOf(jobDetail);
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
          State.WAITING,
          new QuartzTaskState(
              taskInfo.getConfiguration().apply(QuartzTaskJob.configurationOf(jobDetail)),
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
          State.WAITING,
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
  public ScheduleFactory scheduleFactory() {
    ensureStarted();

    return scheduleFactory;
  }

  @Override
  public String renderStatusMessage() {
    ensureStarted();

    StringBuilder buff = new StringBuilder();

    SchedulerMetaData metaData;
    try {
      metaData = scheduler.getMetaData();
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
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
  public String renderDetailMessage() {
    ensureStarted();

    try {
      return scheduler.getMetaData().getSummary();
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  @Nullable
  public TaskInfo getTaskById(final String id) {
    ensureStarted();

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
      throw Throwables.propagate(e);
    }
    return null;
  }

  @Override
  public List<TaskInfo> listsTasks() {
    ensureStarted();

    try {
      // returns all tasks which are NOT removed or done
      return allTasks().values().stream()
          .filter((task) -> !task.isRemovedOrDone())
          .collect(Collectors.toList());
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public TaskInfo scheduleTask(final TaskConfiguration config,
                               final Schedule schedule)
  {
    ensureStarted();

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
            State.WAITING,
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
      throw Throwables.propagate(e);
    }
  }

  private JobDetail buildJob(final TaskConfiguration config, final JobKey jobKey) {
    return JobBuilder.newJob(QuartzTaskJob.class)
        .withIdentity(jobKey)
        .withDescription(config.getName())
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
  public int getRunningTaskCount() {
    ensureStarted();

    try (TcclBlock tccl = TcclBlock.begin(this)) {
      return scheduler.getCurrentlyExecutingJobs().size();
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
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
  public boolean cancelJob(final JobKey jobKey) {
    checkNotNull(jobKey);
    ensureStarted();

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
  public void runNow(final JobKey jobKey, final TaskConfiguration config)
      throws TaskRemovedException, SchedulerException
  {
    ensureStarted();

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
  public boolean removeTask(final JobKey jobKey) {
    ensureStarted();

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
      throw Throwables.propagate(e);
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

  private static long getNextFireMillis(final Trigger trigger) {
    Date nextFireTime = trigger.getNextFireTime();
    return nextFireTime != null ? nextFireTime.getTime() : 0L;
  }
}
