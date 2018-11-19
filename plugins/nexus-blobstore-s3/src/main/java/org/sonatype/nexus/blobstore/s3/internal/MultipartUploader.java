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

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

import static com.google.common.base.Preconditions.checkState;

/**
 * Uploads a file, using multipart upload if the file is larger or equal to the chunk size.  A normal putObject request
 * is used instead if only a single chunk would be sent.
 *
 * @since 3.12
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
    try (InputStream input = contents) {
      InputStream chunkOne = readChunk(input);
      if (chunkOne.available() < chunkSize) {
        uploadSinglePart(s3, bucket, key, chunkOne);
      }
      else {
        uploadMultiPart(s3, bucket, key, chunkOne, contents);
      }
    }
    catch(IOException | SdkClientException e) { // NOSONAR
      throw new BlobStoreException("Error uploading blob", e, null);
    }
  }

  private void uploadSinglePart(final AmazonS3 s3, final String bucket, final String key, final InputStream contents)
      throws IOException {
    log.debug("Starting upload to key {} in bucket {} of {} bytes", key, bucket, contents.available());
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(contents.available());
    s3.putObject(bucket, key, contents, metadata);
  }

  private void uploadMultiPart(final AmazonS3 s3,
                               final String bucket,
                               final String key,
                               final InputStream firstChunk,
                               final InputStream restOfContents)
      throws IOException {
    checkState(firstChunk.available() > 0);
    String uploadId = null;
    try {
      InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucket, key);
      uploadId = s3.initiateMultipartUpload(initiateRequest).getUploadId();

      log.debug("Starting multipart upload {} to key {} in bucket {}", uploadId, key, bucket);

      List<UploadPartResult> results = new ArrayList<>();
      for (int partNumber = 1; ; partNumber++) {
        InputStream chunk = partNumber == 1 ? firstChunk : readChunk(restOfContents);
        if (chunk.available() == 0) {
          break;
        }
        else {
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
      uploadId = null;
    }
    finally {
      if (uploadId != null) {
        try {
          s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
        }
        catch(Exception e) {
          log.error("Error aborting S3 multipart upload to bucket {} with key {}", bucket, key,
              log.isDebugEnabled() ? e : null);
        }
      }
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
      return new ByteArrayInputStream(new byte[0]);
    }
  }
}
