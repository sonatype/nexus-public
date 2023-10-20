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
package org.sonatype.nexus.repository.tools.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.tools.DeadBlobResult;

import com.google.common.collect.ForwardingCollection;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.tools.ResultState.ASSET_DELETED;
import static org.sonatype.nexus.repository.tools.ResultState.DELETED;
import static org.sonatype.nexus.repository.tools.ResultState.MISSING_BLOB_REF;
import static org.sonatype.nexus.repository.tools.ResultState.SHA1_DISAGREEMENT;
import static org.sonatype.nexus.repository.tools.ResultState.UNAVAILABLE_BLOB;

public class DatastoreDeadBlobFinderTest
    extends TestSupport
{
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private Repository repository;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private Blob blob;

  @Mock
  private BlobRef blobRef;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore blobStore;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private FluentAssetBuilder builder;

  private InputStream blobStream = IOUtils.toInputStream("foo");

  private FluentAsset asset;

  private BlobMetrics blobMetrics = new BlobMetrics(DateTime.now(), "1234", 1234);

  private DatastoreDeadBlobFinder deadBlobFinder;

  @Before
  public void setup() {
    deadBlobFinder = new DatastoreDeadBlobFinder(blobStoreManager);
    asset = createAsset();
    when(repository.getName()).thenReturn("bar");
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(contentFacet.assets()).thenReturn(fluentAssets);

    BlobId blobId = new BlobId("blob-1");
    when(blobRef.getBlobId()).thenReturn(blobId);
    when(blobRef.getStore()).thenReturn("my-blobstore");
    when(blobStore.get(blobId)).thenReturn(blob);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);

    when(fluentAssets.path("foo")).thenReturn(builder);
    when(builder.find()).thenReturn(Optional.of(asset));
  }

  private FluentAsset createAsset() {
    return createAsset(assetBlob);
  }

  private FluentAsset createAsset(final AssetBlob assetBlob) {
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.path()).thenReturn("foo");
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap(HashAlgorithm.SHA1.name(), "1234"));
    when(assetBlob.blobRef()).thenReturn(blobRef);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    return asset;
  }

  @Test
  public void batch_noErrorsIfBlobExistsAndMatchesChecksum() {
    mockAssetBrowse();
    commonBlobMock();

    TestConsumer<DeadBlobResult<Asset>> testConsumer = new TestConsumer<>();
    deadBlobFinder.findAndProcessBatch(repository, false, 100, testConsumer);

    assertThat(testConsumer.getResult(), hasSize(0));
  }

  @Test
  public void noErrorsIfBlobExistsAndMatchesChecksum() {
    mockAssetBrowse();
    commonBlobMock();

    List<DeadBlobResult<Asset>> result = deadBlobFinder.find(repository);

    assertThat(result, hasSize(0));
  }

  @Test
  public void batch_errorsIfBlobSha1DisagreesWithDb() {
    mockAssetBrowse();
    mockAssetReload();
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blob.getInputStream()).thenReturn(blobStream);
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap(HashAlgorithm.SHA1.toString(), "1235"));

    TestConsumer<DeadBlobResult<Asset>> testConsumer = new TestConsumer<>();
    deadBlobFinder.findAndProcessBatch(repository, false, 100, testConsumer);
    List<DeadBlobResult<Asset>> result = testConsumer.getResult();

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getAsset().path(), is("foo"));
    assertThat(result.get(0).getResultState(), is(SHA1_DISAGREEMENT));
  }

  @Test
  public void errorsIfBlobSha1DisagreesWithDb() {
    mockAssetBrowse();
    mockAssetReload();
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blob.getInputStream()).thenReturn(blobStream);
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap(HashAlgorithm.SHA1.toString(), "1235"));

    List<DeadBlobResult<Asset>> result = deadBlobFinder.find(repository);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getAsset().path(), is("foo"));
    assertThat(result.get(0).getResultState(), is(SHA1_DISAGREEMENT));
  }

  @Test
  public void errorsIfBlobInputStreamIsNotAvailable() throws IOException {
    InputStream is = mock(InputStream.class);
    when(is.available()).thenReturn(0);

    mockAssetBrowse();
    commonBlobMock(2, is);
    mockAssetReload();

    List<DeadBlobResult<Asset>> result = deadBlobFinder.find(repository);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getAsset().path(), is("foo"));
    assertThat(result.get(0).getResultState(), is(UNAVAILABLE_BLOB));
  }

  @Test
  public void doNotCheckAvailabilityOfInputStreamIfContentLengthIsZero() throws IOException {
    mockAssetBrowse();
    InputStream stream = mock(InputStream.class);
    commonBlobMock(1, stream, new BlobMetrics(DateTime.now(), "1234", 0));

    List<DeadBlobResult<Asset>> result = deadBlobFinder.find(repository);
    assertThat(result, empty());
    verify(stream, never()).available(); // never called as zero length blob would return 0 correctly
  }

  @Test
  public void missingAssetBlobRefIsAnErrorIfNotIgnored() {
    when(asset.blob()).thenReturn(Optional.empty());
    mockAssetBrowse();
    mockAssetReload();

    List<DeadBlobResult<Asset>> result = deadBlobFinder.find(repository, false);
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getAsset().path(), is("foo"));
    assertThat(result.get(0).getResultState(), is(MISSING_BLOB_REF));
    assertThat(result.get(0).getErrorMessage(), is("Blob not found."));
  }

  @Test
  public void missingAssetBlobRefIsNotAnErrorIfIgnoredNugetCase() {
    when(asset.blob()).thenReturn(Optional.empty());
    mockAssetBrowse();

    List<DeadBlobResult<Asset>> result = deadBlobFinder.find(repository, true);

    assertThat(result, empty());
  }

  @Test
  public void passingInANullRepositoryResultsInAnError() {
    thrown.expect(NullPointerException.class);

    deadBlobFinder.find(null);
    verifyNoMoreInteractions(blobStoreManager);
  }

  @Test
  public void anAssetCanBeDeletedWhileTheSystemIsInspected() throws IOException {
    InputStream is = mock(InputStream.class);
    when(is.available()).thenThrow(new IOException("cannot read inputstream"));

    mockAssetBrowse();
    commonBlobMock(1, is);
    mockAssetReload(null);

    List<DeadBlobResult<Asset>> result = deadBlobFinder.find(repository, true);

    assertThat(result, hasSize(1));
    assertNull(result.get(0).getAsset());
    assertThat(result.get(0).getResultState(), is(ASSET_DELETED));
  }

  @Test
  public void anAssetBlobCanBeDeletedWhileTheSystemIsInspected() {
    AssetBlob missingAssetBlob = mockAssetBlob(mock(AssetBlob.class));
    when(asset.blob()).thenReturn(Optional.of(missingAssetBlob)); // first pass we have a missing blobRef

    FluentAsset reloadedAsset = createAsset(assetBlob);
    Blob reloadedBlob = mock(Blob.class); // second pass the blobRef is there but file does not exist
    when(reloadedBlob.getMetrics()).thenReturn(blobMetrics);
    BlobId missingBlobId = reloadedAsset.blob().get().blobRef().getBlobId();
    when(blobStore.get(missingBlobId)).thenReturn(reloadedBlob);

    mockAssetBrowse();
    mockAssetReload(reloadedAsset);

    when(reloadedBlob.getMetrics()).thenReturn(blobMetrics);
    when(reloadedBlob.getInputStream()).thenThrow(new BlobStoreException("Blob has been deleted", new BlobId("foo")));

    List<DeadBlobResult<Asset>> result = deadBlobFinder.find(repository, true);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getResultState(), is(DELETED));
  }

  private static AssetBlob mockAssetBlob(final AssetBlob assetBlob) {
    BlobRef blobRef = mock(BlobRef.class);
    when(blobRef.getStore()).thenReturn("my-blobstore");
    when(assetBlob.blobRef()).thenReturn(blobRef);
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap(HashAlgorithm.SHA1.name(), "1234"));

    BlobId blobId = mock(BlobId.class);
    when(blobRef.getBlobId()).thenReturn(blobId);
    return assetBlob;
  }

  /**
   * Verify one pass over all Assets.
   *
   * @param assets
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private void mockAssetBrowse(final List<FluentAsset> assets) {
    when(fluentAssets.browse(anyInt(), isNull())).thenReturn(new TestContinuation<>(assets, "token"));
    when(fluentAssets.browse(anyInt(), notNull())).thenReturn(new TestContinuation<>(emptyList(), null));
  }

  private static class TestContinuation<E>
      extends ForwardingCollection<E>
      implements Continuation<E>
  {
    private final Collection<E> collection;

    private final String continuationToken;

    public TestContinuation(final Collection<E> collection, final String continuationToken) {
      this.collection = collection;
      this.continuationToken = continuationToken;
    }

    @Override
    protected Collection<E> delegate() {
      return collection;
    }

    @Override
    public String nextContinuationToken() {
      return continuationToken;
    }
  }

  private void mockAssetBrowse() {
    mockAssetBrowse(Collections.singletonList(asset));
  }

  /**
   * Verify the reload of one particular Asset.
   *
   * @param reloadedAsset the Asset to be individually loaded
   */
  private void mockAssetReload(final FluentAsset reloadedAsset) {
    when(builder.find()).thenReturn(Optional.ofNullable(reloadedAsset));
  }

  private void mockAssetReload() {
    mockAssetReload(asset);
  }

  /**
   * Helper to parameterize mock configuration of Blob access.
   */
  private void commonBlobMock() {
    commonBlobMock(1, blobStream, blobMetrics, "1234");
  }

  private void commonBlobMock(final int passes, final InputStream stream) {
    commonBlobMock(passes, stream, blobMetrics, "1234");
  }

  private void commonBlobMock(final int passes, final InputStream stream, final BlobMetrics blobMetrics) {
    commonBlobMock(passes, stream, blobMetrics, "1234");
  }

  private void commonBlobMock(
      final int passes,
      final InputStream stream,
      final BlobMetrics blobMetrics,
      final String sha1)
  {
    when(blobRef.getStore()).thenReturn("my-blobstore");

    BlobId blobId = new BlobId("blobId");
    when(blobRef.getBlobId()).thenReturn(blobId);
    when(blobStore.get(blobId)).thenReturn(blob);
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap(HashAlgorithm.SHA1.name(), sha1));
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blob.getInputStream()).thenReturn(stream);

  }

  private static class TestConsumer<T> implements Consumer<T> {

    private final List<T> result = new ArrayList<>();

    @Override
    public void accept(final T t) {
      result.add(t);
    }

    public List<T> getResult() {
      return result;
    }
  }
}
