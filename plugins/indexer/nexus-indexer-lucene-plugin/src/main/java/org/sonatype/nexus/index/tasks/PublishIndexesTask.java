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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.index.tasks.descriptors.PublishIndexesTaskDescriptor;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Publish indexes task.
 */
@Named(PublishIndexesTaskDescriptor.ID)
public class PublishIndexesTask
    extends AbstractNexusRepositoriesTask<Object>
{
  /**
   * System event action: publish indexes
   */
  public static final String ACTION = "PUBLISHINDEX";

  private final IndexerManager indexerManager;

  @Inject
  public PublishIndexesTask(final IndexerManager indexerManager) {
    this.indexerManager = checkNotNull(indexerManager);
  }

  @Override
  protected String getRepositoryFieldId() {
    return PublishIndexesTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected Object doRun()
      throws Exception
  {
    try {
      if (getRepositoryId() != null) {
        indexerManager.publishRepositoryIndex(getRepositoryId());
      }
      else {
        indexerManager.publishAllIndex();
      }
    }
    catch (IOException e) {
      getLogger().error("Cannot publish indexes!", e);
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
      return "Publishing indexes for repository " + getRepositoryName();
    }
    else {
      return "Publishing indexes for all registered repositories";
    }
  }

}
