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
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import spock.lang.Specification
/**
 * {@link ProducerConsumerUploader} tests.
 */
class ProducerConsumerUploaderTest
    extends Specification
{
  def 'an empty stream still causes an upload'() {
    given: 'A mock metrics registry'
      Timer.Context context = Mock()
      Timer timer = Mock()
      timer.time() >> context
      MetricRegistry registry = Mock()
      registry.timer(_) >> timer
    and: 'A producer/consumer uploader'
      ProducerConsumerUploader producerConsumerUploader = new ProducerConsumerUploader(100, 4, registry)
      producerConsumerUploader.start()
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[0])
      producerConsumerUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the putObject method is called'
      1 * s3.putObject(_, _, _, _)
      0 * s3.initiateMultipartUpload(_)
  }

  def 'upload uploads with the multipart api'() {
    given: 'A mock metrics registry'
      Timer.Context context = Mock()
      Timer timer = Mock()
      timer.time() >> context
      MetricRegistry registry = Mock()
      registry.timer(_) >> timer
    and: 'A producer/consumer uploader'
      ProducerConsumerUploader producerConsumerUploader = new ProducerConsumerUploader(100, 4, registry)
      producerConsumerUploader.start()
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[100])
      producerConsumerUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the multipart api is called'
      1 * s3.initiateMultipartUpload(_) >> new InitiateMultipartUploadResult(uploadId: 'testupload')
      1 * s3.uploadPart(_) >> new UploadPartResult()
      1 * s3.completeMultipartUpload(_)
      0 * s3.abortMultipartUpload(_)
  }

  def 'larger upload with the multipart api emit metrics'() {
    given: 'A mock metrics registry'
      MetricRegistry registry = Mock()
      Timer.Context context = Mock()

      Timer readChunk = Mock()
      readChunk.time() >> context
      registry.timer('org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.uploader.readChunk') >> readChunk

      Timer uploadChunk = Mock()
      uploadChunk.time() >> context
      registry.timer('org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.uploader.uploadChunk') >> uploadChunk

      Timer multiPartUpload = Mock()
      multiPartUpload.time() >> context
      registry.timer('org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.uploader.multiPartUpload') >> multiPartUpload

    and: 'A producer/consumer uploader'
      ProducerConsumerUploader producerConsumerUploader = new ProducerConsumerUploader(100, 4, registry)
      producerConsumerUploader.start()
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[350])
      producerConsumerUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the multipart api is called'
      1 * s3.initiateMultipartUpload(_) >> new InitiateMultipartUploadResult(uploadId: 'testupload')
      4 * s3.uploadPart(_) >> new UploadPartResult()
      1 * s3.completeMultipartUpload(_)
      0 * s3.abortMultipartUpload(_)
      1 * multiPartUpload.time()
      6 * readChunk.time() //once for chunk to determine if multipart, 4 for the chunks, and once for the empty/end chunk
      4 * uploadChunk.time()
  }

  def 'upload aborts multipart uploads on error'() {
    given: 'A mock metrics registry'
      Timer.Context context = Mock()
      Timer timer = Mock()
      timer.time() >> context
      MetricRegistry registry = Mock()
      registry.timer(_) >> timer
    and: 'A producer/consumer uploader'
      ProducerConsumerUploader producerConsumerUploader = new ProducerConsumerUploader(100, 4, registry)
      producerConsumerUploader.start()
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[100])
      producerConsumerUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the upload is aborted'
      thrown(BlobStoreException)
      1 * s3.initiateMultipartUpload(_) >> new InitiateMultipartUploadResult(uploadId: 'testupload')
      1 * s3.uploadPart(_) >> { UploadPartRequest request -> throw new SdkClientException('') }
      1 * s3.abortMultipartUpload(_)
  }

  def 'upload uses putObject for small uploads'() {
    given: 'A mock metrics registry'
      Timer.Context context = Mock()
      Timer timer = Mock()
      timer.time() >> context
      MetricRegistry registry = Mock()
      registry.timer(_) >> timer
    and: 'A producer/consumer uploader'
      ProducerConsumerUploader producerConsumerUploader = new ProducerConsumerUploader(100, 4, registry)
      producerConsumerUploader.start()
      AmazonS3 s3 = Mock()

    when: 'an upload is started'
      def input = new ByteArrayInputStream(new byte[50])
      producerConsumerUploader.upload(s3, 'bucketName', 'key', input)

    then: 'the putObject method is called'
      1 * s3.putObject(_, _, _, _)
      0 * s3.initiateMultipartUpload(_)
  }
}
