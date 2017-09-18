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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.ComponentDatabase;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.common.annotations.VisibleForTesting;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;

/**
 * @since 3.6
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class RebuildBrowseNodesManager
    extends StateGuardLifecycleSupport
{
  @VisibleForTesting
  static final String ASSET_COUNT = "select count(*) from asset";

  @VisibleForTesting
  static final String BROWSE_NODE_COUNT = "select count(*) from browse_node";

  private static final String COUNT = "count";

  private final Provider<DatabaseInstance> componentDatabaseInstanceProvider;

  private final TaskScheduler taskScheduler;

  private final RepositoryManager repositoryManager;

  private final boolean automaticRebuildEnabled;

  @Inject
  public RebuildBrowseNodesManager(@Named(ComponentDatabase.NAME)
                                   final Provider<DatabaseInstance> componentDatabaseInstanceProvider,
                                   final TaskScheduler taskScheduler,
                                   final RepositoryManager repositoryManager,
                                   final BrowseNodeConfiguration configuration)
  {
    this.componentDatabaseInstanceProvider = checkNotNull(componentDatabaseInstanceProvider);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.repositoryManager = repositoryManager;
    this.automaticRebuildEnabled = checkNotNull(configuration).isAutomaticRebuildEnabled();
  }

  @Override
  protected void doStart() {
    if (!automaticRebuildEnabled) {
      return;
    }
    try {
      long assetCount = inTx(componentDatabaseInstanceProvider)
          .call(db -> execute(db, ASSET_COUNT).get(0).field(COUNT));
      long browseNodeCount = inTx(componentDatabaseInstanceProvider)
          .call(db -> execute(db, BROWSE_NODE_COUNT).get(0).field(COUNT));

      if (assetCount == 0 && browseNodeCount == 0) {
        log.debug("browse_node table won't be populated as there are no assets");
      }
      else if ((assetCount == 0 && browseNodeCount > 0) || (assetCount > 0 && browseNodeCount == 0)) {
        log.debug("browse_node table will be repopulated");
        for (Repository repository : repositoryManager.browse()) {
          if (!launchExistingTask(repository.getName())) {
            launchNewTask(repository.getName());
          }
        }
      }
      else {
        log.debug("browse_node table already populated");
      }
    }
    catch (Exception e) { // NOSONAR
      log.error("Failed to determine if the browse nodes need to be rebuilt for any repositories", e);
    }
  }

  private boolean launchExistingTask(final String repositoryName) throws TaskRemovedException {
    for (TaskInfo taskInfo : taskScheduler.listsTasks()) {
      if (isRebuildTask(repositoryName, taskInfo)) {
        if (!TaskInfo.State.RUNNING.equals(taskInfo.getCurrentState().getState())) {
          taskInfo.runNow();
        }
        return true;
      }
    }

    return false;
  }

  private void launchNewTask(final String repositoryName) {
    TaskConfiguration configuration = taskScheduler
        .createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    configuration.setString(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, repositoryName);
    configuration.setName("Rebuild repository browse tree - (" + repositoryName + ")");
    taskScheduler.submit(configuration);
  }

  private boolean isRebuildTask(final String repositoryName, final TaskInfo taskInfo) {
    return RebuildBrowseNodesTaskDescriptor.TYPE_ID.equals(taskInfo.getConfiguration().getTypeId()) && repositoryName
        .equals(taskInfo.getConfiguration().getString(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID));
  }

  private List<ODocument> execute(final ODatabaseDocumentTx db, final String query) {
    return db.command(new OCommandSQL(query)).execute();
  }
}
