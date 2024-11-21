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
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.inject.Inject;

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.goodies.httpfixture.server.jetty.behaviour.Content;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.common.net.PortAllocator;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.testsuite.helpers.ComponentAssetTestHelper;
import org.sonatype.nexus.testsuite.testsupport.NexusBaseITSupport;
import org.sonatype.nexus.testsuite.testsupport.blobstore.restore.BlobstoreRestoreTestHelper;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.testsuite.testsupport.system.RestTestHelper.hasStatus;

public class RawRestoreBlobIT
    extends NexusBaseITSupport
{
  private static final String TEST_CONTENT = "alphabet.txt";

  private Server proxyServer;

  private Repository hostedRepository;

  private Repository proxyRepository;

  private Map<String, BlobId> hostedPathsToBlobs;

  private Map<String, BlobId> proxyPathsToBlobs;

  @Inject
  private BlobstoreRestoreTestHelper restoreTestHelper;

  @Inject
  private ComponentAssetTestHelper componentAssetTestHelper;

  private String blobStoreName;

  @Before
  public void setup() throws Exception {
    testData.addDirectory(resolveBaseFile("target/it-resources/raw"));
    blobStoreName = testName.getMethodName();
    nexus.blobStores().create(blobStoreName);
    hostedRepository = nexus.repositories()
        .raw()
        .hosted(repoName("hosted"))
        .withBlobstore(blobStoreName)
        .create();

    proxyServer = Server.withPort(PortAllocator.nextFreePort()).start();
    proxyServer.serve("/" + TEST_CONTENT).withBehaviours(resolveFile(TEST_CONTENT));

    proxyRepository = nexus.repositories()
        .raw()
        .proxy(repoName("proxy"))
        .withRemoteUrl("http://localhost:" + proxyServer.getPort() + "/")
        .withBlobstore(blobStoreName)
        .create();

    File testFile = resolveTestFile(TEST_CONTENT);
    assertThat(nexus.rest()
        .put(path(hostedRepository, TEST_CONTENT),
            FileUtils.readFileToString(testFile, StandardCharsets.UTF_8), "admin", "admin123"),
        hasStatus(HttpStatus.CREATED));

    assertThat(nexus.rest().get(path(proxyRepository, TEST_CONTENT)), hasStatus(OK));

    hostedPathsToBlobs = restoreTestHelper.getAssetToBlobIds(hostedRepository);
    proxyPathsToBlobs = restoreTestHelper.getAssetToBlobIds(proxyRepository);
  }

  @Test
  public void testMetadataRestoreWhenBothAssetsAndComponentsAreMissing() throws Exception {
    verifyMetadataRestored(restoreTestHelper::simulateComponentAndAssetMetadataLoss);
  }

  @Test
  public void testMetadataRestoreWhenOnlyAssetsAreMissing() throws Exception {
    verifyMetadataRestored(restoreTestHelper::simulateAssetMetadataLoss);
  }

  /**
   * NEXUS-40244 - if the blobstore has not been compacted it may contain multiple revisions of the same asset. As such
   * we need to ensure that the most recent revision wins.
   */
  @Test
  public void testRestoresMostRecentAsset() throws Exception {
    // We can't guarantee the order blobs will be processed, so for the test we want to create enough assets that
    // there is a low chance that the last blob is processed last which would mean our test verifies nothing.
    for (int i = 0; i < 20; i++) {
      assertThat(nexus.rest().put(path(hostedRepository, TEST_CONTENT), "test" + i, "admin", "admin123"),
          hasStatus(HttpStatus.CREATED));
    }
    hostedPathsToBlobs = restoreTestHelper.getAssetToBlobIds(hostedRepository);

    verifyMetadataRestored(restoreTestHelper::simulateAssetMetadataLoss);
  }

  /**
   * For Orient this tests restoring newdb assets, for newdb this tests restoring Orient assets
   */
  @Test
  public void testRestoreFromOtherDatabase() throws Exception {
    verifyMetadataRestored(() -> {
      restoreTestHelper.rewriteBlobNames();
      restoreTestHelper.simulateAssetMetadataLoss();
    });
  }

  @Test
  public void testDryRunRestore() {
    assertTrue(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    restoreTestHelper.simulateComponentAndAssetMetadataLoss();
    assertFalse(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    restoreTestHelper.runRestoreMetadataTaskWithTimeout(blobStoreName, 10, true);
    assertFalse(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
  }

  @Test
  public void testNotDryRunRestore() {
    assertTrue(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    restoreTestHelper.simulateComponentAndAssetMetadataLoss();
    assertFalse(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    restoreTestHelper.runRestoreMetadataTaskWithTimeout(blobStoreName, 10, false);
    assertTrue(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    verityBlobsUnchanged();
  }

  private void verifyMetadataRestored(final Runnable metadataLossSimulation) throws Exception {
    metadataLossSimulation.run();

    restoreTestHelper.runRestoreMetadataTask(blobStoreName);

    assertTrue(componentAssetTestHelper.assetExists(proxyRepository, TEST_CONTENT));
    assertTrue(componentAssetTestHelper.assetExists(hostedRepository, TEST_CONTENT));

    assertTrue(componentExists(hostedRepository, TEST_CONTENT));
    assertTrue(componentExists(proxyRepository, TEST_CONTENT));

    restoreTestHelper.assertAssetMatchesBlob(hostedRepository, TEST_CONTENT);
    restoreTestHelper.assertAssetMatchesBlob(proxyRepository, TEST_CONTENT);

    assertTrue(assetWithComponentExists(hostedRepository, TEST_CONTENT, "/", TEST_CONTENT));
    assertTrue(assetWithComponentExists(proxyRepository, TEST_CONTENT, "/", TEST_CONTENT));

    verityBlobsUnchanged();
    assertThat(nexus.rest().get(path(hostedRepository, TEST_CONTENT)).getStatus(), is(OK));
    assertThat(nexus.rest().get(path(proxyRepository, TEST_CONTENT)).getStatus(), is(OK));
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

  private boolean assetWithComponentExists(
      final Repository repository,
      final String path,
      final String group,
      final String name)
  {
    return componentAssetTestHelper.assetWithComponentExists(repository, path, group, name)
        || componentAssetTestHelper.assetWithComponentExists(hostedRepository, prependIfMissing(path, "/"), group,
            prependIfMissing(name, "/"));
  }

  private Content resolveFile(final String filename) {
    return Behaviours.file(testData.resolveFile(filename));
  }

  private String repoName(final String prefix) {
    return String.format("%s-%s", prefix, testName.getMethodName());
  }

  private String path(final Repository repository, final String path) {
    return "repository/" + repository.getName() + '/' + path;
  }
}
