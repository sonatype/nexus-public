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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.INDEX;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.copyAttributes;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findAsset;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findComponent;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.saveAsset;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.toContent;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractFilenameFromPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractNameFromFilename;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractVersionFromFilename;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.buildIndexPage;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.buildRootIndexPage;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.makeIndexRelative;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.makeRootIndexRelative;
import static org.sonatype.nexus.repository.pypi.internal.PyPiInfoUtils.extractMetadata;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.INDEX_PATH_PREFIX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.indexPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.isSearchRequest;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.matcherState;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.name;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.packagesPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.path;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * PyPI {@link ProxyFacet} implementation.
 *
 * @since 3.1
 */
@Named
public class PyPiProxyFacetImpl
    extends ProxyFacetSupport
{
  private final TemplateHelper templateHelper;

  @Inject
  public PyPiProxyFacetImpl(final TemplateHelper templateHelper) {
    this.templateHelper = checkNotNull(templateHelper);
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
    if (assetKind.equals(AssetKind.SEARCH)) {
      return null;  // we do not store search results
    }
    TokenMatcher.State state = matcherState(context);
    switch (assetKind) {
      case ROOT_INDEX:
        return getAsset(INDEX_PATH_PREFIX);
      case INDEX:
        return getAsset(indexPath(name(state)));
      case PACKAGE:
        return getAsset(packagesPath(path(state)));
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    if (assetKind.equals(AssetKind.SEARCH)) {
      return content;  // we do not store search results
    }
    TokenMatcher.State state = matcherState(context);
    switch (assetKind) {
      case ROOT_INDEX:
        return putRootIndex(content);
      case INDEX:
        return putIndex(indexPath(name(state)), content);
      case PACKAGE:
        return putPackage(packagesPath(path(state)), content);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    setCacheInfo(content, cacheInfo);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    String url = context.getRequest().getPath();
    return url.substring(1);
  }

  @Override
  protected HttpRequestBase buildFetchHttpRequest(URI uri, Context context) {
    Request request = context.getRequest();
    // If we're doing a search operation, we have to proxy the content of the XML-RPC POST request to the PyPI server...
    if (isSearchRequest(request)) {
      Payload payload = checkNotNull(request.getPayload());
      try {
        ContentType contentType = ContentType.parse(payload.getContentType());
        HttpPost post = new HttpPost(uri);
        post.setEntity(new InputStreamEntity(payload.openInputStream(), payload.getSize(), contentType));
        return post;
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return super.buildFetchHttpRequest(uri, context);
  }

  @TransactionalTouchBlob
  protected Content getAsset(final String name) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), name);
    if (asset == null) {
      return null;
    }

    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  private Content putPackage(final String path, final Content content) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content, HASH_ALGORITHMS)) {
      return doPutPackage(path, tempBlob, content);
    }
  }

  @TransactionalStoreBlob
  protected Content doPutPackage(final String path,
                                 final TempBlob tempBlob,
                                 final Payload payload) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    String filename = extractFilenameFromPath(path);
    Map<String, String> attributes;
    try (InputStream is = tempBlob.get()) {
      attributes = extractMetadata(is);
    }

    if (!attributes.containsKey(P_NAME)) {
      log.debug("No name found in metadata for {}, extracting from filename.", filename);
      attributes.put(P_NAME, extractNameFromFilename(filename));
    }
    if (!attributes.containsKey(P_VERSION)) {
      log.debug("No version found in metadata for {}, extracting from filename.", filename);
      attributes.put(P_VERSION, extractVersionFromFilename(filename));
    }

    String name = attributes.get(P_NAME);
    String version = attributes.get(P_VERSION);

    Component component = findComponent(tx, getRepository(), name, version);
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat()).name(name).version(version);
    }
    if (component.isNew() || attributes.containsKey(P_SUMMARY)) {
      component.formatAttributes().set(P_SUMMARY, attributes.get(P_SUMMARY));
      tx.saveComponent(component);
    }

    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(path);
      asset.formatAttributes().set(P_ASSET_KIND, AssetKind.PACKAGE.name());
    }

    copyAttributes(asset, attributes);
    return saveAsset(tx, asset, tempBlob, payload);
  }

  private Content putRootIndex(final Content content) throws IOException {
    try (InputStream inputStream = content.openInputStream()) {
      Map<String, String> links = makeRootIndexRelative(inputStream);
      String indexPage = buildRootIndexPage(templateHelper, links);
      return storeHtmlPage(content, indexPage, ROOT_INDEX, INDEX_PATH_PREFIX);
    }
  }

  private Content putIndex(final String name, final Content content) throws IOException {
    String html;
    try (InputStream inputStream = content.openInputStream()) {
      Map<String, String> links = makeIndexRelative(inputStream);
      html = buildIndexPage(templateHelper, name.substring(name.indexOf('/') + 1, name.length() - 1), links);
    }
    return storeHtmlPage(content, html, INDEX, name);
  }

  private Content storeHtmlPage(final Content content,
                                final String indexPage,
                                final AssetKind rootIndex,
                                final String indexPathPrefix) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (ByteArrayInputStream stream = new ByteArrayInputStream(indexPage.getBytes(StandardCharsets.UTF_8))) {
      try (TempBlob tempBlob = storageFacet.createTempBlob(stream, PyPiDataUtils.HASH_ALGORITHMS)) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(P_ASSET_KIND, rootIndex.name());
        return doPutIndex(indexPathPrefix, tempBlob, content, attributes);
      }
    }
  }

  @TransactionalStoreBlob
  protected Content doPutIndex(final String name,
                               final TempBlob tempBlob,
                               final Payload payload,
                               final Map<String, Object> attributes) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset asset = findAsset(tx, bucket, name);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(name);
    }
    if (attributes != null) {
      for (Entry<String, Object> entry : attributes.entrySet()) {
        asset.formatAttributes().set(entry.getKey(), entry.getValue());
      }
    }
    return saveAsset(tx, asset, tempBlob, payload);
  }

  @TransactionalTouchMetadata
  protected void setCacheInfo(final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent pypi asset {}", content.getAttributes().require(Asset.class)
      );
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Nonnull
  @Override
  protected CacheController getCacheController(@Nonnull final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    return cacheControllerHolder.require(assetKind.getCacheType());
  }
}
