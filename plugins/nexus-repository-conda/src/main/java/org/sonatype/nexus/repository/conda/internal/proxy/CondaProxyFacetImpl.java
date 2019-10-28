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
package org.sonatype.nexus.repository.conda.internal.proxy;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.conda.CondaFacet;
import org.sonatype.nexus.repository.conda.internal.AssetKind;
import org.sonatype.nexus.repository.conda.internal.CondaFacetImpl;
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
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.conda.internal.AssetKind.ARCH_CONDA_PACKAGE;
import static org.sonatype.nexus.repository.conda.internal.AssetKind.ARCH_TAR_PACKAGE;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.CHANNELDATA_JSON;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.INDEX_HTML;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.REPODATA2_JSON;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.REPODATA_JSON;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.REPODATA_JSON_BZ2;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.RSS_XML;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.arch;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.buildArchAssetPath;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.buildAssetPath;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.buildCondaPackagePath;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.matcherState;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.name;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.version;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Conda {@link ProxyFacet} implementation.
 *
 * @since 3.19
 */
@Named
@Facet.Exposed
public class CondaProxyFacetImpl
    extends ProxyFacetSupport
{
  @Nullable
  @Override
  protected Content getCachedContent(final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState = matcherState(context);
    String assetPath;
    switch (assetKind) {
      case CHANNEL_INDEX_HTML:
        assetPath = buildAssetPath(matcherState, INDEX_HTML);
        break;
      case CHANNEL_DATA_JSON:
        assetPath = buildAssetPath(matcherState, CHANNELDATA_JSON);
        break;
      case CHANNEL_RSS_XML:
        assetPath = buildAssetPath(matcherState, RSS_XML);
        break;
      case ARCH_INDEX_HTML:
        assetPath = buildArchAssetPath(matcherState, INDEX_HTML);
        break;
      case ARCH_REPODATA_JSON:
        assetPath = buildArchAssetPath(matcherState, REPODATA_JSON);
        break;
      case ARCH_REPODATA_JSON_BZ2:
        assetPath = buildArchAssetPath(matcherState, REPODATA_JSON_BZ2);
        break;
      case ARCH_REPODATA2_JSON:
        assetPath = buildArchAssetPath(matcherState, REPODATA2_JSON);
        break;
      case ARCH_TAR_PACKAGE:
      case ARCH_CONDA_PACKAGE:
        assetPath = buildCondaPackagePath(matcherState);
        break;
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
    return getAsset(assetPath);
  }

  @TransactionalTouchBlob
  protected Content getAsset(final String assetPath) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = getCondaFacet().findAsset(tx, tx.findBucket(getRepository()), assetPath);
    if (asset == null) {
      return null;
    }
    return getCondaFacet().toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException
  {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState = matcherState(context);
    String assetPath;
    switch (assetKind) {
      case ARCH_TAR_PACKAGE:
      case ARCH_CONDA_PACKAGE:
        return putCondaPackage(content, assetKind, matcherState);
      case CHANNEL_INDEX_HTML:
        assetPath = buildAssetPath(matcherState, INDEX_HTML);
        break;
      case CHANNEL_DATA_JSON:
        assetPath = buildAssetPath(matcherState, CHANNELDATA_JSON);
        break;
      case CHANNEL_RSS_XML:
        assetPath = buildAssetPath(matcherState, RSS_XML);
        break;
      case ARCH_INDEX_HTML:
        assetPath = buildArchAssetPath(matcherState, INDEX_HTML);
        break;
      case ARCH_REPODATA_JSON:
        assetPath = buildArchAssetPath(matcherState, REPODATA_JSON);
        break;
      case ARCH_REPODATA_JSON_BZ2:
        assetPath = buildArchAssetPath(matcherState, REPODATA_JSON_BZ2);
        break;
      case ARCH_REPODATA2_JSON:
        assetPath = buildArchAssetPath(matcherState, REPODATA2_JSON);
        break;
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
    return putMetadata(content, assetKind, assetPath);
  }

  private Content putCondaPackage(final Content content,
                                  final AssetKind assetKind,
                                  final TokenMatcher.State matcherState) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);

    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), CondaFacetImpl.HASH_ALGORITHMS)) {
      Component component = findOrCreateComponent(arch(matcherState), name(matcherState), version(matcherState));

      return findOrCreateAsset(tempBlob, content, assetKind, buildCondaPackagePath(matcherState), component);
    }
  }

  @TransactionalStoreBlob
  protected Component findOrCreateComponent(final String arch, final String name, final String version)
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Component component = getCondaFacet().findComponent(tx, getRepository(), arch, name, version);

    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat())
          .group(arch)
          .name(name)
          .version(version);
    }
    tx.saveComponent(component);

    return component;
  }

  private Content putMetadata(final Content content, final AssetKind assetKind, final String assetPath)
      throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);

    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), CondaFacetImpl.HASH_ALGORITHMS)) {
      return findOrCreateAsset(tempBlob, content, assetKind, assetPath, null);
    }
  }

  @TransactionalStoreBlob
  protected Content findOrCreateAsset(final TempBlob tempBlob,
                                      final Content content,
                                      final AssetKind assetKind,
                                      final String assetPath,
                                      final Component component) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset asset = getCondaFacet().findAsset(tx, bucket, assetPath);

    if (asset == null) {
      if (ARCH_TAR_PACKAGE.equals(assetKind) || ARCH_CONDA_PACKAGE.equals(assetKind)) {
        asset = tx.createAsset(bucket, component);
      }
      else {
        asset = tx.createAsset(bucket, getRepository().getFormat());
      }
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
    }

    return getCondaFacet().saveAsset(tx, asset, tempBlob, content);
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
  {
    setCacheInfo(content, cacheInfo);
  }

  @TransactionalTouchMetadata
  protected void setCacheInfo(final Content content, final CacheInfo cacheInfo)
  {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent Conda asset {}", content.getAttributes().require(Asset.class)
      );
      return;
    }
    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1);
  }

  private CondaFacet getCondaFacet() {
    return getRepository().facet(CondaFacet.class);
  }
}
