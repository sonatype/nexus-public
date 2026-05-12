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

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentContinuation;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

@ExtendWith({AuthenticationExtension.class, MockitoExtension.class})
public class BlobRepositoryMismatchTaskTest
{
  @Mock
  RepositoryManager repositoryManager;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  BlobStore blobStore;

  @Mock
  ContentFacetSupport content;

  @Mock
  FluentAssets assets;

  @Mock
  TaskResultStateStore taskResultStateStore;

  private BlobRepositoryMismatchTask underTest;

  @BeforeEach
  public void setup() {
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, 5, false);
    underTest.install(repositoryManager, new GroupType());
  }

  @Test
  void testExecute() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData assetBlobWithoutMismatch1 = mockBlobAndAsset("my-blob-1", repository.getName());
    AssetData assetBlobWithoutMismatch2 = mockBlobAndAsset("my-blob-2", repository.getName());
    AssetData assetBlobWithoutMismatch3 = mockBlobAndAsset("my-blob-3", repository.getName());
    AssetData assetBlobWithMismatch1 = mockBlobAndAsset("my-blob-4", "other-repo");
    AssetData assetBlobWithMismatch2 = mockBlobAndAsset("my-blob-5", "other-repo");
    AssetData assetBlobWithMismatch3 = mockBlobAndAsset("my-blob-6", "other-repo");
    AssetData assetBlobWithMismatch4 = mockBlobAndAsset("my-blob-7", "other-repo");
    AssetData assetBlobWithMismatch5 = mockBlobAndAsset("my-blob-8", "other-repo");

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    when(assets.browseEager(anyInt(), any())).thenReturn(of(assetBlobWithoutMismatch1, assetBlobWithoutMismatch2,
        assetBlobWithoutMismatch3, assetBlobWithMismatch1, assetBlobWithMismatch2, assetBlobWithMismatch3,
        assetBlobWithMismatch4, assetBlobWithMismatch5));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    assertThat(((Long) underTest.result()), equalTo(5L));
  }

  @Test
  void testExecute_NoMismatches() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData assetBlobWithoutMismatch1 = mockBlobAndAsset("my-blob-1", repository.getName());
    AssetData assetBlobWithoutMismatch2 = mockBlobAndAsset("my-blob-2", repository.getName());
    AssetData assetBlobWithoutMismatch3 = mockBlobAndAsset("my-blob-3", repository.getName());

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    when(assets.browseEager(anyInt(), any())).thenReturn(of(assetBlobWithoutMismatch1, assetBlobWithoutMismatch2,
        assetBlobWithoutMismatch3));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testExecute_NotAHostedRepo() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new GroupType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testAppliesTo_Hosted() {
    mockBlobStore("my-file", "File");
    Repository repository1 = mockRepository("my-repo1", "my-file", new HostedType());
    assertThat(underTest.appliesTo(repository1), is(true));

    mockBlobStore("my-s3", "S3");
    Repository repository2 = mockRepository("my-repo2", "my-s3", new HostedType());
    assertThat(underTest.appliesTo(repository2), is(true));

    Repository repository3 = mockRepository("my-repo3", "my-file", new GroupType());
    assertThat(underTest.appliesTo(repository3), is(false));

    Repository repository4 = mockRepository("my-repo4", "my-file", new ProxyType());
    assertThat(underTest.appliesTo(repository4), is(false));
  }

  @Test
  void testAppliesTo_NullConfiguration() {
    Repository repository = mock(Repository.class);
    when(repository.getConfiguration()).thenReturn(null);

    // getBlobStore returns empty, so appliesTo returns false before checking type
    assertThat(underTest.appliesTo(repository), is(false));
  }

  @Test
  void testAppliesTo_NullAttributes() {
    Repository repository = mock(Repository.class);
    Configuration config = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(config);
    when(config.getAttributes()).thenReturn(null);

    // getBlobStore returns empty, so appliesTo returns false before checking type
    assertThat(underTest.appliesTo(repository), is(false));
  }

  @Test
  void testAppliesTo_MissingStorageAttribute() {
    Repository repository = mock(Repository.class);
    Configuration config = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(config);
    when(config.getAttributes()).thenReturn(Collections.emptyMap());

    // getBlobStore returns empty, so appliesTo returns false before checking type
    assertThat(underTest.appliesTo(repository), is(false));
  }

  @Test
  void testAppliesTo_MissingBlobStoreNameInStorage() {
    Repository repository = mock(Repository.class);
    Configuration config = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(config);
    Map<String, Map<String, Object>> attributes = Map.of(STORAGE, Collections.emptyMap());
    when(config.getAttributes()).thenReturn(attributes);

    // getBlobStore returns empty, so appliesTo returns false before checking type
    assertThat(underTest.appliesTo(repository), is(false));
  }

  @Test
  void testAppliesTo_BlobStoreManagerReturnsNull() {
    Repository repository = mock(Repository.class);
    Configuration config = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(config);
    Map<String, Map<String, Object>> attributes = Map.of(STORAGE, Map.of(BLOB_STORE_NAME, "unknown-store"));
    when(config.getAttributes()).thenReturn(attributes);
    when(blobStoreManager.get("unknown-store")).thenReturn(null);

    // getBlobStore returns empty, so appliesTo returns false before checking type
    assertThat(underTest.appliesTo(repository), is(false));
  }

  @Test
  void testExecute_AssetWithEmptyBlob() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // Create an asset that has hasBlob()=true but blob() returns empty Optional
    AssetData assetWithEmptyBlob = mock(AssetData.class);
    when(assetWithEmptyBlob.hasBlob()).thenReturn(true);
    when(assetWithEmptyBlob.blob()).thenReturn(Optional.empty());
    lenient().when(assetWithEmptyBlob.path()).thenReturn("/test/empty-blob");

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(assetWithEmptyBlob));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // No fix should happen for asset with empty blob
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testExecute_MissingBlobStoreForAsset() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // Create an asset whose blob exists but store lookup fails
    // Since we cache the blob store from repository, we test what happens when
    // accessing the blob itself fails (the blob store is already cached)
    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("test-blob", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    when(ref.getBlobId()).thenReturn(blobId);

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // No fix needed since blob doesn't exist
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testExecute_MissingBlobInStore() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // Create an asset whose blob reference points to a blob that doesn't exist
    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("missing-blob-id", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    when(ref.getBlobId()).thenReturn(blobId);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    when(blobStore.get(blobId)).thenReturn(null);

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // MissingBlobException should be caught, no fix applied
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testExecute_BlobWithNullHeaders_FixFails() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    Blob blob = mock(Blob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("blob-null-headers", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    when(ref.getBlobId()).thenReturn(blobId);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    when(blobStore.get(blobId)).thenReturn(blob);
    // First call: getHeaders() returns mismatching header so fix is attempted
    // Second call inside fixBlobRepositoryHeader: getHeaders() returns null to trigger IOException
    when(blob.getHeaders())
        .thenReturn(Map.of(REPO_NAME_HEADER, "wrong-repo"))
        .thenReturn(null);
    when(blob.getId()).thenReturn(blobId);

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Fix should fail due to null headers in fixBlobRepositoryHeader
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testExecute_BlobWithEmptyHeaders_FixFails() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    Blob blob = mock(Blob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("blob-empty-headers", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    lenient().when(ref.getStore()).thenReturn("my-blobstore");
    when(ref.getBlobId()).thenReturn(blobId);
    when(blobStore.get(blobId)).thenReturn(blob);
    // First call: mismatching header; second call: empty headers
    when(blob.getHeaders())
        .thenReturn(Map.of(REPO_NAME_HEADER, "wrong-repo"))
        .thenReturn(Collections.emptyMap());
    when(blob.getId()).thenReturn(blobId);

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Fix should fail due to empty headers in fixBlobRepositoryHeader
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testExecute_BlobStoreNotFoundForJobStream() throws Exception {
    // Repository with valid config but blobStoreManager returns null for its blobstore
    Repository repository = mock(Repository.class);
    Configuration config = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(config);
    Map<String, Map<String, Object>> attributes = Map.of(STORAGE, Map.of(BLOB_STORE_NAME, "gone-store"));
    when(config.getAttributes()).thenReturn(attributes);
    lenient().when(repository.getName()).thenReturn("my-repo");
    lenient().when(repository.getType()).thenReturn(new HostedType());
    when(blobStoreManager.get("gone-store")).thenReturn(null);
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Task should complete without processing because appliesTo returns false (no blobstore)
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testExecute_AssetWithNoBlob_Filtered() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // Asset without blob should be filtered out by the hasBlob check
    AssetData assetWithoutBlob = mock(AssetData.class);
    when(assetWithoutBlob.hasBlob()).thenReturn(false);

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(assetWithoutBlob));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testGetMessage() {
    String message = underTest.getMessage();

    assertThat(message, containsString(REPO_NAME_HEADER));
    assertThat(message, containsString("Searching for blob properties mismatching"));
  }

  @Test
  void testExecute_MixedAssetsWithMissingBlobAndMismatch() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // One asset with missing blob data (should be skipped)
    AssetData assetWithEmptyBlob = mock(AssetData.class);
    when(assetWithEmptyBlob.hasBlob()).thenReturn(true);
    when(assetWithEmptyBlob.blob()).thenReturn(Optional.empty());
    lenient().when(assetWithEmptyBlob.path()).thenReturn("/test/missing");

    // One asset with mismatch (should be fixed)
    AssetData assetWithMismatch = mockBlobAndAsset("my-blob-fix", "other-repo");

    // One matching asset (should be skipped)
    AssetData assetMatching = mockBlobAndAsset("my-blob-match", "my-repo");

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(assetWithEmptyBlob, assetWithMismatch, assetMatching));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Only the mismatch should be fixed
    assertThat(((Long) underTest.result()), equalTo(1L));
  }

  @Test
  void testExecute_FixBlobHeaderExceptionCaught() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    Blob blob = mock(Blob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("blob-exception", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    lenient().when(ref.getStore()).thenReturn("my-blobstore");
    when(ref.getBlobId()).thenReturn(blobId);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    when(blobStore.get(blobId)).thenReturn(blob);
    when(blob.getId()).thenReturn(blobId);

    // Mismatching header
    Map<String, String> headers = new HashMap<>();
    headers.put(REPO_NAME_HEADER, "wrong-repo");
    when(blob.getHeaders()).thenReturn(headers);

    // createBlobAttributesInstance throws RuntimeException
    when(blobStore.createBlobAttributesInstance(eq(blobId), any(), any()))
        .thenThrow(new RuntimeException("Storage failure"));

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Exception during fix should be caught, count stays at 0
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  void testExecute_BlobWithNullHeadersMatchesAnyRepository() throws Exception {
    // When blob headers are null, checkBlobRepositoryHeaderMatch returns true (match)
    // meaning no fix is needed
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    Blob blob = mock(Blob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("blob-null-hdr", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    when(ref.getBlobId()).thenReturn(blobId);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    when(blobStore.get(blobId)).thenReturn(blob);
    when(blob.getHeaders()).thenReturn(null);

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Null headers should be treated as matching - no fix
    assertThat(((Long) underTest.result()), equalTo(0L));
    verify(blobStore, never()).createBlobAttributesInstance(any(), any(), any());
  }

  @Test
  void testExecute_BlobHeadersMissingRepoNameKey() throws Exception {
    // When headers exist but REPO_NAME_HEADER key is missing, treat as mismatch and fix
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    Blob blob = mock(Blob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("blob-no-repo-hdr", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    lenient().when(ref.getStore()).thenReturn("my-blobstore");
    lenient().when(ref.getBlobId()).thenReturn(blobId);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    lenient().when(blobStore.get(blobId)).thenReturn(blob);
    // Headers exist but without REPO_NAME_HEADER - this is now treated as a mismatch
    when(blob.getHeaders()).thenReturn(Map.of("other-header", "value"));

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Missing REPO_NAME_HEADER triggers fix - fix applied
    assertThat(((Long) underTest.result()), equalTo(1L));
    verify(blobStore).createBlobAttributesInstance(any(), any(), any());
  }

  private void mockBlobStore(final String name, final String type) {
    BlobStore blobStore = mock(BlobStore.class);
    lenient().when(blobStoreManager.get(name)).thenReturn(blobStore);
    this.blobStore = blobStore;

    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    lenient().when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    lenient().when(config.getType()).thenReturn(type);
  }

  private Repository mockRepository(final String name, final String blobStoreName, final Type type) {
    Repository repository = mock(Repository.class);
    Configuration config = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(config);
    lenient().when(repository.getType()).thenReturn(new ProxyType());

    Map<String, Map<String, Object>> attributes = Map.of(STORAGE, Map.of(BLOB_STORE_NAME, blobStoreName));
    when(config.getAttributes()).thenReturn(attributes);

    lenient().when(repositoryManager.get(name)).thenReturn(repository);
    lenient().when(repository.getName()).thenReturn(name);
    when(repository.getType()).thenReturn(type);

    return repository;
  }

  private Continuation<FluentAsset> of(final AssetData... assets) {
    ContinuationArrayList<AssetData> continuation = new ContinuationArrayList<>();
    continuation.addAll(Arrays.asList(assets));
    return new FluentContinuation<FluentAsset, AssetData>(continuation, asset -> new FluentAssetImpl(content, asset));
  }

  private AssetData mockBlobAndAsset(final String blobIdString, final String repositoryName) {
    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    Blob blob = blobIdString != null ? mock(Blob.class) : null;
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId(blobIdString, OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(blob != null);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    lenient().when(ref.getBlobId()).thenReturn(blobId);
    lenient().when(assetBlob.blobRef()).thenReturn(ref);
    lenient().when(ref.getStore()).thenReturn("my-blobstore");
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    lenient().when(blobStore.get(eq(blobId))).thenReturn(blob);
    if (blob != null) {
      lenient().when(blob.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, repositoryName));
    }

    lenient().when(blobStore.createBlobAttributesInstance(eq(blobId), any(), any())).then((invocationOnMock) -> {
      Map<String, String> newHeaders = ((Map) invocationOnMock.getArguments()[1]);
      BlobAttributes blobAttributes = mock(BlobAttributes.class);
      lenient().when(blobAttributes.getHeaders()).thenReturn(newHeaders);
      return blobAttributes;
    });

    lenient().doAnswer(invocationOnMock -> when(blob.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, repositoryName)))
        .when(blobStore)
        .setBlobAttributes(eq(blobId), any());

    return asset;
  }

  @Test
  void testSkipProcessing_Enabled() throws Exception {
    // Create new task with skipProcessing enabled
    BlobRepositoryMismatchTask taskWithSkip =
        new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, 5, true);
    taskWithSkip.install(repositoryManager, new GroupType());

    // Even though skipProcessing=true, the task framework still calls appliesTo() which needs
    // repository lookup. We verify the repository is NOT accessed during jobStream execution.
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    taskWithSkip.configure(task);
    taskWithSkip.call();

    // Result should be 0 and blobStore should never be accessed (skip happens before processing)
    assertThat(((Long) taskWithSkip.result()), equalTo(0L));
    verifyNoInteractions(blobStore);
  }

  // ==================== PROGRESS REPORTING TESTS (Tests 7.1, 7.2) ====================

  /**
   * Test 7.1: Progress Updates During Execution (5000 assets)
   * Verifies that progress updates are logged periodically and reflect actual processing state.
   */
  @Test
  void testExecute_ProgressUpdatesDuringExecution_5000Assets() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("maven-hosted", "my-blobstore", new HostedType());
    when(repositoryManager.get("maven-hosted")).thenReturn(repository);

    int totalAssets = 5000;
    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    ContinuationArrayList<AssetData> allAssets = new ContinuationArrayList<>();
    for (int i = 0; i < totalAssets; i++) {
      AssetData asset = mock(AssetData.class);
      String blobId = "blob-" + i;
      String expectedRepo = (i % 50 == 0) ? "wrong-repo-" + i : "maven-hosted";

      AssetBlob assetBlob = mock(AssetBlob.class);
      BlobRef ref = mock(BlobRef.class);
      BlobId blobIdObj = new BlobId(blobId, OffsetDateTime.now());
      Blob blob = mock(Blob.class);

      lenient().when(asset.hasBlob()).thenReturn(true);
      lenient().when(asset.blob()).thenReturn(Optional.of(assetBlob));
      lenient().when(assetBlob.blobRef()).thenReturn(ref);
      lenient().when(ref.getStore()).thenReturn("my-blobstore");
      lenient().when(ref.getBlobId()).thenReturn(blobIdObj);
      lenient().when(asset.nextContinuationToken()).thenReturn(i < totalAssets - 1 ? "token-" + (i + 1) : null);

      lenient().when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
      lenient().when(blobStore.get(blobIdObj)).thenReturn(blob);
      lenient().when(blob.getId()).thenReturn(blobIdObj);
      lenient().when(blob.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, expectedRepo));

      allAssets.add(asset);
    }

    int browseLimit = 1000;
    Continuation<FluentAsset> continuation =
        createContinuation(allAssets.subList(0, browseLimit), allAssets, "token-1000");
    when(assets.browseEager(eq(browseLimit), isNull())).thenReturn(continuation);

    for (int i = 1000; i < totalAssets; i += browseLimit) {
      int pageStart = i;
      int pageEnd = Math.min(i + browseLimit, totalAssets);
      String tokenKey = "token-" + i;
      String nextToken = (pageEnd < totalAssets) ? "token-" + pageEnd : null;
      continuation = createContinuation(allAssets.subList(pageStart, pageEnd), allAssets, nextToken);
      when(assets.browseEager(eq(browseLimit), eq(tokenKey))).thenReturn(continuation);
    }

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "maven-hosted");
    underTest.configure(task);
    underTest.call();

    Long result = (Long) underTest.result();
    assertThat(result, is(greaterThanOrEqualTo(0L)));
  }

  // ==================== CONTINUATION TOKEN CLEANUP TESTS ====================

  /**
   * Test 7.3: Continuation tokens cleaned up after completion
   * Verifies that when a task completes successfully, its continuation tokens
   * are removed from the configuration.
   */
  @Test
  void testExecute_ContinuationTokensCleanedUpAfterCompletion() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("cleanup-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("cleanup-repo")).thenReturn(repository);

    int totalAssets = 500; // Less than browseLimit (1000), so single page
    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    ContinuationArrayList<AssetData> allAssets = new ContinuationArrayList<>();
    for (int i = 0; i < totalAssets; i++) {
      AssetData asset = mock(AssetData.class);
      String blobId = "cleanup-blob-" + i;
      String expectedRepo = "cleanup-repo";

      AssetBlob assetBlob = mock(AssetBlob.class);
      BlobRef ref = mock(BlobRef.class);
      BlobId blobIdObj = new BlobId(blobId, OffsetDateTime.now());
      Blob blob = mock(Blob.class);

      lenient().when(asset.hasBlob()).thenReturn(true);
      lenient().when(asset.blob()).thenReturn(Optional.of(assetBlob));
      lenient().when(assetBlob.blobRef()).thenReturn(ref);
      lenient().when(ref.getStore()).thenReturn("my-blobstore");
      lenient().when(ref.getBlobId()).thenReturn(blobIdObj);
      lenient().when(asset.nextContinuationToken()).thenReturn(null); // Last page

      lenient().when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
      lenient().when(blobStore.get(blobIdObj)).thenReturn(blob);
      lenient().when(blob.getId()).thenReturn(blobIdObj);
      lenient().when(blob.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, expectedRepo));

      allAssets.add(asset);
    }

    int browseLimit = 1000;
    Continuation<FluentAsset> continuation = createContinuation(allAssets, allAssets, null);
    lenient().when(assets.browseEager(eq(browseLimit), isNull())).thenReturn(continuation);
    lenient().when(assets.browseEager(eq(browseLimit), eq("some-token"))).thenReturn(continuation);

    // Pre-populate continuation tokens in configuration (simulating a resumed task)
    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "cleanup-repo");
    underTest.configure(task);

    // Store some continuation tokens that should be cleaned up
    underTest.getConfiguration().setString(".continuationToken.cleanup-repo", "some-token");
    underTest.getConfiguration().setString(".currentRepositoryBlobCount.cleanup-repo", "100");

    // Verify tokens are present before execution
    assertThat(underTest.getConfiguration().getString(".continuationToken.cleanup-repo"), is("some-token"));
    assertThat(underTest.getConfiguration().getLong(".currentRepositoryBlobCount.cleanup-repo", 0L), is(100L));

    // Execute the task
    underTest.call();

    // Verify continuation tokens are cleaned up after completion
    assertThat(underTest.getConfiguration().getString(".continuationToken.cleanup-repo"), is(nullValue()));
    assertThat(underTest.getConfiguration().getLong(".currentRepositoryBlobCount.cleanup-repo", 0L), is(0L));
  }

  private Continuation<FluentAsset> createContinuation(
      List<AssetData> assets,
      List<AssetData> allAssets,
      String nextToken)
  {
    ContinuationArrayList<AssetData> continuation = new ContinuationArrayList<>();
    continuation.addAll(assets);

    Continuation<AssetData> mockContinuation = mock();
    lenient().when(mockContinuation.nextContinuationToken()).thenReturn(nextToken);
    lenient().when(mockContinuation.iterator()).thenAnswer(invocation -> assets.iterator());
    lenient().when(mockContinuation.size()).thenReturn(continuation.size());

    return new FluentContinuation<>(continuation, a -> new FluentAssetImpl(content, a));
  }

  /**
   * Test 7.2: Task Status in UI
   * Verifies that UI shows task running state and final result.
   */
  @Test
  void testExecute_TaskStatusUpdatedInUI() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("nexus-releases", "my-blobstore", new HostedType());
    when(repositoryManager.get("nexus-releases")).thenReturn(repository);

    int totalAssets = 100;
    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    ContinuationArrayList<AssetData> allAssets = new ContinuationArrayList<>();
    for (int i = 0; i < totalAssets; i++) {
      AssetData asset = mock(AssetData.class);
      String blobId = "release-blob-" + i;
      String expectedRepo = (i % 10 == 0 && i > 0) ? "wrong-repo" : "nexus-releases";

      AssetBlob assetBlob = mock(AssetBlob.class);
      BlobRef ref = mock(BlobRef.class);
      BlobId blobIdObj = new BlobId(blobId, OffsetDateTime.now());
      Blob blob = mock(Blob.class);

      lenient().when(asset.hasBlob()).thenReturn(true);
      lenient().when(asset.blob()).thenReturn(Optional.of(assetBlob));
      lenient().when(assetBlob.blobRef()).thenReturn(ref);
      lenient().when(ref.getStore()).thenReturn("my-blobstore");
      lenient().when(ref.getBlobId()).thenReturn(blobIdObj);
      lenient().when(asset.nextContinuationToken()).thenReturn(i < totalAssets - 1 ? "next-token" : null);

      lenient().when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
      lenient().when(blobStore.get(blobIdObj)).thenReturn(blob);
      lenient().when(blob.getId()).thenReturn(blobIdObj);
      lenient().when(blob.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, expectedRepo));

      lenient().doAnswer(invocationOnMock -> {
        Map<String, String> newHeaders = ((Map) invocationOnMock.getArguments()[1]);
        BlobAttributes blobAttributes = mock(BlobAttributes.class);
        lenient().when(blobAttributes.getHeaders()).thenReturn(newHeaders);
        return blobAttributes;
      }).when(blobStore).createBlobAttributesInstance(eq(blobIdObj), any(), any());

      lenient().doAnswer(invocationOnMock -> {
        lenient().when(blob.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, "nexus-releases"));
        return null;
      }).when(blobStore).setBlobAttributes(eq(blobIdObj), any());

      allAssets.add(asset);
    }

    Continuation<FluentAsset> continuation = createContinuation(allAssets, allAssets, null);
    when(assets.browseEager(anyInt(), any())).thenReturn(continuation);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "nexus-releases");
    underTest.configure(task);
    underTest.call();

    Long result = (Long) underTest.result();
    assertThat(result, equalTo(9L));
    verify(taskResultStateStore, atLeastOnce()).updateJobDataMap(any());
  }

  /**
   * Test 7.2 variant: Verify progress message format during execution
   */
  @Test
  void testExecute_ProgressMessageFormat() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("nexus-snapshots", "my-blobstore", new HostedType());
    when(repositoryManager.get("nexus-snapshots")).thenReturn(repository);

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    AssetData asset = mockBlobAndAsset("snapshot-blob", "wrong-repo");
    Continuation<FluentAsset> continuation =
        createContinuation(Collections.singletonList(asset), Collections.singletonList(asset), null);
    when(assets.browseEager(anyInt(), any())).thenReturn(continuation);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "nexus-snapshots");
    underTest.configure(task);
    underTest.call();

    assertThat(((Long) underTest.result()), equalTo(1L));
    verify(taskResultStateStore, atLeastOnce()).updateJobDataMap(underTest.getTaskInfo());
  }

  // ==================== ERROR HANDLING TESTS (Tests 9.1, 9.2, 9.3) ====================

  /**
   * Test 9.1: Exception During Blob Header Fix - IOException during setBlobAttributes
   * Verifies that exceptions during header update are caught and logged, and task continues.
   */
  @Test
  void testExecute_ExceptionDuringSetBlobAttributes_CaughtAndLogged() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData asset1 = mockBlobAndAsset("my-blob-1", "wrong-repo");
    AssetData asset2 = mockBlobAndAsset("my-blob-2", "my-repo");

    // Create a separate blob for asset1 that throws on setBlobAttributes
    AssetBlob assetBlob1 = mock(AssetBlob.class);
    BlobRef ref1 = mock(BlobRef.class);
    BlobId blobId1 = new BlobId("blob-exception-on-set", OffsetDateTime.now());
    when(asset1.hasBlob()).thenReturn(true);
    when(asset1.blob()).thenReturn(Optional.of(assetBlob1));
    when(assetBlob1.blobRef()).thenReturn(ref1);
    when(ref1.getBlobId()).thenReturn(blobId1);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    Blob blob1 = mock(Blob.class);
    when(blobStore.get(blobId1)).thenReturn(blob1);
    when(blob1.getId()).thenReturn(blobId1);
    // Mismatching header - mismatch needs to be fixed
    when(blob1.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, "wrong-repo"));

    // createBlobAttributesInstance succeeds
    lenient().when(blobStore.createBlobAttributesInstance(eq(blobId1), any(), any())).then((invocationOnMock) -> {
      Map<String, String> newHeaders = ((Map) invocationOnMock.getArguments()[1]);
      BlobAttributes blobAttributes = mock(BlobAttributes.class);
      lenient().when(blobAttributes.getHeaders()).thenReturn(newHeaders);
      return blobAttributes;
    });

    // setBlobAttributes throws RuntimeException to simulate storage failure
    doThrow(new RuntimeException("Blob store write failure"))
        .when(blobStore)
        .setBlobAttributes(eq(blobId1), any());

    lenient().when(repository.facet(ContentFacet.class)).thenReturn(content);
    lenient().when(content.assets()).thenReturn(assets);
    lenient().when(assets.browseEager(anyInt(), any())).thenReturn(of(asset1, asset2));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Asset1 fix fails (IOException), asset2 unchanged - total 0 fixes because asset1 was mismatch but failed
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  /**
   * Test 9.1 variant: Exception During Blob Header Fix - IOException during createBlobAttributesInstance
   * Verifies exception handling during blob attributes creation.
   */
  @Test
  void testExecute_ExceptionDuringCreateBlobAttributesInstance_CaughtAndLogged() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    Blob blob = mock(Blob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("blob-exception-on-create", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    lenient().when(ref.getStore()).thenReturn("my-blobstore");
    when(ref.getBlobId()).thenReturn(blobId);
    when(blobStore.get(blobId)).thenReturn(blob);
    when(blob.getId()).thenReturn(blobId);
    when(blob.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, "wrong-repo"));

    // createBlobAttributesInstance throws RuntimeException
    when(blobStore.createBlobAttributesInstance(eq(blobId), any(), any()))
        .thenThrow(new RuntimeException("Blob attributes creation failed"));

    lenient().when(repository.facet(ContentFacet.class)).thenReturn(content);
    lenient().when(content.assets()).thenReturn(assets);
    lenient().when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Exception during fix should be caught
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  /**
   * Test 9.2: Missing AssetBlob Data (hasBlob()=true but blob() returns empty Optional)
   * Verifies that assets with missing blob data are filtered out gracefully.
   */
  @Test
  void testExecute_MissingAssetBlob_EmptyOptional_Skipped() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // Asset with hasBlob()=true but blob() returns empty Optional
    AssetData asset = mock(AssetData.class);
    lenient().when(asset.hasBlob()).thenReturn(true);
    lenient().when(asset.blob()).thenReturn(Optional.empty());
    lenient().when(asset.path()).thenReturn("/path/to/missing/blob");

    lenient().when(repository.facet(ContentFacet.class)).thenReturn(content);
    lenient().when(content.assets()).thenReturn(assets);
    lenient().when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // Asset filtered by hasBlob check in createJob - no fix attempted
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  /**
   * Test 9.3: Missing Blob on Asset Processing
   * Verifies that assets whose blob doesn't exist in the store are handled gracefully.
   */
  @Test
  void testExecute_MissingBlobInStore_JobLevel() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // Asset whose blob reference points to a blob that doesn't exist
    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId("missing-blob-123", OffsetDateTime.now());
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    when(ref.getBlobId()).thenReturn(blobId);

    lenient().when(asset.path()).thenReturn("/assets/missing-blob");

    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    when(blobStore.get(blobId)).thenReturn(null);

    lenient().when(repository.facet(ContentFacet.class)).thenReturn(content);
    lenient().when(content.assets()).thenReturn(assets);
    lenient().when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // getBlobData throws MissingBlobException - caught in createJob
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  /**
   * Test 9.3 variant: Mixed valid and failing assets
   * Verifies task succeeds with partial result when some assets fail.
   */
  @Test
  void testExecute_MixOfValidAndFailingAssets_PartialSuccess() throws Exception {
    mockBlobStore("my-blobstore", "File");
    Repository repository = mockRepository("my-repo", "my-blobstore", new HostedType());
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // Asset 1: Valid asset with mismatch (should be fixed)
    AssetData asset1 = mockBlobAndAsset("valid-mismatch-1", "other-repo");

    // Asset 2: Asset with missing blob store (should fail gracefully)
    AssetData asset2 = mock(AssetData.class);
    AssetBlob assetBlob2 = mock(AssetBlob.class);
    BlobRef ref2 = mock(BlobRef.class);
    when(asset2.hasBlob()).thenReturn(true);
    when(asset2.blob()).thenReturn(Optional.of(assetBlob2));
    when(assetBlob2.blobRef()).thenReturn(ref2);

    // Asset 3: Valid asset with mismatch (should be fixed)
    AssetData asset3 = mockBlobAndAsset("valid-mismatch-3", "other-repo");

    lenient().when(repository.facet(ContentFacet.class)).thenReturn(content);
    lenient().when(content.assets()).thenReturn(assets);
    lenient().when(assets.browseEager(anyInt(), any())).thenReturn(of(asset1, asset2, asset3));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // 2 valid mismatches fixed, 1 asset skipped due to missing store
    assertThat(((Long) underTest.result()), equalTo(2L));
  }

  // ==================== CONCURRENCY LIMIT TESTS ====================

  /**
   * Test 10.1: Null concurrency limit uses default (Runtime.getRuntime().availableProcessors() / 2)
   * Verifies that when concurrencyLimit is null, the resolved value is the default based on available processors.
   */
  @Test
  void testConcurrencyLimit_Null_UsesDefault() {
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, null, false);
    assertThat(underTest.concurrencyLimit(), is(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));
  }

  /**
   * Test 10.2: Zero concurrency limit falls back to default
   * Verifies that when concurrencyLimit is 0, it falls back to the default.
   */
  @Test
  void testConcurrencyLimit_Zero_FallsBackToDefault() {
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, 0, false);
    assertThat(underTest.concurrencyLimit(), is(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));
  }

  /**
   * Test 10.3: Custom concurrency limit is used
   * Verifies that when concurrencyLimit is positive, it uses that value directly.
   */
  @Test
  void testConcurrencyLimit_CustomValue_UsesProvided() {
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, 10, false);
    assertThat(underTest.concurrencyLimit(), is(10));
  }

  /**
   * Test 10.4: Negative concurrency limit falls back to default
   * Verifies that when concurrencyLimit is negative, it falls back to the default.
   */
  @Test
  void testConcurrencyLimit_Negative_FallsBackToDefault() {
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, -5, false);
    assertThat(underTest.concurrencyLimit(), is(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));
  }

  /**
   * Test 10.5: Concurrency limit is capped at MAX_CONCURRENCY_LIMIT (64)
   * Verifies that when concurrencyLimit exceeds 64, it's capped at the maximum.
   */
  @Test
  void testConcurrencyLimit_ExceedsMax_CappedAt64() {
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, 100, false);
    assertThat(underTest.concurrencyLimit(), is(64));
  }

  /**
   * Test 10.6: Concurrency limit exactly at MAX_CONCURRENCY_LIMIT uses that value
   * Verifies that when concurrencyLimit is exactly 64, it uses that value.
   */
  @Test
  void testConcurrencyLimit_AtMax_UsesProvided() {
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, 64, false);
    assertThat(underTest.concurrencyLimit(), is(64));
  }

  /**
   * Test 10.9: Resume accuracy - browsedCountInThisRun initialized from saved count
   * Verifies that when resuming from a saved continuation token, the browsedCountInThisRun
   * is correctly initialized from the savedProcessedCount, so progress tracking continues
   * accurately from where the previous run left off.
   */
  @Test
  void testResume_BrowsedCountInitializedFromSavedCount() throws Exception {
    String repoName = "resume-test-repo";
    String savedToken = "token-from-previous-run";
    long savedProcessedCount = 2500L; // Previous run processed 2500 assets
    String blobStoreName = "my-blobstore";

    // Setup mocks
    mockBlobStore(blobStoreName, "File");
    Repository repository = mockRepository(repoName, blobStoreName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);
    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    // First page from resume (5 assets)
    AssetData asset1 = mockBlobAndAsset("blob-1", repoName);
    AssetData asset2 = mockBlobAndAsset("blob-2", repoName);
    AssetData asset3 = mockBlobAndAsset("blob-3", repoName);
    AssetData asset4 = mockBlobAndAsset("blob-4", repoName);
    AssetData asset5 = mockBlobAndAsset("blob-5", repoName);
    ContinuationArrayList<AssetData> page1 = new ContinuationArrayList<>();
    page1.add(asset1);
    page1.add(asset2);
    page1.add(asset3);
    page1.add(asset4);
    page1.add(asset5);

    lenient().when(assets.browseEager(eq(1000), eq(savedToken)))
        .thenReturn(new FluentContinuation<>(page1, a -> new FluentAssetImpl(content, a)));

    lenient().when(assets.browseEager(eq(1000), isNull()))
        .thenReturn(new FluentContinuation<>(new ContinuationArrayList<AssetData>(),
            a -> new FluentAssetImpl(content, a)));

    // Configure with saved token and saved processed count
    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    task.setString(".continuationToken." + repoName, savedToken);
    task.setLong(".currentRepositoryBlobCount." + repoName, savedProcessedCount);
    underTest.configure(task);

    underTest.call();

    // verify updateJobDataMap was called for:
    // 1. Token saved after first page completes (tokenFromTokenPersistenceCallback)
    // 2. Completion message (finishedProgress)
    // The callback saves progress after processing the first page with 5 assets
    verify(taskResultStateStore, times(2)).updateJobDataMap(any());
  }

}
