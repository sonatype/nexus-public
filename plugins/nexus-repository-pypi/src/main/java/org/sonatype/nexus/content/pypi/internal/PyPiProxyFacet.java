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
package org.sonatype.nexus.content.pypi.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.content.pypi.PypiContentFacet;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.pypi.internal.PyPiLink;
import org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.sonatype.nexus.content.pypi.internal.ContentPypiPathUtils.indexPath;
import static org.sonatype.nexus.content.pypi.internal.ContentPypiPathUtils.indexUriPath;
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
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.isSearchRequest;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.matcherState;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.name;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.path;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_HTML;

/**
 * @since 3.next
 */
@Named
public class PyPiProxyFacet
    extends ContentProxyFacetSupport
{
  private final TemplateHelper templateHelper;

  @Inject
  public PyPiProxyFacet(final TemplateHelper templateHelper) {
    this.templateHelper = checkNotNull(templateHelper);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) {
    AssetKind assetKind = getAssetKind(context);
    if (assetKind.equals(AssetKind.SEARCH)) {
      return null;  // we do not store search results
    }
    TokenMatcher.State state = matcherState(context);
    switch (assetKind) {
      case ROOT_INDEX:
        return content().getAsset(indexPath()).map(FluentAsset::download).orElse(null);
      case INDEX:
        return rewriteIndex(name(state));
      case PACKAGE:
        return content().getAsset(path(state)).map(FluentAsset::download).orElse(null);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  @Nullable
  protected Content fetch(final Context context, Content stale) throws IOException {
    try {
      return super.fetch(context, stale);
    }
    catch (NonResolvablePackageException ex) {
      log.error("Failed to resolve package {}", ex.getMessage());
      return null;
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = getAssetKind(context);
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
        return putPackage(path(state), content, assetKind);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    AssetKind assetKind = getAssetKind(context);

    if (PACKAGE == assetKind) {
      return getPackageUrl(context);
    }
    return context.getRequest().getPath().substring(1);
  }

  private Content rewriteIndex(final String name) {
    Optional<FluentAsset> fluentAsset = content().getAsset(indexPath(name));

    if (!fluentAsset.isPresent()) {
      return null;
    }
    Content content = fluentAsset.get().download();

    try (InputStream inputStream = content.openInputStream()) {
      List<PyPiLink> links = makeIndexLinksNexusPaths(name, inputStream);
      String html = buildIndexPage(templateHelper, name.substring(name.indexOf('/') + 1, name.length() - 1), links);
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

  @Override
  protected HttpRequestBase buildFetchHttpRequest(final URI uri, final Context context) {
    Request request = context.getRequest();
    // If we're doing a search operation, we have to proxy the content of the XML-RPC POST request to the PyPI server...
    if (isSearchRequest(request)) {
      Payload payload = checkNotNull(request.getPayload());
      try (InputStream inputStream = payload.openInputStream()) {
        String type = checkNotNull(payload.getContentType());
        ContentType contentType = ContentType.parse(type);
        HttpPost post = new HttpPost(uri);
        post.setEntity(new InputStreamEntity(inputStream, payload.getSize(), contentType));
        return post;
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return super.buildFetchHttpRequest(uri, context); // URI needs to be replaced here
  }

  @Nonnull
  @Override
  protected CacheController getCacheController(@Nonnull final Context context) {
    AssetKind assetKind = getAssetKind(context);
    return cacheControllerHolder.require(assetKind.getCacheType());
  }

  private Content putPackage(final String path, final Content content, final AssetKind assetKind) throws IOException {
    Map<String, String> attributes;
    TempBlob tempBlob = content().getTempBlob(content);
    try (InputStream is = tempBlob.get()) {
      attributes = extractMetadata(is);
    }

    String filename = extractFilenameFromPath(path);
    if (!attributes.containsKey(P_NAME)) {
      log.debug("No name found in metadata for {}, extracting from the filename.", filename);
      attributes.put(P_NAME, extractNameFromFilename(filename));
    }
    if (!attributes.containsKey(P_VERSION)) {
      log.debug("No version found in metadata for {}, extracting from the filename.", filename);
      attributes.put(P_VERSION, extractVersionFromFilename(filename));
    }
    attributes.put(P_ASSET_KIND, AssetKind.PACKAGE.name());

    String name = checkNotNull(attributes.get(P_NAME));
    String version = checkNotNull(attributes.get(P_VERSION));
    FluentComponent component = content().findOrCreateComponent(name, version, name);
    String summary = attributes.get(P_SUMMARY);
    if (summary != null) {
      component.withAttribute(P_SUMMARY, summary);
    }

    return content().findOrCreateAsset(path, component, assetKind.name())
        .attach(tempBlob)
        .markAsCached(content)
        .withAttribute(PyPiFormat.NAME, attributes)
        .download();
  }

  private Content putIndex(final String name, final Content content) throws IOException {
    String path = indexPath(name);
    try (InputStream inputStream = content.openInputStream()) {
      String html = IOUtils.toString(inputStream, UTF_8);

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

  private Content putRootIndex(final Content content) throws IOException {
    try (InputStream inputStream = content.openInputStream()) {
      List<PyPiLink> links = makeRootIndexRelative(inputStream);
      String indexPage = buildRootIndexPage(templateHelper, links);
      return storeHtmlPage(content, indexPage, ROOT_INDEX, indexPath());
    }
  }

  private Content storeHtmlPage(
      final Content content,
      final String indexPage,
      final AssetKind rootIndex,
      final String indexPathPrefix) throws IOException
  {
    try (ByteArrayInputStream stream = new ByteArrayInputStream(indexPage.getBytes(UTF_8))) {
      TempBlob tempBlob = content().getTempBlob(stream, content.getContentType());
      return content().findOrCreateAsset(indexPathPrefix, rootIndex.name())
          .withAttribute(PyPiFormat.NAME, singletonMap(P_ASSET_KIND, rootIndex.name()))
          .attach(tempBlob)
          .markAsCached(content)
          .download();
    }
  }

  /**
   * Retrieves the remote URL for a package using the package's simple index.
   */
  private String getPackageUrl(final Context context) {
    String packageName = PyPiPathUtils.packageNameFromPath(context.getRequest().getPath());
    String filename = PyPiFileUtils.extractFilenameFromPath(context.getRequest().getPath());

    PyPiLink link =
        getExistingPackageLink(packageName, filename)
            .orElseGet(() -> cachePackageRootMetadataAndRetrieveLink(context, packageName, filename));
    if (link == null) {
      throw new NonResolvablePackageException(
          "Unable to find reference for " + filename + " in package " + packageName);
    }

    URI remoteUrl = getRemoteUrl();
    if (!remoteUrl.getPath().endsWith("/")) {
      remoteUrl = URI.create(remoteUrl.toString() + "/");
    }
    remoteUrl = remoteUrl.resolve(indexUriPath(packageName));

    return remoteUrl.resolve(link.getLink()).toString();
  }

  /**
   * Using a cached index asset attempt to retrieve the link for a given package.
   */
  private Optional<PyPiLink> getExistingPackageLink(final String packageName, final String filename) {
    Optional<FluentAsset> fluentAsset = content().getAsset(indexPath(packageName));

    if (!fluentAsset.isPresent()) {
      return Optional.empty();
    }

    String rootFilename = filename.endsWith(".asc") ? filename.substring(0, filename.length() - 4) : filename;

    try (InputStream in = fluentAsset.get().download().openInputStream()) {
      return extractLinksFromIndex(in).stream().filter(link -> rootFilename.equals(link.getFile()))
          .findFirst();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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
   * Attempt to cache the index for the given package.
   */
  private void tryCachingPackageIndex(final String packageName, final Context context) throws IOException {
    try {
      Request getRequest = new Request.Builder().action(GET).path('/' + indexUriPath(packageName)).build();
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

  private AssetKind getAssetKind(final Context context) {
    return context.getAttributes().require(AssetKind.class);
  }

  private PypiContentFacet content() {
    return getRepository().facet(PypiContentFacet.class);
  }

  /**
   * Internal exception thrown when resolving of tarball name to package version using package metadata fails.
   *
   * @see #getUrl(Context)
   * @see #fetch(Context, Content)
   */
  private static class NonResolvablePackageException
      extends RuntimeException
  {
    private static final long serialVersionUID = 4744330472156130441L;

    public NonResolvablePackageException(final String message) {
      super(message);
    }
  }
}
