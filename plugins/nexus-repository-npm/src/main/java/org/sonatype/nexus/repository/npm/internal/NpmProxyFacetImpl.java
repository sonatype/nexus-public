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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.npm.NpmFacet;
import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexFilter;
import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexInvalidatedEvent;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.ViewUtils;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.findRepositoryRootAsset;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.saveRepositoryRoot;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.toContent;
import static org.sonatype.nexus.repository.npm.internal.NpmHandlers.packageId;
import static org.sonatype.nexus.repository.npm.internal.NpmHandlers.tarballName;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * npm {@link ProxyFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class NpmProxyFacetImpl
    extends ProxyFacetSupport
{
  @Override
  @Nullable
  protected Content fetch(final Context context, Content stale) throws IOException {
    try {
      return super.fetch(context, stale);
    }
    catch (NonResolvableTarballNameException e) { //NOSONAR
      log.debug("npm tarball URL not resolvable: {}", e.getMessage());
      return null;
    }
  }

  @Override
  protected Content getCachedContent(final Context context) throws IOException
  {
    ProxyTarget proxyTarget = context.getAttributes().require(ProxyTarget.class);

    if (proxyTarget.equals(ProxyTarget.SEARCH_V1_RESULTS)) {
      return null; // we do not cache search results
    }
    else if (ProxyTarget.PACKAGE == proxyTarget) {
      return getPackageRoot(packageId(matcherState(context)));
    }
    else if (ProxyTarget.TARBALL == proxyTarget) {
      TokenMatcher.State state = matcherState(context);
      return getTarball(packageId(state), tarballName(state));
    }
    else if (ProxyTarget.SEARCH_INDEX == proxyTarget) {
      Content fullIndex = getRepositoryRoot();
      if (fullIndex == null) {
        return null;
      }
      return NpmSearchIndexFilter.filterModifiedSince(
          fullIndex, NpmHandlers.indexSince(context.getRequest().getParameters()));
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
    if (proxyTarget.equals(ProxyTarget.SEARCH_V1_RESULTS)) {
      return content; // we do not cache search results
    }

    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content, NpmFacetUtils.HASH_ALGORITHMS)) {
      if (ProxyTarget.PACKAGE == proxyTarget) {
        return putPackageRoot(packageId(matcherState(context)), tempBlob, content);
      }
      else if (ProxyTarget.TARBALL == proxyTarget) {
        TokenMatcher.State state = matcherState(context);
        return putTarball(packageId(state), tarballName(state), tempBlob, content, context);
      }
      else if (ProxyTarget.SEARCH_INDEX == proxyTarget) {
        Content fullIndex = putRepositoryRoot(tempBlob, content);
        return NpmSearchIndexFilter.filterModifiedSince(
            fullIndex, NpmHandlers.indexSince(context.getRequest().getParameters()));
      }
      throw new IllegalStateException();
    }
  }

  @Override
  protected void indicateVerified(final Context context,
                                  final Content content,
                                  final CacheInfo cacheInfo) throws IOException
  {
    setCacheInfo(content, cacheInfo);
  }

  /**
   * The url is the same as the incoming request, except for tarballs, whose url is looked up in the metadata.
   */
  @Override
  protected String getUrl(@Nonnull final Context context) {
    String url = context.getRequest().getPath().substring(1); // omit leading slash
    ProxyTarget proxyTarget = context.getAttributes().require(ProxyTarget.class);
    if (ProxyTarget.TARBALL == proxyTarget) {
      TokenMatcher.State state = matcherState(context);
      try {
        NestedAttributesMap packageVersion = retrievePackageVersion(packageId(state), tarballName(state), context);
        url = packageVersion.child(NpmMetadataUtils.DIST).get(NpmMetadataUtils.TARBALL, String.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    else if (ProxyTarget.PACKAGE == proxyTarget) {
      NpmPackageId packageId = packageId(matcherState(context));
      if (packageId.scope() != null) {
        String newUrl = "@" + packageId.scope() + "%2f" + packageId.name();
        log.trace("Scoped package URL fix: {} -> {}", url, newUrl);
        url = newUrl;
      }
    }
    else if (ProxyTarget.SEARCH_V1_RESULTS == proxyTarget) {
      Parameters parameters = context.getRequest().getParameters();
      if (parameters != null) {
        return ViewUtils.buildUrlWithParameters(url, parameters);
      }
    }
    return url;
  }

  @TransactionalTouchMetadata
  public void setCacheInfo(final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent npm asset {}", content.getAttributes().require(Asset.class)
      );
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Nullable
  @TransactionalTouchBlob
  public Content getPackageRoot(final NpmPackageId packageId) throws IOException {
    checkNotNull(packageId);
    StorageTx tx = UnitOfWork.currentTx();
    Asset packageRootAsset = NpmFacetUtils.findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);
    if (packageRootAsset == null) {
      return null;
    }
    if (packageRootAsset.markAsDownloaded()) {
      tx.saveAsset(packageRootAsset);
    }
    NestedAttributesMap packageRoot = NpmFacetUtils.loadPackageRoot(tx, packageRootAsset);
    NpmMetadataUtils.rewriteTarballUrl(getRepository().getName(), packageRoot);
    return NpmFacetUtils.toContent(packageRootAsset, packageRoot);
  }

  private Content putPackageRoot(final NpmPackageId packageId,
                                 final TempBlob tempBlob,
                                 final Content payload) throws IOException
  {
    checkNotNull(packageId);
    checkNotNull(payload);
    checkNotNull(tempBlob);
    return doPutPackageRoot(packageId, NpmFacetUtils.parse(tempBlob), payload);
  }

  @TransactionalStoreBlob
  protected Content doPutPackageRoot(final NpmPackageId packageId,
                                     final NestedAttributesMap packageRoot,
                                     final Content content) throws IOException
  {
    log.debug("Storing package: {}", packageId);
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset asset = NpmFacetUtils.findPackageRootAsset(tx, bucket, packageId);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat()).name(packageId.id());
    }
    Content.applyToAsset(asset, Content.maintainLastModified(asset, content.getAttributes()));
    NpmFacetUtils.savePackageRoot(tx, asset, packageRoot);
    // we must have the round-trip to equip record with _rev rewrite the URLs (ret val is served to client)
    NestedAttributesMap storedPackageRoot = NpmFacetUtils.loadPackageRoot(tx, asset);
    NpmMetadataUtils.rewriteTarballUrl(getRepository().getName(), storedPackageRoot);
    return NpmFacetUtils.toContent(asset, storedPackageRoot);
  }

  @Nullable
  @TransactionalTouchBlob
  public Content getTarball(final NpmPackageId packageId, final String tarballName) throws IOException {
    checkNotNull(packageId);
    checkNotNull(tarballName);
    StorageTx tx = UnitOfWork.currentTx();
    return NpmFacetUtils.getTarballContent(tx, tx.findBucket(getRepository()), packageId, tarballName);
  }

  private Content putTarball(final NpmPackageId packageId,
                             final String tarballName,
                             final TempBlob tempBlob,
                             final Content content,
                             final Context context) throws IOException
  {
    checkNotNull(packageId);
    checkNotNull(tarballName);
    checkNotNull(tempBlob);
    checkNotNull(content);
    checkNotNull(context);
    return doPutTarball(packageId, tarballName, tempBlob, content);
  }

  @TransactionalStoreBlob
  protected Content doPutTarball(final NpmPackageId packageId,
                                 final String tarballName,
                                 final TempBlob tempBlob,
                                 final Content content) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    AssetBlob assetBlob = NpmFacetUtils.createTarballAssetBlob(tx, packageId, tarballName, tempBlob);

    NpmFacet npmFacet = facet(NpmFacet.class);
    npmFacet.putTarball(packageId.id(), tarballName, assetBlob, content.getAttributes());

    return NpmFacetUtils.getTarballContent(tx, tx.findBucket(getRepository()), packageId, tarballName);
  }

  private Content putRepositoryRoot(final TempBlob tempBlob,
                                    final Content content) throws IOException
  {
    checkNotNull(tempBlob);
    checkNotNull(content);
    Content result = doPutRepositoryRoot(tempBlob, content);
    getEventManager().post(new NpmSearchIndexInvalidatedEvent(getRepository()));
    return result;
  }

  @Nullable
  @TransactionalTouchBlob
  public Content getRepositoryRoot() throws IOException {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findRepositoryRootAsset(tx, tx.findBucket(getRepository()));
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @TransactionalStoreBlob
  protected Content doPutRepositoryRoot(final TempBlob tempBlob,
                                        final Content content) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findRepositoryRootAsset(tx, bucket);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat()).name(NpmFacetUtils.REPOSITORY_ROOT_ASSET);
    }
    return saveRepositoryRoot(tx, asset, tempBlob, content);
  }

  protected NestedAttributesMap retrievePackageVersion(final NpmPackageId packageId,
                                                       final String tarballName,
                                                       final Context context) throws IOException
  {
    // ensure package root is up to date
    retrievePackageRoot(packageId, context);
    // do the work in TX
    return retrievePackageVersionTx(packageId, tarballName);
  }

  @TransactionalTouchBlob
  protected NestedAttributesMap retrievePackageVersionTx(final NpmPackageId packageId,
                                                         final String tarballName) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    // get the asset as we need the original tarball url
    Asset asset = findPackageRootAsset(packageId);
    if (asset == null) {
      throw new NonResolvableTarballNameException("Could not find package " + packageId);
    }
    NestedAttributesMap packageRoot = NpmFacetUtils.loadPackageRoot(tx, asset);
    NestedAttributesMap packageVersion = NpmMetadataUtils.selectVersionByTarballName(packageRoot, tarballName);
    if (packageVersion == null) {
      throw new NonResolvableTarballNameException("Could not find package " + packageId + " version for " + tarballName);
    }
    return packageVersion;
  }

  @Nullable
  @Transactional
  public Asset findPackageRootAsset(final NpmPackageId packageId) {
    StorageTx tx = UnitOfWork.currentTx();
    return NpmFacetUtils.findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);
  }

  /**
   * This method MUST NOT be called from within a TX, as it dispatches a new request! It fails with
   * {@code java.lang.IllegalStateException}: "Transaction already in progress" otherwise!
   */
  private NestedAttributesMap retrievePackageRoot(final NpmPackageId packageId, final Context context)
      throws IOException
  {
    try {
      Request getRequest = new Request.Builder().action(GET).path("/" + packageId.id()).build();
      Response response = getRepository().facet(ViewFacet.class).dispatch(getRequest, context);
      if (response.getPayload() == null) {
        throw new IOException("Could not retrieve package " + packageId);
      }
      final InputStream packageRootIn = response.getPayload().openInputStream();
      return NpmFacetUtils.parse(() -> packageRootIn);
    }
    catch (IOException e) {
      throw e;
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new IOException(e);
    }
  }

  private TokenMatcher.State matcherState(final Context context) {
    return context.getAttributes().require(TokenMatcher.State.class);
  }

  /**
   * Internal exception thrown when resolving of tarball name to package version using package metadata fails.
   *
   * @see #retrievePackageVersionTx(NpmPackageId, String)
   * @see #getUrl(Context)
   * @see #fetch(Context, Content)
   */
  private static class NonResolvableTarballNameException extends RuntimeException {
    public NonResolvableTarballNameException(final String message) {
      super(message);
    }
  }

  public enum ProxyTarget
  {
    SEARCH_INDEX(CacheControllerHolder.METADATA),
    SEARCH_V1_RESULTS(CacheControllerHolder.METADATA),
    PACKAGE(CacheControllerHolder.METADATA),
    TARBALL(CacheControllerHolder.CONTENT);

    private final CacheType cacheType;

    ProxyTarget(final CacheType cacheType) {
      this.cacheType = checkNotNull(cacheType);
    }

    @Nonnull
    public CacheType getCacheType() {
      return cacheType;
    }
  }
}
