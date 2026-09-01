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
package org.sonatype.nexus.blobstore.external;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.ExternalMetadata;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentContinuation;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.MEMBERS_KEY;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

@ExtendWith(MockitoExtension.class)
class ExternalMetadataTaskTest

{
  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  ContentFacetSupport content;

  @Mock
  ContentFacetStores stores;

  @Mock
  FluentAssets assets;

  @Mock
  AssetBlobStore assetBlobStore;

  @Mock
  SecurityManager securityManager;

  ExternalMetadataTask underTest;

  @BeforeEach
  void setup() {
    ThreadContext.bind(securityManager);

    underTest = new ExternalMetadataTask(blobStoreManager, 5);
    underTest.install(repositoryManager, new GroupType());

    lenient().when(content.stores()).thenReturn(stores);
    lenient().when(stores.assetBlobStore()).thenReturn(assetBlobStore);
  }

  @AfterEach
  void tearDown() {
    ThreadContext.unbindSecurityManager();
  }

  @Test
  void testConstructor() {
    assertThrows(IllegalArgumentException.class, () -> new ExternalMetadataTask(blobStoreManager, 0),
        "external.metadata.repository.concurrencyLimit must be positive");
  }

  @Test
  void testExecute() throws Exception {
    BlobStore blobstore = mockBlobStore("my-blobstore", "S3");
    Repository repository = mockRepository("my-repo", "my-blobstore");
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    // asset with existing metadata -> not called
    AssetBlob blobWithExternalMetadata = mockBlob(true);
    AssetData assetWithExternalMetadata = asset(blobWithExternalMetadata);
    // asset without blob -> not called
    AssetData assetWithoutBlob = asset(null);
    // asset with blob no metadata, and no remote data
    AssetBlob blobWithoutExternalMetadataAndNoRemote = mockBlob(false);
    AssetData assetWithoutExternalMetadataAndNoRemote = asset(blobWithoutExternalMetadataAndNoRemote);
    // asset with blob no metadata
    AssetBlob blobWithoutExternalMetadata = mockBlob(false);
    AssetData assetWithoutExternalMetadata = asset(blobWithoutExternalMetadata);

    when(blobstore.getExternalMetadata(not(eq(blobWithoutExternalMetadata.blobRef())))).thenReturn(Optional.empty());
    when(blobstore.getExternalMetadata(blobWithoutExternalMetadata.blobRef()))
        .thenReturn(Optional.of(new ExternalMetadata("my-etag", OffsetDateTime.now())));

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    when(assets.browse(anyInt(), any())).thenReturn(of(assetWithExternalMetadata, assetWithoutBlob,
        assetWithoutExternalMetadata, assetWithoutExternalMetadataAndNoRemote));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    verify(blobstore).getBlobStoreConfiguration();
    verify(blobstore, never()).getExternalMetadata(blobWithExternalMetadata.blobRef());
    verify(blobstore).getExternalMetadata(blobWithoutExternalMetadata.blobRef());
    verify(blobstore).getExternalMetadata(blobWithoutExternalMetadataAndNoRemote.blobRef());
    verify(assetBlobStore).setExternalMetadata(eq(blobWithoutExternalMetadata), notNull());
    verifyNoMoreInteractions(blobstore, assetBlobStore);
  }

  @Test
  void testExecute_Azure() throws Exception {
    BlobStore blobstore = mockBlobStore("my-blobstore", "Azure Cloud Storage");
    Repository repository = mockRepository("my-repo", "my-blobstore");
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    // asset with existing metadata -> not called
    AssetBlob blobWithExternalMetadata = mockBlob(true);
    AssetData assetWithExternalMetadata = asset(blobWithExternalMetadata);
    // asset without blob -> not called
    AssetData assetWithoutBlob = asset(null);
    // asset with blob no metadata, and no remote data
    AssetBlob blobWithoutExternalMetadataAndNoRemote = mockBlob(false);
    AssetData assetWithoutExternalMetadataAndNoRemote = asset(blobWithoutExternalMetadataAndNoRemote);
    // asset with blob no metadata
    AssetBlob blobWithoutExternalMetadata = mockBlob(false);
    AssetData assetWithoutExternalMetadata = asset(blobWithoutExternalMetadata);

    when(blobstore.getExternalMetadata(not(eq(blobWithoutExternalMetadata.blobRef())))).thenReturn(Optional.empty());
    when(blobstore.getExternalMetadata(blobWithoutExternalMetadata.blobRef()))
        .thenReturn(Optional.of(new ExternalMetadata("my-etag", OffsetDateTime.now())));

    when(repository.facet(ContentFacet.class)).thenReturn(content);
    when(content.assets()).thenReturn(assets);

    when(assets.browse(anyInt(), any())).thenReturn(of(assetWithExternalMetadata, assetWithoutBlob,
        assetWithoutExternalMetadata, assetWithoutExternalMetadataAndNoRemote));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, "my-repo");
    underTest.configure(task);
    underTest.call();

    verify(blobstore).getBlobStoreConfiguration();
    verify(blobstore, never()).getExternalMetadata(blobWithExternalMetadata.blobRef());
    verify(blobstore).getExternalMetadata(blobWithoutExternalMetadata.blobRef());
    verify(blobstore).getExternalMetadata(blobWithoutExternalMetadataAndNoRemote.blobRef());
    verify(assetBlobStore).setExternalMetadata(eq(blobWithoutExternalMetadata), notNull());
    verifyNoMoreInteractions(blobstore, assetBlobStore);
  }

  @Test
  void testAppliesTo_group() {
    mockBlobStore("my-file", "File");
    BlobStore groupBlobStore = mockBlobStore("my-group", "Group");
    mockMembers(groupBlobStore, "my-file");
    Repository repository = mockRepository("my-repo", "my-group");

    assertThat(underTest.appliesTo(repository), is(false));

    mockBlobStore("my-s3", "S3");
    mockMembers(groupBlobStore, "my-file", "my-s3");
    assertThat(underTest.appliesTo(repository), is(true));

    // Azure alone in the group is also sufficient (replacing the S3 member above)
    mockBlobStore("my-azure", "Azure Cloud Storage");
    mockMembers(groupBlobStore, "my-file", "my-azure");
    assertThat(underTest.appliesTo(repository), is(true));
  }

  @Test
  void testAppliesTo_s3() {
    mockBlobStore("my-s3", "S3");
    Repository repository = mockRepository("my-repo", "my-s3");
    assertThat(underTest.appliesTo(repository), is(true));
  }

  @Test
  void testAppliesTo_Azure() {
    mockBlobStore("my-azure", "Azure Cloud Storage");
    Repository repository = mockRepository("my-repo", "my-azure");
    assertThat(underTest.appliesTo(repository), is(true));
  }

  @Test
  void testAppliesTo_Files() {
    mockBlobStore("my-file", "File");
    Repository repository = mockRepository("my-repo", "my-file");
    assertThat(underTest.appliesTo(repository), is(false));
  }

  @Test
  void testAppliesTo_Google() {
    mockBlobStore("my-gcp", "Google Cloud Storage");
    Repository repository = mockRepository("my-repo", "my-gcp");
    assertThat(underTest.appliesTo(repository), is(false));
  }

  private BlobStore mockBlobStore(final String name, final String type) {
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStoreManager.get(name)).thenReturn(blobStore);

    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    lenient().when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    lenient().when(config.getType()).thenReturn(type);

    return blobStore;
  }

  private static void mockMembers(final BlobStore group, final String... memberNames) {
    BlobStoreConfiguration config = group.getBlobStoreConfiguration();
    NestedAttributesMap map = new NestedAttributesMap(CONFIG_KEY, new HashMap<>());
    when(config.attributes(CONFIG_KEY)).thenReturn(map);
    map.set(MEMBERS_KEY, List.of(memberNames));
  }

  private static AssetBlob mockBlob(final boolean hasExternal) {
    AssetBlob blob = mock(AssetBlob.class);
    BlobRef ref = mock(BlobRef.class);
    when(blob.blobRef()).thenReturn(ref);

    if (hasExternal) {
      when(blob.externalMetadata()).thenReturn(new ExternalMetadata("some-etag", OffsetDateTime.now()));
    }

    return blob;
  }

  private static AssetData asset(@Nullable final AssetBlob blob) {
    AssetData asset = mock(AssetData.class);
    when(asset.hasBlob()).thenReturn(blob != null);
    lenient().when(asset.blob()).thenReturn(Optional.ofNullable(blob));
    return asset;
  }

  private Continuation<FluentAsset> of(final AssetData... assets) {
    ContinuationArrayList<AssetData> continuation = new ContinuationArrayList<>();
    continuation.addAll(Arrays.asList(assets));
    return new FluentContinuation<FluentAsset, AssetData>(continuation, asset -> new FluentAssetImpl(content, asset));
  }

  private Repository mockRepository(final String name, final String blobStoreName) {
    Repository repository = mock(Repository.class);
    Configuration config = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(config);
    lenient().when(repository.getType()).thenReturn(new ProxyType());

    Map<String, Map<String, Object>> attributes = Map.of(STORAGE, Map.of(BLOB_STORE_NAME, blobStoreName));
    when(config.getAttributes()).thenReturn(attributes);

    lenient().when(repositoryManager.get(name)).thenReturn(repository);

    return repository;
  }
}
