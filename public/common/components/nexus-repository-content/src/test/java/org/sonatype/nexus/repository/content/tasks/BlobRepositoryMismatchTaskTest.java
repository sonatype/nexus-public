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
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.Test5Support;
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
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

@ExtendWith(AuthenticationExtension.class)
public class BlobRepositoryMismatchTaskTest
    extends Test5Support
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

  private BlobRepositoryMismatchTask underTest;

  @BeforeEach
  public void setup() {
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, 5, false);
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

    // Create an asset whose blobRef points to a non-existent store
    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef ref = mock(BlobRef.class);
    when(asset.hasBlob()).thenReturn(true);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(ref);
    when(ref.getStore()).thenReturn("missing-store");
    when(blobStoreManager.get("missing-store")).thenReturn(null);

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);
    when(assets.browseEager(anyInt(), any())).thenReturn(of(asset));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    // IOException should be caught, no fix applied
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
    when(ref.getStore()).thenReturn("my-blobstore");
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
    when(ref.getStore()).thenReturn("my-blobstore");
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
    when(ref.getStore()).thenReturn("my-blobstore");
    when(ref.getBlobId()).thenReturn(blobId);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
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
    when(ref.getStore()).thenReturn("my-blobstore");
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
    when(ref.getStore()).thenReturn("my-blobstore");
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
    // When headers exist but REPO_NAME_HEADER key is missing, checkBlobRepositoryHeaderMatch returns true
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
    when(ref.getStore()).thenReturn("my-blobstore");
    when(ref.getBlobId()).thenReturn(blobId);
    when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    when(blobStore.get(blobId)).thenReturn(blob);
    // Headers exist but without REPO_NAME_HEADER
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

    // Missing REPO_NAME_HEADER treated as match (orElse(true)) - no fix
    assertThat(((Long) underTest.result()), equalTo(0L));
    verify(blobStore, never()).createBlobAttributesInstance(any(), any(), any());
  }

  private void mockBlobStore(final String name, final String type) {
    BlobStore blobStore = mock(BlobStore.class);
    lenient().when(blobStoreManager.get(name)).thenReturn(blobStore);

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
    when(ref.getBlobId()).thenReturn(blobId);
    when(assetBlob.blobRef()).thenReturn(ref);
    when(ref.getStore()).thenReturn("my-blobstore");
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
    BlobRepositoryMismatchTask taskWithSkip = new BlobRepositoryMismatchTask(blobStoreManager, 5, true);
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
}
