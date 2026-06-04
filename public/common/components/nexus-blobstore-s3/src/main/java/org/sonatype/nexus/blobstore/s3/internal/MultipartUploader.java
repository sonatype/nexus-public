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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.blobstore.api.BlobStoreException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

/**
 * Uploads a file, using multipart upload if the file is larger or equal to the chunk size. A normal putObject request
 * is used instead if only a single chunk would be sent.
 *
 * @since 3.12
 */
@ConditionalOnProperty(name = "nexus.s3.uploaderName", havingValue = "multipart-uploader")
@Component
@Qualifier("multipart-uploader")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MultipartUploader
    implements S3Uploader
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final int chunkSize;

  @Autowired
  public MultipartUploader(
      @Value("${nexus.s3.multipartupload.chunksize:5242880}") final int chunkSize)
  {
    this.chunkSize = chunkSize;
  }

  @Override
  public void upload(final EncryptingS3Client s3, final String bucket, final String key, final InputStream contents) {
    try (InputStream input = contents) {
      InputStream chunkOne = readChunk(input);
      if (chunkOne.available() < chunkSize) {
        uploadSinglePart(s3, bucket, key, chunkOne);
      }
      else {
        uploadMultiPart(s3, bucket, key, chunkOne, contents);
      }
    }
    catch (IOException | SdkClientException e) { // NOSONAR
      throw new BlobStoreException("Error uploading blob", e, null);
    }
  }

  private void uploadSinglePart(
      final EncryptingS3Client s3,
      final String bucket,
      final String key,
      final InputStream contents) throws IOException
  {
    log.debug("Starting upload to key {} in bucket {} of {} bytes", key, bucket, contents.available());
    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentLength((long) contents.available())
        .build();
    s3.putObject(putRequest, RequestBody.fromInputStream(contents, contents.available()));
  }

  private void uploadMultiPart(
      final EncryptingS3Client s3,
      final String bucket,
      final String key,
      final InputStream firstChunk,
      final InputStream restOfContents) throws IOException
  {
    checkState(firstChunk.available() > 0);
    String uploadId = null;
    try {
      CreateMultipartUploadRequest initiateRequest = CreateMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();
      uploadId = s3.createMultipartUpload(initiateRequest).uploadId();

      log.debug("Starting multipart upload {} to key {} in bucket {}", uploadId, key, bucket);

      final List<CompletedPart> completedParts = new ArrayList<>();
      for (int partNumber = 1;; partNumber++) {
        InputStream chunk = partNumber == 1 ? firstChunk : readChunk(restOfContents);
        if (chunk.available() == 0) {
          break;
        }
        else {
          log.debug("Uploading chunk {} for {} of {} bytes", partNumber, uploadId, chunk.available());
          UploadPartRequest part = UploadPartRequest.builder()
              .bucket(bucket)
              .key(key)
              .uploadId(uploadId)
              .partNumber(partNumber)
              .contentLength((long) chunk.available())
              .build();
          final UploadPartResponse response =
              s3.uploadPart(part, RequestBody.fromInputStream(chunk, chunk.available()));
          completedParts.add(CompletedPart.builder()
              .partNumber(partNumber)
              .eTag(response.eTag())
              .build());
        }
      }
      CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
          .parts(completedParts)
          .build();
      CompleteMultipartUploadRequest compRequest = CompleteMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(key)
          .uploadId(uploadId)
          .multipartUpload(completedUpload)
          .build();
      s3.completeMultipartUpload(compRequest);
      log.debug("Upload {} complete", uploadId);
      uploadId = null;
    }
    finally {
      if (uploadId != null) {
        try {
          s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
              .bucket(bucket)
              .key(key)
              .uploadId(uploadId)
              .build());
        }
        catch (Exception e) {
          log.error("Error aborting S3 multipart upload to bucket {} with key {} {}", bucket, key,
              log.isDebugEnabled() ? "Inner Exception: " + e : null);
        }
      }
    }
  }

  @VisibleForTesting
  InputStream readChunk(final InputStream input) throws IOException {
    byte[] buffer = new byte[chunkSize];
    int offset = 0;
    int remain = chunkSize;
    int bytesRead = 0;

    while (remain > 0 && bytesRead >= 0) {
      bytesRead = input.read(buffer, offset, remain);
      if (bytesRead > 0) {
        offset += bytesRead;
        remain -= bytesRead;
      }
    }
    if (offset > 0) {
      return new ByteArrayInputStream(buffer, 0, offset);
    }
    else {
      return new ByteArrayInputStream(new byte[0]);
    }
  }
}
