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
package org.sonatype.nexus.testsuite.proxy.nexus179;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Create an http server. Create a proxy repo to http server. Access a file from http server. Stop http server. access
 * file again (should work.) Clear cache and try it again.
 */
public class Nexus179RemoteRepoDownIT
    extends AbstractNexusProxyIntegrationTest
{

  public Nexus179RemoteRepoDownIT() {
    super(REPO_RELEASE_PROXY_REPO1);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  @Category(PROXY.class)
  public void downloadFromDisconnectedProxy()
      throws Exception
  {
    // stop the proxy
    this.stopProxy();

    // delete everything under this tests group id if exist anything
    this.deleteFromRepository("nexus179/");

    Gav gav =
        new Gav(this.getTestId(), "repo-down-test-artifact", "1.0.0", null, "xml", 0, new Date().getTime(),
            "Simple Test Artifact", false, null, false, null);

    File localFile = this.getLocalFile(REPO_RELEASE_PROXY_REPO1, gav);

    // make sure this exists first, or the test is invalid anyway.
    Assert.assertTrue("The File: " + localFile + " does not exist.", localFile.exists());

    try {
      this.downloadArtifact(gav, "target/downloads");
      Assert.fail("A FileNotFoundException should have been thrown.");
    }
    catch (FileNotFoundException e) {
    }

    // Start up the proxy
    this.startProxy();

    // should not be able to download artifact after starting proxy, without clearing the cache.
    try {
      this.downloadArtifact(gav, "target/downloads");
      Assert.fail("A FileNotFoundException should have been thrown.");
    }
    catch (FileNotFoundException e) {
    }

    clearProxyCache();

    // unblock the proxy
    repositoryUtil.setBlockProxy(REPO_RELEASE_PROXY_REPO1, false);

    File artifact = this.downloadArtifact(gav, "target/downloads");

    Assert.assertTrue(FileTestingUtils.compareFileSHA1s(artifact, localFile));
  }

  private void clearProxyCache()
      throws Exception
  {

    String serviceURI = "service/local/data_cache/repositories/" + REPO_RELEASE_PROXY_REPO1 + "/content";

    Response response = RequestFacade.sendMessage(serviceURI, Method.DELETE);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not clear the cache for repo: " + REPO_RELEASE_PROXY_REPO1);
    }

    TaskScheduleUtil.waitForAllTasksToStop();
    getEventInspectorsUtil().waitForCalmPeriod();
  }

}
