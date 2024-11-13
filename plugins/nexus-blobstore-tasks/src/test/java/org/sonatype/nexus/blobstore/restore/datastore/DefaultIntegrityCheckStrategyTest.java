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
package org.sonatype.nexus.blobstore.restore.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;

import com.google.common.hash.HashCode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import static java.lang.String.format;
import static java.util.Collections.addAll;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.restore.datastore.DefaultIntegrityCheckStrategy.*;

public class DefaultIntegrityCheckStrategyTest
    extends TestSupport
{
  private static final Optional<HashCode> TEST_HASH1 = of(HashCode.fromString("aa"));

  private static final Optional<HashCode> TEST_HASH2 = of(HashCode.fromString("bb"));

  private static final BooleanSupplier NO_CANCEL = () -> false;

  private static final int SINCE_NO_DAYS = 0;

  @Mock
  private Repository repository;

  @Mock
  private BlobStore blobStore;

  @Mock
  private InputStream blobData;

  @Mock
  FluentAssets assets;

  @Mock
  Consumer<Asset> checkFailedHandler;

  @Mock
  private Logger logger;

  private DefaultIntegrityCheckStrategy defaultIntegrityCheckStrategy;

  @Before
  public void setup() throws Exception {
    BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class);
    when(blobStoreConfiguration.getName()).thenReturn("testBlobStore");

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);

    ContentFacet contentFacet = mock(ContentFacet.class);
    when(contentFacet.assets()).thenReturn(assets);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);

    defaultIntegrityCheckStrategy = spy(new TestDefaultIntegrityCheckStrategy(10));
  }

  @Test
  public void testNoBlobAttributes() {
    BlobId blobId = mock(BlobId.class);
    AssetBlob assetBlob = mockBlob(blobId, TEST_HASH1);
    FluentAsset mockAsset = getMockAsset("name", of(assetBlob));

    Continuation<FluentAsset> continuation = buildContinuation(mockAsset);
    when(assets.browse(anyInt(), nullable(String.class))).thenReturn(continuation)
        .thenReturn(new ContinuationArrayList<>());

    // stub attribute load to fail
    when(blobStore.getBlobAttributes(blobId)).thenReturn(null);

    defaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, SINCE_NO_DAYS, checkFailedHandler);

    verify(logger).error(BLOB_PROPERTIES_MISSING_FOR_ASSET, mockAsset.path());
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testBlobDeleted() {
    BlobId blobId = mock(BlobId.class);
    AssetBlob assetBlob = mockBlob(blobId, TEST_HASH1);
    FluentAsset mockAsset = getMockAsset("name", of(assetBlob));

    Continuation<FluentAsset> continuation = buildContinuation(mockAsset);
    when(assets.browse(anyInt(), nullable(String.class))).thenReturn(continuation)
        .thenReturn(new ContinuationArrayList<>());

    BlobAttributes blobAttributes = getMockBlobAttributes(of("name"), TEST_HASH1, true);
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);

    defaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, SINCE_NO_DAYS, checkFailedHandler);

    verify(logger).warn(BLOB_PROPERTIES_MARKED_AS_DELETED, "name");
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testMissingAssetBlob() {
    BlobId blobId = mock(BlobId.class);
    AssetBlob assetBlob = mockBlob(blobId, TEST_HASH1);
    FluentAsset mockAsset = getMockAsset("name", of(assetBlob));

    Continuation<FluentAsset> continuation = buildContinuation(mockAsset);
    when(assets.browse(anyInt(), nullable(String.class))).thenReturn(continuation)
        .thenReturn(new ContinuationArrayList<>());

    when(mockAsset.blob()).thenReturn(empty());

    defaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, SINCE_NO_DAYS, checkFailedHandler);

    verify(logger).error(ERROR_ACCESSING_BLOB, "name");
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testUnexpectedException() {
    BlobId blobId = mock(BlobId.class);
    AssetBlob assetBlob = mockBlob(blobId, TEST_HASH1);
    FluentAsset mockAsset = getMockAsset("name", of(assetBlob));

    Continuation<FluentAsset> continuation = buildContinuation(mockAsset);
    when(assets.browse(anyInt(), nullable(String.class))).thenReturn(continuation)
        .thenReturn(new ContinuationArrayList<>());
    // throw an unexpected error
    NullPointerException ex = new NullPointerException(format("Missing property: %s", "blob_ref"));
    when(mockAsset.blob()).thenThrow(ex);

    defaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, SINCE_NO_DAYS, checkFailedHandler);

    verify(logger).error(ERROR_PROCESSING_ASSET, mockAsset.toString(), ex);
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testCheck_EverythingMatches() {
    runTest("name", TEST_HASH1, "name", TEST_HASH1, () -> false);

    verifyNoMoreInteractions(logger);
    verify(checkFailedHandler, never()).accept(any());
  }

  @Test
  public void testCheck_MissingAssetSha1() {
    runTest("name", empty(), "name", TEST_HASH1, () -> false);

    verify(logger, never()).error(eq(NAME_MISMATCH), nullable(String.class), nullable(String.class));
    verify(logger).error(eq(ERROR_PROCESSING_ASSET_WITH_EX), any(), eq(ASSET_SHA1_MISSING), any());
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testCheck_MismatchAssetSha1() {
    runTest("name", TEST_HASH1, "name", TEST_HASH2, () -> false);

    verify(logger, never()).error(eq(NAME_MISMATCH), nullable(String.class), nullable(String.class));
    verify(logger).error(eq(SHA1_MISMATCH), eq("name"), nullable(String.class), nullable(String.class));
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testCheck_MissingAssetName() {
    runTest(null, TEST_HASH1, "name", TEST_HASH1, () -> false);

    verify(logger, never()).error(eq(NAME_MISMATCH), nullable(String.class), nullable(String.class));
    verify(logger).error(eq(ERROR_PROCESSING_ASSET_WITH_EX), any(), eq(ASSET_NAME_MISSING), any());
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testCheck_MissingBlobName() {
    runTest("name", TEST_HASH1, null, TEST_HASH1, () -> false);

    verify(logger, never()).error(eq(NAME_MISMATCH), nullable(String.class), nullable(String.class));
    verify(logger).error(eq(ERROR_PROCESSING_ASSET_WITH_EX), any(), eq(BLOB_NAME_MISSING), any());
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void test_missingBlobSha1() {
    runTest("name", TEST_HASH1, "name", empty(), () -> false);

    verify(logger, never()).error(eq(NAME_MISMATCH), nullable(String.class), nullable(String.class));
    verify(logger).error(eq(ERROR_PROCESSING_ASSET_WITH_EX), any(), eq(BLOB_METRICS_MISSING_SHA1), any());
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testCheck_MismatchName() {
    runTest("aa", TEST_HASH1, "bb", TEST_HASH1, () -> false);

    verify(logger).error(eq(NAME_MISMATCH), nullable(String.class), nullable(String.class));
    verify(logger, never()).error(eq(SHA1_MISMATCH), nullable(String.class), nullable(String.class),
        nullable(String.class));
    verify(checkFailedHandler).accept(any());
  }

  /* This will happen in the case of Orient data migrated to NewDB */
  @Test
  public void testCheck_BlobNameMissingSlash() {
    runTest("/aa", TEST_HASH1, "aa", TEST_HASH1, () -> false);

    verifyNoMoreInteractions(logger);
    verify(checkFailedHandler, never()).accept(any());
  }

  @Test
  public void testCheck_MissingBlobData() throws IOException {
    doThrow(new BlobStoreException("bse", new BlobId("blob"))).when(blobData).close();
    runTest("name", TEST_HASH1, "name", TEST_HASH1, () -> false);

    verify(logger).error(eq(BLOB_DATA_MISSING_FOR_ASSET), nullable(String.class));
    verify(checkFailedHandler).accept(any());
  }

  @Test
  public void testCheckAsset_Canceled() {
    runTest("name", TEST_HASH1, "name", TEST_HASH1, () -> true);

    verify(logger).warn(eq(CANCEL_WARNING));
    verifyNoMoreInteractions(logger);
    verify(checkFailedHandler, never()).accept(any());
  }

  @Test
  public void shouldNotGetBlobsFromBeforeSinceDays() {
    BlobId blobId = mock(BlobId.class);
    AssetBlob assetBlob = mockBlob(blobId, TEST_HASH1);
    FluentAsset mockAsset = getMockAsset("name", of(assetBlob));
    when(mockAsset.blob()).thenReturn(Optional.of(assetBlob));

    OffsetDateTime date = mock(OffsetDateTime.class);
    when(assetBlob.blobCreated()).thenReturn(date);
    when(date.toLocalDate()).thenReturn(LocalDate.now().minusDays(2));

    Continuation<FluentAsset> continuation = buildContinuation(mockAsset);
    when(assets.browse(anyInt(), nullable(String.class))).thenReturn(continuation)
        .thenReturn(new ContinuationArrayList<>());

    // stub attribute load to fail
    when(blobStore.getBlobAttributes(blobId)).thenReturn(null);

    defaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, 1, checkFailedHandler);

    verify(logger, times(1)).info("Checking integrity of assets in repository '{}' with blob store '{}'", null,
        "testBlobStore");
    verify(logger, times(0)).debug(any());
    verify(checkFailedHandler, never()).accept(any());
  }

  @Test
  public void shouldOnlyGetBlobsFromSinceDays() {
    BlobId blobId = mock(BlobId.class);
    AssetBlob assetBlob = mockBlob(blobId, TEST_HASH1);
    FluentAsset mockAsset = getMockAsset("name", of(assetBlob));
    when(mockAsset.blob()).thenReturn(Optional.of(assetBlob));

    OffsetDateTime date = mock(OffsetDateTime.class);
    when(assetBlob.blobCreated()).thenReturn(date);
    when(date.toLocalDate()).thenReturn(LocalDate.now().minusDays(1));

    Continuation<FluentAsset> continuation = buildContinuation(mockAsset);
    when(assets.browse(anyInt(), nullable(String.class))).thenReturn(continuation)
        .thenReturn(new ContinuationArrayList<>());

    // stub attribute load to fail
    when(blobStore.getBlobAttributes(blobId)).thenReturn(null);

    defaultIntegrityCheckStrategy.check(repository, blobStore, NO_CANCEL, 2, checkFailedHandler);

    verify(logger, times(1)).info("Checking integrity of assets in repository '{}' with blob store '{}'", null,
        "testBlobStore");
    verify(logger, times(1)).debug("Checking asset {}", "name");
  }

  private void runTest(
      final String assetPath,
      final Optional<HashCode> assetHash,
      final String blobName,
      final Optional<HashCode> blobHash,
      final BooleanSupplier cancel)
  {
    BlobAttributes blobAttributes = getMockBlobAttributes(ofNullable(blobName), blobHash, false);
    BlobId blobId = mock(BlobId.class);
    AssetBlob assetBlob = mockBlob(blobId, assetHash);
    FluentAsset mockAsset = getMockAsset(assetPath, of(assetBlob));
    Continuation<FluentAsset> assetContinuation = buildContinuation(mockAsset);

    when(assets.browse(anyInt(), nullable(String.class))).thenReturn(assetContinuation)
        .thenReturn(new ContinuationArrayList<>());
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);

    defaultIntegrityCheckStrategy.check(repository, blobStore, cancel, SINCE_NO_DAYS, checkFailedHandler);

    verify(logger).info(startsWith("Checking integrity of assets"), nullable(String.class), nullable(String.class));

    // if cancel is invoked, we'll never see the debug line
    if (!cancel.getAsBoolean()) {
      verify(logger).debug(startsWith("Checking asset {}"), nullable(String.class));
    }
  }

  private static class ContinuationArrayList<E>
      extends ArrayList<E>
      implements Continuation<E>
  {
    private static final long serialVersionUID = -8278643802740770499L;

    @Override
    public String nextContinuationToken() {
      return null;
    }
  }

  private BlobAttributes getMockBlobAttributes(
      final Optional<String> name,
      final Optional<HashCode> sha1,
      final boolean deleted)
  {
    BlobAttributes blobAttributes = mock(BlobAttributes.class);

    Properties properties = new Properties();
    when(blobAttributes.getProperties()).thenReturn(properties);
    name.ifPresent(n -> properties.setProperty(HEADER_PREFIX + BLOB_NAME_HEADER, n));

    BlobMetrics metrics = new BlobMetrics(new DateTime(), sha1.map(HashCode::toString).orElse(null), 0);
    when(blobAttributes.getMetrics()).thenReturn(metrics);

    when(blobAttributes.isDeleted()).thenReturn(deleted);

    return blobAttributes;
  }

  private Continuation<FluentAsset> buildContinuation(final FluentAsset... assets) {
    ContinuationArrayList<FluentAsset> browseResults = new ContinuationArrayList<>();
    addAll(browseResults, assets);
    return browseResults;
  }

  private AssetBlob mockBlob(final BlobId blobId, final Optional<HashCode> sha1) {
    Map<String, String> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1.name(), sha1.map(HashCode::toString).orElse(null));

    BlobRef blobRef = mock(BlobRef.class);
    when(blobRef.getBlobId()).thenReturn(blobId);

    AssetBlob assetBlob = mock(AssetBlob.class);
    when(assetBlob.blobRef()).thenReturn(blobRef);
    when(assetBlob.checksums()).thenReturn(checksums);

    Blob blob = mock(Blob.class);
    when(blob.getInputStream()).thenReturn(blobData);

    when(blobStore.get(blobId)).thenReturn(blob);

    return assetBlob;
  }

  private FluentAsset getMockAsset(final String path, final Optional<AssetBlob> assetBlob) {
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.path()).thenReturn(path);
    when(asset.blob()).thenReturn(assetBlob);

    return asset;
  }

  // The whole point of the integrity checker is to log, so we need to run verifications against a mock logger
  private class TestDefaultIntegrityCheckStrategy
      extends DefaultIntegrityCheckStrategy
  {
    TestDefaultIntegrityCheckStrategy(final int batchSize) {
      super(batchSize);
    }

    @Override
    protected Logger createLogger() {
      return logger;
    }
  }
}
