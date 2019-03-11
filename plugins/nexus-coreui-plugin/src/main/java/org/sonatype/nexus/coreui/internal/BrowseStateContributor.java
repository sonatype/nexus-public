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
package org.sonatype.nexus.coreui.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.ALL_REPOSITORIES;
import static org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID;

@Singleton
@Named
public class BrowseStateContributor
    implements StateContributor
{
  private final BrowseNodeConfiguration browseNodeConfiguration;

  private final TaskScheduler taskScheduler;

  @Inject
  public BrowseStateContributor(final BrowseNodeConfiguration browseNodeConfiguration,
                                final TaskScheduler taskScheduler)
  {
    this.browseNodeConfiguration = checkNotNull(browseNodeConfiguration);
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @Override
  public Map<String, Object> getState() {
    Map<String, Object> state = new HashMap<>();

    state.put("rebuildingRepositories", getRepositoryNamesForRunningTasks());
    state.put("browseTreeMaxNodes", browseNodeConfiguration.getMaxNodes());

    return state;
  }

  private Set<String> getRepositoryNamesForRunningTasks() {
    Set<String> repositoryNames = new HashSet<>();
    for (TaskInfo taskInfo : taskScheduler.listsTasks()) {
      if (RebuildBrowseNodesTaskDescriptor.TYPE_ID.equals(taskInfo.getTypeId()) && TaskInfo.RunState.RUNNING
          .equals(taskInfo.getCurrentState().getRunState())) {
        String repositoryName = taskInfo.getConfiguration().getString(REPOSITORY_NAME_FIELD_ID);
        if (ALL_REPOSITORIES.equals(repositoryName)) {
          //if all repos, just return a single entry denoting that, save the time to check other tasks
          return Collections.singleton(ALL_REPOSITORIES);
        }
        else {
          repositoryNames.add(repositoryName);
        }
      }
    }
    return repositoryNames;
  }
}
