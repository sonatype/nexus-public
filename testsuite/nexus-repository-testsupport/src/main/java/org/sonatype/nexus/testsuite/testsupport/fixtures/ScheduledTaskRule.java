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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;

import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import org.junit.rules.ExternalResource;

public class ScheduledTaskRule
    extends ExternalResource
{
  private Provider<TaskScheduler> taskSchedulerProvider;

  private List<TaskInfo> tasks = new ArrayList<>();

  public ScheduledTaskRule(final Provider<TaskScheduler> taskSchedulerProvider) {
    this.taskSchedulerProvider = taskSchedulerProvider;
  }

  public TaskInfo create(final String name, final String typeId, final Map<String, String> attributes) {
    return create(name, typeId, attributes, false);
  }

  public TaskInfo create(final String name, final String typeId, final Map<String, String> attributes, boolean runNow) {
    TaskScheduler taskScheduler = taskSchedulerProvider.get();

    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(typeId);
    attributes.forEach(taskConfiguration::setString);
    taskConfiguration.setName(name);
    taskConfiguration.setEnabled(true);

    Schedule schedule = runNow ? taskScheduler.getScheduleFactory().now() : taskScheduler.getScheduleFactory().manual();
    TaskInfo taskInfo = taskScheduler.scheduleTask(taskConfiguration, schedule);
    tasks.add(taskInfo);
    return taskInfo;
  }

  public void removeAll() {
    taskSchedulerProvider.get().listsTasks().forEach(TaskInfo::remove);
    tasks.clear();
  }

  @Override
  protected void after() {
    tasks.forEach(TaskInfo::remove);
    tasks.clear();
  }
}
