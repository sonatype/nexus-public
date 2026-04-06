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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.BlobIdLocationResolver;
import org.sonatype.nexus.blobstore.BlobSupport;
import org.sonatype.nexus.blobstore.CloudBlobStoreSupport;
import org.sonatype.nexus.blobstore.DateBasedHelper.DateInterval;
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
import org.sonatype.nexus.blobstore.api.BlobStoreMigrationStateProvider;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.api.ExternalMetadata;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsService;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobIndex;
import org.sonatype.nexus.blobstore.metrics.MonitoringBlobStoreMetrics;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaUsageChecker;
import org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.time.DateHelper;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.cache.CacheLoader.from;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.sonatype.nexus.blobstore.DirectPathLocationStrategy.DIRECT_PATH_ROOT;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;
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
@Component
@Qualifier(S3BlobStore.TYPE)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class S3BlobStore
    extends CloudBlobStoreSupport<S3AttributesLocation>
{
  public static final String TYPE = "S3";

  public static final String ACCESS_KEY_ID_KEY = "accessKeyId";

  public static final String SECRET_ACCESS_KEY_KEY = "secretAccessKey";

  public static final String SESSION_TOKEN_KEY = "sessionToken";

  public static final String ASSUME_ROLE_KEY = "assumeRole";

  public static final String REGION_KEY = "region";

  public static final String ENDPOINT_KEY = "endpoint";

  public static final String SIGNERTYPE_KEY = "signertype";

  public static final String FORCE_PATH_STYLE_KEY = "forcepathstyle";

  public static final String MAX_CONNECTION_POOL_KEY = "max_connection_pool_size";

  public static final String ENCRYPTION_TYPE = "encryption_type";

  public static final String ENCRYPTION_KEY = "encryption_key";

  public static final String PRE_SIGNED_URL_ENABLED = "preSignedUrlEnabled";

  public static final String BUCKET_REGEX =
      "^([a-z]|(\\d(?!\\d{0,2}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})))([a-z\\d]|(\\.(?!(\\.|-)))|(-(?!\\.))){1,61}[a-z\\d]$";

  public static final String METADATA_FILENAME = "metadata.properties";

  public static final String TYPE_KEY = "type";

  public static final String TYPE_V1 = "s3/1";

  public static final String DIRECT_PATH_PREFIX = CONTENT_PREFIX + "/" + DIRECT_PATH_ROOT;

  private static final String FILE_V1 = "file/1";

  private final AmazonS3Factory amazonS3Factory;

  private final BucketManager bucketManager;

  private S3Uploader uploader;

  private S3Copier copier;

  private final SoftDeletedBlobIndex deletedBlobIndex;

  private boolean preferAsyncCleanup;

  private BlobStoreMetricsService<S3BlobStore> metricsService;

  private BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker;

  private EncryptingS3Client s3;

  private ExecutorService executorService;

  private static final String METRIC_NAME = "s3Blobstore";

  private final Timer existsTimer;

  private final Timer expireTimer;

  private final Timer hardDeleteTimer;

  @Inject
  public S3BlobStore(
      final AmazonS3Factory amazonS3Factory,
      final BlobIdLocationResolver blobIdLocationResolver,
      final S3Uploader uploader,
      final S3Copier copier,
      @Value("${nexus.s3.preferAsyncCleanup:true}") final boolean preferAsyncCleanup,
      @Qualifier(S3BlobStore.TYPE) final BlobStoreMetricsService<S3BlobStore> metricsService,
      final SoftDeletedBlobIndex deletedBlobIndex,
      final DryRunPrefix dryRunPrefix,
      final BucketManager bucketManager,
      final BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker,
      @Value("${nexus.s3.loadFromDb:true}") final boolean loadFromDb,
      @Nullable final BlobStoreMigrationStateProvider migrationStateProvider)
  {
    super(blobIdLocationResolver, dryRunPrefix, loadFromDb, migrationStateProvider);
    this.amazonS3Factory = checkNotNull(amazonS3Factory);
    this.copier = checkNotNull(copier);
    this.uploader = checkNotNull(uploader);
    this.metricsService = checkNotNull(metricsService);
    this.deletedBlobIndex = checkNotNull(deletedBlobIndex);
    this.blobStoreQuotaUsageChecker = checkNotNull(blobStoreQuotaUsageChecker);
    this.bucketManager = checkNotNull(bucketManager);
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
    liveBlobs = CacheBuilder.newBuilder().weakValues().build(from(this::initializeBlob));
    metricsService.init(this);

    blobStoreQuotaUsageChecker.setBlobStore(this);
    blobStoreQuotaUsageChecker.start();

    if (this.preferAsyncCleanup && executorService == null) {
      this.executorService = newFixedThreadPool(8,
          new NexusThreadFactory("s3-blobstore", "async-ops"));
    }
  }

  @Override
  protected BlobSupport initializeBlob(final BlobId blobId) {
    return new S3Blob(blobId);
  }

  @Override
  protected boolean blobExistsInStorage(final BlobId blobId) {
    try {
      s3.getObjectMetadata(getConfiguredBucket(), contentPath(blobId));
      return true;
    }
    catch (NoSuchKeyException e) {
      log.debug("Blob {} does not exist in S3 bucket {}", blobId, getConfiguredBucket());
      return false;
    }
    catch (Exception e) {
      log.warn("Error checking blob existence for {} in S3", blobId, e);
      // Assume exists on error to avoid false negatives that could break normal operations
      return true;
    }
  }

  @Override
  protected void doStop() throws Exception {
    liveBlobs = null;
    if (executorService != null) {
      executorService.shutdown();
      executorService = null;
    }
    // Properly close S3Client to prevent resource leaks
    // The S3Client (and its internal EncryptingS3Client wrapper) must be closed to:
    // 1. Release HTTP connection pools
    // 2. Close any internal credentials providers and their STS clients
    // 3. Prevent "Connection pool shut down" errors on subsequent uses
    // Note: We don't null out s3 here because remove() may still need it after stop()
    if (s3 != null) {
      try {
        s3.close();
      }
      catch (Exception e) {
        log.warn("Error closing S3 client during blob store shutdown", e);
      }
    }
    metricsService.stop();
    blobStoreQuotaUsageChecker.stop();
  }

  /**
   * Returns path for blob-id content file relative to root directory.
   */
  protected String contentPath(final BlobId id) {
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

  @Override
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

  @Override
  public void createBlobAttributes(
      final BlobId blobId,
      final Map<String, String> headers,
      final BlobMetrics blobMetrics)
  {
    String attributePath = attributePath(blobId);
    try {
      writeBlobAttributes(headers, attributePath, blobMetrics);
    }
    catch (Exception e) {
      // Something went wrong, clean up the file we created
      deleteQuietly(attributePath);
      throw new BlobStoreException(e, blobId);
    }
  }

  @Override
  public S3BlobAttributes createBlobAttributesInstance(
      final BlobId blobId,
      final Map<String, String> headers,
      final BlobMetrics blobMetrics)
  {
    return new S3BlobAttributes(s3, getConfiguredBucket(), attributePath(blobId), headers, blobMetrics);
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

    final S3Blob blob = (S3Blob) liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      log.debug("Writing blob {} to {}", blobId, blobPath);

      final StreamMetrics streamMetrics = ingester.ingestTo(blobPath);
      final BlobMetrics metrics = new BlobMetrics(new DateTime(), streamMetrics.getSha1(), streamMetrics.getSize());
      blob.refresh(headers, metrics);
      S3BlobAttributes blobAttributes = writeBlobAttributes(headers, attributePath, metrics);
      if (isDirectPath && existingSize != null) {
        metricsService.recordDeletion(existingSize);
      }
      metricsService.recordAddition(blobAttributes.getMetrics().getContentSize());

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
  public boolean isInternalMoveSupported(final BlobStore destBlobStore) {
    return false;
  }

  @Override
  public Blob moveInternal(final BlobStore destBlobStore, final BlobId blobId, final Map<String, String> headers) {
    throw new UnsupportedOperationException("Internal move operation is not supported.");
  }

  @Override
  @Guarded(by = STARTED)
  @Timed
  public Blob writeBlobProperties(final BlobId blobId, final Map<String, String> headers) {
    S3Blob blob = (S3Blob) checkNotNull(get(blobId));
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

  @Override
  @Timed
  protected boolean doDelete(final BlobId blobId, final String reason) {
    final BlobSupport blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try (final Timer.Context expireContext = expireTimer.time()) {
      log.debug("Soft deleting blob {}", blobId);

      String attributePath = attributePath(blobId);
      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath);

      boolean loaded = blobAttributes.load();
      if (!loaded) {
        // This could happen under some concurrent situations (two threads try to delete the same blob)
        // but it can also occur if the deleted index refers to a manually-deleted blob.
        log.warn("Attempt to mark-for-delete non-existent blob {}", blobId);
        return false;
      }
      else if (blobAttributes.isDeleted()) {
        log.debug("Attempt to delete already-deleted blob {}", blobId);
        return true;
      }

      BlobId propRef = new BlobId(blobId.asUniqueString(), UTC.now());
      String softDeletedLocation = attributePath(propRef);

      DateTime deletedDateTime = new DateTime();
      blobAttributes.setDeleted(true);
      blobAttributes.setDeletedReason(reason);
      blobAttributes.setDeletedDateTime(deletedDateTime);

      String softDeletedPrefixLocation = getLocationPrefix(propRef);
      blobAttributes.setSoftDeletedLocation(softDeletedPrefixLocation);

      // Save properties file under the new location
      String originalPrefixLocation = getLocationPrefix(blobId);
      if (!originalPrefixLocation.equals(softDeletedPrefixLocation)) {
        S3BlobAttributes newBlobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), softDeletedLocation);
        newBlobAttributes.updateFrom(blobAttributes);
        newBlobAttributes.setOriginalLocation(originalPrefixLocation);
        newBlobAttributes.store();
      }

      blobAttributes.store();

      deletedBlobIndex.createRecord(blobId);
      blob.markStale();

      return true;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  protected boolean doDeleteHard(final BlobId blobId) {
    final BlobSupport blob = liveBlobs.getUnchecked(blobId);
    Lock lock = blob.lock();
    try (final Timer.Context performHardDeleteContext = hardDeleteTimer.time()) {
      log.debug("Hard deleting blob {}", blobId);

      String attributePath = attributePath(blobId);
      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath);
      Long contentSize = getContentSizeForDeletion(blobAttributes);

      String blobPath = contentPath(blobId);
      boolean blobDeleted = batchDelete(blobPath, attributePath);

      if (blobDeleted && contentSize != null) {
        Optional<String> softDeletedLocation = blobAttributes.getSoftDeletedLocation();
        // Remove copied soft-deleted attributes
        softDeletedLocation.ifPresent(location -> deleteCopiedAttributes(blobId, location));

        metricsService.recordDeletion(contentSize);
      }

      return blobDeleted;
    }
    finally {
      lock.unlock();
      liveBlobs.invalidate(blobId);
    }
  }

  @Guarded(by = STARTED)
  @Override
  protected void doCompact(@Nullable final BlobStoreUsageChecker inUseChecker, final Duration blobsOlderThan) {
    try {
      doCompactWithDeletedBlobIndex(inUseChecker, blobsOlderThan);
    }
    catch (BlobStoreException | TaskInterruptedException e) {
      throw e;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, null);
    }
  }

  void doCompactWithDeletedBlobIndex(
      @Nullable final BlobStoreUsageChecker inUseChecker,
      final Duration blobsOlderThan)
  {
    OffsetDateTime date = OffsetDateTime.now().minus(blobsOlderThan);
    String blobStoreName = blobStoreConfiguration.getName();
    log.info("Begin deleted blobs processing for blob store '{}' before {}", blobStoreName, date);
    // only process each blob once (in-use blobs may be re-added to the index)
    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60)) {
      int numBlobs = deletedBlobIndex.count(date);
      AtomicInteger counter = new AtomicInteger();
      deletedBlobIndex.getRecordsBefore(date).forEach(blobId -> {
        CancelableHelper.checkCancellation();

        BlobSupport blob = liveBlobs.getIfPresent(blobId);
        log.debug("Next available record for compaction: {}", blobId);
        if (Objects.isNull(blob) || blob.isStale()) {
          log.debug("Compacting...");
          maybeCompactBlob(inUseChecker, blobId);
          deletedBlobIndex.deleteRecord(blobId);
        }
        else {
          log.debug("Still in use to deferring");
        }

        progressLogger.info("Blob store '{}' - Elapsed time: {}, processed: {}/{}", blobStoreName,
            progressLogger.getElapsed(), counter.incrementAndGet(), numBlobs);
      });
    }
  }

  private boolean maybeCompactBlob(@Nullable final BlobStoreUsageChecker inUseChecker, final BlobId blobId) {
    Optional<S3BlobAttributes> attributesOption = ofNullable((S3BlobAttributes) getBlobAttributes(blobId));
    if (!attributesOption.isPresent() || !undelete(inUseChecker, blobId, attributesOption.get(), false)) {
      // attributes file is missing or blob id not in use, so it's safe to delete the file
      log.debug("Hard deleting blob id: {}, in blob store: {}", blobId, blobStoreConfiguration.getName());
      return deleteHard(blobId);
    }
    return false;
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
  public BlobStoreMetricsService<S3BlobStore> getMetricsService() {
    return metricsService;
  }

  @Override
  @Guarded(by = STARTED)
  @Timed
  public BlobStoreMetrics getMetrics() {
    return metricsService.getMetrics();
  }

  @Override
  public Map<OperationType, OperationMetrics> getOperationMetricsByType() {
    return metricsService.getOperationMetrics();
  }

  @Override
  public Map<OperationType, OperationMetrics> getOperationMetricsDelta() {
    return metricsService.getOperationMetricsDelta();
  }

  @Override
  public void clearOperationMetrics() {
    metricsService.clearOperationMetrics();
  }

  @Override
  protected void doInit(final BlobStoreConfiguration configuration) {
    try {
      this.s3 = amazonS3Factory.create(configuration);
      bucketManager.setS3(s3);
      bucketManager.prepareStorageLocation(blobStoreConfiguration);
      S3BlobStoreConfigurationHelper.setConfiguredBucket(blobStoreConfiguration, getConfiguredBucket());

      deletedBlobIndex.init(this);
    }
    catch (S3Exception e) {
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
    final ObjectIdentifier[] identifiers = Arrays.stream(paths)
        .map(path -> ObjectIdentifier.builder().key(path).build())
        .toArray(ObjectIdentifier[]::new);

    final Delete delete = Delete.builder()
        .objects(identifiers)
        .build();

    DeleteObjectsRequest request = DeleteObjectsRequest.builder()
        .bucket(getConfiguredBucket())
        .delete(delete)
        .build();
    return s3.deleteObjects(request).deleted().size() == paths.length;
  }

  private void deleteQuietly(final String path) {
    s3.deleteObject(getConfiguredBucket(), path);
  }

  protected String getConfiguredBucket() {
    return S3BlobStoreConfigurationHelper.getConfiguredBucket(blobStoreConfiguration);
  }

  String getBucketPrefix() {
    return S3BlobStoreConfigurationHelper.getBucketPrefix(blobStoreConfiguration);
  }

  protected EncryptingS3Client getS3() {
    return s3;
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
      metricsService.remove();

      boolean contentEmpty = s3.listObjectsV2(getConfiguredBucket(), getContentPrefix()).contents().isEmpty();
      if (contentEmpty) {
        S3PropertiesFile metadata = new S3PropertiesFile(s3, getConfiguredBucket(), metadataFilePath());
        metadata.remove();

        bucketManager.deleteStorageLocation(getBlobStoreConfiguration());
      }
      else {
        log.warn("Unable to delete non-empty blob store content directory in bucket {}", getConfiguredBucket());
      }
    }
    catch (S3Exception s3Exception) {
      if ("BucketNotEmpty".equals(s3Exception.awsErrorDetails().errorCode())) {
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

  protected class S3Blob
      extends BlobSupport
  {
    protected S3Blob(final BlobId blobId) {
      super(blobId);
    }

    @Override
    protected InputStream doGetInputStream() {
      final InputStream inputStream = s3.getObject(getConfiguredBucket(), contentPath(getId()));
      return performanceLogger.maybeWrapForPerformanceLogging(inputStream);
    }

    @VisibleForTesting
    S3BlobStore owner() {
      return S3BlobStore.this;
    }
  }

  private interface BlobIngester
  {
    StreamMetrics ingestTo(final String destination) throws IOException;
  }

  @Override
  @Timed
  public Stream<BlobId> getBlobIdStream() {
    return blobIdStream(s3.listObjectsWithPrefix(getContentPrefix()));
  }

  @Override
  @Guarded(by = STARTED)
  public Stream<BlobId> getBlobIdUpdatedSinceStream(
      final String prefix,
      final OffsetDateTime fromDateTime,
      final OffsetDateTime toDateTime)
  {
    String fullPrefix = getContentPrefix() + prefix;

    DateInterval interval = new DateInterval(fromDateTime, toDateTime);
    Predicate<Blob> blobMatches = !VOLUME_PREFIX.equals(prefix)
        ? this::isNonTempBlob
        : ((Predicate<Blob>) this::isNonTempBlob).and(blob -> isBlobCreatedInRange(blob, interval));

    return s3.listObjectsWithPrefix(fullPrefix)
        // Restrict to bytes & properties files
        .filter(object -> isBlobStoreContent(object.key()))
        // Create a location and parse it into a BlobId
        .map(S3AttributesLocation::new)
        .map(this::getBlobIdFromAttributeFilePath)
        .filter(Objects::nonNull)
        // Check that if the blobId has a date, its in the expected range
        .filter(blobId -> blobIdInRange(blobId, interval))
        // Read the blob attributes and ensure its not a temp blob
        // vol- paths are also checked for creation date
        .filter(blobId -> {
          try {
            return Optional.ofNullable(get(blobId, true))
                .map(blobMatches::test)
                .orElse(true);
          }
          catch (Exception e) {
            return true;
          }
        });
  }

  @Override
  @Timed
  public Stream<BlobId> getDirectPathBlobIdStream(final String prefix) {
    String subpath = getBucketPrefix() + format("%s/%s", DIRECT_PATH_PREFIX, prefix);
    return s3.listObjectsWithPrefix(subpath)
        .map(S3Object::key)
        .filter(key -> key.endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX))
        .map(this::attributePathToDirectPathBlobId);
  }

  private Stream<S3Object> nonTempBlobPropertiesFileStream(final Stream<S3Object> summaries) {
    return summaries
        .filter(o -> o.key().endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX))
        .filter(this::isNotTempBlob);
  }

  private boolean isNotTempBlob(final S3Object object) {
    try {
      final HeadObjectResponse response = s3.getObjectMetadata(getConfiguredBucket(), object.key());
      Map<String, String> userMetadata = response.metadata();
      return !userMetadata.containsKey(TEMPORARY_BLOB_HEADER);
    }
    catch (Exception e) {
      // On occasion a blob might be deleted between our retrieving the summary and asking for the metadata
      log.debug("An error occurred determining whether blob was temporary", e);
      return false;
    }
  }

  private Stream<BlobId> blobIdStream(final Stream<S3Object> summaries) {
    return nonTempBlobPropertiesFileStream(summaries)
        .map(S3AttributesLocation::new)
        .map(this::getBlobIdFromAttributeFilePath)
        .filter(Objects::nonNull);
  }

  @Nullable
  @Override
  public BlobAttributes getBlobAttributes(final BlobId blobId) {
    try {
      return getBlobAttributesWithException(blobId);
    }
    catch (Exception e) {
      return null;
    }
  }

  @Override
  public BlobAttributes getBlobAttributes(final S3AttributesLocation attributesFilePath) throws IOException {
    try {
      S3BlobAttributes s3BlobAttributes = new S3BlobAttributes(
          s3, getConfiguredBucket(), attributesFilePath.getFullPath());
      return s3BlobAttributes.load() ? s3BlobAttributes : null;
    }
    catch (Exception e) {
      log.error("Unable to load S3BlobAttributes by path: {}", attributesFilePath.getFullPath(), e);
      throw new IOException(e);
    }
  }

  @Override
  @Timed
  public void setBlobAttributes(final BlobId blobId, final BlobAttributes blobAttributes) {
    S3BlobAttributes s3BlobAttributes = (S3BlobAttributes) getBlobAttributes(blobId);
    if (s3BlobAttributes != null) {
      try {
        s3BlobAttributes.updateFrom(blobAttributes);
        s3BlobAttributes.store();
      }
      catch (Exception e) {
        log.error("Unable to set BlobAttributes for blob id: {}, exception: {}",
            blobId, e.getMessage(), log.isDebugEnabled() ? e : null);
      }
    }
    else {
      // Benign race condition - concurrent request is updating the same blob properties file
      log.debug("Blob attributes temporarily unavailable for blob id: {} during concurrent access", blobId);
    }
  }

  @Override
  @Timed
  protected void doUndelete(final BlobId blobId, final BlobAttributes attributes) {
    deletedBlobIndex.deleteRecord(blobId);
    metricsService.recordAddition(attributes.getMetrics().getContentSize());
  }

  @Override
  protected void deleteCopiedAttributes(final BlobId blobId, final String softDeletedLocation) {
    deleteQuietly(attributePath(createBlobIdForTimePath(blobId, softDeletedLocation)));
  }

  @Override
  @Timed
  public boolean isStorageAvailable() {
    try {
      return s3.doesBucketExist(getConfiguredBucket());
    }
    catch (SdkClientException e) {
      log.warn("S3 bucket '{}' is not writable.", getConfiguredBucket(), e);
      return false;
    }
  }

  /**
   * This is a simple existence check resulting from NEXUS-16729. This allows clients to perform a simple check
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
  public boolean bytesExists(final BlobId blobId) {
    checkNotNull(blobId);
    try (final Timer.Context existsContext = existsTimer.time()) {
      return s3.doesObjectExist(getConfiguredBucket(), contentPath(blobId));
    }
    catch (Exception e) {
      log.debug("Unable to check existence of {}", contentPath(blobId));
      throw e;
    }
  }

  @Override
  @Timed
  public Future<Boolean> asyncDelete(final BlobId blobId) {
    if (preferAsyncCleanup) {
      return executorService.submit(() -> this.deleteHard(blobId));
    }
    else {
      return CompletableFuture.completedFuture(this.deleteHard(blobId));
    }
  }

  @Override
  public Blob getBlobFromCache(final BlobId blobId) {
    return liveBlobs.getUnchecked(blobId);
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
        DIRECT_PATH_BLOB_HEADER, "true");
    return blobIdLocationResolver.fromHeaders(headers);
  }

  @Override
  @VisibleForTesting
  public void flushMetrics() throws IOException {
    metricsService.flush();
  }

  @Override
  public Optional<ExternalMetadata> getExternalMetadata(final BlobId blobId) {
    String path = contentPath(blobId);
    try {
      final HeadObjectResponse objectMetadata = s3.getObjectMetadata(getConfiguredBucket(), path);

      return Optional.of(new ExternalMetadata(objectMetadata.eTag(),
          DateHelper.toOffsetDateTime(Date.from(objectMetadata.lastModified()))));
    }
    catch (Exception e) {
      log.warn("Unable to retrieve remote metadata for path {} cause {} {}", path, e.getMessage(),
          log.isDebugEnabled() ? "Internal Exception: " + e : null);
    }
    return Optional.empty();
  }

  @Override
  public boolean isOwner(final Blob blob) {
    return blob instanceof S3Blob s3blob && s3blob.owner() == this;
  }

  @Override
  public BlobAttributes loadBlobAttributes(final BlobId blobId) throws IOException {
    String attributePath = attributePath(blobId);
    S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath);

    try {
      return blobAttributes.load() ? blobAttributes : null;
    }
    catch (IOException e) {
      // NEXUS-50152 Fix: Distinguish between transient S3 errors and actual corruption
      // Do NOT delete properties files here - let the repair task handle deletion

      // Check if object exists - if not, return null (normal for missing properties)
      if (!s3.doesObjectExist(getConfiguredBucket(), attributePath)) {
        log.debug("Properties file {} for blob {} does not exist in S3 (normal for new blobs or after deletion)",
            attributePath, blobId);
        return null;
      }

      // File exists but couldn't be loaded - likely transient S3 error
      // Be conservative: DO NOT delete on I/O errors as they may be temporary
      log.warn("Transient S3 error reading properties file {} for blob {}: {}", attributePath, blobId, e.getMessage());

      // Propagate the IOException to indicate the file is temporarily unavailable
      throw e;
    }
    catch (IllegalArgumentException | IllegalStateException e) {
      // These exceptions indicate actual corruption (parsing errors, invalid data, etc.)
      // Do NOT delete here - let the repair task handle deletion
      log.warn("Corrupt properties file detected for blob {} at {}: {}", blobId, attributePath, e.getMessage());

      // Return null to allow reconciliation task to recreate
      return null;
    }
  }

  @Nullable
  @Override
  public BlobAttributes getBlobAttributesWithException(final BlobId blobId) throws BlobStoreException {
    try {
      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath(blobId));
      return blobAttributes.load() ? blobAttributes : null;
    }
    catch (Exception e) {
      log.error("Unable to load S3BlobAttributes for blob id: {}", blobId, e);
      throw new BlobStoreException(e, blobId);
    }
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
