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

import org.sonatype.goodies.testsupport.TestSupport;

import com.amazonaws.services.s3.model.AbstractPutObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class S3ManagedEncrypterTest
    extends TestSupport
{

  @Mock
  private InitiateMultipartUploadRequest initiateMultipartUploadRequest;

  @Mock
  private AbstractPutObjectRequest abstractPutObjectRequest;

  @Mock
  private CopyObjectRequest copyObjectRequest;

  @Mock
  private ObjectMetadata objectMetadata;

  private final S3ManagedEncrypter encrypter = new S3ManagedEncrypter();

  @Test
  public void testS3ManagedServerSideEncWorksForInitiateMultipartUploadRequest() {
    when(initiateMultipartUploadRequest.getObjectMetadata()).thenReturn(objectMetadata);

    encrypter.addEncryption(initiateMultipartUploadRequest);

    verify(initiateMultipartUploadRequest).setObjectMetadata(objectMetadata);
    verify(objectMetadata).setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
  }

  @Test
  public void testS3ManagedServerSideEncWorksForAbstractPutObjectRequest() {
    when(abstractPutObjectRequest.getMetadata()).thenReturn(objectMetadata);

    encrypter.addEncryption(abstractPutObjectRequest);

    verify(abstractPutObjectRequest).setMetadata(objectMetadata);
    verify(objectMetadata).setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
  }

  @Test
  public void testS3ManagedServerSideEncWorksForCopyObjectRequest() {
    when(copyObjectRequest.getNewObjectMetadata()).thenReturn(objectMetadata);

    encrypter.addEncryption(copyObjectRequest);

    verify(copyObjectRequest).setNewObjectMetadata(objectMetadata);
    verify(objectMetadata).setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
  }
}
