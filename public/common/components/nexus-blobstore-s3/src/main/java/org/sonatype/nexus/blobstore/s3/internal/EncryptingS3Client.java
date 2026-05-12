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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper;
import org.sonatype.nexus.blobstore.s3.internal.encryption.KMSEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.NoEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.S3Encrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.S3ManagedEncrypter;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENCRYPTION_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENCRYPTION_TYPE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.FORCE_PATH_STYLE_KEY;
import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;

public class EncryptingS3Client
    implements AutoCloseable
{
  private static final String METRIC_NAME = "encryptingS3Client";

  private final S3Client delegate;

  private final BlobStoreConfiguration blobStoreConfig;

  private final S3Encrypter encrypter;

  private final Timer getTimer;

  private final Timer putTimer;

  private final Timer copyTimer;

  private final Timer uploadPartTimer;

  private final Timer deleteTimer;

  private final Timer setTaggingTimer;

  private final S3Presigner presigner;

  public EncryptingS3Client(
      final S3Client delegate,
      final BlobStoreConfiguration blobStoreConfig)
  {
    this.delegate = delegate;
    this.blobStoreConfig = blobStoreConfig;
    this.presigner = createPresigner(delegate, blobStoreConfig);

    encrypter = getEncrypter(blobStoreConfig);

    MetricRegistry registry = SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME);
    getTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "get"));
    putTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "put"));
    copyTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "copy"));
    uploadPartTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "uploadPart"));
    deleteTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "delete"));
    setTaggingTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "setTagging"));
  }

  @Override
  public void close() throws Exception {
    if (presigner != null) {
      presigner.close();
    }
  }

  public AbortMultipartUploadResponse abortMultipartUpload(final AbortMultipartUploadRequest request) {
    return delegate.abortMultipartUpload(request);
  }

  public CopyObjectResponse copyObject(
      final String sourceBucketName,
      final String sourceKey,
      final String destinationBucketName,
      final String destinationKey)
  {
    return copyObject(CopyObjectRequest.builder()
        .sourceBucket(sourceBucketName)
        .sourceKey(sourceKey)
        .destinationBucket(destinationBucketName)
        .destinationKey(destinationKey)
        .build());
  }

  public CopyObjectResponse copyObject(final CopyObjectRequest request) {
    CopyObjectRequest.Builder requestBuilder = request.toBuilder();
    encrypter.addEncryption(requestBuilder);

    try (final Timer.Context copyContext = copyTimer.time()) {
      return delegate.copyObject(requestBuilder.build());
    }
  }

  public CompleteMultipartUploadResponse completeMultipartUpload(final CompleteMultipartUploadRequest request) {
    return delegate.completeMultipartUpload(request);
  }

  public CreateBucketResponse createBucket(final String bucket) {
    return delegate.createBucket(software.amazon.awssdk.services.s3.model.CreateBucketRequest.builder()
        .bucket(bucket)
        .build());
  }

  public CreateMultipartUploadResponse createMultipartUpload(final String bucket, final String key) {
    return createMultipartUpload(CreateMultipartUploadRequest.builder()
        .bucket(bucket)
        .key(key)
        .build());
  }

  public CreateMultipartUploadResponse createMultipartUpload(final CreateMultipartUploadRequest request) {
    CreateMultipartUploadRequest.Builder encryptedRequestion =
        encrypter.addEncryption(request.toBuilder());

    return delegate.createMultipartUpload(encryptedRequestion.build());
  }

  public DeleteBucketResponse deleteBucket(final String bucket) {
    return delegate.deleteBucket(DeleteBucketRequest.builder()
        .bucket(bucket)
        .build());
  }

  public void deleteBucketLifecycleConfiguration(final String bucket) {
    delegate.deleteBucketLifecycle(DeleteBucketLifecycleRequest.builder()
        .bucket(bucket)
        .build());
  }

  public void deleteBucketLifecycleConfigurationWithRules(
      final String bucket,
      final List<LifecycleRule> nonBlobstoreRules)
  {
    BucketLifecycleConfiguration newConfig = BucketLifecycleConfiguration.builder()
        .rules(nonBlobstoreRules)
        .build();

    delegate.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder()
        .bucket(bucket)
        .lifecycleConfiguration(newConfig)
        .build());
  }

  public void deleteObject(final String bucketName, final String key) {
    deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
  }

  public void deleteObject(final DeleteObjectRequest deleteObjectRequest) {
    try (final Timer.Context deleteContext = deleteTimer.time()) {
      delegate.deleteObject(deleteObjectRequest);
    }
  }

  public DeleteObjectsResponse deleteObjects(final DeleteObjectsRequest deleteObjectsRequest) {
    try (final Timer.Context deleteContext = deleteTimer.time()) {
      return delegate.deleteObjects(deleteObjectsRequest);
    }
  }

  public boolean doesBucketExist(final String bucket) {
    try {
      delegate.headBucket(HeadBucketRequest.builder()
          .bucket(bucket)
          .build());

      return true;
    }
    catch (NoSuchBucketException e) {
      return false;
    }
  }

  public boolean doesObjectExist(final String bucket, final String key) {
    try {
      delegate.headObject(HeadObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build());

      return true;
    }
    catch (NoSuchKeyException e) {
      return false;
    }
  }

  public URL generatePresignedUrl(
      final String bucketName,
      final String key,
      final Duration preSignedUrlDuration)
  {
    return generatePresignedUrl(bucketName, key, preSignedUrlDuration, null, null);
  }

  public URL generatePresignedUrl(
      final String bucketName,
      final String key,
      final Duration preSignedUrlDuration,
      final String responseHeaderFileName,
      final String contentType)
  {
    GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder()
        .bucket(bucketName) // or getConfiguredBucket() if you prefer
        .key(key);

    if (responseHeaderFileName != null) {
      requestBuilder.responseContentDisposition("attachment; filename=\"%s\"".formatted(responseHeaderFileName));
    }
    if (contentType != null) {
      requestBuilder.responseContentType(contentType);
    }

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(preSignedUrlDuration)
        .getObjectRequest(requestBuilder.build())
        .build();

    PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
    return presignedRequest.url();
  }

  public GetBucketLifecycleConfigurationResponse getBucketLifecycleConfiguration(final String bucket) {
    return delegate.getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder()
        .bucket(bucket)
        .build());
  }

  public GetBucketPolicyResponse getBucketPolicy(final String bucket) {
    return delegate.getBucketPolicy(GetBucketPolicyRequest.builder()
        .bucket(bucket)
        .build());
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

  public ResponseInputStream<GetObjectResponse> getObject(final GetObjectRequest getObjectRequest) {
    try (final Timer.Context getContext = getTimer.time()) {
      return delegate.getObject(getObjectRequest);
    }
  }

  public ResponseInputStream<GetObjectResponse> getObject(final String bucketName, final String key) {
    return getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build());
  }

  public HeadObjectResponse getObjectMetadata(final String bucket, final String key) {
    return delegate.headObject(HeadObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build());
  }

  public ListObjectsV2Response listObjectsV2(final ListObjectsV2Request request) {
    return delegate.listObjectsV2(request);
  }

  public ListObjectsV2Response listObjectsV2(final String bucket, final String prefix) {
    return listObjectsV2(ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(prefix)
        .build());
  }

  public Stream<S3Object> listObjectsWithPrefix(final String prefix) {
    final String bucket = S3BlobStoreConfigurationHelper.getConfiguredBucket(blobStoreConfig);

    final ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(prefix);

    return delegate
        .listObjectsV2Paginator(requestBuilder.build())
        .contents()
        .stream();
  }

  public PutObjectResponse putObject(final PutObjectRequest request, final RequestBody requestBody) {
    final PutObjectRequest.Builder requestBuilder = encrypter.addEncryption(request.toBuilder());

    try (final Timer.Context putContext = putTimer.time()) {
      return delegate.putObject(requestBuilder.build(), requestBody);
    }
  }

  public UploadPartResponse uploadPart(final UploadPartRequest uploadPartRequest, final RequestBody requestBody) {
    try (final Timer.Context uploadPartContext = uploadPartTimer.time()) {
      return delegate.uploadPart(uploadPartRequest, requestBody);
    }
  }

  public UploadPartCopyResponse uploadPartCopy(final UploadPartCopyRequest uploadPartCopyRequest) {
    return delegate.uploadPartCopy(uploadPartCopyRequest);
  }

  public PutObjectTaggingResponse setObjectTagging(final PutObjectTaggingRequest setObjectTaggingRequest) {
    try (final Timer.Context setTaggingContext = setTaggingTimer.time()) {
      return delegate.putObjectTagging(setObjectTaggingRequest);
    }
  }

  public CompletedUpload uploadWithTransferManger(
      final String bucket,
      final String key,
      final InputStream contents)
  {
    // Properly close async resources to prevent resource leaks
    // Both S3AsyncClient and S3TransferManager implement AutoCloseable and must be closed.
    // Create a NEW credentials provider instance to avoid sharing with the main S3Client,
    // preventing premature STS client shutdown during IRSA token refresh.
    final S3AsyncClient asyncClient = S3AsyncClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(delegate.serviceClientConfiguration().region())
        .build();

    try (asyncClient) {
      final S3TransferManager transferManager = S3TransferManager.builder()
          .s3Client(asyncClient)
          .build();

      try (transferManager) {
        final PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/best-practices-s3-uploads.html
        // Using the asynchronous API: null can be used when contentLength is not known, adding this comment
        // because this is not stated in the JavaDocs, was a bit hard to find and the Claude Code seemed to get
        // tripped up over this method, trying instead to pass -1L which throws an exception
        final UploadRequest uploadRequest = UploadRequest.builder()
            .putObjectRequest(putRequest)
            .requestBody(AsyncRequestBody.fromInputStream(
                contents,
                null,
                Executors.newSingleThreadExecutor()))
            .build();

        return transferManager.upload(uploadRequest)
            .completionFuture()
            .join();
      }
    }
  }

  /*
   * Constructing the S3Presigner involves classpath scanning by the AWS SDK to identify interceptors
   *
   * IMPORTANT: S3Presigner MUST have its own credentials provider instance, separate from the S3Client.
   * Sharing the same credentials provider causes "Connection pool shut down" errors when using AWS IRSA
   * (AWS_WEB_IDENTITY_TOKEN_FILE) because closing the presigner closes the shared credentials provider's
   * internal STS client used for token refresh.
   *
   * See: https://github.com/aws/aws-sdk-java-v2/issues/4386
   * "The StsClient should not be shut down as long as the credentials provider is in use"
   */
  private static S3Presigner createPresigner(final S3Client delegate, final BlobStoreConfiguration blobStoreConfig) {
    // Create S3Presigner with the same configuration as the main S3 client
    S3Presigner.Builder presignerBuilder = S3Presigner.builder();

    // Copy the same configuration from the delegate S3Client
    presignerBuilder.region(delegate.serviceClientConfiguration().region());

    // Create a NEW credentials provider instance for the presigner
    // instead of sharing the delegate's credentials provider. This prevents the presigner from
    // closing the shared credentials provider's internal STS client when the presigner is closed.
    // The DefaultCredentialsProvider will use the same credential sources (env vars, config files,
    // IRSA token file, etc.) but with its own STS client instance.
    presignerBuilder.credentialsProvider(
        software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create());

    // Copy endpoint override if present
    Optional<URI> endpointOverride = delegate.serviceClientConfiguration().endpointOverride();
    endpointOverride.ifPresent(presignerBuilder::endpointOverride);

    NestedAttributesMap s3Configuration = blobStoreConfig.attributes(CONFIG_KEY);
    if (Objects.equals("true", s3Configuration.get(FORCE_PATH_STYLE_KEY, String.class))) {
      // Copy force path style setting
      presignerBuilder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
    }
    return presignerBuilder.build();
  }
}
