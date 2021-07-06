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
package org.sonatype.nexus.repository.search.index;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.ALL_REPOSITORIES;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;

/**
 * This class handles the rebuilding of all Elasticsearch indexes on startup given a specified environment variable is
 * set to true.
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class IndexStartupRebuildManager
    extends LifecycleSupport
{
  public static final String REBUILD_ON_STARTUP_VARIABLE_NAME = "NEXUS_SEARCH_INDEX_REBUILD_ON_STARTUP";

  private final TaskScheduler taskScheduler;

  private final boolean rebuildOnStart;

  @Inject
  public IndexStartupRebuildManager(final TaskScheduler taskScheduler) {
    this(taskScheduler, System.getenv(REBUILD_ON_STARTUP_VARIABLE_NAME));
  }

  public IndexStartupRebuildManager(final TaskScheduler taskScheduler, @Nullable final String rebuildOnStartEnvVar) {
    this.taskScheduler = taskScheduler;
    this.rebuildOnStart = Boolean.parseBoolean(rebuildOnStartEnvVar);
  }

  @Override
  protected void doStart() throws Exception {
    if (rebuildOnStart) {
      log.info("Scheduling rebuild of repository indexes");
      doRebuildAllIndexes();
    }
  }

  /**
   * Schedule one-off background task to rebuild the indexes of all repositories.
   */
  private void doRebuildAllIndexes() {
    try {
      TaskConfiguration taskConfig = taskScheduler.createTaskConfigurationInstance(RebuildIndexTaskDescriptor.TYPE_ID);
      taskConfig.setString(REPOSITORY_NAME_FIELD_ID, ALL_REPOSITORIES);
      taskScheduler.submit(taskConfig);
    }
    catch (RuntimeException e) {
      log.warn("Problem scheduling rebuild of repository indexes", e);
    }
  }
}
