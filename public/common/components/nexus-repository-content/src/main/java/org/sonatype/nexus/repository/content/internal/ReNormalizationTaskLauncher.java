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
package org.sonatype.nexus.repository.content.internal;

import java.time.Duration;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.KeyValueEvent;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.node.datastore.NodeHeartbeatManager;
import org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask;
import org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask.CURSOR_KEY;
import static org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask.GATE_KEY_FORMAT;
import static org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask.RENORMALIZE_FORMATS;

/**
 * Submits {@link ReNormalizeComponentVersionTask} directly to the live {@link TaskScheduler}, on every startup,
 * whenever a repair is outstanding - either freshly raised by {@code ScheduleReNormalizationMigrationStep} or left
 * behind by a run that was interrupted before completing (the run cursor is still present, or any target format's
 * gate is still raised).
 * <p>
 * Deliberately does not go through {@link org.sonatype.nexus.scheduling.UpgradeTaskScheduler}: that scheduler
 * runs every queued upgrade task strictly in the order it was queued, and a single stuck/slow unrelated upgrade
 * task ahead of this one in that shared queue would block this repair forever. This task doesn't need that
 * ordering - it's idempotent and safe to run independently of any other upgrade task (see
 * {@link ReNormalizeComponentVersionTask#jobStream}), so it's submitted straight to the scheduler instead.
 * <p>
 * Runs at {@link ManagedLifecycle.Phase#TASKS}, the same phase the upgrade task queue itself starts in, since
 * {@link TaskScheduler#submit} requires the underlying scheduler to already be started - calling it any earlier
 * (e.g. at {@code UPGRADE}, where the gate-raising migration and old resume checker used to run) throws.
 * <p>
 * During a self-hosted ZDU rolling restart, submission is skipped while any node in the cluster is still on a
 * different version/build (see {@link NodeHeartbeatManager#hasVersionSkew()}) - an old node could otherwise grab
 * the execution before it's ever taken out of rotation and fail on a task type it doesn't know about. The
 * {@link NodeHeartbeatManager} is {@code @Nullable} because its sole implementation lives in a Pro-only plugin
 * and is absent from OSS/single-node Spring contexts, which never see version skew in the first place.
 * <p>
 * The boot-time check alone is not enough to be self-healing: a node can reach {@link ManagedLifecycle.Phase#TASKS}
 * before the gate is raised (e.g. {@code ScheduleReNormalizationMigrationStep} on another node in the cluster
 * hasn't run yet, or this node was mid-rolling-restart and deferred above) and would otherwise never notice the
 * gate flip afterwards. Two mechanisms close that gap without a wait loop:
 * <ul>
 * <li>a {@link KeyValueEvent} subscription reacts immediately whenever the cursor or a target format's gate
 * changes via {@link GlobalKeyValueStore} - the same pattern {@code ProCleanupFeatureCheck} already uses for the
 * same keys;</li>
 * <li>a periodic recheck (mirroring {@code ProCleanupFeatureCheck}'s own fallback) covers the case the event
 * can't: {@code ScheduleReNormalizationMigrationStep} raises the gate through {@code UpgradeNexusKeyValueStore}
 * at {@code UPGRADE} phase, before {@code EventManager} exists, so that particular write can never post a
 * {@link KeyValueEvent} in the first place.</li>
 * </ul>
 */
@Component
@ManagedLifecycle(phase = TASKS)
public class ReNormalizationTaskLauncher
    extends LifecycleSupport
    implements EventAware, EventAware.Asynchronous
{
  private static final Duration RECHECK_INTERVAL = Duration.ofMinutes(5);

  // 12 recheck cycles at the default 5-minute RECHECK_INTERVAL: escalate once an hour so a stuck rolling
  // upgrade is observable, without paging on every single recheck while the upgrade is still in progress
  private static final int DEFERRAL_WARN_EVERY_N_RECHECKS = 12;

  private final GlobalKeyValueStore globalKeyValueStore;

  private final TaskScheduler taskScheduler;

  @Nullable
  private final NodeHeartbeatManager heartbeatManager;

  private final PeriodicJobService jobService;

  private PeriodicJob job;

  private int consecutiveVersionSkewDeferrals;

  @Autowired
  public ReNormalizationTaskLauncher(
      final GlobalKeyValueStore globalKeyValueStore,
      final TaskScheduler taskScheduler,
      @Nullable final NodeHeartbeatManager heartbeatManager,
      final PeriodicJobService jobService)
  {
    this.globalKeyValueStore = requireNonNull(globalKeyValueStore);
    this.taskScheduler = requireNonNull(taskScheduler);
    this.heartbeatManager = heartbeatManager;
    this.jobService = requireNonNull(jobService);
  }

  @Override
  protected void doStart() {
    attemptSubmission();
    jobService.startUsing();
    try {
      job = jobService.schedule(this::attemptSubmission, RECHECK_INTERVAL);
    }
    catch (RuntimeException e) {
      jobService.stopUsing();
      throw e;
    }
  }

  @Override
  protected void doStop() {
    if (job != null) {
      job.cancel();
    }
    jobService.stopUsing();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final KeyValueEvent event) {
    if (isRelevantKey(event.getKey())) {
      attemptSubmission();
    }
  }

  private synchronized void attemptSubmission() {
    try {
      if (!resumeIsNeeded()) {
        return;
      }

      if (heartbeatManager != null && heartbeatManager.hasVersionSkew()) {
        consecutiveVersionSkewDeferrals++;
        if (consecutiveVersionSkewDeferrals == 1
            || consecutiveVersionSkewDeferrals % DEFERRAL_WARN_EVERY_N_RECHECKS == 0) {
          log.warn("re-normalization repair outstanding, but a rolling restart is in progress "
              + "(nodes report mixed versions) for {} consecutive check(s); deferring submission until it "
              + "completes - if this persists, check cluster node states", consecutiveVersionSkewDeferrals);
        }
        else {
          log.info("re-normalization repair outstanding, but a rolling restart is in progress "
              + "(nodes report mixed versions); deferring submission until it completes");
        }
        return;
      }
      consecutiveVersionSkewDeferrals = 0;

      // in active-active HA every node reaches this phase independently and would otherwise submit its own
      // instance of the same repair; the live scheduler is already STARTED here (unlike the old Phase.UPGRADE
      // checker this replaced), so querying it is safe
      if (taskScheduler.getTaskByTypeId(ReNormalizeComponentVersionTaskDescriptor.TYPE_ID) != null) {
        log.debug("re-normalization task is already scheduled or running, skipping submission");
        return;
      }

      log.info("re-normalization repair outstanding, submitting {}",
          ReNormalizeComponentVersionTaskDescriptor.TYPE_ID);
      taskScheduler.submit(taskScheduler.createTaskConfigurationInstance(
          ReNormalizeComponentVersionTaskDescriptor.TYPE_ID));
    }
    catch (Exception e) {
      log.warn("Failed to submit re-normalization repair task, will retry on next recheck", e);
    }
  }

  private boolean isRelevantKey(final String key) {
    return CURSOR_KEY.equals(key)
        || RENORMALIZE_FORMATS.stream().anyMatch(format -> GATE_KEY_FORMAT.formatted(format).equals(key));
  }

  private boolean resumeIsNeeded() {
    return cursorPresent() || RENORMALIZE_FORMATS.stream().anyMatch(this::gateRaised);
  }

  private boolean cursorPresent() {
    return globalKeyValueStore.getKey(CURSOR_KEY).isPresent();
  }

  private boolean gateRaised(final String format) {
    return globalKeyValueStore.getKey(GATE_KEY_FORMAT.formatted(format))
        .map(NexusKeyValue::getAsBoolean)
        .orElse(false);
  }
}
