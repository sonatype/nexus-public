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
package org.sonatype.nexus.testsuite.p2.nxcm2076;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.contains;

public class NXCM2076P2ProxyCompositeChangeRemoteUrlIT
    extends AbstractNexusProxyP2IT
{

  private final RepositoryMessageUtil repoUtil;

  public NXCM2076P2ProxyCompositeChangeRemoteUrlIT()
      throws Exception
  {
    super("nxcm2076");
    repoUtil = new RepositoryMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void test()
      throws Exception
  {
    File artifactsXmlFile = downloadFile(
        new URL(getNexusTestRepoUrl() + "artifacts.xml"),
        "target/downloads/nxcm2076/artifactsBeforeChange.xml"
    );
    assertThat(artifactsXmlFile, contains("id=\"com.sonatype.nexus.p2.its.bundle\""));
    assertThat(artifactsXmlFile, not(contains("id=\"com.sonatype.nexus.p2.its.bundle3\"")));

    try {
      downloadFile(new URL(getNexusTestRepoUrl() + "plugins/com.sonatype.nexus.p2.its.bundle3_1.0.0.jar"),
          "target/downloads/nxcm2076/com.sonatype.nexus.p2.its.bundle3_1.0.0.jar");
      Assert.fail("Expected FileNotFoundException for " + getNexusTestRepoUrl()
          + "plugins/com.sonatype.nexus.p2.its.bundle3_1.0.0.jar");
    }
    catch (final FileNotFoundException expected) {
    }

    // Change the remote url
    final RepositoryProxyResource p2ProxyRepo = (RepositoryProxyResource) repoUtil.getRepository(
        getTestRepositoryId()
    );
    String remoteUrl = p2ProxyRepo.getRemoteStorage().getRemoteStorageUrl();
    remoteUrl = remoteUrl.replace("nxcm2076-1", "nxcm2076-2");
    p2ProxyRepo.getRemoteStorage().setRemoteStorageUrl(remoteUrl);
    repoUtil.updateRepo(p2ProxyRepo, false);

    TaskScheduleUtil.waitForAllTasksToStop();

    artifactsXmlFile = downloadFile(
        new URL(getNexusTestRepoUrl() + "artifacts.xml"),
        "target/downloads/nxcm2076/artifactsAfterChange.xml"
    );
    assertThat(artifactsXmlFile, not(contains("id=\"com.sonatype.nexus.p2.its.bundle\"")));
    assertThat(artifactsXmlFile, contains("id=\"com.sonatype.nexus.p2.its.bundle3\""));

    downloadFile(
        new URL(getNexusTestRepoUrl() + "plugins/com.sonatype.nexus.p2.its.bundle3_1.0.0.jar"),
        "target/downloads/nxcm2076/com.sonatype.nexus.p2.its.bundle3_1.0.0.jar"
    );
  }

}
