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
package org.sonatype.nexus.testsuite.proxy.nexus5291;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.test.utils.NexusRequestMatchers;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.sisu.goodies.common.Time;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Status;

import static org.sonatype.nexus.integrationtests.RequestFacade.doGet;

/**
 * NEXUS-5291: When nexus transport connection pool (using HC4x RRS) gets depleted, Nexus should respond with transient
 * "503 Service Unavailable".
 *
 * @author cstamas
 */
public class Nexus5291OverloadResponseIs503IT
    extends AbstractNexusProxyIntegrationTest
{
  protected Server server;

  @BeforeClass
  public static void setHc4Parameters() {
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolMaxSize", "1");
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolSize", "1");
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolTimeout", "50");
  }

  @AfterClass
  public static void unsetHc4Parameters() {
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolMaxSize");
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolSize");
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolTimeout");
  }

  @Before
  @Override
  public void startProxy()
      throws Exception
  {
    server = Server
        .withPort(TestProperties.getInteger("proxy-repo-port"))
        .serve("/").withBehaviours(Behaviours.pause(Time.days(1))).start();
  }

  @After
  @Override
  public void stopProxy()
      throws Exception
  {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void overloadResultsin503Code()
      throws Exception
  {
    final Thread stalledRequest = new Thread()
    {
      @Override
      public void run() {
        try {
          // this will wait as long timeout occurs, and will get 404 eventually
          doGet(
              "content/repositories/" + getTestRepositoryId() + "/nexus5291/artifact/1.0/artifact-1.0.jar",
              NexusRequestMatchers.respondsWithStatus(Status.CLIENT_ERROR_NOT_FOUND));
        }
        catch (Exception e) {
          // barf, this will fail but we do not care
          // it will either timeout on client side (IT), or on nexus side (connection timeout)
          // and will result in 404.
        }
      }
    };
    stalledRequest.start();
    try {
      Thread.sleep(200);

      // expect 503 (as pool is depleted) when going directly to proxy repository
      // watch for different path, to avoid UID locking that would serialize the above "stalled" request and this
      doGet("content/repositories/" + getTestRepositoryId() + "/nexus5291/artifact/1.1/artifact-1.1.jar",
          NexusRequestMatchers.respondsWithStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE));

      // expect 404 (as pool is depleted) when going over a group, as group aggregates (hosted + proxies)
      // watch for different path, to avoid UID locking that would serialize the above "stalled" request and this
      doGet("content/groups/public/nexus5291/artifact/1.1/artifact-1.1.jar",
          NexusRequestMatchers.respondsWithStatus(Status.CLIENT_ERROR_NOT_FOUND));
    }
    finally {
      stalledRequest.join();
    }
  }
}
