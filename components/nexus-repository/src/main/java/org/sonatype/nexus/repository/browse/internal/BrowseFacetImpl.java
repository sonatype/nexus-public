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
package org.sonatype.nexus.repository.browse.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.browse.BrowseFacet;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID;

/**
 * @since 3.next
 */
@Named
public class BrowseFacetImpl
    extends FacetSupport
    implements BrowseFacet
{
  private final TaskScheduler taskScheduler;

  @Inject
  public BrowseFacetImpl(final TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
  }

  @Override
  public boolean isRebuilding() {
    String repositoryName = getRepository().getName();

    return taskScheduler.listsTasks().stream()
        .filter(task -> RebuildBrowseNodesTaskDescriptor.TYPE_ID.equals(task.getTypeId()))
        .filter(task -> TaskInfo.State.RUNNING.equals(task.getCurrentState().getState()))
        .anyMatch(task -> {
          String taskRepositoryName = task.getConfiguration().getString(REPOSITORY_NAME_FIELD_ID);
          return taskRepositoryName.equals(repositoryName) || taskRepositoryName.equals(RepositorySelector.ALL);
        });
  }
}
