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
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.AttributeChange;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.WrappedContent;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_IP_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.common.time.DateHelper.toOffsetDateTime;
import static org.sonatype.nexus.repository.cache.CacheInfo.CACHE;
import static org.sonatype.nexus.repository.cache.CacheInfo.CACHE_TOKEN;
import static org.sonatype.nexus.repository.cache.CacheInfo.INVALIDATED;
import static org.sonatype.nexus.repository.content.AttributeChange.OVERLAY;
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
  public Optional<OffsetDateTime> lastDownloaded() {
    return asset.lastDownloaded();
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
  public OffsetDateTime lastUpdated() {
    return asset.lastUpdated();
  }

  @Override
  public FluentAsset attributes(final AttributeChange change, final String key, final Object value) {
    facet.stores().assetStore.updateAssetAttributes(asset, change, key, value);
    return this;
  }

  @Override
  public FluentAsset attach(final TempBlob tempBlob) {
    facet.checkAttachAllowed(asset);
    return doAttach(makePermanent(tempBlob.getBlob()), tempBlob.getHashes());
  }

  @Override
  public FluentAsset attach(final Blob blob, final Map<HashAlgorithm, HashCode> checksums) {
    facet.checkAttachAllowed(asset);
    return doAttach(blob, checksums);
  }

  @Override
  public Content download() {
    AssetBlob assetBlob = asset.blob()
        .orElseThrow(() -> new IllegalStateException("No blob attached to " + asset.path()));

    BlobRef blobRef = assetBlob.blobRef();
    Blob blob = facet.stores().blobStore.get(blobRef.getBlobId());
    if (blob == null) {
      throw new MissingBlobException(blobRef);
    }

    Content content = new Content(new BlobPayload(blob, assetBlob.contentType()));
    AttributesMap contentAttributes = content.getAttributes();

    // attach asset so downstream format handlers can retrieve it if neccessary
    contentAttributes.set(Asset.class, this);

    if (attributes().contains(CACHE)) {
      // internal cache details used to decide when content is stale/invalidated
      contentAttributes.set(CacheInfo.class, CacheInfo.fromMap(attributes(CACHE)));
    }

    if (attributes().contains(CONTENT)) {
      // external cache details previously recorded from upstream content
      AttributesMap contentHeaders = attributes(CONTENT);
      contentAttributes.set(CONTENT_LAST_MODIFIED, new DateTime(contentHeaders.get(CONTENT_LAST_MODIFIED)));
      contentAttributes.set(CONTENT_ETAG, contentHeaders.get(CONTENT_ETAG));
    }
    else {
      // otherwise use the blob to supply details for external caching
      BlobMetrics metrics = blob.getMetrics();
      contentAttributes.set(CONTENT_LAST_MODIFIED, metrics.getCreationTime());
      contentAttributes.set(CONTENT_ETAG, metrics.getSha1Hash());
    }

    return content;
  }

  @Override
  public FluentAsset markAsDownloaded() {
    facet.stores().assetStore.markAsDownloaded(asset);
    return this;
  }

  @Override
  public FluentAsset markAsCached(final Payload content) {
    if (content instanceof Content) {
      AttributesMap contentAttributes = ((Content) content).getAttributes();
      CacheInfo cacheInfo = contentAttributes.get(CacheInfo.class);
      if (cacheInfo != null) {
        markAsCached(cacheInfo);
      }
      cacheContentHeaders(contentAttributes);
    }
    return this;
  }

  @Override
  public FluentAsset markAsCached(final CacheInfo cacheInfo) {
    return withAttribute(CACHE, cacheInfo.toMap());
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
  public boolean delete() {
    facet.checkDeleteAllowed(asset);
    return facet.stores().assetStore.deleteAsset(asset);
  }

  @Override
  public Asset unwrap() {
    return asset;
  }

  private AssetBlobData createAssetBlob(final BlobRef blobRef,
                                        final Blob blob,
                                        final Map<HashAlgorithm, HashCode> checksums)
  {
    BlobMetrics metrics = blob.getMetrics();
    Map<String, String> headers = blob.getHeaders();

    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setBlobRef(blobRef);
    assetBlob.setBlobSize(metrics.getContentSize());
    assetBlob.setContentType(headers.get(CONTENT_TYPE_HEADER));

    assetBlob.setChecksums(checksums.entrySet().stream().collect(
        toImmutableMap(
            e -> e.getKey().name(),
            e -> e.getValue().toString())));

    assetBlob.setBlobCreated(toOffsetDateTime(metrics.getCreationTime()));
    assetBlob.setCreatedBy(headers.get(CREATED_BY_HEADER));
    assetBlob.setCreatedByIp(headers.get(CREATED_BY_IP_HEADER));

    facet.stores().assetBlobStore.createAssetBlob(assetBlob);

    return assetBlob;
  }

  private BlobRef blobRef(final Blob blob) {
    return new BlobRef(facet.nodeName(), facet.stores().blobStoreName, blob.getId().asUniqueString());
  }

  private Blob makePermanent(final Blob tempBlob) {
    ImmutableMap.Builder<String, String> headerBuilder = ImmutableMap.builder();

    Map<String, String> tempHeaders = tempBlob.getHeaders();
    headerBuilder.put(REPO_NAME_HEADER, tempHeaders.get(REPO_NAME_HEADER));
    headerBuilder.put(BLOB_NAME_HEADER, asset.path());
    headerBuilder.put(CREATED_BY_HEADER, tempHeaders.get(CREATED_BY_HEADER));
    headerBuilder.put(CREATED_BY_IP_HEADER, tempHeaders.get(CREATED_BY_IP_HEADER));
    headerBuilder.put(CONTENT_TYPE_HEADER, facet.checkContentType(asset, tempBlob));

    return facet.stores().blobStore.copy(tempBlob.getId(), headerBuilder.build());
  }

  private FluentAsset doAttach(final Blob blob, final Map<HashAlgorithm, HashCode> checksums) {

    BlobRef blobRef = blobRef(blob);
    AssetBlob assetBlob = facet.stores().assetBlobStore.readAssetBlob(blobRef)
        .orElseGet(() -> createAssetBlob(blobRef, blob, checksums));

    ((AssetData) asset).setAssetBlob(assetBlob);
    facet.stores().assetStore.updateAssetBlobLink(asset);

    return this;
  }

  /**
   * Record external cache details provided by upstream content.
   *
   * @see ProxyFacetSupport#fetch
   */
  private void cacheContentHeaders(final AttributesMap contentAttributes) {
    ImmutableMap.Builder<String, String> headerBuilder = ImmutableMap.builder();
    if (contentAttributes.contains(CONTENT_LAST_MODIFIED)) {
      headerBuilder.put(CONTENT_LAST_MODIFIED, contentAttributes.get(CONTENT_LAST_MODIFIED).toString());
    }
    if (contentAttributes.contains(CONTENT_ETAG)) {
      headerBuilder.put(CONTENT_ETAG, contentAttributes.get(CONTENT_ETAG, String.class));
    }
    Map<String, String> contentHeaders = headerBuilder.build();
    if (!contentHeaders.isEmpty()) {
      withAttribute(CONTENT, contentHeaders);
    }
    else {
      withoutAttribute(CONTENT);
    }
  }
}
