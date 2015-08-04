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
package org.sonatype.nexus.tasks;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.wastebasket.RepositoryFolderRemover;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Delete repository folders.
 */
@Named("DeleteRepositoryFoldersTask")
public class DeleteRepositoryFoldersTask
    extends AbstractNexusRepositoriesTask<Object>
{
  /**
   * System event action: remove repository folder
   */
  public static final String ACTION = "REMOVE_REPO_FOLDER";

  private final RepositoryFolderRemover repositoryFolderRemover;

  private boolean deleteForever = false;

  private Repository repository = null;

  @Inject
  public DeleteRepositoryFoldersTask(final RepositoryFolderRemover repositoryFolderRemover) {
    this.repositoryFolderRemover = checkNotNull(repositoryFolderRemover);
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
    setRepositoryId(repository.getId());
  }

  public boolean isDeleteForever() {
    return deleteForever;
  }

  public void setDeleteForever(boolean deleteForever) {
    this.deleteForever = deleteForever;
  }

  @Override
  public boolean isExposed() {
    return false;
  }

  @Override
  protected Object doRun()
      throws Exception
  {
    if (repository != null) {
      try {
        repositoryFolderRemover.deleteRepositoryFolders(repository, deleteForever);
      }
      catch (IOException e) {
        getLogger().warn("Unable to delete repository folders ", e);
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
      return "Deleting folders with repository ID: " + getRepositoryId();
    }
    return null;
  }

  @Override
  public String getRepositoryName() {
    return repository.getName();
  }

}
