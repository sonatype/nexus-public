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
package org.sonatype.repository.conan.internal.orient.proxy.v1;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.conan.internal.AssetKind;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;
import org.sonatype.repository.conan.internal.orient.metadata.ConanHashVerifier;
import org.sonatype.repository.conan.internal.orient.metadata.ConanUrlIndexer;

import com.google.common.base.Supplier;
import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.view.Content.maintainLastModified;
import static org.sonatype.repository.conan.internal.AssetKind.CONAN_MANIFEST;
import static org.sonatype.repository.conan.internal.AssetKind.CONAN_PACKAGE;
import static org.sonatype.repository.conan.internal.AssetKind.DIGEST;
import static org.sonatype.repository.conan.internal.AssetKind.DOWNLOAD_URL;
import static org.sonatype.repository.conan.internal.orient.proxy.v1.OrientConanProxyHelper.DOWNLOAD_ASSET_KINDS;
import static org.sonatype.repository.conan.internal.orient.proxy.v1.OrientConanProxyHelper.HASH_ALGORITHMS;
import static org.sonatype.repository.conan.internal.orient.proxy.v1.OrientConanProxyHelper.convertFromState;
import static org.sonatype.repository.conan.internal.orient.proxy.v1.OrientConanProxyHelper.getComponentVersion;
import static org.sonatype.repository.conan.internal.orient.proxy.v1.OrientConanProxyHelper.getProxyAssetPath;
import static org.sonatype.repository.conan.internal.orient.utils.ConanFacetUtils.findAsset;
import static org.sonatype.repository.conan.internal.orient.utils.ConanFacetUtils.findComponent;

/**
 * @since 3.next
 */
@Named
public class ConanProxyFacet
    extends ProxyFacetSupport
{
  private final ConanHashVerifier hashVerifier;

  private final ConanUrlIndexer conanUrlIndexer;

  @Inject
  public ConanProxyFacet(final ConanUrlIndexer conanUrlIndexer,
                         final ConanHashVerifier hashVerifier)
  {
    this.conanUrlIndexer = conanUrlIndexer;
    this.hashVerifier = hashVerifier;
  }

  // HACK: Workaround for known CGLIB issue, forces an Import-Package for org.sonatype.nexus.repository.config
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    log.error("doValidate with config {}", configuration);
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    ConanCoords conanCoords = convertFromState(state);
    String assetPath = getProxyAssetPath(conanCoords, assetKind);
    return  getAssetContent(assetPath, context, assetKind);
  }

  @TransactionalTouchBlob
  @Nullable
  protected Content getAssetContent(final String name, final Context context, final AssetKind  assetKind)
      throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), name);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    Content content = toContent(asset, tx.requireBlob(asset.requireBlobRef()));
    if (content != null &&
        (assetKind.equals(DOWNLOAD_URL) ||
            assetKind.equals(DIGEST))) {
      content = new Content(
          new StringPayload(
              conanUrlIndexer.updateAbsoluteUrls(context, content, getRepository()),
              ContentTypes.APPLICATION_JSON)
      );
      Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    }
    return content;
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    ConanCoords conanCoords = convertFromState(state);
    if (assetKind.equals(CONAN_PACKAGE)) {
      return putPackage(content, conanCoords, assetKind);
    }
    return putMetadata(context, content, assetKind, conanCoords);
  }

  private Content putPackage(final Content content,
                             final ConanCoords coords,
                             final AssetKind assetKind) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      return doPutPackage(tempBlob, content, coords, assetKind);
    }
  }

  private Content putMetadata(final Context context,
                              final Content content,
                              final AssetKind assetKind,
                              final ConanCoords coords)
      throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {

      if (assetKind == DOWNLOAD_URL || assetKind == DIGEST) {
        Content saveMetadata = doSaveMetadata(tempBlob, content, assetKind, coords);
        return new Content(
            new StringPayload(
                conanUrlIndexer.updateAbsoluteUrls(context, saveMetadata, getRepository()),
                ContentTypes.APPLICATION_JSON)
        );
      }
      return doSaveMetadata(tempBlob, content, assetKind, coords);
    }
  }

  static Content toContent(final Asset asset, final Blob blob) {
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  private Component getOrCreateComponent(final StorageTx tx,
                                         final Bucket bucket,
                                         final ConanCoords coords)
  {
    Component component = findComponent(tx, getRepository(), coords);
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat())
          .group(coords.getGroup())
          .name(coords.getProject())
          .version(getComponentVersion(coords));
      component.formatAttributes().set("baseVersion", coords.getVersion());
      component.formatAttributes().set("channel", coords.getChannel());
    }
    tx.saveComponent(component);
    return component;
  }

  @TransactionalStoreBlob
  protected Content doPutPackage(final TempBlob tempBlob,
                                 final Payload content,
                                 final ConanCoords coords,
                                 final AssetKind assetKind) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Component component = getOrCreateComponent(tx, bucket, coords);

    String assetPath = getProxyAssetPath(coords, assetKind);
    Asset asset = findAsset(tx, bucket, assetPath);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, CONAN_PACKAGE.name());
    }
    else if (!asset.componentId().equals(EntityHelper.id(component))) {
      asset.componentId(EntityHelper.id(component));
    }
    return saveAsset(tx, asset, tempBlob, content, null);
  }

  @TransactionalStoreBlob
  protected Content doSaveMetadata(final TempBlob metadataContent,
                                   final Payload payload,
                                   final AssetKind assetKind,
                                   final ConanCoords coords) throws IOException
  {
    HashCode hash = null;
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Component component = getOrCreateComponent(tx, bucket, coords);

    String assetPath = getProxyAssetPath(coords, assetKind);
    Asset asset = findAsset(tx, bucket, assetPath);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
      hash = hashVerifier.lookupHashFromAsset(tx, bucket, assetPath);
    }
    else if (!asset.componentId().equals(EntityHelper.id(component))) {
      asset.componentId(EntityHelper.id(component));
    }
    return saveAsset(tx, asset, metadataContent, payload, hash);
  }

  /**
   * Save an asset and create a blob
   *
   * @return blob content
   */
  private Content saveAsset(final StorageTx tx,
                            final Asset asset,
                            final Supplier<InputStream> contentSupplier,
                            final Payload payload,
                            final HashCode hash) throws IOException
  {
    AttributesMap contentAttributes = null;
    String contentType = null;
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
      contentType = payload.getContentType();
    }
    return saveAsset(tx, asset, contentSupplier, contentType, contentAttributes, hash);
  }

  /**
   * Save an asset and create a blob
   *
   * @return blob content
   */
  private Content saveAsset(final StorageTx tx,
                            final Asset asset,
                            final Supplier<InputStream> contentSupplier,
                            final String contentType,
                            final AttributesMap contentAttributes,
                            final HashCode hash) throws IOException
  {
    Content.applyToAsset(asset, maintainLastModified(asset, contentAttributes));
    AssetBlob assetBlob = tx.setBlob(
        asset, asset.name(), contentSupplier, HASH_ALGORITHMS, null, contentType, false
    );

    if (!hashVerifier.verify(hash, assetBlob.getHashes().get(MD5))) {
      return null;
    }
    asset.markAsDownloaded();
    tx.saveAsset(asset);
    return toContent(asset, assetBlob.getBlob());
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
  {
    // do nothing
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);

    if (DOWNLOAD_ASSET_KINDS.contains(assetKind)) {
      return context.getRequest().getPath().substring(1);
    }

    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    ConanCoords coords = convertFromState(state);

    if (assetKind == CONAN_MANIFEST) {
      return getUrlForConanManifest(coords);
    }

    log.info("AssetKind {} to be fetched is {}", assetKind, context.getRequest().getPath());

    // TODO: There are two different URLs for DOWNLOAD_URL, this seems to only look in one of them, that seems problematic
    String download_urls = OrientConanProxyHelper.getProxyAssetPath(coords, DOWNLOAD_URL);
    return getUrlFromDownloadAsset(download_urls, assetKind.getFilename());
  }

  @TransactionalTouchBlob
  @Nullable
  protected String getUrlForConanManifest(final ConanCoords coords) {
    String downloadUrlsAssetPath = OrientConanProxyHelper.getProxyAssetPath(coords, DOWNLOAD_URL);
    StorageTx tx = UnitOfWork.currentTx();

    Asset downloadUrlAsset = findAsset(tx, tx.findBucket(getRepository()), downloadUrlsAssetPath);
    if (downloadUrlAsset == null) {
      // NEXUS-22735. Looks like it was search request. So let's look up conanmanifest url from digest
      String digestAssetPath = OrientConanProxyHelper.getProxyAssetPath(coords, DIGEST);
      Asset digest = findAsset(tx, tx.findBucket(getRepository()), digestAssetPath);
      if (digest == null) {
        throw new IllegalStateException("DIGEST not found");
      }
      return conanUrlIndexer.findUrl(tx.requireBlob(digest.blobRef()).getInputStream(), CONAN_MANIFEST.getFilename());
    }
    return conanUrlIndexer.findUrl(tx.requireBlob(downloadUrlAsset.blobRef()).getInputStream(), CONAN_MANIFEST.getFilename());
  }

  @TransactionalTouchBlob
  protected String getUrlFromDownloadAsset(final String download_urls, final String find) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), download_urls);
    if (asset == null) {
      return null;
    }
    return conanUrlIndexer.findUrl(tx.requireBlob(asset.blobRef()).getInputStream(), find);
  }

  @Nonnull
  @Override
  protected CacheController getCacheController(@Nonnull final Context context) {
    final AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    return checkNotNull(cacheControllerHolder.get(assetKind.getCacheType()));
  }
}
