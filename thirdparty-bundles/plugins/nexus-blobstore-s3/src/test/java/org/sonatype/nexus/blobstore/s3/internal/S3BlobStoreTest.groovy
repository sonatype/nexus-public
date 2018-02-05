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

import org.sonatype.nexus.blobstore.BlobIdLocationResolver
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.amazonaws.services.s3.model.StorageClass
import spock.lang.Specification

import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.BLOB_ATTRIBUTE_SUFFIX
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.BLOB_CONTENT_SUFFIX

/**
 * {@link S3BlobStore} tests.
 */
class S3BlobStoreTest
    extends Specification
{

  AmazonS3Factory amazonS3Factory = Mock()

  BlobIdLocationResolver locationResolver = Mock()

  S3BlobStoreMetricsStore storeMetrics = Mock()

  AmazonS3 s3 = Mock()

  S3BlobStore blobStore = new S3BlobStore(amazonS3Factory, locationResolver, storeMetrics)

  def config = new BlobStoreConfiguration()

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
    locationResolver.getLocation(_) >> { args -> args[0].toString() }
    amazonS3Factory.create(_) >> s3
    config.attributes = [s3: [bucket: 'mybucket']]
  }

  def "Get blob"() {
    given: 'A mocked S3 setup'
      def attributesS3Object = mockS3Object(attributesContents)
      def contentS3Object = mockS3Object('hello world')
      1 * s3.doesBucketExist('mybucket') >> true
      1 * s3.getBucketLifecycleConfiguration('mybucket') >>
          blobStore.makeLifecycleConfiguration(null, S3BlobStore.DEFAULT_EXPIRATION_IN_DAYS)
      1 * s3.doesObjectExist('mybucket', 'metadata.properties') >> false
      1 * s3.doesObjectExist('mybucket', 'content/test.properties') >> true
      1 * s3.getObject('mybucket', 'content/test.properties') >> attributesS3Object
      1 * s3.getObject('mybucket', 'content/test.bytes') >> contentS3Object

    when: 'An existing blob is read'
      blobStore.init(config)
      blobStore.doStart()
      def blob = blobStore.get(new BlobId('test'))

    then: 'The contents are read from s3'
      blob.inputStream.text == 'hello world'
  }

  def 'set lifecycle on pre-existing bucket if not present'() {
    given: 'bucket already exists, but has null lifecycle configuration'
      s3.doesBucketExist('mybucket') >> true

    when: 'init fires'
      blobStore.init(config)

    then: 'lifecycle configuration is added'
      1 * s3.setBucketLifecycleConfiguration('mybucket', !null)
  }

  def 'soft delete successful'() {
    given: 'blob exists'
      blobStore.init(config)
      blobStore.doStart()
      def attributesS3Object = mockS3Object(attributesContents)
      1 * s3.doesObjectExist('mybucket', 'content/soft-delete-success.properties') >> true
      1 * s3.getObject('mybucket', 'content/soft-delete-success.properties') >> attributesS3Object

    when: 'blob is deleted'
      def deleted = blobStore.delete(new BlobId('soft-delete-success'), 'successful test')

    then: 'deleted tag is added'
      deleted == true
      1 * s3.setObjectTagging(_) >> { args ->
        assert args[0].getKey().endsWith(BLOB_CONTENT_SUFFIX) == true
      }
      1 * s3.setObjectTagging(_) >> { args ->
        assert args[0].getKey().endsWith(BLOB_ATTRIBUTE_SUFFIX) == true
      }
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

  def 'isExpirationLifecycleConfigurationPresent returns false on empty config'() {
    given: 'empty lifecycleConfiguration'
      def bucketConfig = new BucketLifecycleConfiguration()

    when: 'isExpirationLifecycleConfigurationPresent called'
      def result = blobStore.isExpirationLifecycleConfigurationPresent(bucketConfig)

    then: 'false'
      !result
  }

  /**
   * Make sure if admins have set other lifecycle rules we don't clobber them.
   */
  def 'adding lifecycle leaves other rules alone'() {
    given: 'empty lifecycleConfiguration'
      def bucketConfig = new BucketLifecycleConfiguration()
      def rule = new BucketLifecycleConfiguration.Rule()
          .withId('some other rule')
          .withTransitions([
              new Transition().withStorageClass(StorageClass.Glacier).withDays(365)
          ])
          .withStatus(BucketLifecycleConfiguration.ENABLED.toString())
      bucketConfig.setRules([ rule ])

      s3.doesBucketExist('mybucket') >> true
      s3.getBucketLifecycleConfiguration('mybucket') >> bucketConfig

    when: 'init called'
      blobStore.init(config)

    then: 'glacier rule still present'
      1 * s3.setBucketLifecycleConfiguration(_, _) >> { bucketName, capturedConfig ->
        assert capturedConfig.getRules().size() == 2
        assert capturedConfig.getRules().stream().anyMatch { it.id == 'some other rule' }
        assert capturedConfig.getRules().stream().anyMatch { it.id == S3BlobStore.LIFECYCLE_EXPIRATION_RULE_ID }
      }
  }

  private mockS3Object(String contents) {
    S3Object s3Object = Mock()
    s3Object.getObjectContent() >> new S3ObjectInputStream(new ByteArrayInputStream(contents.bytes), null)
    s3Object
  }
}
