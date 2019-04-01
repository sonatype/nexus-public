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
package org.sonatype.nexus.repository.search;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Now;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.ALL_REPOSITORIES;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.scheduling.TaskDescriptorSupport.LIMIT_NODE_KEY;
import static org.sonatype.nexus.scheduling.TaskDescriptorSupport.MULTINODE_KEY;

/**
 * Manages automatic rebuilding of repository indexes.
 *
 * @since 3.next
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class IndexRebuildManager
    extends LifecycleSupport
{
  private final NodeAccess nodeAccess;

  private final TaskScheduler taskScheduler;

  private final boolean autoRebuild;

  private volatile boolean rebuildAllIndexes = false;

  @Inject
  public IndexRebuildManager(final NodeAccess nodeAccess,
                             final TaskScheduler taskScheduler,
                             @Named("${nexus.elasticsearch.autoRebuild:-true}") final boolean autoRebuild)
  {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.autoRebuild = autoRebuild;
  }

  public void rebuildAllIndexes() {
    rebuildAllIndexes = true;
  }

  @Override
  protected void doStart() throws Exception {
    if (autoRebuild && rebuildAllIndexes) {
      rebuildAllIndexes = false;
      if (!hasAutoRebuildTask()) {
        log.info("Scheduling automatic rebuild of repository indexes");
        doRebuildAllIndexes();
      }
      else {
        log.info("Automatic rebuild of repository indexes is already scheduled");
      }
    }
  }

  /**
   * Schedule one-off background task to rebuild the indexes of all repositories on this node.
   */
  private void doRebuildAllIndexes() {
    try {
      TaskConfiguration taskConfig = taskScheduler.createTaskConfigurationInstance(RebuildIndexTaskDescriptor.TYPE_ID);
      taskConfig.setString(REPOSITORY_NAME_FIELD_ID, ALL_REPOSITORIES);
      taskConfig.setString(LIMIT_NODE_KEY, nodeAccess.getId());
      taskConfig.setBoolean(MULTINODE_KEY, false);
      taskScheduler.submit(taskConfig);
    }
    catch (RuntimeException e) {
      log.warn("Problem scheduling automatic rebuild of repository indexes", e);
    }
  }

  /**
   * Does an auto-rebuild task already exist for this node?
   */
  private boolean hasAutoRebuildTask() {
    return taskScheduler.listsTasks().stream()
        .filter(task -> task.getSchedule() instanceof Now)
        .map(TaskInfo::getConfiguration)
        .filter(config -> RebuildIndexTaskDescriptor.TYPE_ID.equals(config.getTypeId()))
        .filter(config -> ALL_REPOSITORIES.equals(config.getString(REPOSITORY_NAME_FIELD_ID)))
        .anyMatch(config -> nodeAccess.getId().equals(config.getString(LIMIT_NODE_KEY)));
  }
}
