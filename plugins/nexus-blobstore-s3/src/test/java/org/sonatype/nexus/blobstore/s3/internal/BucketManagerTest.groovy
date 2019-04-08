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

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.StorageClass
import com.amazonaws.services.s3.model.S3ObjectSummary
import spock.lang.Specification

/**
 * {@link BucketManager} tests.
 */
class BucketManagerTest
    extends Specification
{

  AmazonS3 s3 = Mock()

  BucketManager bucketManager = new BucketManager()

  def 'set lifecycle on pre-existing bucket if not present'() {
    given: 'bucket already exists, but has null lifecycle configuration'
      s3.doesBucketExistV2('mybucket') >> true
      def cfg = new BlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      bucketManager.s3 = s3

    when: 'bucket created'
      bucketManager.prepareStorageLocation(cfg)

    then: 'lifecycle configuration is added'
      1 * s3.setBucketLifecycleConfiguration('mybucket', !null)
  }

  def 'isExpirationLifecycleConfigurationPresent returns false on empty config'() {
    given: 'empty lifecycleConfiguration'
      def bucketConfig = new BucketLifecycleConfiguration()
      bucketManager.s3 = s3

    when: 'isExpirationLifecycleConfigurationPresent called'
      def result = bucketManager.isExpirationLifecycleConfigurationPresent(bucketConfig, 3)

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

      s3.doesBucketExistV2('mybucket') >> true
      s3.getBucketLifecycleConfiguration('mybucket') >> bucketConfig
      def cfg = new BlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      bucketManager.s3 = s3

    when: 'create called'
      bucketManager.prepareStorageLocation(cfg)

    then: 'glacier rule still present'
      1 * s3.setBucketLifecycleConfiguration(_, _) >> { bucketName, capturedConfig ->
        assert capturedConfig.getRules().size() == 2
        assert capturedConfig.getRules().stream().anyMatch { it.id == 'some other rule' }
        assert capturedConfig.getRules().stream().anyMatch { it.id == BucketManager.LIFECYCLE_EXPIRATION_RULE_ID }
      }
  }

  def 'deleteStorageLocation removes bucket if empty'() {
    given: 'empty bucket'
      ObjectListing objectListing = Mock()
      objectListing.getObjectSummaries() >> []
      s3.listObjects(_) >> objectListing
      def cfg = new BlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      bucketManager.s3 = s3

    when: 'deleteStorageLocation is called'
      bucketManager.deleteStorageLocation(cfg)

    then: 'bucket is deleted'
      1 * s3.deleteBucket('mybucket')
  }

  def 'deleteStorageLocation does not remove bucket if not empty'() {
    given: 'non-empty bucket'
      ObjectListing objectListing = Mock()
      objectListing.getObjectSummaries() >> [new S3ObjectSummary()]
      s3.listObjects(_) >> objectListing
      def cfg = new BlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      bucketManager.s3 = s3

    when: 'deleteStorageLocation is called'
      bucketManager.deleteStorageLocation(cfg)

    then: 'bucket is deleted'
      0 * s3.deleteBucket('mybucket')
  }
}
