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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.inject.Inject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.ACCESS_DENIED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.METHOD_NOT_ALLOWED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.NO_SUCH_BUCKET_POLICY_CODE;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
public class BucketValidationCacheService
    extends ComponentSupport
{
  /**
   * ThreadLocal so that we can use the bucket's client during a cache miss. We do not include this in BucketReference
   * as we do not want to hold references to the S3Client which should be disposed.
   */
  private final ThreadLocal<EncryptingS3Client> s3Holder = new ThreadLocal<>();

  private final LoadingCache<BucketReference, BucketValidationResult> cache;

  private final BucketOwnershipCheckFeatureFlag ownershipCheckFeatureFlag;

  @Inject
  public BucketValidationCacheService(
      @Value("${nexus.blobstore.s3.bucket.validation.cache.ttl:60}") final int ttl,
      @Value("${nexus.blobstore.s3.bucket.validation.cache.maxSize:100}") final int maxSize,
      final BucketOwnershipCheckFeatureFlag ownershipCheckFeatureFlag)
  {
    this.ownershipCheckFeatureFlag = checkNotNull(ownershipCheckFeatureFlag);
    this.cache = CacheBuilder.newBuilder()
        .expireAfterWrite(ttl, TimeUnit.SECONDS)
        .maximumSize(maxSize)
        .recordStats()
        .build(CacheLoader.from(this::performS3Validation));
  }

  public BucketValidationResult validate(
      final EncryptingS3Client s3,
      final String endpoint,
      final String bucket) throws ExecutionException
  {
    try {
      s3Holder.set(checkNotNull(s3));
      return cache.get(new BucketReference(endpoint, bucket));
    }
    finally {
      s3Holder.set(null);
    }
  }

  public void invalidate(final String endpoint, final String bucket) {
    log.debug("Invalidating cache for endpoint {} bucket: {}", endpoint, bucket);
    cache.invalidate(new BucketReference(endpoint, bucket));
  }

  private BucketValidationResult performS3Validation(final BucketReference reference) {
    log.debug("Performing S3 validation for: {}", reference);

    EncryptingS3Client s3 = s3Holder.get();
    boolean exists = s3.doesBucketExist(reference.bucketName());
    boolean ownershipValid = false;

    if (exists && !ownershipCheckFeatureFlag.isDisabled()) {
      ownershipValid = validateOwnership(reference.bucketName());
    }
    else if (exists) {
      // Feature flag disabled - assume ownership is valid since the check is explicitly skipped
      ownershipValid = true;
    }

    return new BucketValidationResult(exists, ownershipValid);
  }

  private boolean validateOwnership(final String bucket) {
    try {
      s3Holder.get().getBucketPolicy(bucket);
      return true;
    }
    catch (S3Exception e) {
      String errorCode = e.awsErrorDetails().errorCode();
      String logMessage = String.format("Exception thrown checking ownership of \"%s\" bucket.", bucket);

      if (ACCESS_DENIED_CODE.equals(errorCode)) {
        log.debug(logMessage, e);
        return false;
      }
      if (METHOD_NOT_ALLOWED_CODE.equals(errorCode)) {
        log.debug(logMessage, e);
        return false;
      }
      if (NO_SUCH_BUCKET_POLICY_CODE.equals(errorCode)) {
        // AWS SDK 2.x: NoSuchBucketPolicy means bucket has default permissions - this is normal and valid
        log.debug("Bucket \"{}\" has no bucket policy (using default permissions) - ownership check passed", bucket);
        return true;
      }

      log.info(logMessage, log.isDebugEnabled() ? e : e.getMessage());
      throw e;
    }
  }

  public record BucketValidationResult(boolean exists, boolean ownershipValid)
  {
  }

  private static record BucketReference(String endpoint, String bucketName)
  {
  }
}
