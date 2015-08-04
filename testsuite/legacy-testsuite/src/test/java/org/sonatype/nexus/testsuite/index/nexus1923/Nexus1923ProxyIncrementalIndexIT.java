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

public class Nexus1923ProxyIncrementalIndexIT
    extends AbstractNexus1923
{
  public Nexus1923ProxyIncrementalIndexIT()
      throws Exception
  {
    super();
  }

  @Test
  @Category(INDEX.class)
  public void validateIncrementalIndexesDownloaded()
      throws Exception
  {
    File hostedRepoStorageDirectory = getHostedRepositoryStorageDirectory();

    //First create our hosted repository
    createHostedRepository();
    //And hosted repository task
    String hostedReindexId = createHostedReindexTask();

    TaskScheduleUtil.waitForAllTasksToStop();

    FileUtils.copyDirectory(getTestFile(FIRST_ARTIFACT),
        hostedRepoStorageDirectory);

    reindexHostedRepository(hostedReindexId);

    TaskScheduleUtil.waitForAllTasksToStop();

    //validate that after reindex is done we have an incremental chunk in the hosted repo
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("2").exists());

    //Now create our proxy repository
    createProxyRepository();

    //will download the initial index because repo has download remote set to true
    TaskScheduleUtil.waitForAllTasksToStop();

    String proxyReindexId = createProxyReindexTask();

    reindexProxyRepository(proxyReindexId);

    TaskScheduleUtil.waitForAllTasksToStop();

    Assert.assertTrue(getProxyRepositoryIndex().exists());
    Assert.assertFalse(getProxyRepositoryIndexIncrement("1").exists());

    //Now make sure that the search is properly working
    searchForArtifactInProxyIndex(FIRST_ARTIFACT, true);
    searchForArtifactInProxyIndex(SECOND_ARTIFACT, false);
    searchForArtifactInProxyIndex(THIRD_ARTIFACT, false);
    searchForArtifactInProxyIndex(FOURTH_ARTIFACT, false);
    searchForArtifactInProxyIndex(FIFTH_ARTIFACT, false);

    //Now add items to hosted, and reindex to create incremental chunk
    FileUtils.copyDirectory(getTestFile(SECOND_ARTIFACT),
        hostedRepoStorageDirectory);
    reindexHostedRepository(hostedReindexId);

    TaskScheduleUtil.waitForAllTasksToStop();

    //validate that after reindex is done we have an incremental chunk in the hosted repo
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("3").exists());

    //now download via the proxy repo
    reindexProxyRepository(proxyReindexId);

    TaskScheduleUtil.waitForAllTasksToStop();

    //validate that after reindex is done we have an incremental chunk of our own in the proxy repo
    Assert.assertTrue(getProxyRepositoryIndex().exists());
    Assert.assertTrue(getProxyRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getProxyRepositoryIndexIncrement("2").exists());

    //Now make sure that the search is properly working
    searchForArtifactInProxyIndex(FIRST_ARTIFACT, true);
    searchForArtifactInProxyIndex(SECOND_ARTIFACT, true);
    searchForArtifactInProxyIndex(THIRD_ARTIFACT, false);
    searchForArtifactInProxyIndex(FOURTH_ARTIFACT, false);
    searchForArtifactInProxyIndex(FIFTH_ARTIFACT, false);

    // Now make the hosted have 3 more index chunks
    FileUtils.copyDirectory(getTestFile(THIRD_ARTIFACT),
        hostedRepoStorageDirectory);
    reindexHostedRepository(hostedReindexId);

    TaskScheduleUtil.waitForAllTasksToStop();

    //validate that after reindex is done we have an incremental chunk in the hosted repo
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("3").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("4").exists());

    FileUtils.copyDirectory(getTestFile(FOURTH_ARTIFACT),
        hostedRepoStorageDirectory);
    reindexHostedRepository(hostedReindexId);

    TaskScheduleUtil.waitForAllTasksToStop();

    //validate that after reindex is done we have an incremental chunk in the hosted repo
    Assert.assertTrue(getHostedRepositoryIndex().exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("2").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("3").exists());
    Assert.assertTrue(getHostedRepositoryIndexIncrement("4").exists());
    Assert.assertFalse(getHostedRepositoryIndexIncrement("5").exists());

    reindexProxyRepository(proxyReindexId);

    TaskScheduleUtil.waitForAllTasksToStop();

    //validate that after reindex is done we have an incremental chunk of our own in the proxy repo
    //of course only 2 indexes, as these published indexes should NOT line up 1 to 1 with the hosted repo
    Assert.assertTrue(getProxyRepositoryIndex().exists());
    Assert.assertTrue(getProxyRepositoryIndexIncrement("1").exists());
    Assert.assertTrue(getProxyRepositoryIndexIncrement("2").exists());
    Assert.assertFalse(getProxyRepositoryIndexIncrement("3").exists());

    //Now make sure that the search is properly working
    searchForArtifactInProxyIndex(FIRST_ARTIFACT, true);
    searchForArtifactInProxyIndex(SECOND_ARTIFACT, true);
    searchForArtifactInProxyIndex(THIRD_ARTIFACT, true);
    searchForArtifactInProxyIndex(FOURTH_ARTIFACT, true);
    searchForArtifactInProxyIndex(FIFTH_ARTIFACT, false);
  }
}
