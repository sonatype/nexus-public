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
package org.sonatype.nexus.blobstore.restore.orient;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.hash.HashCode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.restore.orient.DefaultOrientIntegrityCheckStrategy.*;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_BLOB_REF;

public class DefaultOrientIntegrityCheckStrategyTest
    extends TestSupport
{
  private static final HashCode TEST_HASH1 = HashCode.fromString("aa");

  private static final HashCode TEST_HASH2 = HashCode.fromString("bb");

  private static final Supplier<Boolean> NO_CANCEL = () -> false;

  private static boolean checkFailed = false;

  private static final Consumer<Asset> CHECK_FAILED_HANDLER = (asset) -> checkFailed = true;

  @Mock
  private Repository repository;

  @Mock
  private StorageTx storageTx;

  @Mock
  private BlobStore blobStore;

  @Mock
  private Bucket bucket;

  @Mock
  private Logger logger;

  private DefaultOrientIntegrityCheckStrategy orientDefaultIntegrityCheckStrategy;

  private Set<Asset> assets;

  @Before
  public void setup() throws Exception {
    orientDefaultIntegrityCheckStrategy = spy(new TestOrientIntegrityCheckFacet());

    checkFailed = false;

    assets = new HashSet<>();

    BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class);
    when(blobStoreConfiguration.getName()).thenReturn("reponame");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);

    StorageFacet storageFacet = mock(StorageFacet.class);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);

    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.browseAssets(bucket)).thenReturn(assets);
  }

  @Test
  public void testNoBlobAttributes() {
    Asset asset = getMockAsset("name", TEST_HASH1);
    assets.add(asset);

    // stub attribute load to fail
    when(blobStore.getBlobAttributes(new BlobId("blob"))).thenReturn(null);

    orientDefaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, CHECK_FAILED_HANDLER);

    verify(orientDefaultIntegrityCheckStrategy, never()).checkAsset(any(), any());
    verify(logger).error(BLOB_PROPERTIES_MISSING_FOR_ASSET, asset.name());

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testBlobDeleted() {
    Asset asset = getMockAsset("name", TEST_HASH1);
    assets.add(asset);

    BlobAttributes blobAttributes = getMockBlobAttribues("name", "sha1", true);
    when(blobStore.getBlobAttributes(new BlobId("blob"))).thenReturn(blobAttributes);

    orientDefaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, CHECK_FAILED_HANDLER);

    verify(orientDefaultIntegrityCheckStrategy, never()).checkAsset(any(), any());
    verify(logger).warn(BLOB_PROPERTIES_MARKED_AS_DELETED, asset.name());

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testNullBlobRef() {
    Asset asset = getMockAsset("name", TEST_HASH1);
    assets.add(asset);

    // requireBlobRef throws an IllegalStateException on the require
    IllegalStateException ex = new IllegalStateException(format("Missing property: %s", P_BLOB_REF));
    when(asset.requireBlobRef()).thenThrow(ex);

    orientDefaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, CHECK_FAILED_HANDLER);

    verify(orientDefaultIntegrityCheckStrategy, never()).checkAsset(any(), any());
    verify(logger).error(ERROR_ACCESSING_BLOB, asset.toString(), ex.getMessage(), null);

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testUnexpectedException() {
    Asset asset = getMockAsset("name", TEST_HASH1);
    assets.add(asset);

    // throw an unexpected error
    NullPointerException ex = new NullPointerException(format("Missing property: %s", P_BLOB_REF));
    when(asset.requireBlobRef()).thenThrow(ex);

    orientDefaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, CHECK_FAILED_HANDLER);

    verify(orientDefaultIntegrityCheckStrategy, never()).checkAsset(any(), any());
    verify(logger).error(ERROR_PROCESSING_ASSET, asset.toString(), ex);

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testCheck_EverythingMatches() {
    runTest("name", TEST_HASH1, "name", TEST_HASH1);

    verifyNoMoreInteractions(logger);

    assertThat(checkFailed, is(false));
  }

  @Test
  public void testCheck_MissingAssetSha1() {
    runTest("name", null, "name", TEST_HASH1);

    verify(logger, never()).error(eq(NAME_MISMATCH), anyString(), anyString());
    verify(logger).error(eq(ERROR_PROCESSING_ASSET_WITH_EX), any(), eq(ASSET_SHA1_MISSING), any());

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testCheck_MismatchAssetSha1() {
    runTest("name", TEST_HASH1, "name", TEST_HASH2);

    verify(logger, never()).error(eq(NAME_MISMATCH), anyString(), anyString());
    verify(logger).error(eq(SHA1_MISMATCH), eq("name"), anyString(), anyString());

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testCheck_MissingAssetName() {
    runTest(null, TEST_HASH1, "name", TEST_HASH1);

    verify(logger, never()).error(eq(NAME_MISMATCH), anyString(), anyString());
    verify(logger).error(eq(ERROR_PROCESSING_ASSET_WITH_EX), any(), eq(ASSET_NAME_MISSING), any());

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testCheck_MissingBlobName() {
    runTest("name", TEST_HASH1, null, TEST_HASH1);

    verify(logger, never()).error(eq(NAME_MISMATCH), anyString(), anyString());
    verify(logger).error(eq(ERROR_PROCESSING_ASSET_WITH_EX), any(), eq(BLOB_NAME_MISSING), any());

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testCheck_MismatchName() {
    runTest("aa", TEST_HASH1, "bb", TEST_HASH1);

    verify(logger).error(eq(NAME_MISMATCH), anyString(), anyString());
    verify(logger, never()).error(eq(SHA1_MISMATCH), anyString(), anyString(), anyString());

    assertThat(checkFailed, is(true));
  }

  /* This will happen in the case of NewDB data migrated to Orient */
  @Test
  public void testCheck_BlobNameStartsWithSlash() {
    runTest("aa", TEST_HASH1, "/aa", TEST_HASH1);

    verifyNoMoreInteractions(logger);

    assertThat(checkFailed, is(false));
  }

  @Test
  public void testCheck_MissingBlobData() {
    runTest("name", TEST_HASH1, "name", TEST_HASH1, () -> false, "name", getMockBlobWithoutData());

    verify(logger).error(eq(BLOB_DATA_MISSING_FOR_ASSET), anyString());

    assertThat(checkFailed, is(true));
  }

  @Test
  public void testCheckAsset_Canceled() {
    runTest("name", TEST_HASH1, "name", TEST_HASH1, () -> true);

    verify(logger).warn(eq(CANCEL_WARNING));
    verifyNoMoreInteractions(logger);

    assertThat(checkFailed, is(false));
  }

  private void runTest(final String assetName, final HashCode assetHash, final String blobName, final HashCode blobHash)
  {
    runTest(assetName, assetHash, blobName, blobHash, () -> false);
  }

  private void runTest(final String assetName,
                       final HashCode assetHash,
                       final String blobName,
                       final HashCode blobHash,
                       final Supplier<Boolean> cancel)
  {
    runTest(assetName, assetHash, blobName, blobHash, cancel, "blob", getMockBlobWithData());
  }

  private void runTest(final String assetName,
                       final HashCode assetHash,
                       final String blobName,
                       final HashCode blobHash,
                       final Supplier<Boolean> cancel,
                       final String blobId,
                       final Blob mockBlob)
  {
    Asset asset = getMockAsset(assetName, assetHash);
    BlobAttributes blobAttributes = getMockBlobAttribues(blobName, blobHash.toString());
    when(storageTx.browseAssets(any(Bucket.class))).thenReturn(newHashSet(asset));
    when(blobStore.getBlobAttributes(any())).thenReturn(blobAttributes);
    when(blobStore.get(new BlobId(blobId))).thenReturn(mockBlob);

    orientDefaultIntegrityCheckStrategy.check(repository, blobStore, cancel, CHECK_FAILED_HANDLER);

    verify(logger).info(startsWith("Checking integrity of assets"), any(), anyString());

    // if cancel is invoked, we'll never see the debug line
    if (!cancel.get()) {
      verify(logger).debug(startsWith("checking asset {}"), any(Asset.class));
    }
  }

  private BlobAttributes getMockBlobAttribues(final String name, final String sha1) {
    return getMockBlobAttribues(name, sha1, false);
  }

  private BlobAttributes getMockBlobAttribues(final String name, final String sha1, final boolean deleted) {
    BlobAttributes blobAttributes = mock(BlobAttributes.class);

    Properties properties = new Properties();
    if (name != null) {
      properties.setProperty(HEADER_PREFIX + BLOB_NAME_HEADER, name);
    }
    when(blobAttributes.getProperties()).thenReturn(properties);

    BlobMetrics metrics = new BlobMetrics(new DateTime(), sha1, 0);
    when(blobAttributes.getMetrics()).thenReturn(metrics);

    when(blobAttributes.isDeleted()).thenReturn(deleted);

    return blobAttributes;
  }

  private Asset getMockAsset(final String name, final HashCode sha1) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(name);
    when(asset.getChecksum(SHA1)).thenReturn(sha1);
    when(asset.requireBlobRef()).thenReturn(new BlobRef("node", "store", "blob"));
    return asset;
  }

  private Blob getMockBlobWithData() {
    Blob blob = mock(Blob.class);
    InputStream blobBytes = mock(InputStream.class);
    when(blob.getInputStream()).thenReturn(blobBytes);
    return blob;
  }

  private Blob getMockBlobWithoutData() {
    Blob blob = mock(Blob.class);
    when(blob.getInputStream()).thenThrow(new BlobStoreException("bse", new BlobId("blob")));
    return blob;
  }

  // The whole point of the integrity checker is to log, so we need to run verifications against a mock logger
  private class TestOrientIntegrityCheckFacet
      extends DefaultOrientIntegrityCheckStrategy
  {
    @Override
    protected Logger createLogger() {
      return logger;
    }
  }
}
