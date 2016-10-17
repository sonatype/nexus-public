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

import java.nio.file.Path

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.blobstore.api.Blob
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobMetrics
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.common.hash.HashAlgorithm
import org.sonatype.nexus.common.node.NodeAccess

import com.google.common.hash.HashCode
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.is
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1

/**
 * Tests for {@link BlobTx}.
 */
class BlobTxTest
    extends TestSupport
{
  @Test
  void 'create blob with verified hashes'() {

    final long blobSize = 10L
    final String contentType = 'text/plain'
    final InputStream inputStream = new ByteArrayInputStream('helloworld'.getBytes('UTF-8'))

    final NodeAccess nodeAccess = mock(NodeAccess.class)
    when(nodeAccess.getId()).thenReturn('id')

    final BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class)
    when(blobStoreConfiguration.getName()).thenReturn('blobStoreConfiguration')

    final BlobMetrics blobMetrics = mock(BlobMetrics.class)
    when(blobMetrics.getContentSize()).thenReturn(blobSize)

    final BlobId blobId = new BlobId('blobid')
    final Blob blob = mock(Blob.class)
    when(blob.getMetrics()).thenReturn(blobMetrics)
    when(blob.getId()).thenReturn(blobId)

    final BlobStore blobStore = [
        getBlobStoreConfiguration: { blobStoreConfiguration },
        create                   : { InputStream is, Map<String, String> map -> is.text; blob }
    ] as BlobStore

    final BlobTx testSubject = new BlobTx(nodeAccess, blobStore)
    final AssetBlob assetBlob = testSubject.create(inputStream, [:], [SHA1], contentType)

    assertThat(assetBlob.contentType, is(equalTo(contentType)))
    assertThat(assetBlob.size, is(equalTo(blobSize)))
    assertThat(assetBlob.hashesVerified, is(true))
    assertThat(assetBlob.hashes, hasEntry(SHA1, HashCode.fromString('6adfb183a4a2c94a2f92dab5ade762a47889a5a1')))
    assertThat(assetBlob.blob, is(blob))
  }

  @Test
  void 'create blob from hard link without verified hashes'() {

    final Path path = mock(Path.class)
    final long blobSize = 1L
    final String contentType = 'text/plain'
    final Map<String, String> headers = [:]
    final Map<HashAlgorithm, HashCode> hashes = [(SHA1): HashCode.fromString('356a192b7913b04c54574d18c28d46e6395428ab')]

    final NodeAccess nodeAccess = mock(NodeAccess.class)
    when(nodeAccess.getId()).thenReturn('id')

    final BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class)
    when(blobStoreConfiguration.getName()).thenReturn('blobStoreConfiguration')

    final BlobMetrics blobMetrics = mock(BlobMetrics.class)
    when(blobMetrics.getContentSize()).thenReturn(blobSize)

    final BlobId blobId = new BlobId('blobid')
    final Blob blob = mock(Blob.class)
    when(blob.getMetrics()).thenReturn(blobMetrics)
    when(blob.getId()).thenReturn(blobId)

    final BlobStore blobStore = [
        getBlobStoreConfiguration: { blobStoreConfiguration },
        create                   : { Path p, Map<String, String> map, long size, HashCode sha1 -> blob }
    ] as BlobStore

    final BlobTx testSubject = new BlobTx(nodeAccess, blobStore)
    final AssetBlob assetBlob = testSubject.createByHardLinking(path, headers, hashes, contentType, blobSize)

    assertThat(assetBlob.contentType, is(equalTo(contentType)))
    assertThat(assetBlob.size, is(equalTo(blobSize)))
    assertThat(assetBlob.hashesVerified, is(false))
    assertThat(assetBlob.hashes, hasEntry(SHA1, HashCode.fromString('356a192b7913b04c54574d18c28d46e6395428ab')))
    assertThat(assetBlob.blob, is(blob))
  }

  @Test
  void 'create blob from existing blob'() {

    long blobSize = 1L
    String contentType = 'text/plain'
    Map<String, String> headers = [(CONTENT_TYPE_HEADER): contentType]
    Map<HashAlgorithm, HashCode> hashes = [(SHA1): HashCode.fromString('356a192b7913b04c54574d18c28d46e6395428ab')]

    NodeAccess nodeAccess = mock(NodeAccess.class)
    when(nodeAccess.getId()).thenReturn('id')

    BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class)
    when(blobStoreConfiguration.getName()).thenReturn('blobStoreConfiguration')

    BlobMetrics blobMetrics = mock(BlobMetrics.class)
    when(blobMetrics.getContentSize()).thenReturn(blobSize)
    when(blobMetrics.getSha1Hash()).thenReturn('356a192b7913b04c54574d18c28d46e6395428ab')

    BlobId blobId = new BlobId('blobid')
    Blob blob = mock(Blob.class)
    when(blob.getMetrics()).thenReturn(blobMetrics)
    when(blob.getId()).thenReturn(blobId)

    BlobStore blobStore = [
        getBlobStoreConfiguration: { blobStoreConfiguration },
        get                      : { BlobId id -> blob },
        copy                     : { BlobId id, Map<String, String> map -> blob }
    ] as BlobStore

    BlobTx testSubject = new BlobTx(nodeAccess, blobStore)
    AssetBlob assetBlob = testSubject.createByCopying(blobId, headers, hashes, true)

    assertThat(assetBlob.contentType, is(equalTo(contentType)))
    assertThat(assetBlob.size, is(equalTo(blobSize)))
    assertThat(assetBlob.hashesVerified, is(true))
    assertThat(assetBlob.hashes, hasEntry(SHA1, HashCode.fromString('356a192b7913b04c54574d18c28d46e6395428ab')))
    assertThat(assetBlob.blob, is(blob))
  }
}
