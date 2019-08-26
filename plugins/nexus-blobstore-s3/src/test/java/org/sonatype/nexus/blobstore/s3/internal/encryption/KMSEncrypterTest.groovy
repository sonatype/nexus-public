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
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams
import spock.lang.Specification
import spock.lang.Unroll

class KMSEncrypterTest
    extends Specification
{

  @Unroll
  def 'Constructor properly handles kmsId:\'#kmsId\''() {
    when: 'you create a kms encrypter'
      new KMSEncrypter(Optional.ofNullable(kmsId))

    then: 'aws sdk is setup properly'
      noExceptionThrown()

    where:
        kmsId << [
          null,
          "",
          " ",
          "   ",
          "aProperKeyId"
      ]
  }

  @Unroll
  def 'Supplying no kms id adds the correct kms parameters to \'#request\''() {
    given: 'an encrypter without a selected key'
      def request = Mock(requestType)
      KMSEncrypter kmsEncrypter = new KMSEncrypter()

    when: 'you encrypt a request'
      kmsEncrypter.addEncryption(request)

    then: 'Correct params are set'
      1 * request.setSSEAwsKeyManagementParams(*_) >> { args ->
        final SSEAwsKeyManagementParams params = args[0]
        assert params.getAwsKmsKeyId() == null
      }

    where:
      requestType << [
          InitiateMultipartUploadRequest,
          AbstractPutObjectRequest,
          CopyObjectRequest
      ]
  }

  @Unroll
  def 'Adds the correct kms parameters with key ID to \'#request\''() {
    given: 'an encrypter with a kms key ID'
      def request = Mock(requestType)
      KMSEncrypter kmsEncrypter = new KMSEncrypter(Optional.of('FakeKeyId'))

    when: 'you encrypt a request'
      kmsEncrypter.addEncryption(request)

    then: 'Correct params are set'
      1* request.setSSEAwsKeyManagementParams(*_) >> { args ->
        final SSEAwsKeyManagementParams params = args[0]
        assert params.getAwsKmsKeyId().equals('FakeKeyId')
      }

    where:
      requestType << [
          InitiateMultipartUploadRequest,
          AbstractPutObjectRequest,
          CopyObjectRequest
      ]
  }
}
