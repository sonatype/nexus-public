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
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NoEncrypterTest
{

  private final NoEncrypter noEncrypter = new NoEncrypter();

  @Mock
  private CreateMultipartUploadRequest.Builder createMultipartUploadRequestBuilder;

  @Mock
  private PutObjectRequest.Builder putObjectRequestBuilder;

  @Mock
  private CopyObjectRequest.Builder copyObjectRequestBuilder;

  @Test
  public void testNoEncrypterDoesNothingToInitiateMultipartUploadRequest() {
    noEncrypter.addEncryption(createMultipartUploadRequestBuilder);
    verifyNoInteractions(createMultipartUploadRequestBuilder);
  }

  @Test
  public void testNoEncrypterDoesNothingToAbstractPutObjectRequest() {
    noEncrypter.addEncryption(putObjectRequestBuilder);
    verifyNoInteractions(putObjectRequestBuilder);
  }

  @Test
  public void testNoEncrypterDoesNothingToCopyObjectRequest() {
    noEncrypter.addEncryption(copyObjectRequestBuilder);
    verifyNoInteractions(copyObjectRequestBuilder);
  }
}
