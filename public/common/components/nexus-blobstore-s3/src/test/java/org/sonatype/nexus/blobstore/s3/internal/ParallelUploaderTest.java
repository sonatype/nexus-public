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

import org.junit.Before;
import org.junit.Test;

import org.sonatype.nexus.blobstore.api.BlobStoreException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import org.mockito.Mock;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ParallelUploaderTest
{

  private ParallelUploader parallelUploader;

  @Mock
  private EncryptingS3Client s3;

  @Mock
  private CreateMultipartUploadResponse createMultipartUploadResponse;

  @Before
  public void setUp() {
    when(createMultipartUploadResponse.uploadId()).thenReturn("some-upload-sid");

    parallelUploader = new ParallelUploader(100, 4);
  }

  @Test
  public void testEmptyStreamCausesUpload() {
    InputStream input = new ByteArrayInputStream(new byte[0]);
    parallelUploader.upload(s3, "bucketName", "key", input);

    verify(s3).putObject(any(), any());
    verify(s3, times(0)).createMultipartUpload(any());
  }

  @Test
  public void testUploadWithMultipartApi() {
    InputStream input = new ByteArrayInputStream(new byte[100]);
    when(s3.createMultipartUpload(anyString(), anyString())).thenReturn(createMultipartUploadResponse);
    when(s3.uploadPart(any(), any())).thenReturn(UploadPartResponse.builder().eTag("some-etag").build());

    parallelUploader.upload(s3, "bucketName", "key", input);

    verify(s3).createMultipartUpload(anyString(), anyString());
    verify(s3).uploadPart(any(), any());
    verify(s3).completeMultipartUpload(any());
    verify(s3, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
  }

  @Test
  public void testUploadAbortsMultipartUploadsOnError() {
    InputStream input = new ByteArrayInputStream(new byte[100]);
    when(s3.createMultipartUpload(anyString(), anyString())).thenReturn(createMultipartUploadResponse);
    when(s3.uploadPart(any(), any())).thenThrow(SdkClientException.builder().build());

    assertThrows(BlobStoreException.class, () -> parallelUploader.upload(s3, "bucketName", "key", input));

    verify(s3).createMultipartUpload("bucketName", "key");
    verify(s3).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
    verify(s3).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
  }

  @Test
  public void testUploadUsesPutObjectForSmallUploads() {
    InputStream input = new ByteArrayInputStream(new byte[50]);
    parallelUploader.upload(s3, "bucketName", "key", input);

    verify(s3).putObject(any(), any());
    verify(s3, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
  }
}
