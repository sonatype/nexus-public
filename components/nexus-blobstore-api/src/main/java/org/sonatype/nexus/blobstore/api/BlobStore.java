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
package org.sonatype.nexus.blobstore.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsService;
import org.sonatype.nexus.transaction.TransactionalStore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import org.slf4j.Logger;

/**
 * A generic storage bin for binary objects of all sizes.
 * <p>
 * In general, most methods can throw {@link BlobStoreException} for conditions such as network connectivity problems,
 * or file IO issues, blob store misconfiguration, or internal corruption.
 *
 * @since 3.0
 */
public interface BlobStore
    extends TransactionalStore<BlobSession<?>>, Lifecycle
{
  /**
   * Suffix of the file that contains the bytes
   */
  String BLOB_FILE_CONTENT_SUFFIX = ".bytes";

  /**
   * Suffix of the file that contains the attributes
   */
  String BLOB_FILE_ATTRIBUTES_SUFFIX = ".properties";

  /**
   * An identifying name for disaster recovery purposes (which isn't required to be strictly unique)
   */
  String BLOB_NAME_HEADER = "BlobStore.blob-name";

  /**
   * An informational header for disaster recovery purposes (describes what blob contains)
   */
  String CONTENT_TYPE_HEADER = "BlobStore.content-type";

  /**
   * Audit information (e.g. the name of a principal that created the blob)
   */
  String CREATED_BY_HEADER = "BlobStore.created-by";

  /**
   * Audit information (e.g. the ip of a principal that created the blob)
   *
   * @since 3.6.1
   */
  String CREATED_BY_IP_HEADER = "BlobStore.created-by-ip";

  /**
   * Header whose presence indicates a temporary blob (may be handled differently by the underlying implementation).
   *
   * @since 3.1
   */
  String TEMPORARY_BLOB_HEADER = "BlobStore.temporary-blob";

  /**
   * Header that indicates this blob uses "direct paths." A direct path blob is blob that has a computable, predictable
   * {@link BlobId} reflecting a path like structure. If the create methods in this class are called with a header with
   * this key and a value of "true", the blob will be stored in the blob store using a direct file system path, using
   * {@link #BLOB_NAME_HEADER} header for the tail.
   * <p>
   * For example, if the {@link #BLOB_NAME_HEADER} contains a value like "path/to/index.html", the blob will be stored
   * on disk within the blob store at a path that terminates in "path/to/index.html.bytes". Note: direct-path blobs only
   * use the unix-style path separator ('/'), even if the underlying filesystem is not.
   * <p>
   * Use this feature for Blobs that:
   *
   * <ul>
   * <li>will not or cannot have a database table mapping generated {@link BlobId} to paths</li>
   * <li>can be overwritten on "disk" without side effects</li>
   * </ul>
   *
   * @since 3.8
   */
  String DIRECT_PATH_BLOB_HEADER = "BlobStore.direct-path";

  /**
   * An associated repository name for disaster recovery purposes (which isn't required to be strictly unique)
   * <p>
   * (uses 'Bucket' prefix for legacy compatibility reasons)
   *
   * @since 3.25
   */
  String REPO_NAME_HEADER = "Bucket.repo-name";

  /**
   * Creates a new blob. The header map must contain at least two keys:
   *
   * <ul>
   * <li>{@link #BLOB_NAME_HEADER}</li>
   * <li>{@link #CREATED_BY_HEADER}</li>
   * </ul>
   * <p>
   * Note: if headers contains an entry with key {@link #DIRECT_PATH_BLOB_HEADER} and value true, and the
   * {@link #BLOB_NAME_HEADER} matches a direct-path blob that already exists, the blob will be overwritten.
   *
   * @throws BlobStoreException (or a subclass) if the input stream can't be read correctly
   * @throws IllegalArgumentException if mandatory headers are missing
   */
  Blob create(InputStream blobData, Map<String, String> headers);

  /**
   * Creates a new blob with the provided {@link BlobId}.
   *
   * @since 3.15
   */
  Blob create(InputStream blobData, Map<String, String> headers, @Nullable BlobId blobId);

  /**
   * Imports a blob by creating a hard link, throwing {@link BlobStoreException} if that's not supported from the source
   * file's location.
   * <p>
   * Otherwise similar to {@link #create(InputStream, Map)} with the difference that a known file size and sha1 are
   * already provided.
   *
   * @since 3.1
   */
  Blob create(Path sourceFile, Map<String, String> headers, long size, HashCode sha1);

  /**
   * Creates the new blob attributes
   *
   * @param blobId the blob id
   * @param headers the headers of the blob, {@see BlobStore}
   * @param blobMetrics blob metrics
   */
  default void createBlobAttributes(BlobId blobId, Map<String, String> headers, BlobMetrics blobMetrics) {
  }

  /**
   * Creates the new blob attributes instance
   *
   * @param blobId the blob id
   * @param headers the headers of the blob, {@see BlobStore}
   * @param metrics blob metrics
   */
  BlobAttributes createBlobAttributesInstance(BlobId blobId, Map<String, String> headers, BlobMetrics metrics);

  /**
   * Duplicates a blob within the blob store by copying the temp blob but with the provided headers. The blob must be in
   * this blob store; moving blobs between blob stores is not supported.
   *
   * @since 3.1
   */
  Blob copy(BlobId blobId, Map<String, String> headers);

  /**
   * Makes a blob permanent by writing the specified permanent blob headers into the blob's properties file.
   *
   * @since 3.37
   */
  default Blob makeBlobPermanent(final BlobId blobId, final Map<String, String> headers) {
    return copy(blobId, headers); // default to copy for non-cloud blob stores
  }

  /**
   * Returns the corresponding {@link Blob}, or {@code null} if the blob does not exist or has been
   * {@link #delete deleted}.
   */
  @Nullable
  Blob get(BlobId blobId);

  /**
   * Returns the corresponding {@link Blob}, or {@code null} if the blob does not exist, or has been
   * {@link #delete deleted} and {@code includeDeleted} is {@code false}).
   */
  @Nullable
  Blob get(BlobId blobId, boolean includeDeleted);

  /**
   * Performs a simple existence check using the attributes path returning {@code true} if it exists and {@code false}
   * if it does not.
   * <p>
   * This was introduced to allow existence checking of direct-path blobs in support of edge cases such as RHC.
   */
  boolean exists(BlobId blobId);

  /**
   * Performs a simple existence check for {@code .bytes} for given {@code blobId}.
   *
   * @return {@code true} if it exists and {@code false} if it does not.
   */
  boolean bytesExists(BlobId blobId);

  /**
   * Performs a simple existence and empty size check for {@code .bytes} for given {@code blobId}.
   *
   * @return {@code true} if the blobstore is exists and empty {@code false} if it does not.
   */
  boolean isBlobEmpty(BlobId blobId);

  /**
   * Removes a blob from the blob store. This may not immediately delete the blob from the underlying storage
   * mechanism, but will make it immediately unavailable to future calls to {@link BlobStore#get(BlobId)}.
   *
   * @return {@code true} if the blob has been deleted, {@code false} if no blob was found by that ID.
   */
  boolean delete(BlobId blobId, String reason);

  /**
   * Removes a blob from the blob store immediately, disregarding any locking or concurrent access by other threads.
   * This should be considered exceptional (e.g. administrative) usage.
   *
   * @return {@code true} if the blob has been deleted, {@code false} if no blob was found by that ID.
   */
  boolean deleteHard(BlobId blobId);

  /**
   * Provides access to the blob store's metrics service instance
   * <p>
   * Note: this method may not be available for all the blob store implementations and, if not supported, expect a
   * {@link UnsupportedOperationException}
   * </p>
   */
  <B extends BlobStore> BlobStoreMetricsService<B> getMetricsService();

  /**
   * Provides an immutable snapshot of metrics about the BlobStore's usage.
   */
  BlobStoreMetrics getMetrics();

  /**
   * A map of blob storage metrics by operation type which are filled by (@code BlobStoreAnalyticsInterceptor)
   */
  Map<OperationType, OperationMetrics> getOperationMetricsByType();

  /**
   * Map of operation metrics that to be updated with changes as they happen.
   */
  Map<OperationType, OperationMetrics> getOperationMetricsDelta();

  /**
   * Clears the operational metrics of the blobstore.
   */
  void clearOperationMetrics();

  /**
   * Perform garbage collection, purging blobs marked for deletion or whatever other periodic, implementation-specific
   * tasks need doing. Takes an optional {@link BlobStoreUsageChecker} and an optional {@link Logger} from the caller.
   *
   * @since 3.5
   */
  void compact(@Nullable BlobStoreUsageChecker inUseChecker);

  /**
   * Delete blob temporary files
   */
  void deleteTempFiles(Integer daysOlderThan);

  /**
   * Returns the configuration entity for the BlobStore.
   */
  BlobStoreConfiguration getBlobStoreConfiguration();

  /**
   * Initialize the BlobStore.
   */
  void init(BlobStoreConfiguration configuration) throws Exception;

  /**
   * Signifies that the {@link BlobStoreManager} has permanently deleted this blob store.
   */
  void remove();

  /**
   * Get a {@link Stream} of {@link BlobId} for blobs contained in this blob store.
   */
  Stream<BlobId> getBlobIdStream();

  /**
   * Get a {@link Stream} of {@link BlobId} for blobs contained in this blob store that have been updated within the
   * provided duration.
   */
  Stream<BlobId> getBlobIdUpdatedSinceStream(Duration duration);

  /**
   * Get a {@link Stream} of {@link BlobId} for blobs contained in this blob store that have been updated within the
   * provided date range and under the specified path prefix.
   */
  PaginatedResult<BlobId> getBlobIdUpdatedSinceStream(
      String prefix,
      OffsetDateTime fromDateTime,
      OffsetDateTime toDateTime,
      @Nullable String continuationToken,
      int pageSize);

  /**
   * Get a {@link Stream} of direct-path {@link BlobId}s under the specified path prefix.
   */
  Stream<BlobId> getDirectPathBlobIdStream(String prefix);

  /**
   * Get {@link BlobAttributes} for the {@link BlobId} provided.
   */
  @Nullable
  BlobAttributes getBlobAttributes(BlobId blobId);

  /**
   * Set {@link BlobAttributes} for the {@link BlobId} provided.
   *
   * @since 3.7
   */
  void setBlobAttributes(BlobId blobId, BlobAttributes blobAttributes);

  /**
   * Undeletes a soft deleted blob, if possible.
   *
   * @return {@code true} if the blob has been successfully undeleted.
   * @since 3.12
   */
  boolean undelete(
      @Nullable BlobStoreUsageChecker inUseChecker,
      BlobId blobId,
      BlobAttributes attributes,
      boolean isDryRun);

  /**
   * Identifies if the storage backed by the instance is available to be written to
   *
   * @return {@code true} if the blob store can be written to
   * @since 3.14
   */
  boolean isStorageAvailable();

  /**
   * Identifies if the instance can be a member of a group
   *
   * @return {@code true} if the blob store can be a member of a group
   * @since 3.14
   */
  default boolean isGroupable() {
    return true;
  }

  /**
   * Identifies if the instance is writable. The writable state is a configuration option and not representative of the
   * underlying storage implementation. To communicate the underlying implementations status see
   * {@link #isStorageAvailable()}
   *
   * @return {@code true} if the blob store is writable
   * @since 3.15
   */
  default boolean isWritable() {
    return getBlobStoreConfiguration().isWritable();
  }

  /**
   * Returns true if the blobstore has been started.
   *
   * @return {@code true} if the blobstore has been started.
   * @since 3.15
   */
  boolean isStarted();

  /**
   * Returns true if the blobstore has no blobs within it.
   *
   * @return {@code true} if the blobstore has no blobs within it.
   * @since 3.17
   */
  boolean isEmpty();

  /**
   * Permanently stops this blob store.
   *
   * @since 3.22
   */
  void shutdown() throws Exception;

  /**
   * Acts as a {@code BlobStore::hardDelete}, except it may be executed asynchronously.
   *
   * @since 3.29
   */
  default Future<Boolean> asyncDelete(BlobId blobId) {
    return CompletableFuture.completedFuture(deleteHard(blobId));
  }

  /**
   * Deletes the blob if it is indeed a Temporary blob Note: This method should only called with a BlobId known to have
   * been created as a TempBlob, implementations may perform no checks if they provide no special handling.
   *
   * @since 3.37
   */
  default boolean deleteIfTemp(BlobId blobId) {
    return deleteHard(blobId);
  }

  default void validateCanCreateAndUpdate() throws Exception {
  }

  /**
   * Flush blobstore metrics to disk, forgoing the typical wait period
   *
   * @throws IOException
   */
  @VisibleForTesting
  default void flushMetrics() throws IOException {
    // default impl does nothing
  }

  /**
   * @since 3.31
   */
  RawObjectAccess getRawObjectAccess();
}
