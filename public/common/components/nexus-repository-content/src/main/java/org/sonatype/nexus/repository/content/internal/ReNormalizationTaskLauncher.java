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

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.lifecycle.LifecycleSupport;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask;
import org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
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
 */
@Component
@ManagedLifecycle(phase = TASKS)
public class ReNormalizationTaskLauncher
    extends LifecycleSupport
{
  private final GlobalKeyValueStore globalKeyValueStore;

  private final TaskScheduler taskScheduler;

  @Autowired
  public ReNormalizationTaskLauncher(final GlobalKeyValueStore globalKeyValueStore, final TaskScheduler taskScheduler) {
    this.globalKeyValueStore = checkNotNull(globalKeyValueStore);
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @Override
  protected void doStart() {
    try {
      if (!resumeIsNeeded()) {
        return;
      }

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
      log.error("Failed to submit re-normalization repair task, will retry on next startup", e);
    }
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
