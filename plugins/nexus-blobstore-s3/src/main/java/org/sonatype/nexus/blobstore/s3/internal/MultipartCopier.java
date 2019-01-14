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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreException;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static com.google.common.base.Preconditions.checkState;

/**
 * Copies a file, using multipart copy if the file is larger or equal to the chunk size.  A normal copyObject request is
 * used instead if only a single chunk would be copied.
 *
 * @since 3.15
 */
@Named("multipart-copier")
public class MultipartCopier
    extends ComponentSupport
    implements S3Copier
{

  private final int chunkSize;

  @Inject
  public MultipartCopier(@Named("${nexus.s3.multipartupload.chunksize:-5242880}") final int chunkSize) {
    this.chunkSize = chunkSize;
  }

  @Override
  public void copy(final AmazonS3 s3, final String bucket, final String sourcePath, final String destinationPath) {
    ObjectMetadata metadataResult = s3.getObjectMetadata(bucket, sourcePath);
    long length = metadataResult.getContentLength();

    try {
      if (length < chunkSize) {
        copySinglePart(s3, bucket, sourcePath, destinationPath);
      }
      else {
        copyMultiPart(s3, bucket, sourcePath, destinationPath, length);
      }
    }
    catch(SdkClientException e) {
      throw new BlobStoreException("Error copying blob", e, null);
    }
  }

  private void copySinglePart(final AmazonS3 s3,
                              final String bucket,
                              final String sourcePath,
                              final String destinationPath) {
    s3.copyObject(bucket, sourcePath, bucket, destinationPath);
  }

  private void copyMultiPart(final AmazonS3 s3,
                             final String bucket,
                             final String sourcePath,
                             final String destinationPath,
                             final long length) {
    checkState(length > 0);
    String uploadId = null;
    try {
      long remaining = length;
      long offset = 0;

      InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucket, destinationPath);
      uploadId = s3.initiateMultipartUpload(initiateRequest).getUploadId();

      log.debug("Starting multipart copy {} to key {} from key {}", uploadId, destinationPath, sourcePath);

      List<CopyPartResult> results = new ArrayList<>();
      for (int partNumber = 1; ; partNumber++) {
        if (remaining <= 0) {
          break;
        }
        else {
          long partSize = min(remaining, chunkSize);
          log.trace("Copying chunk {} for {} from byte {} to {}, size {}", partNumber, uploadId, offset,
              offset + partSize - 1, partSize);
          CopyPartRequest part = new CopyPartRequest()
              .withSourceBucketName(bucket)
              .withSourceKey(sourcePath)
              .withDestinationBucketName(bucket)
              .withDestinationKey(destinationPath)
              .withUploadId(uploadId)
              .withPartNumber(partNumber)
              .withFirstByte(offset)
              .withLastByte(offset + partSize - 1);
          results.add(s3.copyPart(part));
          offset += partSize;
          remaining -= partSize;
        }
      }
      CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest()
          .withBucketName(bucket)
          .withKey(destinationPath)
          .withUploadId(uploadId)
          .withPartETags(results.stream().map(r -> new PartETag(r.getPartNumber(), r.getETag())).collect(toList()));
      s3.completeMultipartUpload(compRequest);
      log.debug("Copy {} complete", uploadId);
    }
    catch(SdkClientException e) {
      if (uploadId != null) {
        try {
          s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, destinationPath, uploadId));
        }
        catch(Exception inner) {
          log.error("Error aborting S3 multipart copy to bucket {} with key {}", bucket, destinationPath,
              log.isDebugEnabled() ? inner : null);
        }
      }
      throw e;
    }
  }
}
