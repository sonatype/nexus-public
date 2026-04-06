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

package org.sonatype.nexus.repository.content.fluent.internal;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.ExternalMetadata;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.time.DateHelper;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetDependencies;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.move.RepositoryMoveService;
import org.sonatype.nexus.repository.storage.BlobMetadataStorage;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.view.Content.CONTENT_ETAG;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;
import static org.sonatype.nexus.repository.view.Content.CONTENT_PCCS_HASH;

class FluentAssetImplTest
    extends Test5Support
{
  @Mock
  private ContentFacetSupport contentFacet;

  private ContentFacetStores contentFacetStores;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore blobStore;

  @Mock
  private ContentFacetDependencies dependencies;

  @Mock
  private RepositoryMoveService moveService;

  @Mock
  private AssetStore<?> assetStore;

  @Mock
  private AssetBlobStore<?> assetBlobStore;

  @Mock
  private BlobMetadataStorage blobMetadataStorage;

  @Mock
  private Asset asset;

  @Mock
  private AssetBlob assetBlob;

  private FluentAssetImpl underTest;

  @BeforeEach
  void setUp() {
    FormatStoreManager mockFormatStoreManager = mock(FormatStoreManager.class);
    Repository mockRepository = mock(Repository.class);

    lenient().when(blobStoreManager.get(anyString())).thenReturn(blobStore);
    lenient().when(mockFormatStoreManager.assetStore(anyString())).thenReturn(assetStore);
    lenient().when(mockFormatStoreManager.assetBlobStore(anyString())).thenReturn(assetBlobStore);

    contentFacetStores = new ContentFacetStores(blobStoreManager, "test", mockFormatStoreManager, "test");

    lenient().when(contentFacet.stores()).thenReturn(contentFacetStores);
    lenient().when(contentFacet.repository()).thenReturn(mockRepository);
    lenient().when(contentFacet.blobMetadataStorage()).thenReturn(blobMetadataStorage);
    lenient().when(asset.blob()).thenReturn(Optional.of(assetBlob));
    lenient().when(asset.path()).thenReturn("/test/path");
    lenient().when(assetBlob.blobRef()).thenReturn(new BlobRef("default", "test"));
    lenient().when(assetBlob.contentType()).thenReturn("text");

    underTest = new FluentAssetImpl(contentFacet, asset);
  }

  @Test
  void testDownloadWorksAsExpected() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);

    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);

    DateTime creationDate = DateTime.now();

    when(mockMetrics.getCreationTime()).thenReturn(creationDate);
    when(mockMetrics.getSha1Hash()).thenReturn("sha1-test");

    try (Content result = underTest.download()) {

      assertNotNull(result);
      assertEquals("text", result.getContentType());
      assertEquals(creationDate, result.getAttributes().get(CONTENT_LAST_MODIFIED));

      verify(blobStore, times(1)).get(any(BlobRef.class));
    }
  }

  @Test
  void testDownloadWorksIfMoveInProgress() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);

    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(blobStore.get(any(BlobRef.class))).thenReturn(null);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(contentFacet.dependencies()).thenReturn(dependencies);
    when(dependencies.getMoveService()).thenReturn(Optional.of(moveService));
    when(moveService.getIfBeingMoved(any(BlobRef.class), anyString())).thenReturn(mockBlob);
    when(mockRepository.getName()).thenReturn("test");
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);

    DateTime creationDate = DateTime.now();

    when(mockMetrics.getCreationTime()).thenReturn(creationDate);
    when(mockMetrics.getSha1Hash()).thenReturn("sha1-test");

    try (Content result = underTest.download()) {

      assertNotNull(result);
      assertEquals("text", result.getContentType());
      assertEquals(creationDate, result.getAttributes().get(CONTENT_LAST_MODIFIED));

      verify(blobStore, times(1)).get(any(BlobRef.class));
      verify(moveService, times(1)).getIfBeingMoved(any(BlobRef.class), anyString());
    }
  }

  @Test
  void testDownloadSetCorrectAttributesForProxyRepository() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);
    DateTime creationDate = DateTime.now();

    NestedAttributesMap attributes = new NestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.CONTENT_LAST_MODIFIED, creationDate);
    String etag = "sha1-test";
    String pccsHash = "pccs-hash-123";
    attributes.child(Content.CONTENT).set(Content.CONTENT_ETAG, etag);
    attributes.child(Content.CONTENT).set(Content.CONTENT_PCCS_HASH, pccsHash);
    when(asset.attributes()).thenReturn(attributes);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);

    when(mockRepository.getType()).thenReturn(new ProxyType());

    try (Content result = underTest.download()) {

      assertNotNull(result);
      assertEquals("text", result.getContentType());
      assertEquals(creationDate, result.getAttributes().get(CONTENT_LAST_MODIFIED));
      assertEquals(etag, result.getAttributes().get(CONTENT_ETAG));
      assertEquals(pccsHash, result.getAttributes().get(CONTENT_PCCS_HASH));
    }
  }

  @Test
  void testDownloadSetCorrectAttributesForHostedRepository() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);
    DateTime creationDate = DateTime.now();
    DateTime contentCreationDate = creationDate.minusDays(3);
    String contentETag = "content-sha1-test";
    String expectedETag = "sha1-test";
    NestedAttributesMap attributes = new NestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.CONTENT_LAST_MODIFIED, contentCreationDate);
    attributes.child(Content.CONTENT).set(Content.CONTENT_ETAG, contentETag);
    when(asset.attributes()).thenReturn(attributes);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);

    when(mockRepository.getType()).thenReturn(new HostedType());
    when(mockMetrics.getCreationTime()).thenReturn(creationDate);
    when(mockMetrics.getSha1Hash()).thenReturn(expectedETag);

    try (Content result = underTest.download()) {

      assertNotNull(result);
      assertEquals("text", result.getContentType());
      assertEquals(creationDate, result.getAttributes().get(CONTENT_LAST_MODIFIED));
      assertEquals(expectedETag, result.getAttributes().get(Content.CONTENT_ETAG));
    }
  }

  @Test
  void testDownloadSetCorrectExternalAttributesIfPresent() throws IOException {
    ExternalMetadata externalAttrs = new ExternalMetadata("etag", DateHelper.toOffsetDateTime(new Date()));

    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);
    DateTime creationDate = DateTime.now();

    NestedAttributesMap attributes = new NestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.CONTENT_LAST_MODIFIED, creationDate);
    String etag = "sha1-test";
    attributes.child(Content.CONTENT).set(Content.CONTENT_ETAG, etag);

    when(asset.attributes()).thenReturn(attributes);
    when(assetBlob.externalMetadata()).thenReturn(externalAttrs);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    when(mockRepository.getType()).thenReturn(new ProxyType());

    try (Content result = underTest.download()) {
      assertNotNull(result);
      assertEquals("text", result.getContentType());
      assertEquals(creationDate, result.getAttributes().get(Content.CONTENT_LAST_MODIFIED));
      assertEquals(etag, result.getAttributes().get(Content.CONTENT_ETAG));
      assertEquals(externalAttrs.etag(),
          result.getAttributes().get(Content.EXTERNAL_ETAG));
      assertEquals(externalAttrs.lastModified(),
          result.getAttributes().get(Content.EXTERNAL_LAST_MODIFIED));
    }
  }

  @Test
  void testPathDelegatesToAsset() {
    when(asset.path()).thenReturn("/delegated/path");
    assertEquals("/delegated/path", underTest.path());
    verify(asset).path();
  }

  @Test
  void testKindDelegatesToAsset() {
    when(asset.kind()).thenReturn("ARTIFACT");
    assertEquals("ARTIFACT", underTest.kind());
    verify(asset).kind();
  }

  @Test
  void testComponentDelegatesToAssetWhenPresent() {
    Component mockComponent = mock(Component.class);
    when(asset.component()).thenReturn(Optional.of(mockComponent));
    Optional<Component> result = underTest.component();
    assertTrue(result.isPresent());
    assertSame(mockComponent, result.get());
    verify(asset).component();
  }

  @Test
  void testComponentDelegatesToAssetWhenEmpty() {
    when(asset.component()).thenReturn(Optional.empty());
    Optional<Component> result = underTest.component();
    assertFalse(result.isPresent());
    verify(asset).component();
  }

  @Test
  void testBlobDelegatesToAsset() {
    // asset.blob() is already stubbed in setUp to return Optional.of(assetBlob)
    Optional<AssetBlob> result = underTest.blob();
    assertTrue(result.isPresent());
    assertSame(assetBlob, result.get());
  }

  @Test
  void testBlobDelegatesToAssetWhenEmpty() {
    when(asset.blob()).thenReturn(Optional.empty());
    Optional<AssetBlob> result = underTest.blob();
    assertFalse(result.isPresent());
  }

  @Test
  void testHasBlobDelegatesToAssetTrue() {
    when(asset.hasBlob()).thenReturn(true);
    assertTrue(underTest.hasBlob());
    verify(asset).hasBlob();
  }

  @Test
  void testHasBlobDelegatesToAssetFalse() {
    when(asset.hasBlob()).thenReturn(false);
    assertFalse(underTest.hasBlob());
    verify(asset).hasBlob();
  }

  @Test
  void testLastDownloadedDelegatesToAssetWhenPresent() {
    OffsetDateTime now = OffsetDateTime.now();
    when(asset.lastDownloaded()).thenReturn(Optional.of(now));
    Optional<OffsetDateTime> result = underTest.lastDownloaded();
    assertTrue(result.isPresent());
    assertEquals(now, result.get());
    verify(asset).lastDownloaded();
  }

  @Test
  void testLastDownloadedDelegatesToAssetWhenEmpty() {
    when(asset.lastDownloaded()).thenReturn(Optional.empty());
    Optional<OffsetDateTime> result = underTest.lastDownloaded();
    assertFalse(result.isPresent());
    verify(asset).lastDownloaded();
  }

  @Test
  void testBlobStoreNameDelegatesToAsset() {
    when(asset.blobStoreName()).thenReturn("my-blob-store");
    assertEquals("my-blob-store", underTest.blobStoreName());
    verify(asset).blobStoreName();
  }

  @Test
  void testAssetBlobSizeDelegatesToAsset() {
    when(asset.assetBlobSize()).thenReturn(12345L);
    assertEquals(12345L, underTest.assetBlobSize());
    verify(asset).assetBlobSize();
  }

  @Test
  void testAttributesDelegatesToAsset() {
    NestedAttributesMap mockAttributes = new NestedAttributesMap();
    mockAttributes.set("testKey", "testValue");
    when(asset.attributes()).thenReturn(mockAttributes);
    NestedAttributesMap result = underTest.attributes();
    assertSame(mockAttributes, result);
    assertEquals("testValue", result.get("testKey"));
  }

  @Test
  void testCreatedDelegatesToAsset() {
    OffsetDateTime createdTime = OffsetDateTime.now();
    when(asset.created()).thenReturn(createdTime);
    assertEquals(createdTime, underTest.created());
    verify(asset).created();
  }

  @Test
  void testLastUpdatedDelegatesToAsset() {
    OffsetDateTime lastUpdatedTime = OffsetDateTime.now();
    when(asset.lastUpdated()).thenReturn(lastUpdatedTime);
    assertEquals(lastUpdatedTime, underTest.lastUpdated());
    verify(asset).lastUpdated();
  }

  @Test
  void testUnwrapReturnsWrappedAsset() {
    assertSame(asset, underTest.unwrap());
  }

  @Test
  void testRepositoryDelegatesToFacet() {
    Repository mockRepository = mock(Repository.class);
    when(contentFacet.repository()).thenReturn(mockRepository);
    assertSame(mockRepository, underTest.repository());
    verify(contentFacet).repository();
  }

  @Test
  void testToStringDelegatesToAsset() {
    when(asset.toString()).thenReturn("Asset[path=/test/path]");
    assertEquals("Asset[path=/test/path]", underTest.toString());
  }

  @Test
  void testIsStaleReturnsTrueWhenCacheControllerSaysStale() {
    NestedAttributesMap attributes = new NestedAttributesMap();
    CacheInfo cacheInfo = new CacheInfo(DateTime.now(), "some-token");
    attributes.child("cache").set("last_verified", cacheInfo.getLastVerified().toString());
    attributes.child("cache").set("cache_token", "some-token");
    when(asset.attributes()).thenReturn(attributes);

    CacheController cacheController = mock(CacheController.class);
    when(cacheController.isStale(any(CacheInfo.class))).thenReturn(true);

    assertTrue(underTest.isStale(cacheController));
  }

  @Test
  void testIsStaleReturnsFalseWhenCacheControllerSaysNotStale() {
    NestedAttributesMap attributes = new NestedAttributesMap();
    CacheInfo cacheInfo = new CacheInfo(DateTime.now(), "valid-token");
    attributes.child("cache").set("last_verified", cacheInfo.getLastVerified().toString());
    attributes.child("cache").set("cache_token", "valid-token");
    when(asset.attributes()).thenReturn(attributes);

    CacheController cacheController = mock(CacheController.class);
    when(cacheController.isStale(any(CacheInfo.class))).thenReturn(false);

    assertFalse(underTest.isStale(cacheController));
  }

  @Test
  void testIsStaleReturnsFalseWhenNoCacheInfo() {
    NestedAttributesMap attributes = new NestedAttributesMap();
    when(asset.attributes()).thenReturn(attributes);

    CacheController cacheController = mock(CacheController.class);

    // CacheInfo.fromMap returns null when the cache child map has no entries,
    // so isStale should return false
    assertFalse(underTest.isStale(cacheController));
  }

  @Test
  void testDeleteCallsCheckDeleteAllowedAndDelegates() {
    doNothing().when(contentFacet).checkDeleteAllowed(asset);
    when(assetStore.deleteAsset(asset)).thenReturn(true);

    boolean result = underTest.delete();

    assertTrue(result);
    verify(contentFacet).checkDeleteAllowed(asset);
    verify(assetStore).deleteAsset(asset);
  }

  @Test
  void testDeleteReturnsFalseWhenAssetNotDeleted() {
    doNothing().when(contentFacet).checkDeleteAllowed(asset);
    when(assetStore.deleteAsset(asset)).thenReturn(false);

    boolean result = underTest.delete();

    assertFalse(result);
    verify(contentFacet).checkDeleteAllowed(asset);
    verify(assetStore).deleteAsset(asset);
  }

  @Test
  void testDeleteThrowsWhenDeleteNotAllowed() {
    doThrow(new IllegalOperationException("cannot be deleted"))
        .when(contentFacet)
        .checkDeleteAllowed(asset);

    assertThrows(IllegalOperationException.class, () -> underTest.delete());
    verify(assetStore, never()).deleteAsset(any());
  }

  @Test
  void testKindSetsKindOnAssetDataAndUpdatesStore() {
    AssetData assetData = new AssetData();
    assetData.setPath("/test/path");
    assetData.setKind("OLD_KIND");
    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);

    fluentAsset.kind("NEW_KIND");

    assertEquals("NEW_KIND", assetData.kind());
    verify(assetStore).updateAssetKind(assetData);
  }

  @Test
  void testKindReturnsSelfForFluency() {
    AssetData assetData = new AssetData();
    assetData.setPath("/test/path");
    assetData.setKind("INITIAL");
    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);

    FluentAssetImpl result = (FluentAssetImpl) fluentAsset.kind("UPDATED");

    assertSame(fluentAsset, result);
  }

  @Test
  void testUpdatePathSetsPathOnAssetDataAndUpdatesStore() {
    AssetData assetData = new AssetData();
    assetData.setPath("/old/path");
    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);

    fluentAsset.updatePath("/new/path");

    assertEquals("/new/path", assetData.path());
    verify(assetStore).updateAssetPath(assetData);
  }

  @Test
  void testUpdatePathReturnsSelfForFluency() {
    AssetData assetData = new AssetData();
    assetData.setPath("/old/path");
    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);

    FluentAssetImpl result = (FluentAssetImpl) fluentAsset.updatePath("/new/path");

    assertSame(fluentAsset, result);
  }

  @Test
  void testMarkAsDownloadedDelegatesToStore() {
    underTest.markAsDownloaded();

    verify(assetStore).markAsDownloaded(asset);
  }

  @Test
  void testMarkAsStaleOverlaysCacheWithInvalidatedToken() {
    NestedAttributesMap attributes = new NestedAttributesMap();
    when(asset.attributes()).thenReturn(attributes);
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap("sha1", "abc123"));

    underTest.markAsStale();

    verify(assetStore).updateAssetAttributes(eq(asset), any(AttributeChangeSet.class));
  }

  @Test
  void testMarkAsCachedWithCacheInfoDelegatesToAttributeStore() {
    CacheInfo cacheInfo = new CacheInfo(DateTime.now(), "test-token");
    when(asset.blob()).thenReturn(Optional.empty());

    underTest.markAsCached(cacheInfo);

    verify(assetStore).updateAssetAttributes(eq(asset), any(AttributeChangeSet.class));
  }

  @Test
  void testMarkAsCachedWithContentPayloadExtractsCacheInfoAndHeaders() {
    Content content = mock(Content.class);
    AttributesMap contentAttributes = new AttributesMap();
    CacheInfo cacheInfo = new CacheInfo(DateTime.now(), "test-token");
    contentAttributes.set(CacheInfo.class, cacheInfo);
    DateTime lastModified = DateTime.now();
    contentAttributes.set(CONTENT_LAST_MODIFIED, lastModified.toString());
    contentAttributes.set(CONTENT_ETAG, "etag-value");
    when(content.getAttributes()).thenReturn(contentAttributes);
    when(asset.blob()).thenReturn(Optional.empty());

    underTest.markAsCached(content);

    // Should have been called to persist the cache info and content headers
    verify(assetStore).updateAssetAttributes(eq(asset), any(AttributeChangeSet.class));
  }

  @Test
  void testMarkAsCachedWithNonContentPayloadIsNoOp() {
    Payload payload = mock(Payload.class);

    underTest.markAsCached(payload);

    // Should not interact with assetStore since payload is not Content
    verify(assetStore, never()).updateAssetAttributes(any(), any(AttributeChangeSet.class));
  }

  @Test
  void testMarkAsCachedWithContentPayloadNoCacheInfoStillCachesHeaders() {
    Content content = mock(Content.class);
    AttributesMap contentAttributes = new AttributesMap();
    // no CacheInfo set
    contentAttributes.set(CONTENT_LAST_MODIFIED, DateTime.now().toString());
    contentAttributes.set(CONTENT_ETAG, "etag-value");
    when(content.getAttributes()).thenReturn(contentAttributes);
    when(asset.blob()).thenReturn(Optional.empty());

    underTest.markAsCached(content);

    // Should still update because content headers are present (cacheContentHeaders is called)
    verify(assetStore).updateAssetAttributes(eq(asset), any(AttributeChangeSet.class));
  }

  @Test
  void testMarkAsCachedWithContentPayloadNoHeadersAndNoCacheInfo() {
    Content content = mock(Content.class);
    AttributesMap contentAttributes = new AttributesMap();
    // no CacheInfo, no content headers
    when(content.getAttributes()).thenReturn(contentAttributes);
    when(asset.blob()).thenReturn(Optional.empty());

    underTest.markAsCached(content);

    // cacheContentHeaders produces empty headers so calls withoutAttribute(CONTENT)
    // which triggers attributes(...) which calls assetStore
    verify(assetStore).updateAssetAttributes(eq(asset), any(AttributeChangeSet.class));
  }

  @Test
  void testMarkAsCachedWithContentPayloadIncludesPccsHash() {
    Content content = mock(Content.class);
    AttributesMap contentAttributes = new AttributesMap();
    CacheInfo cacheInfo = new CacheInfo(DateTime.now(), "token");
    contentAttributes.set(CacheInfo.class, cacheInfo);
    contentAttributes.set(CONTENT_LAST_MODIFIED, DateTime.now().toString());
    contentAttributes.set(CONTENT_ETAG, "etag");
    contentAttributes.set(CONTENT_PCCS_HASH, "pccs-hash-value");
    when(content.getAttributes()).thenReturn(contentAttributes);
    when(asset.blob()).thenReturn(Optional.empty());

    underTest.markAsCached(content);

    verify(assetStore).updateAssetAttributes(eq(asset), any(AttributeChangeSet.class));
  }

  @Test
  void testAttributesWithOperationUpdatesStoreAndAttachesBlobMetadata() {
    NestedAttributesMap attributes = new NestedAttributesMap();
    when(asset.attributes()).thenReturn(attributes);
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap("sha1", "abc123"));

    underTest.attributes(AttributeOperation.SET, "myKey", "myValue");

    verify(assetStore).updateAssetAttributes(eq(asset), any(AttributeChangeSet.class));
    verify(blobMetadataStorage).attach(eq(blobStore), any(), any(), eq(attributes), any(Map.class));
  }

  @Test
  void testAttributesWithOperationNoBlobSkipsMetadataAttach() {
    when(asset.blob()).thenReturn(Optional.empty());

    underTest.attributes(AttributeOperation.SET, "myKey", "myValue");

    verify(assetStore).updateAssetAttributes(eq(asset), any(AttributeChangeSet.class));
    verify(blobMetadataStorage, never()).attach(any(), any(), any(), any(), any());
  }

  @Test
  void testAttributesWithChangeSetUpdatesStoreAndAttachesBlobMetadata() {
    NestedAttributesMap attributes = new NestedAttributesMap();
    when(asset.attributes()).thenReturn(attributes);
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap("sha1", "abc123"));

    AttributeChangeSet changes = new AttributeChangeSet();
    changes.attributes(AttributeOperation.SET, "key1", "value1");

    underTest.attributes(changes);

    verify(assetStore).updateAssetAttributes(eq(asset), eq(changes));
    verify(blobMetadataStorage).attach(eq(blobStore), any(), any(), eq(attributes), any(Map.class));
  }

  @Test
  void testAttributesWithChangeSetNoBlobSkipsMetadataAttach() {
    when(asset.blob()).thenReturn(Optional.empty());

    AttributeChangeSet changes = new AttributeChangeSet();
    changes.attributes(AttributeOperation.SET, "key1", "value1");

    underTest.attributes(changes);

    verify(assetStore).updateAssetAttributes(eq(asset), eq(changes));
    verify(blobMetadataStorage, never()).attach(any(), any(), any(), any(), any());
  }

  @Test
  void testAttributesWithOperationReturnsSelfForFluency() {
    when(asset.blob()).thenReturn(Optional.empty());

    FluentAssetImpl result =
        (FluentAssetImpl) underTest.attributes(AttributeOperation.SET, "key", "value");

    assertSame(underTest, result);
  }

  @Test
  void testAttributesWithChangeSetReturnsSelfForFluency() {
    when(asset.blob()).thenReturn(Optional.empty());

    FluentAssetImpl result = (FluentAssetImpl) underTest.attributes(new AttributeChangeSet());

    assertSame(underTest, result);
  }

  @Test
  void testDownloadThrowsIllegalStateExceptionWhenNoBlob() {
    when(asset.blob()).thenReturn(Optional.empty());

    IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> underTest.download());

    assertTrue(thrown.getMessage().contains("No blob attached to"));
    assertTrue(thrown.getMessage().contains("/test/path"));
  }

  @Test
  void testDownloadThrowsMissingBlobExceptionWhenBlobNotInStoreAndNoMoveService() {
    // Use AssetData so InternalIds.internalAssetId() works during retry
    AssetData assetData = createAssetDataWithBlob();
    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);

    when(blobStore.get(any(BlobRef.class))).thenReturn(null);
    when(contentFacet.dependencies()).thenReturn(dependencies);
    when(dependencies.getMoveService()).thenReturn(Optional.empty());
    // Reload returns same asset (still missing blob in store); assetId is 42
    when(assetStore.readAsset(42)).thenReturn(Optional.of(assetData));

    assertThrows(MissingBlobException.class, () -> fluentAsset.download());
  }

  @Test
  void testDownloadThrowsMissingBlobExceptionWhenMoveServiceReturnsNull() throws IOException {
    // Use AssetData so InternalIds.internalAssetId() works during retry
    AssetData assetData = createAssetDataWithBlob();
    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);

    Repository mockRepository = mock(Repository.class);
    when(blobStore.get(any(BlobRef.class))).thenReturn(null);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(contentFacet.dependencies()).thenReturn(dependencies);
    when(dependencies.getMoveService()).thenReturn(Optional.of(moveService));
    when(moveService.getIfBeingMoved(any(BlobRef.class), anyString())).thenReturn(null);
    when(mockRepository.getName()).thenReturn("test-repo");
    // Reload returns same asset (still missing blob in store); assetId is 42
    when(assetStore.readAsset(42)).thenReturn(Optional.of(assetData));

    assertThrows(MissingBlobException.class, () -> fluentAsset.download());
  }

  @Test
  void testDownloadSetsAssetAttributeOnContent() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    when(mockMetrics.getCreationTime()).thenReturn(DateTime.now());
    when(mockMetrics.getSha1Hash()).thenReturn("sha1");

    try (Content result = underTest.download()) {
      assertSame(underTest, result.getAttributes().get(Asset.class));
    }
  }

  @Test
  void testDownloadSetsCacheInfoWhenCacheAttributePresent() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    NestedAttributesMap attributes = new NestedAttributesMap();
    CacheInfo cacheInfo = new CacheInfo(DateTime.now(), "cache-token");
    attributes.child("cache").set("last_verified", cacheInfo.getLastVerified().toString());
    attributes.child("cache").set("cache_token", "cache-token");
    when(asset.attributes()).thenReturn(attributes);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    when(mockMetrics.getCreationTime()).thenReturn(DateTime.now());
    when(mockMetrics.getSha1Hash()).thenReturn("sha1");

    try (Content result = underTest.download()) {
      CacheInfo resultCacheInfo = result.getAttributes().get(CacheInfo.class);
      assertNotNull(resultCacheInfo);
      assertEquals("cache-token", resultCacheInfo.getCacheToken());
    }
  }

  @Test
  void testDownloadProxyRepositoryWithNullLastModifiedPassesNullToDateTime() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);

    // Set up CONTENT child with no CONTENT_LAST_MODIFIED
    NestedAttributesMap attributes = new NestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.CONTENT_ETAG, "etag-value");
    // no CONTENT_LAST_MODIFIED set
    when(asset.attributes()).thenReturn(attributes);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    // For non-group proxy type, getCreationTime/getSha1Hash won't be called when etag is present
    when(mockRepository.getType()).thenReturn(new ProxyType());

    try (Content result = underTest.download()) {
      // For proxy type with null lastModified and non-Group type, lastModified stays null
      // and is wrapped in new DateTime(null) which gives epoch-based DateTime
      assertNotNull(result);
      assertNotNull(result.getAttributes().get(CONTENT_LAST_MODIFIED));
    }
  }

  @Test
  void testDownloadGroupRepositoryFallsBackToCreationTimeWhenLastModifiedNull() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);
    DateTime creationDate = DateTime.now();

    // Set up CONTENT child with no CONTENT_LAST_MODIFIED
    NestedAttributesMap attributes = new NestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.CONTENT_ETAG, "etag-value");
    // no CONTENT_LAST_MODIFIED -> null, and GroupType triggers fallback to blob creation time
    when(asset.attributes()).thenReturn(attributes);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    when(mockMetrics.getCreationTime()).thenReturn(creationDate);
    // getSha1Hash not needed because CONTENT_ETAG is already set in attributes
    when(mockRepository.getType()).thenReturn(new GroupType());

    try (Content result = underTest.download()) {
      assertNotNull(result);
      // For group type with null lastModified, it falls back to blob.getMetrics().getCreationTime()
      assertEquals(creationDate, result.getAttributes().get(CONTENT_LAST_MODIFIED));
    }
  }

  @Test
  void testDownloadProxyRepositoryUsesMetricsSha1WhenNoEtag() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);
    DateTime creationDate = DateTime.now();

    NestedAttributesMap attributes = new NestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.CONTENT_LAST_MODIFIED, creationDate);
    // no CONTENT_ETAG set
    when(asset.attributes()).thenReturn(attributes);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    when(mockMetrics.getSha1Hash()).thenReturn("metrics-sha1");
    when(mockRepository.getType()).thenReturn(new ProxyType());

    try (Content result = underTest.download()) {
      // When CONTENT_ETAG is null, fallback to blob.getMetrics().getSha1Hash()
      assertEquals("metrics-sha1", result.getAttributes().get(CONTENT_ETAG));
    }
  }

  @Test
  void testDownloadExternalMetadataWithOnlyEtag() throws IOException {
    ExternalMetadata externalAttrs = new ExternalMetadata("ext-etag", null);

    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(assetBlob.externalMetadata()).thenReturn(externalAttrs);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    when(mockMetrics.getCreationTime()).thenReturn(DateTime.now());
    when(mockMetrics.getSha1Hash()).thenReturn("sha1");

    try (Content result = underTest.download()) {
      assertEquals("ext-etag", result.getAttributes().get(Content.EXTERNAL_ETAG));
      // lastModified is null so EXTERNAL_LAST_MODIFIED should not be set
      assertFalse(result.getAttributes().contains(Content.EXTERNAL_LAST_MODIFIED));
    }
  }

  @Test
  void testDownloadExternalMetadataWithOnlyLastModified() throws IOException {
    OffsetDateTime lastModified = OffsetDateTime.now();
    ExternalMetadata externalAttrs = new ExternalMetadata(null, lastModified);

    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(assetBlob.externalMetadata()).thenReturn(externalAttrs);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    when(mockMetrics.getCreationTime()).thenReturn(DateTime.now());
    when(mockMetrics.getSha1Hash()).thenReturn("sha1");

    try (Content result = underTest.download()) {
      assertEquals(lastModified, result.getAttributes().get(Content.EXTERNAL_LAST_MODIFIED));
      // etag is null so EXTERNAL_ETAG should not be set
      assertFalse(result.getAttributes().contains(Content.EXTERNAL_ETAG));
    }
  }

  @Test
  void testDownloadExternalMetadataNull() throws IOException {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(assetBlob.externalMetadata()).thenReturn(null);
    when(blobStore.get(any(BlobRef.class))).thenReturn(mockBlob);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);
    when(mockMetrics.getCreationTime()).thenReturn(DateTime.now());
    when(mockMetrics.getSha1Hash()).thenReturn("sha1");

    try (Content result = underTest.download()) {
      assertFalse(result.getAttributes().contains(Content.EXTERNAL_ETAG));
      assertFalse(result.getAttributes().contains(Content.EXTERNAL_LAST_MODIFIED));
    }
  }

  @Test
  void testCreatedSetsOnAssetDataAndDelegatesToStore() {
    AssetData assetData = new AssetData();
    assetData.setPath("/test/path");
    OffsetDateTime createdTime = OffsetDateTime.now();
    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);

    fluentAsset.created(createdTime);

    assertEquals(createdTime, assetData.created());
    verify(assetStore).created(fluentAsset, createdTime);
  }

  @Test
  void testLastDownloadedSetsOnAssetDataAndDelegatesToStore() {
    AssetData assetData = new AssetData();
    assetData.setPath("/test/path");
    OffsetDateTime lastDownloadedTime = OffsetDateTime.now();
    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);

    fluentAsset.lastDownloaded(lastDownloadedTime);

    assertTrue(assetData.lastDownloaded().isPresent());
    assertEquals(lastDownloadedTime, assetData.lastDownloaded().get());
    verify(assetStore).lastDownloaded(fluentAsset, lastDownloadedTime);
  }

  @Test
  void testLastUpdatedDelegatesToStore() {
    OffsetDateTime time = OffsetDateTime.now();

    underTest.lastUpdated(time);

    verify(assetStore).lastUpdated(underTest, time);
  }

  @Test
  void testBlobCreatedWithAssetBlobPresent() {
    OffsetDateTime blobCreated = OffsetDateTime.now();

    underTest.blobCreated(blobCreated);

    verify(assetBlobStore).setBlobCreated(assetBlob, blobCreated);
  }

  @Test
  void testBlobCreatedWithNoBlobIsNoOp() {
    when(asset.blob()).thenReturn(Optional.empty());
    OffsetDateTime blobCreated = OffsetDateTime.now();

    underTest.blobCreated(blobCreated);

    verify(assetBlobStore, never()).setBlobCreated(any(), any());
  }

  @Test
  void testBlobAddedToRepositoryWithBlobPresent() {
    OffsetDateTime addedTime = OffsetDateTime.now();

    underTest.blobAddedToRepository(addedTime);

    verify(assetBlobStore).setAddedToRepository(assetBlob, addedTime);
  }

  @Test
  void testBlobAddedToRepositoryWithNoBlobIsNoOp() {
    when(asset.blob()).thenReturn(Optional.empty());
    OffsetDateTime addedTime = OffsetDateTime.now();

    underTest.blobAddedToRepository(addedTime);

    verify(assetBlobStore, never()).setAddedToRepository(any(), any());
  }

  @Test
  void testCreatedByWithBlobPresent() {
    underTest.createdBy("admin");

    verify(assetBlobStore).setCreatedBy(assetBlob, "admin");
  }

  @Test
  void testCreatedByWithNoBlobIsNoOp() {
    when(asset.blob()).thenReturn(Optional.empty());

    underTest.createdBy("admin");

    verify(assetBlobStore, never()).setCreatedBy(any(), anyString());
  }

  @Test
  void testCreatedByIPWithBlobPresent() {
    underTest.createdByIP("192.168.1.1");

    verify(assetBlobStore).setCreatedByIP(assetBlob, "192.168.1.1");
  }

  @Test
  void testCreatedByIPWithNoBlobIsNoOp() {
    when(asset.blob()).thenReturn(Optional.empty());

    underTest.createdByIP("192.168.1.1");

    verify(assetBlobStore, never()).setCreatedByIP(any(), anyString());
  }

  @Test
  void testBlobCreatedSetsOnAssetBlobData() {
    // When the assetBlob is an AssetBlobData, setBlobCreated should be called on it
    AssetBlobData assetBlobData = new AssetBlobData();
    assetBlobData.setAssetBlobId(1);
    assetBlobData.setBlobRef(new BlobRef("default", "test-blob"));
    assetBlobData.setContentType("text/plain");

    AssetData assetData = new AssetData();
    assetData.setPath("/test/path");
    assetData.setAssetBlob(assetBlobData);

    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);
    OffsetDateTime blobCreated = OffsetDateTime.now();

    fluentAsset.blobCreated(blobCreated);

    assertEquals(blobCreated, assetBlobData.blobCreated());
    verify(assetBlobStore).setBlobCreated(assetBlobData, blobCreated);
  }

  @Test
  void testMarkAsStaleReturnsSelfForFluency() {
    NestedAttributesMap attributes = new NestedAttributesMap();
    when(asset.attributes()).thenReturn(attributes);
    when(assetBlob.checksums()).thenReturn(Collections.singletonMap("sha1", "abc123"));

    FluentAssetImpl result = (FluentAssetImpl) underTest.markAsStale();

    assertSame(underTest, result);
  }

  @Test
  void testMarkAsCachedWithCacheInfoReturnsSelfForFluency() {
    CacheInfo cacheInfo = new CacheInfo(DateTime.now(), "test-token");
    when(asset.blob()).thenReturn(Optional.empty());

    FluentAssetImpl result = (FluentAssetImpl) underTest.markAsCached(cacheInfo);

    assertSame(underTest, result);
  }

  @Test
  void testMarkAsCachedWithPayloadReturnsSelfForFluency() {
    Payload payload = mock(Payload.class);

    FluentAssetImpl result = (FluentAssetImpl) underTest.markAsCached(payload);

    assertSame(underTest, result);
  }

  @Test
  void testMarkAsDownloadedReturnsSelfForFluency() {
    FluentAssetImpl result = (FluentAssetImpl) underTest.markAsDownloaded();

    assertSame(underTest, result);
  }

  @Test
  void testCreatedSetterWithAssetData() {
    AssetData assetData = new AssetData();
    assetData.setPath("/test");
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime newCreated = now.minusDays(1);
    assetData.setCreated(now);

    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);
    fluentAsset.created(newCreated);

    assertEquals(newCreated, assetData.created());
    verify(assetStore).created(fluentAsset, newCreated);
  }

  @Test
  void testLastDownloadedSetterSetsOnAssetData() {
    AssetData assetData = new AssetData();
    assetData.setPath("/test");
    OffsetDateTime downloadTime = OffsetDateTime.now().minusHours(2);

    FluentAssetImpl fluentAsset = new FluentAssetImpl(contentFacet, assetData);
    fluentAsset.lastDownloaded(downloadTime);

    assertTrue(assetData.lastDownloaded().isPresent());
    assertEquals(downloadTime, assetData.lastDownloaded().get());
    verify(assetStore).lastDownloaded(fluentAsset, downloadTime);
  }

  @Test
  void testLastUpdatedDelegatesToStoreWithTimestamp() {
    OffsetDateTime time = OffsetDateTime.now().minusMinutes(30);

    underTest.lastUpdated(time);

    verify(assetStore).lastUpdated(underTest, time);
  }

  @Test
  void testCreatedByIPDelegatesToBlobStore() {
    underTest.createdByIP("10.0.0.1");

    verify(assetBlobStore).setCreatedByIP(assetBlob, "10.0.0.1");
  }

  @Test
  void testCreatedByDelegatesToBlobStore() {
    underTest.createdBy("user1");

    verify(assetBlobStore).setCreatedBy(assetBlob, "user1");
  }

  @Test
  void testBlobAddedToRepositoryDelegatesToBlobStore() {
    OffsetDateTime addedTime = OffsetDateTime.now();

    underTest.blobAddedToRepository(addedTime);

    verify(assetBlobStore).setAddedToRepository(assetBlob, addedTime);
  }

  /**
   * Creates an AssetData with a blob attached, suitable for tests that exercise retry logic
   * requiring InternalIds.internalAssetId() to work (which casts to AssetData internally).
   */
  private AssetData createAssetDataWithBlob() {
    AssetBlobData assetBlobData = new AssetBlobData();
    assetBlobData.setAssetBlobId(1);
    assetBlobData.setBlobRef(new BlobRef("default", "test"));
    assetBlobData.setContentType("text");

    AssetData assetData = new AssetData();
    assetData.setAssetId(42);
    assetData.setPath("/test/path");
    assetData.setKind("");
    assetData.setAssetBlob(assetBlobData);
    return assetData;
  }

  /**
   * Stub exception type used in delete tests since the actual exception thrown by
   * ContentFacetSupport.checkDeleteAllowed is a RuntimeException.
   */
  private static class IllegalOperationException
      extends RuntimeException
  {
    IllegalOperationException(final String message) {
      super(message);
    }
  }
}
