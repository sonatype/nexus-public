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

import org.sonatype.nexus.integrationtests.ITGroups.INDEX;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class Nexus1923GroupIncrementalIndexIT
    extends AbstractNexus1923
{
  public Nexus1923GroupIncrementalIndexIT()
      throws Exception
  {
    super();
  }

  @Test
  @Category(INDEX.class)
  public void validateIncrementalIndexesCreated()
      throws Exception
  {
    createHostedRepository();
    createSecondHostedRepository();
    createThirdHostedRepository();
    createGroup(GROUP_ID, HOSTED_REPO_ID, SECOND_HOSTED_REPO_ID, THIRD_HOSTED_REPO_ID);

    //all groups/repos should be indexed once on creation!
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getSecondHostedRepositoryIndex().exists());
    Assert.assertTrue(getThirdHostedRepositoryIndex().exists());
    Assert.assertTrue(getGroupIndex().exists());

    String reindexId = createReindexTask(GROUP_ID, GROUP_REINDEX_TASK_NAME);

    // deploy artifact 1 on repo 1
    FileUtils.copyDirectory(getTestFile(FIRST_ARTIFACT), getHostedRepositoryStorageDirectory());

    reindexRepository(reindexId, GROUP_REINDEX_TASK_NAME);

    // repo 1 has index when it is created, so should create .1
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("2").exists());
    validateCurrentHostedIncrementalCounter(1);

    Assert.assertTrue(getSecondHostedRepositoryIndex().exists());
    Assert.assertFalse(getSecondHostedRepositoryIndexIncrement("1").exists());
    validateCurrentSecondHostedIncrementalCounter(0);

    Assert.assertTrue(getThirdHostedRepositoryIndex().exists());
    Assert.assertFalse(getThirdHostedRepositoryIndexIncrement("1").exists());
    validateCurrentThirdHostedIncrementalCounter(0);

    Assert.assertTrue(getGroupIndex().exists());
    Assert.assertTrue(getGroupIndexIncrement("1").exists());
    Assert.assertFalse(getGroupIndexIncrement("2").exists());
    validateCurrentGroupIncrementalCounter(1);

    searchFor(HOSTED_REPO_ID, FIRST_ARTIFACT);

    // deploy artifact 2 on repo 2
    FileUtils.copyDirectory(getTestFile(SECOND_ARTIFACT), getSecondHostedRepositoryStorageDirectory());

    reindexRepository(reindexId, GROUP_REINDEX_TASK_NAME);

    // shouldn't changed from first status
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("2").exists());
    validateCurrentHostedIncrementalCounter(1);

    // repo 2 has index when it is created, so should create .1
    Assert.assertTrue(getSecondHostedRepositoryIndex().exists());
    Assert.assertTrue(getSecondHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getSecondHostedRepositoryIndexIncrement("2").exists());
    validateCurrentSecondHostedIncrementalCounter(1);

    // shouldn't change from first status
    Assert.assertTrue(getThirdHostedRepositoryIndex().exists());
    Assert.assertFalse(getThirdHostedRepositoryIndexIncrement("1").exists());
    validateCurrentThirdHostedIncrementalCounter(0);

    // group create index .1
    Assert.assertTrue(getGroupIndex().exists());
    Assert.assertTrue(getGroupIndexIncrement("1").exists());
    Assert.assertTrue(getGroupIndexIncrement("2").exists());
    Assert.assertFalse(getGroupIndexIncrement("3").exists());
    validateCurrentGroupIncrementalCounter(2);

    searchFor(HOSTED_REPO_ID, FIRST_ARTIFACT);
    searchFor(SECOND_HOSTED_REPO_ID, SECOND_ARTIFACT);

    // deploy artifact 3 on repo 3
    FileUtils.copyDirectory(getTestFile(THIRD_ARTIFACT), getThirdHostedRepositoryStorageDirectory());
    reindexRepository(reindexId, GROUP_REINDEX_TASK_NAME);

    // shouldn't changed from previous status
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("2").exists());
    validateCurrentHostedIncrementalCounter(1);

    // shouldn't changed from previous status
    Assert.assertTrue(getSecondHostedRepositoryIndex().exists());
    Assert.assertTrue(getSecondHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getSecondHostedRepositoryIndexIncrement("2").exists());
    validateCurrentSecondHostedIncrementalCounter(1);

    // repo 3 has index when it is created, so should create .1
    Assert.assertTrue(getThirdHostedRepositoryIndex().exists());
    Assert.assertTrue(getThirdHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getThirdHostedRepositoryIndexIncrement("2").exists());
    validateCurrentThirdHostedIncrementalCounter(1);

    // group create index .2
    Assert.assertTrue(getGroupIndex().exists());
    Assert.assertTrue(getGroupIndexIncrement("1").exists());
    Assert.assertTrue(getGroupIndexIncrement("2").exists());
    Assert.assertTrue(getGroupIndexIncrement("3").exists());
    Assert.assertFalse(getGroupIndexIncrement("4").exists());
    validateCurrentGroupIncrementalCounter(3);

    searchFor(HOSTED_REPO_ID, FIRST_ARTIFACT);
    searchFor(SECOND_HOSTED_REPO_ID, SECOND_ARTIFACT);
    searchFor(THIRD_HOSTED_REPO_ID, THIRD_ARTIFACT);

    // deploy artifact 4 on repo 1
    FileUtils.copyDirectory(getTestFile(FOURTH_ARTIFACT), getHostedRepositoryStorageDirectory());
    reindexRepository(reindexId, GROUP_REINDEX_TASK_NAME);

    // now repo 1 gets index .2
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("3").exists());
    validateCurrentHostedIncrementalCounter(2);

    // shouldn't changed from previous status
    Assert.assertTrue(getSecondHostedRepositoryIndex().exists());
    Assert.assertTrue(getSecondHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getSecondHostedRepositoryIndexIncrement("2").exists());
    validateCurrentSecondHostedIncrementalCounter(1);

    // repo 3 has index when it is created, so should create .1
    Assert.assertTrue(getThirdHostedRepositoryIndex().exists());
    Assert.assertTrue(getThirdHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getThirdHostedRepositoryIndexIncrement("2").exists());
    validateCurrentThirdHostedIncrementalCounter(1);

    // group create index .3
    Assert.assertTrue(getGroupIndex().exists());
    Assert.assertTrue(getGroupIndexIncrement("1").exists());
    Assert.assertTrue(getGroupIndexIncrement("2").exists());
    Assert.assertTrue(getGroupIndexIncrement("3").exists());
    Assert.assertTrue(getGroupIndexIncrement("4").exists());
    Assert.assertFalse(getGroupIndexIncrement("5").exists());
    validateCurrentGroupIncrementalCounter(4);

    searchFor(HOSTED_REPO_ID, FIRST_ARTIFACT, FOURTH_ARTIFACT);
    searchFor(SECOND_HOSTED_REPO_ID, SECOND_ARTIFACT);
    searchFor(THIRD_HOSTED_REPO_ID, THIRD_ARTIFACT);
  }

}
