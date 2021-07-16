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
package org.sonatype.nexus.repository.r.orient.internal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.r.AssetKind;
import org.sonatype.nexus.repository.r.orient.OrientRFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.r.internal.util.RDescriptionUtils.extractDescriptionFromArchive;
import static org.sonatype.nexus.repository.r.internal.util.RMetadataUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.extractRequestPath;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.saveAsset;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.toContent;

/**
 * R {@link ProxyFacet} implementation.
 *
 * @since 3.28
 */
@Named
public class OrientRProxyFacetImpl
    extends ProxyFacetSupport
{
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
      case RDS_METADATA:
      case PACKAGES:
      case ARCHIVE:
        return getAsset(extractRequestPath(context));
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case RDS_METADATA:
      case PACKAGES:
        return putMetadata(extractRequestPath(context), content);
      case ARCHIVE:
        return putArchive(extractRequestPath(context), content);
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

  @TransactionalTouchMetadata
  public void setCacheInfo(final Content content, final CacheInfo cacheInfo) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent R asset {}", content.getAttributes().require(Asset.class)
      );
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  private Content putArchive(final String path, final Content content) throws IOException {
    checkNotNull(path);
    checkNotNull(content);
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      return doPutArchive(path, tempBlob, content);
    }
  }

  @TransactionalStoreBlob
  protected Content doPutArchive(final String path,
                                 final TempBlob archiveContent,
                                 final Content content) throws IOException
  {
    OrientRFacet rFacet = facet(OrientRFacet.class);
    StorageTx tx = UnitOfWork.currentTx();

    Map<String, String> attributes;
    try (InputStream is = archiveContent.get()) {
      attributes = extractDescriptionFromArchive(path, is);
    }

    Component component = rFacet.findOrCreateComponent(tx, path, attributes);
    Asset asset = rFacet.findOrCreateAsset(tx, component, path, attributes);
    return saveAsset(tx, asset, archiveContent, content);
  }

  private Content putMetadata(final String path, final Content content)
      throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      return doPutMetadata(path, tempBlob, content);
    }
  }

  @TransactionalStoreBlob
  protected Content doPutMetadata(final String path,
                                  final TempBlob packagesContent,
                                  final Payload payload) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    OrientRFacet rFacet = facet(OrientRFacet.class);

    Asset asset = rFacet.findOrCreateAsset(tx, path);
    return saveAsset(tx, asset, packagesContent, payload);
  }

  @TransactionalTouchBlob
  protected Content getAsset(final String name) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), name);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1);
  }

  @Override
  protected CacheController getCacheController(final Context context) {
    final AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    return checkNotNull(cacheControllerHolder.get(assetKind.getCacheType()));
  }
}
