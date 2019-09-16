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
package org.sonatype.nexus.blobstore.s3.internal

import org.sonatype.nexus.blobstore.api.BlobStoreException

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult
import com.amazonaws.services.s3.model.UploadPartRequest
import com.amazonaws.services.s3.model.UploadPartResult
import spock.lang.Specification
/**
 * {@link ParallelUploader} tests.
 */
class ParallelUploaderTest
    extends Specification
{

  def 'an empty stream still causes an upload'() {
    given: 'A parallel uploader'
      ParallelUploader parallelUploader = new ParallelUploader(100, 4)
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[0])
      parallelUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the putObject method is called'
      1 * s3.putObject(_, _, _, _)
      0 * s3.initiateMultipartUpload(_)
  }

  def 'upload uploads with the multipart api'() {
    given: 'A parallel uploader'
      ParallelUploader parallelUploader = new ParallelUploader(100, 4)
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[100])
      parallelUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the multipart api is called'
      1 * s3.initiateMultipartUpload(_) >> new InitiateMultipartUploadResult(uploadId: 'testupload')
      1 * s3.uploadPart(_) >> new UploadPartResult()
      1 * s3.completeMultipartUpload(_)
      0 * s3.abortMultipartUpload(_)
  }

  def 'upload aborts multipart uploads on error'() {
    given: 'A parallel uploader'
      ParallelUploader parallelUploader = new ParallelUploader(100, 4)
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[100])
      parallelUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the upload is aborted'
      thrown(BlobStoreException)
      1 * s3.initiateMultipartUpload(_) >> new InitiateMultipartUploadResult(uploadId: 'testupload')
      1 * s3.uploadPart(_) >> { UploadPartRequest request -> throw new SdkClientException('') }
      1 * s3.abortMultipartUpload(_)
  }

  def 'upload uses putObject for small uploads'() {
    given: 'A parallel uploader'
      ParallelUploader parallelUploader = new ParallelUploader(100, 4)
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[50])
      parallelUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the putObject method is called'
      1 * s3.putObject(_, _, _, _)
      0 * s3.initiateMultipartUpload(_)
  }
}
