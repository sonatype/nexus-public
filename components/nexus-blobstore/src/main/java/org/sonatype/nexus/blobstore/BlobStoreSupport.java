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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobSession;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.stateguard.Transitions;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobRef.DATE_TIME_PATH_FORMATTER;
import static org.sonatype.nexus.common.app.FeatureFlags.DATE_BASED_BLOBSTORE_LAYOUT_ENABLED_NAMED;
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
  public static final String CONTENT_TMP_PATH = "/content/tmp/";

  private final Map<String, Timer> timers = new ConcurrentHashMap<>();

  protected final PerformanceLogger performanceLogger = new PerformanceLogger();

  private MetricRegistry metricRegistry;

  protected final BlobIdLocationResolver blobIdLocationResolver;

  protected final DryRunPrefix dryRunPrefix;

  protected BlobStoreConfiguration blobStoreConfiguration;

  private static final Pattern UUID_PATTERN = Pattern.compile(
          ".*vol-\\d{2}[/\\\\]chap-\\d{2}[/\\\\]\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b.properties$",
          Pattern.CASE_INSENSITIVE);

  /**
   * To match "content/2024/01/10/18/13/0c89ccf4-ec5b-44a8-83b2-d08df2599c6e.properties"
   */
  private static final Pattern DATE_BASED_PATTERN = Pattern.compile(
      ".*(\\d{4}/\\d{2}/\\d{2}/\\d{2}/\\d{2})/(\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12})\\b.properties$",
      Pattern.CASE_INSENSITIVE);

  public static final int MAX_NAME_LENGTH = 255;

  public static final int MIN_NAME_LENGTH = 1;

  private boolean dateBasedLayoutEnabled;

  public BlobStoreSupport(final BlobIdLocationResolver blobIdLocationResolver,
                          final DryRunPrefix dryRunPrefix)
  {
    this.blobIdLocationResolver = checkNotNull(blobIdLocationResolver);
    this.dryRunPrefix = checkNotNull(dryRunPrefix);
  }

  @Inject
  public void setMetricRegistry(final MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  @Inject
  public void setDateBasedLayoutEnabled(
      @Named(DATE_BASED_BLOBSTORE_LAYOUT_ENABLED_NAMED) final boolean dateBasedLayoutEnabled) {
    this.dateBasedLayoutEnabled = dateBasedLayoutEnabled;
  }

  protected BlobId getBlobId(final Map<String, String> headers, @Nullable final BlobId blobId) {
    return Optional.ofNullable(blobId).orElseGet(() -> blobIdLocationResolver.fromHeaders(headers));
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
  public boolean undelete(@Nullable final BlobStoreUsageChecker inUseChecker, final BlobId blobId,
                          final BlobAttributes attributes,
                          final boolean isDryRun)
  {
    checkNotNull(attributes);
    String logPrefix = isDryRun ? dryRunPrefix.get() : "";
    Optional<String> blobName = Optional.of(attributes)
        .map(BlobAttributes::getProperties)
        .map(p -> p.getProperty(HEADER_PREFIX + BLOB_NAME_HEADER));
    if (!blobName.isPresent()) {
      log.error("Property not present: {}, for blob id: {}, at path: {}", HEADER_PREFIX + BLOB_NAME_HEADER,
          blobId, attributePathString(blobId)); // NOSONAR
      return false;
    }
    if (attributes.isDeleted() && inUseChecker != null && inUseChecker.test(this, blobId, blobName.get())) {
      String deletedReason = attributes.getDeletedReason();
      if (!isDryRun) {
        attributes.setDeleted(false);
        attributes.setDeletedReason(null);
        try {
          doUndelete(blobId, attributes);
          attributes.store();
        }
        catch (IOException e) {
          log.error("Error while un-deleting blob id: {}, deleted reason: {}, blob store: {}, blob name: {}",
              blobId, deletedReason, blobStoreConfiguration.getName(), blobName.get(), e);
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

  protected void doUndelete(final BlobId blobId, final BlobAttributes attributes) {
    // no-op
  }

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
  public synchronized void compact(@Nullable final BlobStoreUsageChecker inUseChecker) {
    long start = System.nanoTime();
    try {
      doCompact(inUseChecker);
    }
    finally {
      updateTimer("compact", System.nanoTime() - start);
    }
  }

  protected void doCompact(@Nullable final BlobStoreUsageChecker inUseChecker) {
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
  public void init(BlobStoreConfiguration configuration) {
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
    if (UUID_PATTERN.matcher(attributeFilePath.getFullPath()).matches()) {
      String filename = attributeFilePath.getFileName();
      String id = filename.substring(0, filename.length() - BLOB_FILE_ATTRIBUTES_SUFFIX.length());
      return new BlobId(id, null);
    }

    Matcher matcher = DATE_BASED_PATTERN.matcher(attributeFilePath.getFullPath());
    if (matcher.find()) {
      LocalDateTime localDateTime = LocalDateTime.parse(matcher.group(1), DATE_TIME_PATH_FORMATTER);
      OffsetDateTime blobCreatedRef = localDateTime.atOffset(ZoneOffset.UTC);
      return new BlobId(matcher.group(2), blobCreatedRef);
    }

    try {
      BlobAttributes fileBlobAttributes = getBlobAttributes(attributeFilePath);
      if (fileBlobAttributes != null && fileBlobAttributes.getHeaders() != null) {
        String id = blobIdLocationResolver.fromHeaders(fileBlobAttributes.getHeaders()).asUniqueString();
        return new BlobId(id, null);
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

  private void updateTimer(final String name, final long value) {
    if (metricRegistry != null) {
      Timer timer = timers.computeIfAbsent(name, key ->
          metricRegistry.timer(getClass().getName().replaceAll("\\$.*", "") + '.' + name + ".timer"));
      timer.update(value, TimeUnit.NANOSECONDS);
    }
  }

  @Override
  public boolean isEmpty() {
    return !getBlobIdStream().findAny().isPresent();
  }

  public boolean isDateBasedLayoutEnabled() {
    return dateBasedLayoutEnabled;
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
}
