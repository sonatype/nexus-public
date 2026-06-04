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
package org.sonatype.nexus.blobstore.s3.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.blobstore.api.BlobStoreException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import com.codahale.metrics.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static java.lang.Math.min;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Copies a file, using multipart copy in parallel if the file is larger or equal to the chunk size. A normal
 * copyObject request is
 * used instead if only a single chunk would be copied.
 *
 * @since 3.19
 */
@ConditionalOnProperty(name = "nexus.s3.copierName", havingValue = "parallelCopier", matchIfMissing = true)
@Component
@Qualifier("parallelCopier")
public class ParallelCopier
    extends ParallelRequester
    implements S3Copier
{
  @Autowired
  public ParallelCopier(
      @Value("${nexus.s3.parallelRequests.chunksize:5242880}") final int chunkSize,
      @Value("${nexus.s3.parallelRequests.parallelism:0}") final int nThreads)
  {
    super(chunkSize, nThreads, "copyThreads");
  }

  @Override
  @Timed
  public void copy(final EncryptingS3Client s3, final String bucket, final String srcKey, final String destKey) {
    long length = s3.getObjectMetadata(bucket, srcKey).contentLength();

    try {
      if (length < chunkSize) {
        s3.copyObject(bucket, srcKey, bucket, destKey);
      }
      else {
        final AtomicInteger offset = new AtomicInteger(1);
        parallelRequests(s3, bucket, destKey,
            () -> (uploadId -> copyParts(s3, uploadId, bucket, srcKey, destKey, length, offset)));
      }
    }
    catch (SdkClientException e) {
      throw new BlobStoreException("Error copying blob", e, null);
    }
  }

  private List<CompletedPart> copyParts(
      final EncryptingS3Client s3,
      final String uploadId,
      final String bucket,
      final String srcKey,
      final String destKey,
      final long size,
      final AtomicInteger offset)
  {
    List<CompletedPart> completedParts = new ArrayList<>();
    int partNumber;

    while (getFirstByte((partNumber = offset.getAndIncrement()), chunkSize) < size) {
      final long firstByte = getFirstByte(partNumber, chunkSize);
      final long lastByte = getLastByte(size, partNumber, chunkSize);
      UploadPartCopyRequest request = UploadPartCopyRequest.builder()
          .sourceBucket(bucket)
          .sourceKey(srcKey)
          .destinationBucket(bucket)
          .destinationKey(destKey)
          .uploadId(uploadId)
          .partNumber(partNumber)
          .copySourceRange("bytes=" + firstByte + "-" + lastByte)
          .build();

      UploadPartCopyResponse response = s3.uploadPartCopy(request);
      completedParts.add(CompletedPart.builder()
          .partNumber(partNumber)
          .eTag(response.copyPartResult().eTag())
          .build());
    }

    return completedParts;
  }

  static long getFirstByte(final long partNumber, final long chunkSize) {
    return (partNumber - 1) * chunkSize;
  }

  static long getLastByte(final long size, final long partNumber, final long chunkSize) {
    return min(partNumber * chunkSize, size) - 1;
  }
}
