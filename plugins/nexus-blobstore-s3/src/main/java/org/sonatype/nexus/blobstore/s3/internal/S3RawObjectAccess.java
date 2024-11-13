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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.PerformanceLogger;
import org.sonatype.nexus.blobstore.api.RawObjectAccess;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Implementation of {@link RawObjectAccess} for the {@link S3BlobStore}.
 *
 * @since 3.31
 */
public class S3RawObjectAccess
    implements RawObjectAccess
{
  /* How many keys to fetch in one request (maximum value is 1000) */
  private static final int LIST_RAW_OBJECTS_MAX_KEYS = 1000;

  private final String bucket;

  private final String bucketPrefix;

  private final AmazonS3 s3;

  private final PerformanceLogger performanceLogger;

  private S3Uploader uploader;

  public S3RawObjectAccess(
      final String bucket,
      final String bucketPrefix,
      final AmazonS3 s3,
      final PerformanceLogger performanceLogger,
      final S3Uploader uploader)
  {
    this.bucket = requireNonNull(bucket);
    this.bucketPrefix = requireNonNull(bucketPrefix);
    this.s3 = requireNonNull(s3);
    this.performanceLogger = requireNonNull(performanceLogger);
    this.uploader = requireNonNull(uploader);
  }

  /**
   * List raw objects at this path in the blobstore. This implementation returns a maximum of 10,000 results.
   */
  @Override
  public Stream<String> listRawObjects(@Nullable final Path path) {
    final String prefix = bucketPrefix + normalizeS3Path(path, true);

    ObjectListing listing = s3.listObjects(
        new ListObjectsRequest().withBucketName(bucket)
            .withPrefix(prefix)
            .withDelimiter("/")
            .withMaxKeys(LIST_RAW_OBJECTS_MAX_KEYS));

    List<String> rawObjects = new ArrayList<>(listingToFilenames(listing));

    while (listing.isTruncated()) {
      listing = s3.listNextBatchOfObjects(listing);
      rawObjects.addAll(listingToFilenames(listing));
    }

    return rawObjects.stream().sorted();
  }

  private List<String> listingToFilenames(final ObjectListing listing) {
    return listing.getObjectSummaries().stream().map(s -> {
      String key = s.getKey();
      return key.substring(key.lastIndexOf('/') + 1);
    }).collect(toList());
  }

  @Override
  @Nullable
  public InputStream getRawObject(final Path path) {
    try {
      S3Object object = s3.getObject(bucket, bucketPrefix + normalizeS3Path(path));
      return performanceLogger.maybeWrapForPerformanceLogging(object.getObjectContent());
    }
    catch (AmazonServiceException e) {
      if (e.getStatusCode() == 404) {
        return null;
      }
      throw e;
    }
  }

  @Override
  public void putRawObject(final Path path, final InputStream input) {
    try (InputStream in = input) {
      uploader.upload(s3, bucket, bucketPrefix + normalizeS3Path(path), in);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean hasRawObject(final Path path) {
    try {
      return s3.doesObjectExist(bucket, bucketPrefix + normalizeS3Path(path));
    }
    catch (AmazonServiceException e) {
      if (e.getStatusCode() == 404) {
        return false;
      }
      throw e;
    }
  }

  @Override
  public void deleteRawObject(final Path path) {
    s3.deleteObject(bucket, bucketPrefix + normalizeS3Path(path));
  }

  @Override
  public void deleteRawObjectsInPath(final Path path) {
    final String prefix = bucketPrefix + normalizeS3Path(path, true);
    ObjectListing listing = s3.listObjects(
        new ListObjectsRequest().withBucketName(bucket)
            .withPrefix(prefix)
            .withDelimiter("/")
            .withMaxKeys(LIST_RAW_OBJECTS_MAX_KEYS));
    deleteObjectsInListing(listing);

    while (listing.isTruncated()) {
      listing = s3.listNextBatchOfObjects(listing);
      deleteObjectsInListing(listing);
    }
  }

  private void deleteObjectsInListing(final ObjectListing listing) {
    List<KeyVersion> keys = listing.getObjectSummaries().stream().map(s -> new KeyVersion(s.getKey())).collect(toList());
    s3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(keys));
  }

  private String normalizeS3Path(final Path path) {
    return normalizeS3Path(path, false);
  }

  private String normalizeS3Path(final Path path, final boolean requireTrailingSlash) {
    if (path == null) {
      return "/";
    }

    String normalized = path.toString().replace("\\", "/");
    if (requireTrailingSlash && !normalized.endsWith("/")) {
      return normalized + "/";
    }
    return normalized;
  }
}
