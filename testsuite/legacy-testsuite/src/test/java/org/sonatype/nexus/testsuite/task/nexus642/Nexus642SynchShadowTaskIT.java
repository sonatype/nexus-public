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
package org.sonatype.nexus.testsuite.task.nexus642;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.SynchronizeShadowTaskDescriptor;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus642SynchShadowTaskIT
    extends AbstractNexusIntegrationTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void synchShadowTest()
      throws Exception
  {
    // create shadow repo 'nexus-shadow-repo'
    RepositoryMessageUtil repoUtil =
        new RepositoryMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
    String shadowRepoId = "nexus-shadow-repo";
    String taskName = "synchShadowTest";

    RepositoryShadowResource repo = new RepositoryShadowResource();
    repo.setId(shadowRepoId);
    repo.setProvider("m2-m1-shadow");
    // format is neglected by server from now on, provider is the new guy in the town
    repo.setFormat("maven1");
    repo.setName(shadowRepoId);
    repo.setRepoType("virtual");
    repo.setShadowOf(this.getTestRepositoryId());
    repo.setSyncAtStartup(false);
    repo.setExposed(true);
    repoUtil.createRepository(repo);

    // create Sync Repo Task
    // repo: 'nexus-shadow-repo'
    // recurrence: 'manual'
    // run it manually
    this.executeTask(taskName, repo.getId());

    // download the file using the shadow repo
    File actualFile =
        this.downloadFile(
            new URL(this.getBaseNexusUrl() + "content/repositories/" + shadowRepoId + "/" + this.getTestId()
                + "/jars/artifact-5.4.3.jar"), "target/downloads/nexus642.jar");
    File expectedFile = this.getTestResourceAsFile("projects/artifact/artifact.jar");
    Assert.assertTrue(FileTestingUtils.compareFileSHA1s(expectedFile, actualFile));

  }

  private void executeTask(String taskName, String shadowRepo)
      throws Exception
  {
    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("sync-repo-props");
    repo.setValue(shadowRepo);

    ScheduledServicePropertyResource age = new ScheduledServicePropertyResource();
    age.setKey("shadowRepositoryId");
    age.setValue(shadowRepo);

    // clean unused
    TaskScheduleUtil.runTask(taskName, SynchronizeShadowTaskDescriptor.ID, repo, age);
  }

}
