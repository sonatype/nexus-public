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

import java.util.function.BiFunction;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.rest.BlobStoreApiSoftQuota;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiAdvancedBucketConnection;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucket;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketConfiguration;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketSecurity;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiEncryption;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiModel;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.LIMIT_KEY;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.ROOT_KEY;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.TYPE_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.*;

/**
 * Transforms a {@link S3BlobStoreApiModel} to an {@link BlobStoreConfiguration}.
 *
 * @since 3.20
 */
public final class S3BlobStoreApiModelMapper
{
  public static final int ONE_MILLION = 1_000_000;

  public static final BiFunction<BlobStoreConfiguration, S3BlobStoreApiModel, BlobStoreConfiguration> MODEL_MAPPER =
      (initialConfig, model) -> {
        final BlobStoreConfiguration blobStoreConfiguration = checkNotNull(initialConfig);
        final S3BlobStoreApiModel request = checkNotNull(model);
        final S3BlobStoreApiBucketConfiguration
            bucketConfiguration = checkNotNull(request.getBucketConfiguration());
        blobStoreConfiguration.setName(request.getName());
        blobStoreConfiguration.setType(TYPE);
        copyBucketConfiguration(bucketConfiguration, blobStoreConfiguration.attributes(CONFIG_KEY));
        copySoftQuota(request.getSoftQuota(), blobStoreConfiguration.attributes(ROOT_KEY));
        return blobStoreConfiguration;
      };

  private static void copyBucketConfiguration(final S3BlobStoreApiBucketConfiguration bucketConfiguration, final NestedAttributesMap s3BucketAttributes) {
    copyGeneralS3BucketSettings(checkNotNull(bucketConfiguration.getBucket()), s3BucketAttributes);
    copyBucketSecuritySettings(bucketConfiguration.getBucketSecurity(), s3BucketAttributes);
    copyBucketEncryptionSettings(bucketConfiguration.getEncryption(), s3BucketAttributes);
    copyAdvancedBucketConnectionSettings(bucketConfiguration.getAdvancedBucketConnection(), s3BucketAttributes);
  }

  private static void copyGeneralS3BucketSettings(
      final S3BlobStoreApiBucket bucket,
      final NestedAttributesMap s3BucketAttributes)
  {
    s3BucketAttributes.set(REGION_KEY, checkNotNull(bucket.getRegion()));
    s3BucketAttributes.set(BUCKET_KEY, checkNotNull(bucket.getName()));
    setAttribute(s3BucketAttributes, BUCKET_PREFIX_KEY, bucket.getPrefix());
    setAttribute(s3BucketAttributes, EXPIRATION_KEY, String.valueOf(bucket.getExpiration()));
  }

  private static void setAttribute(
      final NestedAttributesMap s3BucketAttributes,
      final String key, final String value)
  {
    if (nonNull(value)) {
      s3BucketAttributes.set(key, value);
    }
  }

  private static void copyBucketSecuritySettings(
      final S3BlobStoreApiBucketSecurity bucketSecurity,
      final NestedAttributesMap s3BucketAttributes)
  {
    if (nonNull(bucketSecurity)) {
      setAttribute(s3BucketAttributes, ACCESS_KEY_ID_KEY, bucketSecurity.getAccessKeyId());
      setAttribute(s3BucketAttributes, SECRET_ACCESS_KEY_KEY, bucketSecurity.getSecretAccessKey());
      setAttribute(s3BucketAttributes, ASSUME_ROLE_KEY, bucketSecurity.getRole());
      setAttribute(s3BucketAttributes, SESSION_TOKEN_KEY, bucketSecurity.getSessionToken());
    }
  }

  private static void copyBucketEncryptionSettings(
      final S3BlobStoreApiEncryption encryption,
      final NestedAttributesMap s3BucketAttributes)
  {
    if (nonNull(encryption)) {
      setAttribute(s3BucketAttributes, ENCRYPTION_KEY, encryption.getEncryptionKey());
      setAttribute(s3BucketAttributes, ENCRYPTION_TYPE, encryption.getEncryptionType());
    }
  }

  private static void copyAdvancedBucketConnectionSettings(
      final S3BlobStoreApiAdvancedBucketConnection advancedBucketConnection,
      final NestedAttributesMap s3BucketAttributes)
  {
    if (nonNull(advancedBucketConnection)) {
      setAttribute(s3BucketAttributes, ENDPOINT_KEY, advancedBucketConnection.getEndpoint());
      setAttribute(s3BucketAttributes, SIGNERTYPE_KEY, advancedBucketConnection.getSignerType());
      setForcePathStyleIfTrue(advancedBucketConnection.getForcePathStyle(), s3BucketAttributes);
    }
  }

  private static void setForcePathStyleIfTrue(
      final Boolean forcePathStyle,
      final NestedAttributesMap s3BucketAttributes)
  {
    if (nonNull(forcePathStyle) && forcePathStyle) {
      setAttribute(s3BucketAttributes, FORCE_PATH_STYLE_KEY, Boolean.TRUE.toString());
    }
  }

  private static void copySoftQuota(
      final BlobStoreApiSoftQuota softQuota,
      final NestedAttributesMap softQuotaAttributes)
  {
    if (nonNull(softQuota)) {
      softQuotaAttributes.set(TYPE_KEY, checkNotNull(softQuota.getType()));
      final Long softQuotaLimit = checkNotNull(softQuota.getLimit());
      softQuotaAttributes.set(LIMIT_KEY, softQuotaLimit * ONE_MILLION);
    }
  }
}
