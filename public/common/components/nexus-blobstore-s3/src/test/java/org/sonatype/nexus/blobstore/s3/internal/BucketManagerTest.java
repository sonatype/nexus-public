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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.s3.internal.BucketValidationCacheService.BucketValidationResult;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.ACCESS_DENIED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.BUCKET_OWNERSHIP_ERR_MSG;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.INSUFFICIENT_PERM_CREATE_BUCKET_ERR_MSG;

/**
 * {@link BucketManager} tests.
 */
class BucketManagerTest
    extends Test5Support
{
  @Mock
  private EncryptingS3Client s3;

  @Mock
  private BucketOperations bucketOperations;

  @Mock
  private BucketValidationCacheService cacheService;

  private BucketManager underTest;

  @BeforeEach
  void setup() throws Exception {
    lenient().when(cacheService.validate(any(), any(), anyString()))
        .thenReturn(
            new BucketValidationResult(true, true));
    underTest = new BucketManager(cacheService, List.of(bucketOperations));
    underTest.setS3(s3);
  }

  @Test
  void deleteStorageLocationRemovesBucketIfEmpty() {
    ListObjectsV2Response listingMock = mock(ListObjectsV2Response.class);
    when(s3.listObjectsV2(anyString(), anyString())).thenReturn(listingMock);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(s3).deleteBucket("mybucket");
  }

  @Test
  void deleteStorageLocationDoesNotRemoveBucketIfNotEmpty() {
    ListObjectsV2Response listingMock = mock(ListObjectsV2Response.class);
    when(listingMock.contents()).thenReturn(List.of(S3Object.builder().build(), S3Object.builder().build()));
    when(s3.listObjectsV2("mybucket", "")).thenReturn(listingMock);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(s3, times(0)).deleteBucket("mybucket");
  }

  @Test
  void deleteCallsBucketOperationsImpl() {
    ListObjectsV2Response listingMock = mock(ListObjectsV2Response.class);
    when(listingMock.contents()).thenReturn(List.of(S3Object.builder().build(), S3Object.builder().build()));
    when(s3.listObjectsV2("mybucket", "")).thenReturn(listingMock);

    underTest.setS3(s3);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setName("my_s3_blob_store");
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(bucketOperations).delete("my_s3_blob_store", "mybucket", s3);
  }

  @Test
  void testBucketCreationAccessDeniedError() throws Exception {
    String bucketName = "bucketName";
    S3Exception s3Exception = mock(S3Exception.class);
    when(s3Exception.awsErrorDetails()).thenReturn(
        AwsErrorDetails.builder()
            .errorCode(ACCESS_DENIED_CODE)
            .build());
    when(cacheService.validate(any(), any(), anyString())).thenReturn(
        new BucketValidationResult(false, true));
    when(s3.createBucket(anyString())).thenThrow(s3Exception);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals(INSUFFICIENT_PERM_CREATE_BUCKET_ERR_MSG, ex.getMessage());
  }

  @Test
  void testBucketCreationUnexpectedError() throws Exception {
    String bucketName = "bucketName";
    S3Exception s3Exception = mock(S3Exception.class);
    when(s3Exception.awsErrorDetails()).thenReturn(
        AwsErrorDetails.builder()
            .errorCode("Some_Unexpected_Code")
            .build());
    when(cacheService.validate(any(), any(), anyString())).thenReturn(
        new BucketValidationResult(false, true));
    when(s3.createBucket(anyString())).thenThrow(s3Exception);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals("An unexpected error occurred creating bucket. Check the logs for more details.", ex.getMessage());
  }

  @Test
  void testExecutionExceptionWrapping() throws Exception {
    String bucketName = "bucketName";
    S3Exception s3Exception = mock(S3Exception.class);

    ExecutionException executionException = new ExecutionException(s3Exception);
    when(cacheService.validate(any(), any(), anyString())).thenThrow(executionException);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(BlobStoreException.class,
        () -> underTest.prepareStorageLocation(cfg));
    assertThat(ex.getMessage(), startsWith("Failed to validate bucket: " + bucketName));
  }

  @Test
  void testOwnershipValidationFailure() throws Exception {
    String bucketName = "bucketName";
    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    when(cacheService.validate(any(), any(), anyString())).thenReturn(
        new BucketValidationResult(true, false));

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals(BUCKET_OWNERSHIP_ERR_MSG, ex.getMessage());
  }
}
