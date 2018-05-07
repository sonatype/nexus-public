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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

/**
 * Uploads a file as a multipart upload.
 * @since 3.next
 */
@Named("multipart-uploader")
public class MultipartUploader
    extends ComponentSupport
    implements S3Uploader
{

  private final int chunkSize;

  @Inject
  public MultipartUploader(@Named("${nexus.s3.multipartupload.chunksize:-5242880}") final int chunkSize) {
    this.chunkSize = chunkSize;
  }

  @Override
  public void upload(final AmazonS3 s3, final String bucket, final String key, final InputStream contents) {

    InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucket, key);
    String uploadId = s3.initiateMultipartUpload(initiateRequest).getUploadId();

    log.debug("Starting upload {} to key {} in bucket {}", uploadId, key, bucket);

    try (InputStream input = contents) {
      List<UploadPartResult> results = new ArrayList<>();
      for (int partNumber = 1; ; partNumber++) {
        InputStream chunk = readChunk(input);
        if (chunk == null && partNumber > 1) {
          break;
        }
        else {
          // must provide a zero sized chunk if contents is empty
          if (chunk == null) {
            chunk = new ByteArrayInputStream(new byte[0]);
          }
          log.debug("Uploading chunk {} for {} of {} bytes", partNumber, uploadId, chunk.available());
          UploadPartRequest part = new UploadPartRequest()
              .withBucketName(bucket)
              .withKey(key)
              .withUploadId(uploadId)
              .withPartNumber(partNumber)
              .withInputStream(chunk)
              .withPartSize(chunk.available());
          results.add(s3.uploadPart(part));
        }
      }
      CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest()
          .withBucketName(bucket)
          .withKey(key)
          .withUploadId(uploadId)
          .withPartETags(results);
      s3.completeMultipartUpload(compRequest);
      log.debug("Upload {} complete", uploadId);
    }
    catch(Exception e) {
      try {
        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
      }
      catch(Exception abortException) {
        log.error("Error aborting S3 multipart upload to bucket {} with key {}", bucket, key,
            log.isDebugEnabled() ? abortException : null);
      }
      throw new BlobStoreException("Error uploading blob", e, null);
    }
  }

  private InputStream readChunk(final InputStream input) throws IOException {
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
      return null;
    }
  }
}
