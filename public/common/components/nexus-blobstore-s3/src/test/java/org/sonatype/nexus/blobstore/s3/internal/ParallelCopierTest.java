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

import org.junit.Before;
import org.junit.Test;

import org.sonatype.nexus.blobstore.api.BlobStoreException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ParallelCopierTest
{
  private ParallelCopier copier;

  @Mock
  private EncryptingS3Client s3;

  @Mock
  private CreateMultipartUploadResponse createMultipartUploadResponse;

  @Before
  public void setUp() {
    when(createMultipartUploadResponse.uploadId()).thenReturn("uploadId");
    copier = new ParallelCopier(100, 4);
  }

  @Test
  public void testCalcFirstAndLastBytesProperly() {
    assertThat(ParallelCopier.getFirstByte(1, 500), is(0L));
    assertThat(ParallelCopier.getLastByte(1700, 1, 500), is(499L));
    assertThat(ParallelCopier.getFirstByte(2, 500), is(500L));
    assertThat(ParallelCopier.getLastByte(1700, 2, 500), is(999L));
    assertThat(ParallelCopier.getFirstByte(3, 500), is(1000L));
    assertThat(ParallelCopier.getLastByte(1700, 3, 500), is(1499L));
    assertThat(ParallelCopier.getFirstByte(4, 500), is(1500L));
    assertThat(ParallelCopier.getLastByte(1700, 4, 500), is(1699L));
    assertThat(ParallelCopier.getFirstByte(5, 500), is(2000L));
    assertThat(ParallelCopier.getLastByte(1700, 5, 500), is(1699L));
  }

  @Test
  public void testCopyWithMultipartApi() {
    when(s3.createMultipartUpload(anyString(), anyString())).thenReturn(createMultipartUploadResponse);
    when(s3.getObjectMetadata("bucketName", "source")).thenReturn(
        HeadObjectResponse.builder().contentLength(101L).build());
    when(s3.uploadPartCopy(any())).thenReturn(UploadPartCopyResponse.builder()
        .copyPartResult(CopyPartResult.builder().eTag("some-etag").build())
        .build());

    copier.copy(s3, "bucketName", "source", "destination");

    verify(s3).createMultipartUpload(anyString(), anyString());
    verify(s3).getObjectMetadata("bucketName", "source");
    verify(s3, times(2)).uploadPartCopy(any());
    verify(s3).completeMultipartUpload(any());
    verify(s3, never()).abortMultipartUpload(any());
  }

  @Test
  public void testCopyAbortsMultipartOnError() {
    when(s3.createMultipartUpload(any(), any())).thenReturn(createMultipartUploadResponse);
    when(s3.getObjectMetadata("bucketName", "source")).thenReturn(
        HeadObjectResponse.builder().contentLength(101L).build());
    when(s3.uploadPartCopy(any())).thenThrow(SdkClientException.create(""));

    assertThrows(BlobStoreException.class, () -> copier.copy(s3, "bucketName", "source", "destination"));

    verify(s3).createMultipartUpload(anyString(), anyString());
    verify(s3).getObjectMetadata("bucketName", "source");
    verify(s3, atLeastOnce()).uploadPartCopy(any());
    verify(s3).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
  }

  @Test
  public void testCopySplitsParts() {
    when(s3.createMultipartUpload(anyString(), anyString())).thenReturn(createMultipartUploadResponse);
    when(s3.getObjectMetadata("bucketName", "source")).thenReturn(
        HeadObjectResponse.builder().contentLength(345L).build());
    when(s3.uploadPartCopy(any())).thenReturn(UploadPartCopyResponse.builder()
        .copyPartResult(software.amazon.awssdk.services.s3.model.CopyPartResult.builder()
            .eTag("test-etag")
            .build())
        .build());

    copier.copy(s3, "bucketName", "source", "destination");

    verify(s3).createMultipartUpload("bucketName", "destination");
    verify(s3).getObjectMetadata("bucketName", "source");
    verify(s3, times(4)).uploadPartCopy(any());
    verify(s3).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    verify(s3, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
  }

  @Test
  public void testCopyUsesCopyObjectForSmallCopies() {
    when(s3.getObjectMetadata("bucketName", "source")).thenReturn(HeadObjectResponse.builder()
        .contentLength(99L)
        .build());

    copier.copy(s3, "bucketName", "source", "destination");

    verify(s3).getObjectMetadata("bucketName", "source");
    verify(s3).copyObject(anyString(), anyString(), anyString(), anyString());
    verify(s3, never()).createMultipartUpload(any());
  }
}
