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

import java.io.File;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.scheduling.SchedulerTask;

import org.junit.Test;

/**
 * Test if the repo folders(storage, indexer, proxy attributes) were deleted correctly
 *
 * @author juven
 */
public class DeleteRepositoryFoldersTaskTest
    extends AbstractMavenRepoContentTests
{
  @Test
  public void testTrashRepositoryFolders()
      throws Exception
  {
    fillInRepo();

    String repoId = snapshots.getId();

    DeleteRepositoryFoldersTask task = (DeleteRepositoryFoldersTask) lookup(SchedulerTask.class,
        DeleteRepositoryFoldersTask.class.getSimpleName());
    task.setRepository(snapshots);
    task.setDeleteForever(false);

    task.call();

    File workDir = nexusConfiguration().getWorkingDirectory();
    File trashDir = new File(workDir, "trash");

    assertFalse(new File(new File(workDir, "storage"), repoId).exists());
    assertFalse(new File(new File(workDir, "indexer"), repoId + "-local").exists());
    assertFalse(new File(new File(workDir, "indexer"), repoId + "-remote").exists());
    assertFalse(new File(new File(new File(workDir, "proxy"), "attributes"), repoId).exists());

    assertTrue(new File(trashDir, repoId).exists());
    assertFalse(new File(trashDir, repoId + "-local").exists());
    assertFalse(new File(trashDir, repoId + "-remote").exists());
  }

  @Test
  public void testDeleteForeverRepositoryFolders()
      throws Exception
  {
    fillInRepo();

    String repoId = snapshots.getId();

    DeleteRepositoryFoldersTask task = (DeleteRepositoryFoldersTask) lookup(SchedulerTask.class,
        DeleteRepositoryFoldersTask.class.getSimpleName());
    task.setRepository(snapshots);
    task.setDeleteForever(true);

    task.call();

    File workDir = nexusConfiguration().getWorkingDirectory();
    File trashDir = new File(workDir, "trash");

    assertFalse(new File(new File(workDir, "storage"), repoId).exists());
    assertFalse(new File(new File(workDir, "indexer"), repoId + "-local").exists());
    assertFalse(new File(new File(workDir, "indexer"), repoId + "-remote").exists());
    assertFalse(new File(new File(new File(workDir, "proxy"), "attributes"), repoId).exists());

    assertFalse(new File(trashDir, repoId).exists());
    assertFalse(new File(trashDir, repoId + "-local").exists());
    assertFalse(new File(trashDir, repoId + "-remote").exists());
  }
}
