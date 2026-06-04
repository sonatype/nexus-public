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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.s3.internal.ParallelUploader.ChunkReader.Chunk;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Uploads an InputStream, using multipart upload in parallel if the file is larger or equal to the chunk size.
 * A normal putObject request is used instead if only a single chunk would be sent.
 *
 * @since 3.19
 */
@ConditionalOnProperty(name = "nexus.s3.uploaderName", havingValue = "parallelUploader")
@Component
@Qualifier("parallelUploader")
public class ParallelUploader
    extends ParallelRequester
    implements S3Uploader
{
  private static final Chunk EMPTY_CHUNK = new ChunkReader.Chunk(0, new byte[0], 0);

  @Autowired
  public ParallelUploader(
      @Value("${nexus.s3.parallelRequests.chunksize:5242880}") final int chunkSize,
      @Value("${nexus.s3.parallelRequests.parallelism:0}") final int nThreads)
  {
    super(chunkSize, nThreads, "uploadThreads");
  }

  @Override
  public void upload(final EncryptingS3Client s3, final String bucket, final String key, final InputStream contents) {
    try (InputStream input = new BufferedInputStream(contents, chunkSize)) {
      log.debug("Starting upload to key {} in bucket {}", key, bucket);

      input.mark(chunkSize);
      ChunkReader chunkReader = new ChunkReader(input);
      Chunk chunk = chunkReader.readChunk(chunkSize).orElse(EMPTY_CHUNK);
      input.reset();

      if (chunk.dataLength < chunkSize) {
        final PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentLength((long) chunk.dataLength)
            .build();

        final RequestBody requestBody = RequestBody.fromInputStream(
            new ByteArrayInputStream(chunk.data, 0, chunk.dataLength), chunk.dataLength);
        s3.putObject(putRequest, requestBody);
      }
      else {
        ChunkReader parallelReader = new ChunkReader(input);
        parallelRequests(s3, bucket, key,
            () -> (uploadId -> uploadChunks(s3, bucket, key, uploadId, parallelReader)));
      }
      log.debug("Finished upload to key {} in bucket {}", key, bucket);
    }
    catch (IOException | SdkClientException e) { // NOSONAR
      throw new BlobStoreException(format("Error uploading blob to bucket:%s key:%s", bucket, key), e, null);
    }
  }

  private List<CompletedPart> uploadChunks(
      final EncryptingS3Client s3,
      final String bucket,
      final String key,
      final String uploadId,
      final ChunkReader chunkReader) throws IOException
  {
    List<CompletedPart> tags = new ArrayList<>();
    Optional<Chunk> chunkOpt;

    while ((chunkOpt = chunkReader.readChunk(chunkSize)).isPresent()) {
      final Chunk chunk = chunkOpt.get();

      UploadPartRequest request = UploadPartRequest.builder()
          .bucket(bucket)
          .key(key)
          .uploadId(uploadId)
          .partNumber(chunk.chunkNumber)
          .contentLength((long) chunk.dataLength)
          .build();

      final UploadPartResponse uploadedPart = uploadPart(s3, request, chunk);

      tags.add(CompletedPart.builder()
          .partNumber(chunk.chunkNumber)
          .eTag(uploadedPart.eTag())
          .build());
    }

    return tags;
  }

  private UploadPartResponse uploadPart(
      final EncryptingS3Client s3,
      final UploadPartRequest request,
      final Chunk chunk)
  {
    return s3.uploadPart(request,
        RequestBody.fromInputStream(
            new ByteArrayInputStream(chunk.data, 0, chunk.dataLength),
            chunk.dataLength));
  }

  static class ChunkReader
  {
    private final AtomicInteger counter;

    private final InputStream input;

    private ChunkReader(final InputStream input) {
      this.counter = new AtomicInteger(1);
      this.input = checkNotNull(input);
    }

    synchronized Optional<Chunk> readChunk(final int size) throws IOException {
      byte[] buf = new byte[size];
      int bytesRead = 0;
      int readSize;

      while ((readSize = input.read(buf, bytesRead, size - bytesRead)) != -1 && bytesRead < size) {
        bytesRead += readSize;
      }

      return bytesRead > 0 ? of(new Chunk(bytesRead, buf, counter.getAndIncrement())) : empty();
    }

    static class Chunk
    {
      final byte[] data;

      final int dataLength;

      final int chunkNumber;

      Chunk(final int dataLength, final byte[] data, final int chunkNumber) {
        this.dataLength = dataLength;
        this.data = data; // NOSONAR
        this.chunkNumber = chunkNumber;
      }
    }
  }
}
