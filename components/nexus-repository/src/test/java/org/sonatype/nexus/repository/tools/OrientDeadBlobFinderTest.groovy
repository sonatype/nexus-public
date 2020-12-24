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
package org.sonatype.nexus.repository.tools

import org.sonatype.nexus.blobstore.api.Blob
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobMetrics
import org.sonatype.nexus.blobstore.api.BlobRef
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.common.entity.DetachedEntityMetadata
import org.sonatype.nexus.common.entity.DetachedEntityVersion
import org.sonatype.nexus.common.hash.HashAlgorithm
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.AssetEntityAdapter
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.repository.tools.orient.OrientDeadBlobFinder

import java.util.function.Supplier
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.apache.tika.io.IOUtils
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static org.sonatype.nexus.repository.tools.ResultState.ASSET_DELETED
import static org.sonatype.nexus.repository.tools.ResultState.DELETED
import static org.sonatype.nexus.repository.tools.ResultState.MISSING_BLOB_REF
import static org.sonatype.nexus.repository.tools.ResultState.SHA1_DISAGREEMENT
import static org.sonatype.nexus.repository.tools.ResultState.UNAVAILABLE_BLOB

class OrientDeadBlobFinderTest
    extends Specification
{
  Repository repository = Mock()

  StorageFacet storageFacet = Mock()

  StorageTx tx = Mock()

  NestedAttributesMap attributes = Mock()

  Blob blob = Mock()

  BlobRef blobRef = Mock()

  AssetEntityAdapter assetEntityAdapter = Mock()

  ODatabaseDocumentTx db = Mock()

  @Shared
  InputStream blobStream = IOUtils.toInputStream('foo')

  @Shared
  Asset asset

  @Shared
  BlobMetrics blobMetrics = new BlobMetrics(DateTime.now(), '1234', 1234)

  @Subject
  DeadBlobFinder deadBlobFinder = new OrientDeadBlobFinder(assetEntityAdapter)

  def setup() {
    asset = createAsset()
    repository.name >> 'bar'
  }

  Asset createAsset() {
    Asset asset = new Asset()
    asset.attributes(attributes)
    asset.name('foo')
    asset.blobRef(blobRef)
    asset.entityMetadata = new DetachedEntityMetadata(new DetachedEntityId('foo'), new DetachedEntityVersion('1'))
    return asset
  }

  def 'no errors if blob exists and matches checksum'() {
    when: 'On the happy path'
      def result = deadBlobFinder.find(repository)

    then: 'no errors are surfaced'
      interaction {
        mockTxState()
        mockAssetBrowse()
        commonBlobMock()
      }
      result.isEmpty()
  }

  def 'errors if blob sha1 disagrees with db'() {
    when: 'asset hash is in disagreement with blob'
      def result = deadBlobFinder.find(repository)

    then: 'an error is returned'
      interaction {
        mockTxState()
        mockAssetBrowse()
        mockAssetReload()
      }
      2 * attributes.child('checksum') >> attributes
      2 * attributes.get(HashAlgorithm.SHA1.name(), String) >> '1235'
      2 * tx.requireBlob(_) >> blob
      2 * blob.metrics >> blobMetrics
      !result.isEmpty()
      result[0].asset.name() == 'foo'
      result[0].resultState == SHA1_DISAGREEMENT
  }

  def 'errors if blob InputStream is not available'() {
    given: 'An inputstream which is not available'
      InputStream is = Mock()

    when:
      def result = deadBlobFinder.find(repository)

    then:
      interaction {
        mockTxState()
        mockAssetBrowse()
        commonBlobMock(2, is)
        mockAssetReload()
      }
      2 * is.available() >> 0
      !result.isEmpty()
      result[0].asset.name() == 'foo'
      result[0].resultState == UNAVAILABLE_BLOB
  }

  def 'do not check availability of InputStream if content length is zero'() {
    when: 'Our metrics indicate that a Blob has zero content length'
      def result = deadBlobFinder.find(repository)

    then: 'we do not bother to check availability'
      interaction {
        mockTxState()
        mockAssetBrowse()
        commonBlobMock(1, blobStream, new BlobMetrics(DateTime.now(), '1234', 0))
      }
      0 * blobStream.available() // never called as zero length blob would return 0 correctly
      result.isEmpty()
  }

  def 'missing Asset.blobRef is an error if not ignored'() {
    given: 'An asset with missing BlobRef'
      asset.blobRef(null)

    when: 'we elect not to ignore missing blobs'
      def result = deadBlobFinder.find(repository, false)

    then: 'an error result is returned'
      interaction {
        mockTxState()
        mockAssetBrowse()
        mockAssetReload()
      }
      !result.isEmpty()
      result[0].asset.name() == 'foo'
      result[0].resultState == MISSING_BLOB_REF
      result[0].errorMessage == 'Missing property: blob_ref'
  }

  def 'missing Asset.blobRef is not an error if ignored (nuget case)'() {
    given: 'An asset with missing BlobRef'
      asset.blobRef(null)

    when: 'we elect to ignore missing blobs'
      def result = deadBlobFinder.find(repository, true)

    then: 'no error result is returned'
      interaction {
        mockTxState()
        mockAssetBrowse()
      }
      result.isEmpty()
  }

  def 'passing in a null repository results in an error'() {
    when: 'A null repository is passed in '
      deadBlobFinder.find(null)

    then: 'an exception is thrown before interacting with the persistence layer at all'
      thrown(NullPointerException)
      0 * _
  }

  def 'an asset can be deleted while the system is inspected'() {
    given: 'An InputStream with a problem reading'
      InputStream is = Mock()

    when: 'We encounter an Asset with a problem on the first pass'
      def result = deadBlobFinder.find(repository)

    then: 'The Asset is reported as deleted'
      interaction {
        mockTxState()
        mockAssetBrowse()
        commonBlobMock(1, is)
        mockAssetReload(null)
      }
      is.available() >> { throw new IOException('cannot read inputstream') }
      !result.isEmpty()
      result[0].asset == null
      result[0].resultState == ASSET_DELETED
  }

  def 'an asset blob can be deleted while the system is inspected'() {
    given: 'A missing blobRef on the first pass and a missing file on the second'
      asset.blobRef(null) // first pass we have a missing blobRef
      Asset reloadedAsset = createAsset()
      Blob reloadedBlob = Mock() // second pass the blobRef is there but file does not exist

    when: 'We encounter an Asset with a missing blob file on the second pass'
      def result = deadBlobFinder.find(repository, false)

    then: 'The Asset is reported as having the associated Blob deleted'
      interaction {
        mockTxState()
        mockAssetBrowse()
        mockAssetReload(reloadedAsset)
      }
      1 * tx.requireBlob(_) >> reloadedBlob
      1 * reloadedBlob.metrics >> blobMetrics
      1 * attributes.child('checksum') >> attributes
      1 * attributes.get(HashAlgorithm.SHA1.name(), String) >> '1234'
      1 * reloadedBlob.inputStream >> { throw new BlobStoreException('Blob has been deleted', new BlobId('foo')) }
      !result.isEmpty()
      result[0].asset.name() == 'foo'
      result[0].resultState == DELETED
  }

  /**
   * Verify that begin/close are always called.
   */
  private void mockTxState() {
    1 * tx.begin()
    1 * tx.close()
  }

  /**
   * Verify one pass over all Assets.
   * @param assets
   */
  private void mockAssetBrowse(List<Asset> assets = [asset]) {
    1 * repository.facet(StorageFacet) >> storageFacet
    1 * storageFacet.txSupplier() >> ({ -> tx } as Supplier<StorageTx>)
    1 * tx.browseAssets(_) >> assets
  }

  /**
   * Verify the reload of one particular Asset.
   * @param reloadedAsset the Asset to be individually loaded
   */
  private void mockAssetReload(Asset reloadedAsset = asset) {
    1 * tx.db >> db
    1 * assetEntityAdapter.read(_, _) >> reloadedAsset
  }

  /**
   * Helper to parameterize mock configuration of Blob access.
   */
  private void commonBlobMock(int passes = 1, stream = blobStream, BlobMetrics metrics = blobMetrics,
                              String sha1 = '1234')
  {
    passes * tx.requireBlob(_) >> blob
    passes * attributes.child('checksum') >> attributes
    passes * attributes.get(HashAlgorithm.SHA1.name(), String) >> sha1
    passes * blob.metrics >> metrics
    passes * blob.inputStream >> stream
  }
}
