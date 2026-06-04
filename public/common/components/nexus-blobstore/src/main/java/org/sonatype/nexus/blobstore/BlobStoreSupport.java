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
package org.sonatype.nexus.blobstore;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.DateBasedHelper.DateInterval;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobSession;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.metrics.MonitoringBlobStoreMetrics;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.stateguard.Transitions;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.common.time.DateHelper;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.LoadingCache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobRef.DATE_TIME_FORMATTER;
import static org.sonatype.nexus.blobstore.api.BlobRef.DATE_TIME_PATH_FORMATTER;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.SHUTDOWN;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Supports the implementation of {@link BlobStore}.
 *
 * @since 3.15
 */
public abstract class BlobStoreSupport<T extends AttributesLocation>
    extends StateGuardLifecycleSupport
    implements BlobStore
{
  public static final String CONTENT_PREFIX = "content";

  public static final String CONTENT_TMP_PATH = "/content/tmp/";

  private final Map<String, Timer> timers = new ConcurrentHashMap<>();

  protected final PerformanceLogger performanceLogger = new PerformanceLogger();

  private MetricRegistry metricRegistry;

  protected final BlobIdLocationResolver blobIdLocationResolver;

  protected final DryRunPrefix dryRunPrefix;

  protected BlobStoreConfiguration blobStoreConfiguration;

  protected LoadingCache<BlobId, BlobSupport> liveBlobs;

  private static final Pattern UUID_PATTERN = Pattern.compile(
      ".*vol-\\d{2}[/\\\\]chap-\\d{2}[/\\\\]\\b([0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b).(properties|bytes)$",
      Pattern.CASE_INSENSITIVE);

  /**
   * To match "content/2024/01/10/18/13/0c89ccf4-ec5b-44a8-83b2-d08df2599c6e.properties"
   */
  private static final Pattern DATE_BASED_PATTERN = Pattern.compile(
      ".*(\\d{4}([/\\\\]\\d{2}){4})[/\\\\]([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.(properties|bytes)$",
      Pattern.CASE_INSENSITIVE);

  public static final int MAX_NAME_LENGTH = 255;

  public static final int MIN_NAME_LENGTH = 1;

  private static final int DEFAULT_MAX_RETRIES = 1;

  private static final long DEFAULT_RETRY_DELAY_MS = 100;

  private int maxRetries;

  private long retryDelayMs;

  public BlobStoreSupport(
      final BlobIdLocationResolver blobIdLocationResolver,
      final DryRunPrefix dryRunPrefix)
  {
    this.blobIdLocationResolver = checkNotNull(blobIdLocationResolver);
    this.dryRunPrefix = checkNotNull(dryRunPrefix);
  }

  @Autowired
  public void setRetryConfiguration(
      @Value("${nexus.blobstore.get.maxRetries:" + DEFAULT_MAX_RETRIES + "}") final int maxRetries,
      @Value("${nexus.blobstore.get.retryDelayMs:" + DEFAULT_RETRY_DELAY_MS + "}") final long retryDelayMs)
  {
    this.maxRetries = maxRetries;
    this.retryDelayMs = retryDelayMs;
  }

  protected abstract BlobAttributes loadBlobAttributes(final BlobId blobId) throws IOException;

  @VisibleForTesting
  public Blob refreshBlob(final BlobSupport blob, final BlobId blobId, final boolean includeDeleted) {
    Lock lock = blob.lock();
    try {
      if (blob.isStale()) {
        BlobAttributes blobAttributes = loadBlobAttributes(blobId);
        if (blobAttributes == null) {
          if (log.isDebugEnabled()) {
            log.warn("Attempt to access non-existent blob {}", blobId);
          }
          return null;
        }

        if (blobAttributes.isDeleted() && !includeDeleted) {
          if (log.isDebugEnabled()) {
            log.warn("Attempt to access soft-deleted blob {} ({})", blobId, blobAttributes);
          }
          return null;
        }

        blob.refresh(blobAttributes.getHeaders(), blobAttributes.getMetrics());
      }
      return blob;
    }
    catch (IOException e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
    }
  }

  @Autowired
  public void setMetricRegistry(final MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  protected BlobId getBlobId(final Map<String, String> headers, @Nullable final BlobId blobId) {
    return Optional.ofNullable(blobId)
        .map(reused -> {
          // NEXUS-50201: if dateTime is provided, only reuse uuid and assign current time
          if (reused.getBlobCreatedRef() != null) {
            return new BlobId(reused.asUniqueString(), UTC.now());
          }
          return reused;
        })
        .orElseGet(() -> blobIdLocationResolver.fromHeaders(headers));
  }

  private void checkIsWritable() {
    checkState(isWritable(), "Operation not permitted when blob store is not writable");
  }

  @Override
  @Guarded(by = STARTED)
  public BlobSession<?> openSession() {
    return new MemoryBlobSession(this);
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final InputStream blobData, final Map<String, String> headers) {
    return create(blobData, headers, null);
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final InputStream blobData, final Map<String, String> headers, @Nullable final BlobId blobId) {
    checkNotNull(blobData);
    checkNotNull(headers);
    checkIsWritable();

    checkArgument(headers.containsKey(BLOB_NAME_HEADER), "Missing header: %s", BLOB_NAME_HEADER);
    checkArgument(headers.containsKey(CREATED_BY_HEADER), "Missing header: %s", CREATED_BY_HEADER);

    long start = System.nanoTime();
    Blob blob = null;
    try {
      blob = doCreate(blobData, headers, blobId);
    }
    finally {
      long elapsed = System.nanoTime() - start;
      updateTimer("create", elapsed);
      if (blob != null) {
        performanceLogger.logCreate(blob, elapsed);
      }
    }
    return blob;
  }

  protected abstract Blob doCreate(InputStream blobData, Map<String, String> headers, @Nullable BlobId blobId);

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobId blobId) {
    return get(blobId, false);
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  @Timed
  @MonitoringBlobStoreMetrics(operationType = DOWNLOAD)
  public Blob get(final BlobId blobId, final boolean includeDeleted) {
    return getWithRetries(blobId, includeDeleted);
  }

  private Blob getWithRetries(final BlobId blobId, final boolean includeDeleted) {
    // First attempt
    Blob blob = doGet(blobId, includeDeleted);
    if (blob != null) {
      return blob;
    }

    // Check if retries should be skipped for soft-deleted blobs
    if (shouldSkipRetries(blobId, includeDeleted)) {
      return null;
    }

    // Retry for non-deleted blobs or when includeDeleted is true
    return retryGetBlob(blobId, includeDeleted);
  }

  private boolean shouldSkipRetries(final BlobId blobId, final boolean includeDeleted) {
    return !includeDeleted && isBlobSoftDeleted(blobId);
  }

  private Blob retryGetBlob(final BlobId blobId, final boolean includeDeleted) {
    for (int attempt = 1; attempt <= this.maxRetries; attempt++) {
      log.debug("Failed to retrieve blob {} on attempt {}, retrying after {} ms", blobId, attempt, retryDelayMs);

      if (sleepBetweenRetries(blobId)) {
        break;
      }

      Blob blob = doGet(blobId, includeDeleted);
      if (blob != null) {
        return blob;
      }
    }

    log.debug("Failed to retrieve blob {} after {} attempts", blobId, maxRetries);
    return null;
  }

  private boolean sleepBetweenRetries(final BlobId blobId) {
    try {
      Thread.sleep(this.retryDelayMs);
      return false;
    }
    catch (InterruptedException e) {
      if (log.isDebugEnabled()) {
        log.warn("Thread interrupted while waiting to retry getting blob {}", blobId);
      }
      Thread.currentThread().interrupt();
      return true;
    }
  }

  private boolean isBlobSoftDeleted(final BlobId blobId) {
    try {
      BlobAttributes blobAttributes = loadBlobAttributes(blobId);
      return blobAttributes != null && blobAttributes.isDeleted();
    }
    catch (IOException e) {
      // If we can't load attributes, assume it's not just soft-deleted
      return false;
    }
  }

  protected final Blob doGet(final BlobId blobId, final boolean includeDeleted) {
    checkNotNull(blobId);
    final BlobSupport blob = liveBlobs.getUnchecked(blobId);
    if (blob.isStale()) {
      return refreshBlob(blob, blobId, includeDeleted);
    }
    return blob;
  }

  @Override
  @Guarded(by = STARTED)
  public boolean delete(final BlobId blobId, final String reason) {
    checkNotNull(blobId);

    long start = System.nanoTime();
    try {
      return doDelete(blobId, reason);
    }
    finally {
      long elapsed = System.nanoTime() - start;
      updateTimer("delete", elapsed);
      performanceLogger.logDelete(elapsed);
    }
  }

  protected abstract boolean doDelete(BlobId blobId, String reason);

  @Override
  @Guarded(by = STARTED)
  public boolean undelete(
      @Nullable final BlobStoreUsageChecker inUseChecker,
      final BlobId blobId,
      final BlobAttributes attributes,
      final boolean isDryRun)
  {
    checkNotNull(attributes);
    String logPrefix = isDryRun ? dryRunPrefix.get() : "";
    Optional<String> blobName = Optional.of(attributes)
        .map(BlobAttributes::getProperties)
        .map(p -> p.getProperty(HEADER_PREFIX + BLOB_NAME_HEADER));
    if (!blobName.isPresent()) {
      log.error("Property not present: {}, for blob id: {}, at path: {}", HEADER_PREFIX + BLOB_NAME_HEADER, blobId,
          attributePathString(blobId)); // NOSONAR
      return false;
    }
    Optional<String> originalLocation = attributes.getOriginalLocation();
    if (originalLocation.isPresent()) {
      log.debug("Undelete invoked with copied attributes. Retrying with original location: {}",
          originalLocation.get());
      BlobId originalBlobId = createBlobIdForTimePath(blobId, originalLocation.get());
      // Prevent self-referential recursion by ensuring the originalBlobId differs from the current blobId.
      if (!blobId.equals(originalBlobId)) {
        return undelete(inUseChecker, originalBlobId, getBlobAttributes(originalBlobId), isDryRun);
      }
    }

    if (attributes.isDeleted() && inUseChecker != null && inUseChecker.test(this, blobId, blobName.get())) {
      String deletedReason = attributes.getDeletedReason();
      if (!isDryRun) {
        try {
          Optional<String> softDeletedLocation = attributes.getSoftDeletedLocation();
          // Remove deletion flag and persist
          attributes.setDeleted(false);
          attributes.setDeletedReason(null);
          attributes.setSoftDeletedLocation(null);
          doUndelete(blobId, attributes);
          attributes.store();

          // Remove copied soft-deleted attributes
          softDeletedLocation.ifPresent(location -> deleteCopiedAttributes(blobId, location));
        }
        catch (IOException e) {
          log.error("Error while un-deleting blob id: {}, deleted reason: {}, blob store: {}, blob name: {}", blobId,
              deletedReason, blobStoreConfiguration.getName(), blobName, e);
        }
      }
      log.warn(
          "{}Soft-deleted blob still in use, un-deleting blob id: {}, deleted reason: {}, blob store: {}, blob name: {}",
          logPrefix, blobId, deletedReason, blobStoreConfiguration.getName(), blobName.get());
      return true;
    }
    return false;
  }

  protected abstract String attributePathString(BlobId blobId);

  /**
   * Invoked during an undelete activity before the properties file is stored. This allows implementations to add
   * necessary book keeping.
   */
  protected void doUndelete(final BlobId blobId, final BlobAttributes attributes) {
    // no-op
  }

  /**
   * Deletes the underlying attributes file, this should only be used to remove duplicate attributes
   */
  protected abstract void deleteCopiedAttributes(BlobId blobId, String softDeletedLocation);

  @Override
  @Guarded(by = STARTED)
  public boolean deleteHard(final BlobId blobId) {
    checkNotNull(blobId);

    long start = System.nanoTime();
    try {
      return doDeleteHard(blobId);
    }
    finally {
      updateTimer("deleteHard", System.nanoTime() - start);
    }
  }

  protected abstract boolean doDeleteHard(final BlobId blobId);

  @Override
  @Guarded(by = STARTED)
  public synchronized void compact(@Nullable final BlobStoreUsageChecker inUseChecker, final Duration blobsOlderThan) {
    long start = System.nanoTime();
    try {
      doCompact(inUseChecker, blobsOlderThan);
    }
    finally {
      updateTimer("compact", System.nanoTime() - start);
    }
  }

  protected void doCompact(@Nullable final BlobStoreUsageChecker inUseChecker, final Duration blobsOlderThan) {
    // no-op
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void deleteTempFiles(final Integer daysOlderThan) {
    long start = System.nanoTime();
    try {
      doDeleteTempFiles(daysOlderThan);
    }
    finally {
      updateTimer("delete temp files", System.nanoTime() - start);
    }
  }

  protected void doDeleteTempFiles(final Integer daysOlderThan) {
    // no-op
  }

  @Override
  public void init(final BlobStoreConfiguration configuration) {
    this.blobStoreConfiguration = configuration;
    this.performanceLogger.setBlobStoreName(configuration.getName());
    doInit(this.blobStoreConfiguration);
  }

  protected abstract void doInit(BlobStoreConfiguration configuration);

  @Override
  public BlobStoreConfiguration getBlobStoreConfiguration() {
    return this.blobStoreConfiguration;
  }

  protected abstract BlobAttributes getBlobAttributes(final T attributesFilePath) throws IOException;

  protected BlobId getBlobIdFromAttributeFilePath(final T attributeFilePath) {
    Matcher matcher = UUID_PATTERN.matcher(attributeFilePath.getFullPath());
    if (matcher.find()) {
      return new BlobId(matcher.group(1), null, replaceBytesName(attributeFilePath));
    }

    matcher = DATE_BASED_PATTERN.matcher(attributeFilePath.getFullPath());
    if (matcher.find()) {
      LocalDateTime localDateTime = LocalDateTime.parse(matcher.group(1).replace("\\", "/"), DATE_TIME_PATH_FORMATTER);
      OffsetDateTime blobCreatedRef = localDateTime.atOffset(ZoneOffset.UTC);
      String id = matcher.group(3);
      return new BlobId(id, blobCreatedRef, replaceBytesName(attributeFilePath));
    }

    try {
      BlobAttributes fileBlobAttributes = getBlobAttributes(attributeFilePath);
      if (fileBlobAttributes != null && fileBlobAttributes.getHeaders() != null) {
        String id = blobIdLocationResolver.fromHeaders(fileBlobAttributes.getHeaders()).asUniqueString();
        return new BlobId(id, null, getBlobstorePath(id));
      }
      else {
        log.error("Broken properties file by path: {}", attributeFilePath.getFullPath());
        return null;
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String replaceBytesName(final T attributeFilePath) {
    return attributeFilePath.getFullPath().replace(".bytes", "");
  }

  private void updateTimer(final String name, final long value) {
    if (metricRegistry != null) {
      Timer timer = timers.computeIfAbsent(name,
          key -> metricRegistry.timer(getClass().getName().replaceAll("\\$.*", "") + '.' + name + ".timer"));
      timer.update(value, TimeUnit.NANOSECONDS);
    }
  }

  @Override
  public boolean isEmpty() {
    return !getBlobIdStream().findAny().isPresent();
  }

  /**
   * Permanently stops this blob store regardless of the current state, disallowing restarts.
   */
  @Override
  @Transitions(to = SHUTDOWN)
  public void shutdown() throws Exception {
    if (isStarted()) {
      doStop();
    }
  }

  /**
   * @return if the blobId uses a date, returns formatted via dateTime,
   *         or an empty string if the blobId does not reference a date
   */
  protected static String getLocationPrefix(final BlobId blobId) {
    return Optional.ofNullable(blobId.getBlobCreatedRef())
        .map(time -> time.format(DATE_TIME_FORMATTER))
        .orElse("");
  }

  /**
   * For a blob, create a BlobId which represents the location of an attributes file relocated to a different timestamp.
   * An empty location string will result in a BlobId without a timestamp (i.e. old style layout)
   *
   * @param blobId used to provided the unique part of the blob's identifier
   * @param locationTimestamp a DATE_TIME_FORMATTER string for the soft-deleted location
   * @return an optional containing the BobId if getSoftDeletedLocation provides a value
   */
  protected BlobId createBlobIdForTimePath(final BlobId blobId, final String locationTimestamp) {
    if (Strings2.isBlank(locationTimestamp)) {
      return new BlobId(blobId.asUniqueString());
    }

    LocalDateTime localDateTime = LocalDateTime.parse(locationTimestamp, DATE_TIME_FORMATTER);
    OffsetDateTime softDeletedDateTime = localDateTime.atOffset(ZoneOffset.UTC);
    return new BlobId(blobId.asUniqueString(), softDeletedDateTime);
  }

  /**
   * Read the attributes from the provided location and check to see if it creation date is within the provided
   * interval.
   *
   * @return {@code true} if the creation date is in range, if the attributes cannot be read, or if an exception occurs.
   */
  protected boolean isBlobCreatedInRange(final Blob blob, final DateInterval interval) {
    try {
      return Optional.ofNullable(blob)
          .map(Blob::getMetrics)
          .map(BlobMetrics::getCreationTime)
          .map(DateHelper::toOffsetDateTime)
          .map(interval::inRange)
          .orElse(true);
    }
    catch (Exception e) {
      log.debug("Failed to read attributes for path {}", blob, e);
      return true;
    }
  }

  /**
   * @return {@code false} if blob's headers includes the temporary blob header, true otherwise
   */
  protected boolean isNonTempBlob(final Blob blob) {
    return !isTempBlob(blob);
  }

  /**
   * @return {@code true} if blob's headers includes the temporary blob header, false otherwise
   */
  protected boolean isTempBlob(final Blob blob) {
    try {
      return Optional.ofNullable(blob)
          .map(Blob::getHeaders)
          .map(headers -> headers.containsKey(TEMPORARY_BLOB_HEADER))
          // for data recovery missing data is taken to indicate this is not a temp blob
          .orElse(false);
    }
    catch (Exception e) {
      log.debug("Failed to read attributes for path {}", blob, e);
      // For data recovery if we failed to read the file we should consider an error to indicate this is not a
      // temp blob
      return false;
    }
  }

  /**
   * @return {@code true} when the BlobId has a date, and the date is in range, or if the blobId has no date.
   */
  protected static boolean blobIdInRange(BlobId blobId, DateInterval interval) {
    return Optional.ofNullable(blobId)
        .map(BlobId::getBlobCreatedRef)
        .map(interval::inRange)
        .orElse(true);
  }

  /**
   * @return true when the path is a properties file or a bytes file, false otherwise
   */
  protected static boolean isBlobStoreContent(final String path) {
    return path.endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX) || path.endsWith(BLOB_FILE_CONTENT_SUFFIX);
  }

  protected String getBlobstorePath(final String id) {
    BlobId blobId = new BlobId(id, null);
    return this.blobIdLocationResolver.getLocation(blobId);
  }
}
