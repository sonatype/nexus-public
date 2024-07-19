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
package org.sonatype.nexus.scheduling.internal.upgrade.orient;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class UpgradeTaskSchedulerImpl
    extends StateGuardLifecycleSupport
    implements UpgradeTaskScheduler
{
  private final List<TaskConfiguration> configurations = new ArrayList<>();

  private final TaskScheduler scheduler;

  @Inject
  public UpgradeTaskSchedulerImpl(final TaskScheduler scheduler) {
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  public void schedule(final TaskConfiguration configuration) {
    if (this.isStarted()) {
      scheduleTask(configuration);
    }
    else {
      configurations.add(configuration);
    }
  }

  @Override
  public TaskConfiguration createTaskConfigurationInstance(final String typeId) {
    return scheduler.createTaskConfigurationInstance(typeId);
  }

  @Override
  protected void doStart() throws Exception {
    configurations.forEach(this::scheduleTask);
    configurations.clear();
  }

  private void scheduleTask(final TaskConfiguration configuration) {
    try {
      TaskInfo taskInfo = scheduler.submit(configuration);
      log.debug("Scheduled {} for {}", taskInfo, configuration);
    }
    catch (Exception e) {
      log.warn("An error occurred scheduling a required task", e);
    }
  }
}
