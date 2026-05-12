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
package org.sonatype.nexus.blobstore.s3.internal.encryption;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class S3ManagedEncrypterTest
{

  @Mock
  private CreateMultipartUploadRequest.Builder createMultipartUploadRequestBuilder;

  @Mock
  private PutObjectRequest.Builder putObjectRequestBuilder;

  @Mock
  private CopyObjectRequest.Builder copyObjectRequestBuilder;

  private final S3ManagedEncrypter encrypter = new S3ManagedEncrypter();

  @Test
  public void testS3ManagedServerSideEncWorksForCreateMultipartUploadRequest() {
    when(createMultipartUploadRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(createMultipartUploadRequestBuilder);

    encrypter.addEncryption(createMultipartUploadRequestBuilder);

    verify(createMultipartUploadRequestBuilder).serverSideEncryption(ServerSideEncryption.AES256);
  }

  @Test
  public void testS3ManagedServerSideEncWorksForPutObjectRequest() {
    when(putObjectRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(putObjectRequestBuilder);

    encrypter.addEncryption(putObjectRequestBuilder);

    verify(putObjectRequestBuilder).serverSideEncryption(ServerSideEncryption.AES256);
  }

  @Test
  public void testS3ManagedServerSideEncWorksForCopyObjectRequest() {
    when(copyObjectRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(copyObjectRequestBuilder);

    encrypter.addEncryption(copyObjectRequestBuilder);

    verify(copyObjectRequestBuilder).serverSideEncryption(ServerSideEncryption.AES256);
  }
}
