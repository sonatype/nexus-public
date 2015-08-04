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

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.EvictUnusedItemsTaskDescriptor;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Assert;
import org.junit.Test;

public class Nexus977GroupOfGroupsEvictUnusedProxiedItemsTaskIT
    extends AbstractNexusProxyIntegrationTest
{

  @Test
  public void evictUnused()
      throws Exception
  {
    downloadArtifactFromGroup("g4", GavUtil.newGav(getTestId(), "project", "0.8"),
        "target/downloads/nexus977evict");
    downloadArtifactFromGroup("g4", GavUtil.newGav(getTestId(), "project", "2.1"),
        "target/downloads/nexus977evict");

    Assert.assertTrue(new File(nexusWorkDir, "storage/r4/nexus977tasks/project/0.8/project-0.8.jar").exists());
    Assert.assertTrue(new File(nexusWorkDir, "storage/r5/nexus977tasks/project/2.1/project-2.1.jar").exists());

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("g4");

    ScheduledServicePropertyResource age = new ScheduledServicePropertyResource();
    age.setKey("evictOlderCacheItemsThen");
    age.setValue(String.valueOf(0));

    TaskScheduleUtil.runTask(EvictUnusedItemsTaskDescriptor.ID, repo, age);

    Assert.assertFalse(new File(nexusWorkDir, "storage/r4/nexus977tasks/project/0.8/project-0.8.jar").exists());
    Assert.assertFalse(new File(nexusWorkDir, "storage/r5/nexus977tasks/project/2.1/project-2.1.jar").exists());

  }

}
