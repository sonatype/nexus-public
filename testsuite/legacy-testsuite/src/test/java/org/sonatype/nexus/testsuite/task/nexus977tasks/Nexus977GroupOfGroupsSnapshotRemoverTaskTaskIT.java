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
import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.SnapshotRemovalTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.maven.model.Model;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;

public class Nexus977GroupOfGroupsSnapshotRemoverTaskTaskIT
    extends AbstractNexusProxyIntegrationTest
{

  @Override
  protected void deployArtifacts(File project, String wagonHint, String deployUrl, Model model)
      throws Exception
  {
    super.deployArtifacts(project, wagonHint, deployUrl, model);

    if (!model.getVersion().equals("1.0-SNAPSHOT")) {
      return;
    }

    File pom = new File(project, "pom.xml");

    String artifactFileName = model.getArtifactId() + "." + model.getPackaging();
    File artifactFile = new File(project, artifactFileName);

    String path = "nexus977tasks/project/1.0-SNAPSHOT/project-1.0-20100520.154534-88.";

    try {
      getDeployUtils().deployWithWagon(wagonHint, deployUrl, artifactFile, path + "jar");

      getDeployUtils().deployWithWagon(wagonHint, deployUrl, pom, path + "pom");
    }
    catch (Exception e) {
      log.error(getTestId() + " Unable to deploy " + artifactFileName, e);
      throw e;
    }
  }

  @Test
  public void snapshotRemoval()
      throws Exception
  {
    String path = "nexus977tasks/project/1.0-SNAPSHOT/project-1.0-20100520.154534-88.jar";
    downloadFile(new URL(AbstractNexusIntegrationTest.nexusBaseUrl + REPOSITORY_RELATIVE_URL + "g4/" + path),
        "target/downloads/nexus977tasks/project-1.0-20100520.154534-88.jar");

    ScheduledServicePropertyResource keepSnapshotsProp = new ScheduledServicePropertyResource();
    keepSnapshotsProp.setKey("minSnapshotsToKeep");
    keepSnapshotsProp.setValue(String.valueOf(0));

    ScheduledServicePropertyResource ageProp = new ScheduledServicePropertyResource();
    ageProp.setKey("removeOlderThanDays");
    ageProp.setValue(String.valueOf(0));

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("g4");
    TaskScheduleUtil.runTask("SnapshotRemovalTask-snapshot", SnapshotRemovalTaskDescriptor.ID, repo,
        keepSnapshotsProp, ageProp);

    try {
      downloadFile(
          new URL(AbstractNexusIntegrationTest.nexusBaseUrl + REPOSITORY_RELATIVE_URL + "g4/" + path),
          "target/downloads/nexus977tasks/project-1.0-20100520.154534-88-2.jar");
      Assert.fail("snapshot removal should have deleted this");
    }
    catch (FileNotFoundException e) {
      MatcherAssert.assertThat(e.getMessage(), containsString("404"));
    }

  }
}
