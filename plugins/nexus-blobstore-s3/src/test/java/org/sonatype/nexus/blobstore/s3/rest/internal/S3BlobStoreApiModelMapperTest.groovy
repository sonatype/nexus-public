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
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiModel
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucket
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketConfiguration
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketSecurity
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiEncryption
import org.sonatype.nexus.common.collect.NestedAttributesMap

import spock.lang.Specification

import static java.lang.Boolean.parseBoolean
import static java.lang.Integer.parseInt
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.LIMIT_KEY
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.ROOT_KEY
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.TYPE_KEY
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.*
import static S3BlobStoreApiModelMapper.ONE_MILLION

class S3BlobStoreApiModelMapperTest
    extends Specification
{
  private static final String BLOB_STORE_NAME = 'anS3BlobStore'

  private static final String S3_BUCKET_NAME = 'theBucket'

  private static final String BUCKET_PREFIX = "special_bucket"

  private static final String AWS_REGION = "aws-region-1"

  private static final int BUCKET_EXPIRATION = 5

  private static final String AN_IAM_ACCESS_KEY = 'anAccessKey'

  private static final String AN_IAM_SECRET_ACCESS_KEY = 'aSecretAccessKey'

  private static final String AN_IAM_ROLE = 'anIamRole'

  private static final String AN_IAM_SESSION_TOKEN = 'anIamSessionToken'

  private static final String S3_MANAGED_ENCRYPTION = 's3ManagedEncryption'

  private static final String SUPER_SECURE_ENCRYPTION_KEY = 'supersecurekey'

  private static final String S3_ENDPOINT_URL = 's3://superbucketendpoint'

  private static final String S3_SIGNER_TYPE = 's3SignerType'

  private static final boolean FORCE_PATH_STYLE = true

  private static final String QUOTA_TYPE = 'spaceRemainingQuota'

  private static final int QUOTA_LIMIT = 2

  def underTest = S3BlobStoreApiModelMapper.MODEL_MAPPER

  def 'Should copy non null attributes only'() {
    given:
      S3BlobStoreApiModel model = aMinimalS3BlobStoreApiModel()
    when:
      def configuration = underTest.apply(new MockBlobStoreConfiguration(), model)
      def s3BucketAttributes = configuration.attributes(CONFIG_KEY)
      def softQuotaAttributes = configuration.attributes(ROOT_KEY)
    then:
      assertGeneralBucketDetails(s3BucketAttributes)
      assertBucketSecurityIsNotSet(s3BucketAttributes)
      assertBucketEncryptionIsNotSet(s3BucketAttributes)
      assertBucketAdvancedConnectionIsNotSet(s3BucketAttributes)
      configuration.type == TYPE
      configuration.name == BLOB_STORE_NAME
      softQuotaAttributes.get(TYPE_KEY) == null
      softQuotaAttributes.get(LIMIT_KEY) == null
  }

  def 'Should convert a S3BlobStoreApiModel to BlobStoreConfiguration'() {
    given:
      S3BlobStoreApiModel model = aFullySetS3BlobStoreApiModel()
    when:
      def configuration = underTest.apply(new MockBlobStoreConfiguration(), model)
      def s3BucketAttributes = configuration.attributes(CONFIG_KEY)
    then:
      configuration.type == TYPE
      configuration.name == BLOB_STORE_NAME
      assertSoftQuota(configuration)
      assertGeneralBucketDetails(s3BucketAttributes)
      assertBucketSecurityDetails(s3BucketAttributes)
      assertBucketEncryptionDetails(s3BucketAttributes)
      assertBucketAdvancedConnectionDetails(s3BucketAttributes)
  }

  static S3BlobStoreApiModel aMinimalS3BlobStoreApiModel() {
    new S3BlobStoreApiModel(BLOB_STORE_NAME,
        null,
        new S3BlobStoreApiBucketConfiguration(anS3BlobStoreBucket(),
            null, null, null))
  }

  static S3BlobStoreApiBucket anS3BlobStoreBucket() {
    new S3BlobStoreApiBucket(AWS_REGION, S3_BUCKET_NAME, BUCKET_PREFIX, BUCKET_EXPIRATION)
  }

  static S3BlobStoreApiModel aFullySetS3BlobStoreApiModel() {
    new S3BlobStoreApiModel(BLOB_STORE_NAME,
        new BlobStoreApiSoftQuota(type: QUOTA_TYPE, limit: QUOTA_LIMIT),
        aS3BlobStoreBucketConfiguration())
  }

  static aS3BlobStoreBucketConfiguration() {
    new S3BlobStoreApiBucketConfiguration(anS3BlobStoreBucket(),
        anS3BlobStoreBucketSecurity(),
        anS3BlobStoreEncryption(),
        anAdvancedBucketConnection()
    )
  }

  static S3BlobStoreApiBucketSecurity anS3BlobStoreBucketSecurity() {
    new S3BlobStoreApiBucketSecurity(AN_IAM_ACCESS_KEY, AN_IAM_SECRET_ACCESS_KEY, AN_IAM_ROLE, AN_IAM_SESSION_TOKEN)
  }

  static S3BlobStoreApiEncryption anS3BlobStoreEncryption() {
    new S3BlobStoreApiEncryption(S3_MANAGED_ENCRYPTION, SUPER_SECURE_ENCRYPTION_KEY)
  }

  private static anAdvancedBucketConnection() {
    new S3BlobStoreApiAdvancedBucketConnection(
        S3_ENDPOINT_URL,
        S3_SIGNER_TYPE,
        FORCE_PATH_STYLE
    )
  }

  private static void assertGeneralBucketDetails(NestedAttributesMap attributes) {
    assert getAttribute(REGION_KEY, attributes) == AWS_REGION
    assert getAttribute(BUCKET_KEY, attributes) == S3_BUCKET_NAME
    assert parseInt(getAttribute(EXPIRATION_KEY, attributes)) == BUCKET_EXPIRATION
    assert getAttribute(BUCKET_PREFIX_KEY, attributes) == BUCKET_PREFIX
  }

  private static void assertBucketSecurityIsNotSet(NestedAttributesMap attributes) {
    assert getAttribute(ACCESS_KEY_ID_KEY, attributes) == null
    assert getAttribute(SECRET_ACCESS_KEY_KEY, attributes) == null
    assert getAttribute(ASSUME_ROLE_KEY, attributes) == null
    assert getAttribute(SESSION_TOKEN_KEY, attributes) == null
  }

  private static void assertBucketEncryptionIsNotSet(NestedAttributesMap attributes) {
    assert getAttribute(ENCRYPTION_TYPE, attributes) == null
    assert getAttribute(ENCRYPTION_KEY, attributes) == null
  }

  private static void assertBucketAdvancedConnectionIsNotSet(NestedAttributesMap attributes) {
    assert getAttribute(ENDPOINT_KEY, attributes) == null
    assert getAttribute(SIGNERTYPE_KEY, attributes) == null
    assert getAttribute(FORCE_PATH_STYLE_KEY, attributes) == null
  }

  private static void assertSoftQuota(final BlobStoreConfiguration configuration) {
    def quotaSettings = configuration.attributes(ROOT_KEY)
    assert getAttribute(TYPE_KEY, quotaSettings) == QUOTA_TYPE
    assert parseInt(getAttribute(LIMIT_KEY, quotaSettings)) == QUOTA_LIMIT * ONE_MILLION
  }

  private static String getAttribute(String key, NestedAttributesMap attributes) {
    attributes.get(key)?.toString()
  }

  private static void assertBucketSecurityDetails(NestedAttributesMap attributes) {
    assert getAttribute(ACCESS_KEY_ID_KEY, attributes) == AN_IAM_ACCESS_KEY
    assert getAttribute(SECRET_ACCESS_KEY_KEY, attributes) == AN_IAM_SECRET_ACCESS_KEY
    assert getAttribute(ASSUME_ROLE_KEY, attributes) == AN_IAM_ROLE
    assert getAttribute(SESSION_TOKEN_KEY, attributes) == AN_IAM_SESSION_TOKEN
  }

  private static void assertBucketEncryptionDetails(NestedAttributesMap attributes) {
    assert getAttribute(ENCRYPTION_TYPE, attributes) == S3_MANAGED_ENCRYPTION
    assert getAttribute(ENCRYPTION_KEY, attributes) == SUPER_SECURE_ENCRYPTION_KEY
  }

  private static void assertBucketAdvancedConnectionDetails(NestedAttributesMap attributes) {
    assert getAttribute(ENDPOINT_KEY, attributes) == S3_ENDPOINT_URL
    assert getAttribute(SIGNERTYPE_KEY, attributes) == S3_SIGNER_TYPE
    assert parseBoolean(getAttribute(FORCE_PATH_STYLE_KEY, attributes)) == FORCE_PATH_STYLE
  }
}
