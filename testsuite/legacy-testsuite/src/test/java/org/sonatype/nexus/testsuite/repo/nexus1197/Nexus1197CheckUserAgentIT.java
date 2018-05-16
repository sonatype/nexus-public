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
package org.sonatype.nexus.testsuite.repo.nexus1197;

import java.io.FileNotFoundException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.TestProperties;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;

public class Nexus1197CheckUserAgentIT
    extends AbstractNexusIntegrationTest
{

  private static RequestHandler handler;

  private static Server server;

  public Nexus1197CheckUserAgentIT() {
    super("release-proxy-repo-1");
  }

  @BeforeClass
  public static void setUp()
      throws Exception
  {
    handler = new RequestHandler();

    server = new Server(TestProperties.getInteger("proxy.server.port"));
    server.setHandler(handler);
    server.start();
  }

  @AfterClass
  public static void tearDown()
      throws Exception
  {
    server.stop();
  }

  @Test
  public void downloadArtifactOverWebProxy()
      throws Exception
  {

    try {
      this.downloadArtifact("nexus1197", "artifact", "1.0", "pom", null, "target/downloads");
    }
    catch (FileNotFoundException e) {
      // ok, just ignore
    }

    // Nexus/1.2.0-beta-2-SNAPSHOT (OSS; Windows XP; 5.1; x86; 1.6.0_07)
    // apacheHttpClient3x/1.2.0-beta-2-SNAPSHOT Nexus/1.0
    String userAgent = handler.getUserAgent();

    Assert.assertNotNull(userAgent);
    Assert.assertTrue(userAgent.startsWith("Nexus/"));
    assertThat(userAgent, anyOf(containsString("(OSS"), containsString("(PRO")));

  }

}
