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

import java.util.List;

import javax.inject.Inject;

import org.sonatype.nexus.index.tasks.descriptors.AbstractIndexTaskDescriptor;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesPathAwareTask;

/**
 * Base class for indexer related tasks.
 */
public abstract class AbstractIndexerTask
    extends AbstractNexusRepositoriesPathAwareTask<Object>
{
  /**
   * System event action: reindex
   */
  public static final String ACTION = "REINDEX";

  private List<ReindexTaskHandler> handlers;

  private String action;

  private boolean fullReindex;

  public AbstractIndexerTask(String action, boolean fullReindex) {
    super();
    this.action = action;
    this.fullReindex = fullReindex;
  }

  @Inject
  public void setReindexTaskHandlers(final List<ReindexTaskHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  protected String getRepositoryFieldId() {
    return AbstractIndexTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected String getRepositoryPathFieldId() {
    return AbstractIndexTaskDescriptor.RESOURCE_STORE_PATH_FIELD_ID;
  }

  @Override
  public Object doRun()
      throws Exception
  {
    for (ReindexTaskHandler handler : handlers) {
      try {
        if (getRepositoryId() != null) {
          handler.reindexRepository(getRepositoryId(), getResourceStorePath(), fullReindex);
        }
        else {
          handler.reindexAllRepositories(getResourceStorePath(), fullReindex);
        }
      }
      catch (NoSuchRepositoryException nsre) {
        // TODO: When we get to implement NEXUS-3977/NEXUS-1002 we'll be able to stop the indexing task when the
        // repo is deleted, so this exception handling/warning won't be needed anymore.
        if (getRepositoryId() != null) {
          getLogger().warn(
              "Repository with ID=\""
                  + getRepositoryId()
                  +
                  "\" was not found. It's likely that the repository was deleted while either the repair or the update index task was running.");
        }

        throw nsre;
      }
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
      return action + " repository index \"" + getRepositoryName() + "\" from path " + getResourceStorePath()
          + " and below.";
    }
    else {
      return action + " all registered repositories index";
    }
  }

}