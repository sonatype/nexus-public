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
package org.sonatype.nexus.internal.blobstore

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.DatabaseInstance
import org.sonatype.nexus.orient.OClassNameBuilder
import org.sonatype.nexus.repository.config.Configuration

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass
import com.orientechnologies.orient.core.metadata.OMetadataDefault
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OProperty
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy
import com.orientechnologies.orient.core.record.impl.ODocument
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyNoMoreInteractions
import static org.mockito.Mockito.when

/**
 * Tests {@link ConfigDatabaseUpgrade_1_2}
 */
class ConfigDatabaseUpgrade_1_2_Test
    extends TestSupport
{
  @Mock
  Provider<DatabaseInstance> databaseInstanceProvider

  @Mock
  Provider<DatabaseInstance> componentDatabaseInstance

  @Mock
  DatabaseInstance databaseInstance

  @Mock
  ODatabaseDocumentTx documentTx

  @Mock
  OMetadataDefault oMetadataDefault

  @Mock
  OSchemaProxy oSchemaProxy

  @Mock
  OClass blobStoreClass

  @Mock
  OProperty nameProperty

  @Mock
  ODocument defaultBlobStore

  @Mock
  ODocument duplicateDefaultBlobStore

  @Mock
  ODocument uniqueBlobStore

  @Mock
  ODocument customBlobStore

  @Mock
  ODocument duplicateCustomBlobStore

  @Mock
  ODocument repository_dupeDefaultBlobStore;

  @Mock
  Configuration configuration_dupeDefaultBlobStore;

  Map defaultDupeStorageMap = [blobStoreName: "deFAULT"]

  @Mock
  ODocument repository_dupeCustomBlobStore

  @Mock
  Configuration configuration_dupeCustomBlobStore

  Map customDupeStorageMap = [blobStoreName: "CUSTOM"]

  @Mock
  ODocument repository_noDupeBlobStore

  @Mock
  Configuration configuration_noDupeBlobStore

  @Mock
  Map noDupeStorageMap = [blobStoreName: "unique"]


  @Mock
  ODocument compressDuplicateJobDoc;

  @Mock
  ODocument nonDuplicateJobDoc;

  @Mock
  ODocument someOtherJobDoc;

  @Mock
  ORecordIteratorClass oRecordIteratorClass;

  @Mock
  ORecordIteratorClass oRecordIteratorClassBlobStores;

  ConfigDatabaseUpgrade_1_2 underTest

  String DB_CLASS = new OClassNameBuilder()
      .prefix("repository")
      .type("blobstore")
      .build()

  @Before
  void setup() {
    when(databaseInstanceProvider.get()).thenReturn(databaseInstance)
    when(databaseInstance.connect()).thenReturn(documentTx)
    when(documentTx.getMetadata()).thenReturn(oMetadataDefault)
    when(oMetadataDefault.getSchema()).thenReturn(oSchemaProxy)
    when(oSchemaProxy.getClass(DB_CLASS)).thenReturn(blobStoreClass)
    when(blobStoreClass.getProperty('name')).thenReturn(nameProperty)

    when(repository_dupeDefaultBlobStore.field("attributes")).thenReturn([storage: defaultDupeStorageMap])

    when(repository_dupeCustomBlobStore.field("attributes")).thenReturn([storage: customDupeStorageMap])

    when(repository_noDupeBlobStore.field("attributes")).thenReturn([storage: noDupeStorageMap])
    when(noDupeStorageMap.get("blobStoreName")).thenReturn("unique")

    when(defaultBlobStore.field('name', String.class)).thenReturn('default')
    when(duplicateDefaultBlobStore.field('name', String.class)).thenReturn('deFAULT')
    when(uniqueBlobStore.field('name', String.class)).thenReturn('unique')
    when(duplicateCustomBlobStore.field('name', String.class)).thenReturn('CUSTOM')
    when(customBlobStore.field('name', String.class)).thenReturn('custom')

    underTest = new ConfigDatabaseUpgrade_1_2(databaseInstanceProvider, componentDatabaseInstance)
  }

  @Test
  void 'test upgrade to case insensitive name'() {
    underTest.upgradeSchemaToCaseInsensitiveName()
    verify(nameProperty).setCollate(new OCaseInsensitiveCollate())
  }

  @Test
  void 'test rename duplicate blob stores'() {

    def results = [defaultBlobStore, duplicateDefaultBlobStore, uniqueBlobStore, customBlobStore,
                   duplicateCustomBlobStore]
    when(documentTx.browseClass("repository_blobstore")).thenReturn(oRecordIteratorClassBlobStores);
    when(oRecordIteratorClassBlobStores.spliterator()).thenReturn(results.spliterator())

    def repositories = [repository_dupeCustomBlobStore, repository_dupeDefaultBlobStore, repository_noDupeBlobStore]
    when(documentTx.browseClass("repository")).thenReturn(oRecordIteratorClass)
    when(oRecordIteratorClass.spliterator()).thenReturn(Spliterators.spliteratorUnknownSize(repositories.iterator(), 0),
        Spliterators.spliteratorUnknownSize(repositories.iterator(), 0))

    ArgumentCaptor<String> defaultDupeNameCaptor = ArgumentCaptor.forClass(String.class)
    when(duplicateDefaultBlobStore.field(eq('name'), defaultDupeNameCaptor.capture())).
        thenReturn(duplicateDefaultBlobStore)

    ArgumentCaptor<String> customDupeNameCaptor = ArgumentCaptor.forClass(String.class)
    when(duplicateCustomBlobStore.field(eq('name'), customDupeNameCaptor.capture())).
        thenReturn(duplicateCustomBlobStore)

    when(repository_dupeDefaultBlobStore.field(any(), any())).thenReturn(repository_dupeDefaultBlobStore)
    when(repository_dupeCustomBlobStore.field(any(), any())).thenReturn(repository_dupeCustomBlobStore)
    def renames = underTest.renameDuplicateBlobStores()
    underTest.updateRepositoriesForRenamedBlobStores(renames)

    verify(uniqueBlobStore, times(1)).field('name', String.class)
    verifyNoMoreInteractions(uniqueBlobStore)

    assertThat(defaultDupeNameCaptor.getValue(), Matchers.is('deFAULT-1'))
    assertThat(customDupeNameCaptor.getValue(), Matchers.is('CUSTOM-1'))

    assertThat(defaultDupeStorageMap.get("blobStoreName"), Matchers.is("deFAULT-1"))
    assertThat(customDupeStorageMap.get("blobStoreName"), Matchers.is("CUSTOM-1"))

  }

  @Test
  public void testUpdateBlobStoreJobs() {
    when(documentTx.browseClass("quartz_job_detail")).thenReturn(oRecordIteratorClass)
    def records = [compressDuplicateJobDoc, nonDuplicateJobDoc, someOtherJobDoc];
    when(oRecordIteratorClass.spliterator()).thenReturn(Spliterators.spliteratorUnknownSize(records.iterator(), 0))

    def duplicateValueData = [jobDataMap: [blobstoreName: "DEFAULT"]]
    when(compressDuplicateJobDoc.field("value_data")).thenReturn(duplicateValueData)

    def nonDuplicateValueData = [jobDataMap: [blobstoreName: "non-dupe"]]
    when(nonDuplicateJobDoc.field("value_data")).thenReturn(nonDuplicateValueData)

    def notBlobStoreJobValueData = [jobDataMap: [random_data: "test"]]
    when(someOtherJobDoc.field("value_data")).thenReturn(notBlobStoreJobValueData)

    when(compressDuplicateJobDoc.field("value_data", [jobDataMap: [blobstoreName: "DEFAULT-1"]])).
        thenReturn(compressDuplicateJobDoc)

    underTest.updateBlobStoreJobs([DEFAULT: "DEFAULT-1"])

    assertThat(duplicateValueData.jobDataMap.blobstoreName, Matchers.is("DEFAULT-1"))
    assertThat(nonDuplicateValueData, Matchers.is([jobDataMap: [blobstoreName: "non-dupe"]]))
    assertThat(notBlobStoreJobValueData, Matchers.is([jobDataMap: [random_data: "test"]]))

    verify(compressDuplicateJobDoc).save()
    verify(nonDuplicateJobDoc, times(0)).save()
    verify(someOtherJobDoc, times(0)).save()
  }
}
