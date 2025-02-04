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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerSPI;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;
import org.sonatype.nexus.thread.DatabaseStatusDelayedExecutor;

import com.google.common.eventbus.Subscribe;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.spi.JobStore;

import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener.listenerName;

/**
 * Quartz {@link SchedulerSPI}.
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class DatastoreQuartzSchedulerSPI
    extends QuartzSchedulerSPI
    implements EventAware
{
  private final Object mutex = new Object();

  @Inject
  public DatastoreQuartzSchedulerSPI(
      final EventManager eventManager,
      final NodeAccess nodeAccess,
      final Provider<JobStore> jobStoreProvider,
      final Provider<Scheduler> schedulerProvider,
      final LastShutdownTimeService lastShutdownTimeService,
      final DatabaseStatusDelayedExecutor delayedExecutor,
      @Named("${nexus.quartz.recoverInterruptedJobs:-true}") final boolean recoverInterruptedJobs)
  {
    super(eventManager, nodeAccess, jobStoreProvider, schedulerProvider, lastShutdownTimeService, delayedExecutor,
        recoverInterruptedJobs);
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();

    scheduler.getListenerManager().addSchedulerListener(new SchedulerListenerSupport()
    {
      @Override
      public void jobScheduled(final Trigger trigger) {
        if (!EventHelper.isReplicating()) {
          eventManager.post(new TriggerCreatedEvent(trigger.getKey()));
        }
      }

      @Override
      public void jobUnscheduled(final TriggerKey triggerKey) {
        if (!EventHelper.isReplicating()) {
          eventManager.post(new TriggerDeletedEvent(triggerKey));
        }
      }
    });
  }

  @Guarded(by = STARTED)
  @Override
  public boolean cancel(final String id, final boolean mayInterruptIfRunning) {
    boolean locallyCancelled = super.cancel(id, mayInterruptIfRunning);
    if (locallyCancelled) {
      return true;
    }

    if (!EventHelper.isReplicating()) {
      eventManager.post(new CancelJobEvent(id, mayInterruptIfRunning));
    }

    return false;
  }

  private Optional<QuartzTaskJobListener> attachJobListener(final JobKey jobKey) {
    try {
      TriggerKey triggerKey = triggerKey(jobKey.getName(), jobKey.getGroup());
      Trigger trigger = scheduler.getTrigger(triggerKey);
      JobDetail jobDetail = scheduler.getJobDetail(jobKey);
      return Optional.of(attachJobListener(jobDetail, trigger));
    }
    catch (SchedulerException e) {
      log.warn("Unable to attach listener for task '{}' cause {}", jobKey, e.getMessage(),
          log.isDebugEnabled() ? e : null);

      return Optional.empty();
    }
  }

  /*
   * Synchronized to prevent attaching a second listener to a locally created task
   */
  @Override
  protected synchronized QuartzTaskJobListener attachJobListener(
      final JobDetail jobDetail,
      final Trigger trigger) throws SchedulerException
  {
    // Check for an existing listener
    JobListener jobListener = scheduler.getListenerManager().getJobListener(listenerName(jobDetail.getKey()));
    if (jobListener != null) {
      updateJobListener(jobDetail);
      return (QuartzTaskJobListener) jobListener;
    }

    //
    return super.attachJobListener(jobDetail, trigger);
  }

  @Override
  protected Map<JobKey, QuartzTaskInfo> allTasks() throws SchedulerException {
    try (TcclBlock tccl = TcclBlock.begin(this)) {
      return scheduler.getJobKeys(jobGroupEquals(GROUP_NAME))
          .stream()
          .map(this::getOrCreateQuartzTaskInfo)
          .filter(Objects::nonNull)
          .collect(Collectors.toMap(QuartzTaskInfo::getJobKey, Function.identity()));
    }
  }

  @Nullable
  private QuartzTaskInfo getOrCreateQuartzTaskInfo(final JobKey jobKey) {
    QuartzTaskJobListener listener = getExistingListener(jobKey);
    if (listener != null) {
      return listener.getTaskInfo();
    }

    return attachJobListener(jobKey)
        .map(QuartzTaskJobListener::getTaskInfo)
        .orElse(null);
  }

  /*
   * Retrieves an existing listener from the Quartz scheduler's ListenerManager
   */
  @Nullable
  private QuartzTaskJobListener getExistingListener(final JobKey jobKey) {
    try {
      return (QuartzTaskJobListener) scheduler.getListenerManager().getJobListener(listenerName(jobKey));
    }
    catch (SchedulerException e) {
      log.debug("An error occurred retrieving the listener for jobKey {}", jobKey, e);
      return null;
    }
  }

  @Guarded(by = STARTED)
  @Override
  protected QuartzTaskInfo createNewJob(
      final TaskConfiguration config,
      final Schedule schedule) throws SchedulerException
  {
    QuartzTaskInfo taskInfo = super.createNewJob(config, schedule);
    if (!EventHelper.isReplicating()) {
      eventManager.post(new JobCreatedEvent(taskInfo.getJobKey()));
    }
    return taskInfo;
  }

  @Guarded(by = STARTED)
  @Override
  public boolean removeTask(final JobKey jobKey) {
    boolean removed = super.removeTask(jobKey);
    if (removed && !EventHelper.isReplicating()) {
      eventManager.post(new JobDeletedEvent(jobKey));
    }
    return removed;
  }

  @Guarded(by = STARTED)
  @Override
  protected QuartzTaskInfo updateJob(
      final QuartzTaskInfo old,
      final TaskConfiguration config,
      final Schedule schedule) throws SchedulerException
  {
    QuartzTaskInfo taskInfo = super.updateJob(old, config, schedule);
    if (!EventHelper.isReplicating()) {
      eventManager.post(new JobUpdatedEvent(taskInfo.getJobKey()));
    }
    return taskInfo;
  }

  @Subscribe
  public void on(final CancelJobEvent event) {
    if (EventHelper.isReplicating()) {
      log.debug("Received remote request to cancel {}", event.getId());
      cancel(event.getId(), event.isMayInterruptIfRunning());
    }
  }

  @Subscribe
  public void on(final JobCreatedEvent event) {
    handle(event, jobDetail -> {
      attachJobListener(jobDetail.getKey());
      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
      quartzScheduler.notifySchedulerListenersJobAdded(jobDetail);
    });
  }

  @Subscribe
  public void on(final JobDeletedEvent event) {
    handle(event, jobDetail -> {
      removeJobListener(jobDetail.getKey());
      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
      quartzScheduler.notifySchedulerListenersJobDeleted(jobDetail.getKey());
    });
  }

  @Subscribe
  public void on(final JobUpdatedEvent event) {
    handle(event, jobDetail -> {
      updateJobListener(jobDetail);
      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
      quartzScheduler.notifySchedulerListenersJobAdded(jobDetail);
    });
  }

  private void handle(final JobEventSupport event, final Consumer<JobDetail> handler) {
    if (EventHelper.isReplicating()) {
      log.debug("Received {} for {} from node {}", event.getClass(), event.getJobKey(), event.getRemoteNodeId());

      try (TcclBlock tccl = TcclBlock.begin(this)) {
        JobDetail jobDetail = scheduler.getJobDetail(event.getJobKey());
        if (jobDetail == null) {
          log.debug("Missing job for {}", event.getJobKey());
          return;
        }
        synchronized (mutex) {
          handler.accept(jobDetail);
        }
      }
      catch (SchedulerException e) {
        log.warn("Error handling {} for {} cause {}", event.getClass(), event.getJobKey(), e.getMessage(),
            log.isDebugEnabled() ? e : null);
      }
    }
  }

  @Subscribe
  public void on(final TriggerCreatedEvent event) {
    handle(event, trigger -> {
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
    });
  }

  @Subscribe
  public void remoteTriggerUpdated(final TriggerUpdatedEvent event) throws SchedulerException {
    handle(event, trigger -> {
      if (!isRunNow(trigger)) {

        updateJobListener(trigger);

        // simulate signals Quartz would have sent
        quartzScheduler.getSchedulerSignaler().signalSchedulingChange(getNextFireMillis(trigger));
        quartzScheduler.notifySchedulerListenersUnscheduled(trigger.getKey());
        quartzScheduler.notifySchedulerListenersSchduled(trigger);
      }
    });
  }

  @Subscribe
  public void remoteTriggerDeleted(final TriggerDeletedEvent event) throws SchedulerException {
    handle(event, trigger -> {
      if (!isRunNow(trigger)) {

        // simulate signals Quartz would have sent
        quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
        quartzScheduler.notifySchedulerListenersUnscheduled(trigger.getKey());

        removeJobListener(trigger.getJobKey());
      }
    });
  }

  private void handle(final TriggerEventSupport event, final Consumer<Trigger> handler) {
    if (EventHelper.isReplicating()) {
      log.debug("Received {} for {} from node {}", event.getClass(), event.getTriggerKey(), event.getRemoteNodeId());

      try (TcclBlock tccl = TcclBlock.begin(this)) {
        Trigger trigger = scheduler.getTrigger(event.getTriggerKey());
        if (trigger == null) {
          log.debug("Missing trigger {}", event.getTriggerKey());
          return;
        }

        synchronized (mutex) {
          handler.accept(trigger);
        }
      }
      catch (SchedulerException e) {
        log.warn("Error handling {} for {} cause {}", event.getClass(), event.getTriggerKey(), e.getMessage(),
            log.isDebugEnabled() ? e : null);
      }
    }
  }

  @FunctionalInterface
  private interface Consumer<E>
  {
    void accept(E entity) throws SchedulerException;
  }
}
