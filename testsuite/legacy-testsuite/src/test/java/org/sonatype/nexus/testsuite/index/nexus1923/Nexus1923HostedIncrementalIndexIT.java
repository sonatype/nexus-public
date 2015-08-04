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
package org.sonatype.nexus.testsuite.index.nexus1923;

import java.io.File;

import org.sonatype.nexus.integrationtests.ITGroups.INDEX;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class Nexus1923HostedIncrementalIndexIT
    extends AbstractNexus1923
{
  public Nexus1923HostedIncrementalIndexIT()
      throws Exception
  {
    super();
  }

  @Test
  @Category(INDEX.class)
  public void validateIncrementalIndexesCreated()
      throws Exception
  {
    File repoStorageDirectory = getHostedRepositoryStorageDirectory();

    //First create our repository
    createHostedRepository();

    TaskScheduleUtil.waitForAllTasksToStop();

    //Create the reindex task
    String reindexId = createHostedReindexTask();

    //Put an artifact in the storage
    FileUtils.copyDirectory(getTestFile(FIRST_ARTIFACT),
        repoStorageDirectory);

    //Now reindex the repo
    reindexHostedRepository(reindexId);

    //Now make sure there is an index file, and no incremental files
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("2").exists());
    validateCurrentHostedIncrementalCounter(1);

    //Now make sure that the search is properly working
    searchForArtifactInHostedIndex(FIRST_ARTIFACT, true);
    searchForArtifactInHostedIndex(SECOND_ARTIFACT, false);
    searchForArtifactInHostedIndex(THIRD_ARTIFACT, false);
    searchForArtifactInHostedIndex(FOURTH_ARTIFACT, false);
    searchForArtifactInHostedIndex(FIFTH_ARTIFACT, false);

    //Put an artifact in the storage
    FileUtils.copyDirectory(getTestFile(SECOND_ARTIFACT),
        repoStorageDirectory);

    //Now reindex the repo
    reindexHostedRepository(reindexId);

    //Now make sure there is an index file, and 1 incremental file
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("3").exists());
    validateCurrentHostedIncrementalCounter(2);

    //Now make sure that the search is properly working
    searchForArtifactInHostedIndex(FIRST_ARTIFACT, true);
    searchForArtifactInHostedIndex(SECOND_ARTIFACT, true);
    searchForArtifactInHostedIndex(THIRD_ARTIFACT, false);
    searchForArtifactInHostedIndex(FOURTH_ARTIFACT, false);
    searchForArtifactInHostedIndex(FIFTH_ARTIFACT, false);

    //Put an artifact in the storage
    FileUtils.copyDirectory(getTestFile(THIRD_ARTIFACT),
        repoStorageDirectory);

    //Now reindex the repo
    reindexHostedRepository(reindexId);

    //Now make sure there is an index file, and 2 incremental file
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("3").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("4").exists());
    validateCurrentHostedIncrementalCounter(3);

    //Now make sure that the search is properly working
    searchForArtifactInHostedIndex(FIRST_ARTIFACT, true);
    searchForArtifactInHostedIndex(SECOND_ARTIFACT, true);
    searchForArtifactInHostedIndex(THIRD_ARTIFACT, true);
    searchForArtifactInHostedIndex(FOURTH_ARTIFACT, false);
    searchForArtifactInHostedIndex(FIFTH_ARTIFACT, false);

    //Put an artifact in the storage
    FileUtils.copyDirectory(getTestFile(FOURTH_ARTIFACT),
        repoStorageDirectory);

    //Now reindex the repo
    reindexHostedRepository(reindexId);

    //Now make sure there is an index file, and 3 incremental file
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("3").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("4").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("5").exists());
    validateCurrentHostedIncrementalCounter(4);

    //Now make sure that the search is properly working
    searchForArtifactInHostedIndex(FIRST_ARTIFACT, true);
    searchForArtifactInHostedIndex(SECOND_ARTIFACT, true);
    searchForArtifactInHostedIndex(THIRD_ARTIFACT, true);
    searchForArtifactInHostedIndex(FOURTH_ARTIFACT, true);
    searchForArtifactInHostedIndex(FIFTH_ARTIFACT, false);

    //Put an artifact in the storage
    FileUtils.copyDirectory(getTestFile(FIFTH_ARTIFACT),
        repoStorageDirectory);

    //Now reindex the repo
    reindexHostedRepository(reindexId);

    //Now make sure there is an index file, and 4 incremental file
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("3").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("4").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("5").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("6").exists());
    validateCurrentHostedIncrementalCounter(5);

    //Now make sure that the search is properly working
    searchForArtifactInHostedIndex(FIRST_ARTIFACT, true);
    searchForArtifactInHostedIndex(SECOND_ARTIFACT, true);
    searchForArtifactInHostedIndex(THIRD_ARTIFACT, true);
    searchForArtifactInHostedIndex(FOURTH_ARTIFACT, true);
    searchForArtifactInHostedIndex(FIFTH_ARTIFACT, true);

    //Now reindex the repo again, and make sure nothing new is created
    reindexHostedRepository(reindexId);

    //Now make sure there is an index file, and 4 incremental file
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("3").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("4").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("5").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("6").exists());
    validateCurrentHostedIncrementalCounter(5);

    //Now make sure that the search is properly working
    searchForArtifactInHostedIndex(FIRST_ARTIFACT, true);
    searchForArtifactInHostedIndex(SECOND_ARTIFACT, true);
    searchForArtifactInHostedIndex(THIRD_ARTIFACT, true);
    searchForArtifactInHostedIndex(FOURTH_ARTIFACT, true);
    searchForArtifactInHostedIndex(FIFTH_ARTIFACT, true);

    //Now delete some items and put some back
    deleteAllNonHiddenContent(getHostedRepositoryStorageDirectory());
    FileUtils.copyDirectory(getTestFile(FIRST_ARTIFACT),
        repoStorageDirectory);
    FileUtils.copyDirectory(getTestFile(SECOND_ARTIFACT),
        repoStorageDirectory);

    //Reindex
    reindexHostedRepository(reindexId);

    //Now make sure there is an index file, and 5 incremental file
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("3").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("4").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("5").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("6").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("7").exists());
    validateCurrentHostedIncrementalCounter(6);

    //Now make sure that the search is properly working
    searchForArtifactInHostedIndex(FIRST_ARTIFACT, true);
    searchForArtifactInHostedIndex(SECOND_ARTIFACT, true);
    searchForArtifactInHostedIndex(THIRD_ARTIFACT, false);
    searchForArtifactInHostedIndex(FOURTH_ARTIFACT, false);
    searchForArtifactInHostedIndex(FIFTH_ARTIFACT, false);
  }
}
