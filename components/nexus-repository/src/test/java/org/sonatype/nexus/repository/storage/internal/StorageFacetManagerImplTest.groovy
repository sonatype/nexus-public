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
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Cron
import org.sonatype.nexus.scheduling.schedule.Schedule
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock

import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.startsWith
import static org.junit.Assert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Matchers.same
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class StorageFacetManagerImplTest
    extends TestSupport
{
  static final String TASK_TYPE_ID = 'repository.storage-facet-cleanup'

  static final String TEST_REPOSITORY_NAME = 'testRepository'

  static final String TEST_BLOB_STORE_NAME = 'testBlobStore'

  static final String CRON_EXPRESSION = '0 * * * * ?'

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
  private TaskScheduler taskScheduler

  @Mock
  private ScheduleFactory scheduleFactory

  @Mock
  private Repository repository

  @Mock
  private TaskInfo taskInfo

  @Mock
  private Cron cron

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
        taskScheduler,
        bucketDeleter,
        CRON_EXPRESSION
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

  @Test
  void 'will create a new cleanup task if one does not exist on startup'() {
    TaskConfiguration taskConfiguration = new TaskConfiguration()
    taskConfiguration.setTypeId(TASK_TYPE_ID)

    when(taskScheduler.listsTasks()).thenReturn([])
    when(taskScheduler.getScheduleFactory()).thenReturn(scheduleFactory)
    when(taskScheduler.createTaskConfigurationInstance(TASK_TYPE_ID)).thenReturn(taskConfiguration)
    when(scheduleFactory.cron(any(Date), eq(CRON_EXPRESSION))).thenReturn(cron)

    underTest.doStart()

    verify(taskScheduler).scheduleTask(taskConfiguration, cron)
  }

  @Test
  void 'will not create a duplicate cleanup task if one exists on startup'() {
    TaskConfiguration taskConfiguration = new TaskConfiguration()
    taskConfiguration.setTypeId(TASK_TYPE_ID)

    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration)
    when(taskScheduler.listsTasks()).thenReturn([taskInfo])

    underTest.doStart()

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration), any(Schedule))
  }
}
