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
package org.sonatype.nexus.api.rest.common.blobstore.s3;

import java.security.InvalidKeyException;
import java.util.List;
import java.util.Objects;

import org.sonatype.nexus.api.rest.common.blobstore.model.BlobStoreApiSoftQuota;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiAdvancedBucketConnection;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiBucket;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiBucketConfiguration;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiBucketSecurity;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiCloudFront;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiEncryption;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiFailoverBucket;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.S3BlobStoreApiGlobalDistribution;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiModel;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiTransferAcceleration;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationBuilder;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.ssl.CertificateUtil;

import jakarta.ws.rs.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Transforms a {@link S3BlobStoreApiModel} to an {@link BlobStoreConfiguration}.
 *
 * @since 3.20
 */
public final class S3BlobStoreApiModelMapper
{
  private static final Logger log = LoggerFactory.getLogger(S3BlobStoreApiModelMapper.class);

  public static BlobStoreConfiguration map(
      final BlobStoreConfiguration blobStoreConfiguration,
      final S3BlobStoreApiModel request,
      final ApplicationVersion applicationVersion)
  {
    requireNonNull(blobStoreConfiguration);
    requireNonNull(request);
    final S3BlobStoreApiBucketConfiguration bucketConfiguration = requireNonNull(request.getBucketConfiguration());
    S3BlobStoreApiBucket bucket =
        Objects.requireNonNull(bucketConfiguration.getBucket(), "Missing bucket configuration");

    final Boolean preSignedUrlEnabled = bucketConfiguration.getPreSignedUrlEnabled();
    if (Boolean.TRUE.equals(preSignedUrlEnabled) && !applicationVersion.isAnyProEdition()) {
      throw new PreSignedUrlNotAllowedException(
          "Pre-signed URL feature is only available in Nexus Repository Manager Pro");
    }

    S3BlobStoreConfigurationBuilder builder =
        S3BlobStoreConfigurationBuilder.builder(blobStoreConfiguration, request.getName())
            .bucket(bucket.getName())
            .region(bucket.getRegion())
            .prefix(bucket.getPrefix())
            .preSignedUrlEnabled(preSignedUrlEnabled);

    S3BlobStoreApiBucketSecurity bucketSecurity = bucketConfiguration.getBucketSecurity();
    if (bucketSecurity != null) {
      builder.accessKey(bucketSecurity.getAccessKeyId());
      builder.accessSecret(bucketSecurity.getSecretAccessKey());
      builder.assumeRole(bucketSecurity.getRole());
      builder.sessionTokenKey(bucketSecurity.getSessionToken());
    }

    S3BlobStoreApiEncryption encryption = bucketConfiguration.getEncryption();
    if (encryption != null) {
      builder.encryptionKey(encryption.getEncryptionKey());
      builder.encryptionType(encryption.getEncryptionType());
    }

    S3BlobStoreApiAdvancedBucketConnection advanced = bucketConfiguration.getAdvancedBucketConnection();
    if (advanced != null) {
      builder.endpoint(advanced.getEndpoint());
      builder.signerType(advanced.getSignerType());
      builder.maxConnectionPool(advanced.getMaxConnectionPoolSize());
      builder.forcePathStyle(advanced.getForcePathStyle());
    }

    List<S3BlobStoreApiFailoverBucket> failoverBuckets = bucketConfiguration.getFailoverBuckets();
    if (failoverBuckets != null) {
      failoverBuckets.forEach(failover -> builder.failover(failover.getRegion(), failover.getBucketName()));
    }

    BlobStoreApiSoftQuota softQuota = request.getSoftQuota();
    if (softQuota != null) {
      builder.quotaConfig(softQuota.getType(), requireNonNull(softQuota.getLimit(), "Missing quota limit"));
    }

    S3BlobStoreApiGlobalDistribution globalDistribution = request.getGlobalDistribution();

    if (globalDistribution instanceof S3BlobStoreApiTransferAcceleration) {
      builder.transferAccelerationEnabled();
    }
    else if (globalDistribution instanceof S3BlobStoreApiCloudFront cloudFront) {
      String privateKey = cloudFront.privateKey();
      String privateKeyPassword = cloudFront.privateKeyPassword();
      if (!Strings2.isBlank(privateKeyPassword)) {
        try {
          privateKey = CertificateUtil.removePemPassphrase(privateKey, privateKeyPassword);
        }
        catch (InvalidKeyException e) {
          log.debug("Failed to decrypt certificate", e);
          throw new BadRequestException("Unable to decrypt certificate");
        }
      }
      builder.cloudFrontEnabled(cloudFront.distributionUrl(), cloudFront.keyId(), privateKey);
    }

    return builder.build();
  }
}
