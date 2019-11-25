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
package org.sonatype.nexus.blobstore.s3.rest.internal

import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.rest.BlobStoreApiSoftQuota
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiAdvancedBucketConnection
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucket
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketSecurity
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiEncryption
import org.sonatype.nexus.common.collect.NestedAttributesMap

import spock.lang.Specification

import static S3BlobStoreApiModelMapper.ONE_MILLION
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.LIMIT_KEY
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.ROOT_KEY
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.TYPE_KEY
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.*

class S3BlobStoreApiConfigurationMapperTest
    extends Specification
{
  private static final String BLOB_STORE_NAME = 'anS3BlobStore'

  private static final String QUOTA_TYPE = 'spaceRemainingQuota'

  private static final String S3_BUCKET_NAME = 'theBucket'

  private static final String BUCKET_PREFIX = "special_bucket"

  private static final String AWS_REGION = "aws-region-1"

  private static final String AN_IAM_ACCESS_KEY = 'anAccessKey'

  private static final String AN_IAM_SECRET_ACCESS_KEY = 'aSecretAccessKey'

  private static final String AN_IAM_ROLE = 'anIamRole'

  private static final String AN_IAM_SESSION_TOKEN = 'anIamSessionToken'

  private static final String S3_MANAGED_ENCRYPTION = 's3ManagedEncryption'

  private static final String SUPER_SECURE_ENCRYPTION_KEY = 'supersecurekey'

  private static final String S3_ENDPOINT_URL = 's3://superbucketendpoint'

  private static final String S3_SIGNER_TYPE = 's3SignerType'

  private static final boolean FORCE_PATH_STYLE = true

  private static final int BUCKET_EXPIRATION = 5

  private static final int QUOTA_LIMIT = 2

  def underTest = S3BlobStoreApiConfigurationMapper.CONFIGURATION_MAPPER

  def 'Should only copy non null attributes'() {
    given:
      BlobStoreConfiguration configuration = aMinimalBlobStoreConfiguration()
    when:
      def model = underTest.apply(configuration)
      def bucketConfiguration = model.bucketConfiguration
    then:
      model.name == BLOB_STORE_NAME
      assertRequiredBucketDetails(bucketConfiguration.bucket)
      model.softQuota == null
      bucketConfiguration.bucketSecurity == null
      bucketConfiguration.encryption == null
      bucketConfiguration.advancedBucketConnection == null
  }

  def 'Should convert a BlobStoreConfiguration to a S3BlobStoreApiModel'() {
    given:
      BlobStoreConfiguration configuration = aFullySetBlobStoreConfiguration()
    when:
      def model = underTest.apply(configuration)
      def bucketConfiguration = model.bucketConfiguration
    then:
      model.name == BLOB_STORE_NAME
      assertSoftQuota(model.softQuota)
      assertRequiredBucketDetails(bucketConfiguration.bucket)
      assertBucketSecurityDetails(bucketConfiguration.bucketSecurity)
      assertBucketEncryptionDetails(bucketConfiguration.encryption)
      assertBucketAdvancedConnectionDetails(bucketConfiguration.advancedBucketConnection)
  }

  private static BlobStoreConfiguration aMinimalBlobStoreConfiguration() {
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration()
    configuration.type = TYPE
    configuration.name = BLOB_STORE_NAME
    fillRequiredBucketAttributes(configuration.attributes(CONFIG_KEY))
    configuration
  }

  private static BlobStoreConfiguration aFullySetBlobStoreConfiguration() {
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration()
    configuration.type = TYPE
    configuration.name = BLOB_STORE_NAME
    createSoftQuota(configuration)
    def bucketAttributes = configuration.attributes(CONFIG_KEY)
    fillRequiredBucketAttributes(bucketAttributes)
    fillOptionalBucketDetails(bucketAttributes)
    configuration
  }

  private static void createSoftQuota(BlobStoreConfiguration configuration) {
    def softQuotaAttributes = configuration.attributes(ROOT_KEY)
    softQuotaAttributes.set(TYPE_KEY, QUOTA_TYPE)
    softQuotaAttributes.set(LIMIT_KEY, QUOTA_LIMIT * ONE_MILLION)
  }

  private static void fillRequiredBucketAttributes(NestedAttributesMap bucketAttributes) {
    bucketAttributes.set(REGION_KEY, AWS_REGION)
    bucketAttributes.set(BUCKET_KEY, S3_BUCKET_NAME)
    bucketAttributes.set(BUCKET_PREFIX_KEY, BUCKET_PREFIX)
    bucketAttributes.set(EXPIRATION_KEY, BUCKET_EXPIRATION)
  }

  private static void fillOptionalBucketDetails(NestedAttributesMap bucketAttributes) {
    fillBucketSecurityDetails(bucketAttributes)
    fillBucketEncryptionDetails(bucketAttributes)
    fillBucketAdvancedConnectionDetails(bucketAttributes)
  }

  private static void fillBucketSecurityDetails(NestedAttributesMap bucketAttributes) {
    bucketAttributes.set(ACCESS_KEY_ID_KEY, AN_IAM_ACCESS_KEY)
    bucketAttributes.set(SECRET_ACCESS_KEY_KEY, AN_IAM_SECRET_ACCESS_KEY)
    bucketAttributes.set(ASSUME_ROLE_KEY, AN_IAM_ROLE)
    bucketAttributes.set(SESSION_TOKEN_KEY, AN_IAM_SESSION_TOKEN)
  }

  private static void fillBucketEncryptionDetails(final NestedAttributesMap bucketAttributes) {
    bucketAttributes.set(ENCRYPTION_TYPE, S3_MANAGED_ENCRYPTION)
    bucketAttributes.set(ENCRYPTION_KEY, SUPER_SECURE_ENCRYPTION_KEY)
  }

  private static void fillBucketAdvancedConnectionDetails(final NestedAttributesMap bucketAttributes) {
    bucketAttributes.set(ENDPOINT_KEY, S3_ENDPOINT_URL)
    bucketAttributes.set(SIGNERTYPE_KEY, S3_SIGNER_TYPE)
    bucketAttributes.set(FORCE_PATH_STYLE_KEY, FORCE_PATH_STYLE)
  }

  private static void assertSoftQuota(final BlobStoreApiSoftQuota softQuota) {
    assert softQuota.type == QUOTA_TYPE
    assert softQuota.limit.intValue() == QUOTA_LIMIT
  }

  private static void assertRequiredBucketDetails(S3BlobStoreApiBucket s3BlobStoreBucket) {
    assert s3BlobStoreBucket.region == AWS_REGION
    assert s3BlobStoreBucket.name == S3_BUCKET_NAME
    assert s3BlobStoreBucket.expiration == BUCKET_EXPIRATION
    assert s3BlobStoreBucket.prefix == BUCKET_PREFIX
  }

  private static void assertBucketSecurityDetails(final S3BlobStoreApiBucketSecurity bucketSecurity) {
    assert bucketSecurity.accessKeyId == AN_IAM_ACCESS_KEY
    assertSecretAccessKeyIsNull(bucketSecurity)
    assert bucketSecurity.role == AN_IAM_ROLE
    assert bucketSecurity.sessionToken == AN_IAM_SESSION_TOKEN
  }

  private static void assertSecretAccessKeyIsNull(S3BlobStoreApiBucketSecurity bucketSecurity) {
    assert bucketSecurity.secretAccessKey == null
  }

  private static void assertBucketEncryptionDetails(final S3BlobStoreApiEncryption s3BlobStoreEncryption) {
    assert s3BlobStoreEncryption.encryptionType == S3_MANAGED_ENCRYPTION
    assert s3BlobStoreEncryption.encryptionKey == SUPER_SECURE_ENCRYPTION_KEY
  }

  private static void assertBucketAdvancedConnectionDetails(
      S3BlobStoreApiAdvancedBucketConnection advancedBucketConnection)
  {
    assert advancedBucketConnection.endpoint == S3_ENDPOINT_URL
    assert advancedBucketConnection.signerType == S3_SIGNER_TYPE
    assert advancedBucketConnection.forcePathStyle == FORCE_PATH_STYLE
  }
}
