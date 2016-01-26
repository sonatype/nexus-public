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
package org.sonatype.nexus.testsuite.ruby;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.ruby.client.RubyProxyRepository;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;
import org.sonatype.tests.http.server.jetty.behaviour.Content;
import org.sonatype.tests.http.server.jetty.behaviour.Record;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.fail;

public class ProxiedBundleIT
    extends BundleITSupport
{
  private static final String REMOTE_REPO_ID = "gemsremoteproxy";

  private Content emptyMarshalledList = Behaviours.content(
      new byte[] { 0x04, 0x08, 0x5B, 0x00 }, "application/octet-stream");

  private Record proxyRecord = Behaviours.record();

  private Server proxyServer;

  @Before
  public void initProxyServer() throws Exception {
    proxyServer = Server.withPort(0)
        .serve("/gems/does-not-exist-123.gem").withBehaviours(proxyRecord, Behaviours.error(404))
        .serve("/api/v1/dependencies").withBehaviours(proxyRecord, emptyMarshalledList)
        .serve("/*").withBehaviours(proxyRecord)
        .start();

    createRubyProxyRepository(REMOTE_REPO_ID, proxyServer.getUrl().toString());
  }

  @After
  public void stopProxyServer() throws Exception {
    if (proxyServer != null) {
      proxyServer.stop();
      proxyServer = null;
    }

    repositories().get(REMOTE_REPO_ID).remove();
  }

  public ProxiedBundleIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates, "gemsproxy");
  }

  @Override
  @Test
  public void checkUpdateOfMissingDependencies() throws IOException {
    super.checkUpdateOfMissingDependencies();
  }

  @Test
  public void exerciseProxyCache() throws Exception {

    // expect single GET of remote gem
    proxyRecord.clear();
    requestExistingGem();
    requestExistingGem();
    requestExistingGem();
    assertThat(proxyRecord.getRequests(), hasSize(1));

    content().delete(new Location(REMOTE_REPO_ID, "gems/d/does-exist-321.gem"));

    // expect single GET to replace deleted gem
    proxyRecord.clear();
    requestExistingGem();
    requestExistingGem();
    requestExistingGem();
    assertThat(proxyRecord.getRequests(), hasSize(1));

    scheduler().run("ExpireCacheTask", null);
    scheduler().waitForAllTasksToStop(); // wait for it

    // expect HEAD+GET to re-fetch expired gem
    proxyRecord.clear();
    requestExistingGem();
    requestExistingGem();
    requestExistingGem();
    assertThat(proxyRecord.getRequests(), hasSize(2));
  }

  @Test
  public void exerciseNFC() throws Exception {

    // expect multiple GETs with NFC disabled
    proxyRecord.clear();
    repositories().get(RubyProxyRepository.class, REMOTE_REPO_ID).withNotFoundCacheTTL(0).save();
    requestNoneExistingGem();
    requestNoneExistingGem();
    requestNoneExistingGem();
    assertThat(proxyRecord.getRequests(), hasSize(3));

    // expect single GET with NFC enabled
    proxyRecord.clear();
    repositories().get(RubyProxyRepository.class, REMOTE_REPO_ID).withNotFoundCacheTTL(1000).save();
    requestNoneExistingGem();
    requestNoneExistingGem();
    requestNoneExistingGem();
    assertThat(proxyRecord.getRequests(), hasSize(1));
  }

  @Test
  public void exerciseDependencyCaching() throws Exception {

    // expect single GET of remote dependencies list
    proxyRecord.clear();
    requestGemDependencies();
    requestGemDependencies();
    requestGemDependencies();
    assertThat(proxyRecord.getRequests(), hasSize(1));

    scheduler().run("ExpireCacheTask", null);
    scheduler().waitForAllTasksToStop(); // wait for it

    // expect single GET of expired dependencies list
    proxyRecord.clear();
    requestGemDependencies();
    requestGemDependencies();
    requestGemDependencies();
    assertThat(proxyRecord.getRequests(), hasSize(1));
  }

  private void requestGemDependencies() throws IOException {
    File download = new File(util.createTempDir(), "null");
    content().download(new Location(REMOTE_REPO_ID, "/api/v1/dependencies?gems=foo,bar"), download);
    download.deleteOnExit();
  }

  private void requestExistingGem() throws IOException {
    File download = new File(util.createTempDir(), "null");
    content().download(new Location(REMOTE_REPO_ID, "/gems/does-exist-321.gem"), download);
    download.deleteOnExit();
  }

  private void requestNoneExistingGem() throws IOException {
    File download = new File(util.createTempDir(), "null");
    try {
      content().download(new Location(REMOTE_REPO_ID, "/gems/does-not-exist-123.gem"), download);
      fail("Expected NexusClientNotFoundException");
    }
    catch (NexusClientNotFoundException ignore) {
      // expected since gem doesn't exist
    }
    download.deleteOnExit();
  }
}