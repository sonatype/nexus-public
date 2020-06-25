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
package org.sonatype.nexus.repository.pypi.internal.orient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils;
import org.sonatype.nexus.repository.pypi.internal.PyPiLink;
import org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils;
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
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.INDEX;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.PACKAGE;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractFilenameFromPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractNameFromFilename;
import static org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils.extractVersionFromFilename;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.buildIndexPage;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.buildRootIndexPage;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.extractLinksFromIndex;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.makeIndexLinksNexusPaths;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.makeRootIndexRelative;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.validateIndexLinks;
import static org.sonatype.nexus.repository.pypi.internal.PyPiInfoUtils.extractMetadata;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.INDEX_PATH_PREFIX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.indexPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.isSearchRequest;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.matcherState;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.name;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.path;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.copyAttributes;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.findAsset;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.findComponent;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.saveAsset;
import static org.sonatype.nexus.repository.pypi.internal.orient.OrientPyPiDataUtils.toContent;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_HTML;

/**
 * PyPI {@link ProxyFacet} implementation.
 *
 * @since 3.1
 */
@Named
public class OrientPyPiProxyFacetImpl
    extends ProxyFacetSupport
{
  private final TemplateHelper templateHelper;

  @Inject
  public OrientPyPiProxyFacetImpl(final TemplateHelper templateHelper) {
    this.templateHelper = checkNotNull(templateHelper);
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
        return rewriteIndex(name(state));
      case PACKAGE:
        return getAsset(path(state));
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  @Nullable
  protected Content fetch(final Context context, Content stale) throws IOException {
    try {
      return super.fetch(context, stale);
    } catch (NonResolvablePackageException ex) {
      log.error("Failed to resolve package {}", ex.getMessage());
      return null;
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
        String name = name(state);
        return putIndex(name, content);
      case PACKAGE:
        return putPackage(path(state), content);
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
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);

    if (PACKAGE == assetKind) {
      return getPackageUrl(context);
    }
    return context.getRequest().getPath().substring(1);
  }

  /**
   * Retrieves the remote URL for a package using the package's simple index.
   */
  private String getPackageUrl(final Context context) {
    String packageName = PyPiPathUtils.packageNameFromPath(context.getRequest().getPath());
    String filename = PyPiFileUtils.extractFilenameFromPath(context.getRequest().getPath());

    PyPiLink link =
        getExistingPackageLink(packageName, filename).orElseGet(() -> cachePackageRootMetadataAndRetrieveLink(context, packageName, filename));
    if (link == null) {
      throw new NonResolvablePackageException(
          "Unable to find reference for " + filename + " in package " + packageName);
    }

    URI remoteUrl = getRemoteUrl();
    if (!remoteUrl.getPath().endsWith("/")) {
      remoteUrl = URI.create(remoteUrl.toString() + "/");
    }
    remoteUrl = remoteUrl.resolve(indexPath(packageName));

    return remoteUrl.resolve(link.getLink()).toString();
  }

  /**
   * Retrieve the remote package index then attempt to find the matching upstream link.
   */
  private PyPiLink cachePackageRootMetadataAndRetrieveLink(
      final Context context,
      final String packageName,
      final String filename)
  {
    try {
      tryCachingPackageIndex(packageName, context);
      return getExistingPackageLink(packageName, filename).orElse(null);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Using a cached index asset attempt to retrieve the link for a given package.
   */
  private Optional<PyPiLink> getExistingPackageLink(final String packageName, final String filename) {
    Content index = getAsset(indexPath(packageName));

    if (index == null) {
      return Optional.empty();
    }

    String rootFilename = filename.endsWith(".asc") ? filename.substring(0, filename.length() - 4) : filename;

    try (InputStream in = index.openInputStream()) {
      return extractLinksFromIndex(in).stream().filter(link -> rootFilename.equals(link.getFile()))
          .findFirst();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Attempt to cache the index for the given package.
   */
  private void tryCachingPackageIndex(final String packageName, final Context context) throws IOException {
    try {
      Request getRequest = new Request.Builder().action(GET).path('/'+indexPath(packageName)).build();
      Response response = getRepository().facet(ViewFacet.class).dispatch(getRequest, context);
      if (!response.getStatus().isSuccessful()) {
        log.debug("Could not retrieve package {}", packageName);
      }
    }
    catch (IOException e) {
      throw e;
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new IOException(e);
    }
  }

  @Override
  protected HttpRequestBase buildFetchHttpRequest(final URI uri, final Context context) {
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
    return super.buildFetchHttpRequest(uri, context); // URI needs to be replaced here
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
      List<PyPiLink> links = makeRootIndexRelative(inputStream);
      String indexPage = buildRootIndexPage(templateHelper, links);
      return storeHtmlPage(content, indexPage, ROOT_INDEX, INDEX_PATH_PREFIX);
    }
  }

  private Content putIndex(final String name, final Content content) throws IOException {
    String html;
    String path = indexPath(name);
    try (InputStream inputStream = content.openInputStream()) {
      html = IOUtils.toString(inputStream);

      if (!validateIndexLinks(name, extractLinksFromIndex(html))) {
        return null;
      }
      makeIndexLinksNexusPaths(name, inputStream);

      storeHtmlPage(content, html, INDEX, path);
      return rewriteIndex(name);
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }

  private Content rewriteIndex(final String name) throws IOException {
    Content content = getAsset(indexPath(name));

    if (content == null) {
      return null;
    }

    String html;
    try (InputStream inputStream = content.openInputStream()) {
      List<PyPiLink> links = makeIndexLinksNexusPaths(name, inputStream);
      html = buildIndexPage(templateHelper, name.substring(name.indexOf('/') + 1, name.length() - 1), links);
      Content newContent = new Content(new BytesPayload(html.getBytes(), TEXT_HTML));
      content.getAttributes().forEach(e -> newContent.getAttributes().set(e.getKey(), e.getValue()));
      return newContent;
    }
    catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.warn("An error occurred re-writing the index for package {}", name, e);
      }
      else {
        log.warn("An error occurred re-writing the index for package {}", name);
      }
      return null;
    }
  }

  private Content storeHtmlPage(final Content content,
                                final String indexPage,
                                final AssetKind rootIndex,
                                final String indexPathPrefix) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (ByteArrayInputStream stream = new ByteArrayInputStream(indexPage.getBytes(StandardCharsets.UTF_8))) {
      try (TempBlob tempBlob = storageFacet.createTempBlob(stream, HASH_ALGORITHMS)) {
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

  /**
   * Internal exception thrown when resolving of tarball name to package version using package metadata fails.
   *
   * @see #retrievePackageVersionTx(NpmPackageId, String)
   * @see #getUrl(Context)
   * @see #fetch(Context, Content)
   */
  private static class NonResolvablePackageException
      extends RuntimeException
  {
    public NonResolvablePackageException(final String message) {
      super(message);
    }
  }
}
