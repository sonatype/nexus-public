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
package org.sonatype.nexus.testsuite.index.nexus983;

import java.io.File;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.ITGroups.INDEX;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.apache.maven.index.SearchType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * Copy (filesystem copy) a jar to a nexus repo and run reindex to see what happens
 */
public class Nexus983IndexArtifactsWihoutPomIT
    extends AbstractNexusIntegrationTest
{
  @Test
  @Category(INDEX.class)
  public void deployPomlessArtifact()
      throws Exception
  {
    File artifactFile = getTestFile("artifact.jar");
    getDeployUtils().deployWithWagon("http", nexusBaseUrl + "content/repositories/" + REPO_TEST_HARNESS_REPO,
        artifactFile, "nexus983/nexus983-artifact1/1.0.0/nexus983-artifact1-1.0.0.jar");

    // wait to index up the changes
    getEventInspectorsUtil().waitForCalmPeriod();

    List<NexusArtifact> artifacts = getSearchMessageUtil().searchFor("nexus983-artifact1", SearchType.EXACT);
    assertThat("Should find exactly one artifact", artifacts, hasSize(1));
  }

  @Test
  @Category(INDEX.class)
  public void copyPomlessArtifact()
      throws Exception
  {
    File artifactFile = getTestFile("artifact.jar");
    FileUtils.copyFile(artifactFile, new File(nexusWorkDir, "storage/" + REPO_TEST_HARNESS_REPO
        + "/nexus983/nexus983-artifact2/1.0.0/nexus983-artifact2-1.0.0.jar"));

    // if something is running, let it finish
    TaskScheduleUtil.waitForAllTasksToStop();

    RepositoryMessageUtil.updateIndexes(REPO_TEST_HARNESS_REPO);

    // wait to index up the changes
    getEventInspectorsUtil().waitForCalmPeriod();

    List<NexusArtifact> artifacts = getSearchMessageUtil().searchFor("nexus983-artifact2", SearchType.EXACT);
    assertThat("Should find exactly one artifact", artifacts, hasSize(1));
  }

}
