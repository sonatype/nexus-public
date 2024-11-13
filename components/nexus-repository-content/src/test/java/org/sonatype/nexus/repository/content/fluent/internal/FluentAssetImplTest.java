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
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacetDependencies;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.move.RepositoryMoveService;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

public class FluentAssetImplTest
    extends TestSupport
{
  @Mock
  private ContentFacetSupport contentFacet;

  @Mock
  private ContentFacetStores contentFacetStores;

  @Mock
  private BlobStore blobStore;

  @Mock
  private ContentFacetDependencies dependencies;

  @Mock
  private RepositoryMoveService moveService;

  @Mock
  private Asset asset;

  @Mock
  private AssetBlob assetBlob;

  private FluentAssetImpl underTest;

  @Before
  public void setUp() {
    BlobStoreManager mockBlobstoreManager = mock(BlobStoreManager.class);
    FormatStoreManager mockFormatStoreManager = mock(FormatStoreManager.class);
    Repository mockRepository = mock(Repository.class);

    when(mockBlobstoreManager.get(anyString())).thenReturn(blobStore);

    contentFacetStores = new ContentFacetStores(mockBlobstoreManager, "test", mockFormatStoreManager, "test");

    when(contentFacet.stores()).thenReturn(contentFacetStores);
    when(contentFacet.dependencies()).thenReturn(dependencies);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(dependencies.getMoveService()).thenReturn(Optional.of(moveService));
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(new BlobRef("default", "test"));
    when(assetBlob.contentType()).thenReturn("text");

    underTest = new FluentAssetImpl(contentFacet, asset);
  }

  @Test
  public void testDownloadWorksAsExpected() {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);

    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(blobStore.get(any())).thenReturn(mockBlob);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);

    DateTime creationDate = DateTime.now();

    when(mockMetrics.getCreationTime()).thenReturn(creationDate);
    when(mockMetrics.getSha1Hash()).thenReturn("sha1-test");

    try (Content result = underTest.download()) {

      assertNotNull(result);
      assertEquals("text", result.getContentType());
      assertEquals(creationDate, result.getAttributes().get(CONTENT_LAST_MODIFIED));

      verify(blobStore, times(1)).get(any());
    }
    catch (IOException ex) {
      fail();
    }
  }

  @Test
  public void testDownloadWorksIfMoveInProgress(){
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);

    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(blobStore.get(any())).thenReturn(null);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(moveService.getIfBeingMoved(any(BlobRef.class) , anyString())).thenReturn(mockBlob);
    when(mockRepository.getName()).thenReturn("test");
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);

    DateTime creationDate = DateTime.now();

    when(mockMetrics.getCreationTime()).thenReturn(creationDate);
    when(mockMetrics.getSha1Hash()).thenReturn("sha1-test");

    try (Content result = underTest.download()) {

      assertNotNull(result);
      assertEquals("text", result.getContentType());
      assertEquals(creationDate, result.getAttributes().get(CONTENT_LAST_MODIFIED));

      verify(blobStore, times(1)).get(any());
      verify(moveService , times(1)).getIfBeingMoved(any(BlobRef.class) , anyString());
    }
    catch (IOException ex) {
      fail();
    }
  }

  @Test
  public void testDownloadSetCorrectAttributesForProxyRepository() {
    Blob mockBlob = mock(Blob.class);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    Repository mockRepository = mock(Repository.class);
    DateTime creationDate = DateTime.now();

    NestedAttributesMap attributes = new NestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.CONTENT_LAST_MODIFIED, creationDate);
    String etag = "sha1-test";
    attributes.child(Content.CONTENT).set(Content.CONTENT_ETAG, etag);
    when(asset.attributes()).thenReturn(attributes);
    when(blobStore.get(any())).thenReturn(mockBlob);
    when(contentFacet.repository()).thenReturn(mockRepository);
    when(mockBlob.getMetrics()).thenReturn(mockMetrics);

    when(mockRepository.getType()).thenReturn(new ProxyType());
    when(mockMetrics.getCreationTime()).thenReturn(creationDate);
    when(mockMetrics.getSha1Hash()).thenReturn(etag);

    try (Content result = underTest.download()) {

      assertNotNull(result);
      assertEquals("text", result.getContentType());
      assertEquals(creationDate, result.getAttributes().get(CONTENT_LAST_MODIFIED));
      assertEquals(etag, result.getAttributes().get(Content.CONTENT_ETAG));
    }
    catch (IOException ex) {
      fail();
    }
  }

  @Test
  public void testDownloadSetCorrectAttributesForHostedRepository() {
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
    when(blobStore.get(any())).thenReturn(mockBlob);
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
    catch (IOException ex) {
      fail();
    }
  }
}
