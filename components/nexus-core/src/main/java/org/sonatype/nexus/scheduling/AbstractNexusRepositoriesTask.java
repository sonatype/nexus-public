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
package org.sonatype.nexus.scheduling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.scheduling.DefaultScheduledTask;
import org.sonatype.scheduling.ScheduledTask;

import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractNexusRepositoriesTask<T>
    extends AbstractNexusTask<T>
{

  private RepositoryRegistry repositoryRegistry;

  @Inject
  public void setRepositoryRegistry(final RepositoryRegistry repositoryRegistry) {
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  protected RepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  // This is simply a default to help for old api tasks
  // This method SHOULD be overridden in new task impls
  protected String getRepositoryFieldId() {
    return "repositoryId";
  }

  public String getRepositoryId() {
    final String id = getParameters().get(getRepositoryFieldId());
    if ("all_repo".equals(id)) {
      return null;
    }
    return id;
  }

  public void setRepositoryId(String repositoryId) {
    if (!StringUtils.isEmpty(repositoryId)) {
      getParameters().put(getRepositoryFieldId(), repositoryId);
    }
  }

  public String getRepositoryName() {
    try {
      Repository repo = getRepositoryRegistry().getRepository(getRepositoryId());

      return repo.getName();
    }
    catch (NoSuchRepositoryException e) {
      this.getLogger().warn("Could not read repository!", e);

      return getRepositoryId();
    }
  }

  @Override
  public boolean allowConcurrentExecution(Map<String, List<ScheduledTask<?>>> activeTasks) {
    return !hasIntersectingTasksThatRuns(activeTasks);
  }

  protected boolean hasIntersectingTasksThatRuns(Map<String, List<ScheduledTask<?>>> activeTasks) {
    // get all activeTasks that runs and are descendants of AbstractNexusRepositoriesTask
    for (List<ScheduledTask<?>> scheduledTasks : activeTasks.values()) {
      for (ScheduledTask<?> task : scheduledTasks) {
        if (AbstractNexusRepositoriesTask.class.isAssignableFrom(task.getTask().getClass())) {
          // check against RUNNING or CANCELLING intersection
          if (task.getTaskState().isExecuting()
              && DefaultScheduledTask.class.isAssignableFrom(task.getClass())
              && repositorySetIntersectionIsNotEmpty(
              task.getTaskParams().get(getRepositoryFieldId()))) {
            getLogger().debug(
                "Task {} is already running and is conflicting with task {}",
                task.getName(), this.getClass().getSimpleName()
            );
            return true;
          }
        }
      }
    }

    return false;
  }

  protected boolean repositorySetIntersectionIsNotEmpty(String repositoryId) {
    // simplest cases, checking for repoId and groupId equality
    if (StringUtils.equals(getRepositoryId(), repositoryId)) {
      return true;
    }

    // All repo check
    if (getRepositoryId() == null || repositoryId == null) {
      return true;
    }

    try {
      // complex case: repoA may be in both groupA and groupB as member
      // so we actually evaluate all tackled reposes for both task and have intersected those
      final List<Repository> thisReposes = new ArrayList<Repository>();
      {
        final Repository repo = getRepositoryRegistry().getRepository(getRepositoryId());

        if (repo.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
          thisReposes.addAll(repo.adaptToFacet(GroupRepository.class).getTransitiveMemberRepositories());
        }
        else {
          thisReposes.add(repo);
        }
      }

      final List<Repository> reposes = new ArrayList<Repository>();
      {
        final Repository repo = getRepositoryRegistry().getRepository(repositoryId);

        if (repo.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
          reposes.addAll(repo.adaptToFacet(GroupRepository.class).getTransitiveMemberRepositories());
        }
        else {
          reposes.add(repo);
        }
      }

      HashSet<Repository> testSet = new HashSet<Repository>();
      testSet.addAll(thisReposes);
      testSet.addAll(reposes);

      // the set does not intersects
      return thisReposes.size() + reposes.size() != testSet.size();
    }
    catch (NoSuchResourceStoreException e) {
      if (getLogger().isDebugEnabled()) {
        getLogger().error(e.getMessage(), e);
      }

      // in this case, one of the tasks will die anyway, let's say false
      return false;
    }
  }

}
