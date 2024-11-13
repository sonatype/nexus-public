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
package com.amazonaws.services.s3;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore;
import org.sonatype.nexus.blobstore.s3.internal.encryption.KMSEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.NoEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.S3Encrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.S3ManagedEncrypter;

import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingResult;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENCRYPTION_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENCRYPTION_TYPE;

/**
 * Extends the {@link AmazonS3Client} with custom behavior that adds server side encryption to requests.
 *
 * The odd packaging is due to {@link AmazonS3ClientParams}
 *
 * @since 3.19
 */
public class EncryptingAmazonS3Client
    extends AmazonS3Client
{
  private static final String METRIC_NAME = "encryptingS3Client";

  private final S3Encrypter encrypter;

  private final Timer getTimer;

  private final Timer putTimer;

  private final Timer copyTimer;

  private final Timer uploadPartTimer;

  private final Timer deleteTimer;

  private final Timer setTaggingTimer;

  public EncryptingAmazonS3Client(
      final BlobStoreConfiguration blobStoreConfig,
      final AmazonS3ClientParams s3ClientParams)
  {
    super(s3ClientParams);
    encrypter = getEncrypter(blobStoreConfig);

    MetricRegistry registry = SharedMetricRegistries.getOrCreate("nexus");
    getTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "get"));
    putTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "put"));
    copyTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "copy"));
    uploadPartTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "uploadPart"));
    deleteTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "delete"));
    setTaggingTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "setTagging"));
  }

  private S3Encrypter getEncrypter(final BlobStoreConfiguration blobStoreConfig) {
    Optional<String> encryptionType = ofNullable(
        blobStoreConfig.attributes(CONFIG_KEY).get(ENCRYPTION_TYPE, String.class));
    return encryptionType.map(id -> {
      if (S3ManagedEncrypter.ID.equals(id)) {
        return new S3ManagedEncrypter();
      }
      else if (KMSEncrypter.ID.equals(id)) {
        Optional<String> key = ofNullable(
            blobStoreConfig.attributes(CONFIG_KEY).get(ENCRYPTION_KEY, String.class));
        return new KMSEncrypter(key);
      }
      else if (NoEncrypter.ID.equals(id)) {
        return NoEncrypter.INSTANCE;
      }
      else {
        throw new IllegalStateException("Failed to find encrypter for id:" + id);
      }
    }).orElse(NoEncrypter.INSTANCE);
  }

  @Override
  public CopyObjectResult copyObject(final CopyObjectRequest request) {
    encrypter.addEncryption(request);

    try (final Timer.Context copyContext = copyTimer.time()) {
      return super.copyObject(request);
    }
  }

  @Override
  public CopyObjectResult copyObject(
      final String sourceBucketName, final String sourceKey,
      final String destinationBucketName, final String destinationKey)
  {
    return copyObject(new CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName, destinationKey));
  }

  @Override
  public InitiateMultipartUploadResult initiateMultipartUpload(final InitiateMultipartUploadRequest request) {
    encrypter.addEncryption(request);
    return super.initiateMultipartUpload(request);
  }

  @Override
  public PutObjectResult putObject(final String bucketName, final String key, final File file) {
    return putObject(new PutObjectRequest(bucketName, key, file));
  }

  @Override
  public PutObjectResult putObject(final String bucketName, final String key, final String redirectLocation) {
    return putObject(new PutObjectRequest(bucketName, key, redirectLocation));
  }

  @Override
  public PutObjectResult putObject(
      final String bucketName, final String key,
      final InputStream input, final ObjectMetadata metadata)
  {
    return putObject(new PutObjectRequest(bucketName, key, input, metadata));
  }

  @Override
  public PutObjectResult putObject(final PutObjectRequest request) {
    encrypter.addEncryption(request);
    try (final Timer.Context putContext = putTimer.time()) {
      return super.putObject(request);
    }
  }

  @Override
  public S3Object getObject(GetObjectRequest getObjectRequest) {
    try (final Timer.Context getContext = getTimer.time()) {
      return super.getObject(getObjectRequest);
    }
  }

  @Override
  public S3Object getObject(String bucketName, String key)
  {
    return getObject(new GetObjectRequest(bucketName, key));
  }

  @Override
  public UploadPartResult uploadPart(UploadPartRequest uploadPartRequest) {
    try (final Timer.Context uploadPartContext = uploadPartTimer.time()) {
      return super.uploadPart(uploadPartRequest);
    }
  }

  @Override
  public void deleteObject(String bucketName, String key)
  {
    deleteObject(new DeleteObjectRequest(bucketName, key));
  }

  @Override
  public void deleteObject(DeleteObjectRequest deleteObjectRequest) {
    try (final Timer.Context deleteContext = deleteTimer.time()) {
      super.deleteObject(deleteObjectRequest);
    }
  }

  @Override
  public SetObjectTaggingResult setObjectTagging(SetObjectTaggingRequest setObjectTaggingRequest) {
    try (final Timer.Context setTaggingContext = setTaggingTimer.time()) {
      return super.setObjectTagging(setObjectTaggingRequest);
    }
  }
}
