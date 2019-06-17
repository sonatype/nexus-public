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
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.BlobIdLocationResolver;
import org.sonatype.nexus.blobstore.BlobStoreSupport;
import org.sonatype.nexus.blobstore.BlobSupport;
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
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.stateguard.Guarded;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
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
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.blobstore.DirectPathLocationStrategy.DIRECT_PATH_ROOT;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreConfigurationHelper.getConfiguredExpirationInDays;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STOPPED;

/**
 * A {@link BlobStore} that stores its content on AWS S3.
 *
 * @since 3.6.1
 */
@Named(S3BlobStore.TYPE)
public class S3BlobStore
    extends BlobStoreSupport<S3AttributesLocation>
{
  public static final String TYPE = "S3";

  public static final String BLOB_CONTENT_SUFFIX = ".bytes";

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

  static final String LIFECYCLE_EXPIRATION_RULE_ID = "Expire soft-deleted blobstore objects";

  private static final String FILE_V1 = "file/1";

  private final AmazonS3Factory amazonS3Factory;

  private final BucketManager bucketManager;

  private S3Uploader uploader;

  private S3Copier copier;

  private S3BlobStoreMetricsStore storeMetrics;

  private LoadingCache<BlobId, S3Blob> liveBlobs;

  private AmazonS3 s3;

  @Inject
  public S3BlobStore(final AmazonS3Factory amazonS3Factory,
                     final BlobIdLocationResolver blobIdLocationResolver,
                     @Named("multipart-uploader") final S3Uploader uploader,
                     final S3Copier copier,
                     final S3BlobStoreMetricsStore storeMetrics,
                     final DryRunPrefix dryRunPrefix,
                     final BucketManager bucketManager)
  {
    super(blobIdLocationResolver, dryRunPrefix);
    this.amazonS3Factory = checkNotNull(amazonS3Factory);
    this.uploader = checkNotNull(uploader);
    this.copier = checkNotNull(copier);
    this.storeMetrics = checkNotNull(storeMetrics);
    this.bucketManager = checkNotNull(bucketManager);
  }

  @Override
  protected void doStart() throws Exception {
    // ensure blobstore is supported
    S3PropertiesFile metadata = new S3PropertiesFile(s3, getConfiguredBucket(), metadataFilePath());
    if (metadata.exists()) {
      metadata.load();
      String type = metadata.getProperty(TYPE_KEY);
      checkState(TYPE_V1.equals(type) || FILE_V1.equals(type), "Unsupported blob store type/version: %s in %s", type, metadata);
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
  }

  @Override
  protected void doStop() throws Exception {
    liveBlobs = null;
    storeMetrics.stop();
  }

  /**
   * Returns path for blob-id content file relative to root directory.
   */
  private String contentPath(final BlobId id) {
    return getLocation(id) + BLOB_CONTENT_SUFFIX;
  }

  private String metadataFilePath() {
    return getBucketPrefix() + METADATA_FILENAME;
  }

  /**
   * Returns path for blob-id attribute file relative to root directory.
   */
  private String attributePath(final BlobId id) {
    return getLocation(id) + BLOB_ATTRIBUTE_SUFFIX;
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
  protected Blob doCreate(final InputStream blobData, final Map<String, String> headers, @Nullable final BlobId blobId) {
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
  public Blob create(final Path sourceFile, final Map<String, String> headers, final long size, final HashCode sha1) {
    throw new BlobStoreException("hard links not supported", null);
  }

  private Blob create(final Map<String, String> headers,
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

      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath, headers, metrics);

      blobAttributes.store();
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

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobId blobId) {
    return get(blobId, false);
  }

  @Nullable
  @Override
  @Timed
  public Blob get(final BlobId blobId, final boolean includeDeleted) {
    checkNotNull(blobId);

    final S3Blob blob = liveBlobs.getUnchecked(blobId);

    if (blob.isStale()) {
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
  protected boolean doDelete(final BlobId blobId, final String reason) {
    if (deleteByExpire()) {
      return expire(blobId, reason);
    }
    else {
      return doDeleteHard(blobId);
    }
  }

  private boolean deleteByExpire() {
    return getConfiguredExpirationInDays(blobStoreConfiguration) != NO_AUTOMATIC_EXPIRY_HARD_DELETE;
  }

  private boolean expire(final BlobId blobId, final String reason) {
    final S3Blob blob = liveBlobs.getUnchecked(blobId);

    Lock lock = blob.lock();
    try {
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

      // soft delete is implemented using an S3 lifecycle that sets expiration on objects with DELETED_TAG
      // tag the bytes
      s3.setObjectTagging(tagAsDeleted(contentPath(blobId)));
      // tag the attributes
      s3.setObjectTagging(tagAsDeleted(attributePath(blobId)));
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
    final S3Blob blob = liveBlobs.getUnchecked(blobId);
    Lock lock = blob.lock();
    try {
      log.debug("Hard deleting blob {}", blobId);

      String attributePath = attributePath(blobId);
      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath);
      Long contentSize = getContentSizeForDeletion(blobAttributes);

      String blobPath = contentPath(blobId);

      boolean blobDeleted = delete(blobPath);
      delete(attributePath);

      if (blobDeleted && contentSize != null) {
        storeMetrics.recordDeletion(contentSize);
      }

      return blobDeleted;
    }
    catch (IOException e) {
      throw new BlobStoreException(e, blobId);
    }
    finally {
      lock.unlock();
      liveBlobs.invalidate(blobId);
    }
  }

  @Nullable
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
  public BlobStoreMetrics getMetrics() {
    return storeMetrics.getMetrics();
  }

  @Override
  protected void doInit(final BlobStoreConfiguration configuration) {
    try {
      this.s3 = amazonS3Factory.create(configuration);
      bucketManager.setS3(s3);
      bucketManager.prepareStorageLocation(blobStoreConfiguration);
      S3BlobStoreConfigurationHelper.setConfiguredBucket(blobStoreConfiguration, getConfiguredBucket());
    }
    catch (Exception e) {
      throw new BlobStoreException("Unable to initialize blob store bucket: " + getConfiguredBucket(), e, null);
    }
  }

  private boolean delete(final String path) throws IOException {
    s3.deleteObject(getConfiguredBucket(), path);
    // note: no info returned from s3
    return true;
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
   *
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
  @Guarded(by = {NEW, STOPPED, FAILED})
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
    public InputStream getInputStream() {
      S3Object object = s3.getObject(getConfiguredBucket(), contentPath(getId()));
      return object.getObjectContent();
    }
  }

  private interface BlobIngester {
    StreamMetrics ingestTo(final String destination) throws IOException;
  }

  @Override
  public Stream<BlobId> getBlobIdStream() {
    Iterable<S3ObjectSummary> summaries = S3Objects.withPrefix(s3, getConfiguredBucket(), getContentPrefix());
    return blobIdStream(summaries);
  }

  @Override
  public Stream<BlobId> getDirectPathBlobIdStream(final String prefix) {
    String subpath = getBucketPrefix() + format("%s/%s", DIRECT_PATH_PREFIX, prefix);
    Iterable<S3ObjectSummary> summaries = S3Objects.withPrefix(s3, getConfiguredBucket(), subpath);
    return stream(summaries.spliterator(), false)
      .map(S3ObjectSummary::getKey)
      .filter(key -> key.endsWith(BLOB_ATTRIBUTE_SUFFIX))
      .map(this::attributePathToDirectPathBlobId);
  }

  private Stream<BlobId> blobIdStream(Iterable<S3ObjectSummary> summaries) {
    return stream(summaries.spliterator(), false)
      .filter(o -> o.getKey().endsWith(BLOB_ATTRIBUTE_SUFFIX))
      .map(S3AttributesLocation::new)
      .map(this::getBlobIdFromAttributeFilePath)
      .map(BlobId::new);
  }

  @Nullable
  @Override
  public BlobAttributes getBlobAttributes(final BlobId blobId) {
    try {
      S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath(blobId));
      return blobAttributes.load() ? blobAttributes : null;
    }
    catch (IOException e) {
      log.error("Unable to load S3BlobAttributes for blob id: {}", blobId, e);
      return null;
    }
  }

  @Override
  public BlobAttributes getBlobAttributes(final S3AttributesLocation attributesFilePath) throws IOException {
    S3BlobAttributes s3BlobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(),
        attributesFilePath.getFullPath());
    s3BlobAttributes.load();
    return s3BlobAttributes;
  }

  @Override
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
  protected void doUndelete(final BlobId blobId) {
    s3.setObjectTagging(untagAsDeleted(contentPath(blobId)));
    s3.setObjectTagging(untagAsDeleted(attributePath(blobId)));
  }

  @Override
  public boolean isStorageAvailable() {
    try {
      return s3.doesBucketExistV2(getConfiguredBucket());
    } catch (SdkBaseException e) {
      log.warn("S3 bucket '{}' is not writable.", getConfiguredBucket(), e);
      return false;
    }
  }

  /**
   * This is a simple existence check resulting from NEXUS-16729.  This allows clients
   * to perform a simple check primarily intended for use in directpath scenarios.
   */
  @Override
  public boolean exists(final BlobId blobId) {
    checkNotNull(blobId);
    S3BlobAttributes blobAttributes = new S3BlobAttributes(s3, getConfiguredBucket(), attributePath(blobId));
    try {
      return blobAttributes.load();
    } catch (IOException ioe) {
      log.debug("Unable to load attributes {} during existence check, exception: {}", blobAttributes, ioe);
      return false;
    }
  }

  /**
   * Used by {@link #getDirectPathBlobIdStream(String)} to convert an s3 key to a {@link BlobId}.
   *
   * @see BlobIdLocationResolver
   */
  private BlobId attributePathToDirectPathBlobId(final String s3Key) { // NOSONAR
    checkArgument(s3Key.startsWith(getBucketPrefix() + DIRECT_PATH_PREFIX + "/"), "Not direct path blob path: %s", s3Key);
    checkArgument(s3Key.endsWith(BLOB_ATTRIBUTE_SUFFIX), "Not blob attribute path: %s", s3Key);
    String blobName = s3Key
        .substring(0, s3Key.length() - BLOB_ATTRIBUTE_SUFFIX.length())
        .substring((getBucketPrefix() + DIRECT_PATH_PREFIX).length() + 1);
    Map<String, String> headers = ImmutableMap.of(
        BLOB_NAME_HEADER, blobName,
        DIRECT_PATH_BLOB_HEADER, "true"
    );
    return blobIdLocationResolver.fromHeaders(headers);
  }
}
