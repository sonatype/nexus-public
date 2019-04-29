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
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.StorageClass
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.model.lifecycle.LifecycleAndOperator
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter
import com.amazonaws.services.s3.model.lifecycle.LifecyclePrefixPredicate
import com.amazonaws.services.s3.model.lifecycle.LifecycleTagPredicate
import spock.lang.Specification
import spock.lang.Unroll

import static org.sonatype.nexus.blobstore.s3.internal.BucketManager.LIFECYCLE_EXPIRATION_RULE_ID_PREFIX
import static org.sonatype.nexus.blobstore.s3.internal.BucketManager.OLD_LIFECYCLE_EXPIRATION_RULE_ID

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
      def cfg = new BlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      def bucketConfig = new BucketLifecycleConfiguration()
      bucketManager.s3 = s3

    when: 'isExpirationLifecycleConfigurationPresent called'
      def result = bucketManager.isExpirationLifecycleConfigurationPresent(bucketConfig, cfg)

    then: 'false'
      !result
  }

  /**
   * Make sure if admins have set other lifecycle rules we don't clobber them.
   */
  def 'adding lifecycle leaves other rules alone'() {
    given: 'lifecycleConfiguration with other rules'
      def bucketConfig = new BucketLifecycleConfiguration()
      def glacierRule = new BucketLifecycleConfiguration.Rule()
          .withId('some other rule')
          .withTransitions([
              new Transition().withStorageClass(StorageClass.Glacier).withDays(365)
          ])
          .withStatus(BucketLifecycleConfiguration.ENABLED)
      def otherBlobStoreRule = new BucketLifecycleConfiguration.Rule()
          .withId(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + 'other')
          .withFilter(new LifecycleFilter(
             new LifecycleAndOperator([
                new LifecyclePrefixPredicate('otherPrefix/'),
                new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)])))
          .withExpirationInDays(123)
          .withStatus(BucketLifecycleConfiguration.ENABLED)
      bucketConfig.setRules([ glacierRule, otherBlobStoreRule ])

      s3.doesBucketExistV2('mybucket') >> true
      s3.getBucketLifecycleConfiguration('mybucket') >> bucketConfig
      def cfg = new BlobStoreConfiguration(name: 'blobs')
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      bucketManager.s3 = s3

    when: 'prepareStorageLocation called'
      bucketManager.prepareStorageLocation(cfg)

    then: 'glacier rule still present'
      1 * s3.setBucketLifecycleConfiguration(_, _) >> { bucketName, capturedConfig ->
        assert capturedConfig.rules.size() == 3
        assert capturedConfig.rules.any { it.id == 'some other rule' }
        assert capturedConfig.rules.any { it.id == LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + 'other' }
        assert capturedConfig.rules.any { it.id == LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + 'blobs' }
      }
  }

  def 'lifecycle rule updated when expiry date changes'() {
    given: 'lifecycleConfiguration with inital expiry days'
      def bucketConfig = new BucketLifecycleConfiguration()
      def rule1 = new BucketLifecycleConfiguration.Rule()
          .withId('some other rule')
          .withTransitions([
              new Transition().withStorageClass(StorageClass.Glacier).withDays(365)
          ])
          .withStatus(BucketLifecycleConfiguration.ENABLED)
      def rule2 = new BucketLifecycleConfiguration.Rule()
          .withId(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + 'blobs')
          .withFilter(new LifecycleFilter(
              new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)))
          .withExpirationInDays(2)
          .withStatus(BucketLifecycleConfiguration.ENABLED)
      bucketConfig.setRules([ rule1, rule2 ])

      s3.doesBucketExistV2('mybucket') >> true
      s3.getBucketLifecycleConfiguration('mybucket') >> bucketConfig
      def cfg = new BlobStoreConfiguration(name: 'blobs')
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      bucketManager.s3 = s3

    when: 'prepareStorageLocation called'
      bucketManager.prepareStorageLocation(cfg)

    then: 'the expiry date on the expiration rule is updated'
      1 * s3.setBucketLifecycleConfiguration(_, _) >> { bucketName, capturedConfig ->
        assert capturedConfig.rules.size() == 2
        assert capturedConfig.rules.any { it.id == 'some other rule' }
        assert capturedConfig.rules.any { it.id == LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + 'blobs' && it.expirationInDays == 3 }
      }
  }

  def 'lifecycle configuration removed if all rules removed'() {
    given: 'lifecycleConfiguration with inital expiry days'
      def bucketConfig = new BucketLifecycleConfiguration()
      def rule1 = new BucketLifecycleConfiguration.Rule()
          .withId(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + 'mybucket')
          .withFilter(new LifecycleFilter(
              new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)))
          .withExpirationInDays(3)
          .withStatus(BucketLifecycleConfiguration.ENABLED)
      bucketConfig.setRules([ rule1 ])

      s3.doesBucketExistV2('mybucket') >> true
      s3.getBucketLifecycleConfiguration('mybucket') >> bucketConfig
      def cfg = new BlobStoreConfiguration(name: 'mybucket')
      cfg.attributes = [s3: [bucket: 'mybucket', expiration: '0']]
      bucketManager.s3 = s3

    when: 'prepareStorageLocation called'
      bucketManager.prepareStorageLocation(cfg)

    then: 'there are no more rules so the lifecycle configuration is removed'
      1 * s3.deleteBucketLifecycleConfiguration('mybucket')
      0 * s3.setBucketLifecycleConfiguration(_, _)
  }

  def 'global lifecycle rule switched to blob store specific if present'() {
    given: 'global lifecycleConfiguration with inital expiry days'
      def bucketConfig = new BucketLifecycleConfiguration()
      def rule1 = new BucketLifecycleConfiguration.Rule()
          .withId(OLD_LIFECYCLE_EXPIRATION_RULE_ID)
          .withFilter(new LifecycleFilter(
              new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)))
          .withExpirationInDays(3)
          .withStatus(BucketLifecycleConfiguration.ENABLED)
      bucketConfig.setRules([ rule1 ])

      s3.doesBucketExistV2('mybucket') >> true
      s3.getBucketLifecycleConfiguration('mybucket') >> bucketConfig
      def cfg = new BlobStoreConfiguration(name: 'blobs')
      cfg.attributes = [s3: [bucket: 'mybucket', expiration: '4']]
      bucketManager.s3 = s3

    when: 'prepareStorageLocation called'
      bucketManager.prepareStorageLocation(cfg)

    then: 'the updated rule replaces the global config'
      1 * s3.setBucketLifecycleConfiguration(_, _) >> { bucketName, capturedConfig ->
        assert capturedConfig.rules.size() == 1
        assert capturedConfig.rules[0].id == LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + 'blobs'
        assert capturedConfig.rules[0].expirationInDays == 4
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
      s3.getBucketLifecycleConfiguration(_) >> Mock(BucketLifecycleConfiguration)
      def cfg = new BlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      bucketManager.s3 = s3

    when: 'deleteStorageLocation is called'
      bucketManager.deleteStorageLocation(cfg)

    then: 'bucket is not deleted'
      0 * s3.deleteBucket('mybucket')
  }

  @Unroll
  def 'it will only remove nxrm managed life cycles from the bucket'() {
    given: 'non-empty bucket with lifecycle configuration rules'
      ObjectListing objectListing = Mock()
      objectListing.getObjectSummaries() >> [new S3ObjectSummary()]
      s3.listObjects(_) >> objectListing
      s3.getBucketLifecycleConfiguration(_) >> Mock(BucketLifecycleConfiguration) {
        getRules() >> rules
      }
      def cfg = new BlobStoreConfiguration()
      cfg.attributes = [s3: [bucket: 'mybucket', prefix: 'myprefix', expiration: '3']]
      cfg.name = 'my_s3_blob_store'
      bucketManager.s3 = s3

    when: 'deleteStorageLocation is called'
      bucketManager.deleteStorageLocation(cfg)

    then: 'the bucket lifecycle configuration is updated accordingly'
      deleteLifeCycleCallCount * s3.deleteBucketLifecycleConfiguration(_)
      setLifeCycleCallCount * s3.setBucketLifecycleConfiguration(_, _)

    where:
      rules                                                     | deleteLifeCycleCallCount | setLifeCycleCallCount
      []                                                        | 1                        | 0
      [oldNxrmRule()]                                           | 1                        | 0
      [newNxrmRule('my_s3_blob_store')]                         | 1                        | 0
      [userRule()]                                              | 0                        | 1
      [userRule(), oldNxrmRule(), userRule()]                   | 0                        | 1
      [userRule(), newNxrmRule('my_s3_blob_store'), userRule()] | 0                        | 1
  }

  private Rule userRule() {
    Mock(Rule) { getId() >> 'user_rule_id' }
  }

  private Rule oldNxrmRule() {
    Mock(Rule) { getId() >> OLD_LIFECYCLE_EXPIRATION_RULE_ID }
  }

  private Rule newNxrmRule(final String name) {
    Mock(Rule) { getId() >> LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + name }
  }
}
