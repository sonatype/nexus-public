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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.scheduling.ClusteredTaskStateStore;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.events.TaskDeletedEvent;
import org.sonatype.nexus.scheduling.events.TaskEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Updates {@link ClusteredTaskStateStore} with local task states.
 * 
 * @since 3.1
 */
@Named
@Singleton
@ManagedLifecycle(phase = Phase.TASKS)
public class LocalTaskStateTracker
    extends LifecycleSupport
    implements EventAware
{
  private final ClusteredTaskStateStore clusteredTaskStateStore;

  private final TaskScheduler taskScheduler;

  @Inject
  public LocalTaskStateTracker(final ClusteredTaskStateStore clusteredTaskStateStore,
                               final TaskScheduler taskScheduler)
  {
    this.clusteredTaskStateStore = checkNotNull(clusteredTaskStateStore);
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @Override
  protected void doStart() throws Exception {
    log.debug("Recording initial task states");
    taskScheduler.listsTasks().stream().forEach(clusteredTaskStateStore::setLocalState);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(TaskEvent event) {
    log.debug("Updating task state for event {}", event);
    if (event instanceof TaskDeletedEvent) {
      clusteredTaskStateStore.removeClusteredState(event.getTaskInfo().getId());
    }
    else {
      clusteredTaskStateStore.setLocalState(event.getTaskInfo());
    }
  }
}
