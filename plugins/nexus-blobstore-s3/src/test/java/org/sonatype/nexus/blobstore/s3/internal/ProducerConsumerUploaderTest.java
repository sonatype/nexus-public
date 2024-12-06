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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreException;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.mockito.Mock;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

public class ProducerConsumerUploaderTest
    extends TestSupport
{

  private ProducerConsumerUploader producerConsumerUploader;

  @Mock
  private AmazonS3 s3;

  @Mock
  private MetricRegistry registry;

  @Mock
  private Timer.Context context;

  @Mock
  private Timer timer;

  @Mock
  private InitiateMultipartUploadResult initiateMultipartUploadResult;

  @Mock
  private Timer readChunk;

  @Mock
  private Timer uploadChunk;

  @Mock
  private Timer multiPartUpload;

  @Before
  public void setUp() throws Exception {
    when(initiateMultipartUploadResult.getUploadId()).thenReturn("uploadId");
    when(timer.time()).thenReturn(context);
    when(registry.timer(anyString())).thenReturn(timer);

    when(readChunk.time()).thenReturn(context);
    when(registry.timer("org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.uploader.readChunk")).thenReturn(readChunk);

    when(uploadChunk.time()).thenReturn(context);
    when(registry.timer("org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.uploader.uploadChunk")).thenReturn(uploadChunk);

    when(multiPartUpload.time()).thenReturn(context);
    when(registry.timer("org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.uploader.multiPartUpload")).thenReturn(multiPartUpload);

    producerConsumerUploader = new ProducerConsumerUploader(100, 4, registry);
    producerConsumerUploader.start();
  }

  @Test
  public void testEmptyStreamCausesUpload() {
    InputStream input = new ByteArrayInputStream(new byte[0]);

    producerConsumerUploader.upload(s3, "bucketName", "key", input);

    verify(s3).putObject(any(), any(), any(), any());
    verify(s3, never()).initiateMultipartUpload(any());
  }

  @Test
  public void testUploadWithMultipartApi() {
    InputStream input = new ByteArrayInputStream(new byte[100]);
    when(s3.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
    when(s3.uploadPart(any())).thenReturn(new UploadPartResult());

    producerConsumerUploader.upload(s3, "bucketName", "key", input);

    verify(s3).initiateMultipartUpload(any());
    verify(s3).uploadPart(any());
    verify(s3).completeMultipartUpload(any());
    verify(s3, never()).abortMultipartUpload(any());
  }

  @Test
  public void testLargerUploadWithMultipartApiEmitMetrics() {
    InputStream input = new ByteArrayInputStream(new byte[350]);
    when(s3.initiateMultipartUpload(any())).thenReturn(new InitiateMultipartUploadResult());
    when(s3.uploadPart(any())).thenReturn(new UploadPartResult());

    producerConsumerUploader.upload(s3, "bucketName", "key", input);

    verify(s3).initiateMultipartUpload(any());
    verify(s3, times(4)).uploadPart(any());
    verify(s3).completeMultipartUpload(any());
    verify(s3, never()).abortMultipartUpload(any());
    verify(multiPartUpload).time();
    verify(readChunk, times(6)).time();
    verify(uploadChunk, times(4)).time();
  }

  @Test
  public void testUploadAbortsMultipartOnError() {
    InputStream input = new ByteArrayInputStream(new byte[100]);
    when(s3.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
    when(s3.uploadPart(any())).thenThrow(new SdkClientException(""));

    assertThrows(BlobStoreException.class, () -> producerConsumerUploader.upload(s3, "bucketName", "key", input));

    verify(s3).initiateMultipartUpload(any());
    verify(s3).uploadPart(any());
    verify(s3).abortMultipartUpload(any());
  }

  @Test
  public void testUploadUsesPutObjectForSmallUploads() {
    InputStream input = new ByteArrayInputStream(new byte[50]);

    producerConsumerUploader.upload(s3, "bucketName", "key", input);

    verify(s3).putObject(any(), any(), any(), any());
    verify(s3, never()).initiateMultipartUpload(any());
  }
}
