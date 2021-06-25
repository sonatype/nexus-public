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
package org.sonatype.repository.conan.internal.datastore;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.repository.conan.ConanContentFacet;
import org.sonatype.repository.conan.internal.AssetKind;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.conan.internal.AssetKind.CONAN_MANIFEST;
import static org.sonatype.repository.conan.internal.AssetKind.CONAN_PACKAGE;
import static org.sonatype.repository.conan.internal.AssetKind.DIGEST;
import static org.sonatype.repository.conan.internal.AssetKind.DOWNLOAD_URL;

/**
 * @since 3.32
 */
@Named
public class ConanProxyFacet
    extends ProxyFacetSupport
{
  private final ConanHashVerifier hashVerifier;

  private final ConanUrlIndexer conanUrlIndexer;

  @Inject
  public ConanProxyFacet(
      final ConanUrlIndexer conanUrlIndexer,
      final ConanHashVerifier hashVerifier)
  {
    this.conanUrlIndexer = conanUrlIndexer;
    this.hashVerifier = hashVerifier;
  }

  // HACK: Workaround for known CGLIB issue, forces an Import-Package for org.sonatype.nexus.repository.config
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    ConanCoords coords = ConanProxyHelper.convertFromState(state);
    Optional<Content> optionalContent = facet(ConanContentFacet.class)
        .getAsset(coords, assetKind)
        .map(FluentAsset::download);

    if (optionalContent.isPresent() && (assetKind.equals(DOWNLOAD_URL) || assetKind.equals(DIGEST))) {
      return new Content(
          new StringPayload(conanUrlIndexer.updateAbsoluteUrls(context, optionalContent.get(), getRepository()),
              ContentTypes.APPLICATION_JSON));
    }
    return optionalContent.orElse(null);
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    ConanCoords conanCoords = ConanProxyHelper.convertFromState(state);
    if (assetKind.equals(CONAN_PACKAGE)) {
      return putPackage(content, conanCoords, assetKind);
    }
    return putMetadata(context, content, assetKind, conanCoords);
  }

  private Content putPackage(final Content content, final ConanCoords coords, final AssetKind assetKind) {
    try (TempBlob tempBlob = facet(ContentFacet.class).blobs().ingest(content, ConanProxyHelper.HASH_ALGORITHMS)) {
      return facet(ConanContentFacet.class).putPackage(tempBlob, coords, assetKind).download();
    }
  }

  private Content putMetadata(
      final Context context,
      final Content content,
      final AssetKind assetKind,
      final ConanCoords coords)
      throws IOException
  {
    try (TempBlob tempBlob = facet(ContentFacet.class).blobs().ingest(content, ConanProxyHelper.HASH_ALGORITHMS)) {
      if (assetKind == DOWNLOAD_URL || assetKind == DIGEST) {
        Content saveMetadata = doSaveMetadata(tempBlob, assetKind, coords);
        return new Content(
            new StringPayload(
                conanUrlIndexer.updateAbsoluteUrls(context, saveMetadata, getRepository()),
                ContentTypes.APPLICATION_JSON)
        );
      }
      return doSaveMetadata(tempBlob, assetKind, coords);
    }
  }

  protected Content doSaveMetadata(
      final TempBlob metadataContent,
      final AssetKind assetKind,
      final ConanCoords coords)
  {
    String assetPath = ConanProxyHelper.getProxyAssetPath(coords, assetKind);
    HashCode hashFromManifest = hashVerifier.lookupHashFromAsset(getRepository(), assetPath);
    HashCode newAssetHash = metadataContent.getHashes().get(HashAlgorithm.MD5);

    if (!hashVerifier.verify(hashFromManifest, newAssetHash)) {
      return null;
    }

    return facet(ConanContentFacet.class).putMetadata(metadataContent, coords, assetKind).download();
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
  {
    // do nothing
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);

    if (ConanProxyHelper.DOWNLOAD_ASSET_KINDS.contains(assetKind)) {
      return context.getRequest().getPath().substring(1);
    }

    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    ConanCoords coords = ConanProxyHelper.convertFromState(state);

    if (assetKind == CONAN_MANIFEST) {
      return getUrlForConanManifest(coords);
    }

    log.info("AssetKind {} to be fetched is {}", assetKind, context.getRequest().getPath());

    // TODO: There are two different URLs for DOWNLOAD_URL, this seems to only look in one of them, that seems problematic
    return facet(ConanContentFacet.class)
        .getAsset(coords, DOWNLOAD_URL)
        .map(fluentAsset -> {
          try {
            return conanUrlIndexer.findUrl(fluentAsset.download().openInputStream(), assetKind.getFilename());
          }
          catch (IOException e) {
            return null;
          }
        })
        .orElse(null);
  }

  @Nullable
  protected String getUrlForConanManifest(final ConanCoords coords) {
    Optional<FluentAsset> downloadUrlAsset = facet(ConanContentFacet.class).getAsset(coords, DOWNLOAD_URL);

    if (downloadUrlAsset.isPresent()) {
      try {
        return conanUrlIndexer
            .findUrl(downloadUrlAsset.get().download().openInputStream(), CONAN_MANIFEST.getFilename());
      }
      catch (IOException e) {
        String downloadUrlsAssetPath = ConanProxyHelper.getProxyAssetPath(coords, DOWNLOAD_URL);
        log.warn("Failed to read manifest blob of '{}': {}", downloadUrlsAssetPath, e.getMessage(),
            log.isDebugEnabled() ? e : null);
        return null;
      }
    }
    else {
      String digestAssetPath = ConanProxyHelper.getProxyAssetPath(coords, DIGEST);
      return facet(ConanContentFacet.class)
          .getAsset(coords, DIGEST)
          .map(digest -> {
            try {
              return conanUrlIndexer.findUrl(digest.download().openInputStream(), CONAN_MANIFEST.getFilename());
            }
            catch (IOException e) {
              log.warn("Failed to read digest blob of '{}': {}", digestAssetPath, e.getMessage(),
                  log.isDebugEnabled() ? e : null);
              return null;
            }
          })
          .orElseThrow(() -> new IllegalStateException("DIGEST not found at " + digestAssetPath));
    }
  }

  @Nonnull
  @Override
  protected CacheController getCacheController(@Nonnull final Context context) {
    final AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    return checkNotNull(cacheControllerHolder.get(assetKind.getCacheType()));
  }
}
