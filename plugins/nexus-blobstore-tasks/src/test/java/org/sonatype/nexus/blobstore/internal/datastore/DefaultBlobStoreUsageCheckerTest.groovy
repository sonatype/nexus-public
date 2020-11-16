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
package org.sonatype.nexus.blobstore.internal.datastore

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobRef
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.internal.datastore.DefaultBlobStoreUsageChecker
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.content.AssetBlob
import org.sonatype.nexus.repository.content.facet.ContentFacet
import org.sonatype.nexus.repository.content.fluent.FluentAsset
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder
import org.sonatype.nexus.repository.content.fluent.FluentAssets
import org.sonatype.nexus.repository.content.store.AssetBlobStore
import org.sonatype.nexus.repository.manager.RepositoryManager

import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static com.google.common.collect.Lists.newArrayList
import static java.util.Optional.empty
import static java.util.Optional.of
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.powermock.api.mockito.PowerMockito.when

class DefaultBlobStoreUsageCheckerTest
    extends TestSupport
{
  static final String DEFAULT = 'default'

  static final String NOT_DEFAULT = 'notADefault'

  static final String BLOB_NAME = '/org/example/3.1/plexus-3.1.pom'

  static final String BLOB_ID_STRING = '86e20baa-0bca-4915-a7dc-9a4f34e72321'

  static final BlobId BLOB_ID = new BlobId(BLOB_ID_STRING)

  @Mock
  RepositoryManager repositoryManager

  @Mock
  BlobStore blobStore

  @Mock
  Repository repoA

  @Mock
  Repository repoB

  @Mock
  BlobStoreConfiguration blobStoreConfiguration

  @Mock
  AssetBlobStore assetBlobStore

  @Mock
  AssetBlob assetBlob

  @Mock
  ContentFacet contentFacet

  @Mock
  FluentAssets fluentAssets

  @Mock
  FluentAssetBuilder presentFluentAssetBuilder

  @Mock
  FluentAssetBuilder missingFluentAssetBuilder

  @Mock
  FluentAsset fluentAsset

  DefaultBlobStoreUsageChecker underTest

  @Before
  void setUp() {
    BlobRef blobRef = new BlobRef("%", DEFAULT, BLOB_ID.asUniqueString())

    when(repoA.facet(ContentFacet.class)).thenReturn(contentFacet)
    when(repoB.facet(ContentFacet.class)).thenReturn(contentFacet)

    when(contentFacet.assets()).thenReturn(fluentAssets)

    Map<Repository, AssetBlobStore> assetBlobStoreMap = new HashMap<>()
    assetBlobStoreMap.put(repoA, assetBlobStore)
    assetBlobStoreMap.put(repoB, assetBlobStore)

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration)
    when(blobStoreConfiguration.getName()).thenReturn(DEFAULT)

    when(assetBlobStore.readAssetBlob(any())).thenReturn(empty())
    when(assetBlobStore.readAssetBlob(eq(blobRef))).thenReturn(of(assetBlob))

    when(fluentAssets.path(any())).thenReturn(missingFluentAssetBuilder)
    when(missingFluentAssetBuilder.find()).thenReturn(empty())

    when(fluentAssets.path(eq(BLOB_NAME))).thenReturn(presentFluentAssetBuilder)
    when(presentFluentAssetBuilder.find()).thenReturn(of(fluentAsset))

    when(repositoryManager.browseForBlobStore(eq(DEFAULT))).thenReturn(newArrayList(repoA))
    when(repositoryManager.browseForBlobStore(eq(NOT_DEFAULT))).thenReturn(newArrayList(repoB))

    underTest = new DefaultBlobStoreUsageChecker(repositoryManager,  { r -> assetBlobStoreMap.get(r)})
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
    when(blobStoreConfiguration.getName()).thenReturn(NOT_DEFAULT)

    assertThat(underTest.test(blobStore, BLOB_ID, BLOB_NAME), equalTo(false))
  }
}
