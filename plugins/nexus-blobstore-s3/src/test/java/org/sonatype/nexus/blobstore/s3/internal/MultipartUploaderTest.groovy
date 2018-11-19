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

import spock.lang.Specification

import org.sonatype.nexus.blobstore.api.BlobStoreException

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult
import com.amazonaws.services.s3.model.UploadPartRequest
import com.amazonaws.services.s3.model.UploadPartResult

/**
 * {@link MultipartUploader} tests.
 */
class MultipartUploaderTest
    extends Specification
{

  def 'upload uploads with the multipart api'() {
    given: 'A multipart uploader'
      MultipartUploader multipartUploader = new MultipartUploader(100)
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[100])
      multipartUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the multipart api is called'
      1 * s3.initiateMultipartUpload(_) >> new InitiateMultipartUploadResult(uploadId: 'testupload')
      1 * s3.uploadPart(_) >> new UploadPartResult()
      1 * s3.completeMultipartUpload(_)
      0 * s3.abortMultipartUpload(_)
  }

  def 'upload aborts multipart uploads on error'() {
    given: 'A multipart uploader'
      MultipartUploader multipartUploader = new MultipartUploader(100)
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[100])
      multipartUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the upload is aborted'
      thrown(BlobStoreException)
      1 * s3.initiateMultipartUpload(_) >> new InitiateMultipartUploadResult(uploadId: 'testupload')
      1 * s3.uploadPart(_) >> { UploadPartRequest request -> throw new SdkClientException('') }
      1 * s3.abortMultipartUpload(_)
  }

  def 'readChunk reads streams in chunks'() {
    given: 'A multipart uploader with a custom chunk size'
      MultipartUploader multipartUploader = new MultipartUploader(100)

    when: 'an input stream of a given size is read'
      def input = new ByteArrayInputStream(new byte[inputSize])
      def chunks = []
      while (true) {
        chunks << multipartUploader.readChunk(input)
        if (chunks[-1].available() == 0) {
          break
        }
      }
      def chunkSizes = chunks.collect { it?.available() }

    then: 'The resulting chunk sizes are correct'
      chunkSizes == expectedChunkSizes

    where:
      inputSize || expectedChunkSizes
      0         || [0]
      99        || [99, 0]
      100       || [100, 0]
      101       || [100, 1, 0]
      500       || [100, 100, 100, 100, 100, 0]
  }

  def 'upload uses putObject for small uploads'() {
    given: 'A multipart uploader'
      MultipartUploader multipartUploader = new MultipartUploader(100)
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[50])
      multipartUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the putObject method is called'
      1 * s3.putObject(_, _, _, _)
      0 * s3.initiateMultipartUpload(_)
  }
}
