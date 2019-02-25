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
package org.sonatype.nexus.internal.orient;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Cron;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.internal.orient.ClusterLogTaskDescriptor.TYPE_ID;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.CLUSTER_LOG_ONLY;

/**
 * Component to create cluster logging task using quartz to periodically execute
 *
 * @since 3.next
 */
@Named
@Singleton
@ManagedLifecycle(phase = Phase.TASKS)
public class ClusterLogTaskInitializer
    extends ComponentSupport
    implements Lifecycle
{
  private static final String DEFAULT_CRON_EXPRESSION = "0 */5 * ? * * *";

  private final NodeAccess nodeAccess;

  private final TaskScheduler taskScheduler;

  private final boolean enabled;

  @Inject
  public ClusterLogTaskInitializer(
      final NodeAccess nodeAccess,
      final TaskScheduler taskScheduler,
      @Named("${nexus.log.cluster.enabled:-true}") final boolean enabled)
  {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.enabled = enabled;
  }

  @Override
  public void start() {
    log.info("Starting table count logging");
    List<TaskInfo> loggingTasks = taskScheduler.listsTasks().stream()
        .filter((ti) -> TYPE_ID.equals(ti.getTypeId()))
        .collect(Collectors.toList());
    if (enabled && nodeAccess.isClustered() && loggingTasks.isEmpty()) {
      log.debug("Table count logging enabled");
      TaskConfiguration configuration = taskScheduler.createTaskConfigurationInstance(TYPE_ID);
      Cron cron = taskScheduler.getScheduleFactory().cron(new Date(), DEFAULT_CRON_EXPRESSION);
      TaskInfo taskInfo = taskScheduler.scheduleTask(configuration, cron);
      log.debug("New task {} created", taskInfo.getId());
    }
    else if (!enabled) {
      log.debug("Table count logging disabled");
      removeTasks(loggingTasks);
    }
  }

  private void removeTasks(final List<TaskInfo> loggingTasks) {
    loggingTasks.forEach(taskInfo -> {
      log.debug(CLUSTER_LOG_ONLY, "Removing task {}", taskInfo.getId());
      taskInfo.remove();
    });
  }

  @Override
  public void stop() {
    //no-op
  }
}
