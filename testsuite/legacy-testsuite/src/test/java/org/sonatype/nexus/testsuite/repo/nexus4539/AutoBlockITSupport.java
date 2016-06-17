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
package org.sonatype.nexus.testsuite.repo.nexus4539;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContext;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.RemoteStatus;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.collect.Lists;
import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.eclipse.jetty.servlet.ServletHolder;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Support class for autoblock ITs
 */
public abstract class AutoBlockITSupport
    extends AbstractNexusIntegrationTest
{

  protected static final String REPO = "basic";

  protected Server server;

  protected Integer sleepTime;

  protected List<String> pathsTouched;

  protected RepositoryMessageUtil repoUtil;

  public AutoBlockITSupport() {
    super(REPO);
  }

  @SuppressWarnings("serial")
  @Before
  public void setup() throws Exception
  {
    sleepTime = -1;
    pathsTouched = Lists.newArrayList();

    server = Server.withPort(TestProperties.getInteger("webproxy-server-port"));
    server.serve("/*").withServlet(new GenericServlet()
    {
      @Override
      public void service(ServletRequest req, ServletResponse res)
          throws ServletException, IOException
      {
        pathsTouched.add(((HttpServletRequest) req).getPathInfo());
        try {
          if (sleepTime != -1) {
            Thread.sleep(sleepTime * 1000);
          }
        }
        catch (InterruptedException e) {
          HttpServletResponse resp = (HttpServletResponse) res;
          resp.sendError(Status.CLIENT_ERROR_REQUEST_TIMEOUT.getCode());
        }
      }
    });
    server.start();

    this.repoUtil = new RepositoryMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @After
  public void shutdown() throws Exception
  {
    pathsTouched = null;
    server.stop();
  }

  /**
   * Just request anything so if request is processed and error happens, it will auto block immediately
   */
  protected void shakeNexus()
      throws IOException
  {
    // don't wanna hit nexus NFC
    downloadArtifact("nexus4539", "a", "404-" + System.nanoTime());
  }

  /**
   * request status every half second until it matches expected status
   */
  protected RepositoryStatusResource waitFor(RemoteStatus status, ProxyMode mode, boolean force)
      throws Exception
  {
    repoUtil.getStatus(REPO, force);
    RepositoryStatusResource s = null;
    for (int i = 0; i < 1000; i++) {
      s = repoUtil.getStatus(REPO, false);
      log.debug("Waiting for: " + status + "," + mode + " - " + getJsonXStream().toXML(s));
      if (status.name().equals(s.getRemoteStatus()) && mode.name().equals(s.getProxyMode())) {
        return s;
      }
      Thread.sleep(1500);
    }

    assertRepositoryStatus(s, status, mode);

    throw new IllegalStateException();
  }

  /**
   * Set Nexus repository status to {@link ProxyMode#BLOCKED_MANUAL} and ensure the mode is set.
   *
   * @throws Exception re-thrown
   */
  protected void manualBlockNexus()
      throws Exception
  {
    repoUtil.setBlockProxy(REPO, true);
    assertRepositoryProxyMode(repoUtil.getStatus(REPO), ProxyMode.BLOCKED_MANUAL);
  }

  /**
   * Set Nexus repository status to {@link ProxyMode#ALLOW} and ensure the mode is set.
   *
   * @throws Exception re-thrown
   */
  protected void manualUnblockNexus()
      throws Exception
  {
    repoUtil.setBlockProxy(REPO, false);
    assertRepositoryProxyMode(repoUtil.getStatus(REPO), ProxyMode.ALLOW);
  }

  /**
   * Force Nexus into {@link ProxyMode#BLOCKED_AUTO} mode by not responding to its requests to proxy repository.
   *
   * @throws Exception re-thrown
   */
  protected void autoBlockNexus()
      throws Exception
  {
    // let's stall the response
    sleepTime = 100;
    shakeNexus();
    waitFor(RemoteStatus.UNAVAILABLE, ProxyMode.BLOCKED_AUTO, false);
    // ensure nexus did touch server
    assertThat(pathsTouched, not(Matchers.<String>empty()));
    pathsTouched.clear();
  }

  /**
   * Force Nexus to get out from {@link ProxyMode#BLOCKED_AUTO} mode.
   *
   * @throws Exception re-thrown
   */
  protected void autoUnblockNexus()
      throws Exception
  {
    sleepTime = -1;
    waitFor(RemoteStatus.AVAILABLE, ProxyMode.ALLOW, true);
    // ensure nexus did touch server
    assertThat(pathsTouched, not(Matchers.<String>empty()));
    pathsTouched.clear();
  }

  /**
   * Assert repository remote status and proxy mode.
   */
  protected void assertRepositoryStatus(final RepositoryStatusResource status,
                                        final RemoteStatus remoteStatus,
                                        final ProxyMode mode)
  {
    assertRepositoryProxyMode(status, mode);
    assertThat(status.getRemoteStatus(), equalTo(remoteStatus.toString()));
  }

  /**
   * Assert repository remote and proxy mode status.
   */
  protected void assertRepositoryProxyMode(final RepositoryStatusResource status,
                                           final ProxyMode mode)
  {
    assertThat(status, notNullValue());
    assertThat(status.getProxyMode(), equalTo(mode.toString()));
  }

  /**
   * Download specified artifact, not failing if not found.
   *
   * @throws IOException re-thrown if not an {@link FileNotFoundException}
   */
  protected void downloadArtifact(final String groupId, final String artifactId, final String version)
      throws IOException
  {
    final Gav gav = GavUtil.newGav(groupId, artifactId, version);
    try {
      downloadArtifactFromRepository(REPO, gav, "target/downloads/" + getTestId());
    }
    catch (FileNotFoundException e) {
      // ignore just fine
      // e.printStackTrace();
    }
  }

  @BeforeClass
  public static void fixAutoblockTime() {
    // NEXUS-4539 - to get test faster reduced autoblock check from 5 minutes to 30 seconds
    System.setProperty("plexus.autoblock.remote.status.retain.time", String.valueOf(30 * 1000));
  }

  @AfterClass
  public static void restoreAutoblockTime() {
    System.clearProperty("plexus.autoblock.remote.status.retain.time");
  }

}
