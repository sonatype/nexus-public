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
package org.sonatype.nexus.testsuite.proxy;

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.test.http.HttpProxyServer;
import org.sonatype.nexus.test.http.HttpProxyServer.RequestResponseListener;
import org.sonatype.nexus.test.utils.TestProperties;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractNexusWebProxyIntegrationTest
    extends AbstractNexusProxyIntegrationTest
{

  protected static final int webProxyPort;

  protected HttpProxyServer httpProxyServer;

  protected List<String> accessedUris;

  static {
    webProxyPort = TestProperties.getInteger("webproxy.server.port");
  }

  @Before
  public void startWebProxy()
      throws Exception
  {
    httpProxyServer = new HttpProxyServer(
        webProxyPort,
        new RequestResponseListener()
        {
          @Override
          public void servicing(final ServletRequest req, final ServletResponse res) {
            final HttpURI uri = ((Request) req).getHttpURI();
            accessedUris.add(uri.toString());
          }
        }
    );
    httpProxyServer.start();
  }

  @After
  public void stopWebProxy()
      throws Exception
  {
    httpProxyServer.stop();
  }
}
