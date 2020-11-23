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
package org.sonatype.nexus.repository.content.npm.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.content.AttributeChange;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.npm.internal.NonResolvableTarballNameException;
import org.sonatype.nexus.repository.npm.internal.NpmAuditFacet;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.npm.internal.NpmProxyFacet;
import org.sonatype.nexus.repository.npm.orient.internal.search.legacy.NpmSearchIndexInvalidatedEvent;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.ViewUtils;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.RetryDeniedException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonatype.nexus.repository.content.npm.NpmContentFacet.metadataPath;
import static org.sonatype.nexus.repository.content.npm.internal.NpmContentFacetImpl.HASHING;
import static org.sonatype.nexus.repository.content.npm.internal.NpmFacetSupport.REPOSITORY_ROOT_ASSET;
import static org.sonatype.nexus.repository.content.npm.internal.NpmFacetSupport.formatAttributeExtractor;
import static org.sonatype.nexus.repository.content.npm.internal.NpmFacetSupport.tarballAssetName;
import static org.sonatype.nexus.repository.content.npm.internal.NpmFacetSupport.toNpmContent;
import static org.sonatype.nexus.repository.npm.NpmCoordinateUtil.extractVersion;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.P_NPM_LAST_MODIFIED;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.rewriteTarballUrlMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.bytes;
import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.parse;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.DIST;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.META_ID;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.findCachedVersionsRemovedFromRemote;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.maintainTime;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.mergePackageRoots;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.retrievePackageRoot;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.selectVersionByTarballName;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.indexSince;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.packageId;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.tarballName;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.PACKAGE;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.SEARCH_INDEX;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.SEARCH_V1_RESULTS;
import static org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils.errorInputStream;
import static org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexFilter.filterModifiedSince;

/**
 * npm {@link ProxyFacet} implementation.
 *
 * @since 3.next
 */
@Named
public class NpmContentProxyFacet
    extends ContentProxyFacetSupport
    implements NpmProxyFacet
{
  private final NpmPackageParser npmPackageParser;

  @Inject
  public NpmContentProxyFacet(final NpmPackageParser npmPackageParser) {this.npmPackageParser = npmPackageParser;}

  /**
   * Execute http client request.
   */
  @Override
  protected HttpResponse execute(final Context context, final HttpClient client, final HttpRequestBase request)
      throws IOException
  {
    String bearerToken = getRepository().facet(HttpClientFacet.class).getBearerToken();
    if (isNotBlank(bearerToken)) {
      request.setHeader("Authorization", "Bearer " + bearerToken);
    }
    return super.execute(context, client, request);
  }

  @Override
  @Nullable
  protected Content fetch(final Context context, final Content stale) throws IOException {
    try {
      return super.fetch(context, stale);
    }
    catch (NonResolvableTarballNameException e) { //NOSONAR
      log.debug("npm tarball URL not resolvable: {}", e.getMessage());
      return null;
    }
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    ProxyTarget npmProxyTarget = context.getAttributes().require(ProxyTarget.class);

    if (npmProxyTarget.equals(SEARCH_V1_RESULTS)) {
      return null; // we do not cache search results
    }

    if (PACKAGE == npmProxyTarget) {
      return getPackageRoot(packageId(matcherState(context)), context).orElse(null);
    }
    else if (ProxyTarget.DIST_TAGS == npmProxyTarget) {
      return getDistTags(packageId(matcherState(context))).orElse(null);
    }
    else if (ProxyTarget.TARBALL == npmProxyTarget) {
      TokenMatcher.State state = matcherState(context);
      return getTarball(packageId(state), tarballName(state)).orElse(null);
    }
    else if (SEARCH_INDEX == npmProxyTarget) {
      return getSearchIndex(context);
    }
    throw new IllegalStateException();
  }

  @Override
  @Nonnull
  protected CacheController getCacheController(@Nonnull final Context context) {
    final ProxyTarget proxyTarget = context.getAttributes().require(ProxyTarget.class);
    return cacheControllerHolder.require(proxyTarget.getCacheType());
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    ProxyTarget proxyTarget = context.getAttributes().require(ProxyTarget.class);

    if (proxyTarget.equals(SEARCH_V1_RESULTS)) {
      return content; // we do not cache search results
    }

    try (TempBlob tempBlob = contentFacet().blobs().ingest(content, HASHING)) {
      if (PACKAGE == proxyTarget) {
        return putPackageRoot(packageId(matcherState(context)), tempBlob, content);
      }
      else if (ProxyTarget.DIST_TAGS == proxyTarget) {
        NpmPackageId packageId = packageId(matcherState(context));
        putPackageRoot(packageId, tempBlob, content);
        return getDistTags(packageId).orElse(null);
      }
      else if (ProxyTarget.TARBALL == proxyTarget) {
        TokenMatcher.State state = matcherState(context);
        return putTarball(packageId(state), tarballName(state), tempBlob, content);
      }
      else if (SEARCH_INDEX == proxyTarget) {
        return filterModifiedSince(putRepositoryRoot(tempBlob, content), indexSince(context.getRequest().getParameters()));
      }
      throw new IllegalStateException();
    }
  }

  private Content putRepositoryRoot(final TempBlob tempBlob, final Content content) {
    Content result = contentFacet().putRepositoryRoot(createNpmContent(tempBlob, content));
    getEventManager().post(new NpmSearchIndexInvalidatedEvent(getRepository()));
    return result;
  }

  private NpmContent createNpmContent(final TempBlob tempBlob, final Content content) {
    return new NpmContent(new NpmStreamPayload(tempBlob::get), content);
  }

  private Content putTarball(
      final NpmPackageId packageId,
      final String tarballName,
      final TempBlob tempBlob, final Content content) throws IOException
  {
    String version = extractVersion(tarballName);
    Map<String, Object> formatAttributes =
        formatAttributeExtractor(packageId.id(), version, tempBlob).apply(npmPackageParser, log);
    return contentFacet().put(packageId, tarballName, version, formatAttributes, createNpmContent(tempBlob, content));
  }

  /**
   * The url is the same as the incoming request, except for tarballs, whose url is looked up in the metadata.
   */
  @Override
  protected String getUrl(@Nonnull final Context context) {
    String url = context.getRequest().getPath().substring(1); // omit leading slash
    ProxyTarget proxyTarget = context.getAttributes().require(ProxyTarget.class);

    if (ProxyTarget.TARBALL == proxyTarget) {
      url = getUrlFromPackageVersion(context);
    }
    else if (PACKAGE == proxyTarget) {
      NpmPackageId npmPackageId = packageId(matcherState(context));
      url = defaultIfBlank(getUrlFromPackageScope(npmPackageId, url), url);
    }
    else if (ProxyTarget.DIST_TAGS == proxyTarget) {
      NpmPackageId npmPackageId = packageId(matcherState(context));
      url = defaultIfBlank(getUrlFromPackageScope(npmPackageId, url), npmPackageId.name());
    }
    else if (SEARCH_V1_RESULTS == proxyTarget) {
      url = buildUrlWithParameters(context, url);
    }
    return url;
  }

  private String buildUrlWithParameters(@Nonnull final Context context, String url) {
    Parameters parameters = context.getRequest().getParameters();
    if (parameters != null) {
      url = ViewUtils.buildUrlWithParameters(url, parameters);
    }
    return url;
  }

  private String getUrlFromPackageScope(final NpmPackageId packageId, final String oldUrl) {
    if (packageId.scope() != null) {
      String newUrl = "@" + packageId.scope() + "%2f" + packageId.name();
      log.trace("Scoped package URL fix: {} -> {}", oldUrl, newUrl);
      return newUrl;
    }
    return null;
  }

  private String getUrlFromPackageVersion(@Nonnull final Context context) {
    TokenMatcher.State state = matcherState(context);
    try {
      NestedAttributesMap
          packageVersion = retrievePackageVersion(packageId(state), tarballName(state), context);
      return packageVersion.child(DIST).get(NpmMetadataUtils.TARBALL, String.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private NestedAttributesMap retrievePackageVersion(
      final NpmPackageId packageId,
      final String tarballName,
      final Context context) throws IOException
  {
    // ensure package root is up to date
    retrievePackageRoot(packageId, context, getRepository());

    Optional<FluentAsset> packageRootAsset = findPackageRootAsset(packageId);
    if (!packageRootAsset.isPresent()) {
      throw new NonResolvableTarballNameException("Could not find package " + packageId);
    }
    NestedAttributesMap packageVersion =
        selectVersionByTarballName(loadPackageRoot(packageRootAsset.get()), tarballName);
    if (packageVersion == null) {
      throw new NonResolvableTarballNameException(
          "Could not find package " + packageId + " version for " + tarballName);
    }
    return packageVersion;
  }

  private TokenMatcher.State matcherState(final Context context) {
    return context.getAttributes().require(TokenMatcher.State.class);
  }

  private Optional<Content> getPackageRoot(final NpmPackageId packageId, final Context context) {
    Optional<FluentAsset> asset = findPackageRootAsset(packageId);
    try {
      return asset.map(theAsset -> packageRootNpmContent(packageId, theAsset));
    }
    catch (MissingBlobException exception) {
      doGetOnMissingBlob(context, exception, asset.get());
      return findPackageRootAsset(packageId)
          .map(theAsset -> packageRootNpmContent(packageId, theAsset));
    }
  }

  private NpmContent packageRootNpmContent(final NpmPackageId packageId, final FluentAsset asset) {
    return assetToNpmContent(asset, packageId).packageId(asset.path().substring(1));
  }

  private NpmContent assetToNpmContent(final FluentAsset packageRootAsset, final NpmPackageId packageId) {
    return toNpmContent(packageRootAsset.download())
        .fieldMatchers(rewriteTarballUrlMatcher(getRepository(), packageId.id()));
  }

  private Optional<FluentAsset> findPackageRootAsset(final NpmPackageId npmPackageId) {
    return getAsset(metadataPath(npmPackageId));
  }

  private Optional<FluentAsset> getAsset(final String path) {
    return contentFacet().assets().path(path).find();
  }

  private NpmContentFacet contentFacet() {
    return getRepository().facet(NpmContentFacet.class);
  }

  private InputStream doGetOnMissingBlob(final Context context, final MissingBlobException e, final FluentAsset asset)
  {
    log.warn("Unable to find blob {} for {}, will check remote", e.getBlobRef(), getUrl(context));

    try {
      return doGet(context, null).openInputStream();
    }
    catch (IOException ex) {
      log.error("Unable to check remote for missing blob {} at {}", e.getBlobRef(), getUrl(context), ex);

      // we are likely being requested after a 200 ok response is given, stream out error stream.
      return errorInputStream(format("Missing blob for %s. Unable to check remote %s for retrieving latest copy.",
          asset.path(), getUrl(context)));
    }
  }

  private Optional<Content> getDistTags(final NpmPackageId packageId) throws IOException {
    return NpmFacetSupport.loadPackageRoot(packageId, contentFacet())
        .map(packageRootJson -> packageRootJson.child(NpmMetadataUtils.DIST_TAGS))
        .map(distTags -> distTagsToContent(distTags, packageId));
  }

  private Content distTagsToContent(final NestedAttributesMap distTags, final NpmPackageId packageId) {
    try {
      return NpmFacetSupport.distTagsToContent(distTags);
    }
    catch (IOException e) {
      log.error("Unable to read packageRoot {}", packageId.id(), e);
    }
    return null;
  }

  private Optional<Content> getTarball(final NpmPackageId packageId, final String tarballName) {
    return getAsset(tarballAssetName(packageId, tarballName)).map(FluentAsset::download);
  }

  private Content getSearchIndex(final Context context) throws IOException {
    Optional<Content> content = getAsset(REPOSITORY_ROOT_ASSET).map(FluentAsset::download);
    if (content.isPresent()) {
      return filterModifiedSince(content.get(), indexSince(context.getRequest().getParameters()));
    }
    return null;
  }

  private Content putPackageRoot(
      final NpmPackageId packageId,
      final TempBlob tempBlob,
      final Content content)
      throws IOException
  {
    NestedAttributesMap packageRoot = parse(tempBlob);
    try {
      return doPutPackageRoot(packageId, packageRoot, true, content);
    }
    catch (RetryDeniedException | MissingBlobException ex) {
      return maybeHandleMissingBlob(ex, packageId, packageRoot, content);
    }
  }

  private Content doPutPackageRoot(
      final NpmPackageId packageId,
      final NestedAttributesMap packageRoot,
      final boolean mergePackageRoot, final Content content) throws IOException
  {
    log.debug("Storing package: {}", packageId);

    NestedAttributesMap newPackageRoot = packageRoot;

    Optional<FluentAsset> maybePackageRootAsset = findPackageRootAsset(packageId);
    if (maybePackageRootAsset.isPresent() && mergePackageRoot) {
      newPackageRoot = mergeNewRootWithExistingRoot(newPackageRoot, maybePackageRootAsset.get());
    }

    FluentAsset asset = savePackageRoot(packageId, newPackageRoot, content);
    return assetToNpmContent(asset, packageId).revId(asset.path()).packageId(packageId.id());
  }

  private FluentAsset savePackageRoot(
      final NpmPackageId packageId,
      final NestedAttributesMap packageRoot,
      final Content content) throws IOException
  {
    packageRoot.remove(META_ID);
    packageRoot.remove("_attachments");
    Date date = maintainTime(packageRoot).toDate();
    NpmStreamPayload payload = new NpmStreamPayload(() -> new ByteArrayInputStream(bytes(packageRoot)));
    return contentFacet().put(packageId, new NpmContent(payload, content))
        .attributes(AttributeChange.SET, P_NPM_LAST_MODIFIED, date);
  }

  /*
   * Merge new root into existing root. This means any already fetched packages that are removed from the upstream
   * repository will still be available from NXRM. In the cases where the same version exists in both then the new
   * package root wins.
   */
  private NestedAttributesMap mergeNewRootWithExistingRoot(
      final NestedAttributesMap newPackageRoot,
      final FluentAsset asset) throws IOException
  {
    NestedAttributesMap existingPackageRoot = loadPackageRoot(asset);
    return mergePackageRoots(newPackageRoot, existingPackageRoot,
        findCachedVersionsRemovedFromRemote(existingPackageRoot, newPackageRoot, componentExists()));
  }

  private BiFunction<NpmPackageId, String, Boolean> componentExists() {
    return (packageId, version) -> {
      FluentComponentBuilder componentBuilder = contentFacet().components().name(packageId.name()).version(version);
      if (packageId.scope() != null) {
        componentBuilder = componentBuilder.namespace(packageId.scope());
      }
      return componentBuilder.find().isPresent();
    };
  }

  private NestedAttributesMap loadPackageRoot(final FluentAsset asset) throws IOException {
    try (InputStream inputStream = asset.download().openInputStream()) {
      NestedAttributesMap metadata = parse(() -> inputStream);
      metadata.set(META_ID, asset.path());
      return metadata;
    }
  }

  private Content maybeHandleMissingBlob(
      final RuntimeException e,
      final NpmPackageId packageId,
      final NestedAttributesMap packageRoot, final Content content) throws IOException
  {
    BlobRef blobRef = null;

    if (e instanceof MissingBlobException) {
      blobRef = ((MissingBlobException) e).getBlobRef();
    }
    else if (e.getCause() instanceof MissingBlobException) {
      blobRef = ((MissingBlobException) e.getCause()).getBlobRef();
    }

    if (nonNull(blobRef)) {
      log.warn("Unable to find blob {} for {}, skipping merge of package root", blobRef, packageId);
      return doPutPackageRoot(packageId, packageRoot, false, content);
    }

    // when we have no blob ref, just throw the original runtime exception
    throw e;
  }

  @Override
  public void invalidateProxyCaches() {
    super.invalidateProxyCaches();
    final NpmAuditFacet npmAuditFacet = getRepository().facet(NpmAuditFacet.class);
    npmAuditFacet.clearCache();
  }
}
