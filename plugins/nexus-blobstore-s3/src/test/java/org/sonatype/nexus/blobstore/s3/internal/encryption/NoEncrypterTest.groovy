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
import spock.lang.Specification
import spock.lang.Unroll

class NoEncrypterTest
    extends Specification
{

  NoEncrypter noEncrypter = new NoEncrypter()

  @Unroll
  def 'NoEncrypter does nothing to \'#request\''() {
    given: 'a request to s3'
      def request = Mock(requestType)

    when: 'you try to encrypt a request'
      noEncrypter.addEncryption(request)

    then: 'nothing happens'
      0 * request./.*/

    where:
      requestType << [
          InitiateMultipartUploadRequest,
          AbstractPutObjectRequest,
          CopyObjectRequest
      ]
  }
}
