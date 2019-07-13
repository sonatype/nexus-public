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
package org.sonatype.nexus.testsuite.raw;

import java.io.File;

import javax.inject.Inject;

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.goodies.httpfixture.server.jetty.behaviour.Content;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.testsuite.testsupport.blobstore.restore.BlobstoreRestoreTestHelper;
import org.sonatype.nexus.testsuite.testsupport.raw.RawClient;
import org.sonatype.nexus.testsuite.testsupport.raw.RawITSupport;

import org.junit.Before;
import org.junit.Test;

import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RawRestoreBlobIT
    extends RawITSupport
{
  private static final String HOSTED_REPO_NAME = "raw-hosted";

  private static final String PROXY_REPO_NAME = "raw-proxy";

  private static final String TEST_CONTENT = "alphabet.txt";

  private Server proxyServer;

  private Repository hostedRepository;

  private Repository proxyRepository;

  private RawClient hostedClient;

  private RawClient proxyClient;

  @Inject
  private BlobstoreRestoreTestHelper testHelper;

  @Before
  public void setup() throws Exception {
    hostedRepository = repos.createRawHosted(HOSTED_REPO_NAME);
    hostedClient = rawClient(hostedRepository);

    proxyServer = Server.withPort(0).start();
    proxyServer.serve("/" + TEST_CONTENT).withBehaviours(resolveFile(TEST_CONTENT));

    proxyRepository = repos.createRawProxy(PROXY_REPO_NAME, "http://localhost:" + proxyServer.getPort() + "/");
    proxyClient = rawClient(proxyRepository);

    File testFile = resolveTestFile(TEST_CONTENT);
    assertThat(hostedClient.put(TEST_CONTENT, TEXT_PLAIN, testFile), is(HttpStatus.CREATED));
    assertThat(proxyClient.get(TEST_CONTENT).getStatusLine().getStatusCode(), is(HttpStatus.OK));
  }

  @Test
  public void testMetadataRestoreWhenBothAssetsAndComponentsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateComponentAndAssetMetadataLoss);
  }

  @Test
  public void testMetadataRestoreWhenOnlyAssetsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateAssetMetadataLoss);
  }

  @Test
  public void testMetadataRestoreWhenOnlyComponentsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateComponentMetadataLoss);
  }

  private void verifyMetadataRestored(final Runnable metadataLossSimulation) throws Exception {
    metadataLossSimulation.run();

    testHelper.runRestoreMetadataTask();

    testHelper.assertComponentInRepository(hostedRepository, TEST_CONTENT);
    testHelper.assertComponentInRepository(proxyRepository, TEST_CONTENT);

    testHelper.assertAssetMatchesBlob(hostedRepository, TEST_CONTENT);
    testHelper.assertAssetMatchesBlob(proxyRepository, TEST_CONTENT);

    testHelper.assertAssetAssociatedWithComponent(hostedRepository, TEST_CONTENT, TEST_CONTENT);
    testHelper.assertAssetAssociatedWithComponent(proxyRepository, TEST_CONTENT, TEST_CONTENT);

    assertThat(hostedClient.get(TEST_CONTENT).getStatusLine().getStatusCode(), is(HttpStatus.OK));
    assertThat(proxyClient.get(TEST_CONTENT).getStatusLine().getStatusCode(), is(HttpStatus.OK));
  }

  private Content resolveFile(final String filename) {
    return Behaviours.file(testData.resolveFile(filename));
  }
}
