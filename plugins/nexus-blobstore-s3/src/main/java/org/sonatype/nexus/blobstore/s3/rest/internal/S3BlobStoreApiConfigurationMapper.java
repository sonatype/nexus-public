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

import java.util.Objects;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.rest.BlobStoreApiSoftQuota;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiAdvancedBucketConnection;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucket;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketConfiguration;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiBucketSecurity;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiEncryption;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiModel;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import static java.lang.Long.parseLong;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.LIMIT_KEY;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.ROOT_KEY;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.TYPE_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.*;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_PREFIX_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;

/**
 * Transforms a {@link BlobStoreConfiguration} to an {@link S3BlobStoreApiModel}.
 *
 * @since 3.20
 */
public final class S3BlobStoreApiConfigurationMapper
{
  public static S3BlobStoreApiModel map(final BlobStoreConfiguration configuration) {
    return new S3BlobStoreApiModel(
        configuration.getName(),
        createSoftQuota(configuration),
        createS3BlobStoreBucketConfiguration(configuration)
    );
  }

  private static BlobStoreApiSoftQuota createSoftQuota(final BlobStoreConfiguration configuration) {
    final NestedAttributesMap softQuotaAttributes = configuration.attributes(ROOT_KEY);
    if (!softQuotaAttributes.isEmpty()) {
      final String quotaType = getValue(softQuotaAttributes, TYPE_KEY);
      final String quotaLimit = getValue(softQuotaAttributes, LIMIT_KEY);
      if (nonNull(quotaType) && nonNull(quotaLimit)) {
        final BlobStoreApiSoftQuota blobStoreApiSoftQuota = new BlobStoreApiSoftQuota();
        blobStoreApiSoftQuota.setType(quotaType);
        blobStoreApiSoftQuota.setLimit(parseLong(quotaLimit));
        return blobStoreApiSoftQuota;
      }
    }
    return null;
  }

  private static String getValue(final NestedAttributesMap attributes, final String key) {
    return Objects.toString(attributes.get(key), null);
  }

  private static S3BlobStoreApiBucketConfiguration createS3BlobStoreBucketConfiguration(final BlobStoreConfiguration configuration) {
    final NestedAttributesMap s3BucketAttributes = configuration.attributes(CONFIG_KEY);
    return new S3BlobStoreApiBucketConfiguration(
        buildS3BlobStoreBucket(s3BucketAttributes),
        buildS3BlobStoreBucketSecurity(s3BucketAttributes),
        buildS3BlobStoreEncryption(s3BucketAttributes),
        buildS3BlobStoreAdvancedBucketConnection(s3BucketAttributes));
  }

  private static S3BlobStoreApiBucket buildS3BlobStoreBucket(final NestedAttributesMap attributes) {
    final String expiration = getValue(attributes, EXPIRATION_KEY);
    return new S3BlobStoreApiBucket(
        getValue(attributes, REGION_KEY),
        getValue(attributes, BUCKET_KEY),
        getValue(attributes, BUCKET_PREFIX_KEY),
        Integer.valueOf(nonNull(expiration) ? expiration : "0")
    );
  }

  private static S3BlobStoreApiBucketSecurity buildS3BlobStoreBucketSecurity(final NestedAttributesMap attributes) {
    final String accessKeyId = getValue(attributes, ACCESS_KEY_ID_KEY);
    final String roleToAssume = getValue(attributes, ASSUME_ROLE_KEY);
    final String sessionToken = getValue(attributes, SESSION_TOKEN_KEY);
    if (iamCredentialsProvided(accessKeyId, roleToAssume, sessionToken)) {
      return new S3BlobStoreApiBucketSecurity(accessKeyId, null, roleToAssume, sessionToken);
    }
    return null;
  }

  private static boolean iamCredentialsProvided(
      final String accessKeyId,
      final String roleToAssume,
      final String sessionToken)
  {
    return anyNonNull(accessKeyId, roleToAssume, sessionToken);
  }

  private static S3BlobStoreApiEncryption buildS3BlobStoreEncryption(final NestedAttributesMap s3BucketAttributes) {
    final String encryptionType = getValue(s3BucketAttributes, ENCRYPTION_TYPE);
    final String encryptionKey = getValue(s3BucketAttributes, ENCRYPTION_KEY);

    if (anyNonNull(encryptionKey, encryptionType)) {
      return new S3BlobStoreApiEncryption(encryptionType, encryptionKey);
    }
    return null;
  }

  private static S3BlobStoreApiAdvancedBucketConnection buildS3BlobStoreAdvancedBucketConnection(final NestedAttributesMap attributes) {
    final String endpoint = getValue(attributes, ENDPOINT_KEY);
    final String signerType = getValue(attributes, SIGNERTYPE_KEY);
    final String forcePathStyle = getValue(attributes, FORCE_PATH_STYLE_KEY);
    Integer maxConnectionPoolSize =
        Optional.ofNullable(getValue(attributes, MAX_CONNECTION_POOL_KEY)).filter(val -> !"".equals(val))
            .map(Integer::valueOf).orElse(null);

    if (anyNonNull(endpoint, signerType, forcePathStyle, maxConnectionPoolSize)) {
      return new S3BlobStoreApiAdvancedBucketConnection(endpoint, signerType, Boolean.valueOf(forcePathStyle),
          maxConnectionPoolSize);
    }
    return null;
  }

  private static boolean anyNonNull(final Object... objects) {
    for (Object o : objects) {
      if (o != null) {
        return true;
      }
    }
    return false;
  }
}
