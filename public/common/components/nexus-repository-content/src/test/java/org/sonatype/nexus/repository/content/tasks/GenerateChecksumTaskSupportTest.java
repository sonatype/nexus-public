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
package org.sonatype.nexus.repository.content.tasks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.ContentRepositoryStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import com.google.common.hash.Hashing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GenerateChecksumTaskSupportTest
{
  private static final String TEST_BLOB_STORE_NAME = "test-blobstore";

  private static final String CONTENT_STORE_NAME = "test-content-store";

  private static final String SHA256_KEY = HashAlgorithm.SHA256.name();

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore blobStore;

  @Mock
  private Repository repository;

  @Mock
  private Configuration configuration;

  @Mock
  private ContentFacetSupport contentFacet;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private AssetBlobStore<?> assetBlobStore;

  @Mock
  private FormatStoreManager formatStoreManager;

  private TestGenerateChecksumTask underTest;

  /**
   * Concrete test subclass of the abstract GenerateChecksumTaskSupport.
   */
  private static class TestGenerateChecksumTask
      extends GenerateChecksumTaskSupport
  {
    @Override
    protected boolean appliesTo(final Repository repository) {
      return true;
    }
  }

  @Before
  public void setUp() throws Exception {
    underTest = new TestGenerateChecksumTask();
    underTest.init(32768, blobStoreManager);

    // Set up repository -> configuration -> attributes(STORAGE) -> blobStoreName
    when(repository.getConfiguration()).thenReturn(configuration);
    NestedAttributesMap storageAttributes = new NestedAttributesMap("storage",
        Map.of("blobStoreName", TEST_BLOB_STORE_NAME));
    when(configuration.attributes("storage")).thenReturn(storageAttributes);
    when(repository.getName()).thenReturn("test-repo");

    // Set up repository -> contentFacet
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    // Set up blobStoreManager -> blobStore
    when(blobStoreManager.get(TEST_BLOB_STORE_NAME)).thenReturn(blobStore);

    // Set up contentFacet.stores() with a real ContentFacetStores instance
    when(formatStoreManager.assetBlobStore(CONTENT_STORE_NAME)).thenReturn(assetBlobStore);
    when(formatStoreManager.contentRepositoryStore(CONTENT_STORE_NAME)).thenReturn(mock(ContentRepositoryStore.class));
    when(formatStoreManager.componentStore(CONTENT_STORE_NAME)).thenReturn(mock(ComponentStore.class));
    when(formatStoreManager.assetStore(CONTENT_STORE_NAME)).thenReturn(mock(AssetStore.class));

    ContentFacetStores stores = new ContentFacetStores(
        blobStoreManager, TEST_BLOB_STORE_NAME, formatStoreManager, CONTENT_STORE_NAME);
    when(contentFacet.stores()).thenReturn(stores);
  }

  @Test
  public void testGetMessage() {
    assertThat(underTest.getMessage(), is(equalTo("Generating sha256 hashes")));
  }

  @Test
  public void testInitSetsBufferSizeMinimum() throws Exception {
    TestGenerateChecksumTask task = new TestGenerateChecksumTask();
    task.init(1000, blobStoreManager);

    // The bufferSize should be at least 4096 due to Math.max(4096, bufferSize)
    int bufferSize = getFieldValue(task, "bufferSize");
    assertThat(bufferSize, is(equalTo(4096)));
  }

  @Test
  public void testInitSetsBufferSize() throws Exception {
    TestGenerateChecksumTask task = new TestGenerateChecksumTask();
    task.init(65536, blobStoreManager);

    int bufferSize = getFieldValue(task, "bufferSize");
    assertThat(bufferSize, is(equalTo(65536)));
  }

  @Test(expected = NullPointerException.class)
  public void testInitNullBlobStoreManagerRejected() throws Exception {
    TestGenerateChecksumTask task = new TestGenerateChecksumTask();
    task.init(32768, null);
  }

  @Test
  public void testExecuteWithEmptyAssets() {
    when(fluentAssets.browse(anyInt(), eq(null))).thenReturn(emptyContinuation());

    underTest.execute(repository);

    verify(blobStore, never()).get(any(BlobId.class));
    verify(assetBlobStore, never()).setChecksums(any(), any());
  }

  @Test
  public void testExecuteSkipsAssetsWithSha256Already() {
    FluentAsset asset = mock(FluentAsset.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    when(asset.path()).thenReturn("/some/path");
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    Map<String, String> checksums = new HashMap<>();
    checksums.put(SHA256_KEY, "existingsha256value");
    when(assetBlob.checksums()).thenReturn(checksums);

    when(fluentAssets.browse(anyInt(), eq(null))).thenReturn(continuationOf(asset));
    when(fluentAssets.browse(anyInt(), eq("0"))).thenReturn(emptyContinuation());

    underTest.execute(repository);

    // Should not attempt to read any blob since SHA256 already exists
    verify(blobStore, never()).get(any(BlobId.class));
    verify(assetBlobStore, never()).setChecksums(any(), any());
  }

  @Test
  public void testExecuteSkipsAssetsWithNoBlob() {
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.path()).thenReturn("/some/path");
    when(asset.blob()).thenReturn(Optional.empty());

    when(fluentAssets.browse(anyInt(), eq(null))).thenReturn(continuationOf(asset));
    when(fluentAssets.browse(anyInt(), eq("0"))).thenReturn(emptyContinuation());

    underTest.execute(repository);

    // Should not interact with blob store at all since asset has no blob
    verify(blobStore, never()).get(any(BlobId.class));
    verify(assetBlobStore, never()).setChecksums(any(), any());
  }

  @Test
  public void testExecuteUpdatesChecksumWhenMissing() throws Exception {
    FluentAsset asset = mock(FluentAsset.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef blobRef = mock(BlobRef.class);
    BlobId blobId = new BlobId("test-blob-id");
    Blob blob = mock(Blob.class);

    when(asset.path()).thenReturn("/some/path");
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    // Checksums map without SHA256
    Map<String, String> checksums = new HashMap<>();
    checksums.put("md5", "existingmd5value");
    when(assetBlob.checksums()).thenReturn(checksums);
    when(assetBlob.blobRef()).thenReturn(blobRef);
    when(blobRef.getBlobId()).thenReturn(blobId);
    when(blobStore.get(blobId)).thenReturn(blob);

    byte[] content = "test content for checksum".getBytes(StandardCharsets.UTF_8);
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(content));

    when(fluentAssets.browse(anyInt(), eq(null))).thenReturn(continuationOf(asset));
    when(fluentAssets.browse(anyInt(), eq("0"))).thenReturn(emptyContinuation());

    underTest.execute(repository);

    // Verify setChecksums was called
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> checksumsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(assetBlobStore).setChecksums(eq(assetBlob), checksumsCaptor.capture());

    Map<String, String> updatedChecksums = checksumsCaptor.getValue();
    // Verify SHA256 was added
    assertThat(updatedChecksums.containsKey(SHA256_KEY), is(true));
    // Verify existing md5 is still present
    assertThat(updatedChecksums.get("md5"), is(equalTo("existingmd5value")));

    // Verify the SHA256 value is the correct hash of the content
    String expectedSha256 = Hashing.sha256().hashBytes(content).toString();
    assertThat(updatedChecksums.get(SHA256_KEY), is(equalTo(expectedSha256)));
  }

  @Test
  public void testExecuteHandlesNullBlob() {
    FluentAsset asset = mock(FluentAsset.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef blobRef = mock(BlobRef.class);
    BlobId blobId = new BlobId("test-blob-id");

    when(asset.path()).thenReturn("/some/path");
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    // Checksums map without SHA256
    Map<String, String> checksums = new HashMap<>();
    when(assetBlob.checksums()).thenReturn(checksums);
    when(assetBlob.blobRef()).thenReturn(blobRef);
    when(blobRef.getBlobId()).thenReturn(blobId);

    // BlobStore returns null for this blob ID
    when(blobStore.get(blobId)).thenReturn(null);

    when(fluentAssets.browse(anyInt(), eq(null))).thenReturn(continuationOf(asset));
    when(fluentAssets.browse(anyInt(), eq("0"))).thenReturn(emptyContinuation());

    underTest.execute(repository);

    // Should not call setChecksums since blob was null
    verify(assetBlobStore, never()).setChecksums(any(), any());
  }

  @Test
  public void testExecuteHandlesIOExceptionDuringChecksumCalculation() throws Exception {
    FluentAsset asset = mock(FluentAsset.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef blobRef = mock(BlobRef.class);
    BlobId blobId = new BlobId("test-blob-id");
    Blob blob = mock(Blob.class);

    when(asset.path()).thenReturn("/some/path");
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    Map<String, String> checksums = new HashMap<>();
    when(assetBlob.checksums()).thenReturn(checksums);
    when(assetBlob.blobRef()).thenReturn(blobRef);
    when(blobRef.getBlobId()).thenReturn(blobId);
    when(blobStore.get(blobId)).thenReturn(blob);

    // Use a real InputStream that throws IOException on read
    InputStream failingStream = new InputStream()
    {
      @Override
      public int read() throws IOException {
        throw new IOException("simulated read failure");
      }
    };
    when(blob.getInputStream()).thenReturn(failingStream);

    when(fluentAssets.browse(anyInt(), eq(null))).thenReturn(continuationOf(asset));
    when(fluentAssets.browse(anyInt(), eq("0"))).thenReturn(emptyContinuation());

    underTest.execute(repository);

    // Should not call setChecksums since checksum calculation failed
    verify(assetBlobStore, never()).setChecksums(any(), any());
  }

  @Test
  public void testExecuteProcessesMultiplePages() {
    FluentAsset asset1 = mock(FluentAsset.class);
    when(asset1.path()).thenReturn("/path1");
    when(asset1.blob()).thenReturn(Optional.empty());

    FluentAsset asset2 = mock(FluentAsset.class);
    when(asset2.path()).thenReturn("/path2");
    when(asset2.blob()).thenReturn(Optional.empty());

    // First page returns asset1, second page returns asset2, third page is empty
    Continuation<FluentAsset> page1 = continuationOf(asset1);
    Continuation<FluentAsset> page2 = continuationOf(asset2);

    when(fluentAssets.browse(anyInt(), eq(null))).thenReturn(page1);
    when(fluentAssets.browse(anyInt(), eq("0"))).thenReturn(page2);
    when(fluentAssets.browse(anyInt(), eq("0"))).thenReturn(emptyContinuation());

    underTest.execute(repository);

    // No exceptions thrown, task completes normally
    verify(assetBlobStore, never()).setChecksums(any(), any());
  }

  /**
   * Creates an empty {@link Continuation} for use in tests.
   */
  private static Continuation<FluentAsset> emptyContinuation() {
    return new TestContinuation<>();
  }

  /**
   * Creates a {@link Continuation} containing the given assets.
   */
  @SafeVarargs
  private static Continuation<FluentAsset> continuationOf(final FluentAsset... assets) {
    TestContinuation<FluentAsset> continuation = new TestContinuation<>();
    continuation.addAll(Arrays.asList(assets));
    return continuation;
  }

  /**
   * Simple {@link Continuation} implementation for testing.
   */
  private static class TestContinuation<E>
      extends ArrayList<E>
      implements Continuation<E>
  {
    @Override
    public String nextContinuationToken() {
      return isEmpty() ? null : String.valueOf(size() - 1);
    }
  }

  /**
   * Helper to get a field value via reflection, walking up the class hierarchy.
   */
  @SuppressWarnings("unchecked")
  private static <T> T getFieldValue(
      final Object target,
      final String fieldName) throws NoSuchFieldException, IllegalAccessException
  {
    Class<?> clazz = target.getClass();
    while (clazz != null) {
      try {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
      }
      catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
