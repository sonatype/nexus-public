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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.ExternalMetadata;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAttributes;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.store.WrappedContent;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.cache.CacheInfo.CACHE;
import static org.sonatype.nexus.repository.cache.CacheInfo.CACHE_TOKEN;
import static org.sonatype.nexus.repository.cache.CacheInfo.INVALIDATED;
import static org.sonatype.nexus.repository.content.AttributeOperation.OVERLAY;
import static org.sonatype.nexus.repository.view.Content.CONTENT;
import static org.sonatype.nexus.repository.view.Content.CONTENT_ETAG;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

/**
 * {@link FluentAsset} implementation.
 *
 * @since 3.24
 */
public class FluentAssetImpl
    implements FluentAsset, WrappedContent<Asset>
{
  private static final Logger log = LoggerFactory.getLogger(FluentAssetImpl.class);

  private static final int DEFAULT_DOWNLOAD_MAX_RETRIES = 3;

  private static final int DEFAULT_DOWNLOAD_RETRY_DELAY_MS = 100;

  private static final int DEFAULT_DOWNLOAD_MAX_RETRY_DELAY_MS = 2000;

  private static final int downloadMaxRetries = SystemPropertiesHelper.getInteger(
      "nexus.asset.download.maxRetries", DEFAULT_DOWNLOAD_MAX_RETRIES);

  private static final int downloadRetryDelayMs = SystemPropertiesHelper.getInteger(
      "nexus.asset.download.retryDelayMs", DEFAULT_DOWNLOAD_RETRY_DELAY_MS);

  private static final int downloadMaxRetryDelayMs = SystemPropertiesHelper.getInteger(
      "nexus.asset.download.maxRetryDelayMs", DEFAULT_DOWNLOAD_MAX_RETRY_DELAY_MS);

  /**
   * @deprecated legacy field used by Nexus before 3.93 for PCCS.
   */
  @Deprecated
  private static final String CONTENT_PCCS_HASH = "pccs_hash";

  private final ContentFacetSupport facet;

  private final Asset asset;

  public FluentAssetImpl(final ContentFacetSupport facet, final Asset asset) {
    this.facet = checkNotNull(facet);
    this.asset = checkNotNull(asset);
  }

  @Override
  public Repository repository() {
    return facet.repository();
  }

  @Override
  public String path() {
    return asset.path();
  }

  @Override
  public String kind() {
    return asset.kind();
  }

  @Override
  public Optional<Component> component() {
    return asset.component();
  }

  @Override
  public Optional<AssetBlob> blob() {
    return asset.blob();
  }

  @Override
  public boolean hasBlob() {
    return asset.hasBlob();
  }

  @Override
  public Optional<OffsetDateTime> lastDownloaded() {
    return asset.lastDownloaded();
  }

  @Override
  public String blobStoreName() {
    return asset.blobStoreName();
  }

  @Override
  public long assetBlobSize() {
    return asset.assetBlobSize();
  }

  @Override
  public NestedAttributesMap attributes() {
    return asset.attributes();
  }

  @Override
  public OffsetDateTime created() {
    return asset.created();
  }

  @Override
  public void created(final OffsetDateTime lastUpdated) {
    ((AssetData) asset).setCreated(lastUpdated);
    facet.stores().assetStore.created(this, lastUpdated);
  }

  @Override
  public OffsetDateTime lastUpdated() {
    return asset.lastUpdated();
  }

  @Override
  public FluentAsset attributes(final AttributeOperation change, final String key, final Object value) {
    facet.stores().assetStore.updateAssetAttributes(asset, new AttributeChangeSet(change, key, value));
    asset.blob()
        .ifPresent(blob -> facet.blobMetadataStorage()
            .attach(facet.stores().blobStoreProvider.get(), blob.blobRef().getBlobId(), null, asset.attributes(),
                asset.blob().get().checksums()));
    return this;
  }

  @Override
  public FluentAsset attributes(final AttributeChangeSet changes) {
    facet.stores().assetStore.updateAssetAttributes(asset, changes);
    asset.blob()
        .ifPresent(blob -> facet.blobMetadataStorage()
            .attach(facet.stores().blobStoreProvider.get(), blob.blobRef().getBlobId(), null, asset.attributes(),
                blob.checksums()));
    return this;
  }

  @Override
  public FluentAsset attach(final TempBlob blob) {
    return new FluentAssetBuilderImpl(facet, facet.stores().assetStore, asset).attach(blob);
  }

  @Override
  public FluentAsset attach(final Blob blob, final Map<HashAlgorithm, HashCode> checksums) {
    return new FluentAssetBuilderImpl(facet, facet.stores().assetStore, asset)
        .attach(blob, checksums);
  }

  @Override
  public FluentAsset attachIgnoringWritePolicy(final Blob blob, final Map<HashAlgorithm, HashCode> checksums) {
    return new FluentAssetBuilderImpl(facet, facet.stores().assetStore, asset)
        .attachIgnoringWritePolicy(blob, checksums);
  }

  @Override
  public Content download() {
    Asset currentAsset = asset;

    for (int attempt = 1; attempt <= downloadMaxRetries; attempt++) {
      try {
        return downloadAttempt(currentAsset, attempt);
      }
      catch (MissingBlobException e) {
        currentAsset = handleMissingBlobRetry(currentAsset, attempt, e);
      }
    }

    // This should never be reached, but added for completeness
    throw new IllegalStateException("Download retry loop completed without returning content for " + asset.path());
  }

  private Content downloadAttempt(final Asset currentAsset, final int attempt) {
    String assetPath = currentAsset.path();
    AssetBlob assetBlob = currentAsset.blob()
        .orElseThrow(() -> new IllegalStateException("No blob attached to " + assetPath));

    Blob blob = retrieveBlob(assetBlob.blobRef());
    Content content = new Content(new BlobPayload(blob, assetBlob.contentType()));

    setupContentAttributes(content, blob, assetBlob);

    if (attempt > 1) {
      log.debug("Successfully downloaded asset {} on attempt {}", assetPath, attempt);
    }

    return content;
  }

  private Blob retrieveBlob(final BlobRef blobRef) {
    return Optional.ofNullable(facet.stores().blobStoreProvider.get().get(blobRef))
        .orElseGet(() -> facet.dependencies()
            .getMoveService()
            .map(service -> service.getIfBeingMoved(blobRef, repository().getName()))
            .orElseThrow(() -> new MissingBlobException(blobRef)));
  }

  private void setupContentAttributes(final Content content, final Blob blob, final AssetBlob assetBlob) {
    AttributesMap contentAttributes = content.getAttributes();

    // attach asset so downstream format handlers can retrieve it if necessary
    contentAttributes.set(Asset.class, this);

    if (attributes().contains(CACHE)) {
      // internal cache details used to decide when content is stale/invalidated
      contentAttributes.set(CacheInfo.class, CacheInfo.fromMap(attributes(CACHE)));
    }

    if (isProxyOrGroupRepository() && attributes().contains(CONTENT)) {
      setupProxyContentAttributes(contentAttributes, blob);
    }
    else {
      setupHostedContentAttributes(contentAttributes, blob);
    }

    attachExternalMetadata(assetBlob, contentAttributes);
  }

  private boolean isProxyOrGroupRepository() {
    return !(repository().getType() instanceof HostedType);
  }

  private void setupProxyContentAttributes(final AttributesMap contentAttributes, final Blob blob) {
    // external cache details previously recorded from upstream content
    AttributesMap contentHeaders = attributes(CONTENT);

    Object lastModified = contentHeaders.get(CONTENT_LAST_MODIFIED);
    if (lastModified == null && repository().getType() instanceof GroupType) {
      lastModified = blob.getMetrics().getCreationTime();
    }

    contentAttributes.set(CONTENT_LAST_MODIFIED, new DateTime(lastModified));
    contentAttributes.set(CONTENT_ETAG, Optional.ofNullable(contentHeaders.get(CONTENT_ETAG))
        .orElseGet(blob.getMetrics()::getSha1Hash));

    Optional.ofNullable(contentHeaders.get(CONTENT_PCCS_HASH, String.class))
        .ifPresent(pccsHash -> contentAttributes.set(CONTENT_PCCS_HASH, pccsHash));
  }

  private void setupHostedContentAttributes(final AttributesMap contentAttributes, final Blob blob) {
    // use the blob to supply details for external caching
    BlobMetrics metrics = blob.getMetrics();
    contentAttributes.set(CONTENT_LAST_MODIFIED, metrics.getCreationTime());
    contentAttributes.set(CONTENT_ETAG, metrics.getSha1Hash());
  }

  private Asset handleMissingBlobRetry(
      final Asset currentAsset,
      final int attempt,
      final MissingBlobException exception)
  {
    if (attempt < downloadMaxRetries) {
      long retryDelay = calculateRetryDelay(attempt);
      log.warn("Blob {} missing for asset {} on attempt {}, reloading asset metadata and retrying after {}ms",
          exception.getBlobRef(), currentAsset.path(), attempt, retryDelay);

      return reloadAssetAndWait(currentAsset, retryDelay);
    }
    else {
      log.error("Failed to download asset {} after {} attempts due to missing blob {}",
          currentAsset.path(), downloadMaxRetries, exception.getBlobRef());
      throw exception;
    }
  }

  /**
   * Calculates the retry delay using exponential backoff strategy.
   * The delay increases with each attempt: 100ms, 200ms, 400ms, etc.
   * This is especially important in HA environments where blob replication,
   * node synchronization, or storage consistency may need more time.
   *
   * @param attempt the current retry attempt number (1-based)
   * @return the delay in milliseconds, capped at downloadMaxRetryDelayMs
   */
  private long calculateRetryDelay(final int attempt) {
    long exponentialDelay = downloadRetryDelayMs * (1L << attempt - 1);
    return Math.min(exponentialDelay, downloadMaxRetryDelayMs);
  }

  private Asset reloadAssetAndWait(final Asset currentAsset, final long delayMs) {
    try {
      // Reload asset from database to get the updated blob reference
      int assetId = InternalIds.internalAssetId(currentAsset);
      String assetPath = currentAsset.path();
      Asset reloadedAsset = facet.stores().assetStore.readAsset(assetId)
          .orElseThrow(() -> new IllegalStateException(
              "Asset " + assetPath + " no longer exists after blob reference became stale"));

      Thread.sleep(delayMs);
      return reloadedAsset;
    }
    catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while waiting to retry download of asset {}", currentAsset.path());
      throw new MissingBlobException(currentAsset.blob()
          .map(AssetBlob::blobRef)
          .orElseThrow(() -> new IllegalStateException("No blob attached to " + currentAsset.path())));
    }
  }

  @Override
  public FluentAsset markAsDownloaded() {
    facet.stores().assetStore.markAsDownloaded(asset);
    return this;
  }

  @Override
  public FluentAsset markAsCached(final Payload content) {
    if (content instanceof Content) {
      AttributeChangeSet changes = new AttributeChangeSet();
      AttributesMap contentAttributes = ((Content) content).getAttributes();
      CacheInfo cacheInfo = contentAttributes.get(CacheInfo.class);
      if (cacheInfo != null) {
        markAsCached(changes, cacheInfo);
      }
      cacheContentHeaders(changes, contentAttributes);
      attributes(changes);
    }
    return this;
  }

  @Override
  public FluentAsset markAsCached(final CacheInfo cacheInfo) {
    return markAsCached(this, cacheInfo);
  }

  private static <A extends FluentAttributes<A>> A markAsCached(final A attributes, final CacheInfo cacheInfo) {
    return attributes.withAttribute(CACHE, cacheInfo.toMap());
  }

  @Override
  public FluentAsset markAsStale() {
    return attributes(OVERLAY, CACHE, ImmutableMap.of(CACHE_TOKEN, INVALIDATED));
  }

  @Override
  public boolean isStale(final CacheController cacheController) {
    CacheInfo cacheInfo = CacheInfo.fromMap(attributes(CACHE));
    return cacheInfo != null && cacheController.isStale(cacheInfo);
  }

  @Override
  public FluentAsset kind(final String kind) {
    ((AssetData) asset).setKind(kind);
    facet.stores().assetStore.updateAssetKind(asset);
    return this;
  }

  @Override
  public FluentAsset updatePath(final String path) {
    ((AssetData) asset).setPath(path);
    facet.stores().assetStore.updateAssetPath(asset);
    return this;
  }

  @Override
  public boolean delete() {
    facet.checkDeleteAllowed(asset);
    return facet.stores().assetStore.deleteAsset(asset);
  }

  @Override
  public Asset unwrap() {
    return asset;
  }

  /**
   * attaches external metadata if stored in the asset_blob attributes
   *
   * @param assetBlob the {@link AssetBlob} that carries the external metadata
   * @param contentAttributes the {@link Content} attributes to be updated
   */
  private static void attachExternalMetadata(final AssetBlob assetBlob, final AttributesMap contentAttributes) {
    ExternalMetadata externalMetadata = assetBlob.externalMetadata();

    if (externalMetadata == null) {
      return;
    }

    if (externalMetadata.lastModified() != null) {
      contentAttributes.set(Content.EXTERNAL_LAST_MODIFIED, externalMetadata.lastModified());
    }

    if (externalMetadata.etag() != null) {
      contentAttributes.set(Content.EXTERNAL_ETAG, externalMetadata.etag());
    }
  }

  /**
   * Record external cache details provided by upstream content.
   *
   * @see ProxyFacetSupport#fetch
   */
  private static void cacheContentHeaders(final FluentAttributes<?> attributes, final AttributesMap contentAttributes) {
    ImmutableMap.Builder<String, String> headerBuilder = ImmutableMap.builder();
    if (contentAttributes.contains(CONTENT_LAST_MODIFIED)) {
      headerBuilder.put(CONTENT_LAST_MODIFIED, contentAttributes.get(CONTENT_LAST_MODIFIED).toString());
    }
    if (contentAttributes.contains(CONTENT_ETAG)) {
      headerBuilder.put(CONTENT_ETAG, contentAttributes.get(CONTENT_ETAG, String.class));
    }
    if (contentAttributes.contains(CONTENT_PCCS_HASH)) {
      headerBuilder.put(CONTENT_PCCS_HASH, contentAttributes.get(CONTENT_PCCS_HASH, String.class));
    }

    Map<String, String> contentHeaders = headerBuilder.build();
    if (!contentHeaders.isEmpty()) {
      attributes.withAttribute(CONTENT, contentHeaders);
    }
    else {
      attributes.withoutAttribute(CONTENT);
    }
  }

  @Override
  public void blobCreated(final OffsetDateTime blobCreated) {
    blob().ifPresent(assetBlob -> {
      if (assetBlob instanceof AssetBlobData) {
        ((AssetBlobData) assetBlob).setBlobCreated(blobCreated);
      }
      facet.stores().assetBlobStore.setBlobCreated(assetBlob, blobCreated);
    });
  }

  @Override
  public void blobAddedToRepository(final OffsetDateTime addedToRepository) {
    blob().ifPresent(assetBlob -> facet.stores().assetBlobStore.setAddedToRepository(assetBlob, addedToRepository));
  }

  @Override
  public void lastDownloaded(final OffsetDateTime lastDownloaded) {
    ((AssetData) asset).setLastDownloaded(lastDownloaded);
    facet.stores().assetStore.lastDownloaded(this, lastDownloaded);
  }

  @Override
  public void lastUpdated(final OffsetDateTime lastUpdated) {
    facet.stores().assetStore.lastUpdated(this, lastUpdated);
  }

  @Override
  public void createdBy(final String createdBy) {
    blob().ifPresent(assetBlob -> facet.stores().assetBlobStore.setCreatedBy(assetBlob, createdBy));
  }

  @Override
  public void createdByIP(final String createdByIP) {
    blob().ifPresent(assetBlob -> facet.stores().assetBlobStore.setCreatedByIP(assetBlob, createdByIP));
  }

  @Override
  public String toString() {
    return asset.toString();
  }
}
