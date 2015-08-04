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
package org.sonatype.nexus.testsuite.routing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.client.core.subsystem.routing.Status;
import org.sonatype.nexus.client.core.subsystem.routing.Status.Outcome;
import org.sonatype.nexus.testsuite.client.Caches;
import org.sonatype.nexus.testsuite.client.Scheduler;
import org.sonatype.sisu.litmus.testsupport.group.Smoke;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;
import org.sonatype.tests.http.server.jetty.behaviour.PathRecorderBehaviour;

import com.google.common.io.Files;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;

/**
 * Simple "smoke" IT that tests does proxy404 does it's job or not, by detecting what remote requests was made with and
 * without WL built.
 *
 * @author cstamas
 */
@Category(Smoke.class)
public class RoutingFilteringIT
    extends RoutingITSupport
{
  private static final String SOMEORG_ARTIFACT_POM_PATTERN = "/%s/someorg/artifact/%s/artifact-%s.%s";

  private static final String COM_SOMEORG_ARTIFACT_10_POM = String.format(SOMEORG_ARTIFACT_POM_PATTERN, "com",
      "1.0", "1.0", "pom");

  private static final String COM_SOMEORG_ARTIFACT_10_JAR = String.format(SOMEORG_ARTIFACT_POM_PATTERN, "com",
      "1.0", "1.0", "jar");

  private static final String ORG_SOMEORG_ARTIFACT_10_POM = String.format(SOMEORG_ARTIFACT_POM_PATTERN, "org",
      "1.0", "1.0", "pom");

  private static final String ORG_SOMEORG_ARTIFACT_10_JAR = String.format(SOMEORG_ARTIFACT_POM_PATTERN, "org",
      "1.0", "1.0", "jar");

  private static final String FLUKE_ARTIFACT_POM = "/hu/fluke/artifact/1.0/artifact-1.0.pom";

  private static final String FLUKE_ARTIFACT_JAR = "/hu/fluke/artifact/1.0/artifact-1.0.jar";

  /**
   * Constructor.
   */
  public RoutingFilteringIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  protected void fetchAndAssert(final File downloadsDir, final String proxyRepositoryId, final String path,
                                final boolean shouldBeServed)
      throws IOException
  {
    try {
      content().download(repositoryLocation(proxyRepositoryId, path),
          new File(downloadsDir, "aopalliance-1.0.jar"));
      if (!shouldBeServed) {
        assertThat("Should not be able to download", false);
      }
    }
    catch (NexusClientNotFoundException e) {
      if (shouldBeServed) {
        assertThat("Should be able to download", false);
      }
    }
  }

  protected void nukeProxyCaches(final String proxyRepositoryId)
      throws Exception
  {
    // NFC
    client().getSubsystem(Caches.class).expireCaches(proxyRepositoryId);
    // nuke the repo cache
    try {
      content().delete(repositoryLocation(proxyRepositoryId, "/"));
    }
    catch (NexusClientNotFoundException e) {
      // ignore
    }
    // wait for things to calm down (expireCaches happens in bg task)
    client().getSubsystem(Scheduler.class).waitForAllTasksToStop();
  }

  // ==

  /**
   * A proxy "transitions" from not having prefixes file to having prefixes file (and not being scraped either). The
   * test does two passes of requests. In first pass (without WL), the proxy will forward all the requests to it's
   * remote target (pre-WL behaviour of Nexus). Once repository transitions into a state of having WL, with nuked
   * proxy caches same set of requests is repeated. This time, it is validated that only allowed requests are
   * forwared to remote target.
   */
  @Test
  public void proxyWithoutAndWithWL()
      throws Exception
  {
    // where to put downloaded things
    final File remoteRepoRoot = testData().resolveFile("remote-repo");
    final File downloadsDir = testIndex().getDirectory("downloads");

    // bring up remote server using Jetty
    final PathRecorderBehaviour recorder = new PathRecorderBehaviour();
    final PrefixesFile prefixesFile = new PrefixesFile();
    prefixesFile.setContent(null);

    final Server server = Server
        .withPort(0)
        .serve("/*")
        .withBehaviours(
            recorder,
            prefixesFile,
            Behaviours.get(remoteRepoRoot)
        )
        .start();

    // create the proxy
    final MavenProxyRepository proxyRepository = repositories()
        .create(MavenProxyRepository.class, repositoryIdForTest("someorgProxy1"))
        .asProxyOf(server.getUrl().toExternalForm())
        .doNotDownloadRemoteIndexes()
        .doNotAutoBlock()
        .save();

    routingTest().waitForAllRoutingUpdateJobToStop();
    // waitForWLPublishingOutcomes( proxyRepository.id() );
    client().getSubsystem(Scheduler.class).waitForAllTasksToStop();

    // nuke the repo cache
    nukeProxyCaches(proxyRepository.id());

    try {
      // clear recorder
      recorder.clear();
      // remote repo lives without prefix file
      {
        // check that newly added proxy is not publishing prefix file
        assertThat(routing().getStatus(proxyRepository.id()).getPublishedStatus(),
            equalTo(Outcome.FAILED));

        // and because no WL, we can fetch whatever we want (com and org)
        // all these will go remotely
        fetchAndAssert(downloadsDir, proxyRepository.id(), COM_SOMEORG_ARTIFACT_10_POM, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), COM_SOMEORG_ARTIFACT_10_JAR, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), ORG_SOMEORG_ARTIFACT_10_POM, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), ORG_SOMEORG_ARTIFACT_10_JAR, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), FLUKE_ARTIFACT_POM, false);
        fetchAndAssert(downloadsDir, proxyRepository.id(), FLUKE_ARTIFACT_JAR, false);

        // note: sha1 is asked for existing files only
        // GET /hu/fluke/artifact/1.0/artifact-1.0.jar,
        // GET /hu/fluke/artifact/1.0/artifact-1.0.pom,
        // GET /org/someorg/artifact/1.0/artifact-1.0.jar.sha1,
        // GET /org/someorg/artifact/1.0/artifact-1.0.jar,
        // GET /org/someorg/artifact/1.0/artifact-1.0.pom.sha1,
        // GET /org/someorg/artifact/1.0/artifact-1.0.pom,
        // GET /com/someorg/artifact/1.0/artifact-1.0.jar.sha1,
        // GET /com/someorg/artifact/1.0/artifact-1.0.jar,
        // GET /com/someorg/artifact/1.0/artifact-1.0.pom.sha1,
        // GET /com/someorg/artifact/1.0/artifact-1.0.pom,
        final List<String> requests = recorder.getPathsForVerb("GET");
        assertThat(requests.size(), is(10));
        assertThat(
            requests,
            containsInAnyOrder(COM_SOMEORG_ARTIFACT_10_POM, COM_SOMEORG_ARTIFACT_10_POM + ".sha1",
                COM_SOMEORG_ARTIFACT_10_JAR, COM_SOMEORG_ARTIFACT_10_JAR + ".sha1",
                ORG_SOMEORG_ARTIFACT_10_POM, ORG_SOMEORG_ARTIFACT_10_POM + ".sha1",
                ORG_SOMEORG_ARTIFACT_10_JAR, ORG_SOMEORG_ARTIFACT_10_JAR + ".sha1", FLUKE_ARTIFACT_POM,
                FLUKE_ARTIFACT_JAR));
      }

      // nuke the repo cache
      nukeProxyCaches(proxyRepository.id());

      // now set the prefixes file that contains /org/someorg prefix only, and repeat
      prefixesFile.setContent(Files.toString(testData().resolveFile("someorg-prefixes.txt"),
          Charset.forName("UTF-8")));

      // update the WL of proxy repo to have new prefixes file picked up
      routing().updatePrefixFile(proxyRepository.id());

      // wait for update to finish since it's async op, client above returned immediately
      // but update happens in a separate thread. Still this should be quick operation as prefix file is used
      Status proxyStatus = routing().getStatus(proxyRepository.id());
      // sit and wait for remote discovery (or the timeout Junit @Rule will kill us)
      while (proxyStatus.getPublishedStatus() != Outcome.SUCCEEDED) {
        Thread.sleep(1000);
        proxyStatus = routing().getStatus(proxyRepository.id());
      }

      // clear recorder
      recorder.clear();
      // repeat the test with slightly different expectations
      {
        // check that newly added proxy is publishing prefix file
        assertThat(routing().getStatus(proxyRepository.id()).getPublishedStatus(),
            equalTo(Outcome.SUCCEEDED));

        // nuke the repo cache
        nukeProxyCaches(proxyRepository.id());
        // and because we have WL, we cant fetch whatever we want (com and org)
        // only WL-enlisted of these will go remotely
        fetchAndAssert(downloadsDir, proxyRepository.id(), COM_SOMEORG_ARTIFACT_10_POM, false);
        fetchAndAssert(downloadsDir, proxyRepository.id(), COM_SOMEORG_ARTIFACT_10_JAR, false);
        fetchAndAssert(downloadsDir, proxyRepository.id(), ORG_SOMEORG_ARTIFACT_10_POM, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), ORG_SOMEORG_ARTIFACT_10_JAR, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), FLUKE_ARTIFACT_POM, false);
        fetchAndAssert(downloadsDir, proxyRepository.id(), FLUKE_ARTIFACT_JAR, false);

        // GET /org/someorg/artifact/1.0/artifact-1.0.jar.sha1,
        // GET /org/someorg/artifact/1.0/artifact-1.0.jar,
        // GET /org/someorg/artifact/1.0/artifact-1.0.pom.sha1,
        // GET /org/someorg/artifact/1.0/artifact-1.0.pom,
        final List<String> requests = recorder.getPathsForVerb("GET");
        assertThat(requests.toString(), requests.size(), is(4));
        assertThat(
            requests,
            containsInAnyOrder(ORG_SOMEORG_ARTIFACT_10_POM, ORG_SOMEORG_ARTIFACT_10_POM + ".sha1",
                ORG_SOMEORG_ARTIFACT_10_JAR, ORG_SOMEORG_ARTIFACT_10_JAR + ".sha1"));
      }
    }
    finally {
      server.stop();
    }
  }

  /**
   * A proxy "transitions" from having prefixes file to not having prefixes file (and not being scraped either). The
   * test does two passes of requests. In first pass (with WL), the proxy will forward only allowed requests to
   * it's remote target. Then repository transitions into a state of not having WL (like remote prefix file removed
   * and will not be scraped as test Jetty does not have index file), with nuked proxy caches same set of requests is
   * repeated. This time, it is validated that all requests are forwared to remote target (pre-WL behaviour of
   * Nexus).
   * Simply put, proxy repository falls back to pre-WL (pre-2.4) behavior.
   */
  @Test
  public void proxyWithAndWithoutWL()
      throws Exception
  {
    // where to put downloaded things
    final File remoteRepoRoot = testData().resolveFile("remote-repo");
    final File downloadsDir = testIndex().getDirectory("downloads");

    // bring up remote server using Jetty
    final PathRecorderBehaviour recorder = new PathRecorderBehaviour();
    final PrefixesFile prefixesFile = new PrefixesFile();
    // now set the prefixes file that contains /org/someorg prefix only, and repeat
    prefixesFile.setContent(Files.toString(testData().resolveFile("someorg-prefixes.txt"),
        Charset.forName("UTF-8")));
    final Server server = Server
        .withPort(0)
        .serve("/*")
        .withBehaviours(
            recorder,
            prefixesFile,
            Behaviours.get(remoteRepoRoot)
        )
        .start();

    // create the proxy
    final MavenProxyRepository proxyRepository =
        repositories().create(MavenProxyRepository.class, repositoryIdForTest("someorgProxy1"))
            .asProxyOf(server.getUrl().toExternalForm())
            .doNotDownloadRemoteIndexes()
            .doNotAutoBlock()
            .save();

    routingTest().waitForAllRoutingUpdateJobToStop();
    // waitForWLPublishingOutcomes( proxyRepository.id() );
    client().getSubsystem(Scheduler.class).waitForAllTasksToStop();

    try {
      // clear recorder
      recorder.clear();
      // repeat the test with slightly different expectations
      {
        // check that newly added proxy is publishing prefix file
        assertThat(routing().getStatus(proxyRepository.id()).getPublishedStatus(),
            equalTo(Outcome.SUCCEEDED));

        // nuke the repo cache
        nukeProxyCaches(proxyRepository.id());
        // and because we have WL, we cant fetch whatever we want (com and org)
        // only WL-enlisted of these will go remotely
        fetchAndAssert(downloadsDir, proxyRepository.id(), COM_SOMEORG_ARTIFACT_10_POM, false);
        fetchAndAssert(downloadsDir, proxyRepository.id(), COM_SOMEORG_ARTIFACT_10_JAR, false);
        fetchAndAssert(downloadsDir, proxyRepository.id(), ORG_SOMEORG_ARTIFACT_10_POM, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), ORG_SOMEORG_ARTIFACT_10_JAR, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), FLUKE_ARTIFACT_POM, false);
        fetchAndAssert(downloadsDir, proxyRepository.id(), FLUKE_ARTIFACT_JAR, false);

        // GET /org/someorg/artifact/1.0/artifact-1.0.jar.sha1,
        // GET /org/someorg/artifact/1.0/artifact-1.0.jar,
        // GET /org/someorg/artifact/1.0/artifact-1.0.pom.sha1,
        // GET /org/someorg/artifact/1.0/artifact-1.0.pom,
        final List<String> requests = recorder.getPathsForVerb("GET");
        assertThat(requests.toString(), requests.size(), is(4));
        assertThat(
            requests,
            containsInAnyOrder(ORG_SOMEORG_ARTIFACT_10_POM, ORG_SOMEORG_ARTIFACT_10_POM + ".sha1",
                ORG_SOMEORG_ARTIFACT_10_JAR, ORG_SOMEORG_ARTIFACT_10_JAR + ".sha1"));
      }

      // now loose the prefixes file
      prefixesFile.setContent(null);

      // update the WL of proxy repo to have new prefixes file picked up
      routing().updatePrefixFile(proxyRepository.id());

      // wait for update to finish since it's async op, client above returned immediately
      // but update happens in a separate thread. Still this should be quick operation as prefix file is used
      Status proxyStatus = routing().getStatus(proxyRepository.id());
      // sit and wait for remote discovery (or the timeout Junit @Rule will kill us)
      while (proxyStatus.getPublishedStatus() != Outcome.FAILED) {
        Thread.sleep(1000);
        proxyStatus = routing().getStatus(proxyRepository.id());
      }

      // clear recorder
      recorder.clear();
      // remote repo lives without prefix file
      {
        // check that newly added proxy is not publishing prefix file
        assertThat(routing().getStatus(proxyRepository.id()).getPublishedStatus(),
            equalTo(Outcome.FAILED));

        // nuke the repo cache
        nukeProxyCaches(proxyRepository.id());
        // and because no WL, we can fetch whatever we want (com and org)
        // all these will go remotely
        fetchAndAssert(downloadsDir, proxyRepository.id(), COM_SOMEORG_ARTIFACT_10_POM, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), COM_SOMEORG_ARTIFACT_10_JAR, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), ORG_SOMEORG_ARTIFACT_10_POM, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), ORG_SOMEORG_ARTIFACT_10_JAR, true);
        fetchAndAssert(downloadsDir, proxyRepository.id(), FLUKE_ARTIFACT_POM, false);
        fetchAndAssert(downloadsDir, proxyRepository.id(), FLUKE_ARTIFACT_JAR, false);

        // note: sha1 is asked for existing files only
        // GET /hu/fluke/artifact/1.0/artifact-1.0.jar,
        // GET /hu/fluke/artifact/1.0/artifact-1.0.pom,
        // GET /org/someorg/artifact/1.0/artifact-1.0.jar.sha1,
        // GET /org/someorg/artifact/1.0/artifact-1.0.jar,
        // GET /org/someorg/artifact/1.0/artifact-1.0.pom.sha1,
        // GET /org/someorg/artifact/1.0/artifact-1.0.pom,
        // GET /com/someorg/artifact/1.0/artifact-1.0.jar.sha1,
        // GET /com/someorg/artifact/1.0/artifact-1.0.jar,
        // GET /com/someorg/artifact/1.0/artifact-1.0.pom.sha1,
        // GET /com/someorg/artifact/1.0/artifact-1.0.pom,
        final List<String> requests = recorder.getPathsForVerb("GET");
        assertThat(requests.toString(), requests.size(), is(10));
        assertThat(
            requests,
            containsInAnyOrder(COM_SOMEORG_ARTIFACT_10_POM, COM_SOMEORG_ARTIFACT_10_POM + ".sha1",
                COM_SOMEORG_ARTIFACT_10_JAR, COM_SOMEORG_ARTIFACT_10_JAR + ".sha1",
                ORG_SOMEORG_ARTIFACT_10_POM, ORG_SOMEORG_ARTIFACT_10_POM + ".sha1",
                ORG_SOMEORG_ARTIFACT_10_JAR, ORG_SOMEORG_ARTIFACT_10_JAR + ".sha1", FLUKE_ARTIFACT_POM,
                FLUKE_ARTIFACT_JAR));
      }
    }
    finally {
      server.stop();
    }
  }

  // ==

  private static class PrefixesFile
      implements Behaviour
  {
    private String content;

    public void setContent(final String content) {
      this.content = content;
    }

    @Override
    public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
        throws Exception
    {
      if (!request.getPathInfo().equals("/.meta/prefixes.txt")) {
        return true;
      }
      if (content != null) {
        response.setStatus(200);
        response.setContentType("text/plain");
        final byte[] body = content.getBytes("UTF-8");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
      }
      else {
        response.sendError(404);
      }
      return false;
    }
  }
}
