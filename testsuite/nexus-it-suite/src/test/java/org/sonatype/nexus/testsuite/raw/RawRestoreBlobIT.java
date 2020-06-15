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
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.goodies.httpfixture.server.jetty.behaviour.Content;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.content.testsuite.groups.OrientTestGroup;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.testsuite.testsupport.blobstore.restore.BlobstoreRestoreTestHelper;
import org.sonatype.nexus.testsuite.testsupport.raw.RawClient;
import org.sonatype.nexus.testsuite.testsupport.raw.RawITSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.repository.storage.Bucket.REPO_NAME_HEADER;

@Category(OrientTestGroup.class)
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

  @Inject
  @Named("raw")
  private RestoreBlobStrategy restoreBlobStrategy;

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

  @Test
  public void testNotDryRunRestore()
  {
    runBlobRestore(false);
    testHelper.assertAssetNotInRepository(proxyRepository, TEST_CONTENT);
  }

  @Test
  public void testDryRunRestore()
  {
    runBlobRestore(true);
    testHelper.assertAssetInRepository(proxyRepository, TEST_CONTENT);
  }

  private void runBlobRestore(final boolean isDryRun) {
    Asset asset;
    Blob blob;
    try (StorageTx tx = getStorageTx(proxyRepository)) {
      tx.begin();
      asset = tx.findAssetWithProperty(AssetEntityAdapter.P_NAME, TEST_CONTENT, tx.findBucket(proxyRepository));
      blob = tx.getBlob(asset.blobRef());
    }
    testHelper.simulateComponentMetadataLoss();
    Properties properties = new Properties();
    properties.setProperty(HEADER_PREFIX + REPO_NAME_HEADER, proxyRepository.getName());
    properties.setProperty(HEADER_PREFIX + BLOB_NAME_HEADER, asset.name());

    restoreBlobStrategy.restore(properties, blob, HEADER_PREFIX + BLOB_NAME_HEADER, isDryRun);
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

  private static StorageTx getStorageTx(final Repository repository) {
    return repository.facet(StorageFacet.class).txSupplier().get();
  }
}
