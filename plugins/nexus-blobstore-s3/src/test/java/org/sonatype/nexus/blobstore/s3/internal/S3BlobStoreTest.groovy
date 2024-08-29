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
package org.sonatype.nexus.blobstore.s3.internal

import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.stream.Collectors

import org.sonatype.nexus.blobstore.BlobIdLocationResolver
import org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.Blob
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobMetrics
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaUsageChecker
import org.sonatype.nexus.blobstore.s3.internal.datastore.DatastoreS3BlobStoreMetricsService
import org.sonatype.nexus.common.log.DryRunPrefix

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.DeleteObjectsResult
import com.amazonaws.services.s3.model.DeleteObjectsResult.DeletedObject
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.amazonaws.services.s3.model.S3ObjectSummary
import spock.lang.Specification
import spock.lang.Unroll

import static java.util.concurrent.Executors.newFixedThreadPool
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_FILE_ATTRIBUTES_SUFFIX
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_FILE_CONTENT_SUFFIX
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_IP_HEADER
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER

/**
 * {@link S3BlobStore} tests.
 */
class S3BlobStoreTest
    extends Specification
{

  AmazonS3Factory amazonS3Factory = Mock()

  BlobIdLocationResolver locationResolver = new DefaultBlobIdLocationResolver()

  S3Uploader uploader = Mock()

  S3Copier copier =  Mock()

  DatastoreS3BlobStoreMetricsService storeMetrics = Mock()

  BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker = Mock()

  DryRunPrefix dryRunPrefix = Mock()

  BucketManager bucketManager = Mock()

  AmazonS3 s3 = Mock()

  S3BlobStore blobStore = new S3BlobStore(amazonS3Factory, locationResolver, uploader, copier, false, false, false,
      storeMetrics, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker)

  def config = new MockBlobStoreConfiguration()

  def attributesContents = """\
        |#Thu Jun 01 23:10:55 UTC 2017
        |@BlobStore.created-by=admin
        |size=11
        |@Bucket.repo-name=test
        |creationTime=1496358655289
        |@BlobStore.content-type=text/plain
        |@BlobStore.blob-name=test
        |sha1=eb4c2a5a1c04ca2d504c5e57e1f88cef08c75707
      """.stripMargin()

  def setup() {
    amazonS3Factory.create(_) >> s3
    config.attributes = [s3: [bucket: 'mybucket', prefix: 'myPrefix']]
  }

  @Unroll
  def 'getBlobIdStream works with prefix #prefix'() {
    given: 'prefix setup'
      def cfg = new MockBlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: prefix]]
      blobStore.init(cfg)
      blobStore.doStart()

    when: 'blob id stream is fetched'
      def blobIdStream = blobStore.getBlobIdStream()
      def count = blobIdStream.collect(Collectors.toList()).size()

    then: 'the correct request is made'
      count == 1
      1 * s3.listObjects(_) >> { listObjectsRequest ->
        assert listObjectsRequest[0].getPrefix() == expected

        def listing = new ObjectListing()
        listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: expected + 'vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties')
        listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: expected + 'vol-01/chap-01/12345678-1234-1234-1234-123456789abc.bytes')
        listing.truncated = false

        return listing
      }
      1 * s3.getObjectMetadata('mybucket', _) >> new ObjectMetadata()

    where:
      prefix     | expected
      'myPrefix' | 'myPrefix/content/'
      ''         | 'content/'
      null       | 'content/'

  }

  def 'getBlobIdUpdatedSinceStream filters out of date content'() {
    given: 'setup blobstore'
      blobStore.init(config)
      blobStore.doStart()

      s3.listObjects(_ as ListObjectsRequest) >> { listObjectsRequest ->

        def listing = new ObjectListing()
        listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: '/content/vol-01/chap-01/12345678-1234-1234-1234-123456789ghi.properties', lastModified: new Date())
        listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: '/content/vol-01/chap-01/12345678-1234-1234-1234-123456789ghi.bytes', lastModified: new Date())
        listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: 'vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties', lastModified: new Date())
        listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: 'vol-01/chap-01/12345678-1234-1234-1234-123456789abc.bytes', lastModified: new Date())
        listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: 'vol-01/chap-01/12345678-1234-1234-1234-123456789def.properties', lastModified: new Date(System.currentTimeMillis() - 2))
        listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: 'vol-01/chap-01/12345678-1234-1234-1234-123456789def.bytes', lastModified: new Date(System.currentTimeMillis() - 2))
        listing.truncated = false

        return listing
      }

    when: 'blob id stream is fetched only wanting blobs updated in the last day'
      List<BlobId> blobIds = blobStore.getBlobIdUpdatedSinceStream(Duration.ofDays(1L)).collect(Collectors.toList())

    then: 'only the blob updated in the last day will be returned'
      blobIds.size() == 1
      blobIds.get(0).asUniqueString() == "12345678-1234-1234-1234-123456789abc"
      1 * s3.getObjectMetadata('mybucket', '/content/vol-01/chap-01/12345678-1234-1234-1234-123456789ghi.properties') >> getTempBlobMetadata()
      1 * s3.getObjectMetadata('mybucket', 'vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties') >> new ObjectMetadata()
  }

  def 'getBlobIdUpdatedSinceStream throws exception if negative sinceDays is passed in'() {
    given: 'setup blobstore'
      blobStore.init(config)
      blobStore.doStart()

    when: 'blob id stream is fetched'
      blobStore.getBlobIdUpdatedSinceStream(Duration.ofDays(-1L))

    then: 'fails'
      thrown(IllegalArgumentException.class)
  }

  @Unroll
  def "Get blob with bucket prefix #prefix"() {
    given: 'A mocked S3 setup'
      def cfg = new MockBlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: prefix]]
      def pathPrefix = prefix ? (prefix + "/") : ""

      def blobId = new BlobId('test')
      def attributesS3Object = mockS3Object(attributesContents)
      def contentS3Object = mockS3Object('hello world')
      1 * bucketManager.prepareStorageLocation(cfg)
      1 * s3.doesObjectExist('mybucket', pathPrefix + 'metadata.properties') >> false
      1 * s3.doesObjectExist('mybucket', pathPrefix + propertiesLocation(blobId)) >> true
      1 * s3.getObject('mybucket', pathPrefix + propertiesLocation(blobId)) >> attributesS3Object
      1 * s3.getObject('mybucket', pathPrefix + bytesLocation(blobId)) >> contentS3Object

    when: 'An existing blob is read'
      blobStore.init(cfg)
      blobStore.doStart()
      def blob = blobStore.get(blobId)

    then: 'The contents are read from s3'
      blob.inputStream.text == 'hello world'

    where:
      prefix   | _
      null     | _
      ""       | _
      "prefix" | _
  }

  @Unroll
  def 'soft delete successful with bucket prefix #prefix'() {
    given: 'blob exists'
      def blobId = new BlobId('soft-delete-success')
      def cfg = new MockBlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: prefix]]
      def pathPrefix = prefix ? (prefix + "/") : ""

      blobStore.init(cfg)
      blobStore.doStart()
      def attributesS3Object = mockS3Object(attributesContents)
      2 * s3.doesObjectExist('mybucket', pathPrefix + propertiesLocation(blobId)) >> true
      2 * s3.getObject('mybucket', pathPrefix + propertiesLocation(blobId)) >> attributesS3Object

    when: 'blob is deleted'
      def deleted = blobStore.delete(blobId, 'successful test')

    then: 'deleted tag is added'
      deleted == true
      1 * s3.setObjectTagging(_) >> { args ->
        assert args[0].getKey().endsWith(BLOB_FILE_CONTENT_SUFFIX) == true
        assert args[0].getTagging().getTagSet() == [S3BlobStore.DELETED_TAG]
      }
      1 * s3.setObjectTagging(_) >> { args ->
        assert args[0].getKey().endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX) == true
        assert args[0].getTagging().getTagSet() == [S3BlobStore.DELETED_TAG]
      }

    where:
      prefix   | _
      null     | _
      ""       | _
      "prefix" | _
  }

  def 'soft delete returns false when blob does not exist'() {
    given: 'blob store setup'
      blobStore.init(config)
      blobStore.doStart()

    when: 'nonexistent blob is deleted'
      def deleted = blobStore.delete(new BlobId('soft-delete-fail'), 'test')

    then: 'deleted tag is added'
      deleted == false
      0 * s3.setObjectTagging(!null)
  }

  @Unroll
  def 'delete is hard when expiry days is 0 (expiry days = #expiryDays)'() {
    given: 'blob store setup'
      def blobId = new BlobId('some-blob')
      def cfg = new MockBlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: '']]
      def attributesS3Object = mockS3Object(attributesContents)
      _ * s3.doesObjectExist('mybucket', propertiesLocation(blobId)) >> true
      _ * s3.getObject('mybucket', propertiesLocation(blobId)) >> attributesS3Object

      def deleteObjectsResult = Mock(DeleteObjectsResult.class)
      _ * deleteObjectsResult.getDeletedObjects() >> [Mock(DeletedObject.class), Mock(DeletedObject.class)]

    when: 'blob is deleted with given lifecycle expiry days'
      cfg.attributes('s3').set('expiration', expiryDays)
      blobStore.init(cfg)
      blobStore.doStart()
      blobStore.delete(blobId, 'just a test')

    then: 'blob is tagged or deleted correctly'
      deletions * s3.deleteObjects(_ as DeleteObjectsRequest) >> deleteObjectsResult
      tags * s3.setObjectTagging(_)

    where:
      expiryDays || deletions | tags
      -1         || 0         | 2
      0          || 1         | 0
      1          || 0         | 2
      2          || 0         | 2
  }

  def 'undelete successful'() {
    given: 'blob store setup'
      Properties properties = ['@BlobStore.blob-name': 'my-blob']
      S3BlobAttributes blobAttributes = Mock()
      2 * blobAttributes.getProperties() >> properties
      2 * blobAttributes.isDeleted() >> true
      BlobStoreUsageChecker usageChecker = Mock()
      2 * usageChecker.test(*_) >> true
      blobStore.init(config)
      blobStore.doStart()

    when: 'blob is restored in dry run mode'
      def restored = blobStore.undelete(usageChecker, new BlobId('restore-succeed'), blobAttributes, true)

    then: 'blob attributes are not modified'
      restored == true
      0 * blobAttributes.setDeleted(false)
      0 * blobAttributes.setDeletedReason(null)
      0 * s3.setObjectTagging(_)

    when: 'blob is restored for real'
      restored = blobStore.undelete(usageChecker, new BlobId('restore-succeed'), blobAttributes, false)

    then: 'deleted attribute and deleted s3 tag are removed'
      restored == true
      1 * blobAttributes.getMetrics() >> Mock(BlobMetrics)
      1 * blobAttributes.setDeleted(false)
      1 * blobAttributes.setDeletedReason(null)
      1 * s3.setObjectTagging(_) >> { args ->
        assert args[0].getKey().endsWith(BLOB_FILE_CONTENT_SUFFIX) == true
        assert args[0].getTagging().getTagSet().isEmpty()
      }
      1 * s3.setObjectTagging(_) >> { args ->
        assert args[0].getKey().endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX) == true
        assert args[0].getTagging().getTagSet().isEmpty()
      }
  }

  def 'start will accept a metadata.properties originally created with file blobstore'() {
    given: 'metadata.properties comes from a file blobstore'
      1 * s3.doesObjectExist('mybucket', 'myPrefix/metadata.properties') >> true
      1 * s3.getObject('mybucket', 'myPrefix/metadata.properties') >> mockS3Object('type=file/1')

    when: 'doStart is called'
      blobStore.init(config)
      blobStore.doStart()

    then: 'blobstore is started'
     notThrown(IllegalStateException)
  }

  def 'start rejects a metadata.properties containing something other than file or s3 type'() {
    given: 'metadata.properties comes from some unknown blobstore'
      1 * s3.doesObjectExist('mybucket', 'myPrefix/metadata.properties') >> true
      1 * s3.getObject('mybucket', 'myPrefix/metadata.properties') >> mockS3Object('type=other/12')

    when: 'doStart is called'
      blobStore.init(config)
      blobStore.doStart()

    then: 'blobstore fails to start'
      thrown(IllegalStateException)
  }

  def 'remove bucket error throws exception'() {
    given: 'blob store setup'
      blobStore.init(config)
      blobStore.doStart()
      def s3Exception = new AmazonS3Exception("error")
      s3Exception.errorCode = "UnknownError"

    when: 'blobstore is removed'
      blobStore.remove()

    then: 'exception is thrown'
      thrown(BlobStoreException)
      1 * s3.listObjects('mybucket', 'myPrefix/content/') >> new ObjectListing()
      1 * storeMetrics.remove()
      1 * s3.deleteObject('mybucket', 'myPrefix/metadata.properties')
      1 * bucketManager.deleteStorageLocation(config) >> { args -> throw s3Exception }
  }

  def 'remove non-empty bucket generates warning only'() {
    given: 'blob store setup'
      blobStore.init(config)
      blobStore.doStart()
      def s3Exception = new AmazonS3Exception("error")
      s3Exception.errorCode = "BucketNotEmpty"

    when: 'blobstore is removed'
      blobStore.remove()

    then: 'exception is not thrown'
      notThrown(BlobStoreException)
      1 * s3.listObjects('mybucket', 'myPrefix/content/') >> new ObjectListing()
      1 * storeMetrics.remove()
      1 * s3.deleteObject('mybucket', 'myPrefix/metadata.properties')
      1 * bucketManager.deleteStorageLocation(config) >> { args -> throw s3Exception }
  }

  def 'removing non-empty blob store removes lifecycle policy'() {
    given: 'blob store setup'
      blobStore.init(config)
      blobStore.doStart()

    when: 'blobstore is removed'
      blobStore.remove()

    then: 'only the lifecycle policy is removed'
      1 * s3.listObjects('mybucket', 'myPrefix/content/') >> Mock(ObjectListing) {
        getObjectSummaries() >> [Mock(S3ObjectSummary)]
      }
      0 * s3.deleteObject('mybucket', 'myPrefix/metadata.properties')
      0 * bucketManager.deleteStorageLocation(config)
      1 * s3.deleteBucketLifecycleConfiguration('mybucket')
  }

  @Unroll
  def 'bucket name regex validates #name as #validity'() {
    // these rules are documented here:
    // http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html
    // http://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-s3-bucket-naming-requirements.html
    when:
      def matches = name ==~ S3BlobStore.BUCKET_REGEX

    then:
      matches == validity

    where:
      name            || validity
      ''              || false
      'ab'            || false // too short
      'abc'           || true
      '0123456789'    || true
      'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz01234567890' || true
      'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz012345678901' || false // too long
      'foo.bar'       || true
      'foo-bar'       || true
      'foo.bar-blat'  || true
      'foo..bar'      || false // can't have consecutive periods
      '.foobar'       || false // can't begin with period
      'foo.-bar'      || false // can't have period dash
      'foo-.bar'      || false // can't have dash period
      'foobar-'       || false // can't end with dash
      'foobar.'       || false // can't end with period
      '01234.56789'   || true
      '127.0.0.1'     || false // can't look like ip addr
  }

  def 'create direct path blob'() {
    given: 'blob store setup'
      def expectedBytesPath = 'myPrefix/content/directpath/foo/bar/myblob.bytes'
      def expectedPropertiesPath = 'myPrefix/content/directpath/foo/bar/myblob.properties'
      blobStore.init(config)
      blobStore.doStart()

    when: 'a direct path blob is created'
      def blob = blobStore.create(new ByteArrayInputStream('hello world'.bytes),
          ['BlobStore.direct-path': 'true', 'BlobStore.blob-name': 'foo/bar/myblob', 'BlobStore.created-by': 'test'])

    then: 'it gets uploaded into the proper location'
      1 * s3.putObject('mybucket', expectedPropertiesPath, _, _)
      1 * uploader.upload(_, 'mybucket', expectedBytesPath, _)

    when: 'direct path blobs are listed'
      def listing = new ObjectListing()
      listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: expectedPropertiesPath)
      listing.objectSummaries << new S3ObjectSummary(bucketName: 'mybucket', key: expectedBytesPath)
      s3.listObjects(_) >> listing
      def blobIdStream = blobStore.getDirectPathBlobIdStream('foo/bar')

    then: 'the correct blob is returned'
      blobIdStream.collect(Collectors.toList()) == [blob.id]
  }

  def "A S3 blob store is writable when the client can verify that the bucket exists"() {
    when:
      1 * s3.doesBucketExistV2("mybucket") >> { response.call() }
      blobStore.init(config)
      blobStore.doStart()
    then:
      blobStore.isStorageAvailable() == expectedAvailability
    where:
      expectedAvailability | response
      true                 | { return true }
      false                | { return false }
      false                | { throw new SdkClientException("Fake error") }
  }

  def "expiry test"(){
    given: 'blob exists'
      def expiryPreferredBlobStore = new S3BlobStore(amazonS3Factory, locationResolver, uploader, copier, true, false, false,
          storeMetrics, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker)

      def blobId = new BlobId('soft-delete-success')
      def cfg = new MockBlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: prefix]]
      def pathPrefix = prefix ? (prefix + "/") : ""

      expiryPreferredBlobStore.init(cfg)
      expiryPreferredBlobStore.doStart()
      def attributesS3Object = mockS3Object(attributesContents)
      2 * s3.doesObjectExist('mybucket', pathPrefix + propertiesLocation(blobId)) >> true
      2 * s3.getObject('mybucket', pathPrefix + propertiesLocation(blobId)) >> attributesS3Object

    when: 'blob is deleted'
      def deleted = expiryPreferredBlobStore.deleteHard(blobId)

    then: 'the delete is not actual called'
      deleted
      0 * s3.deleteObject(_ as String, _ as String)

    where:
      prefix   | _
      null     | _
      ""       | _
      "prefix" | _
  }

  def "hard delete hard deletes when prefered"(){
    given: 'blob exists'
      def hardDeleteStore = new S3BlobStore(amazonS3Factory, locationResolver, uploader, copier, true, true, false,
          storeMetrics, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker)

      def blobId = new BlobId('soft-delete-success')
      def cfg = new MockBlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: prefix]]
      def pathPrefix = prefix ? (prefix + "/") : ""

      def deleteObjectsResult = Mock(DeleteObjectsResult.class)
      _ * deleteObjectsResult.getDeletedObjects() >> [Mock(DeletedObject.class), Mock(DeletedObject.class)]

      hardDeleteStore.init(cfg)
      hardDeleteStore.doStart()
      def attributesS3Object = mockS3Object(attributesContents)
      1 * s3.doesObjectExist('mybucket', pathPrefix + propertiesLocation(blobId)) >> true
      1 * s3.getObject('mybucket', pathPrefix + propertiesLocation(blobId)) >> attributesS3Object

    when: 'blob is deleted'
      def deleted = hardDeleteStore.deleteHard(blobId)

    then: 'the blob and props are really deleted'
      deleted
      1 * s3.deleteObjects(_ as DeleteObjectsRequest) >> deleteObjectsResult

    where:
      prefix   | _
      null     | _
      ""       | _
      "prefix" | _
  }

  def "regular delete hard deletes when prefered"(){
    given: 'blob exists'
      def hardDeleteStore = new S3BlobStore(amazonS3Factory, locationResolver, uploader, copier, true, true, false,
          storeMetrics, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker)

      def blobId = new BlobId('soft-delete-success')
      def cfg = new MockBlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: prefix]]
      def pathPrefix = prefix ? (prefix + "/") : ""

      def deleteObjectsResult = Mock(DeleteObjectsResult.class)
      _ * deleteObjectsResult.getDeletedObjects() >> [Mock(DeletedObject.class), Mock(DeletedObject.class)]

      hardDeleteStore.init(cfg)
      hardDeleteStore.doStart()
      def attributesS3Object = mockS3Object(attributesContents)
      1 * s3.doesObjectExist('mybucket', pathPrefix + propertiesLocation(blobId)) >> true
      1 * s3.getObject('mybucket', pathPrefix + propertiesLocation(blobId)) >> attributesS3Object

    when: 'blob is deleted'
      def deleted = hardDeleteStore.delete(blobId, "testDelete")

    then: 'the blob and props are really deleted'
      deleted
      1 * s3.deleteObjects(_ as DeleteObjectsRequest) >> deleteObjectsResult

    where:
      prefix   | _
      null     | _
      ""       | _
      "prefix" | _
  }

  def "Concurrent attempts to refresh blob should never return null"() {
    given: 'A mocked S3 setup'
      def cfg = new MockBlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket']]

      def blobId = new BlobId('test')
      def attributesS3Object = mockS3Object(attributesContents)
      def contentS3Object = mockS3Object('hello world')
      _ * bucketManager.prepareStorageLocation(cfg)
      _ * s3.doesObjectExist('mybucket', propertiesLocation(blobId)) >> true
      _ * s3.getObject('mybucket', propertiesLocation(blobId)) >> attributesS3Object
      _ * s3.getObject('mybucket', bytesLocation(blobId)) >> contentS3Object

    when: 'Existing stale blobs are read concurrently'
      blobStore.init(cfg)
      blobStore.doStart()
      def executorService = newFixedThreadPool(2)
      def callable = { blobStore.get(blobId) }
      def results = ([executorService.submit(callable as Callable<Blob>),
                      executorService.submit(callable as Callable<Blob>)] as List<Future<Blob>>)

      executorService.shutdown()
    then: 'The blobs are not null'
      results.get(0).get() != null
      results.get(1).get() != null
  }

  def 'create does not create temp blobs with tmp$ blob id'() {
    given: 'blob store setup'
      blobStore.init(config)
      blobStore.doStart()

    when: 'a tempblob is created'
      def headers = [(CREATED_BY_HEADER): 'test', (CREATED_BY_IP_HEADER): '127.0.0.1',
                     (BLOB_NAME_HEADER) : 'temp', (TEMPORARY_BLOB_HEADER): '']
      def blob = blobStore.create(new ByteArrayInputStream('hello world'.bytes),
          headers)

    then: 'the blob id is not a tmp id'
      !blob.id.asUniqueString().startsWith('tmp$')

    and: 'correct headers are present'
      blob.headers == headers

    and: 'temp blob UserMetaData is present'
      1 * s3.putObject('mybucket', _, _, _) >> { bucket, key, bytes, metadata ->
        ObjectMetadata objectMetadata = metadata
        assert objectMetadata.userMetadata.get(TEMPORARY_BLOB_HEADER) == 'true'
      }
    when: 'the blob is made permanent'
      headers.remove(TEMPORARY_BLOB_HEADER)
      headers.putAll(
          [(BLOB_NAME_HEADER): 'file.txt', (CONTENT_TYPE_HEADER): 'text/plain', (REPO_NAME_HEADER): 'a repository'])
      blob = blobStore.makeBlobPermanent(blob.id, headers)

    then: 'correct headers are present'
      blob.headers == headers

    and: 'temp blob UserMetaData is not present'
      1 * s3.putObject('mybucket', _, _, _) >> { bucket, key, bytes, metadata ->
        ObjectMetadata objectMetadata = metadata
        assert objectMetadata.userMetadata.get(TEMPORARY_BLOB_HEADER) == null
      }
  }

  def 'makeBlobPermanent throws exception if temp blob header is passed in'() {
    given: 'blob store setup'
      blobStore.init(config)
      blobStore.doStart()

    when: 'a tempblob is created'
      def headers = [(CREATED_BY_HEADER): 'test', (CREATED_BY_IP_HEADER): '127.0.0.1',
                     (BLOB_NAME_HEADER) : 'temp', (TEMPORARY_BLOB_HEADER): '']
      def blob = blobStore.create(new ByteArrayInputStream('hello world'.bytes),
          headers)

    and: 'makePermanent is called with temp blob header present'
      blobStore.makeBlobPermanent(blob.id, headers)

    then: 'An IllegalArgumentException is thrown'
      thrown(IllegalArgumentException.class)
  }

  def 'deleteIfTemp deletes blob when temp blob header is present'() {
    given: 'blob store setup'
      blobStore.init(config)
      blobStore.doStart()

      def deleteObjectsResult = Mock(DeleteObjectsResult.class)
      _ * deleteObjectsResult.getDeletedObjects() >> [Mock(DeletedObject.class), Mock(DeletedObject.class)]

    when: 'a tempblob is created'
      def headers = [(CREATED_BY_HEADER): 'test', (CREATED_BY_IP_HEADER): '127.0.0.1',
                     (BLOB_NAME_HEADER) : 'temp', (TEMPORARY_BLOB_HEADER): '']
      def blob = blobStore.create(new ByteArrayInputStream('hello world'.bytes),
          headers)
    then:
      blob != null

    when: 'deleteIfTemp is called'
      def deleted = blobStore.deleteIfTemp(blob.id)

    then: 'the blob is deleted'
      deleted
      blobStore.get(blob.id) == null
      1 * s3.deleteObjects(_ as DeleteObjectsRequest) >> deleteObjectsResult
  }

  def 'deleteIfTemp does not delete blob when temp blob header is absent'() {
    given: 'blob store setup'
      blobStore.init(config)
      blobStore.doStart()

    when: 'a tempblob is created'
      def headers = [(CREATED_BY_HEADER)  : 'test', (CREATED_BY_IP_HEADER): '127.0.0.1',
                     (BLOB_NAME_HEADER)   : 'file.txt', (CONTENT_TYPE_HEADER): 'text/plain', (
                         REPO_NAME_HEADER): 'a repository']
      def blob = blobStore.create(new ByteArrayInputStream('hello world'.bytes),
          headers)

    then:
      blob != null

    and: 'deleteIfTemp is called'
      def deleted = blobStore.deleteIfTemp(blob.id)

    then: 'the blob is not deleted'
      !deleted
      blobStore.get(blob.id) != null
      0 * s3.deleteObjects(_)
  }

  private mockS3Object(String contents) {
    S3Object s3Object = Mock()
    s3Object.getObjectContent() >> new S3ObjectInputStream(new ByteArrayInputStream(contents.bytes), null)
    s3Object
  }

  private String propertiesLocation(BlobId blobId) {
    "content/${locationResolver.permanentLocationStrategy.location(blobId)}.properties"
  }

  private String bytesLocation(BlobId blobId) {
    "content/${locationResolver.permanentLocationStrategy.location(blobId)}.bytes"
  }

  private static ObjectMetadata getTempBlobMetadata() {
    ObjectMetadata tempBlobMetaData = new ObjectMetadata()
    tempBlobMetaData.addUserMetadata(TEMPORARY_BLOB_HEADER, 'true')
    tempBlobMetaData
  }
}
