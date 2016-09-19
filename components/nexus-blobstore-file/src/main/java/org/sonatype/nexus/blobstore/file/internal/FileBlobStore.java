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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.Locks;
import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.file.internal.FileOperations.StreamMetrics;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.io.DirectoryHelper;
import org.sonatype.nexus.common.property.PropertiesFile;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import com.squareup.tape.QueueFile;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.cache.CacheLoader.from;

/**
 * A {@link BlobStore} that stores its content on the file system.
 *
 * @since 3.0
 */
@Named(FileBlobStore.TYPE)
public class FileBlobStore
    extends LifecycleSupport
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
  public static final String DELETIONS_FILENAME = "deletions.index";

  private Path contentDir;

  private final LocationStrategy locationStrategy;

  private final FileOperations fileOperations;

  private BlobStoreConfiguration blobStoreConfiguration;

  private final Path basedir;

  private BlobStoreMetricsStore storeMetrics;

  private LoadingCache<BlobId, FileBlob> liveBlobs;

  private QueueFile deletedBlobIndex;

  @Inject
  public FileBlobStore(final LocationStrategy locationStrategy,
                       final FileOperations fileOperations,
                       final ApplicationDirectories directories,
                       final BlobStoreMetricsStore storeMetrics)
  {
    this.locationStrategy = checkNotNull(locationStrategy);
    this.fileOperations = checkNotNull(fileOperations);
    this.basedir = directories.getWorkDirectory(BASEDIR).toPath();
    this.storeMetrics = checkNotNull(storeMetrics);
  }

  @VisibleForTesting
  public FileBlobStore(final Path contentDir,
                       final LocationStrategy locationStrategy,
                       final FileOperations fileOperations,
                       final BlobStoreMetricsStore storeMetrics,
                       final BlobStoreConfiguration configuration,
                       final ApplicationDirectories directories)
  {
    this(locationStrategy, fileOperations, directories, storeMetrics);
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
    deletedBlobIndex = new QueueFile(storageDir.resolve(DELETIONS_FILENAME).toFile());
    storeMetrics.setStorageDir(storageDir);
    storeMetrics.start();
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
  private Path contentPath(final BlobId id) {
    String location = locationStrategy.location(id);
    return contentDir.resolve(location + BLOB_CONTENT_SUFFIX);
  }

  private Path attributePath(final BlobId id) {
    String location = locationStrategy.location(id);
    return contentDir.resolve(location + BLOB_ATTRIBUTE_SUFFIX);
  }

  @Override
  public Blob create(final InputStream blobData, final Map<String, String> headers) {
    checkNotNull(blobData);

    return create(headers, destination -> fileOperations.create(destination, blobData));
  }

  @Override
  public Blob create(final Path sourceFile, final Map<String, String> headers, final long size, final HashCode sha1) {
    checkNotNull(sourceFile);
    checkNotNull(sha1);
    checkArgument(Files.exists(sourceFile));

    return create(headers, destination -> {
      fileOperations.hardLink(sourceFile, destination);
      return new StreamMetrics(size, sha1.toString());
    });
  }

  private Blob create(final Map<String, String> headers, final BlobIngester ingester) {
    checkNotNull(headers);

    checkArgument(headers.containsKey(BLOB_NAME_HEADER), "Missing header: %s", BLOB_NAME_HEADER);
    checkArgument(headers.containsKey(CREATED_BY_HEADER), "Missing header: %s", CREATED_BY_HEADER);

    // Generate a new blobId
    BlobId blobId = new BlobId(UUID.randomUUID().toString());

    final Path blobPath = contentPath(blobId);
    final Path attributePath = attributePath(blobId);

    final FileBlob blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      log.debug("Writing blob {} to {}", blobId, blobPath);

      final StreamMetrics streamMetrics = ingester.ingestTo(blobPath);
      final BlobMetrics metrics = new BlobMetrics(new DateTime(), streamMetrics.getSha1(), streamMetrics.getSize());
      blob.refresh(headers, metrics);

      // Write the blob attribute file
      BlobAttributes blobAttributes = new BlobAttributes(attributePath, headers, metrics);
      blobAttributes.store();

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

  @Nullable
  @Override
  public Blob get(final BlobId blobId) {
    checkNotNull(blobId);

    final FileBlob blob = liveBlobs.getUnchecked(blobId);

    if (blob.isStale()) {
      Lock lock = blob.lock();
      try {
        if (blob.isStale()) {
          BlobAttributes blobAttributes = new BlobAttributes(attributePath(blobId));
          boolean loaded = blobAttributes.load();
          if (!loaded) {
            log.debug("Attempt to access non-existent blob {}", blobId);
            return null;
          }

          if (blobAttributes.isDeleted()) {
            log.debug("Attempt to get deleted blob {}", blobId);
            return null;
          }

          blob.refresh(blobAttributes.getHeaders(), blobAttributes.getMetrics());
        }
      }
      catch (IOException e) {
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
  public boolean delete(final BlobId blobId) {
    checkNotNull(blobId);

    final FileBlob blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
      Path attribPath = attributePath(blobId);
      BlobAttributes blobAttributes = new BlobAttributes(attribPath);

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
      blobAttributes.store();

      // record blob for hard-deletion when the next compact task runs
      deletedBlobIndex.add(blobId.toString().getBytes(Charsets.UTF_8));
      blob.markStale();

      // TODO: should we only update the size when doing a hard delete?
      storeMetrics.recordDeletion(blobAttributes.getMetrics().getContentSize());

      return true;
    }
    catch (IOException e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public boolean deleteHard(final BlobId blobId) {
    checkNotNull(blobId);

    try {
      delete(attributePath(blobId));

      Path blobPath = contentPath(blobId);
      boolean blobDeleted = delete(blobPath);

      log.debug("Deleting-hard blob {}", blobId);

      return blobDeleted;
    }
    catch (IOException e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      liveBlobs.invalidate(blobId);
    }
  }

  @Override
  public BlobStoreMetrics getMetrics() {
    return storeMetrics.getMetrics();
  }

  @Override
  public void compact() {
    try {
      // only process each blob once (in-use blobs may be re-added to the index)
      for (int i = 0, numBlobs = deletedBlobIndex.size(); i < numBlobs; i++) {
        synchronized (deletedBlobIndex) {
          byte[] bytes = deletedBlobIndex.peek();
          if (bytes == null) {
            return;
          }
          deletedBlobIndex.remove();
          BlobId blobId = new BlobId(new String(bytes, Charsets.UTF_8));
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
    }
    catch (IOException e) {
      log.warn("Problem maintaining deletions index for: {}", getConfiguredBlobStorePath());
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
  public void remove() {
    try {
      Path blobDir = getAbsoluteBlobDir();
      if (fileOperations.deleteEmptyDirectory(contentDir)) {
        deleteQuietly(blobDir.resolve("metrics.properties"));
        deleteQuietly(blobDir.resolve("metadata.properties"));
        deleteQuietly(blobDir.resolve("deletions.index"));
        if (!fileOperations.deleteEmptyDirectory(blobDir)) {
          log.warn("Unable to delete non-empty blob store directory {}", blobDir);
        }
      }
      else {
        log.warn("Unable to delete non-empty blob store content directory {}", contentDir);
      }
    }
    catch (IOException e) {
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

  class FileBlob
      implements Blob
  {
    private final BlobId blobId;

    private final Lock lock;

    private Map<String, String> headers;

    private BlobMetrics metrics;

    private volatile boolean stale;

    FileBlob(final BlobId blobId) {
      this.blobId = checkNotNull(blobId);
      lock = new ReentrantLock();
      stale = true;
    }

    void refresh(final Map<String, String> headers, final BlobMetrics metrics) {
      this.headers = checkNotNull(headers);
      this.metrics = checkNotNull(metrics);
      stale = false;
    }

    void markStale() {
      stale = true;
    }

    boolean isStale() {
      return stale;
    }

    @Override
    public BlobId getId() {
      return blobId;
    }

    @Override
    public Map<String, String> getHeaders() {
      return headers;
    }

    @Override
    public InputStream getInputStream() {
      Path contentPath = contentPath(blobId);
      try {
        checkExists(contentPath, blobId);
        return new BufferedInputStream(fileOperations.openInputStream(contentPath));
      }
      catch (IOException e) {
        throw new BlobStoreException(e, blobId);
      }
    }

    @Override
    public BlobMetrics getMetrics() {
      return metrics;
    }

    Lock lock() {
      return Locks.lock(lock);
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
}
