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
package org.sonatype.nexus.testsuite.proxy.nexus2922;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class Nexus2922CacheRemoteArtifactsIT
    extends AbstractNexusProxyIntegrationTest
{
  private static Gav GAV1;

  private static Gav GAV2;

  @Override
  protected void runOnce()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();
    settings.setSecurityAnonymousAccessEnabled(false);
    SettingsMessageUtil.save(settings);
  }

  @BeforeClass
  public static void enableSecurity() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  public Nexus2922CacheRemoteArtifactsIT() {
    super("release-proxy-repo-1");
    GAV1 = GavUtil.newGav("nexus2922", "artifact", "1.0.0");
    GAV2 = GavUtil.newGav("nexus2922", "artifact", "2.0.0");
  }

  protected void clearCredentials() {
    TestContainer.getInstance().getTestContext().setUsername("");
    TestContainer.getInstance().getTestContext().setPassword("");
  }

  @Test
  @Category(PROXY.class)
  public void downloadNoPriv()
      throws IOException
  {
    String msg = null;
    clearCredentials();

    try {
      this.downloadArtifactFromRepository(REPO_RELEASE_PROXY_REPO1, GAV1, "target/downloads");
      Assert.fail("Should fail to download artifact");
    }
    catch (FileNotFoundException e) {
      // ok!
      msg = e.getMessage();
    }

    File file = new File(nexusWorkDir, "storage/release-proxy-repo-1/nexus2922/artifact/1.0.0/artifact-1.0.0.jar");
    Assert.assertFalse(file.toString(), file.exists());

    Assert.assertTrue(msg, msg.contains("401"));
  }

  @Test
  @Category(PROXY.class)
  public void downloadNoPrivFromProxy()
      throws IOException
  {
    String msg = null;
    clearCredentials();

    try {
      this.downloadArtifactFromRepository(REPO_TEST_HARNESS_REPO, GAV1, "target/downloads");
      Assert.fail("Should fail to download artifact");
    }
    catch (FileNotFoundException e) {
      // ok!
      msg = e.getMessage();
    }

    File file =
        new File(nexusWorkDir, "storage/nexus-test-harness-repo/nexus2922/artifact/1.0.0/artifact-1.0.0.jar");
    Assert.assertFalse(file.toString(), file.exists());

    Assert.assertTrue(msg, msg.contains("401"));
  }

  @Test
  @Category(PROXY.class)
  public void downloadAdmin()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    this.downloadArtifactFromRepository(REPO_RELEASE_PROXY_REPO1, GAV2, "target/downloads");

    File file = new File(nexusWorkDir, "storage/release-proxy-repo-1/nexus2922/artifact/2.0.0/artifact-2.0.0.jar");
    Assert.assertTrue(file.toString(), file.exists());
  }
}
