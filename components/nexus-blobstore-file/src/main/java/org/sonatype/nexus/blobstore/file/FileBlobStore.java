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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.BlobIdLocationResolver;
import org.sonatype.nexus.blobstore.BlobStoreSupport;
import org.sonatype.nexus.blobstore.BlobSupport;
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
import org.sonatype.nexus.blobstore.file.internal.BlobCollisionException;
import org.sonatype.nexus.blobstore.file.internal.FileBlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.file.internal.FileOperations;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.io.DirectoryHelper;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.squareup.tape.QueueFile;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.cache.CacheLoader.from;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver.TEMPORARY_BLOB_ID_PREFIX;
import static org.sonatype.nexus.blobstore.DirectPathLocationStrategy.DIRECT_PATH_ROOT;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STOPPED;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * A {@link BlobStore} that stores its content on the file system.
 *
 * @since 3.0
 */
@Named(FileBlobStore.TYPE)
public class FileBlobStore
    extends BlobStoreSupport<FileAttributesLocation>
{
  public static final String BASEDIR = "blobs";

  public static final String TYPE = "File";

  public static final String BLOB_CONTENT_SUFFIX = ".bytes";

  @VisibleForTesting
  public static final String CONFIG_KEY = "file";

  @VisibleForTesting
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

  private static final boolean RETRY_ON_COLLISION =
      SystemPropertiesHelper.getBoolean("nexus.blobstore.retryOnCollision", true);

  @VisibleForTesting
  static final int MAX_COLLISION_RETRIES = 8;

  private Path contentDir;

  private final FileOperations fileOperations;

  private final Path basedir;

  private FileBlobStoreMetricsStore metricsStore;

  private LoadingCache<BlobId, FileBlob> liveBlobs;

  private QueueFile deletedBlobIndex;

  private final NodeAccess nodeAccess;

  private boolean supportsHardLinkCopy;

  private boolean supportsAtomicMove;

  @Inject
  public FileBlobStore(final BlobIdLocationResolver blobIdLocationResolver,
                       final FileOperations fileOperations,
                       final ApplicationDirectories directories,
                       final FileBlobStoreMetricsStore metricsStore,
                       final NodeAccess nodeAccess,
                       final DryRunPrefix dryRunPrefix)
  {
    super(blobIdLocationResolver, dryRunPrefix);
    this.fileOperations = checkNotNull(fileOperations);
    this.basedir = directories.getWorkDirectory(BASEDIR).toPath();
    this.metricsStore = checkNotNull(metricsStore);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.supportsHardLinkCopy = true;
    this.supportsAtomicMove = true;
  }

  @VisibleForTesting
  public FileBlobStore(final Path contentDir, //NOSONAR
                       final BlobIdLocationResolver blobIdLocationResolver,
                       final FileOperations fileOperations,
                       final FileBlobStoreMetricsStore metricsStore,
                       final BlobStoreConfiguration configuration,
                       final ApplicationDirectories directories,
                       final NodeAccess nodeAccess,
                       final DryRunPrefix dryRunPrefix)
  {
    this(blobIdLocationResolver, fileOperations, directories, metricsStore, nodeAccess, dryRunPrefix);
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
    File deletedIndexFile = storageDir.resolve(getDeletionsFilename()).toFile();
    try {
      maybeUpgradeLegacyIndexFile(deletedIndexFile.toPath());
      deletedBlobIndex = new QueueFile(deletedIndexFile);
    }
    catch (IOException e) {
      log.error("Unable to load deletions index file {}, run the compact blobstore task to rebuild", deletedIndexFile,
          e);
      createEmptyDeletionsIndex(deletedIndexFile);
      deletedBlobIndex = new QueueFile(deletedIndexFile);
      metadata.setProperty(REBUILD_DELETED_BLOB_INDEX_KEY, "true");
      metadata.store();
    }
    metricsStore.setStorageDir(storageDir);
    metricsStore.setBlobStore(this);
    metricsStore.start();
  }

  private void maybeUpgradeLegacyIndexFile(final Path deletedIndexPath) throws IOException {
    //While Path#getParent can return null we don't expect that from a configured blob store directory.
    Path legacyDeletionsIndex = deletedIndexPath.getParent().resolve(DELETIONS_FILENAME); //NOSONAR

    if (!Files.exists(deletedIndexPath) && Files.exists(legacyDeletionsIndex)) {
      log.info("Found 'deletions.index' file in blob store {}, renaming to {}", getAbsoluteBlobDir(),
          deletedIndexPath);
      Files.move(legacyDeletionsIndex, deletedIndexPath);
    }
  }

  private String getDeletionsFilename() {
    return nodeAccess.getId() + "-" + DELETIONS_FILENAME;
  }

  @Override
  protected void doStop() throws Exception {
    liveBlobs = null;
    try {
      deletedBlobIndex.close();
    }
    finally {
      deletedBlobIndex = null;
      metricsStore.stop();
    }
  }

  /**
   * Returns path for blob-id content file relative to root directory.
   */
  @VisibleForTesting
  Path contentPath(final BlobId id) {
    return contentDir.resolve(blobIdLocationResolver.getLocation(id) + BLOB_CONTENT_SUFFIX);
  }

  /**
   * Returns path for blob-id attribute file relative to root directory.
   */
  @VisibleForTesting
  Path attributePath(final BlobId id) {
    return contentDir.resolve(blobIdLocationResolver.getLocation(id) + BLOB_ATTRIBUTE_SUFFIX);
  }

  protected String attributePathString(final BlobId blobId) {
    return attributePath(blobId).toString();
  }

  /**
   * Returns a path for a temporary blob-id content file relative to root directory.
   */
  private Path temporaryContentPath(final BlobId id, final UUID suffix) {
    return contentDir.resolve(blobIdLocationResolver.getTemporaryLocation(id) + "." + suffix + BLOB_CONTENT_SUFFIX);
  }

  /**
   * Returns path for a temporary blob-id attribute file relative to root directory.
   */
  private Path temporaryAttributePath(final BlobId id, final UUID suffix) {
    return contentDir.resolve(blobIdLocationResolver.getTemporaryLocation(id) + "." + suffix + BLOB_ATTRIBUTE_SUFFIX);
  }

  @Override
  protected Blob doCreate(final InputStream blobData, final Map<String, String> headers, @Nullable final BlobId blobId)
  {
    return create(headers, destination -> fileOperations.create(destination, blobData), blobId);
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final Path sourceFile, final Map<String, String> headers, final long size, final HashCode sha1) {
    checkNotNull(sourceFile);
    checkNotNull(sha1);
    checkArgument(Files.exists(sourceFile));

    return create(headers, destination -> {
      fileOperations.hardLink(sourceFile, destination);
      return new StreamMetrics(size, sha1.toString());
    }, null);
  }

  private Blob create(final Map<String, String> headers, final BlobIngester ingester, final BlobId blobId) {
    for (int retries = 0; retries <= MAX_COLLISION_RETRIES; retries++) {
      try {
        return tryCreate(headers, ingester, blobId);
      }
      catch (BlobCollisionException e) { // NOSONAR
        log.warn("BlobId collision: {} already exists{}", e.getBlobId(),
            retries < MAX_COLLISION_RETRIES ? ", retrying with new BlobId" : "!");
      }
    }
    throw new BlobStoreException("Cannot find free BlobId", null);
  }

  private Blob tryCreate(final Map<String, String> headers, final BlobIngester ingester, final BlobId reusedBlobId) { // NOSONAR
    final BlobId blobId = getBlobId(headers, reusedBlobId);
    final boolean isDirectPath = Boolean.parseBoolean(headers.getOrDefault(DIRECT_PATH_BLOB_HEADER, "false"));
    final Long existingSize = isDirectPath && exists(blobId) ? getContentSizeForDeletion(blobId) : null;

    final Path blobPath = contentPath(blobId);
    final Path attributePath = attributePath(blobId);

    final UUID uuidSuffix = UUID.randomUUID();
    final Path temporaryBlobPath = temporaryContentPath(blobId, uuidSuffix);
    final Path temporaryAttributePath = temporaryAttributePath(blobId, uuidSuffix);

    final FileBlob blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      final boolean wouldCollide = fileOperations.exists(blobPath);

      if ((reusedBlobId == null) && RETRY_ON_COLLISION && wouldCollide && !isDirectPath) {
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
          metricsStore.recordDeletion(existingSize);
        }
        else {
          move(temporaryBlobPath, blobPath);
          move(temporaryAttributePath, attributePath);
        }

        metricsStore.recordAddition(blobAttributes.getMetrics().getContentSize());

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

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobId blobId) {
    return get(blobId, false);
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobId blobId, final boolean includeDeleted) {
    checkNotNull(blobId);

    final FileBlob blob = liveBlobs.getUnchecked(blobId);

    if (blob.isStale()) {
      Lock lock = blob.lock();
      try {
        if (blob.isStale()) {
          FileBlobAttributes blobAttributes = getFileBlobAttributes(blobId);
          if (blobAttributes == null) {
            return null;
          }

          if (blobAttributes.isDeleted() && !includeDeleted) {
            log.warn("Attempt to access soft-deleted blob {} attributes: {}", blobId, blobAttributes);
            return null;
          }

          blob.refresh(blobAttributes.getHeaders(), blobAttributes.getMetrics());
        }
      }
      catch (Exception e) {
        throw new BlobStoreException(e, blobId);
      }
      finally {
        lock.unlock();
      }
    }

    log.debug("Accessing blob {}", blobId);

    return blob;
  }

  @Override
  protected boolean doDelete(final BlobId blobId, final String reason) {
    final FileBlob blob = liveBlobs.getUnchecked(blobId);

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
        return false;
      }

      blobAttributes.setDeleted(true);
      blobAttributes.setDeletedReason(reason);
      blobAttributes.store();

      // record blob for hard-deletion when the next compact task runs
      deletedBlobIndex.add(blobId.toString().getBytes(StandardCharsets.UTF_8));
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
    final FileBlob blob = liveBlobs.getUnchecked(blobId);
    Lock lock = blob.lock();
    try {
      log.debug("Hard deleting blob {}", blobId);

      Path attributePath = attributePath(blobId);
      Long contentSize = getContentSizeForDeletion(blobId);

      Path blobPath = contentPath(blobId);

      boolean blobDeleted = delete(blobPath);
      delete(attributePath);

      if (blobDeleted && contentSize != null) {
        metricsStore.recordDeletion(contentSize);
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
    return Optional.ofNullable(getFileBlobAttributes(blobId))
          .map(BlobAttributes::getMetrics)
          .map(BlobMetrics::getContentSize)
          .orElse(null);
  }


  @Override
  @Guarded(by = STARTED)
  public BlobStoreMetrics getMetrics() {
    return metricsStore.getMetrics();
  }

  @Override
  protected void doCompact(@Nullable final BlobStoreUsageChecker inUseChecker) {
    try {
      maybeRebuildDeletedBlobIndex();

      log.info("Begin deleted blobs processing");
      // only process each blob once (in-use blobs may be re-added to the index)
      ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
      for (int counter = 0, numBlobs = deletedBlobIndex.size(); counter < numBlobs; counter++) {
        checkCancellation();
        byte[] bytes = deletedBlobIndex.peek();
        if (bytes == null) {
          return;
        }
        deletedBlobIndex.remove();
        BlobId blobId = new BlobId(new String(bytes, StandardCharsets.UTF_8));
        FileBlob blob = liveBlobs.getIfPresent(blobId);
        if (blob == null || blob.isStale()) {
          maybeCompactBlob(inUseChecker, blobId);
        }
        else {
          // still in use, so move it to end of the queue
          deletedBlobIndex.add(bytes);
        }
        progressLogger.info("Elapsed time: {}, processed: {}/{}", progressLogger.getElapsed(),
            counter + 1, numBlobs);
      }
      progressLogger.flush();
    }
    catch (BlobStoreException | TaskInterruptedException e) {
      throw e;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, null);
    }
  }

  private void maybeCompactBlob(@Nullable final BlobStoreUsageChecker inUseChecker, final BlobId blobId)
      throws IOException
  {
    Optional<FileBlobAttributes> attributesOption = ofNullable((FileBlobAttributes) getBlobAttributes(blobId));
    if (!attributesOption.isPresent() || !undelete(inUseChecker, blobId, attributesOption.get(), false)) {
      // attributes file is missing or blob id not in use, so it's safe to delete the file
      log.debug("Hard deleting blob id: {}, in blob store: {}", blobId, blobStoreConfiguration.getName());
      deleteHard(blobId);
    }
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
      Path blobDir = getAbsoluteBlobDir();
      Path content = blobDir.resolve("content");
      DirectoryHelper.mkdir(content);
      this.contentDir = content;
      setConfiguredBlobStorePath(getRelativeBlobDir());
    }
    catch (Exception e) {
      throw new BlobStoreException(
          "Unable to initialize blob store directory structure: " + getConfiguredBlobStorePath(), e, null);
    }
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
   * This is a simple existence check resulting from NEXUS-16729.  This allows clients
   * to perform a simple check and is primarily intended for use in directpath scenarios.
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

  private boolean delete(final Path path) throws IOException {
    boolean deleted = fileOperations.delete(path);
    if (deleted) {
      log.debug("Deleted {}", path);
    }
    else {
      log.error("No file to delete found at {}", path, new FileNotFoundException(path.toString()));
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
    log.trace("Using normal overwrite for blob store {}, overwriting {} with {}", blobStoreConfiguration.getName(), source, target);
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
  @Guarded(by = {NEW, STOPPED, FAILED})
  public void remove() {
    try {
      Path blobDir = getAbsoluteBlobDir();
      if (fileOperations.deleteEmptyDirectory(contentDir)) {
        metricsStore.remove();
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
  @VisibleForTesting
  Path getAbsoluteBlobDir() throws IOException {
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
    if (configurationPath.isAbsolute()) {
      Path normalizedBase = basedir.toRealPath().normalize();
      Path normalizedPath = configurationPath.toRealPath().normalize();
      if (normalizedPath.startsWith(normalizedBase)) {
        return normalizedBase.relativize(normalizedPath);
      }
    }
    return configurationPath;
  }

  @VisibleForTesting
  void maybeRebuildDeletedBlobIndex() throws IOException {
    PropertiesFile metadata = new PropertiesFile(getAbsoluteBlobDir().resolve(METADATA_FILENAME).toFile());
    metadata.load();
    String deletedBlobIndexRebuildRequired = metadata.getProperty(REBUILD_DELETED_BLOB_INDEX_KEY, "false");
    if (Boolean.parseBoolean(deletedBlobIndexRebuildRequired)) {
      Path deletedIndex = getAbsoluteBlobDir().resolve(getDeletionsFilename());

      log.warn("Clearing deletions index file {} for rebuild", deletedIndex);
      deletedBlobIndex.clear();

      if (!nodeAccess.isOldestNode()) {
        log.info("Skipping deletion index rebuild because this is not the oldest node.");
        return;
      }

      log.warn("Rebuilding deletions index file {}", deletedIndex);
      ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
      final AtomicInteger processed = new AtomicInteger();
      int softDeletedBlobsFound = getBlobIdStream()
          .map(this::getFileBlobAttributes)
          .filter(Objects::nonNull)
          .mapToInt(attributes -> {
            try {
              if (attributes.isDeleted()) {
                String blobId = getBlobIdFromAttributeFilePath(new FileAttributesLocation(attributes.getPath()));
                deletedBlobIndex.add(blobId.getBytes(StandardCharsets.UTF_8));
                return 1;
              }
            }
            catch (IOException e) {
              log.warn("Failed to add blobId to index from attribute file {}", attributes.getPath(), e);
            }
            finally {
              progressLogger.info("Elapsed time: {}, processed: {}, deleted: {}", progressLogger.getElapsed(),
                  processed.incrementAndGet(), deletedBlobIndex.size());
            }
            return 0;
          })
          .sum();

      progressLogger.flush();
      log.warn("Elapsed time: {}, Added {} soft deleted blob(s) to index file {}", progressLogger.getElapsed(),
          softDeletedBlobsFound, deletedIndex);

      metadata.remove(REBUILD_DELETED_BLOB_INDEX_KEY);
      metadata.store();
    }
    else {
      log.info("Deletions index file rebuild not required");
    }
  }

  private Stream<Path> getAttributeFilePaths() throws IOException {
    return getAttributeFilePaths(EMPTY);
  }

  private Stream<Path> getAttributeFilePaths(final String prefix) throws IOException {
    Path parent = contentDir.resolve(prefix);
    if (!parent.toFile().exists()) {
      return Stream.empty();
    }
    return Files.walk(parent, FOLLOW_LINKS).filter(this::isNonTemporaryAttributeFile);
  }

  private boolean isNonTemporaryAttributeFile(final Path path) {
    File attributeFile = path.toFile();
    return attributeFile.isFile() &&
        attributeFile.getName().endsWith(BLOB_ATTRIBUTE_SUFFIX) &&
        !attributeFile.getName().startsWith(TEMPORARY_BLOB_ID_PREFIX);
  }

  private void createEmptyDeletionsIndex(final File deletionsIndex) throws IOException {
    // copy a fresh index on top of existing index to avoid problems
    // with removing or renaming open files on Windows
    Path tempFile = Files.createTempFile(DELETIONS_FILENAME, "tmp");
    Files.delete(tempFile);
    try {
      new QueueFile(tempFile.toFile()).close();
      try (RandomAccessFile raf = new RandomAccessFile(deletionsIndex, "rw")) {
        raf.setLength(0);
        raf.write(Files.readAllBytes(tempFile));
      }
    }
    finally {
      Files.deleteIfExists(tempFile);
    }
  }

  class FileBlob
      extends BlobSupport
  {
    FileBlob(final BlobId blobId) {
      super(blobId);
    }

    @Override
    public InputStream getInputStream() {
      Path contentPath = contentPath(getId());
      try {
        checkExists(contentPath, getId());
        return new BufferedInputStream(fileOperations.openInputStream(contentPath));
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
  }

  private interface BlobIngester
  {
    StreamMetrics ingestTo(final Path destination) throws IOException;
  }

  @VisibleForTesting
  void setLiveBlobs(final LoadingCache<BlobId, FileBlob> liveBlobs) {
    this.liveBlobs = liveBlobs;
  }

  @Override
  public Stream<BlobId> getBlobIdStream() {
    try {
      return getAttributeFilePaths()
          .map(FileAttributesLocation::new)
          .map(this::getBlobIdFromAttributeFilePath)
          .map(BlobId::new);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    catch (IOException e) {
      log.error("Caught IOException during getDirectPathBlobIdStream for {}", prefix, e);
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  Path getContentDir() {
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
          .toString().replace(File.separatorChar, '/'); // guarantee we return unix-style paths
      return removeEnd(pathStr, BLOB_ATTRIBUTE_SUFFIX); // drop the .properties suffix
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
    Path blobPath = attributePath(blobId);
    try {
      FileBlobAttributes blobAttributes = new FileBlobAttributes(blobPath);
      if (!blobAttributes.load()) {
        log.warn("Attempt to access non-existent blob {} ({})", blobId, attributePath(blobId));
        return null;
      }
      else {
        return blobAttributes;
      }
    }
    catch (Exception e) {
        log.error("Unable to load BlobAttributes for blob id: {}, path: {}, exception: {}",
            blobId, blobPath, e.getMessage(), log.isDebugEnabled() ? e : null);
      return null;
    }
  }

  @Override
  public BlobAttributes getBlobAttributes(final FileAttributesLocation attributesFilePath) throws IOException {
    FileBlobAttributes fileBlobAttributes = new FileBlobAttributes(attributesFilePath.getPath());
    fileBlobAttributes.load();
    return fileBlobAttributes;
  }

  @Nullable
  private FileBlobAttributes getFileBlobAttributes(final BlobId blobId) {
    return (FileBlobAttributes) getBlobAttributes(blobId);
  }

  /**
   * Used by {@link #getDirectPathBlobIdStream(String)} to convert a blob "name" ({@link #toBlobName(Path)}) to
   * a {@link BlobId}.
   *
   * @see BlobIdLocationResolver
   */
  private BlobId toBlobId(String blobName) {
    Map<String, String> headers = ImmutableMap.of(
        BLOB_NAME_HEADER, blobName,
        DIRECT_PATH_BLOB_HEADER, "true"
    );
    return blobIdLocationResolver.fromHeaders(headers);
  }

  @Override
  public void setBlobAttributes(BlobId blobId, BlobAttributes blobAttributes) {
    try {
      FileBlobAttributes fileBlobAttributes = getFileBlobAttributes(blobId);
      fileBlobAttributes.updateFrom(blobAttributes);
      fileBlobAttributes.store();
    }
    catch (Exception e) {
      log.error("Unable to set BlobAttributes for blob id: {}, exception: {}",
          blobId, e.getMessage(), log.isDebugEnabled() ? e : null);
    }
  }
}
