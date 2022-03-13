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
package org.sonatype.nexus.repository.storage

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobRef
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.common.stateguard.InvalidStateException
import org.sonatype.nexus.orient.DatabaseInstance

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED
import static org.sonatype.nexus.repository.FacetSupport.State.STOPPED

class BucketDeleterTest
    extends TestSupport
{
  static final String BLOB_STORE_NAME = 'test-blob-store'

  @Mock
  private DatabaseInstance databaseInstance

  @Mock
  private BucketEntityAdapter bucketEntityAdapter

  @Mock
  private ComponentEntityAdapter componentEntityAdapter

  @Mock
  private AssetEntityAdapter assetEntityAdapter

  @Mock
  private BlobStoreManager blobStoreManager

  @Mock
  private BlobStore blobStore

  @Mock
  private Bucket bucket

  @Mock
  private ODatabaseDocumentTx db

  private BucketDeleter underTest

  @Before
  void setUp() {
    when(databaseInstance.acquire()).thenReturn(db)
    underTest = new BucketDeleter({ databaseInstance }, bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter,
        blobStoreManager)
  }

  @Test
  void 'components and assets are deleted in batches'() {
    List<BlobId> allBlobIds = []
    List<BlobRef> allBlobRefs = []
    List<Asset> allAssets = []

    List<Component> componentsWithAssets = (1..201).collect {
      BlobId blobId = mock(BlobId)
      allBlobIds << blobId

      BlobRef blobRef = mockBlobRef(blobId)
      allBlobRefs << blobRef

      Asset asset = mockAsset(blobRef)
      allAssets << asset

      Component component = mock(Component)
      when(assetEntityAdapter.browseByComponent(db, component)).thenReturn([asset])
      return component
    }

    List<Asset> assetsWithoutComponents = (1..201).collect {
      BlobId blobId = mock(BlobId)
      allBlobIds << blobId

      BlobRef blobRef = mockBlobRef(blobId)
      allBlobRefs << blobRef

      Asset asset = mockAsset(blobRef)
      allAssets << asset

      return asset
    }

    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(blobStore)
    when(componentEntityAdapter.browseByBucket(db, bucket)).thenReturn(componentsWithAssets)
    when(assetEntityAdapter.browseByBucket(db, bucket)).thenReturn(assetsWithoutComponents)

    underTest.deleteBucket(bucket)

    componentsWithAssets.each { component -> verify(componentEntityAdapter).deleteEntity(db, component) }
    allAssets.each { asset -> verify(assetEntityAdapter).deleteEntity(db, asset) }
    allBlobIds.each { blobId -> verify(blobStore).delete(blobId, 'Deleting Bucket') }
    verify(bucketEntityAdapter).deleteEntity(db, bucket)
    verify(db, times(8)).commit()
  }

  @Test
  void 'blob deletion correctly ignores subsequent deletes from a blob store that no longer exists'() {
    List<BlobId> blobIds = (1..50).collect { mock(BlobId) }
    List<BlobRef> blobRefs = blobIds.collect { blobId -> mockBlobRef(blobId) }
    List<Asset> assets = blobRefs.collect { blobRef -> mockAsset(blobRef) }

    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(null)
    when(componentEntityAdapter.browseByBucket(db, bucket)).thenReturn([])
    when(assetEntityAdapter.browseByBucket(db, bucket)).thenReturn(assets)

    underTest.deleteBucket(bucket)

    assets.each { asset -> verify(assetEntityAdapter).deleteEntity(db, asset) }
    verify(blobStore, never()).delete(any(BlobId), any(String))
    verify(bucketEntityAdapter).deleteEntity(db, bucket)
    verify(db, times(4)).commit()
  }

  @Test
  void 'blob deletion correctly ignores subsequent deletes of a stopped blob store'() {
    List<BlobId> blobIds = (1..50).collect { mock(BlobId) }
    List<BlobRef> blobRefs = blobIds.collect { blobId -> mockBlobRef(blobId) }
    List<Asset> assets = blobRefs.collect { blobRef -> mockAsset(blobRef) }

    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(blobStore)
    doThrow(new InvalidStateException(STOPPED, STARTED)).when(blobStore).delete(any(BlobId), any(String))
    when(componentEntityAdapter.browseByBucket(db, bucket)).thenReturn([])
    when(assetEntityAdapter.browseByBucket(db, bucket)).thenReturn(assets)

    underTest.deleteBucket(bucket)

    assets.each { asset -> verify(assetEntityAdapter).deleteEntity(db, asset) }
    verify(blobStore, times(1)).delete(any(BlobId), any(String))
    verify(bucketEntityAdapter).deleteEntity(db, bucket)
    verify(db, times(4)).commit()
  }

  @Test
  void 'blob deletion will survive exceptions thrown by individual failed blob deletions'() {
    List<BlobId> blobIds = (1..50).collect { mock(BlobId) }
    List<BlobRef> blobRefs = blobIds.collect { blobId -> mockBlobRef(blobId) }
    List<Asset> assets = blobRefs.collect { blobRef -> mockAsset(blobRef) }

    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(blobStore)
    doThrow(new RuntimeException()).when(blobStore).delete(any(BlobId), any(String))
    when(componentEntityAdapter.browseByBucket(db, bucket)).thenReturn([])
    when(assetEntityAdapter.browseByBucket(db, bucket)).thenReturn(assets)

    underTest.deleteBucket(bucket)

    blobIds.each { blobId -> verify(blobStore).delete(blobId, 'Deleting Bucket') }
    assets.each { asset -> verify(assetEntityAdapter).deleteEntity(db, asset) }
    verify(bucketEntityAdapter).deleteEntity(db, bucket)
    verify(db, times(4)).commit()
  }

  private BlobRef mockBlobRef(final BlobId blobId) {
    BlobRef blobRef = mock(BlobRef)
    when(blobRef.getBlobId()).thenReturn(blobId)
    when(blobRef.getStore()).thenReturn(BLOB_STORE_NAME)
    return blobRef
  }

  private Asset mockAsset(final BlobRef blobRef) {
    Asset asset = mock(Asset)
    when(asset.blobRef()).thenReturn(blobRef)
    return asset
  }
}
