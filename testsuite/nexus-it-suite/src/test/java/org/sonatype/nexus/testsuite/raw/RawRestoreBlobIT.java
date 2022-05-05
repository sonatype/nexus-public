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
import java.util.Map;

import javax.inject.Inject;

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.goodies.httpfixture.server.jetty.behaviour.Content;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.common.net.PortAllocator;
import org.sonatype.nexus.content.testsuite.groups.OrientAndSQLTestGroup;
import org.sonatype.nexus.content.testsuite.groups.OrientTestGroup;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.testsuite.helpers.ComponentAssetTestHelper;
import org.sonatype.nexus.testsuite.testsupport.blobstore.restore.BlobstoreRestoreTestHelper;
import org.sonatype.nexus.testsuite.testsupport.raw.RawClient;
import org.sonatype.nexus.testsuite.testsupport.raw.RawITSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;

@Category(OrientAndSQLTestGroup.class)
public class RawRestoreBlobIT
    extends RawITSupport
{
  private static final String TEST_CONTENT = "alphabet.txt";

  private Server proxyServer;

  private Repository hostedRepository;

  private Repository proxyRepository;

  private RawClient hostedClient;

  private RawClient proxyClient;

  private Map<String, BlobId> hostedPathsToBlobs;

  private Map<String, BlobId> proxyPathsToBlobs;

  @Inject
  private BlobstoreRestoreTestHelper restoreTestHelper;

  @Inject
  private ComponentAssetTestHelper componentAssetTestHelper;

  @Before
  public void setup() throws Exception {
    hostedRepository = repos.createRawHosted(repoName("hosted"));
    hostedClient = rawClient(hostedRepository);

    proxyServer = Server.withPort(PortAllocator.nextFreePort()).start();
    proxyServer.serve("/" + TEST_CONTENT).withBehaviours(resolveFile(TEST_CONTENT));

    proxyRepository = repos.createRawProxy(repoName("proxy"), "http://localhost:" + proxyServer.getPort() + "/");
    proxyClient = rawClient(proxyRepository);

    File testFile = resolveTestFile(TEST_CONTENT);
    assertThat(hostedClient.put(TEST_CONTENT, TEXT_PLAIN, testFile), is(HttpStatus.CREATED));
    assertThat(proxyClient.get(TEST_CONTENT).getStatusLine().getStatusCode(), is(OK));

    hostedPathsToBlobs = restoreTestHelper.getAssetToBlobIds(hostedRepository);
    proxyPathsToBlobs = restoreTestHelper.getAssetToBlobIds(proxyRepository);
  }

  @Category(OrientAndSQLTestGroup.class)
  @Test
  public void testMetadataRestoreWhenBothAssetsAndComponentsAreMissing() throws Exception {
    verifyMetadataRestored(restoreTestHelper::simulateComponentAndAssetMetadataLoss);
  }

  @Category(OrientAndSQLTestGroup.class)
  @Test
  public void testMetadataRestoreWhenOnlyAssetsAreMissing() throws Exception {
    verifyMetadataRestored(restoreTestHelper::simulateAssetMetadataLoss);
  }

  /**
   * For Orient this tests restoring newdb assets, for newdb this tests restoring Orient assets
   */
  @Category(OrientAndSQLTestGroup.class)
  @Test
  public void testRestoreFromOtherDatabase() throws Exception {
    verifyMetadataRestored(() -> {
      restoreTestHelper.rewriteBlobNames();
      restoreTestHelper.simulateAssetMetadataLoss();
    });
  }

  @Category(OrientTestGroup.class)
  @Test
  public void testMetadataRestoreWhenOnlyComponentsAreMissing() throws Exception {
    verifyMetadataRestored(restoreTestHelper::simulateComponentMetadataLoss);
  }

  @Category(OrientAndSQLTestGroup.class)
  @Test
  public void testDryRunRestore()
  {
    assertTrue(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    restoreTestHelper.simulateComponentAndAssetMetadataLoss();
    assertFalse(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    restoreTestHelper.runRestoreMetadataTaskWithTimeout(10, true);
    assertFalse(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
  }

  @Category(OrientAndSQLTestGroup.class)
  @Test
  public void testNotDryRunRestore()
  {
    assertTrue(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    restoreTestHelper.simulateComponentAndAssetMetadataLoss();
    assertFalse(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    restoreTestHelper.runRestoreMetadataTaskWithTimeout(10, false);
    assertTrue(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    verityBlobsUnchanged();
  }

  private void verifyMetadataRestored(final Runnable metadataLossSimulation) throws Exception {
    metadataLossSimulation.run();

    restoreTestHelper.runRestoreMetadataTask();

    assertTrue(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    assertTrue(componentAssetTestHelper.assetExists(hostedRepository, TEST_CONTENT));

    assertTrue(componentExists(hostedRepository, TEST_CONTENT));
    assertTrue(componentExists(proxyRepository, TEST_CONTENT));

    restoreTestHelper.assertAssetMatchesBlob(hostedRepository, TEST_CONTENT);
    restoreTestHelper.assertAssetMatchesBlob(proxyRepository, TEST_CONTENT);

    assertTrue(assetWithComponentExists(hostedRepository, TEST_CONTENT, "/", TEST_CONTENT));
    assertTrue(assetWithComponentExists(proxyRepository, TEST_CONTENT, "/", TEST_CONTENT));

    verityBlobsUnchanged();
    assertThat(hostedClient.get(TEST_CONTENT).getStatusLine().getStatusCode(), is(OK));
    assertThat(proxyClient.get(TEST_CONTENT).getStatusLine().getStatusCode(), is(OK));
  }

  /*
   * Verifies that the original blobs are attached to the assets, not copied.
   */
  private void verityBlobsUnchanged() {
    assertThat(restoreTestHelper.getAssetToBlobIds(hostedRepository), equalTo(hostedPathsToBlobs));
    assertThat(restoreTestHelper.getAssetToBlobIds(proxyRepository), equalTo(proxyPathsToBlobs));
  }

  private boolean componentExists(final Repository repository, final String name) {
    return componentAssetTestHelper.componentExists(repository, name)
        || componentAssetTestHelper.componentExists(repository, prependIfMissing(name, "/"));
  }

  private boolean assetWithComponentExists(final Repository repository, final String path, final String group, final String name) {
    return componentAssetTestHelper.assetWithComponentExists(repository, path, group, name)
        || componentAssetTestHelper.assetWithComponentExists(hostedRepository, prependIfMissing(path, "/"), group, prependIfMissing(name, "/"));
  }

  private Content resolveFile(final String filename) {
    return Behaviours.file(testData.resolveFile(filename));
  }

  private String repoName(final String prefix) {
    return String.format("%s-%s", prefix, testName.getMethodName());
  }
}
