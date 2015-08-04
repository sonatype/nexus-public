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
package org.sonatype.nexus.testsuite.p2.nxcm3916;

import java.net.URL;

import org.sonatype.jettytestsuite.ProxyServer;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;

public class NXCM3916SecureWebProxiedP2IT
    extends AbstractNexusProxyP2IT
{

  private static String baseProxyURL;

  protected ProxyServer webProxyServer;

  static {
    baseProxyURL = TestProperties.getString("proxy.repo.base.url");
  }

  public NXCM3916SecureWebProxiedP2IT() {
    super("nxcm3916");
  }

  @Before
  public void startWebProxy() throws Exception {
    try {
      webProxyServer = lookup(ProxyServer.class);
      webProxyServer.start();
      webProxyServer.getProxyServlet().setUseAuthentication(true);
      webProxyServer.getProxyServlet().getAuthentications().put("admin", "123");
    }
    catch (Exception e) {
      throw new Exception("Current properties:\n" + TestProperties.getAll(), e);
    }

    // ensuring the proxy is working!!!
    assertThat(
        downloadFile(
            new URL(baseProxyURL + "nxcm3916/artifacts.xml"),
            "./target/downloads/nxcm3916/artifacts.xml.temp"
        ),
        exists()
    );
  }

  @After
  public void stopWebProxy()
      throws Exception
  {
    if (webProxyServer != null) {
      if (webProxyServer.getProxyServlet() != null) {
        webProxyServer.getProxyServlet().setUseAuthentication(false);
        webProxyServer.getProxyServlet().setAuthentications(null);
      }
      webProxyServer.stop();
      webProxyServer = null;
    }
  }

  @Test
  public void test()
      throws Exception
  {
    installAndVerifyP2Feature();

    assertThat(
        webProxyServer.getAccessedUris(),
        hasItem(baseProxyURL + "nxcm3916/features/com.sonatype.nexus.p2.its.feature_1.0.0.jar")
    );

    assertThat(
        webProxyServer.getAccessedUris(),
        hasItem(baseProxyURL + "nxcm3916/plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar")
    );
  }

}
