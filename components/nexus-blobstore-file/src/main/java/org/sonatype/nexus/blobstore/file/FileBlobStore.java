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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreListener;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.file.FileOperations.StreamMetrics;
import org.sonatype.nexus.blobstore.file.internal.FileBlobMetadataStoreImpl;
import org.sonatype.nexus.common.collect.AutoClosableIterable;
import org.sonatype.nexus.common.io.DirSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link BlobStore} that stores its content on the file system, and metadata in a {@link FileBlobMetadataStore}.
 *
 * @since 3.0
 */
@Named(FileBlobStore.TYPE)
public class FileBlobStore
    extends LifecycleSupport
    implements BlobStore
{
  public static final String TYPE = "File";

  public static final String BLOB_CONTENT_SUFFIX = ".blob";

  @VisibleForTesting
  static final String CONFIG_KEY = "file";

  @VisibleForTesting
  static final String PATH_KEY = "path";

  private Path root;

  private FileBlobMetadataStore metadataStore;

  private final LocationStrategy locationStrategy;

  private final FileOperations fileOperations;

  private volatile BlobStoreListener listener;

  private BlobStoreConfiguration blobStoreConfiguration;

  @Inject
  public FileBlobStore(final LocationStrategy locationStrategy,
                       final FileOperations fileOperations)
  {
    this.locationStrategy = checkNotNull(locationStrategy);
    this.fileOperations = checkNotNull(fileOperations);
  }

  @VisibleForTesting
  public FileBlobStore(final Path root,
                       final LocationStrategy locationStrategy,
                       final FileOperations fileOperations,
                       final FileBlobMetadataStore metadataStore,
                       final BlobStoreConfiguration configuration)
  {
    this(locationStrategy, fileOperations);
    this.root = checkNotNull(root);
    this.metadataStore = checkNotNull(metadataStore);
    this.blobStoreConfiguration = checkNotNull(configuration);
  }

  @Override
  protected void doStart() throws Exception {
    metadataStore.start();
  }

  @Override
  protected void doStop() throws Exception {
    metadataStore.stop();
  }

  @Override
  public void setBlobStoreListener(@Nullable final BlobStoreListener listener) {
    this.listener = listener;
  }

  @Nullable
  @Override
  public BlobStoreListener getBlobStoreListener() {
    return listener;
  }

  /**
   * Returns path for blob-id content file relative to root directory.
   */
  private Path pathFor(final BlobId id) {
    String location = locationStrategy.location(id);
    return root.resolve(location + BLOB_CONTENT_SUFFIX);
  }

  @Override
  public Blob create(final InputStream blobData, final Map<String, String> headers) {
    checkNotNull(blobData);
    checkNotNull(headers);

    checkArgument(headers.containsKey(BLOB_NAME_HEADER), "Missing header: %s", BLOB_NAME_HEADER);
    checkArgument(headers.containsKey(CREATED_BY_HEADER), "Missing header: %s", CREATED_BY_HEADER);

    BlobId blobId = null;

    try {
      // If the storing of bytes fails, we record a reminder to clean up afterwards
      final FileBlobMetadata metadata = new FileBlobMetadata(FileBlobState.CREATING, headers);
      blobId = metadataStore.add(metadata);

      final Path path = pathFor(blobId);
      log.debug("Writing blob {} to {}", blobId, path);

      final StreamMetrics streamMetrics = fileOperations.create(path, blobData);
      final BlobMetrics metrics = new BlobMetrics(new DateTime(), streamMetrics.getSHA1(), streamMetrics.getSize());
      final FileBlob blob = new FileBlob(blobId, headers, path, metrics);

      if (listener != null) {
        listener.blobCreated(blob, "Blob: " + blobId + " written to: " + path);
      }

      metadata.setMetrics(metrics);
      // Storing the content went fine, so we can now unmark this for deletion
      metadata.setBlobState(FileBlobState.ALIVE);
      metadataStore.update(blobId, metadata);

      return blob;
    }
    catch (IOException e) {
      throw new BlobStoreException(e, blobId);
    }
  }

  @Nullable
  @Override
  public Blob get(final BlobId blobId) {
    checkNotNull(blobId);

    FileBlobMetadata metadata = metadataStore.get(blobId);
    if (metadata == null) {
      log.debug("Attempt to access non-existent blob {}", blobId);
      return null;
    }

    if (!metadata.isAlive()) {
      log.debug("Attempt to access blob {} in state {}", blobId, metadata.getBlobState());
      return null;
    }

    final FileBlob blob = new FileBlob(blobId, metadata.getHeaders(), pathFor(blobId), metadata.getMetrics());

    log.debug("Accessing blob {}", blobId);
    if (listener != null) {
      listener.blobAccessed(blob, null);
    }
    return blob;
  }

  @Override
  public boolean delete(final BlobId blobId) {
    checkNotNull(blobId);

    FileBlobMetadata metadata = metadataStore.get(blobId);
    if (metadata == null) {
      log.debug("Attempt to mark-for-delete non-existent blob {}", blobId);
      return false;
    }
    else if (!metadata.isAlive()) {
      log.debug("Attempt to delete blob {} in state {}", blobId, metadata.getBlobState());
      return false;
    }

    metadata.setBlobState(FileBlobState.MARKED_FOR_DELETION);
    // TODO: Handle concurrent modification of metadata
    metadataStore.update(blobId, metadata);
    return true;
  }

  @Override
  public boolean deleteHard(final BlobId blobId) {
    checkNotNull(blobId);

    FileBlobMetadata metadata = metadataStore.get(blobId);
    if (metadata == null) {
      log.debug("Attempt to deleteHard non-existent blob {}", blobId);
      return false;
    }

    try {
      final Path path = pathFor(blobId);
      final boolean blobDeleted = fileOperations.delete(path);

      if (!blobDeleted) {
        log.error("Deleting blob {} : content file was missing", blobId);
      }

      log.debug("Deleting-hard blob {}", blobId);

      if (listener != null) {
        listener.blobDeleted(blobId, "Path: " + path);
      }

      metadataStore.delete(blobId);

      return blobDeleted;
    }
    catch (IOException e) {
      throw new BlobStoreException(e, blobId);
    }
  }

  @Override
  public BlobStoreMetrics getMetrics() {
    return new BlobStoreMetrics()
    {
      @Override
      public long getBlobCount() {
        return metadataStore.getBlobCount();
      }

      @Override
      public long getTotalSize() {
        return metadataStore.getTotalSize();
      }

      @Override
      public long getAvailableSpace() {
        try {
          final FileStore fileStore = Files.getFileStore(root);
          return fileStore.getUsableSpace();
        }
        catch (IOException e) {
          throw new BlobStoreException(e, null);
        }
      }
    };
  }

  @Override
  public void compact() {
    log.debug("Compacting");

    try {
      int count = 0;
      try (AutoClosableIterable<BlobId> iter = metadataStore.findWithState(FileBlobState.MARKED_FOR_DELETION)) {
        for (BlobId blobId : iter) {
          deleteHard(blobId);
          count++;
        }
      }

      metadataStore.compact();

      log.debug("Deleted {} blobs", count);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public BlobStoreConfiguration getBlobStoreConfiguration() {
    return this.blobStoreConfiguration;
  }

  @Override
  public void init(final BlobStoreConfiguration configuration) {
    this.blobStoreConfiguration = configuration;
    Path blobDir = getConfiguredBlobDir();
    try {
      Path content = blobDir.resolve("content");
      File metadataFile = blobDir.resolve("metadata").toFile();
      DirSupport.mkdir(content);
      DirSupport.mkdir(metadataFile);
      this.root = content;
      this.metadataStore = FileBlobMetadataStoreImpl.create(metadataFile);
    }
    catch (Exception e) {
      throw new BlobStoreException(String.format("Unable to initialize blob store directory structure: %s", blobDir), e, null);
    }
  }

  @Override
  public AutoClosableIterable<BlobId> iterator() {
    return metadataStore.findWithState(FileBlobState.ALIVE);
  }

  private void checkExists(final Path path, final BlobId blobId) throws IOException {
    if (!fileOperations.exists(path)) {
      // I'm not completely happy with this, since it means that blob store clients can get a blob, be satisfied
      // that it exists, and then discover that it doesn't, mid-operation
      throw new BlobStoreException("Blob has been deleted", blobId);
    }
  }

  public static Map<String, Map<String, Object>> attributes(final String path) {
    Map<String, Map<String, Object>> map = Maps.newHashMap();
    HashMap<String, Object> attributes = Maps.newHashMap();
    attributes.put("path", path);
    map.put("file", attributes);
    return map;
  }

  public static BlobStoreConfiguration configure(final String name, final String path) {
    BlobStoreConfiguration configuration = new BlobStoreConfiguration();
    configuration.setName(name);
    configuration.setType(FileBlobStore.TYPE);
    configuration.attributes(CONFIG_KEY).set(PATH_KEY, path);
    return configuration;
  }

  /**
   * Recursively delete everything in the blob store's configured directory.
   */
  @Override
  public void remove() {
    try {
      fileOperations.deleteDirectory(getConfiguredBlobDir());
    }
    catch (IOException e) {
      throw new BlobStoreException(e, null);
    }
  }

  private Path getConfiguredBlobDir()
  {
    return Paths.get(String.valueOf(blobStoreConfiguration.attributes(CONFIG_KEY).require(PATH_KEY)));
  }

  class FileBlob
      implements Blob
  {
    private final BlobId blobId;

    private final Map<String, String> headers;

    private final Path contentPath;

    private final BlobMetrics metrics;

    FileBlob(final BlobId blobId,
             final Map<String, String> headers,
             final Path contentPath,
             final BlobMetrics metrics)
    {
      this.blobId = checkNotNull(blobId);
      this.headers = checkNotNull(headers);
      this.contentPath = checkNotNull(contentPath);
      this.metrics = checkNotNull(metrics);
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
      try {
        checkExists(contentPath, blobId);
        return fileOperations.openInputStream(contentPath);
      }
      catch (IOException e) {
        throw new BlobStoreException(e, blobId);
      }
    }

    @Override
    public BlobMetrics getMetrics() {
      return metrics;
    }
  }
}
