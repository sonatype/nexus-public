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
package org.sonatype.nexus.repository.npm.internal.tasks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.collect.ImmutableNestedAttributesMap;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.TRUE;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.repository.npm.internal.tasks.orient.OrientReindexNpmRepositoryTask.NPM_V1_SEARCH_UNSUPPORTED;
import static org.sonatype.nexus.repository.npm.internal.tasks.ReindexNpmRepositoryTaskDescriptor.REPOSITORY_NAME_FIELD_ID;

/**
 * Ad-hoc "manager" class that checks to see if any npm repositories are in need of reindexing, and in the event that
 * no other tasks are running to reindex the affected repositories, schedules tasks to do so. Intended as a mechanism
 * for upgrading "legacy" npm repositories that did not have their metadata appropriately extracted and indexed.
 *
 * @since 3.7
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class ReindexNpmRepositoryManager
    extends StateGuardLifecycleSupport
{
  private final TaskScheduler taskScheduler;

  private final RepositoryManager repositoryManager;

  private final boolean enabled;

  @Inject
  public ReindexNpmRepositoryManager(final TaskScheduler taskScheduler,
                                     final RepositoryManager repositoryManager,
                                     @Named("${nexus.npm.reindexOnStartup.enabled:-true}") final boolean enabled)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.enabled = enabled;
  }

  @Override
  protected void doStart() {
    if (!enabled) {
      return;
    }
    try {
      for (Repository repository : repositoryManager.browse()) {
        if (isUnprocessedNpmRepository(repository)) {
          boolean existingTask = taskScheduler.findAndSubmit(ReindexNpmRepositoryTaskDescriptor.TYPE_ID,
              ImmutableMap.of(REPOSITORY_NAME_FIELD_ID, repository.getName()));
          if (!existingTask) {
            runReindexTaskForRepository(repository);
          }
        }
      }
    }
    catch (Exception e) {
      log.error("Failed to determine if any npm repositories needed to be reindexed", e);
    }
  }

  /**
   * Schedules and immediately runs a task to reindex a particular npm repository.
   */
  private void runReindexTaskForRepository(final Repository repository) {
    TaskConfiguration configuration = taskScheduler
        .createTaskConfigurationInstance(ReindexNpmRepositoryTaskDescriptor.TYPE_ID);
    configuration.setString(REPOSITORY_NAME_FIELD_ID, repository.getName());
    configuration.setName("Reindex npm repository - (" + repository.getName() + ")");
    taskScheduler.submit(configuration);
  }

  /**
   * Returns whether or not the specified repository has not yet been processed to support npm v1 search.
   */
  private boolean isUnprocessedNpmRepository(final Repository repository) {
    AttributesFacet attributesFacet = repository.facet(AttributesFacet.class);
    ImmutableNestedAttributesMap attributes = attributesFacet.getAttributes();
    return TRUE.equals(attributes.get(NPM_V1_SEARCH_UNSUPPORTED));
  }
}
