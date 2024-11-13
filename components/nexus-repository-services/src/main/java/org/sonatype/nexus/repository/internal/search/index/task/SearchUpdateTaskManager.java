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
package org.sonatype.nexus.repository.internal.search.index.task;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.index.SearchUpdateService;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.repository.internal.search.index.task.SearchUpdateTaskDescriptor.REPOSITORY_NAMES_FIELD_ID;

/**
 * Ad-hoc "manager" class that checks to see if any repository indexes are out of date, missed or need to be updated and
 * schedules a task to do so.
 *
 * @since 3.37
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class SearchUpdateTaskManager
    extends StateGuardLifecycleSupport
{
  private final TaskScheduler taskScheduler;

  private final RepositoryManager repositoryManager;

  private final boolean enabled;

  private final SearchUpdateService searchUpdateService;

  @Inject
  public SearchUpdateTaskManager(
      final TaskScheduler taskScheduler,
      final RepositoryManager repositoryManager,
      final SearchUpdateService searchUpdateService,
      @Named("${nexus.search.updateIndexesOnStartup.enabled:-true}") final boolean enabled)
  {
    this.taskScheduler = requireNonNull(taskScheduler);
    this.repositoryManager = requireNonNull(repositoryManager);
    this.searchUpdateService = requireNonNull(searchUpdateService);
    this.enabled = enabled;
  }

  @Override
  protected void doStart() {
    if (!enabled) {
      return;
    }
    try {
      List<String> reindexList = StreamSupport.stream(repositoryManager.browse().spliterator(), false)
          .filter(searchUpdateService::needsReindex)
          .map(Repository::getName)
          .collect(Collectors.toList());

      if (!reindexList.isEmpty()) {
        boolean existingTask = taskScheduler.findAndSubmit(SearchUpdateTaskDescriptor.TYPE_ID);
        if (!existingTask) {
          runSearchUpdateTaskForRepositories(reindexList);
        }
      }
    }
    catch (Exception e) {
      log.error("Failed to determine if any repository indexes needed to be updated", e);
    }
  }

  private void runSearchUpdateTaskForRepositories(final List<String> repositories) {
    String repositoriesCsv = String.join(",", repositories);
    TaskConfiguration configuration = taskScheduler
        .createTaskConfigurationInstance(SearchUpdateTaskDescriptor.TYPE_ID);
    configuration.setString(REPOSITORY_NAMES_FIELD_ID, repositoriesCsv);
    configuration.setName("Update repository indexes - (" + repositoriesCsv + ")");
    taskScheduler.submit(configuration);
  }
}
