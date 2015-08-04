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
package org.sonatype.nexus.testsuite.task.nexus1022;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.RebuildMavenMetadataTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Assert;
import org.junit.Test;

public class Nexus1022RebuildRepositoryMavenMetadataTaskIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void rebuildMavenMetadata()
      throws Exception
  {
        /*
         * if(true) { printKnownErrorButDoNotFail( getClass(), "rebuildMavenMetadata" ); return; }
         */

    String dummyFile = new File(nexusWorkDir, "nexus1022.dummy").getAbsolutePath();
    String repoPrefix = "storage/nexus-test-harness-repo/";

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();

    repo.setKey("repositoryId");

    repo.setValue(REPO_TEST_HARNESS_REPO);

    TaskScheduleUtil.runTask("RebuildMavenMetadata-Nexus1022", RebuildMavenMetadataTaskDescriptor.ID, repo);
    TaskScheduleUtil.waitForAllTasksToStop();

    File artifactDirMd =
        new File(nexusWorkDir, repoPrefix + "nexus1022/foo/bar/artifact/maven-metadata.xml");
    Assert.assertTrue("Maven metadata file should be generated after rebuild", artifactDirMd.exists());

    File groupPluginMd = new File(nexusWorkDir, repoPrefix + "nexus1022/foo/bar/plugins/maven-metadata.xml");
    Assert.assertTrue("Maven metadata file should be generated after rebuild", groupPluginMd.exists());

    // just downloading it into dummy, since we are just checking is download possible
    // if not, downloadFile() will fail anyway. The content is not we are interested in.
    downloadFile(new URL(nexusBaseUrl + "content/repositories/nexus-test-harness-repo/"
        + "nexus1022/foo/bar/plugins/maven-metadata.xml"), dummyFile);
    downloadFile(
        new URL(nexusBaseUrl + "content/groups/public/" + "nexus1022/foo/bar/plugins/maven-metadata.xml"),
        dummyFile);

    downloadFile(new URL(nexusBaseUrl + "content/repositories/nexus-test-harness-repo/"
        + "nexus1022/foo/bar/plugins/maven-metadata.xml" + ".sha1"), dummyFile);
    downloadFile(new URL(nexusBaseUrl + "content/groups/public/" + "nexus1022/foo/bar/plugins/maven-metadata.xml"
        + ".sha1"), dummyFile);
  }

}
