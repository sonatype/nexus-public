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
package org.sonatype.nexus.scheduling.internal;

import java.util.concurrent.Future;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Manages activation/passivation of the scheduler.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Priority(Integer.MIN_VALUE) // start scheduler at the end of this phase
@Singleton
public class TaskActivation
    extends StateGuardLifecycleSupport
    implements EventAware
{
  private final SchedulerSPI scheduler;

  private final DatabaseFreezeService databaseFreezeService;

  @Inject
  public TaskActivation(final SchedulerSPI scheduler, final DatabaseFreezeService databaseFreezeService) {
    this.scheduler = checkNotNull(scheduler);
    this.databaseFreezeService = checkNotNull(databaseFreezeService);
  }

  @Override
  protected void doStart() throws Exception {
    if (!databaseFreezeService.isFrozen()) {
      scheduler.resume();
    }
  }

  @Override
  protected void doStop() throws Exception {
    scheduler.pause();
  }

  /**
   * @since 3.2.1
   */
  @Subscribe
  public void onDatabaseFreezeChangeEvent(final DatabaseFreezeChangeEvent databaseFreezeChangeEvent) {
    if (databaseFreezeChangeEvent.isFrozen()) {
      scheduler.pause();
      scheduler.listsTasks().stream()
          .filter(taskInfo -> !maybeCancel(taskInfo))
          .forEach(taskInfo -> log.warn("Unable to cancel task: {}", taskInfo.getName()));
    }
    else {
      scheduler.resume();
    }
  }

  private boolean maybeCancel(final TaskInfo taskInfo) {
    Future<?> future = taskInfo.getCurrentState().getFuture();
    return future == null || future.cancel(false);
  }
}
