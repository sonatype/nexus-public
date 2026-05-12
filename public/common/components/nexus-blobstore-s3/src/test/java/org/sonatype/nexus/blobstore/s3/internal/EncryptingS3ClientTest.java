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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
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
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
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
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.ResponseInputStreamTestUtil.getResponseInputStream;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENCRYPTION_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENCRYPTION_TYPE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EncryptingS3ClientTest
{
  @Mock
  S3ServiceClientConfiguration s3Serviceconfiguration;

  @Mock
  private S3Client delegate;

  private EncryptingS3Client clientUnderTest;

  private BlobStoreConfiguration blobStoreConfiguration;

  @BeforeEach
  void setUp() {
    when(delegate.serviceClientConfiguration()).thenReturn(s3Serviceconfiguration);
    when(s3Serviceconfiguration.region()).thenReturn(Region.US_EAST_1);

    blobStoreConfiguration = new MockBlobStoreConfiguration();
    clientUnderTest = new EncryptingS3Client(delegate, blobStoreConfiguration);
  }

  @Test
  void abortMultipartUpload_passesRequestToDelegate() {
    final AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
        .bucket("bucketName")
        .key("objectKey")
        .uploadId("uploadId")
        .build();

    final AbortMultipartUploadResponse response = AbortMultipartUploadResponse.builder().build();
    when(delegate.abortMultipartUpload(request)).thenReturn(response);

    final AbortMultipartUploadResponse result = clientUnderTest.abortMultipartUpload(request);

    verify(delegate).abortMultipartUpload(request);
    assertSame(response, result);
  }

  @Test
  void copyObject_passesRequestToDelegate() {
    final CopyObjectRequest request = CopyObjectRequest.builder()
        .sourceBucket("source-bucket")
        .sourceKey("source-key")
        .destinationBucket("destination-bucket")
        .destinationKey("destination-key")
        .build();

    final CopyObjectResponse response = CopyObjectResponse.builder().build();
    when(delegate.copyObject(request)).thenReturn(response);

    final CopyObjectResponse result = clientUnderTest.copyObject(request);

    verify(delegate).copyObject(request);
    assertSame(response, result);
  }

  @Test
  void copyObject_stringOverload_buildsRequestAndDelegates() {
    final CopyObjectResponse response = CopyObjectResponse.builder().build();
    when(delegate.copyObject(any(CopyObjectRequest.class))).thenReturn(response);

    final CopyObjectResponse result = clientUnderTest.copyObject("src-bucket", "src-key", "dest-bucket", "dest-key");

    final CopyObjectRequest expectedRequest = CopyObjectRequest.builder()
        .sourceBucket("src-bucket")
        .sourceKey("src-key")
        .destinationBucket("dest-bucket")
        .destinationKey("dest-key")
        .build();

    verify(delegate).copyObject(expectedRequest);
    assertSame(response, result);
  }

  @Test
  void copyObject_addsAes256ToRequestsWhenSet() {
    blobStoreConfiguration.setAttributes(Map.of(CONFIG_KEY, Map.of(ENCRYPTION_TYPE, "s3ManagedEncryption")));
    clientUnderTest = new EncryptingS3Client(delegate, blobStoreConfiguration);

    final CopyObjectRequest request = CopyObjectRequest.builder()
        .sourceBucket("src")
        .sourceKey("a")
        .destinationBucket("dst")
        .destinationKey("b")
        .build();

    final CopyObjectResponse response = CopyObjectResponse.builder().build();
    when(delegate.copyObject(any(CopyObjectRequest.class))).thenReturn(response);

    final CopyObjectResponse result = clientUnderTest.copyObject(request);

    final CopyObjectRequest expected = request.toBuilder()
        .serverSideEncryption(ServerSideEncryption.AES256)
        .build();

    verify(delegate).copyObject(expected);
    assertSame(response, result);
  }

  @Test
  void completeMultipartUpload_passesRequestToDelegate() {
    final CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
        .bucket("bucket-name")
        .key("object-key")
        .uploadId("upload-id")
        .build();

    final CompleteMultipartUploadResponse response = CompleteMultipartUploadResponse.builder().build();
    when(delegate.completeMultipartUpload(request)).thenReturn(response);

    final CompleteMultipartUploadResponse result = clientUnderTest.completeMultipartUpload(request);

    verify(delegate).completeMultipartUpload(request);
    assertSame(response, result);
  }

  @Test
  void createBucket_passesRequestToDelegate() {
    final CreateBucketRequest expectedRequest = CreateBucketRequest.builder()
        .bucket("test-bucket")
        .build();

    final CreateBucketResponse response = CreateBucketResponse.builder().build();
    when(delegate.createBucket(expectedRequest)).thenReturn(response);

    final CreateBucketResponse result = clientUnderTest.createBucket("test-bucket");

    verify(delegate).createBucket(expectedRequest);
    assertSame(response, result);
  }

  @Test
  void createMultipartUpload_passesRequestToDelegate() {
    final CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
        .bucket("test-bucket")
        .key("test-key")
        .build();

    final CreateMultipartUploadResponse response = CreateMultipartUploadResponse.builder().build();
    when(delegate.createMultipartUpload(request)).thenReturn(response);

    final CreateMultipartUploadResponse result = clientUnderTest.createMultipartUpload(request);

    verify(delegate).createMultipartUpload(request);
    assertSame(response, result);
  }

  @Test
  void createMultipartUpload_addsKmsAndKeyToCreateMultipartWhenEnabled() {
    blobStoreConfiguration.setAttributes(
        Map.of(CONFIG_KEY, Map.of(ENCRYPTION_TYPE, "kmsManagedEncryption", ENCRYPTION_KEY, "kms-key-123")));
    clientUnderTest = new EncryptingS3Client(delegate, blobStoreConfiguration);

    final CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
        .bucket("bucket")
        .key("key")
        .build();

    final CreateMultipartUploadResponse response = CreateMultipartUploadResponse.builder().build();
    when(delegate.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(response);

    final CreateMultipartUploadResponse result = clientUnderTest.createMultipartUpload(request);

    final CreateMultipartUploadRequest expected = request.toBuilder()
        .serverSideEncryption(ServerSideEncryption.AWS_KMS)
        .ssekmsKeyId("kms-key-123")
        .build();

    verify(delegate).createMultipartUpload(expected);
    assertSame(response, result);
  }

  @Test
  void deleteBucket_passesRequestToDelegate() {
    final DeleteBucketRequest expectedRequest = DeleteBucketRequest.builder()
        .bucket("test-bucket")
        .build();

    final DeleteBucketResponse response = DeleteBucketResponse.builder().build();
    when(delegate.deleteBucket(expectedRequest)).thenReturn(response);

    final DeleteBucketResponse result = clientUnderTest.deleteBucket("test-bucket");

    verify(delegate).deleteBucket(expectedRequest);
    assertSame(response, result);
  }

  @Test
  void deleteBucketLifecycleConfiguration_passesRequestToDelegate() {
    final DeleteBucketLifecycleRequest expectedRequest = DeleteBucketLifecycleRequest.builder()
        .bucket("test-bucket")
        .build();

    clientUnderTest.deleteBucketLifecycleConfiguration("test-bucket");

    verify(delegate).deleteBucketLifecycle(expectedRequest);
  }

  @Test
  void deleteBucketLifecycleConfigurationWithRules_buildsConfigAndDelegates() {
    final LifecycleRule rule = LifecycleRule.builder()
        .id("keep-non-blobstore-rules")
        .build();

    final BucketLifecycleConfiguration newConfig = BucketLifecycleConfiguration.builder()
        .rules(List.of(rule))
        .build();

    final PutBucketLifecycleConfigurationRequest expectedRequest = PutBucketLifecycleConfigurationRequest.builder()
        .bucket("test-bucket")
        .lifecycleConfiguration(newConfig)
        .build();

    clientUnderTest.deleteBucketLifecycleConfigurationWithRules("test-bucket", java.util.List.of(rule));

    verify(delegate).putBucketLifecycleConfiguration(expectedRequest);
  }

  @Test
  void deleteObject_passesRequestToDelegate() {
    final DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket("bucket-name")
        .key("object-key")
        .build();

    clientUnderTest.deleteObject(request);

    verify(delegate).deleteObject(request);
  }

  @Test
  void deleteObject_stringOverload_buildsRequestAndDelegates() {
    final DeleteObjectRequest expectedRequest = DeleteObjectRequest.builder()
        .bucket("bucket-name")
        .key("object-key")
        .build();

    clientUnderTest.deleteObject("bucket-name", "object-key");

    verify(delegate).deleteObject(expectedRequest);
  }

  @Test
  void deleteObjects_passesRequestToDelegate() {
    final DeleteObjectsRequest request = DeleteObjectsRequest.builder()
        .bucket("test-bucket")
        .build();

    final DeleteObjectsResponse response = DeleteObjectsResponse.builder().build();
    when(delegate.deleteObjects(request)).thenReturn(response);

    final DeleteObjectsResponse result = clientUnderTest.deleteObjects(request);

    verify(delegate).deleteObjects(request);
    assertSame(response, result);
  }

  @Test
  void doesBucketExist_returnsTrue_whenHeadBucketSucceeds() {
    final HeadBucketRequest expectedRequest = HeadBucketRequest.builder()
        .bucket("test-bucket")
        .build();

    when(delegate.headBucket(expectedRequest)).thenReturn(HeadBucketResponse.builder().build());

    final boolean result = clientUnderTest.doesBucketExist("test-bucket");

    verify(delegate).headBucket(expectedRequest);
    assertTrue(result);
  }

  @Test
  void doesBucketExist_returnsFalse_whenNoSuchBucket() {
    final HeadBucketRequest expectedRequest = HeadBucketRequest.builder()
        .bucket("missing-bucket")
        .build();

    when(delegate.headBucket(expectedRequest)).thenThrow(NoSuchBucketException.builder().build());

    final boolean result = clientUnderTest.doesBucketExist("missing-bucket");

    verify(delegate).headBucket(expectedRequest);
    assertFalse(result);
  }

  @Test
  void doesObjectExist_returnsTrue_whenHeadObjectSucceeds() {
    final HeadObjectRequest expectedRequest = HeadObjectRequest.builder()
        .bucket("test-bucket")
        .key("test-object")
        .build();

    when(delegate.headObject(expectedRequest)).thenReturn(HeadObjectResponse.builder().build());

    final boolean result = clientUnderTest.doesObjectExist("test-bucket", "test-object");

    verify(delegate).headObject(expectedRequest);
    assertTrue(result);
  }

  @Test
  void doesObjectExist_returnsFalse_whenNoSuchKey() {
    final HeadObjectRequest expectedRequest = HeadObjectRequest.builder()
        .bucket("test-bucket")
        .key("missing-object")
        .build();

    when(delegate.headObject(expectedRequest)).thenThrow(NoSuchKeyException.builder().build());

    final boolean result = clientUnderTest.doesObjectExist("test-bucket", "missing-object");

    verify(delegate).headObject(expectedRequest);
    assertFalse(result);
  }

  @Test
  void generatePresignedUrl_basic_returnsUrl() throws Exception {
    final URL expected = new URL("https://example.com/path/to/object.txt?X-Amz-Signature=abc");
    final String bucketName = "test-bucket";
    final String key = "path/to/object.txt";

    try (final MockedStatic<S3Presigner> presignerMockStatic = mockStatic(S3Presigner.class)) {
      final S3Presigner presigner = mock(S3Presigner.class);
      final PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
      final S3ServiceClientConfiguration config = mock(S3ServiceClientConfiguration.class);
      final S3Presigner.Builder presignerBuildMock = mock(S3Presigner.Builder.class);

      when(delegate.serviceClientConfiguration()).thenReturn(config);

      presignerMockStatic.when(S3Presigner::builder).thenReturn(presignerBuildMock);
      when(presignerBuildMock.build()).thenReturn(presigner);

      when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
      when(presigned.url()).thenReturn(expected);

      clientUnderTest = new EncryptingS3Client(delegate, blobStoreConfiguration);
      final URL result = clientUnderTest.generatePresignedUrl(bucketName, key, Duration.ofMinutes(5));

      final GetObjectPresignRequest expectedPresignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofMinutes(5))
          .getObjectRequest(
              GetObjectRequest.builder().bucket(bucketName).key(key).build())
          .build();
      verify(presigner).presignGetObject(expectedPresignRequest);
      assertSame(expected, result);
    }
  }

  @Test
  void generatePresignedUrl_withHeaders_includesResponseParams() throws Exception {
    final URL expected =
        new URL(
            "https://example.com/file.bin?response-content-disposition=attachment&response-content-type=application%2Foctet-stream");
    final String bucketName = "test-bucket";
    final String key = "file.bin";
    final Duration duration = Duration.ofMinutes(10);
    final String responseHeaderFilename = "my-file.bin";
    final String contentType = "application/octet-stream";

    try (final MockedStatic<S3Presigner> presignerMockStatic = mockStatic(S3Presigner.class)) {
      final S3Presigner presigner = mock(S3Presigner.class);
      final PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
      final S3ServiceClientConfiguration config = mock(S3ServiceClientConfiguration.class);
      final S3Presigner.Builder presignerBuildMock = mock(S3Presigner.Builder.class);

      presignerMockStatic.when(S3Presigner::builder).thenReturn(presignerBuildMock);
      when(presignerBuildMock.build()).thenReturn(presigner);
      when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
      when(presigned.url()).thenReturn(expected);
      when(delegate.serviceClientConfiguration()).thenReturn(config);

      clientUnderTest = new EncryptingS3Client(delegate, blobStoreConfiguration);
      final URL result = clientUnderTest.generatePresignedUrl(
          bucketName,
          key,
          duration,
          responseHeaderFilename,
          contentType);

      ArgumentCaptor<GetObjectPresignRequest> argumentCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
      verify(presigner).presignGetObject(argumentCaptor.capture());
      final GetObjectPresignRequest objectPresignRequestBuilt = argumentCaptor.getValue();

      assertSame(duration, objectPresignRequestBuilt.signatureDuration());
      assertSame(bucketName, objectPresignRequestBuilt.getObjectRequest().bucket());
      assertSame(key, objectPresignRequestBuilt.getObjectRequest().key());
      assertEquals(
          "attachment; filename=\"my-file.bin\"",
          objectPresignRequestBuilt.getObjectRequest().responseContentDisposition());
      assertEquals(contentType, objectPresignRequestBuilt.getObjectRequest().responseContentType());

      assertSame(expected, result);
    }
  }

  @Test
  void generatePresignedUrl_usesDefaultCredentialsProvider() throws Exception {
    final URL expected =
        new URL(
            "https://example.com/file.bin?response-content-disposition=attachment&response-content-type=application%2Foctet-stream");
    final String bucketName = "test-bucket";
    final String key = "file.bin";
    final Duration duration = Duration.ofMinutes(10);

    try (final MockedStatic<S3Presigner> presignerMockStatic = mockStatic(S3Presigner.class)) {
      final S3Presigner presigner = mock(S3Presigner.class);
      final PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
      final S3ServiceClientConfiguration config = mock(S3ServiceClientConfiguration.class);
      final S3Presigner.Builder presignerBuildMock = mock(S3Presigner.Builder.class);

      presignerMockStatic.when(S3Presigner::builder).thenReturn(presignerBuildMock);
      when(presignerBuildMock.build()).thenReturn(presigner);
      when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
      when(presigned.url()).thenReturn(expected);
      when(delegate.serviceClientConfiguration()).thenReturn(config);

      clientUnderTest = new EncryptingS3Client(delegate, blobStoreConfiguration);

      final URL result = clientUnderTest.generatePresignedUrl(
          bucketName,
          key,
          duration,
          null,
          null);

      ArgumentCaptor<GetObjectPresignRequest> argumentCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
      verify(presigner).presignGetObject(argumentCaptor.capture());
      final GetObjectPresignRequest objectPresignRequestBuilt = argumentCaptor.getValue();

      assertSame(duration, objectPresignRequestBuilt.signatureDuration());
      assertSame(bucketName, objectPresignRequestBuilt.getObjectRequest().bucket());
      assertSame(key, objectPresignRequestBuilt.getObjectRequest().key());
      assertSame(expected, result);

      // Verify that a credentials provider was set (DefaultCredentialsProvider)
      verify(presignerBuildMock).credentialsProvider(any());
    }
  }

  @Test
  void getBucketLifecycleConfiguration_passesRequestToDelegate() {
    final GetBucketLifecycleConfigurationRequest expectedRequest = GetBucketLifecycleConfigurationRequest.builder()
        .bucket("test-bucket")
        .build();

    final GetBucketLifecycleConfigurationResponse response = GetBucketLifecycleConfigurationResponse.builder().build();
    when(delegate.getBucketLifecycleConfiguration(expectedRequest)).thenReturn(response);

    final GetBucketLifecycleConfigurationResponse result =
        clientUnderTest.getBucketLifecycleConfiguration("test-bucket");

    verify(delegate).getBucketLifecycleConfiguration(expectedRequest);
    assertSame(response, result);
  }

  @Test
  void getBucketPolicy_passesRequestToDelegate() {
    final GetBucketPolicyRequest expectedRequest = GetBucketPolicyRequest.builder()
        .bucket("test-bucket")
        .build();

    final GetBucketPolicyResponse response = GetBucketPolicyResponse.builder().build();
    when(delegate.getBucketPolicy(expectedRequest)).thenReturn(response);

    final GetBucketPolicyResponse result = clientUnderTest.getBucketPolicy("test-bucket");

    verify(delegate).getBucketPolicy(expectedRequest);
    assertSame(response, result);
  }

  @Test
  void getObject_passesRequestToDelegate() {
    final GetObjectRequest request = GetObjectRequest.builder()
        .bucket("test-bucket")
        .key("test-key")
        .build();

    final ResponseInputStream<GetObjectResponse> response = getResponseInputStream("test content");
    when(delegate.getObject(request)).thenReturn(response);

    final ResponseInputStream<GetObjectResponse> result = clientUnderTest.getObject(request);

    verify(delegate).getObject(request);
    assertSame(response, result);
  }

  @Test
  void getObject_stringOverload_buildsRequestAndDelegates() {
    final GetObjectRequest expectedRequest = GetObjectRequest.builder()
        .bucket("bucket-name")
        .key("object-key")
        .build();

    final ResponseInputStream<GetObjectResponse> response = getResponseInputStream("test content");
    when(delegate.getObject(expectedRequest)).thenReturn(response);

    final ResponseInputStream<GetObjectResponse> result = clientUnderTest.getObject("bucket-name", "object-key");

    verify(delegate).getObject(expectedRequest);
    assertSame(response, result);
  }

  @Test
  void getObjectMetadata_buildsRequestAndDelegates() {
    final HeadObjectRequest expectedRequest = HeadObjectRequest.builder()
        .bucket("test-bucket")
        .key("test-key")
        .build();

    final HeadObjectResponse response = HeadObjectResponse.builder().build();
    when(delegate.headObject(expectedRequest)).thenReturn(response);

    final HeadObjectResponse result = clientUnderTest.getObjectMetadata("test-bucket", "test-key");

    verify(delegate).headObject(expectedRequest);
    assertSame(response, result);
  }

  @Test
  void listObjectsV2_passesRequestToDelegate() {
    final ListObjectsV2Request request = ListObjectsV2Request.builder()
        .bucket("test-bucket")
        .prefix("test-prefix")
        .build();

    final ListObjectsV2Response response = ListObjectsV2Response.builder().build();
    when(delegate.listObjectsV2(request)).thenReturn(response);

    final ListObjectsV2Response result = clientUnderTest.listObjectsV2(request);

    verify(delegate).listObjectsV2(request);
    assertSame(response, result);
  }

  @Test
  void listObjectsV2_stringOverload_buildsRequestAndDelegates() {
    final ListObjectsV2Request expectedRequest = ListObjectsV2Request.builder()
        .bucket("test-bucket")
        .prefix("test-prefix")
        .build();

    final ListObjectsV2Response response = ListObjectsV2Response.builder().build();
    when(delegate.listObjectsV2(expectedRequest)).thenReturn(response);

    final ListObjectsV2Response result = clientUnderTest.listObjectsV2("test-bucket", "test-prefix");

    verify(delegate).listObjectsV2(expectedRequest);
    assertSame(response, result);
  }

  @Test
  void putObject_passesRequestToDelegate() {
    final PutObjectRequest request = PutObjectRequest.builder()
        .bucket("test-bucket")
        .key("test-key")
        .build();

    final RequestBody requestBody = RequestBody.fromString("test content");
    final PutObjectResponse response = PutObjectResponse.builder().build();
    when(delegate.putObject(request, requestBody)).thenReturn(response);

    final PutObjectResponse result = clientUnderTest.putObject(request, requestBody);

    verify(delegate).putObject(request, requestBody);
    assertSame(response, result);
  }

  @Test
  void putObject_addsS3ManagedEncryptionWhenEnabled() {
    blobStoreConfiguration.setAttributes(Map.of(CONFIG_KEY, Map.of(ENCRYPTION_TYPE, "s3ManagedEncryption")));
    clientUnderTest = new EncryptingS3Client(delegate, blobStoreConfiguration);

    final PutObjectRequest request = PutObjectRequest.builder()
        .bucket("test-bucket")
        .key("test-key")
        .build();

    final RequestBody requestBody = RequestBody.fromString("test content");
    final PutObjectResponse response = PutObjectResponse.builder().build();
    when(delegate.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(response);

    final PutObjectResponse result = clientUnderTest.putObject(request, requestBody);

    final PutObjectRequest expected = request.toBuilder()
        .serverSideEncryption(ServerSideEncryption.AES256)
        .build();

    verify(delegate).putObject(expected, requestBody);
    assertSame(response, result);
  }

  @Test
  void uploadPart_passesToDelegate() {
    final UploadPartRequest request = UploadPartRequest.builder()
        .bucket("test-bucket")
        .key("test-key")
        .uploadId("upload-id")
        .partNumber(1)
        .build();

    final RequestBody requestBody = RequestBody.fromString("part content");
    final UploadPartResponse response = UploadPartResponse.builder().build();
    when(delegate.uploadPart(request, requestBody)).thenReturn(response);

    final UploadPartResponse result = clientUnderTest.uploadPart(request, requestBody);

    verify(delegate).uploadPart(request, requestBody);
    assertSame(response, result);
  }

  @Test
  void uploadPartCopy_passesToDelegate() {
    final UploadPartCopyRequest request = UploadPartCopyRequest.builder()
        .sourceBucket("source-bucket")
        .sourceKey("source-key")
        .destinationBucket("dest-bucket")
        .destinationKey("dest-key")
        .uploadId("upload-id")
        .partNumber(1)
        .build();

    final UploadPartCopyResponse response = UploadPartCopyResponse.builder().build();
    when(delegate.uploadPartCopy(request)).thenReturn(response);

    final UploadPartCopyResponse result = clientUnderTest.uploadPartCopy(request);

    verify(delegate).uploadPartCopy(request);
    assertSame(response, result);
  }

  @Test
  void setObjectTagging_passesToDelegate() {
    final PutObjectTaggingRequest request = PutObjectTaggingRequest.builder()
        .bucket("test-bucket")
        .key("test-key")
        .build();

    final PutObjectTaggingResponse response = PutObjectTaggingResponse.builder().build();
    when(delegate.putObjectTagging(request)).thenReturn(response);

    final PutObjectTaggingResponse result = clientUnderTest.setObjectTagging(request);

    verify(delegate).putObjectTagging(request);
    assertSame(response, result);
  }

  @Test
  void uploadWithTransferManger_createsAsyncClientAndUploads() throws Exception {
    // The async client now uses DefaultCredentialsProvider.create() instead of
    // sharing the delegate's credentials provider to avoid STS client shutdown issues with IRSA
    final String bucket = "test-bucket";
    final String key = "test-key";
    final InputStream contents = new ByteArrayInputStream("test content".getBytes());

    final Region region = Region.US_EAST_1;
    final S3ServiceClientConfiguration config = mock(S3ServiceClientConfiguration.class);

    when(delegate.serviceClientConfiguration()).thenReturn(config);
    when(config.region()).thenReturn(region);

    try (final MockedStatic<S3AsyncClient> asyncClientMock = mockStatic(S3AsyncClient.class);
        final MockedStatic<S3TransferManager> transferManagerMock = mockStatic(S3TransferManager.class)) {

      final S3AsyncClient asyncClient = mock(S3AsyncClient.class);
      final S3AsyncClientBuilder asyncClientBuilder = mock(S3AsyncClientBuilder.class);
      final S3TransferManager transferManager = mock(S3TransferManager.class);
      final S3TransferManager.Builder transferManagerBuilder = mock(S3TransferManager.Builder.class);
      final Upload upload = mock(Upload.class);
      final CompletedUpload completedUpload = mock(CompletedUpload.class);
      final CompletableFuture<CompletedUpload> future = CompletableFuture.completedFuture(completedUpload);

      asyncClientMock.when(S3AsyncClient::builder).thenReturn(asyncClientBuilder);
      // Accept any credentials provider (will be DefaultCredentialsProvider)
      when(asyncClientBuilder.credentialsProvider(any())).thenReturn(asyncClientBuilder);

      when(asyncClientBuilder.region(region)).thenReturn(asyncClientBuilder);
      when(asyncClientBuilder.build()).thenReturn(asyncClient);

      transferManagerMock.when(S3TransferManager::builder).thenReturn(transferManagerBuilder);
      when(transferManagerBuilder.s3Client(asyncClient)).thenReturn(transferManagerBuilder);
      when(transferManagerBuilder.build()).thenReturn(transferManager);

      when(transferManager.upload(any(UploadRequest.class))).thenReturn(upload);
      when(upload.completionFuture()).thenReturn(future);

      final CompletedUpload result = clientUnderTest.uploadWithTransferManger(bucket, key, contents);

      assertSame(completedUpload, result);
      verify(transferManager).upload(any(UploadRequest.class));
      // Verify async client was properly closed (try-with-resources)
      verify(asyncClient).close();
    }
  }
}
