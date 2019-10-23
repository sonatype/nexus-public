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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.PartETag;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

/**
 * Common class to execute parallel requests to S3 for a MultipartUpload operation
 *
 * @since 3.19
 */
public abstract class ParallelRequester
    extends StateGuardLifecycleSupport
{
  protected final int chunkSize;

  private final int parallelism;

  private final ExecutorService executorService;

  /**
   * @param chunkSize       - the number of bytes to be processed in one parallel request
   * @param numberOfThreads - a non-negative integer, either 0 to indicate that number of threads should be dynamically
   *                        selected based on the env, or a postive int to set a fixed number of threads
   * @param threadGroupName - a human readable name for the threads
   */
  public ParallelRequester(final int chunkSize, final int numberOfThreads, final String threadGroupName)
  {
    checkArgument(numberOfThreads >= 0, "Must use a non-negative parallelism");
    checkArgument(chunkSize >= 0, "Must use a non-negative chunkSize");
    this.chunkSize = chunkSize;
    this.parallelism = (numberOfThreads > 0) ? numberOfThreads : Runtime.getRuntime().availableProcessors();

    this.executorService = Executors.newFixedThreadPool(parallelism,
        new NexusThreadFactory("s3-parallel", threadGroupName));
  }

  @Override
  protected void doStop() {
    executorService.shutdownNow();
  }


  @FunctionalInterface
  protected interface IOFunction<T, R>
  {
    R apply(T v) throws IOException;
  }

  protected void parallelRequests(final AmazonS3 s3,
                                  final String bucket,
                                  final String key,
                                  final Supplier<IOFunction<String, List<PartETag>>> operations)
  {
    InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucket, key);
    String uploadId = s3.initiateMultipartUpload(initiateRequest).getUploadId();

    CompletionService<List<PartETag>> completionService = new ExecutorCompletionService<>(executorService);
    try {
      for (int i = 0; i < parallelism; i++) {
        completionService.submit(() -> operations.get().apply(uploadId));
      }

      List<PartETag> partETags = new ArrayList<>();
      for (int i = 0; i < parallelism; i++) {
        partETags.addAll(completionService.take().get());
      }

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
    catch (CancellationException | ExecutionException ex) {
      s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
      throw new BlobStoreException(
          format("Error executing parallel requests for bucket:%s key:%s with uploadId:%s", bucket, key, uploadId), ex,
          null);
    }
  }
}
