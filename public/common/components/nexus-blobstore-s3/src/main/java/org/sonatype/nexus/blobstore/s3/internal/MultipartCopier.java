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

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.blobstore.api.BlobStoreException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.min;

/**
 * Copies a file, using multipart copy if the file is larger or equal to the chunk size. A normal copyObject request is
 * used instead if only a single chunk would be copied.
 *
 * @since 3.15
 */
@ConditionalOnProperty(name = "nexus.s3.copierName", havingValue = "multipart-copier")
@Component
@Qualifier("multipart-copier")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MultipartCopier
    implements S3Copier
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final int chunkSize;

  @Autowired
  public MultipartCopier(
      @Value("${nexus.s3.multipartupload.chunksize:5242880}") final int chunkSize)
  {
    this.chunkSize = chunkSize;
  }

  @Override
  public void copy(
      final EncryptingS3Client s3,
      final String bucket,
      final String sourcePath,
      final String destinationPath)
  {
    final HeadObjectResponse metadataResult = s3.getObjectMetadata(bucket, sourcePath);
    long length = metadataResult.contentLength();

    try {
      if (length < chunkSize) {
        copySinglePart(s3, bucket, sourcePath, destinationPath);
      }
      else {
        copyMultiPart(s3, bucket, sourcePath, destinationPath, length);
      }
    }
    catch (SdkClientException e) {
      throw new BlobStoreException("Error copying blob", e, null);
    }
  }

  private void copySinglePart(
      final EncryptingS3Client s3,
      final String bucket,
      final String sourcePath,
      final String destinationPath)
  {
    s3.copyObject(bucket, sourcePath, bucket, destinationPath);
  }

  private void copyMultiPart(
      final EncryptingS3Client s3,
      final String bucket,
      final String sourcePath,
      final String destinationPath,
      final long length)
  {
    checkState(length > 0);
    String uploadId = null;
    try {
      long remaining = length;
      long offset = 0;

      CreateMultipartUploadRequest initiateRequest = CreateMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(destinationPath)
          .build();
      uploadId = s3.createMultipartUpload(initiateRequest).uploadId();

      log.debug("Starting multipart copy {} to key {} from key {}", uploadId, destinationPath, sourcePath);

      final List<CompletedPart> completedParts = new ArrayList<>();
      for (int partNumber = 1;; partNumber++) {
        if (remaining <= 0) {
          break;
        }
        else {
          long partSize = min(remaining, chunkSize);
          log.trace("Copying chunk {} for {} from byte {} to {}, size {}", partNumber, uploadId, offset,
              offset + partSize - 1, partSize);
          UploadPartCopyRequest part = UploadPartCopyRequest.builder()
              .sourceBucket(bucket)
              .sourceKey(sourcePath)
              .destinationBucket(bucket)
              .destinationKey(destinationPath)
              .uploadId(uploadId)
              .partNumber(partNumber)
              .copySourceRange("bytes=" + offset + "-" + (offset + partSize - 1))
              .build();

          final UploadPartCopyResponse response = s3.uploadPartCopy(part);
          completedParts.add(CompletedPart.builder()
              .partNumber(partNumber)
              .eTag(response.copyPartResult().eTag())
              .build());
          offset += partSize;
          remaining -= partSize;
        }
      }
      CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
          .parts(completedParts)
          .build();
      CompleteMultipartUploadRequest compRequest = CompleteMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(destinationPath)
          .uploadId(uploadId)
          .multipartUpload(completedUpload)
          .build();
      s3.completeMultipartUpload(compRequest);
      log.debug("Copy {} complete", uploadId);
    }
    catch (SdkClientException e) {
      if (uploadId != null) {
        try {
          s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
              .bucket(bucket)
              .key(destinationPath)
              .uploadId(uploadId)
              .build());
        }
        catch (Exception inner) {
          log.error("Error aborting S3 multipart copy to bucket {} with key {} {}", bucket, destinationPath,
              log.isDebugEnabled() ? "Inner Exception: " + inner : null);
        }
      }
      throw e;
    }
  }
}
