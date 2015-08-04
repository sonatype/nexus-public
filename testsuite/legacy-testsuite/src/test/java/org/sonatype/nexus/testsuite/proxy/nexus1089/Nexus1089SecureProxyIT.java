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
package org.sonatype.nexus.testsuite.proxy.nexus1089;

import java.io.File;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.test.utils.FileTestingUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class Nexus1089SecureProxyIT
    extends AbstractNexusProxyIntegrationTest
{
  protected ServletServer secureProxyServer = null;

  @Override
  @Before
  public void startProxy()
      throws Exception
  {
    secureProxyServer = lookup(ServletServer.class, "secure");
    secureProxyServer.start();
  }

  @Override
  @After
  public void stopProxy()
      throws Exception
  {
    if (secureProxyServer != null) {
      secureProxyServer.stop();
    }
  }

  @Test
  @Category(PROXY.class)
  public void downloadArtifact()
      throws Exception
  {
    File localFile = this.getLocalFile("release-proxy-repo-1", "nexus1089", "artifact", "1.0", "jar");

    File artifact = this.downloadArtifact("nexus1089", "artifact", "1.0", "jar", null, "target/downloads");

    Assert.assertTrue(FileTestingUtils.compareFileSHA1s(artifact, localFile));

  }
}
