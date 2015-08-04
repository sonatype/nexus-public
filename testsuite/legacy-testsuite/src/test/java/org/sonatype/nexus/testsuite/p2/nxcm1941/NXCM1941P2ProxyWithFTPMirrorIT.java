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
package org.sonatype.nexus.testsuite.p2.nxcm1941;

import java.io.IOException;

import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

public class NXCM1941P2ProxyWithFTPMirrorIT
    extends AbstractNexusProxyP2IT
{

  public NXCM1941P2ProxyWithFTPMirrorIT() {
    super("nxcm1941");
  }

  @Override
  protected void copyTestResources()
      throws IOException
  {
    super.copyTestResources();

    final String proxyRepoBaseUrl = TestProperties.getString("proxy.repo.base.url");
    assertThat(proxyRepoBaseUrl, startsWith("http://"));

    replaceInFile(localStorageDir + "/nxcm1941/artifacts.xml", "${proxy-repo-base-url}", proxyRepoBaseUrl);
    replaceInFile(localStorageDir + "/nxcm1941/mirrors.xml", "${proxy-repo-base-url}", proxyRepoBaseUrl);
    replaceInFile(
        localStorageDir + "/nxcm1941/mirrors.xml",
        "${ftp-proxy-repo-base-url}", "ftp" + proxyRepoBaseUrl.substring(4)
    );
  }

  @Test
  public void test()
      throws Exception
  {
    installAndVerifyP2Feature();
  }

}
