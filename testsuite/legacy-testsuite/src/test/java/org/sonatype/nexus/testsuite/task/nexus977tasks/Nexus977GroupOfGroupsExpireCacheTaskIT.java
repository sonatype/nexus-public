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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ExpireCacheTaskDescriptor;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.apache.maven.index.artifact.Gav;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;

public class Nexus977GroupOfGroupsExpireCacheTaskIT
    extends AbstractNexusProxyIntegrationTest
{

  @Test
  public void expireCache()
      throws Exception
  {
    Gav gav = GavUtil.newGav(getTestId(), "project", "1.0");
    failDownload(gav);

    File dest = new File(localStorageDir, "nexus977tasks/1/nexus977tasks/project/1.0/project-1.0.jar");
    dest.getParentFile().mkdirs();
    FileUtils.copyFile(getTestFile("project.jar"), dest);
    failDownload(gav);

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("g4");
    TaskScheduleUtil.runTask("ExpireCacheTaskDescriptor-snapshot", ExpireCacheTaskDescriptor.ID, repo);

    downloadArtifactFromRepository("g4", gav, "target/downloads/nexus977tasks");
  }

  private void failDownload(Gav gav)
      throws IOException
  {
    try {
      downloadArtifactFromRepository("g4", gav, "target/downloads/nexus977tasks");
      Assert.fail("snapshot removal should have deleted this");
    }
    catch (FileNotFoundException e) {
      MatcherAssert.assertThat(e.getMessage(), containsString("404"));
    }
  }
}
