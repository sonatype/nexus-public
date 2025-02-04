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

import org.sonatype.goodies.testsupport.TestSupport;

import com.amazonaws.services.s3.model.AbstractPutObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;

public class KMSEncrypterTest
    extends TestSupport
{

  @Mock
  private InitiateMultipartUploadRequest initiateMultipartUploadRequest;

  @Mock
  private AbstractPutObjectRequest abstractPutObjectRequest;

  @Mock
  private CopyObjectRequest copyObjectRequest;

  @Captor
  private ArgumentCaptor<SSEAwsKeyManagementParams> sseAwsKeyManagementParamsCaptor;

  @Test
  public void testConstructorHandlesKmsId() {
    assertThat(new KMSEncrypter(Optional.empty()).getKmsParameters().getAwsKmsKeyId(), nullValue());
    assertThat(new KMSEncrypter(Optional.of("")).getKmsParameters().getAwsKmsKeyId(), nullValue());
    assertThat(new KMSEncrypter(Optional.of(" ")).getKmsParameters().getAwsKmsKeyId(), nullValue());
    assertThat(new KMSEncrypter(Optional.of("   ")).getKmsParameters().getAwsKmsKeyId(), nullValue());
    assertThat(new KMSEncrypter(Optional.of("aProperKeyId")).getKmsParameters().getAwsKmsKeyId(), is("aProperKeyId"));
  }

  @Test
  public void testSupplyingNoKmsIdAddsCorrectKmsParameters() {
    KMSEncrypter kmsEncrypter = new KMSEncrypter();

    kmsEncrypter.addEncryption(initiateMultipartUploadRequest);
    verify(initiateMultipartUploadRequest).setSSEAwsKeyManagementParams(sseAwsKeyManagementParamsCaptor.capture());
    assertThat(sseAwsKeyManagementParamsCaptor.getValue().getAwsKmsKeyId(), nullValue());

    kmsEncrypter.addEncryption(abstractPutObjectRequest);
    verify(abstractPutObjectRequest).setSSEAwsKeyManagementParams(sseAwsKeyManagementParamsCaptor.capture());
    assertThat(sseAwsKeyManagementParamsCaptor.getValue().getAwsKmsKeyId(), nullValue());

    kmsEncrypter.addEncryption(copyObjectRequest);
    verify(copyObjectRequest).setSSEAwsKeyManagementParams(sseAwsKeyManagementParamsCaptor.capture());
    assertThat(sseAwsKeyManagementParamsCaptor.getValue().getAwsKmsKeyId(), nullValue());
  }

  @Test
  public void testAddsCorrectKmsParametersWithKeyId() {
    KMSEncrypter kmsEncrypter = new KMSEncrypter(Optional.of("FakeKeyId"));

    kmsEncrypter.addEncryption(initiateMultipartUploadRequest);
    verify(initiateMultipartUploadRequest).setSSEAwsKeyManagementParams(sseAwsKeyManagementParamsCaptor.capture());
    assertThat(sseAwsKeyManagementParamsCaptor.getValue().getAwsKmsKeyId(), is("FakeKeyId"));

    kmsEncrypter.addEncryption(abstractPutObjectRequest);
    verify(abstractPutObjectRequest).setSSEAwsKeyManagementParams(sseAwsKeyManagementParamsCaptor.capture());
    assertThat(sseAwsKeyManagementParamsCaptor.getValue().getAwsKmsKeyId(), is("FakeKeyId"));

    kmsEncrypter.addEncryption(copyObjectRequest);
    verify(copyObjectRequest).setSSEAwsKeyManagementParams(sseAwsKeyManagementParamsCaptor.capture());
    assertThat(sseAwsKeyManagementParamsCaptor.getValue().getAwsKmsKeyId(), is("FakeKeyId"));
  }
}
