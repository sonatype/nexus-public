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
package org.sonatype.nexus.index.tasks;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.index.tasks.descriptors.DownloadIndexesTaskDescriptor;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Publish indexes task.
 */
@Named(DownloadIndexesTaskDescriptor.ID)
public class DownloadIndexesTask
    extends AbstractNexusRepositoriesTask<Object>
{

  /**
   * System event action: download indexes
   */
  public static final String ACTION = "DOWNLOADINDEX";

  private final IndexerManager indexerManager;

  @Inject
  public DownloadIndexesTask(final IndexerManager indexerManager) {
    this.indexerManager = checkNotNull(indexerManager);
  }

  @Override
  protected String getRepositoryFieldId() {
    return DownloadIndexesTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected Object doRun()
      throws Exception
  {
    if (getRepositoryId() != null) {
      indexerManager.downloadRepositoryIndex(getRepositoryId());
    }
    else {
      indexerManager.downloadAllIndex();
    }

    return null;
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() != null) {
      return "Downloading indexes for repository " + getRepositoryName();
    }
    else {
      return "Downloading indexes for all registered repositories";
    }
  }

}
