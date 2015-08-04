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
package org.sonatype.nexus.testsuite.task.nexus641;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.index.tasks.descriptors.UpdateIndexTaskDescriptor;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test task Reindex Repositories.
 *
 * @author marvin
 */
public class Nexus641ReindexTaskIT
    extends AbstractNexusIntegrationTest
{
  protected static Logger logger = LoggerFactory.getLogger(Nexus641ReindexTaskIT.class);

  private File repositoryPath = new File(nexusWorkDir, "storage/" + this.getTestRepositoryId());


  public Nexus641ReindexTaskIT() throws IOException {
    super("nexus641");

  }

  @Test
  public void testReindex()
      throws Exception
  {

    this.repositoryPath = new File(nexusWorkDir, "storage/" + this.getTestRepositoryId());
    logger.info("path: " + repositoryPath);
    File oldSnapshot = getTestFile("repo");

    // Copy artifact to avoid indexing
    FileUtils.copyDirectory(oldSnapshot, repositoryPath);

    // try to seach and fail
    List<NexusArtifact> search = getSearchMessageUtil().searchFor("nexus641");
    Assert.assertEquals("The artifact was already indexed", search.size(), 1);

    // reindex
    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue(this.getTestRepositoryId());

    // reindex
    TaskScheduleUtil.runTask(UpdateIndexTaskDescriptor.ID, prop);

    // try to download again and success
    search = getSearchMessageUtil().searchFor("nexus641");
    Assert.assertEquals("The artifact should be indexed", search.size(), 2);
  }

}
