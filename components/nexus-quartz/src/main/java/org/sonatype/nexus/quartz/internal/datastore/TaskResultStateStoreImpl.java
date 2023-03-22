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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskUtils;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.spi.TaskResultState;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;
import org.sonatype.nexus.transaction.Transactional;

import org.quartz.JobKey;

@Named
@Singleton
public class TaskResultStateStoreImpl
    extends ConfigStoreSupport<QuartzDAO>
    implements TaskResultStateStore
{
  @Inject
  public TaskResultStateStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Override
  public List<TaskResultState> getStates() {
    return getQuartzStates().stream()
        // Group by a task (job name inside Quartz)
        .collect(Collectors.groupingBy(QuartzTaskStateData::getJobName))
        .values()
        .stream()
        .map(TaskResultStateStoreImpl::aggregate)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<TaskResultState> getState(final TaskInfo taskInfo) {
    return Optional.ofNullable(taskInfo)
        .map(QuartzTaskInfo.class::cast)
        .map(QuartzTaskInfo::getJobKey)
        .map(JobKey::getName)
        .flatMap(this::getQuartzState)
        .map(Collections::singletonList)
        .map(TaskResultStateStoreImpl::aggregate);
  }

  @Transactional
  protected List<QuartzTaskStateData> getQuartzStates() {
    return dao().getStates();
  }

  @Transactional
  protected Optional<QuartzTaskStateData> getQuartzState(final String taskId) {
    return dao().getState(taskId);
  }

  private static TaskResultState aggregate(final List<QuartzTaskStateData> jobStates) {
    TaskConfiguration taskConfiguration = jobStates.stream()
        .map(QuartzTaskStateData::getJobData)
        .filter(Objects::nonNull)
        .map(QuartzTaskUtils::configurationOf)
        .filter(Objects::nonNull)
        .findAny()
        .orElse(null);

    if (taskConfiguration == null) {
      return null;
    }

    boolean running = jobStates.stream()
        .map(QuartzTaskStateData::getState)
        .anyMatch("EXECUTING"::equals);

    return new TaskResultState(taskConfiguration.getId(), running ? TaskState.RUNNING : TaskState.WAITING,
        taskConfiguration.getLastRunState());
  }
}
