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
package org.sonatype.nexus.repository.p2.orient.internal.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.mime.ContentValidator;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.p2.internal.metadata.CompositeRepositoryRewriter;
import org.sonatype.nexus.repository.p2.internal.metadata.P2Attributes;
import org.sonatype.nexus.repository.p2.internal.metadata.RemoveMirrorTransformer;
import org.sonatype.nexus.repository.p2.internal.metadata.UriToSiteHashUtil;
import org.sonatype.nexus.repository.p2.internal.metadata.XmlTransformer;
import org.sonatype.nexus.repository.p2.internal.proxy.StreamCopier;
import org.sonatype.nexus.repository.p2.internal.util.P2TempBlobUtils;
import org.sonatype.nexus.repository.p2.orient.P2Facet;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.reflect.TypeToken;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.BINARY_BUNDLE;
import static org.sonatype.nexus.repository.p2.orient.internal.OrientP2Facet.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.p2.orient.internal.util.OrientP2PathUtils.matcherState;
import static org.sonatype.nexus.repository.p2.orient.internal.util.OrientP2PathUtils.toP2Attributes;
import static org.sonatype.nexus.repository.p2.orient.internal.util.OrientP2PathUtils.toP2AttributesBinary;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

/**
 * P2 {@link ProxyFacet} implementation.
 *
 * @since 3.28
 */
@Named
public class OrientP2ProxyFacet
    extends ProxyFacetSupport
{
  public static final String REMOTE_URL = "remote_url";

  public static final String REMOTE_HASH = "remote_site_hash";

  public static final String MIRRORS_URL = "mirrors_url";

  public static final String CHILD_URLS = "child_urls";

  private final P2TempBlobUtils p2TempBlobUtils;

  private final ContentValidator contentValidator;

  @Inject
  public OrientP2ProxyFacet(
      final P2TempBlobUtils p2TempBlobUtils,
      final ContentValidator contentValidator)
  {
    this.p2TempBlobUtils = checkNotNull(p2TempBlobUtils);
    this.contentValidator = checkNotNull(contentValidator);

  }

  // HACK: Workaround for known CGLIB issue, forces an Import-Package for org.sonatype.nexus.repository.config
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case COMPOSITE_ARTIFACTS:
      case COMPOSITE_CONTENT:
      case P2_INDEX:
      case ARTIFACTS_METADATA:
      case CONTENT_METADATA:
      case BUNDLE:
      case BINARY_BUNDLE:
        return getAsset(context.getRequest().getPath().substring(1));
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    String remoteUrl = context.getAttributes().require(REMOTE_URL, String.class);
    TokenMatcher.State matcherState = matcherState(context);
    String path = context.getRequest().getPath().substring(1);
    switch (assetKind) {
      case COMPOSITE_ARTIFACTS:
      case COMPOSITE_CONTENT:
        return storeCompositeMetadata(path, content, assetKind, matcherState, remoteUrl);
      case P2_INDEX:
      case CONTENT_METADATA:
        return putMetadataAsset(path, content, assetKind, remoteUrl);
      case ARTIFACTS_METADATA:
        return storeArtifactsMetadata(path, content, assetKind, matcherState, remoteUrl);
      case BUNDLE:
        return putComponent(toP2Attributes(path, matcherState), content, assetKind);
      case BINARY_BUNDLE:
        return putBinary(toP2AttributesBinary(path, matcherState), content);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  @Nullable
  protected Content fetch(final Context context, final Content stale) throws IOException {
    String url = getUrl(context);
    if (url == null) {
      return null;
    }
    return fetch(url, context, stale);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    TokenMatcher.State matcherState = matcherState(context);
    String site = matcherState.getTokens().get("site");

    String remoteUrl;
    if (site == null) {
      String repositoryUrl = getRemoteUrl().toString();
      remoteUrl = URI.create(repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + '/')
          .resolve(context.getRequest().getPath().substring(1)).toString();
    }
    else {
      String path = context.getRequest().getPath().substring(2 + site.length());
      Optional<URI> baseUri = findRepositoryUrl(site);
      remoteUrl = baseUri.map(b -> b.resolve(path)).map(Object::toString).orElse(null);
    }

    context.getAttributes().backing().put(REMOTE_URL, remoteUrl);
    return remoteUrl;
  }

  private Optional<URI> findRepositoryUrl(final String site) {
    Iterable<Asset> assets = Transactional.operation.withDb(facet(StorageFacet.class).txSupplier()).call(() -> {
      StorageTx tx = UnitOfWork.currentTx();
      String field = P_ATTRIBUTES + '.' + P2Format.NAME + '.' + P_ASSET_KIND;
      Query query = Query.builder().where(field).eq(AssetKind.COMPOSITE_ARTIFACTS).or(field)
          .eq(AssetKind.COMPOSITE_CONTENT).build();
      return tx.findAssets(query, Collections.singleton(getRepository()));
    });

    for (Asset asset : assets) {
      List<String> urls = extractUris(asset);
      for (String url : urls) {
        if (UriToSiteHashUtil.map(url).equals(site)) {
          return Optional.of(URI.create(url));
        }
      }
    }
    log.debug("Unknown remote site: {}", site);
    return Optional.empty();
  }

  private List<String> extractUris(final Asset asset) {
    return asset.formatAttributes().get(CHILD_URLS, new TypeToken<List<String>>()
    {
    });
  }

  @TransactionalStoreBlob
  protected Content putMetadataAsset(
      final String path,
      final Content content,
      final AssetKind assetKind,
      final String remoteUrl) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob metadataContent = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      StorageTx tx = UnitOfWork.currentTx();
      Bucket bucket = tx.findBucket(getRepository());

      Asset asset = facet(P2Facet.class).findAsset(tx, bucket, path);
      if (asset == null) {
        asset = tx.createAsset(bucket, getRepository().getFormat());
        asset.name(path);
        asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
        asset.formatAttributes().set(REMOTE_URL, remoteUrl);
      }

      return facet(P2Facet.class).saveAsset(tx, asset, metadataContent, content);
    }
  }

  private Content putBinary(final P2Attributes p2attributes, final Content content) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      return doPutBinary(p2attributes, tempBlob, content);
    }
  }

  @TransactionalStoreBlob
  protected Content doPutBinary(
      final P2Attributes p2Attributes,
      final TempBlob componentContent,
      final Payload payload) throws IOException
  {
    return facet(P2Facet.class).doCreateOrSaveComponent(p2Attributes, componentContent, payload, BINARY_BUNDLE);
  }

  @TransactionalStoreBlob
  protected Content putComponent(
      final P2Attributes p2Attributes,
      final Content content,
      final AssetKind assetKind) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      return doPutComponent(p2Attributes, tempBlob, content, assetKind);
    }
  }

  private Content doPutComponent(
      final P2Attributes p2Attributes,
      final TempBlob componentContent,
      final Payload payload,
      final AssetKind assetKind) throws IOException
  {
    P2Attributes mergedP2Attributes = p2TempBlobUtils.mergeAttributesFromTempBlob(componentContent, p2Attributes);

    return facet(P2Facet.class).doCreateOrSaveComponent(mergedP2Attributes, componentContent, payload, assetKind);
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo) {
    setCacheInfo(content, cacheInfo);
  }

  @TransactionalTouchMetadata
  public void setCacheInfo(final Content content, final CacheInfo cacheInfo) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug("Attempting to set cache info for non-existent P2 asset {}",
          content.getAttributes().require(Asset.class));
      return;
    }
    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @TransactionalTouchBlob
  protected Content getAsset(final String name) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = facet(P2Facet.class).findAsset(tx, tx.findBucket(getRepository()), name);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return facet(P2Facet.class).toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  private Content storeCompositeMetadata(
      final String assetPath,
      final Content content,
      final AssetKind assetKind,
      final TokenMatcher.State matcherState,
      final String remoteUrl) throws IOException
  {
    String site = getSiteHash(matcherState);
    String filename = AssetKind.COMPOSITE_ARTIFACTS == assetKind ? "compositeArtifacts.xml" : "compositeContent.xml";
    URI baseUri = URI.create(remoteUrl);
    CompositeRepositoryRewriter rewriter = new CompositeRepositoryRewriter(baseUri, site == null);

    Consumer<Asset> assetModifier = asset -> asset.formatAttributes().set(CHILD_URLS, rewriter.getUrls());

    return rewriteAndStoreMetadata(assetPath, content, filename, assetKind, matcherState, remoteUrl,
        rewriter, assetModifier);
  }

  private Content storeArtifactsMetadata(
      final String assetPath,
      final Content content,
      final AssetKind assetKind,
      final TokenMatcher.State matcherState,
      final String remoteUrl) throws IOException
  {
    RemoveMirrorTransformer removeMirrorTransformer = new RemoveMirrorTransformer();

    Consumer<Asset> assetModifier = asset -> {
      Optional<String> mirrorUrl = removeMirrorTransformer.getMirrorsUrl();
      mirrorUrl.ifPresent(s -> asset.formatAttributes().set(MIRRORS_URL, s));
    };

    return rewriteAndStoreMetadata(assetPath, content, "artifacts.xml", assetKind, matcherState,
        remoteUrl, removeMirrorTransformer, assetModifier);
  }

  @TransactionalStoreBlob
  protected Content rewriteAndStoreMetadata(
      final String assetPath,
      final Content content,
      final String internalFilename,
      final AssetKind assetKind,
      final TokenMatcher.State matcherState,
      final String remoteUrl,
      final XmlTransformer transformer,
      final Consumer<Asset> assetModifier) throws IOException
  {
    String siteHash = getSiteHash(matcherState);
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    try (InputStream in = content.openInputStream()) {
      IOUtils.copy(in, buffer);
    }

    ByteArrayInputStream in = new ByteArrayInputStream(buffer.toByteArray());
    buffer.reset();

    String mimeType = contentValidator.determineContentType(false, () -> in, null, assetPath, content.getContentType());
    in.reset();
    StreamCopier.copierFor(mimeType, internalFilename, in, buffer).process(transformer);

    StorageFacet storageFacet = facet(StorageFacet.class);
    try (ByteArrayInputStream rewritten = new ByteArrayInputStream(buffer.toByteArray());
        TempBlob metadataContent = storageFacet.createTempBlob(rewritten, HASH_ALGORITHMS)) {
      StorageTx tx = UnitOfWork.currentTx();
      Bucket bucket = tx.findBucket(getRepository());

      Asset asset = facet(P2Facet.class).findAsset(tx, bucket, assetPath);
      if (asset == null) {
        asset = tx.createAsset(bucket, getRepository().getFormat());
        asset.name(assetPath);
        asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
      }
      asset.formatAttributes().set(REMOTE_URL, remoteUrl);
      asset.formatAttributes().set(REMOTE_HASH, siteHash);

      assetModifier.accept(asset);

      return facet(P2Facet.class).saveAsset(tx, asset, metadataContent, content);
    }
  }

  private String getSiteHash(final TokenMatcher.State matcherState) {
    String site = matcherState.getTokens().get("site");
    if (site == null) {
      return null;
    }
    return UriToSiteHashUtil.map(getRemoteUrl());
  }
}
