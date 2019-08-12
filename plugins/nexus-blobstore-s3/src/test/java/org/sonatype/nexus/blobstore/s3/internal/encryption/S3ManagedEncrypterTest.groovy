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
package org.sonatype.nexus.blobstore.s3.internal.encryption

import com.amazonaws.services.s3.model.AbstractPutObjectRequest
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import spock.lang.Specification

class S3ManagedEncrypterTest
    extends Specification
{

  S3ManagedEncrypter encrypter = new S3ManagedEncrypter()

  def 'S3 Managed Server Side Enc works for InitiateMultipartUploadRequest'() {
    given: 'a multipart upload request'
      def request = Mock(InitiateMultipartUploadRequest)
      ObjectMetadata objectMetadata = Mock(ObjectMetadata)
      request.getObjectMetadata() >> objectMetadata

    when: 'you encrypt a request'
      encrypter.addEncryption(request)

    then: 'Correct params are set'
      1 * request.setObjectMetadata(objectMetadata)
      1 * objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION)
  }

  def 'S3 Managed Server Side Enc works for AbstractPutObjectRequest'() {
    given: 'a put request'
      def request = Mock(AbstractPutObjectRequest)
      ObjectMetadata objectMetadata = Mock(ObjectMetadata)
      request.getMetadata() >> objectMetadata

    when: 'you encrypt a request'
      encrypter.addEncryption(request)

    then: 'Correct params are set'
      1 * request.setMetadata(objectMetadata)
      1 * objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION)
  }

  def 'S3 Managed Server Side Enc works for CopyObjectRequest'() {
    given: 'a copy request'
      def request = Mock(CopyObjectRequest)
      ObjectMetadata objectMetadata = Mock(ObjectMetadata)
      request.getNewObjectMetadata() >> objectMetadata

    when: 'you encrypt a request'
      encrypter.addEncryption(request)

    then: 'Correct params are set'
      1 * request.setNewObjectMetadata(objectMetadata)
      1 * objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION)
  }
}
