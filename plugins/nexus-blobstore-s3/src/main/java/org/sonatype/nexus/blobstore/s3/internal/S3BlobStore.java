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
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.BlobIdLocationResolver;
import org.sonatype.nexus.blobstore.BlobSupport;
import org.sonatype.nexus.blobstore.CloudBlobStoreSupport;
import org.sonatype.nexus.blobstore.MetricsInputStream;
import org.sonatype.nexus.blobstore.StreamMetrics;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.RawObjectAccess;
import org.sonatype.nexus.blobstore.metrics.MonitoringBlobStoreMetrics;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.cache.CacheLoader.from;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.blobstore.DirectPathLocationStrategy.DIRECT_PATH_ROOT;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreConfigurationHelper.getConfiguredExpirationInDays;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.NOT_IMPLEMENTED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.buildException;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.SHUTDOWN;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STOPPED;

/**
 * A {@link BlobStore} that stores its content on AWS S3.
 *
 * @since 3.6.1
 */
@Named(S3BlobStore.TYPE)
public class S3BlobStore
    extends CloudBlobStoreSupport<S3AttributesLocation>
{
  public static final String TYPE = "S3";

  public static final String CONFIG_KEY = "s3";

  public static final String BUCKET_KEY = "bucket";

  public static final String BUCKET_PREFIX_KEY = "prefix";

  public static final String ACCESS_KEY_ID_KEY = "accessKeyId";

  public static final String SECRET_ACCESS_KEY_KEY = "secretAccessKey";

  public static final String SESSION_TOKEN_KEY = "sessionToken";

  public static final String ASSUME_ROLE_KEY = "assumeRole";

  public static final String REGION_KEY = "region";

  public static final String ENDPOINT_KEY = "endpoint";

  public static final String EXPIRATION_KEY = "expiration";

  public static final String SIGNERTYPE_KEY = "signertype";

  public static final String FORCE_PATH_STYLE_KEY = "forcepathstyle";

  public static final String MAX_CONNECTION_POOL_KEY = "max_connection_pool_size";

  public static final String ENCRYPTION_TYPE = "encryption_type";

  public static final String ENCRYPTION_KEY = "encryption_key";

  public static final String BUCKET_REGEX =
      "^([a-z]|(\\d(?!\\d{0,2}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})))([a-z\\d]|(\\.(?!(\\.|-)))|(-(?!\\.))){1,61}[a-z\\d]$";

  public static final int DEFAULT_EXPIRATION_IN_DAYS = 3;

  public static final int NO_AUTOMATIC_EXPIRY_HARD_DELETE = 0;

  public static final String METADATA_FILENAME = "metadata.properties";

  public static final String TYPE_KEY = "type";

  public static final String TYPE_V1 = "s3/1";

  private static final String CONTENT_PREFIX = "content";

  public static final String DIRECT_PATH_PREFIX = CONTENT_PREFIX + "/" + DIRECT_PATH_ROOT;

  public static final Tag DELETED_TAG = new Tag("deleted", "true");

  private static final String FILE_V1 = "file/1";

  private final AmazonS3Factory amazonS3Factory;

  private final BucketManager bucketManager;

  private S3Uploader uploader;

  private S3Copier copier;

  private boolean preferExpire;

  private boolean forceHardDelete;

  private boolean preferAsyncCleanup;

  private S3BlobStoreMetricsStore storeMetrics;

  private LoadingCache<BlobId, S3Blob> liveBlobs;

  private AmazonS3 s3;

  private ExecutorService executorService;

  private static final String METRIC_NAME = "s3Blobstore";

  private final Timer existsTimer;

  private final Timer expireTimer;

  private final Timer hardDeleteTimer;

  private RawObjectAccess rawObjectAccess;

  @Inject
  public S3BlobStore(
      final AmazonS3Factory amazonS3Factory,
      final BlobIdLocationResolver blobIdLocationResolver,
      @Named("${nexus.s3.uploaderName:-producerConsumerUploader}") final S3Uploader uploader,
      @Named("${nexus.s3.copierName:-parallelCopier}") final S3Copier copier,
      @Named("${nexus.s3.preferExpire:-false}") final boolean preferExpire,
      @Named("${nexus.s3.forceHardDelete:-false}") final boolean forceHardDelete,
      @Named("${nexus.s3.preferAsyncCleanup:-true}") final boolean preferAsyncCleanup,
      final S3BlobStoreMetricsStore storeMetrics,
      final DryRunPrefix dryRunPrefix,
      final BucketManager bucketManager)
  {
    super(blobIdLocationResolver, dryRunPrefix);
    this.amazonS3Factory = checkNotNull(amazonS3Factory);
    this.copier = checkNotNull(copier);
    this.uploader = checkNotNull(uploader);
    this.storeMetrics = checkNotNull(storeMetrics);
    this.bucketManager = checkNotNull(bucketManager);
    this.preferExpire = preferExpire;

    this.forceHardDelete = forceHardDelete;
    this.preferAsyncCleanup = preferAsyncCleanup;

    MetricRegistry registry = SharedMetricRegistries.getOrCreate("nexus");

    existsTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "exists"));
    expireTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "expire"));
    hardDeleteTimer = registry.timer(MetricRegistry.name(S3BlobStore.class, METRIC_NAME, "hardDelete"));
  }

  @Override
  protected void doStart() throws Exception {
    // ensure blobstore is supported
    S3PropertiesFile metadata = new S3PropertiesFile(s3, getConfiguredBucket(), metadataFilePath());
    if (metadata.exists()) {
      metadata.load();
      String type = metadata.getProperty(TYPE_KEY);
      checkState(TYPE_V1.equals(type) || FILE_V1.equals(type), "Unsupported blob store type/version: %s in %s", type,
          metadata);
    }
    else {
      // assumes new blobstore, write out type
      metadata.setProperty(TYPE_KEY, TYPE_V1);
      metadata.store();
    }
    liveBlobs = CacheBuilder.newBuilder().weakValues().build(from(S3Blob::new));
    storeMetrics.setBucket(getConfiguredBucket());
    storeMetrics.setBucketPrefix(getBucketPrefix());
    storeMetrics.setS3(s3);
    storeMetrics.setBlobStore(this);
    storeMetrics.start();

    if (this.preferAsyncCleanup && executorService == null) {
      this.executorService = newFixedThreadPool(8,
          new NexusThreadFactory("s3-blobstore", "async-ops"));
    }
  }

  @Override
  protected void doStop() throws Exception {
    liveBlobs = null;
    if (executorService != null) {
      executorService.shutdown();
      executorService = null;
    }
    storeMetrics.stop();
  }

  /**
   * Returns path for blob-id content file relative to root directory.
   */
  private String contentPath(final BlobId id) {
    return getLocation(id) + BLOB_FILE_CONTENT_SUFFIX;
  }

  private String metadataFilePath() {
    return getBucketPrefix() + METADATA_FILENAME;
  }

  /**
   * Returns path for blob-id attribute file relative to root directory.
   */
  private String attributePath(final BlobId id) {
    return getLocation(id) + BLOB_FILE_ATTRIBUTES_SUFFIX;
  }

  protected String attributePathString(final BlobId blobId) {
    return attributePath(blobId);
  }

  /**
   * Returns the location for a blob ID based on whether or not the blob ID is for a temporary or permanent blob.
   */
  private String getLocation(final BlobId id) {
    return getContentPrefix() + blobIdLocationResolver.getLocation(id);
  }

  @Override
  @Timed
  @MonitoringBlobStoreMetrics(operationType = UPLOAD)
  protected Blob doCreate(
      final InputStream blobData,
      final Map<String, String> headers,
      @Nullable final BlobId blobId)
  {
    return create(headers, destination -> {
      try (InputStream data = blobData) {
        MetricsInputStream input = new MetricsInputStream(data);
        uploader.upload(s3, getConfiguredBucket(), destination, input);
        return input.getMetrics();
      }
    }, blobId);
  }

  @Override
  @Guarded(by = STARTED)
  @Timed
  public Blob create(final Path sourceFile, final Map<String, String> headers, final long size, final HashCode sha1) {
    throw new BlobStoreException("hard links not supported", null);
  }

  @Timed
  private Blob create(
      final Map<String, String> headers,
      final BlobIngester ingester,
      @Nullable final BlobId assignedBlobId)
  {
    final BlobId blobId = getBlobId(headers, assignedBlobId);

    final String blobPath = contentPath(blobId);
    final String attributePath = attributePath(blobId);
    final boolean isDirectPath = Boolean.parseBoolean(headers.getOrDefault(DIRECT_PATH_BLOB_HEADER, "false"));
    Long existingSize = null;
    if (isDirectPath) {
      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath);
      if (exists(blobId)) {
        existingSize = getContentSizeForDeletion(blobAttributes);
      }
    }

    final S3Blob blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      log.debug("Writing blob {} to {}", blobId, blobPath);

      final StreamMetrics streamMetrics = ingester.ingestTo(blobPath);
      final BlobMetrics metrics = new BlobMetrics(new DateTime(), streamMetrics.getSha1(), streamMetrics.getSize());
      blob.refresh(headers, metrics);
      S3BlobAttributes blobAttributes = writeBlobAttributes(headers, attributePath, metrics);
      if (isDirectPath && existingSize != null) {
        storeMetrics.recordDeletion(existingSize);
      }
      storeMetrics.recordAddition(blobAttributes.getMetrics().getContentSize());

      return blob;
    }
    catch (IOException e) {
      // Something went wrong, clean up the files we created
      deleteQuietly(attributePath);
      deleteQuietly(blobPath);
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  @Guarded(by = STARTED)
  @Timed
  public Blob copy(final BlobId blobId, final Map<String, String> headers) {
    Blob sourceBlob = checkNotNull(get(blobId));
    String sourcePath = contentPath(sourceBlob.getId());
    return create(headers, destination -> {
      copier.copy(s3, getConfiguredBucket(), sourcePath, destination);
      BlobMetrics metrics = sourceBlob.getMetrics();
      return new StreamMetrics(metrics.getContentSize(), metrics.getSha1Hash());
    }, null);
  }

  @Override
  @Guarded(by = STARTED)
  @Timed
  public Blob writeBlobProperties(final BlobId blobId, final Map<String, String> headers) {
    S3Blob blob = ((S3Blob) checkNotNull(get(blobId)));
    String blobPath = contentPath(blob.getId());
    String attributePath = attributePath(blobId);
    BlobMetrics metrics = blob.getMetrics();

    Lock lock = blob.lock();
    try {
      log.debug("Attempting to make blob with id: {} and path: {} permanent.", blobId, blobPath);
      blob.refresh(headers, metrics);
      writeBlobAttributes(headers, attributePath, metrics);
      return blob;
    }
    catch (IOException e) {
      // Something went wrong, clean up the files we created
      deleteQuietly(attributePath);
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
    }
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobId blobId) {
    return get(blobId, false);
  }

  @Nullable
  @Override
  @Timed
  @MonitoringBlobStoreMetrics(operationType = DOWNLOAD)
  public Blob get(final BlobId blobId, final boolean includeDeleted) {
    checkNotNull(blobId);

    log.debug("Accessing blob {}", blobId);

    final S3Blob blob = liveBlobs.getUnchecked(blobId);

    if (blob.isStale()) {
      return refreshBlob(blob, blobId, includeDeleted);
    }
    else {
      return blob;
    }
  }

  @Timed
  private S3Blob refreshBlob(final S3Blob blob, final BlobId blobId, final boolean includeDeleted) {
    Lock lock = blob.lock();
    try {
      if (blob.isStale()) {
        S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath(blobId));
        boolean loaded = blobAttributes.load();
        if (!loaded) {
          log.warn("Attempt to access non-existent blob {} ({})", blobId, blobAttributes);
          return null;
        }

        if (blobAttributes.isDeleted() && !includeDeleted) {
          log.warn("Attempt to access soft-deleted blob {} attributes: {}", blobId, blobAttributes);
          return null;
        }

        blob.refresh(blobAttributes.getHeaders(), blobAttributes.getMetrics());
        return blob;
      }
      else {
        return blob;
      }
    }
    catch (IOException e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  @Timed
  protected boolean doDelete(final BlobId blobId, final String reason) {
    if (forceHardDelete) {
      return performHardDelete(blobId);
    }
    else if (deleteByExpire()) {
      return expire(blobId, reason);
    }
    else {
      return performHardDelete(blobId);
    }
  }

  private boolean deleteByExpire() {
    return getConfiguredExpirationInDays(blobStoreConfiguration) != NO_AUTOMATIC_EXPIRY_HARD_DELETE;
  }

  @Timed
  private boolean expire(final BlobId blobId, final String reason) {
    final S3Blob blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try (final Timer.Context expireContext = expireTimer.time()) {
      log.debug("Soft deleting blob {}", blobId);

      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath(blobId));

      boolean loaded = blobAttributes.load();
      if (!loaded) {
        // This could happen under some concurrent situations (two threads try to delete the same blob)
        // but it can also occur if the deleted index refers to a manually-deleted blob.
        log.warn("Attempt to mark-for-delete non-existent blob {}", blobId);
        return false;
      }
      else if (blobAttributes.isDeleted()) {
        log.debug("Attempt to delete already-deleted blob {}", blobId);
        return false;
      }

      blobAttributes.setDeleted(true);
      blobAttributes.setDeletedReason(reason);
      blobAttributes.setDeletedDateTime(new DateTime());
      blobAttributes.store();

      try {
        // soft delete is implemented using an S3 lifecycle that sets expiration on objects with DELETED_TAG
        // tag the bytes
        s3.setObjectTagging(tagAsDeleted(contentPath(blobId)));
        // tag the attributes
        s3.setObjectTagging(tagAsDeleted(attributePath(blobId)));
      } catch (AmazonS3Exception e) {
        if (NOT_IMPLEMENTED_CODE.equals(e.getErrorCode())) {
          log.warn("Bucket does not support object tagging.", e);
        } else {
          throw e;
        }
      }
      blob.markStale();

      Long contentSize = getContentSizeForDeletion(blobAttributes);
      if (contentSize != null) {
        storeMetrics.recordDeletion(contentSize);
      }

      return true;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
    }
  }

  private SetObjectTaggingRequest tagAsDeleted(final String key) {
    return new SetObjectTaggingRequest(
        getConfiguredBucket(),
        key,
        new ObjectTagging(singletonList(DELETED_TAG))
    );
  }

  private SetObjectTaggingRequest untagAsDeleted(final String key) {
    return new SetObjectTaggingRequest(
        getConfiguredBucket(),
        key,
        new ObjectTagging(emptyList())
    );
  }

  @Override
  protected boolean doDeleteHard(final BlobId blobId) {
    if (forceHardDelete) {
      return performHardDelete(blobId);
    }
    else if (preferExpire && deleteByExpire()) {
      return expire(blobId, "hard-delete");
    }
    else {
      return performHardDelete(blobId);
    }
  }

  @Timed
  private boolean performHardDelete(final BlobId blobId) {
    final S3Blob blob = liveBlobs.getUnchecked(blobId);
    Lock lock = blob.lock();
    try (final Timer.Context performHardDeleteContext = hardDeleteTimer.time()) {
      log.debug("Hard deleting blob {}", blobId);

      String attributePath = attributePath(blobId);
      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath);
      Long contentSize = getContentSizeForDeletion(blobAttributes);

      String blobPath = contentPath(blobId);
      boolean blobDeleted = batchDelete(blobPath, attributePath);

      if (blobDeleted && contentSize != null) {
        storeMetrics.recordDeletion(contentSize);
      }

      return blobDeleted;
    }
    finally {
      lock.unlock();
      liveBlobs.invalidate(blobId);
    }
  }

  @Nullable
  @Timed
  private Long getContentSizeForDeletion(final S3BlobAttributes blobAttributes) {
    try {
      blobAttributes.load();
      return blobAttributes.getMetrics() != null ? blobAttributes.getMetrics().getContentSize() : null;
    }
    catch (Exception e) {
      log.warn("Unable to load attributes {}, delete will not be added to metrics.", blobAttributes, e);
      return null;
    }
  }

  @Override
  @Guarded(by = STARTED)
  @Timed
  public BlobStoreMetrics getMetrics() {
    return storeMetrics.getMetrics();
  }

  @Override
  public Map<OperationType, OperationMetrics> getOperationMetricsByType() {
    return storeMetrics.getOperationMetrics();
  }

  @Override
  protected void doInit(final BlobStoreConfiguration configuration) {
    try {
      this.s3 = amazonS3Factory.create(configuration);
      bucketManager.setS3(s3);
      bucketManager.prepareStorageLocation(blobStoreConfiguration);
      S3BlobStoreConfigurationHelper.setConfiguredBucket(blobStoreConfiguration, getConfiguredBucket());
      rawObjectAccess = new S3RawObjectAccess(getConfiguredBucket(), getBucketPrefix(), s3, performanceLogger, uploader);
    }
    catch (AmazonS3Exception e) {
      throw buildException(e);
    }
    catch (S3BlobStoreException e) {
      throw e;
    }
    catch (Exception e) {
      throw new BlobStoreException("Unable to initialize blob store bucket: " + getConfiguredBucket(), e, null);
    }
  }

  private boolean batchDelete(final String... paths) {
    DeleteObjectsRequest request = new DeleteObjectsRequest(getConfiguredBucket())
        .withKeys(paths);
    return s3.deleteObjects(request).getDeletedObjects().size() == paths.length;
  }

  private void deleteQuietly(final String path) {
    s3.deleteObject(getConfiguredBucket(), path);
  }

  private String getConfiguredBucket() {
    return S3BlobStoreConfigurationHelper.getConfiguredBucket(blobStoreConfiguration);
  }

  private String getBucketPrefix() {
    return S3BlobStoreConfigurationHelper.getBucketPrefix(blobStoreConfiguration);
  }

  /**
   * @return the complete content prefix, including the trailing slash
   */
  private String getContentPrefix() {
    final String bucketPrefix = getBucketPrefix();
    if (isNullOrEmpty(bucketPrefix)) {
      return CONTENT_PREFIX + "/";
    }
    return bucketPrefix + CONTENT_PREFIX + "/";
  }

  /**
   * Delete files known to be part of the S3BlobStore implementation if the content directory is empty.
   */
  @Override
  @Guarded(by = {NEW, STOPPED, FAILED, SHUTDOWN})
  public void remove() {
    try {
      boolean contentEmpty = s3.listObjects(getConfiguredBucket(), getContentPrefix()).getObjectSummaries().isEmpty();
      if (contentEmpty) {
        S3PropertiesFile metadata = new S3PropertiesFile(s3, getConfiguredBucket(), metadataFilePath());
        metadata.remove();
        storeMetrics.remove();
        bucketManager.deleteStorageLocation(getBlobStoreConfiguration());
      }
      else {
        log.warn("Unable to delete non-empty blob store content directory in bucket {}", getConfiguredBucket());
        s3.deleteBucketLifecycleConfiguration(getConfiguredBucket());
      }
    }
    catch (AmazonS3Exception s3Exception) {
      if ("BucketNotEmpty".equals(s3Exception.getErrorCode())) {
        log.warn("Unable to delete non-empty blob store bucket {}", getConfiguredBucket());
      }
      else {
        throw new BlobStoreException(s3Exception, null);
      }
    }
    catch (IOException e) {
      throw new BlobStoreException(e, null);
    }
  }

  class S3Blob
      extends BlobSupport
  {
    S3Blob(final BlobId blobId) {
      super(blobId);
    }

    @Override
    protected InputStream doGetInputStream() {
      S3Object object = s3.getObject(getConfiguredBucket(), contentPath(getId()));
      return performanceLogger.maybeWrapForPerformanceLogging(object.getObjectContent());
    }
  }

  private interface BlobIngester
  {
    StreamMetrics ingestTo(final String destination) throws IOException;
  }

  @Override
  @Timed
  public Stream<BlobId> getBlobIdStream() {
    Iterable<S3ObjectSummary> summaries = S3Objects.withPrefix(s3, getConfiguredBucket(), getContentPrefix());
    return blobIdStream(stream(summaries.spliterator(), false));
  }

  @Override
  public Stream<BlobId> getBlobIdUpdatedSinceStream(final int sinceDays) {
    if (sinceDays < 0) {
      throw new IllegalArgumentException("sinceDays must >= 0");
    }
    else {
      Iterable<S3ObjectSummary> summaries = S3Objects.withPrefix(s3, getConfiguredBucket(), getContentPrefix());
      OffsetDateTime offsetDateTime = Instant.now().minus(sinceDays, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC);

      return blobIdStream(stream(summaries.spliterator(), false)
          .filter(s3objectSummary -> s3objectSummary.getLastModified().toInstant().atOffset(ZoneOffset.UTC).isAfter(offsetDateTime)));
    }
  }

  @Override
  @Timed
  public Stream<BlobId> getDirectPathBlobIdStream(final String prefix) {
    String subpath = getBucketPrefix() + format("%s/%s", DIRECT_PATH_PREFIX, prefix);
    Iterable<S3ObjectSummary> summaries = S3Objects.withPrefix(s3, getConfiguredBucket(), subpath);
    return stream(summaries.spliterator(), false)
        .map(S3ObjectSummary::getKey)
        .filter(key -> key.endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX))
        .map(this::attributePathToDirectPathBlobId);
  }

  private Stream<S3ObjectSummary> nonTempBlobPropertiesFileStream(final Stream<S3ObjectSummary> summaries) {
    return summaries
        .filter(o -> o.getKey().endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX))
        .filter(this::isNotTempBlob);
  }

  private boolean isNotTempBlob(final S3ObjectSummary object) {
    try {
      ObjectMetadata objectMetadata = s3.getObjectMetadata(getConfiguredBucket(), object.getKey());
      Map<String, String> userMetadata = objectMetadata.getUserMetadata();
      return !userMetadata.containsKey(TEMPORARY_BLOB_HEADER);
    }
    catch (Exception e) {
      // On occasion a blob might be deleted between our retrieving the summary and asking for the metadata
      log.debug("An error occurred determining whether blob was temporary", e);
      return false;
    }
  }

  private Stream<BlobId> blobIdStream(final Stream<S3ObjectSummary> summaries) {
    return nonTempBlobPropertiesFileStream(summaries)
        .map(S3AttributesLocation::new)
        .map(this::getBlobIdFromAttributeFilePath)
        .map(BlobId::new);
  }

  @Nullable
  @Override
  @Timed
  public BlobAttributes getBlobAttributes(final BlobId blobId) {
    try {
      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath(blobId));
      return blobAttributes.load() ? blobAttributes : null;
    }
    catch (Exception e) {
      log.error("Unable to load S3BlobAttributes for blob id: {}", blobId, e);
      return null;
    }
  }

  @Override
  @Timed
  public BlobAttributes getBlobAttributes(final S3AttributesLocation attributesFilePath) throws IOException {
    S3BlobAttributes s3BlobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(),
        attributesFilePath.getFullPath());
    s3BlobAttributes.load();
    return s3BlobAttributes;
  }

  @Override
  @Timed
  public void setBlobAttributes(BlobId blobId, BlobAttributes blobAttributes) {
    try {
      S3BlobAttributes s3BlobAttributes = (S3BlobAttributes) getBlobAttributes(blobId);
      s3BlobAttributes.updateFrom(blobAttributes);
      s3BlobAttributes.store();
    }
    catch (Exception e) {
      log.error("Unable to set BlobAttributes for blob id: {}, exception: {}",
          blobId, e.getMessage(), log.isDebugEnabled() ? e : null);
    }
  }

  @Override
  @Timed
  protected void doUndelete(final BlobId blobId, final BlobAttributes attributes) {
    try {
      s3.setObjectTagging(untagAsDeleted(contentPath(blobId)));
      s3.setObjectTagging(untagAsDeleted(attributePath(blobId)));
    } catch (AmazonS3Exception e) {
      if (NOT_IMPLEMENTED_CODE.equals(e.getErrorCode())) {
        log.warn("Bucket does not support object tagging.", e);
      } else {
        throw e;
      }
    }
    storeMetrics.recordAddition(attributes.getMetrics().getContentSize());
  }

  @Override
  @Timed
  public boolean isStorageAvailable() {
    try {
      return s3.doesBucketExistV2(getConfiguredBucket());
    }
    catch (SdkBaseException e) {
      log.warn("S3 bucket '{}' is not writable.", getConfiguredBucket(), e);
      return false;
    }
  }

  /**
   * This is a simple existence check resulting from NEXUS-16729.  This allows clients to perform a simple check
   * primarily intended for use in directpath scenarios.
   */
  @Override
  @Timed
  public boolean exists(final BlobId blobId) {
    checkNotNull(blobId);
    S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath(blobId));
    try (final Timer.Context existsContext = existsTimer.time()) {
      return blobAttributes.load();
    }
    catch (IOException ioe) {
      log.debug("Unable to load attributes {} during existence check, exception", blobAttributes, ioe);
      return false;
    }
  }

  @Override
  @Timed
  public Future<Boolean> asyncDelete(final BlobId blobId) {
    if (preferAsyncCleanup) {
      return executorService.submit(() -> this.deleteHard(blobId));
    } else {
      return CompletableFuture.completedFuture(this.deleteHard(blobId));
    }
  }

  @Override
  @Timed
  public boolean deleteIfTemp(final BlobId blobId) {
    S3Blob blob = liveBlobs.getUnchecked(blobId);
    if (blob != null) {
      Map<String, String> headers = blob.getHeaders();
      if (headers == null || headers.containsKey(TEMPORARY_BLOB_HEADER)) {
        return deleteHard(blobId);
      }
      log.debug("Not deleting. Blob with id: {} is permanent.", blobId.asUniqueString());
    }
    return false;
  }

  /**
   * Used by {@link #getDirectPathBlobIdStream(String)} to convert an s3 key to a {@link BlobId}.
   *
   * @see BlobIdLocationResolver
   */
  private BlobId attributePathToDirectPathBlobId(final String s3Key) { // NOSONAR
    checkArgument(s3Key.startsWith(getBucketPrefix() + DIRECT_PATH_PREFIX + "/"), "Not direct path blob path: %s",
        s3Key);
    checkArgument(s3Key.endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX), "Not blob attribute path: %s", s3Key);
    String blobName = s3Key
        .substring(0, s3Key.length() - BLOB_FILE_ATTRIBUTES_SUFFIX.length())
        .substring((getBucketPrefix() + DIRECT_PATH_PREFIX).length() + 1);
    Map<String, String> headers = ImmutableMap.of(
        BLOB_NAME_HEADER, blobName,
        DIRECT_PATH_BLOB_HEADER, "true"
    );
    return blobIdLocationResolver.fromHeaders(headers);
  }

  @Override
  public RawObjectAccess getRawObjectAccess() {
    return rawObjectAccess;
  }

  private S3BlobAttributes writeBlobAttributes(
      final Map<String, String> headers,
      final String attributePath,
      final BlobMetrics metrics) throws IOException
  {
    S3BlobAttributes blobAttributes =
        new S3BlobAttributes(s3, getConfiguredBucket(), attributePath, headers, metrics);
    blobAttributes.store();
    return blobAttributes;
  }
}
