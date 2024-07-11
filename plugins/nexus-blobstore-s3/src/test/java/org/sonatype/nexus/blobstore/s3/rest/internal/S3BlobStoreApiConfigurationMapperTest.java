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
package org.sonatype.nexus.blobstore.s3.rest.internal;

import java.util.function.Function;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.rest.BlobStoreApiSoftQuota;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiAdvancedBucketConnection;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucket;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketConfiguration;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketSecurity;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiEncryption;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiModel;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import org.hamcrest.core.IsNull;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.LIMIT_KEY;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.ROOT_KEY;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.TYPE_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.*;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_PREFIX_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;

public class S3BlobStoreApiConfigurationMapperTest
    extends TestSupport
{
  private static final String BLOB_STORE_NAME = "anS3BlobStore";

  private static final String QUOTA_TYPE = "spaceRemainingQuota";

  private static final String S3_BUCKET_NAME = "theBucket";

  private static final String BUCKET_PREFIX = "special_bucket";

  private static final String AWS_REGION = "aws-region-1";

  private static final String AN_IAM_ACCESS_KEY = "anAccessKey";

  private static final String AN_IAM_SECRET_ACCESS_KEY = "aSecretAccessKey";

  private static final String AN_IAM_ROLE = "anIamRole";

  private static final String AN_IAM_SESSION_TOKEN = "anIamSessionToken";

  private static final String S3_MANAGED_ENCRYPTION = "s3ManagedEncryption";

  private static final String SUPER_SECURE_ENCRYPTION_KEY = "supersecurekey";

  private static final String S3_ENDPOINT_URL = "s3://superbucketendpoint";

  private static final String S3_SIGNER_TYPE = "s3SignerType";

  private static final boolean FORCE_PATH_STYLE = true;

  private static int MAX_CONNECTION_POOL = 5;

  private static final int BUCKET_EXPIRATION = 5;

  private static final int QUOTA_LIMIT = 2;

  private Function<BlobStoreConfiguration, S3BlobStoreApiModel> underTest =
      S3BlobStoreApiConfigurationMapper::map;

  @Test
  public void testCopyNonNullAttributes() {
    BlobStoreConfiguration configuration = aMinimalBlobStoreConfiguration();

    S3BlobStoreApiModel model = underTest.apply(configuration);
    S3BlobStoreApiBucketConfiguration bucketConfiguration = model.getBucketConfiguration();

    assertThat(model.getName(), is(BLOB_STORE_NAME));
    assertRequiredBucketDetails(bucketConfiguration.getBucket());
    assertThat(model.getSoftQuota(), nullValue());
    assertThat(bucketConfiguration.getBucketSecurity(), nullValue());
    assertThat(bucketConfiguration.getEncryption(), nullValue());
    assertThat(bucketConfiguration.getAdvancedBucketConnection(), nullValue());
  }

  @Test
  public void testConvertConfigurationToModel() {
    BlobStoreConfiguration configuration = aFullySetBlobStoreConfiguration();

    S3BlobStoreApiModel model = underTest.apply(configuration);
    S3BlobStoreApiBucketConfiguration bucketConfiguration = model.getBucketConfiguration();

    assertThat(model.getName(), is(BLOB_STORE_NAME));
    assertSoftQuota(model.getSoftQuota());
    assertRequiredBucketDetails(bucketConfiguration.getBucket());
    assertBucketSecurityDetails(bucketConfiguration.getBucketSecurity());
    assertBucketEncryptionDetails(bucketConfiguration.getEncryption());
    assertBucketAdvancedConnectionDetails(bucketConfiguration.getAdvancedBucketConnection());
  }

  @Test
  public void testConvertConfigurationToModelEmptyStringMaxConnection() {
    BlobStoreConfiguration configuration = aFullySetBlobStoreConfigurationWithEmptyStringMaxConnection();
    S3BlobStoreApiModel model = underTest.apply(configuration);
    S3BlobStoreApiBucketConfiguration bucketConfiguration = model.getBucketConfiguration();
    assertThat(bucketConfiguration.getAdvancedBucketConnection().getMaxConnectionPoolSize(), nullValue());
  }

  private static BlobStoreConfiguration aMinimalBlobStoreConfiguration() {
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();
    configuration.setType(TYPE);
    configuration.setName(BLOB_STORE_NAME);
    fillRequiredBucketAttributes(configuration.attributes(CONFIG_KEY));
    return configuration;
  }

  private static BlobStoreConfiguration aFullySetBlobStoreConfiguration() {
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();
    configuration.setType(TYPE);
    configuration.setName(BLOB_STORE_NAME);
    createSoftQuota(configuration);
    NestedAttributesMap bucketAttributes = configuration.attributes(CONFIG_KEY);
    fillRequiredBucketAttributes(bucketAttributes);
    fillOptionalBucketDetails(bucketAttributes);
    return configuration;
  }

  private static BlobStoreConfiguration aFullySetBlobStoreConfigurationWithEmptyStringMaxConnection() {
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();
    configuration.setType(TYPE);
    configuration.setName(BLOB_STORE_NAME);
    createSoftQuota(configuration);
    NestedAttributesMap bucketAttributes = configuration.attributes(CONFIG_KEY);
    fillRequiredBucketAttributes(bucketAttributes);
    fillOptionalBucketDetailsEmptyStringMaxConnection(bucketAttributes);
    return configuration;
  }

  private static void createSoftQuota(final BlobStoreConfiguration configuration) {
    NestedAttributesMap softQuotaAttributes = configuration.attributes(ROOT_KEY);
    softQuotaAttributes.set(TYPE_KEY, QUOTA_TYPE);
    softQuotaAttributes.set(LIMIT_KEY, QUOTA_LIMIT);
  }

  private static void fillRequiredBucketAttributes(final NestedAttributesMap bucketAttributes) {
    bucketAttributes.set(REGION_KEY, AWS_REGION);
    bucketAttributes.set(BUCKET_KEY, S3_BUCKET_NAME);
    bucketAttributes.set(BUCKET_PREFIX_KEY, BUCKET_PREFIX);
    bucketAttributes.set(EXPIRATION_KEY, BUCKET_EXPIRATION);
  }

  private static void fillOptionalBucketDetails(final NestedAttributesMap bucketAttributes) {
    fillBucketSecurityDetails(bucketAttributes);
    fillBucketEncryptionDetails(bucketAttributes);
    fillBucketAdvancedConnectionDetails(bucketAttributes);
  }

  private static void fillOptionalBucketDetailsEmptyStringMaxConnection(final NestedAttributesMap bucketAttributes) {
    fillBucketSecurityDetails(bucketAttributes);
    fillBucketEncryptionDetails(bucketAttributes);
    fillBucketAdvancedConnectionDetailsEmptyStringMaxConnection(bucketAttributes);
  }

  private static void fillBucketSecurityDetails(final NestedAttributesMap bucketAttributes) {
    bucketAttributes.set(ACCESS_KEY_ID_KEY, AN_IAM_ACCESS_KEY);
    bucketAttributes.set(SECRET_ACCESS_KEY_KEY, AN_IAM_SECRET_ACCESS_KEY);
    bucketAttributes.set(ASSUME_ROLE_KEY, AN_IAM_ROLE);
    bucketAttributes.set(SESSION_TOKEN_KEY, AN_IAM_SESSION_TOKEN);
  }

  private static void fillBucketEncryptionDetails(final NestedAttributesMap bucketAttributes) {
    bucketAttributes.set(ENCRYPTION_TYPE, S3_MANAGED_ENCRYPTION);
    bucketAttributes.set(ENCRYPTION_KEY, SUPER_SECURE_ENCRYPTION_KEY);
  }

  private static void fillBucketAdvancedConnectionDetails(final NestedAttributesMap bucketAttributes) {
  bucketAttributes.set(ENDPOINT_KEY, S3_ENDPOINT_URL);
  bucketAttributes.set(SIGNERTYPE_KEY, S3_SIGNER_TYPE);
  bucketAttributes.set(FORCE_PATH_STYLE_KEY, FORCE_PATH_STYLE);
  bucketAttributes.set(MAX_CONNECTION_POOL_KEY, MAX_CONNECTION_POOL);
}

  private static void fillBucketAdvancedConnectionDetailsEmptyStringMaxConnection(final NestedAttributesMap bucketAttributes) {
    bucketAttributes.set(ENDPOINT_KEY, S3_ENDPOINT_URL);
    bucketAttributes.set(SIGNERTYPE_KEY, S3_SIGNER_TYPE);
    bucketAttributes.set(FORCE_PATH_STYLE_KEY, FORCE_PATH_STYLE);
    bucketAttributes.set(MAX_CONNECTION_POOL_KEY, "");
  }

  private static void assertSoftQuota(final BlobStoreApiSoftQuota softQuota) {
    assertThat(softQuota.getType(), is(QUOTA_TYPE));
    assertThat(softQuota.getLimit().intValue(), is(QUOTA_LIMIT));
  }

  private static void assertRequiredBucketDetails(final S3BlobStoreApiBucket s3BlobStoreBucket) {
    assertThat(s3BlobStoreBucket.getRegion(), is(AWS_REGION));
    assertThat(s3BlobStoreBucket.getName(), is(S3_BUCKET_NAME));
    assertThat(s3BlobStoreBucket.getExpiration(), is(BUCKET_EXPIRATION));
    assertThat(s3BlobStoreBucket.getPrefix(), is(BUCKET_PREFIX));
  }

  private static void assertBucketSecurityDetails(final S3BlobStoreApiBucketSecurity bucketSecurity) {
    assertThat(bucketSecurity.getAccessKeyId(), is(AN_IAM_ACCESS_KEY));
    assertSecretAccessKeyIsNull(bucketSecurity);
    assertThat(bucketSecurity.getRole(), is(AN_IAM_ROLE));
    assertThat(bucketSecurity.getSessionToken(), is(AN_IAM_SESSION_TOKEN));
  }

  private static void assertSecretAccessKeyIsNull(final S3BlobStoreApiBucketSecurity bucketSecurity) {
    assertThat(bucketSecurity.getSecretAccessKey(), nullValue());
  }

  private static void assertBucketEncryptionDetails(final S3BlobStoreApiEncryption s3BlobStoreEncryption) {
    assertThat(s3BlobStoreEncryption.getEncryptionType(), is(S3_MANAGED_ENCRYPTION));
    assertThat(s3BlobStoreEncryption.getEncryptionKey(), is(SUPER_SECURE_ENCRYPTION_KEY));
  }

  private static void assertBucketAdvancedConnectionDetails(
      final S3BlobStoreApiAdvancedBucketConnection advancedBucketConnection)
  {
    assertThat(advancedBucketConnection.getEndpoint(), is(S3_ENDPOINT_URL));
    assertThat(advancedBucketConnection.getSignerType(), is(S3_SIGNER_TYPE));
    assertThat(advancedBucketConnection.getForcePathStyle(), is(FORCE_PATH_STYLE));
    assertThat(advancedBucketConnection.getMaxConnectionPoolSize(), is(MAX_CONNECTION_POOL));
  }
}
