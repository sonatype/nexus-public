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
package org.sonatype.nexus.repository.content.browse;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodesTaskDescriptor;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Triggers a rebuild task for all repositories when requested. This is largely for upgrades or other early
 * phase code before services have started.
 *
 * @since 3.33
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class RebuildBrowseNodesManager
    extends StateGuardLifecycleSupport
{
  private static final String ALL_REPOSITORIES = "*";

  private final TaskScheduler taskScheduler;

  private final RepositoryManager repositoryManager;

  private boolean rebuildOnStart = false;

  @Inject
  public RebuildBrowseNodesManager(final TaskScheduler taskScheduler, final RepositoryManager repositoryManager)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  public void setRebuildOnSart(final boolean rebuildOnStart) {
    this.rebuildOnStart = rebuildOnStart;
  }

  @Override
  protected void doStart() { // NOSONAR
    if (!rebuildOnStart) {
      return;
    }

    Stopwatch sw = Stopwatch.createStarted();
    try {
      String repositoryNames = StreamSupport.stream(repositoryManager.browse().spliterator(), false)
          .filter(this::hasAssets)
          .map(Repository::getName)
          .collect(Collectors.joining(","));

      if (!Strings2.isEmpty(repositoryNames)) {
        boolean existingTask = taskScheduler.findWaitingTask(RebuildBrowseNodesTaskDescriptor.TYPE_ID,
            ImmutableMap.of(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, ALL_REPOSITORIES));
        if (!existingTask) {
          launchNewTask(repositoryNames);
        }
      }
    }
    catch (Exception e) {
      log.error("Failed to determine if the browse nodes need to be rebuilt for any repositories", e);
    }
    log.debug("scheduling rebuild browse nodes tasks took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
  }

  private boolean hasAssets(final Repository repository) {
    return repository.facet(ContentFacet.class).assets().count() > 0;
  }

  private void launchNewTask(final String repositoryNames) {
    TaskConfiguration configuration = taskScheduler
        .createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    configuration.setString(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, repositoryNames);
    configuration.setName("Rebuild repository browse tree - (" + repositoryNames + ")");
    taskScheduler.submit(configuration);
  }
}
