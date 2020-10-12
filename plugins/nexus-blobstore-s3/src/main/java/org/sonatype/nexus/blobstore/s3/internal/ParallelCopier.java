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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreException;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.codahale.metrics.annotation.Timed;

import static java.lang.Math.min;

/**
 * Copies a file, using multipart copy in parallel if the file is larger or equal to the chunk size.  A normal
 * copyObject request is
 * used instead if only a single chunk would be copied.
 *
 * @since 3.19
 */
@Singleton
@Named("parallelCopier")
public class ParallelCopier
    extends ParallelRequester
    implements S3Copier
{
  @Inject
  public ParallelCopier(@Named("${nexus.s3.parallelRequests.chunksize:-5242880}") final int chunkSize,
                        @Named("${nexus.s3.parallelRequests.parallelism:-0}") final int nThreads)
  {
    super(chunkSize, nThreads, "copyThreads");
  }

  @Override
  @Timed
  public void copy(final AmazonS3 s3, final String bucket, final String srcKey, final String destKey) {
    long length = s3.getObjectMetadata(bucket, srcKey).getContentLength();

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

  private List<PartETag> copyParts(final AmazonS3 s3,
                                   final String uploadId,
                                   final String bucket,
                                   final String srcKey,
                                   final String destKey,
                                   final long size,
                                   final AtomicInteger offset)
  {
    List<PartETag> tags = new ArrayList<>();
    int partNumber;

    while (getFirstByte((partNumber = offset.getAndIncrement()), chunkSize) < size) {
      CopyPartRequest request = new CopyPartRequest()
          .withSourceBucketName(bucket)
          .withSourceKey(srcKey)
          .withDestinationBucketName(bucket)
          .withDestinationKey(destKey)
          .withUploadId(uploadId)
          .withPartNumber(partNumber)
          .withFirstByte(getFirstByte(partNumber, chunkSize))
          .withLastByte(getLastByte(size, partNumber, chunkSize));

      tags.add(s3.copyPart(request).getPartETag());
    }

    return tags;
  }

  static long getFirstByte(final long partNumber, final long chunkSize) {
    return (partNumber - 1) * chunkSize;
  }

  static long getLastByte(final long size, final long partNumber, final long chunkSize) {
    return min(partNumber * chunkSize, size) - 1;
  }
}
