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

import java.util.Optional;

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

import org.junit.Test;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
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

public class S3BlobStoreApiModelMapperTest
    extends TestSupport
{
  private static final String BLOB_STORE_NAME = "anS3BlobStore";

  private static final String S3_BUCKET_NAME = "theBucket";

  private static final String BUCKET_PREFIX = "special_bucket";

  private static final String AWS_REGION = "aws-region-1";

  private static final int BUCKET_EXPIRATION = 5;

  private static final String AN_IAM_ACCESS_KEY = "anAccessKey";

  private static final String AN_IAM_SECRET_ACCESS_KEY = "aSecretAccessKey";

  private static final String AN_IAM_ROLE = "anIamRole";

  private static final String AN_IAM_SESSION_TOKEN = "anIamSessionToken";

  private static final String S3_MANAGED_ENCRYPTION = "s3ManagedEncryption";

  private static final String SUPER_SECURE_ENCRYPTION_KEY = "supersecurekey";

  private static final String S3_ENDPOINT_URL = "s3://superbucketendpoint";

  private static final String S3_SIGNER_TYPE = "s3SignerType";

  private static final boolean FORCE_PATH_STYLE = true;

  private static final String QUOTA_TYPE = "spaceRemainingQuota";

  private static final long QUOTA_LIMIT = 2;

  private static final int MAX_CONNECTION_POOL_SIZE = 3;

  @Test
  public void testShouldCopyNonNullAttributesOnly() {
    S3BlobStoreApiModel model = aMinimalS3BlobStoreApiModel();

    BlobStoreConfiguration configuration = S3BlobStoreApiModelMapper.map(new MockBlobStoreConfiguration(), model);
    NestedAttributesMap s3BucketAttributes = configuration.attributes(CONFIG_KEY);
    NestedAttributesMap softQuotaAttributes = configuration.attributes(ROOT_KEY);

    assertGeneralBucketDetails(s3BucketAttributes);
    assertBucketSecurityIsNotSet(s3BucketAttributes);
    assertBucketEncryptionIsNotSet(s3BucketAttributes);
    assertBucketAdvancedConnectionIsNotSet(s3BucketAttributes);
    assertThat(configuration.getType(), is(TYPE));
    assertThat(configuration.getName(), is(BLOB_STORE_NAME));
    assertThat(softQuotaAttributes.get(TYPE_KEY), nullValue());
    assertThat(softQuotaAttributes.get(LIMIT_KEY), nullValue());
  }

  @Test
  public void testShouldConvertToConfiguration() {
    S3BlobStoreApiModel model = aFullySetS3BlobStoreApiModel();

    BlobStoreConfiguration  configuration = S3BlobStoreApiModelMapper.map(new MockBlobStoreConfiguration(), model);
    NestedAttributesMap s3BucketAttributes = configuration.attributes(CONFIG_KEY);

    assertThat(configuration.getType(), is(TYPE));
    assertThat(configuration.getName(), is(BLOB_STORE_NAME));
    assertSoftQuota(configuration);
    assertGeneralBucketDetails(s3BucketAttributes);
    assertBucketSecurityDetails(s3BucketAttributes);
    assertBucketEncryptionDetails(s3BucketAttributes);
    assertBucketAdvancedConnectionDetails(s3BucketAttributes);
  }

  private static S3BlobStoreApiModel aMinimalS3BlobStoreApiModel() {
    return new S3BlobStoreApiModel(BLOB_STORE_NAME,
        null,
        new S3BlobStoreApiBucketConfiguration(anS3BlobStoreBucket(),
            null, null, null));
  }

  private static S3BlobStoreApiBucket anS3BlobStoreBucket() {
    return new S3BlobStoreApiBucket(AWS_REGION, S3_BUCKET_NAME, BUCKET_PREFIX, BUCKET_EXPIRATION);
  }

  private static S3BlobStoreApiModel aFullySetS3BlobStoreApiModel() {
    BlobStoreApiSoftQuota quota = new BlobStoreApiSoftQuota();
    quota.setType(QUOTA_TYPE);
    quota.setLimit(QUOTA_LIMIT);
    return new S3BlobStoreApiModel(BLOB_STORE_NAME, quota, aS3BlobStoreBucketConfiguration());
  }

  private static S3BlobStoreApiBucketConfiguration aS3BlobStoreBucketConfiguration() {
    return new S3BlobStoreApiBucketConfiguration(anS3BlobStoreBucket(),
        anS3BlobStoreBucketSecurity(),
        anS3BlobStoreEncryption(),
        anAdvancedBucketConnection()
    );
  }

  private static S3BlobStoreApiBucketSecurity anS3BlobStoreBucketSecurity() {
    return new S3BlobStoreApiBucketSecurity(AN_IAM_ACCESS_KEY, AN_IAM_SECRET_ACCESS_KEY, AN_IAM_ROLE, AN_IAM_SESSION_TOKEN);
  }

  private static S3BlobStoreApiEncryption anS3BlobStoreEncryption() {
    return new S3BlobStoreApiEncryption(S3_MANAGED_ENCRYPTION, SUPER_SECURE_ENCRYPTION_KEY);
  }

  private static S3BlobStoreApiAdvancedBucketConnection anAdvancedBucketConnection() {
    return new S3BlobStoreApiAdvancedBucketConnection(
        S3_ENDPOINT_URL,
        S3_SIGNER_TYPE,
        FORCE_PATH_STYLE,
        MAX_CONNECTION_POOL_SIZE
    );
  }

  private static void assertGeneralBucketDetails(final NestedAttributesMap attributes) {
    assertThat(getAttribute(REGION_KEY, attributes), is(AWS_REGION));
    assertThat(getAttribute(BUCKET_KEY, attributes), is(S3_BUCKET_NAME));
    assertThat(parseInt(getAttribute(EXPIRATION_KEY, attributes)), is(BUCKET_EXPIRATION));
    assertThat(getAttribute(BUCKET_PREFIX_KEY, attributes), is(BUCKET_PREFIX));
  }

  private static void assertBucketSecurityIsNotSet(final NestedAttributesMap attributes) {
    assertThat(getAttribute(ACCESS_KEY_ID_KEY, attributes), nullValue());
    assertThat(getAttribute(SECRET_ACCESS_KEY_KEY, attributes), nullValue());
    assertThat(getAttribute(ASSUME_ROLE_KEY, attributes), nullValue());
    assertThat(getAttribute(SESSION_TOKEN_KEY, attributes), nullValue());
  }

  private static void assertBucketEncryptionIsNotSet(final NestedAttributesMap attributes) {
    assertThat(getAttribute(ENCRYPTION_TYPE, attributes), nullValue());
    assertThat(getAttribute(ENCRYPTION_KEY, attributes), nullValue());
  }

  private static void assertBucketAdvancedConnectionIsNotSet(final NestedAttributesMap attributes) {
    assertThat(getAttribute(ENDPOINT_KEY, attributes), nullValue());
    assertThat(getAttribute(SIGNERTYPE_KEY, attributes), nullValue());
    assertThat(getAttribute(FORCE_PATH_STYLE_KEY, attributes), nullValue());
  }

  private static void assertSoftQuota(final BlobStoreConfiguration configuration) {
    NestedAttributesMap quotaSettings = configuration.attributes(ROOT_KEY);
    assertThat(getAttribute(TYPE_KEY, quotaSettings), is(QUOTA_TYPE));
    assertThat(parseInt(getAttribute(LIMIT_KEY, quotaSettings)), is((int) QUOTA_LIMIT));
  }

  private static String getAttribute(final String key, final NestedAttributesMap attributes) {
    return Optional.ofNullable(attributes.get(key)).map(Object::toString).orElse(null);
  }

  private static void assertBucketSecurityDetails(final NestedAttributesMap attributes) {
    assertThat(getAttribute(ACCESS_KEY_ID_KEY, attributes), is(AN_IAM_ACCESS_KEY));
    assertThat(getAttribute(SECRET_ACCESS_KEY_KEY, attributes), is(AN_IAM_SECRET_ACCESS_KEY));
    assertThat(getAttribute(ASSUME_ROLE_KEY, attributes), is(AN_IAM_ROLE));
    assertThat(getAttribute(SESSION_TOKEN_KEY, attributes), is(AN_IAM_SESSION_TOKEN));
  }

  private static void assertBucketEncryptionDetails(final NestedAttributesMap attributes) {
    assertThat(getAttribute(ENCRYPTION_TYPE, attributes), is(S3_MANAGED_ENCRYPTION));
    assertThat(getAttribute(ENCRYPTION_KEY, attributes), is(SUPER_SECURE_ENCRYPTION_KEY));
  }

  private static void assertBucketAdvancedConnectionDetails(final NestedAttributesMap attributes) {
    assertThat(getAttribute(ENDPOINT_KEY, attributes), is(S3_ENDPOINT_URL));
    assertThat(getAttribute(SIGNERTYPE_KEY, attributes), is(S3_SIGNER_TYPE));
    assertThat(parseBoolean(getAttribute(FORCE_PATH_STYLE_KEY, attributes)), is(FORCE_PATH_STYLE));
    assertThat(parseInt(getAttribute(MAX_CONNECTION_POOL_KEY, attributes)), is(MAX_CONNECTION_POOL_SIZE));
  }
}
