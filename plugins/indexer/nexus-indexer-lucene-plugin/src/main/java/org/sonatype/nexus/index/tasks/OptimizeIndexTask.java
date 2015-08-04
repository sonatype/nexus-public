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
import org.sonatype.nexus.index.tasks.descriptors.OptimizeIndexTaskDescriptor;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OptimizeIndex task.
 */
@Named(OptimizeIndexTaskDescriptor.ID)
public class OptimizeIndexTask
    extends AbstractNexusRepositoriesTask<Object>
{
  /**
   * System event action: optimize index
   */
  public static final String ACTION = "OPTIMIZE_INDEX";

  private final IndexerManager indexManager;

  @Inject
  public OptimizeIndexTask(final IndexerManager indexManager) {
    this.indexManager = checkNotNull(indexManager);
  }

  @Override
  protected String getRepositoryFieldId() {
    return OptimizeIndexTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  public Object doRun()
      throws Exception
  {
    if (getRepositoryId() != null) {
      indexManager.optimizeRepositoryIndex(getRepositoryId());
    }
    else {
      indexManager.optimizeAllRepositoriesIndex();
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
      return "Optimizing repository " + getRepositoryName() + " index.";
    }
    else {
      return "Optimizing all maven repositories indexes";
    }
  }

}
