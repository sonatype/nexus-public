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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.s3.internal.ProducerConsumerUploader.ChunkReader.Chunk;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.jmx.reflect.ManagedOperation;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.amazonaws.SdkBaseException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.MIN_VALUE;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Uploads are published to a queue via the calling thread.
 * A pool of tasks consumes the upload requests and returns the {@link PartETag}
 *
 * @since 3.28
 */
@Singleton
@ManagedObject
@ManagedLifecycle(phase = STORAGE)
@Named("producerConsumerUploader")
public class ProducerConsumerUploader
    extends StateGuardLifecycleSupport
    implements S3Uploader
{
  private static final String METRIC_NAME = "uploader";

  private static final PartETag POISON_TAG = new PartETag(MIN_VALUE, "failure");

  private static final Chunk EMPTY_CHUNK = new ChunkReader.Chunk(0, new byte[0], 0);

  private final int chunkSize;

  private final int threadCount;

  private final Timer readChunk;

  private final Timer uploadChunk;

  private final Timer multipartUpload;

  private final BlockingQueue<UploadBundle> waitingRequests;

  private ExecutorService executorService;

  @Inject
  public ProducerConsumerUploader(
      @Named("${nexus.s3.producerConsumerUploader.chunksize:-10485760}") final int chunkSize,
      @Named("${nexus.s3.producerConsumerUploader.parallelism:-0}") final int numberOfThreads,
      final MetricRegistry registry)
  {
    checkArgument(numberOfThreads >= 0, "Must use a non-negative parallelism");
    checkArgument(chunkSize >= 0, "Must use a non-negative chunkSize");
    this.chunkSize = chunkSize;
    this.threadCount = (numberOfThreads > 0) ? numberOfThreads : Runtime.getRuntime().availableProcessors();
    this.waitingRequests = new LinkedBlockingQueue<>(threadCount);

    readChunk = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "readChunk"));
    uploadChunk = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "uploadChunk"));
    multipartUpload = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "multiPartUpload"));
  }

  @Override
  protected void doStart() {
    executorService = newFixedThreadPool(threadCount,
        new NexusThreadFactory("s3-parallel", "producerConsumerThreads"));
    for (int workerCount = 0; workerCount < threadCount; workerCount++) {
      executorService.submit(new ChunkUploader(waitingRequests));
    }
  }

  @Override
  protected void doStop() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdownNow();
      executorService = null;
    }
  }

  @ManagedOperation(description = "Restarts the uploader with a new threadpool")
  public void bounce() throws Exception {
    log.debug("Bouncing ProducerConsumerUploader");
    this.stop();
    this.start();
  }

  @Override
  @Guarded(by = STARTED)
  @Timed
  public void upload(final AmazonS3 s3, final String bucket, final String key, final InputStream contents) {

      try (InputStream input = new BufferedInputStream(contents, chunkSize)) {
        log.debug("Starting upload to key {} in bucket {}", key, bucket);
        input.mark(chunkSize);
        ChunkReader firstReader = new ChunkReader(input, readChunk);
        Chunk firstChunk = firstReader.readChunk(chunkSize).orElse(EMPTY_CHUNK);
        input.reset();

        if (firstChunk.dataLength < chunkSize) {
          ObjectMetadata metadata = new ObjectMetadata();
          metadata.setContentLength(firstChunk.dataLength);
          s3.putObject(bucket, key, new ByteArrayInputStream(firstChunk.data, 0, firstChunk.dataLength), metadata);
        }
        else {
          InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucket, key);
          final String uploadId = s3.initiateMultipartUpload(initiateRequest).getUploadId();
          try (Timer.Context uploadContext = multipartUpload.time()){
            List<PartETag> partETags = submitPartUploads(input, bucket, key, uploadId, s3);

            s3.completeMultipartUpload(new CompleteMultipartUploadRequest()
                .withBucketName(bucket)
                .withKey(key)
                .withUploadId(uploadId)
                .withPartETags(partETags));
          }
          catch (InterruptedException interrupted) {
            s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
            Thread.currentThread().interrupt();
          }
          catch (CancellationException | SdkBaseException ex) {
            s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
            throw new BlobStoreException(
                format("Error executing parallel requests for bucket:%s key:%s with uploadId:%s", bucket, key,
                    uploadId),
                ex,
                null);
          }
        }
        log.debug("Finished upload to key {} in bucket {}", key, bucket);
      }
      catch (IOException | SdkClientException e) { // NOSONAR
        throw new BlobStoreException(format("Error uploading blob to bucket:%s key:%s", bucket, key), e, null);
      }
  }

  private List<PartETag> submitPartUploads(
      final InputStream input,
      final String bucket,
      final String key,
      final String uploadId,
      final AmazonS3 s3) throws IOException, InterruptedException
  {
    BlockingQueue<PartETag> tags = new LinkedBlockingQueue<>();
    ChunkReader parallelReader = new ChunkReader(input, readChunk);

    Optional<Chunk> optionalChunk;
    int chunkCount = 0;
      while ((optionalChunk = parallelReader.readChunk(chunkSize)).isPresent()) {
        Chunk chunk = optionalChunk.get();
        chunkCount++;
        UploadPartRequest request = buildRequest(bucket, key, uploadId, chunk);
        waitingRequests.put(new UploadBundle(s3, request, tags));
      }

    List<PartETag> partETags = new ArrayList<>(chunkCount);
    for (int idx = 0; idx < chunkCount; idx++) {
      PartETag partETag = tags.take();
      if (partETag == POISON_TAG) {
        throw new CancellationException("Part upload failed");
      }
      else {
        partETags.add(partETag);
      }
    }

    return partETags;
  }

  private UploadPartRequest buildRequest(
      final String bucket,
      final String key,
      final String uploadId,
      final Chunk chunk)
  {
    return new UploadPartRequest()
        .withBucketName(bucket)
        .withKey(key)
        .withUploadId(uploadId)
        .withPartNumber(chunk.chunkNumber)
        .withInputStream(new ByteArrayInputStream(chunk.data, 0, chunk.dataLength))
        .withPartSize(chunk.dataLength);
  }

  public static class UploadBundle
  {
    public final AmazonS3 s3;

    public final UploadPartRequest request;

    public final BlockingQueue<PartETag> tags;

    public UploadBundle(
        final AmazonS3 s3,
        final UploadPartRequest request,
        final BlockingQueue<PartETag> tags)
    {
      this.s3 = s3;
      this.request = request;
      this.tags = tags;
    }
  }

  public static class ChunkReader
  {
    private final AtomicInteger counter;

    private final InputStream input;

    private final Timer readChunk;

    public ChunkReader(
        final InputStream input,
        final Timer readTimer)
    {
      this.counter = new AtomicInteger(1);
      this.input = checkNotNull(input);
      this.readChunk = checkNotNull(readTimer);
    }

    synchronized Optional<Chunk> readChunk(final int size) throws IOException
    {
      try (Timer.Context readContext = readChunk.time()) {
        byte[] buf = new byte[size];
        int bytesRead = 0;
        int readSize;

        while ((readSize = input.read(buf, bytesRead, size - bytesRead)) != -1 && bytesRead < size) {
          bytesRead += readSize;
        }

        return bytesRead > 0 ? of(
            new Chunk(bytesRead, buf, counter.getAndIncrement())) : empty();
      }
    }

    static class Chunk
    {
      final byte[] data;

      final int dataLength;

      final int chunkNumber;

      Chunk(final int dataLength, final byte[] data, final int chunkNumber) {
        this.dataLength = dataLength;
        this.data = data;  //NOSONAR
        this.chunkNumber = chunkNumber;
      }
    }
  }

  private class ChunkUploader
      implements Runnable
  {
    private final BlockingQueue<UploadBundle> bundles;

    ChunkUploader(final BlockingQueue<UploadBundle> bundles)
    {
      this.bundles = bundles;
    }

    @Override
    public void run() {
      while (true) { //NOSONAR
        try {
          UploadBundle bundle = bundles.take();
          AmazonS3 s3 = bundle.s3;
          BlockingQueue<PartETag> tags = bundle.tags;
          UploadPartRequest request = bundle.request;
          try (Timer.Context uploadContext = uploadChunk.time()) {
            tags.put(s3.uploadPart(request).getPartETag());
          }
          catch (Exception ex) {
            log.error("Error uploading part of multipart upload", ex);
            tags.put(POISON_TAG);
          }
        }
        catch (InterruptedException e) {
          log.debug("Interrupted while uploading a request");
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
