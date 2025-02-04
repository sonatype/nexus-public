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
package org.sonatype.nexus.scheduling.internal.upgrade.datastore;

import java.time.Duration;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;
import org.sonatype.nexus.common.upgrade.events.UpgradeFailedEvent;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.scheduling.events.TaskEventStopped;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedCanceled;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * An implementation of {@link UpgradeTaskScheduler} which attempts to run tasks scheduled through it in the order which
 * they were provided.
 *
 * When a task fails, or is canceled the queue will stop until a Nexus node restarts at which point it will resume the
 * executing the queue. Uses events to identify when a task changes state.
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class QueuingUpgradeTaskScheduler
    extends StateGuardLifecycleSupport
    implements EventAware, Asynchronous, UpgradeTaskScheduler
{
  private final boolean checkRequiresMigration;

  private final Cooperation2 cooperation;

  private final Duration delayOnStart;

  private final PeriodicJobService periodicJobService;

  private final TaskScheduler taskScheduler;

  private final UpgradeTaskStore upgradeTaskStore;

  @Inject
  public QueuingUpgradeTaskScheduler(
      final PeriodicJobService periodicJobService,
      final TaskScheduler taskScheduler,
      final UpgradeTaskStore upgradeTaskStore,
      @Named("${nexus.upgrade.tasks.checkOnStartup:-true}") final boolean checkRequiresMigration,
      @Named("${nexus.upgrade.tasks.delay:-10s}") final Duration delayOnStart,
      final Cooperation2Factory cooperationFactory)
  {
    this.periodicJobService = checkNotNull(periodicJobService);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.upgradeTaskStore = checkNotNull(upgradeTaskStore);
    this.checkRequiresMigration = checkRequiresMigration;
    this.delayOnStart = checkNotNull(delayOnStart);
    this.cooperation = checkNotNull(cooperationFactory)
        .configure()
        .build("reschedule-upgrade-task");
  }

  @Override
  public void schedule(final TaskConfiguration configuration) {
    upgradeTaskStore.insert(new UpgradeTaskData(configuration.getId(), configuration.asMap()));
  }

  /**
   * On startup reschedule
   * 
   * @throws Exception
   */
  @Override
  protected void doStart() {
    if (!checkRequiresMigration) {
      log.warn("Configured not to reschedule failed upgrade tasks. This may lead to missing features or bugs.");
      return;
    }

    periodicJobService.runOnce(this::maybeStartQueue, (int) delayOnStart.getSeconds());
  }

  /**
   * Listens for local upgrade events, this is used to start the queue after upgrades so tasks aren't invoked during
   * a rolling upgrade.
   */
  @Subscribe
  public void on(final UpgradeCompletedEvent event) {
    if (this.isStarted() && !EventHelper.isReplicating()) {
      log.debug("Starting queue due to event {}", event);
      maybeStartQueue();
    }
  }

  /**
   * Listens for local upgrade events, this is used to start the queue after upgrades so tasks aren't invoked during
   * a rolling upgrade.
   */
  @Subscribe
  public void on(final UpgradeFailedEvent event) {
    if (this.isStarted() && !EventHelper.isReplicating()) {
      log.debug("Starting queue due to event {}", event);
      maybeStartQueue();
    }
  }

  @Subscribe
  public void on(final TaskEventStopped event) {
    log.debug("on event: {}", event);

    TaskInfo taskInfo = event.getTaskInfo();

    if (event instanceof TaskEventStoppedFailed && upgradeTaskStore.markFailed(taskInfo.getId()) > 0) {
      log.error("Upgrade task failed: {}. Queue will be restarted on startup.", taskInfo.getName());
    }
    else if (event instanceof TaskEventStoppedCanceled && upgradeTaskStore.markCanceled(taskInfo.getId()) > 0) {
      log.error("Upgrade task cancelled: {}. Queue will be restarted on startup.", taskInfo.getName());
      // Nexus could be shutting down and the task scheduler is canceling running jobs
    }
    else if (event instanceof TaskEventStoppedDone && upgradeTaskStore.deleteByTaskId(taskInfo.getId()) > 0) {
      log.debug("Task {} completed.", taskInfo);
      maybeStartQueue();
    }
  }

  @Guarded(by = STARTED)
  protected void maybeStartQueue() {
    try {
      cooperation.on(() -> {
        Optional<UpgradeTaskData> next = upgradeTaskStore.next();
        if (!next.isPresent()) {
          return null;
        }
        if (notRunningAndNotDone(next.get())) {
          scheduleTask(next.get());
        }
        return null;
      })
          .checkFunction(Optional::empty)
          .cooperate("queue");
    }
    catch (Exception e) {
      log.error("An error occurred while starting the upgrade task queue.", e);
    }
  }

  /*
   * Attempts to schedule a failed upgrade task.
   *
   * If the task is already running we skip the task
   */
  private TaskInfo scheduleTask(final UpgradeTaskData task) {
    Optional<TaskInfo> taskInfo = upgradeTaskStore.read(task.getId())
        .flatMap(this::getExistingTask);

    String taskName = extractName(task);

    try {
      if (taskInfo.isPresent()) {
        if (notRunningAndNotDone(taskInfo)) {
          log.info("Re-running failed upgrade task {}", taskName);
          taskInfo.get().runNow();
        }
        log.debug("Task already running for {}", taskName);
        return taskInfo.get();
      }

      TaskConfiguration config = new TaskConfiguration();
      log.info("Running failed upgrade task {}", taskName);
      config.addAll(task.getConfiguration());
      TaskInfo result = taskScheduler.submit(config);
      task.setTaskId(result.getId());
      upgradeTaskStore.update(task);
      return result;
    }
    catch (Exception e) {
      log.error("Failed to restart upgrade task: {}", taskName, e);
      return null;
    }
  }

  private boolean notRunningAndNotDone(final UpgradeTaskData upgradetask) {
    log.trace("Checking state of taskId {}", upgradetask.getId());
    return notRunningAndNotDone(getExistingTask(upgradetask));
  }

  private boolean notRunningAndNotDone(final Optional<TaskInfo> upgradeTask) {
    return upgradeTask
        .map(taskScheduler::toExternalTaskState)
        .map(ExternalTaskState::getState)
        .map(state -> !(state.isRunning() || TaskState.OK.equals(state)))
        .orElse(true);
  }

  private Optional<TaskInfo> getExistingTask(final UpgradeTaskData upgradeTask) {
    return Optional.ofNullable(taskScheduler.getTaskById(upgradeTask.getTaskId()));
  }

  private static String extractName(final UpgradeTaskData task) {
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addAll(task.getConfiguration());
    return configuration.getName();
  }

  @Override
  public TaskConfiguration createTaskConfigurationInstance(final String typeId) {
    return taskScheduler.createTaskConfigurationInstance(typeId);
  }
}
