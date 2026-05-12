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

import java.util.Optional;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class KMSEncrypterTest
{

  @Mock
  private CreateMultipartUploadRequest.Builder createMultipartUploadRequestBuilder;

  @Mock
  private PutObjectRequest.Builder putObjectRequestBuilder;

  @Mock
  private CopyObjectRequest.Builder copyObjectRequestBuilder;

  @Test
  public void testConstructorHandlesKmsId() {
    assertThat(new KMSEncrypter(Optional.empty()).getKmsKeyId(), nullValue());
    assertThat(new KMSEncrypter(Optional.of("")).getKmsKeyId(), nullValue());
    assertThat(new KMSEncrypter(Optional.of(" ")).getKmsKeyId(), nullValue());
    assertThat(new KMSEncrypter(Optional.of("   ")).getKmsKeyId(), nullValue());
    assertThat(new KMSEncrypter(Optional.of("aProperKeyId")).getKmsKeyId(), is("aProperKeyId"));
  }

  @Test
  public void testSupplyingNoKmsIdAddsCorrectKmsParameters() {
    KMSEncrypter kmsEncrypter = new KMSEncrypter();

    when(createMultipartUploadRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(createMultipartUploadRequestBuilder);
    kmsEncrypter.addEncryption(createMultipartUploadRequestBuilder);
    verify(createMultipartUploadRequestBuilder).serverSideEncryption(ServerSideEncryption.AWS_KMS);

    when(putObjectRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(putObjectRequestBuilder);
    kmsEncrypter.addEncryption(putObjectRequestBuilder);
    verify(putObjectRequestBuilder).serverSideEncryption(ServerSideEncryption.AWS_KMS);

    when(copyObjectRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(copyObjectRequestBuilder);
    kmsEncrypter.addEncryption(copyObjectRequestBuilder);
    verify(copyObjectRequestBuilder).serverSideEncryption(ServerSideEncryption.AWS_KMS);
  }

  @Test
  public void testAddsCorrectKmsParametersWithKeyId() {
    KMSEncrypter kmsEncrypter = new KMSEncrypter(Optional.of("FakeKeyId"));

    when(createMultipartUploadRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(createMultipartUploadRequestBuilder);
    when(createMultipartUploadRequestBuilder.ssekmsKeyId(anyString())).thenReturn(createMultipartUploadRequestBuilder);
    kmsEncrypter.addEncryption(createMultipartUploadRequestBuilder);
    verify(createMultipartUploadRequestBuilder).serverSideEncryption(ServerSideEncryption.AWS_KMS);
    verify(createMultipartUploadRequestBuilder).ssekmsKeyId("FakeKeyId");

    when(putObjectRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(putObjectRequestBuilder);
    when(putObjectRequestBuilder.ssekmsKeyId(anyString())).thenReturn(putObjectRequestBuilder);
    kmsEncrypter.addEncryption(putObjectRequestBuilder);
    verify(putObjectRequestBuilder).serverSideEncryption(ServerSideEncryption.AWS_KMS);
    verify(putObjectRequestBuilder).ssekmsKeyId("FakeKeyId");

    when(copyObjectRequestBuilder.serverSideEncryption(any(ServerSideEncryption.class)))
        .thenReturn(copyObjectRequestBuilder);
    when(copyObjectRequestBuilder.ssekmsKeyId(anyString())).thenReturn(copyObjectRequestBuilder);
    kmsEncrypter.addEncryption(copyObjectRequestBuilder);
    verify(copyObjectRequestBuilder).serverSideEncryption(ServerSideEncryption.AWS_KMS);
    verify(copyObjectRequestBuilder).ssekmsKeyId("FakeKeyId");
  }
}
