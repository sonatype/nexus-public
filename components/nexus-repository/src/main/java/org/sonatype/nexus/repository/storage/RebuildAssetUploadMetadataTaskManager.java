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
package org.sonatype.nexus.repository.storage;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.scheduling.TaskInfo.State.RUNNING;

/**
 * @since 3.6
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class RebuildAssetUploadMetadataTaskManager
    extends StateGuardLifecycleSupport
{
  private final TaskScheduler taskScheduler;

  private final boolean enabled;

  @Inject
  public RebuildAssetUploadMetadataTaskManager(final TaskScheduler taskScheduler,
                                               final RebuildAssetUploadMetadataConfiguration configuration)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.enabled = checkNotNull(configuration).isEnabled();
  }

  @Override
  protected void doStart() {
    if (!enabled) {
      return;
    }

    Optional<TaskInfo> existingTask = findExistingTask();

    existingTask.ifPresent(taskInfo -> {
      if (!isRunning(taskInfo)) {
        try {
          existingTask.get().runNow();
        }
        catch (TaskRemovedException e) {
          log.warn("Unable to restart existing asset upload metadata task", e);
        }
      }
    });

    if (!existingTask.isPresent()) {
      launchNewTask();
    }
  }

  private Optional<TaskInfo> findExistingTask() {
    return taskScheduler.listsTasks().stream()
        .filter(task -> RebuildAssetUploadMetadataTaskDescriptor.TYPE_ID.equals(task.getConfiguration().getTypeId()))
        .findFirst();
  }

  private void launchNewTask() {
    TaskConfiguration configuration = taskScheduler
        .createTaskConfigurationInstance(RebuildAssetUploadMetadataTaskDescriptor.TYPE_ID);
    taskScheduler.submit(configuration);
  }

  private boolean isRunning(TaskInfo taskInfo) {
    return RUNNING.equals(taskInfo.getCurrentState().getState());
  }
}
