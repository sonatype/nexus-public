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
package org.sonatype.nexus.blobstore.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.record.impl.ODocument
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.when

class FileBlobStoreUsageCheckerTest
    extends TestSupport
{
  static final String ASSET = 'asset'

  static final String DEFAULT = 'default'

  static final String BLOB_NAME = 'org/example/3.1/plexus-3.1.pom'

  static final String BLOB_ID_STRING = '86e20baa-0bca-4915-a7dc-9a4f34e72321'

  static final BlobId BLOB_ID = new BlobId(BLOB_ID_STRING)

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_component")

  @Mock
  BlobStore blobStore

  @Mock
  BlobStoreConfiguration blobStoreConfiguration

  FileBlobStoreUsageChecker underTest

  @Before
  void setUp() {
    configDatabase.instance.connect().withCloseable { db ->
      OSchema schema = db.metadata.schema
      schema.createClass(ASSET)
      asset(BLOB_NAME, "$DEFAULT@some-node-id:$BLOB_ID_STRING")
      asset(BLOB_NAME, "$DEFAULT@some-other-node-id:$BLOB_ID_STRING")
      asset(BLOB_NAME, "notdefault@some-other-node-id:0")
    }

    when(blobStore.blobStoreConfiguration).thenReturn(blobStoreConfiguration)
    when(blobStoreConfiguration.name).thenReturn(DEFAULT)

    underTest = new FileBlobStoreUsageChecker(configDatabase.instanceProvider)
  }

  @Test
  void 'when blob is referenced'() {
    assertThat(underTest.test(blobStore, BLOB_ID, BLOB_NAME), equalTo(true))
  }

  @Test
  void 'when blob name does not match'() {
    assertThat(underTest.test(blobStore, BLOB_ID, 'org/example/not.pom'), equalTo(false))
  }

  @Test
  void 'when blob id does not match'() {
    assertThat(underTest.test(blobStore, new BlobId('0'), BLOB_NAME), equalTo(false))
  }

  @Test
  void 'when blob store does not match'() {
    when(blobStoreConfiguration.name).thenReturn('notdefault')
    assertThat(underTest.test(blobStore, BLOB_ID, BLOB_NAME), equalTo(false))
  }

  static private asset(final name, final blob_ref) {
    def asset = new ODocument('asset')
    asset.field('name', name)
    asset.field('blob_ref', blob_ref)
    asset.save()
  }
}
