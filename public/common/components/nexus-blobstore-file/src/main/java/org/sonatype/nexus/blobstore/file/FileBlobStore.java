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
package org.sonatype.nexus.blobstore.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.BlobIdLocationResolver;
import org.sonatype.nexus.blobstore.BlobStoreReconciliationLogger;
import org.sonatype.nexus.blobstore.BlobStoreSupport;
import org.sonatype.nexus.blobstore.BlobSupport;
import org.sonatype.nexus.blobstore.DateBasedHelper.DateInterval;
import org.sonatype.nexus.blobstore.StreamMetrics;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsService;
import org.sonatype.nexus.blobstore.file.internal.BlobCollisionException;
import org.sonatype.nexus.blobstore.file.internal.FileOperations;
import org.sonatype.nexus.blobstore.metrics.MonitoringBlobStoreMetrics;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaUsageChecker;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.DirectoryHelper;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.cache.CacheLoader.from;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver.TEMPORARY_BLOB_ID_PREFIX;
import static org.sonatype.nexus.blobstore.DirectPathLocationStrategy.DIRECT_PATH_ROOT;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.SHUTDOWN;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STOPPED;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * A {@link BlobStore} that stores its content on the file system.
 *
 * @since 3.0
 */
@Component
@Qualifier(FileBlobStore.TYPE)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FileBlobStore
    extends BlobStoreSupport<FileAttributesLocation>
{
  public static final String BASEDIR = "blobs";

  public static final String TYPE = "File";

  public static final String CONFIG_KEY = "file";

  public static final String PATH_KEY = "path";

  @VisibleForTesting
  public static final String METADATA_FILENAME = "metadata.properties";

  @VisibleForTesting
  public static final String TYPE_KEY = "type";

  @VisibleForTesting
  public static final String TYPE_V1 = "file/1";

  @VisibleForTesting
  public static final String REBUILD_DELETED_BLOB_INDEX_KEY = "rebuildDeletedBlobIndex";

  @VisibleForTesting
  public static final String DELETIONS_FILENAME = "deletions.index";

  public static final String TMP = "tmp";

  private static final boolean RETRY_ON_COLLISION =
      SystemPropertiesHelper.getBoolean("nexus.blobstore.retryOnCollision", true);

  @VisibleForTesting
  static final int MAX_COLLISION_RETRIES = 8;

  private static final int INTERVAL_IN_SECONDS = 60;

  private static final String NATIVE_CONTENT_TMP_PATH = CONTENT_TMP_PATH.replace('/', File.separatorChar);

  public static final String ATTRIBUTES_FOR_BLOB_ID_EXCEPTION =
      "Unable to load BlobAttributes for blob id: {}, path: {}, exception: {} - {}";

  private Path contentDir;

  private Path reconciliationLogDir;

  private final FileOperations fileOperations;

  private final ApplicationDirectories applicationDirectories;

  private final DirectoryHelper directoryHelper;

  private Path basedir;

  private BlobStoreMetricsService<FileBlobStore> metricsService;

  private final FileBlobDeletionIndex blobDeletionIndex;

  private final NodeAccess nodeAccess;

  private boolean supportsHardLinkCopy;

  private boolean supportsAtomicMove;

  private final BlobStoreReconciliationLogger reconciliationLogger;

  private final long pruneEmptyDirectoryAge;

  private final BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker;

  private int blobAttributesMaxRetries;

  private int blobAttributesRetryDelayMs;

  @Inject
  public FileBlobStore(
      final BlobIdLocationResolver blobIdLocationResolver,
      final FileOperations fileOperations,
      final ApplicationDirectories applicationDirectories,
      final DirectoryHelper directoryHelper,
      @Qualifier(FileBlobStore.TYPE) final BlobStoreMetricsService<FileBlobStore> metricsService,
      final NodeAccess nodeAccess,
      final DryRunPrefix dryRunPrefix,
      final BlobStoreReconciliationLogger reconciliationLogger,
      @Value("${nexus.blobstore.prune.empty.directory.age.ms:86400000}") final long pruneEmptyDirectoryAge,
      final BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker,
      final FileBlobDeletionIndex blobDeletionIndex)
  {
    super(blobIdLocationResolver, dryRunPrefix);
    this.fileOperations = checkNotNull(fileOperations);
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.directoryHelper = checkNotNull(directoryHelper);
    this.metricsService = checkNotNull(metricsService);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.supportsHardLinkCopy = true;
    this.supportsAtomicMove = true;
    this.reconciliationLogger = checkNotNull(reconciliationLogger);
    this.pruneEmptyDirectoryAge = pruneEmptyDirectoryAge;
    this.blobStoreQuotaUsageChecker = checkNotNull(blobStoreQuotaUsageChecker);
    this.blobDeletionIndex = checkNotNull(blobDeletionIndex);
  }

  @VisibleForTesting
  public FileBlobStore(
      final Path contentDir, // NOSONAR
      final BlobIdLocationResolver blobIdLocationResolver,
      final FileOperations fileOperations,
      final ApplicationDirectories directories,
      final DirectoryHelper directoryHelper,
      final BlobStoreMetricsService<FileBlobStore> metricsService,
      final BlobStoreConfiguration configuration,
      final NodeAccess nodeAccess,
      final DryRunPrefix dryRunPrefix,
      final BlobStoreReconciliationLogger reconciliationLogger,
      final long pruneEmptyDirectoryAge,
      final BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker,
      final FileBlobDeletionIndex blobDeletionIndex)

  {
    this(blobIdLocationResolver, fileOperations, directories, directoryHelper, metricsService, nodeAccess, dryRunPrefix,
        reconciliationLogger, pruneEmptyDirectoryAge, blobStoreQuotaUsageChecker, blobDeletionIndex);
    this.contentDir = checkNotNull(contentDir);
    this.blobStoreConfiguration = checkNotNull(configuration);
  }

  @Override
  protected void doStart() throws Exception {
    Path storageDir = getAbsoluteBlobDir();

    // ensure blobstore is supported
    PropertiesFile metadata = new PropertiesFile(storageDir.resolve(METADATA_FILENAME).toFile());
    if (metadata.getFile().exists()) {
      metadata.load();
      String type = metadata.getProperty(TYPE_KEY);
      checkState(TYPE_V1.equals(type), "Unsupported blob store type/version: %s in %s", type, metadata.getFile());
    }
    else {
      // assumes new blobstore, write out type
      metadata.setProperty(TYPE_KEY, TYPE_V1);
      metadata.store();
    }
    liveBlobs = CacheBuilder.newBuilder().weakValues().build(from(FileBlob::new));
    blobDeletionIndex.initIndex(metadata, this);
    metricsService.init(this);

    blobStoreQuotaUsageChecker.setBlobStore(this);
    blobStoreQuotaUsageChecker.start();
  }

  /*
   * Returns a Stream of known deletion index files for the blobstore, this will include entries for other nodes and is
   * only intended for use during database migration.
   */
  public Stream<File> getDeletionIndexFiles() throws IOException {
    Path blobDir = getAbsoluteBlobDir();
    Set<Path> deletionsIndexFiles = new HashSet<>();

    // Collect legacy file
    Path legacyPath = blobDir.resolve(DELETIONS_FILENAME);
    if (Files.exists(legacyPath)) {
      deletionsIndexFiles.add(legacyPath);
    }

    // Collect node specific deletion index files
    try (DirectoryStream<Path> deletionsFileStream = Files.newDirectoryStream(blobDir, "*" + DELETIONS_FILENAME)) {
      deletionsFileStream.forEach(deletionsIndexFiles::add);
    }

    log.debug("Found the following deletion index files: {}", deletionsIndexFiles);

    return deletionsIndexFiles.stream()
        .map(Path::toFile);
  }

  @Override
  protected void doStop() throws Exception {
    liveBlobs = null;
    try {
      blobDeletionIndex.stopIndex();
    }
    finally {
      metricsService.stop();
      blobStoreQuotaUsageChecker.stop();
    }
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  @Timed
  @MonitoringBlobStoreMetrics(operationType = DOWNLOAD)
  public Blob get(final BlobId blobId, final boolean includeDeleted) {
    checkNotNull(blobId);

    Path blobPath = contentPath(blobId);
    if (!Files.exists(blobPath)) {
      log.debug("Blob {} not found in file blob store {}, returning null for fallback",
          blobId, getBlobStoreConfiguration().getName());
      return null;
    }

    // File exists, proceed with normal flow
    return super.get(blobId, includeDeleted);
  }

  /**
   * Returns path for blob-id content file relative to root directory.
   */
  @VisibleForTesting
  Path contentPath(final BlobId id) {
    return contentDir.resolve(blobIdLocationResolver.getLocation(id) + BLOB_FILE_CONTENT_SUFFIX);
  }

  /**
   * Returns path for blob-id attribute file relative to root directory.
   */
  @VisibleForTesting
  Path attributePath(final BlobId id) {
    return contentDir.resolve(blobIdLocationResolver.getLocation(id) + BLOB_FILE_ATTRIBUTES_SUFFIX);
  }

  @Override
  protected String attributePathString(final BlobId blobId) {
    return attributePath(blobId).toString();
  }

  /**
   * Returns a path for a temporary blob-id content file relative to root directory.
   */
  private Path temporaryContentPath(final BlobId id, final UUID suffix) {
    return contentDir.resolve(
        blobIdLocationResolver.getTemporaryLocation(id) + "." + suffix + BLOB_FILE_CONTENT_SUFFIX);
  }

  /**
   * Returns path for a temporary blob-id attribute file relative to root directory.
   */
  private Path temporaryAttributePath(final BlobId id, final UUID suffix) {
    return contentDir.resolve(blobIdLocationResolver.getTemporaryLocation(id) + "." + suffix +
        BLOB_FILE_ATTRIBUTES_SUFFIX);
  }

  @Override
  @MonitoringBlobStoreMetrics(operationType = UPLOAD)
  protected Blob doCreate(
      final InputStream blobData,
      final Map<String, String> headers,
      @Nullable final BlobId blobId)
  {
    return create(headers, destination -> fileOperations.create(destination, blobData), blobId);
  }

  @Override
  @Guarded(by = STARTED)
  @MonitoringBlobStoreMetrics(operationType = UPLOAD)
  public Blob create(final Path sourceFile, final Map<String, String> headers, final long size, final HashCode sha1) {
    checkNotNull(sourceFile);
    checkNotNull(sha1);
    checkArgument(Files.exists(sourceFile));

    return create(headers, destination -> {
      fileOperations.hardLink(sourceFile, destination);
      return new StreamMetrics(size, sha1.toString());
    }, null);
  }

  @Override
  public void createBlobAttributes(
      final BlobId blobId,
      final Map<String, String> headers,
      final BlobMetrics blobMetrics)
  {
    Path attributePath = attributePath(blobId);
    try {
      FileBlobAttributes blobAttributes = new FileBlobAttributes(attributePath, headers, blobMetrics);
      blobAttributes.store();
    }
    catch (Exception e) {
      // Something went wrong, clean up the file we created
      fileOperations.deleteQuietly(attributePath);
      throw new BlobStoreException(e, blobId);
    }
  }

  @Override
  public FileBlobAttributes createBlobAttributesInstance(
      final BlobId blobId,
      final Map<String, String> headers,
      final BlobMetrics blobMetrics)
  {
    return new FileBlobAttributes(attributePath(blobId), headers, blobMetrics);
  }

  private Blob create(final Map<String, String> headers, final BlobIngester ingester, final BlobId blobId) {
    for (int retries = 0; retries <= MAX_COLLISION_RETRIES; retries++) {
      try {
        Blob blob = tryCreate(headers, ingester, blobId);
        reconciliationLogger.logBlobCreated(reconciliationLogDir, blob.getId());
        return blob;
      }
      catch (BlobCollisionException e) { // NOSONAR
        log.warn("BlobId collision: {} already exists{}", e.getBlobId(),
            retries < MAX_COLLISION_RETRIES ? ", retrying with new BlobId" : "!");
      }
    }
    throw new BlobStoreException("Cannot find free BlobId", null);
  }

  /**
   * Creates a new blob, or re-uses an existing one if the {@code reusedBlobId} is not null.
   *
   * @param headers the headers for the blob
   * @param ingester the ingester to use for writing the blob content
   * @param reusedBlobId the blob id to re-use, or null to create a new one
   * @return the created or re-used blob
   * @throws BlobStoreException if an error occurs during blob creation
   */
  private Blob tryCreate(
      final Map<String, String> headers,
      final BlobIngester ingester,
      final BlobId reusedBlobId)
  { // NOSONAR
    final BlobId blobId = getBlobId(headers, reusedBlobId);
    final boolean isDirectPath = Boolean.parseBoolean(headers.getOrDefault(DIRECT_PATH_BLOB_HEADER, "false"));
    final Long existingSize = isDirectPath && exists(blobId) ? getContentSizeForDeletion(blobId) : null;

    final Path blobPath = contentPath(blobId);
    final Path attributePath = attributePath(blobId);

    final UUID uuidSuffix = UUID.randomUUID();
    final Path temporaryBlobPath = temporaryContentPath(blobId, uuidSuffix);
    final Path temporaryAttributePath = temporaryAttributePath(blobId, uuidSuffix);

    final BlobSupport blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      final boolean wouldCollide = fileOperations.exists(blobPath);

      if (reusedBlobId == null && RETRY_ON_COLLISION && wouldCollide && !isDirectPath) {
        throw new BlobCollisionException(blobId);
      }
      try {
        log.debug("Writing blob {} to {}", blobId, blobPath);

        final StreamMetrics streamMetrics = ingester.ingestTo(temporaryBlobPath);
        final BlobMetrics metrics = new BlobMetrics(new DateTime(), streamMetrics.getSha1(), streamMetrics.getSize());
        blob.refresh(headers, metrics);

        // Write the blob attribute file
        FileBlobAttributes blobAttributes = new FileBlobAttributes(temporaryAttributePath, headers, metrics);
        blobAttributes.store();

        // Move the temporary files into their final location
        // existing size being not-null also implies isDirectPath is true
        if (existingSize != null) {
          overwrite(temporaryBlobPath, blobPath);
          overwrite(temporaryAttributePath, attributePath);
          metricsService.recordDeletion(existingSize);
        }
        else {
          move(temporaryBlobPath, blobPath);
          move(temporaryAttributePath, attributePath);
        }

        metricsService.recordAddition(blobAttributes.getMetrics().getContentSize());

        return blob;
      }
      catch (Exception e) {
        // Something went wrong, clean up the files we created
        fileOperations.deleteQuietly(temporaryAttributePath);
        fileOperations.deleteQuietly(temporaryBlobPath);
        fileOperations.deleteQuietly(attributePath);
        fileOperations.deleteQuietly(blobPath);
        throw new BlobStoreException(e, blobId);
      }
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
    Path sourcePath = contentPath(sourceBlob.getId());
    if (supportsHardLinkCopy) {
      try {
        return create(headers, destination -> {
          fileOperations.hardLink(sourcePath, destination);
          BlobMetrics metrics = sourceBlob.getMetrics();
          return new StreamMetrics(metrics.getContentSize(), metrics.getSha1Hash());
        }, null);
      }
      catch (BlobStoreException e) {
        supportsHardLinkCopy = false;
        log.trace("Disabling copy by hard link for blob store {}, could not hard link blob {}",
            blobStoreConfiguration.getName(), sourceBlob.getId(), e);
      }
    }
    log.trace("Using fallback mechanism for blob store {}, copying blob {}", blobStoreConfiguration.getName(),
        sourceBlob.getId());
    return create(headers, destination -> {
      fileOperations.copy(sourcePath, destination);
      BlobMetrics metrics = sourceBlob.getMetrics();
      return new StreamMetrics(metrics.getContentSize(), metrics.getSha1Hash());
    }, null);
  }

  @Override
  public boolean isInternalMoveSupported(final BlobStore destBlobStore) {
    return false;
  }

  @Override
  public boolean isOwner(final Blob blob) {
    return blob instanceof FileBlob fileBlob && fileBlob.owner() == this;
  }

  @Override
  public Blob moveInternal(final BlobStore destBlobStore, final BlobId blobId, final Map<String, String> headers) {
    throw new UnsupportedOperationException("Internal move operation is not supported.");
  }

  @Override
  protected boolean doDelete(final BlobId blobId, final String reason) {
    final BlobSupport blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      log.debug("Soft deleting blob {}", blobId);

      FileBlobAttributes blobAttributes = getFileBlobAttributes(blobId);

      if (blobAttributes == null) {
        // This could happen under some concurrent situations (two threads try to delete the same blob)
        // but it can also occur if the deleted index refers to a manually-deleted blob.
        log.warn("Attempt to mark-for-delete non-existent blob {}, hard deleting instead", blobId);
        return deleteHard(blobId);
      }
      else if (blobAttributes.isDeleted()) {
        log.debug("Attempt to delete already-deleted blob {}", blobId);
        return true;
      }

      BlobId propRef = new BlobId(blobId.asUniqueString(), UTC.now());
      String softDeletedPrefixLocation = getLocationPrefix(propRef);
      Path path = attributePath(propRef);
      DateTime deletedDateTime = new DateTime();
      blobAttributes.setDeletedDateTime(deletedDateTime);
      blobAttributes.setSoftDeletedLocation(softDeletedPrefixLocation);

      // Save properties file under the new location
      String originalPrefixLocation = getLocationPrefix(blobId);
      if (!originalPrefixLocation.equals(softDeletedPrefixLocation)) {
        FileBlobAttributes newBlobAttributes = getFileBlobAttributes(path);
        newBlobAttributes.updateFrom(blobAttributes);
        newBlobAttributes.setOriginalLocation(getLocationPrefix(blobId));
        newBlobAttributes.store();
      }

      blobAttributes.setDeleted(true);
      blobAttributes.setDeletedReason(reason);
      blobAttributes.store();

      // record blob for hard-deletion when the next compact task runs
      blobDeletionIndex.createRecord(blobId);
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
    try {
      log.debug("Hard deleting blob {}", blobId);

      // look for a softDeletedLocation from is blob's attributes, and if present, delete it first
      FileBlobAttributes attributes = getFileBlobAttributes(blobId);
      Optional.ofNullable(attributes).ifPresent(attr -> {
        Optional<String> softDeletedLocation = attr.getSoftDeletedLocation();
        // Remove copied soft-deleted attributes
        softDeletedLocation.ifPresent(location -> deleteCopiedAttributes(blobId, location));
      });

      Path attributePath = attributePath(blobId);
      Long contentSize = null;
      // attributes may still be null here if the file was removed from the filesystem out of band, e.g data loss
      if (attributes != null && attributes.getMetrics() != null) {
        contentSize = attributes.getMetrics().getContentSize();
      }

      Path blobPath = contentPath(blobId);

      boolean blobDeleted = delete(blobPath);
      delete(attributePath);

      if (blobDeleted && contentSize != null) {
        metricsService.recordDeletion(contentSize);
      }

      return blobDeleted;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
      liveBlobs.invalidate(blobId);
    }
  }

  @Nullable
  private Long getContentSizeForDeletion(final BlobId blobId) {
    return ofNullable(getFileBlobAttributes(blobId))
        .map(BlobAttributes::getMetrics)
        .map(BlobMetrics::getContentSize)
        .orElse(null);
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStoreMetricsService<FileBlobStore> getMetricsService() {
    return metricsService;
  }

  @Override
  @Guarded(by = STARTED)
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
  protected void doCompact(@Nullable final BlobStoreUsageChecker inUseChecker, final Duration blobsOlderThan) {
    try {
      PropertiesFile metadata = new PropertiesFile(getAbsoluteBlobDir().resolve(METADATA_FILENAME).toFile());
      metadata.load();
      boolean deletedBlobIndexRebuildRequired =
          Boolean.parseBoolean(metadata.getProperty(REBUILD_DELETED_BLOB_INDEX_KEY, "false"));

      if (deletedBlobIndexRebuildRequired) {
        // this is a multi node task, i.e. it will run on all nodes simultaneously, so make sure walking the blobstore
        // is only done on one node
        if (!nodeAccess.isOldestNode()) {
          log.info("Skipping compact without deleted blob index on this node because this is not the oldest node.");
          return;
        }

        doCompactWithoutDeletedBlobIndex(inUseChecker);

        metadata.remove(REBUILD_DELETED_BLOB_INDEX_KEY);
        metadata.store();
      }
      else {
        doCompactWithDeletedBlobIndex(inUseChecker, blobsOlderThan);
      }
    }
    catch (BlobStoreException | TaskInterruptedException e) {
      throw e;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, null);
    }
  }

  @Override
  protected void doDeleteTempFiles(final Integer daysOlderThan) {
    try {
      Date thresholdDate = DateUtils.addDays(new Date(), -daysOlderThan);
      AgeFileFilter ageFileFilter = new AgeFileFilter(thresholdDate);
      Iterator<File> filesToDelete =
          iterateFiles(getAbsoluteBlobDir().resolve(CONTENT_PREFIX).resolve(TMP).toFile(), ageFileFilter,
              ageFileFilter);
      filesToDelete.forEachRemaining(f -> {
        try {
          forceDelete(f);
        }
        catch (UncheckedIOException | IOException e) {
          log.error("Unable to delete temp file {}. Message was {}.", f, e.getMessage());
        }
      });
    }
    catch (UncheckedIOException | NoSuchFileException e) {
      log.debug("Tmp folder is empty: {}", e.getMessage());
    }
    catch (TaskInterruptedException e) {
      throw e;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, null);
    }
  }

  private boolean maybeCompactBlob(@Nullable final BlobStoreUsageChecker inUseChecker, final BlobId blobId) {
    Optional<FileBlobAttributes> attributesOption = ofNullable((FileBlobAttributes) getBlobAttributes(blobId));
    if (!attributesOption.isPresent() || !undelete(inUseChecker, blobId, attributesOption.get(), false)) {
      // attributes file is missing or blob id not in use, so it's safe to delete the file
      log.debug("Hard deleting blob id: {}, in blob store: {}", blobId, blobStoreConfiguration.getName());
      return deleteHard(blobId);
    }
    return false;
  }

  @Override
  public boolean isStorageAvailable() {
    try {
      FileStore fileStore = Files.getFileStore(contentDir);
      long usableSpace = fileStore.getUsableSpace();
      boolean readOnly = fileStore.isReadOnly();
      boolean result = !readOnly && usableSpace > 0;
      if (!result) {
        log.warn("File blob store '{}' is not writable. Read only: {}. Usable space: {}",
            getBlobStoreConfiguration().getName(), readOnly, usableSpace);
      }
      return result;
    }
    catch (IOException e) {
      log.warn("File blob store '{}' is not writable.", getBlobStoreConfiguration().getName(), e);
      return false;
    }
  }

  @Override
  protected void doInit(final BlobStoreConfiguration configuration) {
    try {
      this.basedir = applicationDirectories.getWorkDirectory(BASEDIR).toPath();
    }
    catch (Exception e) {
      log.error("Unable to access file blob store base directory: " + BASEDIR, e);
    }

    try {
      Path blobDir = getAbsoluteBlobDir();
      Path content = blobDir.resolve(CONTENT_PREFIX);
      directoryHelper.mkdir(content);
      this.contentDir = content;
      Path reconciliationLogDir = blobDir.resolve("reconciliation");
      directoryHelper.mkdir(reconciliationLogDir);
      this.reconciliationLogDir = reconciliationLogDir;

      setConfiguredBlobStorePath(getRelativeBlobDir());
    }
    catch (Exception e) {
      throw new BlobStoreException(
          "Unable to initialize blob store directory structure: " + getConfiguredBlobStorePath(), e, null);
    }

    this.blobAttributesMaxRetries = SystemPropertiesHelper.getInteger(
        "nexus.blobstore.setBlobAttributes.maxRetries", 3);
    this.blobAttributesRetryDelayMs = SystemPropertiesHelper.getInteger(
        "nexus.blobstore.setBlobAttributes.retryDelayMs", 100);
  }

  private void checkExists(final Path path, final BlobId blobId) throws IOException {
    if (!fileOperations.exists(path)) {
      // I'm not completely happy with this, since it means that blob store clients can get a blob, be satisfied
      // that it exists, and then discover that it doesn't, mid-operation
      log.warn("Can't open input stream to blob {} as file {} not found", blobId, path);
      throw new BlobStoreException("Blob has been deleted", blobId);
    }
  }

  /**
   * This is a simple existence check resulting from NEXUS-16729. This allows clients to perform a simple check and is
   * primarily intended for use in directpath scenarios.
   */
  @Override
  public boolean exists(final BlobId blobId) {
    checkNotNull(blobId);
    if (!fileOperations.exists(attributePath(blobId))) {
      log.debug("Blob {} was not found during existence check", blobId);
      return false;
    }
    return true;
  }

  @Override
  public boolean bytesExists(final BlobId blobId) {
    checkNotNull(blobId);
    try {
      if (!fileOperations.exists(contentPath(blobId))) {
        log.debug("Blob {} content (.bytes) was not found during existence check", blobId);
        return false;
      }
      return true;
    }
    catch (Exception e) {
      log.debug("Unable to check existence of {}", contentPath(blobId));
      throw e;
    }
  }

  private boolean delete(final Path path) throws IOException {
    boolean deleted = fileOperations.delete(path);
    if (deleted) {
      log.debug("Deleted {}", path);
    }
    else {
      log.debug("No file to delete found at {}", path);
    }
    return deleted;
  }

  private void move(final Path source, final Path target) throws IOException {
    if (supportsAtomicMove) {
      try {
        fileOperations.copyIfLocked(source, target, fileOperations::moveAtomic);
        return;
      }
      catch (AtomicMoveNotSupportedException e) { // NOSONAR
        supportsAtomicMove = false;
        log.warn("Disabling atomic moves for blob store {}, could not move {} to {}, reason deleted: {}",
            blobStoreConfiguration.getName(), source, target, e.getReason());
      }
    }
    log.trace("Using normal move for blob store {}, moving {} to {}", blobStoreConfiguration.getName(), source, target);
    fileOperations.copyIfLocked(source, target, fileOperations::move);
  }

  private void overwrite(final Path source, final Path target) throws IOException {
    if (supportsAtomicMove) {
      try {
        fileOperations.copyIfLocked(source, target, fileOperations::overwriteAtomic);
        return;
      }
      catch (AtomicMoveNotSupportedException e) { // NOSONAR
        supportsAtomicMove = false;
        log.warn("Disabling atomic moves for blob store {}, could not overwrite {} with {}, reason deleted: {}",
            blobStoreConfiguration.getName(), source, target, e.getReason());
      }
    }
    log.trace("Using normal overwrite for blob store {}, overwriting {} with {}", blobStoreConfiguration.getName(),
        source, target);
    fileOperations.copyIfLocked(source, target, fileOperations::overwrite);
  }

  private void setConfiguredBlobStorePath(final Path path) {
    blobStoreConfiguration.attributes(CONFIG_KEY).set(PATH_KEY, path.toString());
  }

  private Path getConfiguredBlobStorePath() {
    return Paths.get(blobStoreConfiguration.attributes(CONFIG_KEY).require(PATH_KEY).toString());
  }

  /**
   * Delete files known to be part of the FileBlobStore implementation if the content directory is empty.
   */
  @Override
  @Guarded(by = {NEW, STOPPED, FAILED, SHUTDOWN})
  public void remove() {
    try {
      metricsService.remove();

      Path blobDir = getAbsoluteBlobDir();
      FileUtils.deleteDirectory(reconciliationLogDir.toFile());
      if (fileOperations.deleteEmptyDirectory(contentDir)) {
        fileOperations.deleteQuietly(blobDir.resolve("metadata.properties"));
        File[] files = blobDir.toFile().listFiles((dir, name) -> name.endsWith(DELETIONS_FILENAME));
        if (files != null) {
          stream(files)
              .map(File::toPath)
              .forEach(fileOperations::deleteQuietly);
        }
        else {
          log.warn("Unable to cleanup file(s) for Deletions Index");
        }
        if (!fileOperations.deleteEmptyDirectory(blobDir)) {
          log.warn("Unable to delete non-empty blob store directory {}", blobDir);
        }
      }
      else {
        log.warn("Unable to delete non-empty blob store content directory {}", contentDir);
      }
    }
    catch (Exception e) {
      throw new BlobStoreException(e, null);
    }
  }

  /**
   * Returns the absolute form of the configured blob directory.
   */
  public Path getAbsoluteBlobDir() throws IOException {
    Path configurationPath = getConfiguredBlobStorePath();
    if (configurationPath.isAbsolute()) {
      return configurationPath;
    }
    Path normalizedBase = basedir.toRealPath().normalize();
    Path normalizedPath = configurationPath.normalize();
    return normalizedBase.resolve(normalizedPath);
  }

  /**
   * Returns the relative file path (if possible) for the configured blob directory. This operation is only valid after
   * the associated directories have been created on the filesystem.
   */
  @VisibleForTesting
  Path getRelativeBlobDir() throws IOException {
    Path configurationPath = getConfiguredBlobStorePath();
    if (configurationPath.isAbsolute() && basedir != null) {
      Path normalizedBase = basedir.toRealPath().normalize();
      Path normalizedPath = configurationPath.toRealPath().normalize();
      if (normalizedPath.startsWith(normalizedBase)) {
        return normalizedBase.relativize(normalizedPath);
      }
    }
    return configurationPath;
  }

  void doCompactWithDeletedBlobIndex(
      @Nullable final BlobStoreUsageChecker inUseChecker,
      final Duration blobsOlderThan)
  {
    OffsetDateTime date = OffsetDateTime.now().minus(blobsOlderThan);
    String blobStoreName = getBlobStoreConfiguration().getName();
    log.info("Begin deleted blobs processing for blob store '{}' before {}", blobStoreName, date);

    // only process each blob once (in-use blobs may be re-added to the index)
    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, INTERVAL_IN_SECONDS)) {
      int numBlobs = blobDeletionIndex.count(date);
      AtomicInteger counter = new AtomicInteger();
      blobDeletionIndex.getRecordsBefore(date).forEach(blobId -> {
        CancelableHelper.checkCancellation();
        BlobSupport blob = liveBlobs.getIfPresent(blobId);
        log.debug("Next available record for compaction: {}", blobId);
        if (Objects.isNull(blob) || blob.isStale()) {
          log.debug("Compacting...");
          maybeCompactBlob(inUseChecker, blobId);
          blobDeletionIndex.deleteRecord(blobId);
        }
        else {
          log.debug("Still in use to deferring");
        }

        progressLogger.info("Blob store '{}' - Elapsed time: {}, processed: {}/{}", blobStoreName,
            progressLogger.getElapsed(), counter.incrementAndGet(), numBlobs);
      });
      // once done removing stuff, clean any empty directories left around in the directpath area
      pruneEmptyDirectories(progressLogger, contentDir.resolve(DIRECT_PATH_ROOT));

      // Also clean up empty directories in the regular content area to prevent inode exhaustion
      if (SystemPropertiesHelper.getBoolean("nexus.blobstore.compact.cleanEmptyDirectories", true)) {
        pruneEmptyContentDirectories(progressLogger);
      }
    }
  }

  /**
   * Prunes empty directories in the content area (date-based: yyyy/MM/dd/HH/mm structure).
   * This prevents inode exhaustion by removing old empty date directories that will never be reused.
   *
   * SECURITY: This method validates all paths stay within contentDir boundaries to prevent
   * directory traversal attacks. Symbolic links are explicitly skipped.
   */
  private void pruneEmptyContentDirectories(final ProgressLogIntervalHelper progressLogger) {
    try {
      Path contentDirRealPath = contentDir.toRealPath();
      log.info("Pruning empty directories in content area {}", contentDir);
      int count = pruneEmptyDirectoriesRecursive(contentDir, contentDirRealPath);

      if (count > 0) {
        progressLogger.info("Removed {} empty directories from content area {}", count, contentDir.toAbsolutePath());
        log.info("Removed {} empty directories from blob store '{}'", count, blobStoreConfiguration.getName());
      }
    }
    catch (IOException e) {
      log.error("Failed to prune some empty directories from content area {}: {}",
          contentDir.toAbsolutePath(), e.getMessage());
    }
  }

  /**
   * Recursively prunes empty directories in a bottom-up traversal.
   *
   * SECURITY PROTECTIONS:
   * - Explicitly skips symbolic links to prevent following links outside the blob store
   * - Validates directory boundaries at each recursion entry via shouldProcessDirectory()
   * - Uses LinkOption.NOFOLLOW_LINKS on all directory checks
   * - Continues processing even if individual paths fail validation
   *
   * PERFORMANCE OPTIMIZATION:
   * - Boundary validation only performed at recursion entry (shouldProcessDirectory)
   * - Child entries are not re-validated since parent is validated and children are not symlinks
   * - Reduces expensive toRealPath() syscalls by ~50-70%
   *
   * @param dir the directory to process
   * @param contentDirRealPath the canonical real path of contentDir for boundary validation
   * @return the count of directories successfully deleted
   * @throws IOException if directory stream cannot be opened
   */
  private int pruneEmptyDirectoriesRecursive(final Path dir, final Path contentDirRealPath) throws IOException {
    if (!shouldProcessDirectory(dir, contentDirRealPath)) {
      return 0;
    }

    int count = 0;

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path entry : stream) {
        // Check if entry is a symbolic link - NEVER follow symlinks during deletion
        if (Files.isSymbolicLink(entry)) {
          log.warn("Skipping symbolic link in blob store (unusual and potentially suspicious): {}", entry);
          continue;
        }

        // Use NOFOLLOW_LINKS to ensure we don't follow symlinks
        if (!Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
          continue;
        }

        // Recursively process subdirectories first (bottom-up)
        count += pruneEmptyDirectoriesRecursive(entry, contentDirRealPath);

        // Skip deletion for special directories (tmp and directpath)
        String entryName = entry.getFileName() != null ? entry.getFileName().toString() : "";
        if (TMP.equals(entryName) || DIRECT_PATH_ROOT.equals(entryName)) {
          continue;
        }

        // Try to delete if empty (parent directories don't need age check since they became empty during this run)
        if (tryDeleteEmptyDirectoryNoAgeCheck(entry)) {
          count++;
        }
      }
    }

    return count;
  }

  /**
   * Checks if a directory should be processed (recursed into) for cleanup.
   * Special directories (tmp, directpath) are still recursed into, just not deleted themselves.
   *
   * SECURITY: Validates the directory exists within contentDirRealPath boundaries to prevent
   * path traversal attacks. Uses LinkOption.NOFOLLOW_LINKS to prevent following symbolic links.
   *
   * @param dir the directory to check
   * @param contentDirRealPath the canonical real path of contentDir for boundary validation
   * @return true if directory exists and should be processed, false otherwise
   */
  private boolean shouldProcessDirectory(final Path dir, final Path contentDirRealPath) {
    if (!Files.exists(dir) || !Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
      return false;
    }

    try {
      // Ensure directory is within contentDir boundaries to prevent path traversal
      Path realPath = dir.toRealPath();
      if (!realPath.startsWith(contentDirRealPath)) {
        log.warn("Skipping directory outside content area: {}", dir);
        return false;
      }
    }
    catch (IOException e) {
      log.debug("Cannot resolve real path for directory: {}", dir, e);
      return false;
    }

    return true;
  }

  /**
   * Attempts to delete an empty directory without age check.
   * Used for parent directories that became empty during the current cleanup run.
   *
   * Files.delete() is atomic and only succeeds if the directory is empty,
   * eliminating the need for a separate isEmpty check and preventing TOCTOU race conditions.
   *
   * @return true if directory was deleted, false otherwise
   */
  private boolean tryDeleteEmptyDirectoryNoAgeCheck(final Path dir) {
    try {
      // Use atomic delete that only succeeds if directory is empty
      // This prevents race condition - delete will fail if directory becomes non-empty
      Files.delete(dir);
      log.trace("Deleted empty directory: {}", dir);
      return true;
    }
    catch (DirectoryNotEmptyException e) {
      // Directory not empty (race condition or has contents) - expected, not an error
      log.trace("Directory not empty, skipping: {}", dir);
      return false;
    }
    catch (IOException e) {
      log.debug("Could not delete directory {}: {}", dir, e.getMessage());
      return false;
    }
  }

  private void pruneEmptyDirectories(final ProgressLogIntervalHelper progressLogger, final Path directPathDir) {
    long timestamp = new Date().getTime() - pruneEmptyDirectoryAge;

    final String absolutePath = directPathDir.toAbsolutePath().toString();

    progressLogger.info("Removing empty directories from {} that haven't been modified in last {}",
        absolutePath,
        DateTimeFormat.forPattern("kk' hours 'mm' minutes 'ss.SSS' seconds'").print(timestamp));
    try {
      int count = directoryHelper.deleteIfEmptyRecursively(directPathDir, timestamp);
      progressLogger.info("Removed {} empty directories from {}", count, absolutePath);
    }
    catch (IOException e) {
      log.error("Failed to remove at least one empty directory from {}", absolutePath, e);
      progressLogger.info("Failed to remove at least one empty directory from {}: {}", absolutePath, e.getMessage());
    }
  }

  @VisibleForTesting
  void doCompactWithoutDeletedBlobIndex(@Nullable final BlobStoreUsageChecker inUseChecker) throws IOException {
    String blobStoreName = getBlobStoreConfiguration().getName();
    log.info("Begin deleted blobs processing for blob store '{}' without deleted blob index", blobStoreName);
    // clear the deleted blob index ahead of time, so we won't lose deletes that may occur while the compact is being
    // performed
    blobDeletionIndex.deleteAllRecords();

    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, INTERVAL_IN_SECONDS)) {
      AtomicInteger count = new AtomicInteger(0);

      // rather than using the blobId stream here, need to use a different means of walking the file tree, as
      // we are deleting items on the way through, and apparently on *nix systems, deleting files that you are about to
      // walk over causes a FileNotFoundException to be thrown and the walking stops. Overridding the visitFileFailed
      // method allows us to get past that
      Files.walkFileTree(contentDir, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>()
      {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
          try {
            checkCancellation();
          }
          catch (TaskInterruptedException e) {
            log.info("Cancel request received, terminating compact process.");
            return FileVisitResult.TERMINATE;
          }

          if (!isNonTemporaryAttributeFile(file)) {
            return FileVisitResult.CONTINUE;
          }

          BlobId blobId = getBlobIdFromAttributeFilePath(new FileAttributesLocation(file));
          if (blobId != null) {
            FileBlobAttributes attributes = getFileBlobAttributes(blobId);

            if (attributes != null && attributes.isDeleted()) {
              compactByAttributes(attributes, inUseChecker, count, progressLogger);
            }
          }

          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
          log.debug("Visit file failed {}, continuing to next.", file);
          return FileVisitResult.CONTINUE;
        }
      });

      // Do this check one final time, to preserve the functionality of throwing an exception when interrupted
      checkCancellation();
    }
  }

  private void compactByAttributes(
      final FileBlobAttributes attributes,
      final BlobStoreUsageChecker inUseChecker,
      final AtomicInteger count,
      final ProgressLogIntervalHelper progressLogger)
  {
    BlobId blobId = getBlobIdFromAttributeFilePath(new FileAttributesLocation(attributes.getPath()));
    BlobSupport blob = blobId != null ? liveBlobs.getIfPresent(blobId) : null;
    if (blob == null || blob.isStale()) {
      if (!maybeCompactBlob(inUseChecker, blobId)) {
        blobDeletionIndex.createRecord(blobId);
      }
      else {
        String blobStoreName = getBlobStoreConfiguration().getName();
        progressLogger.info("Blob store '{}' - Elapsed time: {}, processed: {}", blobStoreName,
            progressLogger.getElapsed(), count.incrementAndGet());
      }
    }
    else {
      blobDeletionIndex.createRecord(blobId);
    }
  }

  private Stream<Path> getAttributeFilePaths() {
    return getAttributeFilePaths(EMPTY);
  }

  private Stream<Path> getAttributeFilePaths(final String prefix) {
    Path parent = contentDir.resolve(prefix);
    if (!parent.toFile().exists()) {
      return Stream.empty();
    }
    try {
      return Files.walk(parent, FOLLOW_LINKS)
          .filter(this::isNonTemporaryAttributeFile);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /*
   * Resolve the prefix against the contentDir and return a stream of non-directory paths. This stream must be closed.
   */
  private Stream<Path> listContentFiles(final String prefix) {
    Path parent = contentDir.resolve(prefix);
    if (!parent.toFile().exists()) {
      return Stream.empty();
    }
    try {
      return Files.walk(parent, FOLLOW_LINKS)
          .filter(Files::isRegularFile);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean isNonTemporaryAttributeFile(final Path path) {
    File attributeFile = path.toFile();
    return attributeFile.isFile() &&
        attributeFile.getName().endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX) &&
        !attributeFile.getName().startsWith(TEMPORARY_BLOB_ID_PREFIX) &&
        !attributeFile.getAbsolutePath().contains(NATIVE_CONTENT_TMP_PATH);
  }

  class FileBlob
      extends BlobSupport
  {
    FileBlob(final BlobId blobId) {
      super(blobId);
    }

    @Override
    protected InputStream doGetInputStream() {
      Path contentPath = contentPath(getId());
      try {
        checkExists(contentPath, getId());
        return performanceLogger.maybeWrapForPerformanceLogging(
            new BufferedInputStream(fileOperations.openInputStream(contentPath)));
      }
      catch (BlobStoreException e) {
        // In certain conditions its possible that a blob does not exist on disk at this point. In this case we need to
        // mark the blob as stale so that subsequent accesses will trigger disk based checks (see NEXUS-13600)
        markStale();
        throw e;
      }
      catch (Exception e) {
        throw new BlobStoreException(e, getId());
      }
    }

    private FileBlobStore owner() {
      return FileBlobStore.this;
    }
  }

  private interface BlobIngester
  {
    StreamMetrics ingestTo(final Path destination) throws IOException;
  }

  @VisibleForTesting
  void setLiveBlobs(final LoadingCache<BlobId, BlobSupport> liveBlobs) {
    this.liveBlobs = liveBlobs;
  }

  @Override
  public Stream<BlobId> getBlobIdStream() {
    return getAttributeFilePaths()
        .map(FileAttributesLocation::new)
        .map(this::getBlobIdFromAttributeFilePath)
        .filter(Objects::nonNull);
  }

  @Override
  @Guarded(by = STARTED)
  public Stream<BlobId> getBlobIdUpdatedSinceStream(
      final String prefix,
      final OffsetDateTime fromDateTime,
      final OffsetDateTime toDateTime)
  {
    boolean isVolumePrefix = VOLUME_PREFIX.equals(prefix);

    DateInterval interval = new DateInterval(fromDateTime, toDateTime);
    Predicate<Blob> blobMatches = !isVolumePrefix
        ? this::isNonTempBlob
        : ((Predicate<Blob>) this::isNonTempBlob).and(blob -> isBlobCreatedInRange(blob, interval));

    Stream<Path> paths;
    if (isVolumePrefix) {
      // Create a list of vol-xx prefixes
      List<String> prefixes = List.of(prefix);
      try (Stream<Path> volumes = Files.list(contentDir)) {
        prefixes = volumes
            .filter(Files::isDirectory)
            .map(Path::getFileName)
            .map(Path::toString)
            .filter(dir -> dir.startsWith(VOLUME_PREFIX))
            .toList();
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      // List files under all the content-dir prefixes
      paths = prefixes.stream()
          .flatMap(this::listContentFiles);
    }
    else {
      // List files in the contentDir with the provided prefixes
      paths = listContentFiles(prefix);
    }

    // Restrict to bytes & properties files
    return paths.filter(path -> isBlobStoreContent(path.toString()))
        // Create a location and parse it into a BlobId
        .map(FileAttributesLocation::new)
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
  public Stream<BlobId> getDirectPathBlobIdStream(final String prefix) {
    checkArgument(!prefix.contains(".."), "path traversal not allowed");
    try {
      return getAttributeFilePaths(DIRECT_PATH_ROOT + "/" + prefix)
          .map(this::toBlobName)
          .filter(Objects::nonNull)
          .map(this::toBlobId);
    }
    catch (UncheckedIOException e) {
      log.error("Caught IOException during getDirectPathBlobIdStream for {}", prefix, e);
      throw e;
    }
  }

  @VisibleForTesting
  public Path getContentDir() {
    return contentDir;
  }

  /**
   * Converts a direct path {@link Path} to the value for {@link #BLOB_NAME_HEADER} that created it.
   *
   * @param path the {@link Path} to the direct path blob
   * @return the correct form for the corresponding {@link #BLOB_NAME_HEADER} or null if the file is no longer available
   */
  @VisibleForTesting
  @Nullable
  String toBlobName(final Path path) {
    try {
      String pathStr = contentDir.resolve(DIRECT_PATH_ROOT)
          .relativize(path) // just the relative path part under DIRECT_PATH_ROOT
          .toString()
          .replace(File.separatorChar, '/'); // guarantee we return unix-style paths
      return removeEnd(pathStr, BLOB_FILE_ATTRIBUTES_SUFFIX); // drop the .properties suffix
    }
    catch (Exception ex) {
      // file is no longer available
      log.debug("Attempting to create blob name from path {}, but caught Exception", path, ex);
      return null;
    }
  }

  @Nullable
  @Override
  public BlobAttributes getBlobAttributes(final BlobId blobId) {
    try {
      return getBlobAttributesWithException(blobId);
    }
    catch (BlobStoreException e) {
      return null;
    }
  }

  @Override
  public BlobAttributes getBlobAttributes(final FileAttributesLocation attributesFilePath) throws IOException {
    try {
      FileBlobAttributes fileBlobAttributes = new FileBlobAttributes(attributesFilePath.getPath());
      return fileBlobAttributes.load() ? fileBlobAttributes : null;
    }
    catch (Exception e) {
      log.error("Unable to load FileBlobAttributes by path: {}", attributesFilePath.getFullPath(), e);
      throw new IOException(e);
    }
  }

  @Nullable
  @VisibleForTesting
  FileBlobAttributes getFileBlobAttributes(final BlobId blobId) {
    return (FileBlobAttributes) getBlobAttributes(blobId);
  }

  @VisibleForTesting
  FileBlobAttributes getFileBlobAttributes(final Path path) {
    return new FileBlobAttributes(path);
  }

  /**
   * Used by {@link #getDirectPathBlobIdStream(String)} to convert a blob "name" ({@link #toBlobName(Path)}) to a
   * {@link BlobId}.
   *
   * @see BlobIdLocationResolver
   */
  private BlobId toBlobId(final String blobName) {
    Map<String, String> headers = ImmutableMap.of(
        BLOB_NAME_HEADER, blobName,
        DIRECT_PATH_BLOB_HEADER, "true");
    return blobIdLocationResolver.fromHeaders(headers);
  }

  @Override
  public void setBlobAttributes(final BlobId blobId, final BlobAttributes blobAttributes) {
    FileBlobAttributes fileBlobAttributes = getFileBlobAttributes(blobId);
    if (fileBlobAttributes == null) {
      // Benign race condition - concurrent request is updating the same blob
      // properties file
      log.debug("Blob attributes temporarily unavailable for blob id: {} during concurrent access",
          blobId);
      return;
    }

    for (int attempt = 1; attempt <= blobAttributesMaxRetries; attempt++) {
      try {
        fileBlobAttributes = getFileBlobAttributes(blobId);
        if (fileBlobAttributes == null) {
          log.debug("Blob attributes not found for blob id: {} during retry attempt {}", blobId, attempt);
          return;
        }

        fileBlobAttributes.updateFrom(blobAttributes);
        fileBlobAttributes.store();

        if (attempt > 1) {
          log.debug("Successfully set BlobAttributes for {} on attempt {}", blobId, attempt);
        }

        return;
      }
      catch (Exception e) {
        // NEXUS-51247: Handle benign race condition on write side
        // If file doesn't exist during write, another thread is likely updating the same blob
        if (e instanceof NoSuchFileException || e.getCause() instanceof NoSuchFileException) {
          log.debug(
              "Blob attributes temporarily unavailable for blob id: {} during concurrent write access (attempt {})",
              blobId, attempt);
          return;
        }

        if (attempt < blobAttributesMaxRetries) {
          log.warn("Failed to set BlobAttributes for {} on attempt {}, retrying after {}ms",
              blobId, attempt, blobAttributesRetryDelayMs);

          try {
            Thread.sleep(blobAttributesRetryDelayMs);
          }
          catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BlobStoreException("Interrupted while retrying setBlobAttributes", e, blobId);
          }
        }
        else {
          log.error("Failed to set BlobAttributes for {} after {} attempts", blobId, blobAttributesMaxRetries);
          throw new BlobStoreException("Unable to set BlobAttributes after retries", e, blobId);
        }
      }
    }
  }

  @Override
  @VisibleForTesting
  public void flushMetrics() throws IOException {
    metricsService.flush();
  }

  @Override
  protected void deleteCopiedAttributes(final BlobId blobId, final String softDeletedLocation) {
    log.trace("deleteCopiedAttributes for blobId: {}, softDeletedLocation: {}", blobId, softDeletedLocation);
    fileOperations.deleteQuietly(attributePath(createBlobIdForTimePath(blobId, softDeletedLocation)));
  }

  @Override
  protected BlobAttributes loadBlobAttributes(final BlobId blobId) throws IOException {
    Path attributePath = attributePath(blobId);
    FileBlobAttributes blobAttributes = new FileBlobAttributes(attributePath);

    try {
      return blobAttributes.load() ? blobAttributes : null;
    }
    catch (IOException e) {
      // NEXUS-50152 Fix: Distinguish between transient I/O errors and actual corruption
      // Do NOT delete properties files here - let the repair task handle deletion

      // Check if file exists - if not, it's normal (new blobs or after deletion)
      if (!fileOperations.exists(attributePath)) {
        log.debug("Properties file {} for blob {} does not exist (normal for new blobs or after deletion)",
            attributePath, blobId);
        return null;
      }

      // File exists but couldn't be loaded - this could be transient (locked, NFS issue, etc.)
      // Be conservative: DO NOT delete on I/O errors as they may be temporary
      log.warn("Error reading properties file {} for blob {}: {}", attributePath, blobId, e.getMessage());

      // Propagate the IOException to indicate the file is temporarily unavailable
      // This allows calling code to retry or handle appropriately
      throw e;
    }
    catch (RuntimeException e) {
      // Runtime exceptions (IllegalArgumentException, etc.) likely indicate actual corruption
      // Do NOT delete here - let the repair task handle deletion
      log.warn("Corrupt properties file detected for blob {} at {}: {}", blobId, attributePath, e.getMessage());

      // Return null so it's treated as "missing" by reconciliation task
      return null;
    }
  }

  @Nullable
  @Override
  public BlobAttributes getBlobAttributesWithException(final BlobId blobId) throws BlobStoreException {
    Path blobPath = attributePath(blobId);
    try {
      FileBlobAttributes blobAttributes = new FileBlobAttributes(blobPath);
      if (!blobAttributes.load()) {
        log.debug("Attempt to access blob attributes file {} for blob {} (file temporarily unavailable or deleted)",
            attributePath(blobId), blobId);
        return null;
      }
      else {
        return blobAttributes;
      }
    }
    catch (Exception e) {
      log.error(ATTRIBUTES_FOR_BLOB_ID_EXCEPTION,
          blobId, blobPath, e.getMessage(), log.isDebugEnabled() ? e : null);
      throw new BlobStoreException(e, blobId);
    }
  }
}
