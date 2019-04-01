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
package org.sonatype.nexus.repository.storage.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.orient.DatabaseInstance
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.storage.Bucket
import org.sonatype.nexus.repository.storage.BucketDeleter
import org.sonatype.nexus.repository.storage.BucketEntityAdapter
import org.sonatype.nexus.transaction.RetryController

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock

import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.startsWith
import static org.junit.Assert.assertThat
import static org.mockito.Matchers.same
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class StorageFacetManagerImplTest
    extends TestSupport
{
  static final String TEST_REPOSITORY_NAME = 'testRepository'

  static final String TEST_BLOB_STORE_NAME = 'testBlobStore'

  @Mock
  private BlobStore blobStore

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration

  @Mock
  private DatabaseInstance databaseInstance

  @Mock
  private BucketEntityAdapter bucketEntityAdapter

  @Mock
  private BucketDeleter bucketDeleter

  @Mock
  private RetryController retryController

  @Mock
  private Repository repository

  @Mock
  private ODatabaseDocumentTx db

  @Captor
  ArgumentCaptor<Bucket> bucketCaptor

  private StorageFacetManagerImpl underTest

  @Before
  public void setUp() {
    when(databaseInstance.acquire()).thenReturn(db)
    underTest = new StorageFacetManagerImpl(
        { databaseInstance },
        bucketEntityAdapter,
        bucketDeleter,
        retryController
    )
  }

  @Test
  void 'correctly marks a bucket for deletion'() {
    Bucket bucket = new Bucket(attributes: new NestedAttributesMap("attributes", [:]))

    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME)
    when(repository.getName()).thenReturn(TEST_REPOSITORY_NAME)
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration)

    underTest.enqueueDeletion(repository, blobStore, bucket)
    verify(bucketEntityAdapter).editEntity(same(db), bucketCaptor.capture())

    NestedAttributesMap bucketAttributes = bucketCaptor.value.attributes()
    assertThat(bucketCaptor.value.repositoryName, startsWith(TEST_REPOSITORY_NAME + '$'))
    assertThat(bucketAttributes.contains('pendingDeletion'), is(true))
  }

  @Test
  void 'only buckets marked for deletion will be deleted, and deletion will be attempted at least once for each'() {
    Bucket normalBucket = new Bucket(attributes: new NestedAttributesMap('attributes', [:]))
    Bucket deleteBucket = new Bucket(attributes: new NestedAttributesMap('attributes', [pendingDeletion: [:]]))
    Bucket failedBucket = new Bucket(attributes: new NestedAttributesMap('attributes', [pendingDeletion: [:]]))

    when(bucketEntityAdapter.browse(db)).thenReturn([normalBucket, failedBucket, deleteBucket])
    doThrow(new RuntimeException()).when(bucketDeleter).deleteBucket(failedBucket)

    long count = underTest.performDeletions()

    assertThat(count, is(1L))
    verify(bucketDeleter, never()).deleteBucket(normalBucket)
    verify(bucketDeleter).deleteBucket(failedBucket)
    verify(bucketDeleter).deleteBucket(deleteBucket)
  }
}
