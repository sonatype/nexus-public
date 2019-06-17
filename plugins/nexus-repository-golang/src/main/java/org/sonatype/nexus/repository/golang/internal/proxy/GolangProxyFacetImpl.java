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
package org.sonatype.nexus.repository.golang.internal.proxy;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.golang.AssetKind;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.golang.internal.util.GolangDataAccess;
import org.sonatype.nexus.repository.golang.internal.util.GolangPathUtils;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.golang.internal.util.GolangDataAccess.HASH_ALGORITHMS;

/**
 * Go {@link ProxyFacet} implementation.
 *
 * @since 3.17
 */
@Named
public class GolangProxyFacetImpl
    extends ProxyFacetSupport
{
  private final GolangPathUtils golangPathUtils;

  private final GolangDataAccess golangDataAccess;

  @Inject
  public GolangProxyFacetImpl(final GolangPathUtils golangPathUtils,
                              final GolangDataAccess golangDataAccess)
  {
    this.golangPathUtils = checkNotNull(golangPathUtils);
    this.golangDataAccess = checkNotNull(golangDataAccess);
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
    TokenMatcher.State matcherState = golangPathUtils.matcherState(context);
    switch (assetKind) {
      case INFO:
      case MODULE:
      case PACKAGE:
        return getAsset(golangPathUtils.assetPath(matcherState));
      case LIST:
        return getAsset(golangPathUtils.listPath(matcherState));
      case LATEST:
        return getAsset(golangPathUtils.latestPath(matcherState));
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState = golangPathUtils.matcherState(context);

    switch (assetKind) {
      case INFO:
      case MODULE:
      case PACKAGE:
        GolangAttributes golangAttributes = golangPathUtils.getAttributesFromMatcherState(matcherState);
        return putComponent(golangAttributes, content, golangPathUtils.assetPath(matcherState), assetKind);
      case LIST:
        return putAsset(content, golangPathUtils.listPath(matcherState), assetKind);
      case LATEST:
        return putAsset(content, golangPathUtils.latestPath(matcherState), assetKind);
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  @TransactionalTouchBlob
  protected Content getAsset(final String path) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = golangDataAccess.findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      return null;
    }

    return golangDataAccess.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  private Content putAsset(final Content content,
                           final String assetPath,
                           final AssetKind assetKind) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      return golangDataAccess.maybeCreateAndSaveAsset(getRepository(),
          assetPath,
          assetKind,
          tempBlob,
          content
      );
    }
  }

  private Content putComponent(final GolangAttributes golangAttributes,
                               final Content content,
                               final String assetPath,
                               final AssetKind assetKind) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      return golangDataAccess.maybeCreateAndSaveComponent(getRepository(),
          golangAttributes,
          assetPath,
          tempBlob,
          content,
          assetKind);
    }
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    setCacheInfo(content, cacheInfo);
  }

  @TransactionalTouchMetadata
  public void setCacheInfo(final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent go asset {}", content.getAttributes().require(Asset.class)
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
}
