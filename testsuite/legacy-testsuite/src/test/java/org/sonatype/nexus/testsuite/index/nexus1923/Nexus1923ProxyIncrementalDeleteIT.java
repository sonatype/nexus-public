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

import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class Nexus1923ProxyIncrementalDeleteIT
    extends AbstractNexus1923
{
  public Nexus1923ProxyIncrementalDeleteIT()
      throws Exception
  {
    super();
  }

  @Test
  public void validateIncrementalIndexesForDeleteCreated()
      throws Exception
  {
    File hostedRepoStorageDirectory = getHostedRepositoryStorageDirectory();

    //First create our hosted repository
    createHostedRepository();
    //And hosted repository task
    String hostedReindexId = createHostedReindexTask();
    //index hosted repo
    FileUtils.copyDirectory(getTestFile(FIRST_ARTIFACT),
        hostedRepoStorageDirectory);
    FileUtils.copyDirectory(getTestFile(SECOND_ARTIFACT),
        hostedRepoStorageDirectory);
    FileUtils.copyDirectory(getTestFile(THIRD_ARTIFACT),
        hostedRepoStorageDirectory);
    FileUtils.copyDirectory(getTestFile(FOURTH_ARTIFACT),
        hostedRepoStorageDirectory);
    FileUtils.copyDirectory(getTestFile(FIFTH_ARTIFACT),
        hostedRepoStorageDirectory);
    reindexHostedRepository(hostedReindexId);

    //Now create our proxy repository
    createProxyRepository();

    //will download the initial index because repo has download remote set to true
    TaskScheduleUtil.waitForAllTasksToStop();

    //Now make sure that the search is properly working
    searchForArtifactInProxyIndex(FIRST_ARTIFACT, true);
    searchForArtifactInProxyIndex(SECOND_ARTIFACT, true);
    searchForArtifactInProxyIndex(THIRD_ARTIFACT, true);
    searchForArtifactInProxyIndex(FOURTH_ARTIFACT, true);
    searchForArtifactInProxyIndex(FIFTH_ARTIFACT, true);

    //Now delete some items and put some back
    deleteAllNonHiddenContent(getHostedRepositoryStorageDirectory());
    deleteAllNonHiddenContent(getProxyRepositoryStorageDirectory());
    FileUtils.copyDirectory(getTestFile(FIRST_ARTIFACT),
        hostedRepoStorageDirectory);
    FileUtils.copyDirectory(getTestFile(SECOND_ARTIFACT),
        hostedRepoStorageDirectory);

    //Reindex
    reindexHostedRepository(hostedReindexId);

    String proxyReindexId = createProxyReindexTask();

    //reindex proxy and make sure we cant search for the now missing items
    reindexProxyRepository(proxyReindexId);

    //Make sure the indexes exist, and that a new one has been created with
    //the deletes
    Assert.assertTrue(getProxyRepositoryIndex().exists());
    Assert.assertTrue(getProxyRepositoryIndexIncrement("1").exists());
    Assert.assertFalse(getProxyRepositoryIndexIncrement("2").exists());

    searchForArtifactInProxyIndex(FIRST_ARTIFACT, true);
    searchForArtifactInProxyIndex(SECOND_ARTIFACT, true);
    searchForArtifactInProxyIndex(THIRD_ARTIFACT, false);
    searchForArtifactInProxyIndex(FOURTH_ARTIFACT, false);
    searchForArtifactInProxyIndex(FIFTH_ARTIFACT, false);
  }
}