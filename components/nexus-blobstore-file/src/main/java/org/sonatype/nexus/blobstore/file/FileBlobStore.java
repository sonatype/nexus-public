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
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.BlobSupport;
import org.sonatype.nexus.blobstore.LocationStrategy;
import org.sonatype.nexus.blobstore.StreamMetrics;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.file.internal.BlobCollisionException;
import org.sonatype.nexus.blobstore.file.internal.BlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.file.internal.FileOperations;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.io.DirectoryHelper;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import com.squareup.tape.QueueFile;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.cache.CacheLoader.from;
import static java.nio.file.Files.exists;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STOPPED;

/**
 * A {@link BlobStore} that stores its content on the file system.
 *
 * @since 3.0
 */
@Named(FileBlobStore.TYPE)
public class FileBlobStore
    extends StateGuardLifecycleSupport
    implements BlobStore
{
  public static final String BASEDIR = "blobs";

  public static final String TYPE = "File";

  public static final String BLOB_CONTENT_SUFFIX = ".bytes";

  public static final String BLOB_ATTRIBUTE_SUFFIX = ".properties";

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

  @VisibleForTesting
  public static final String TEMPORARY_BLOB_ID_PREFIX = "tmp$";

  private static final boolean RETRY_ON_COLLISION =
      SystemPropertiesHelper.getBoolean("nexus.blobstore.retryOnCollision", true);

  @VisibleForTesting
  static final int MAX_COLLISION_RETRIES = 8;

  private Path contentDir;

  private final LocationStrategy permanentLocationStrategy;

  private final LocationStrategy temporaryLocationStrategy;

  private final FileOperations fileOperations;

  private BlobStoreConfiguration blobStoreConfiguration;

  private final Path basedir;

  private BlobStoreMetricsStore storeMetrics;

  private LoadingCache<BlobId, FileBlob> liveBlobs;

  private QueueFile deletedBlobIndex;

  private final NodeAccess nodeAccess;

  private boolean supportsHardLinkCopy;

  private boolean supportsAtomicMove;

  @Inject
  public FileBlobStore(@Named("volume-chapter") final LocationStrategy permanentLocationStrategy,
                       @Named("temporary") final LocationStrategy temporaryLocationStrategy,
                       final FileOperations fileOperations,
                       final ApplicationDirectories directories,
                       final BlobStoreMetricsStore storeMetrics,
                       final NodeAccess nodeAccess)
  {
    this.permanentLocationStrategy = checkNotNull(permanentLocationStrategy);
    this.temporaryLocationStrategy = checkNotNull(temporaryLocationStrategy);
    this.fileOperations = checkNotNull(fileOperations);
    this.basedir = directories.getWorkDirectory(BASEDIR).toPath();
    this.storeMetrics = checkNotNull(storeMetrics);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.supportsHardLinkCopy = true;
    this.supportsAtomicMove = true;
  }

  @VisibleForTesting
  public FileBlobStore(final Path contentDir, //NOSONAR
                       @Named("volume-chapter") final LocationStrategy permanentLocationStrategy,
                       @Named("temporary") final LocationStrategy temporaryLocationStrategy,
                       final FileOperations fileOperations,
                       final BlobStoreMetricsStore storeMetrics,
                       final BlobStoreConfiguration configuration,
                       final ApplicationDirectories directories,
                       final NodeAccess nodeAccess)
  {
    this(permanentLocationStrategy, temporaryLocationStrategy, fileOperations, directories, storeMetrics, nodeAccess);
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
    storeMetrics.setStorageDir(storageDir);
    storeMetrics.start();
  }

  private void maybeUpgradeLegacyIndexFile(final Path deletedIndexPath) throws IOException {
    //While Path#getParent can return null we don't expect that from a configured blob store directory.
    Path legacyDeletionsIndex = deletedIndexPath.getParent().resolve(DELETIONS_FILENAME); //NOSONAR

    if (!exists(deletedIndexPath) && exists(legacyDeletionsIndex)) {
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
      storeMetrics.stop();
    }
  }

  /**
   * Returns path for blob-id content file relative to root directory.
   */
  @VisibleForTesting
  Path contentPath(final BlobId id) {
    return contentDir.resolve(getLocation(id) + BLOB_CONTENT_SUFFIX);
  }

  /**
   * Returns path for blob-id attribute file relative to root directory.
   */
  @VisibleForTesting
  Path attributePath(final BlobId id) {
    return contentDir.resolve(getLocation(id) + BLOB_ATTRIBUTE_SUFFIX);
  }

  /**
   * Returns a path for a temporary blob-id content file relative to root directory.
   */
  private Path temporaryContentPath(final BlobId id, final UUID suffix) {
    return contentDir.resolve(temporaryLocationStrategy.location(id) + "." + suffix + BLOB_CONTENT_SUFFIX);
  }

  /**
   * Returns path for a temporary blob-id attribute file relative to root directory.
   */
  private Path temporaryAttributePath(final BlobId id, final UUID suffix) {
    return contentDir.resolve(temporaryLocationStrategy.location(id) + "." + suffix + BLOB_ATTRIBUTE_SUFFIX);
  }

  /**
   * Returns the location for a blob ID based on whether or not the blob ID is for a temporary or permanent blob.
   */
  private String getLocation(final BlobId id) {
    if (id.asUniqueString().startsWith(TEMPORARY_BLOB_ID_PREFIX)) {
      return temporaryLocationStrategy.location(id);
    }
    return permanentLocationStrategy.location(id);
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final InputStream blobData, final Map<String, String> headers) {
    checkNotNull(blobData);

    return create(headers, destination -> fileOperations.create(destination, blobData));
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final Path sourceFile, final Map<String, String> headers, final long size, final HashCode sha1) {
    checkNotNull(sourceFile);
    checkNotNull(sha1);
    checkArgument(exists(sourceFile));

    return create(headers, destination -> {
      fileOperations.hardLink(sourceFile, destination);
      return new StreamMetrics(size, sha1.toString());
    });
  }

  private Blob create(final Map<String, String> headers, final BlobIngester ingester) {
    checkNotNull(headers);

    checkArgument(headers.containsKey(BLOB_NAME_HEADER), "Missing header: %s", BLOB_NAME_HEADER);
    checkArgument(headers.containsKey(CREATED_BY_HEADER), "Missing header: %s", CREATED_BY_HEADER);

    for (int retries = 0; retries <= MAX_COLLISION_RETRIES; retries++) {
      try {
        return tryCreate(headers, ingester);
      }
      catch (BlobCollisionException e) { // NOSONAR
        log.warn("BlobId collision: {} already exists{}", e.getBlobId(),
            retries < MAX_COLLISION_RETRIES ? ", retrying with new BlobId" : "!");
      }
    }

    throw new BlobStoreException("Cannot find free BlobId", null);
  }

  private Blob tryCreate(final Map<String, String> headers, final BlobIngester ingester) {

    // Generate a new blobId
    BlobId blobId;
    if (headers.containsKey(TEMPORARY_BLOB_HEADER)) {
      blobId = new BlobId(TEMPORARY_BLOB_ID_PREFIX + UUID.randomUUID().toString());
    }
    else {
      blobId = new BlobId(UUID.randomUUID().toString());
    }

    final Path blobPath = contentPath(blobId);
    final Path attributePath = attributePath(blobId);

    final UUID uuidSuffix = UUID.randomUUID();
    final Path temporaryBlobPath = temporaryContentPath(blobId, uuidSuffix);
    final Path temporaryAttributePath = temporaryAttributePath(blobId, uuidSuffix);

    final FileBlob blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      if (RETRY_ON_COLLISION && fileOperations.exists(blobPath)) {
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
        move(temporaryBlobPath, blobPath);
        move(temporaryAttributePath, attributePath);

        storeMetrics.recordAddition(blobAttributes.getMetrics().getContentSize());

        return blob;
      }
      catch (Exception e) {
        // Something went wrong, clean up the files we created
        deleteQuietly(temporaryAttributePath);
        deleteQuietly(temporaryBlobPath);
        deleteQuietly(attributePath);
        deleteQuietly(blobPath);
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
        });
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
    });
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobId blobId) {
    checkNotNull(blobId);

    final FileBlob blob = liveBlobs.getUnchecked(blobId);

    if (blob.isStale()) {
      Lock lock = blob.lock();
      try {
        if (blob.isStale()) {
          FileBlobAttributes blobAttributes = new FileBlobAttributes(attributePath(blobId));
          boolean loaded = blobAttributes.load();
          if (!loaded) {
            log.warn("Attempt to access non-existent blob {} ({})", blobId, blobAttributes.getPath());
            return null;
          }

          if (blobAttributes.isDeleted()) {
            log.warn("Attempt to access soft-deleted blob {} ({}), reason: {}", blobId, blobAttributes.getPath(), blobAttributes.getDeletedReason());
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
  @Guarded(by = STARTED)
  public boolean delete(final BlobId blobId, final String reason) {
    checkNotNull(blobId);

    final FileBlob blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      log.debug("Soft deleting blob {}", blobId);

      Path attribPath = attributePath(blobId);
      FileBlobAttributes blobAttributes = new FileBlobAttributes(attribPath);

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
  @Guarded(by = STARTED)
  public boolean deleteHard(final BlobId blobId) {
    checkNotNull(blobId);

    try {
      log.debug("Hard deleting blob {}", blobId);

      Path attributePath = attributePath(blobId);
      FileBlobAttributes blobAttributes = new FileBlobAttributes(attributePath);
      Long contentSize = getContentSizeForDeletion(blobAttributes);

      Path blobPath = contentPath(blobId);

      boolean blobDeleted = delete(blobPath);
      delete(attributePath);

      if (blobDeleted && contentSize != null) {
        storeMetrics.recordDeletion(contentSize);
      }

      return blobDeleted;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      liveBlobs.invalidate(blobId);
    }
  }

  @Nullable
  private Long getContentSizeForDeletion(final FileBlobAttributes blobAttributes) {
    try {
      blobAttributes.load();
      return blobAttributes.getMetrics() != null ? blobAttributes.getMetrics().getContentSize() : null;
    }
    catch (Exception e) {
      log.warn("Unable to load attributes {}, delete will not be added to metrics.", blobAttributes.getPath(), e);
      return null;
    }
  }


  @Override
  @Guarded(by = STARTED)
  public BlobStoreMetrics getMetrics() {
    return storeMetrics.getMetrics();
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void compact() {
    try {
      maybeRebuildDeletedBlobIndex();
      // only process each blob once (in-use blobs may be re-added to the index)
      for (int i = 0, numBlobs = deletedBlobIndex.size(); i < numBlobs; i++) {
        byte[] bytes = deletedBlobIndex.peek();
        if (bytes == null) {
          return;
        }
        deletedBlobIndex.remove();
        BlobId blobId = new BlobId(new String(bytes, StandardCharsets.UTF_8));
        FileBlob blob = liveBlobs.getIfPresent(blobId);
        if (blob == null || blob.isStale()) {
          // not in use, so it's safe to delete the file
          deleteHard(blobId);
        }
        else {
          // still in use, so move it to end of the queue
          deletedBlobIndex.add(bytes);
        }
      }
    }
    catch (BlobStoreException e) {
      throw e;
    }
    catch (Exception e) {
      throw new BlobStoreException(e, null);
    }
  }

  @Override
  public BlobStoreConfiguration getBlobStoreConfiguration() {
    return this.blobStoreConfiguration;
  }

  @Override
  public void init(final BlobStoreConfiguration configuration) {
    this.blobStoreConfiguration = configuration;
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

  private boolean delete(final Path path) throws IOException {
    boolean deleted = fileOperations.delete(path);
    if (deleted) {
      log.debug("Deleted {}", path);
    }
    else {
      log.error("No file to delete found at {}", path);
    }
    return deleted;
  }

  private void deleteQuietly(final Path path) {
    try {
      fileOperations.delete(path);
    }
    catch (IOException e) {
      log.warn("Blob store unable to delete {}", path, e);
    }
  }

  private void move(final Path source, final Path target) throws IOException {
    if (supportsAtomicMove) {
      try {
        fileOperations.moveAtomic(source, target);
        return;
      }
      catch (AtomicMoveNotSupportedException e) { // NOSONAR
        supportsAtomicMove = false;
        log.warn("Disabling atomic moves for blob store {}, could not move {} to {}, reason deleted: {}",
            blobStoreConfiguration.getName(), source, target, e.getReason());
      }
    }
    log.trace("Using normal move for blob store {}, moving {} to {}", blobStoreConfiguration.getName(), source, target);
    fileOperations.move(source, target);
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
        Stream.of(storeMetrics.listBackingFiles()).forEach(metricsFile -> deleteQuietly(metricsFile.toPath()));
        deleteQuietly(blobDir.resolve("metadata.properties"));
        Stream.of(blobDir.toFile().listFiles((dir, name) -> name.endsWith(DELETIONS_FILENAME)))
            .forEach(deletionIndex -> deleteQuietly(deletionIndex.toPath()));
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
      int softDeletedBlobsFound = getAttributeFilePaths()
          .map(FileBlobAttributes::new)
          .mapToInt(attributes -> {
            try {
              attributes.load();
              if (attributes.isDeleted()) {
                String blobId = getBlobIdFromAttributeFilePath(attributes.getPath());
                deletedBlobIndex.add(blobId.getBytes(StandardCharsets.UTF_8));
                return 1;
              }
            }
            catch (IOException e) {
              log.warn("Failed to add blobId to index from attribute file {}", attributes.getPath(), e);
            }
            return 0;
          })
          .sum();
      log.warn("Added {} soft deleted blob(s) to index file {}", softDeletedBlobsFound, deletedIndex);

      metadata.remove(REBUILD_DELETED_BLOB_INDEX_KEY);
      metadata.store();
    }
  }

  private String getBlobIdFromAttributeFilePath(final Path attributeFilePath) {
    String filename = attributeFilePath.toFile().getName();
    return filename.substring(0, filename.length() - BLOB_ATTRIBUTE_SUFFIX.length());
  }

  private Stream<Path> getAttributeFilePaths() throws IOException {
    return Files.walk(contentDir, FileVisitOption.FOLLOW_LINKS).filter(this::isNonTemporaryAttributeFile);
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
          .map(this::getBlobIdFromAttributeFilePath)
          .map(BlobId::new);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public BlobAttributes getBlobAttributes(final BlobId blobId) {
    try {
      FileBlobAttributes blobAttributes = new FileBlobAttributes(attributePath(blobId));
      return blobAttributes.load() ? blobAttributes : null;
    }
    catch (IOException e) {
      log.error("Unable to load FileBlobAttributes for blob id: {}", blobId, e);
      return null;
    }
  }
}
