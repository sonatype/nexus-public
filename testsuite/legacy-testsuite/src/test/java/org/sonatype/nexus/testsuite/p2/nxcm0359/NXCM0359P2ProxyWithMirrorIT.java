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
package org.sonatype.nexus.testsuite.p2.nxcm0359;

import java.io.IOException;

import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;

public class NXCM0359P2ProxyWithMirrorIT
    extends AbstractNexusProxyP2IT
{

  public NXCM0359P2ProxyWithMirrorIT() {
    super("nxcm0359");
  }

  @Override
  protected void copyTestResources()
      throws IOException
  {
    super.copyTestResources();

    final String proxyRepoBaseUrl = TestProperties.getString("proxy.repo.base.url");

    replaceInFile(localStorageDir + "/nxcm0359/artifacts.xml", "${proxy-repo-base-url}", proxyRepoBaseUrl);
    replaceInFile(localStorageDir + "/nxcm0359/mirrors.xml", "${proxy-repo-base-url}", proxyRepoBaseUrl);
  }

  @Test
  public void test()
      throws Exception
  {
    installAndVerifyP2Feature();
  }

}
