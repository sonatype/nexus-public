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
package org.sonatype.nexus.blobstore.s3.internal;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.sonatype.nexus.blobstore.StorageLocationManager;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.s3.internal.BucketValidationCacheService.BucketValidationResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.getConfiguredBucket;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.getConfiguredEndpoint;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.ACCESS_DENIED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.bucketOwnershipError;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.insufficientCreatePermissionsError;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.unexpectedError;

/**
 * Creates and deletes buckets for the {@link S3BlobStore}.
 *
 * @since 3.16
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BucketManager
    implements StorageLocationManager
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private EncryptingS3Client s3;

  private final BucketValidationCacheService cacheService;

  private final Collection<BucketOperations> bucketOperations;

  @Autowired
  public BucketManager(
      final BucketValidationCacheService cacheService,
      final Collection<BucketOperations> bucketOperations)
  {
    this.cacheService = checkNotNull(cacheService);
    this.bucketOperations = checkNotNull(bucketOperations);
  }

  public void setS3(final EncryptingS3Client s3) {
    this.s3 = s3;
  }

  @Override
  public void prepareStorageLocation(final BlobStoreConfiguration blobStoreConfiguration) {
    String bucket = getConfiguredBucket(blobStoreConfiguration);
    String endpoint = getConfiguredEndpoint(blobStoreConfiguration);

    try {
      BucketValidationResult result = cacheService.validate(s3, endpoint, bucket);

      if (!result.exists()) {
        createBucketAndInvalidateCache(endpoint, bucket);
      }
      else if (!result.ownershipValid()) {
        throw bucketOwnershipError();
      }
    }
    catch (ExecutionException e) {
      throw new BlobStoreException("Failed to validate bucket: " + bucket, e.getCause(), null);
    }
  }

  private void createBucketAndInvalidateCache(final String endpoint, final String bucket) {
    try {
      s3.createBucket(bucket);
      cacheService.invalidate(endpoint, bucket);
    }
    catch (S3Exception e) {
      if (ACCESS_DENIED_CODE.equals(e.awsErrorDetails().errorCode())) {
        log.error("Error creating bucket {}", bucket, e);
        throw insufficientCreatePermissionsError();
      }
      log.info("Error creating bucket {}", bucket, e);
      throw unexpectedError("creating bucket");
    }
  }

  @Override
  public void deleteStorageLocation(final BlobStoreConfiguration blobStoreConfiguration) {
    String bucket = getConfiguredBucket(blobStoreConfiguration);
    ListObjectsV2Response listing = s3.listObjectsV2(bucket, "");
    if (listing.contents().isEmpty()) {
      s3.deleteBucket(bucket);
    }
    else {
      log.info("Not removing S3 bucket {} because it is not empty", bucket);
      bucketOperations.forEach(operation -> {
        operation.delete(blobStoreConfiguration.getName(), bucket, s3);
      });
    }
  }

}
