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
import java.util.List;

import org.sonatype.nexus.index.tasks.descriptors.RepairIndexTaskDescriptor;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class Nexus977GroupOfGroupsReindexTaskIT
    extends AbstractNexusProxyIntegrationTest
{

  @Test
  public void reindex()
      throws Exception
  {
    List<NexusArtifact> result = getSearchMessageUtil().searchForGav(getTestId(), "project", null, "g4");
    // deployed artifacts get automatically indexed
    Assert.assertEquals(3, result.size());

    // add some extra artifacts
    File dest = new File(nexusWorkDir, "storage/r1/nexus977tasks/project/1.0/project-1.0.jar");
    dest.getParentFile().mkdirs();
    FileUtils.copyFile(getTestFile("project.jar"), dest);

    dest = new File(nexusWorkDir, "storage/r2/nexus977tasks/project/2.0/project-2.0.jar");
    dest.getParentFile().mkdirs();
    FileUtils.copyFile(getTestFile("project.jar"), dest);

    dest = new File(nexusWorkDir, "storage/r3/nexus977tasks/project/3.0/project-3.0.jar");
    dest.getParentFile().mkdirs();
    FileUtils.copyFile(getTestFile("project.jar"), dest);

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("g4");
    TaskScheduleUtil.runTask("ReindexTaskDescriptor-snapshot", RepairIndexTaskDescriptor.ID, repo);

    result = getSearchMessageUtil().searchForGav(getTestId(), "project", null, "g4");
    Assert.assertEquals(8, result.size());
  }

}
