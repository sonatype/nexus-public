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
package org.sonatype.nexus.testsuite.task.nexus977tasks;

import java.io.File;

import org.sonatype.nexus.index.tasks.descriptors.DownloadIndexesTaskDescriptor;
import org.sonatype.nexus.index.tasks.descriptors.PublishIndexesTaskDescriptor;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Assert;
import org.junit.Test;

public class Nexus977GroupOfGroupsPublishIndexesTaskIT
    extends AbstractNexusProxyIntegrationTest
{

  @Override
  protected void runOnce()
      throws Exception
  {
    super.runOnce();

    // first must be sure there is an index to be published
    RepositoryMessageUtil.updateIndexes("r1", "r2", "r3");

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("r4");
    TaskScheduleUtil.runTask("r4", DownloadIndexesTaskDescriptor.ID, repo);

    repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("r5");
    TaskScheduleUtil.runTask("r5", DownloadIndexesTaskDescriptor.ID, repo);
  }

  @Test
  public void publishIndexes()
      throws Exception
  {
    Assert.assertFalse(new File(nexusWorkDir, "storage/g1/.index").exists());
    Assert.assertFalse(new File(nexusWorkDir, "storage/g2/.index").exists());
    Assert.assertFalse(new File(nexusWorkDir, "storage/g3/.index").exists());
    Assert.assertFalse(new File(nexusWorkDir, "storage/g4/.index").exists());

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("g4");
    TaskScheduleUtil.runTask("PublishIndexesTaskDescriptor-snapshot", PublishIndexesTaskDescriptor.ID, repo);

    Assert.assertTrue(new File(nexusWorkDir, "storage/r1/.index").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/r2/.index").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/r3/.index").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/r4/.index").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/r5/.index").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/g1/.index").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/g2/.index").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/g3/.index").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/g4/.index").exists());
  }

}
