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
package org.sonatype.repository.helm.internal.orient.proxy;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.orient.HelmFacet;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.metadata.IndexYamlAbsoluteUrlRewriter;
import org.sonatype.repository.helm.internal.util.HelmAttributeParser;
import org.sonatype.repository.helm.internal.util.HelmPathUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.helm.internal.HelmFormat.HASH_ALGORITHMS;

/**
 * Helm {@link ProxyFacet} implementation.
 *
 * @since 3.next
 */
@Named
public class HelmProxyFacetImpl
    extends ProxyFacetSupport
{
  private final HelmPathUtils helmPathUtils;

  private final HelmAttributeParser helmAttributeParser;

  private final IndexYamlAbsoluteUrlRewriter indexYamlAbsoluteUrlRewriter;

  private HelmFacet helmFacet;

  private static final String INDEX_YAML = "index.yaml";

  @Inject
  public HelmProxyFacetImpl(final HelmPathUtils helmPathUtils,
                            final HelmAttributeParser helmAttributeParser,
                            final IndexYamlAbsoluteUrlRewriter indexYamlAbsoluteUrlRewriter)
  {
    this.helmPathUtils = checkNotNull(helmPathUtils);
    this.helmAttributeParser = checkNotNull(helmAttributeParser);
    this.indexYamlAbsoluteUrlRewriter = checkNotNull(indexYamlAbsoluteUrlRewriter);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    helmFacet = facet(HelmFacet.class);
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
      case HELM_INDEX:
        return getAsset(INDEX_YAML);
      case HELM_PACKAGE:
        TokenMatcher.State matcherState = helmPathUtils.matcherState(context);
        return getAsset(helmPathUtils.filename(matcherState));
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch(assetKind) {
      case HELM_INDEX:
        return putMetadata(INDEX_YAML, content, assetKind);
      case HELM_PACKAGE:
        TokenMatcher.State matcherState = helmPathUtils.matcherState(context);
        return putComponent(content, helmPathUtils.filename(matcherState), assetKind);
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  private Content putMetadata(final String path, final Content content, final AssetKind assetKind) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      try (TempBlob newTempBlob = indexYamlAbsoluteUrlRewriter.removeUrlsFromIndexYamlAndWriteToTempBlob(tempBlob, getRepository()) ) {
        return saveMetadataAsAsset(path, newTempBlob, content, assetKind);
      }
    }
  }

  @TransactionalStoreBlob
  protected Content saveMetadataAsAsset(final String assetPath,
                                        final TempBlob metadataContent,
                                        final Payload payload,
                                        final AssetKind assetKind)
  {
    StorageTx tx = UnitOfWork.currentTx();
    HelmAttributes chart = new HelmAttributes(Collections.emptyMap());
    Asset asset = helmFacet.findOrCreateAsset(tx, assetPath, assetKind, chart);
    return helmFacet.saveAsset(tx, asset, metadataContent, payload);
  }

  private Content putComponent(final Content content,
                               final String fileName,
                               final AssetKind assetKind) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      HelmAttributes helmAttributes = helmAttributeParser.getAttributes(assetKind, tempBlob.get());
      return doCreateOrSaveComponent(helmAttributes, fileName, assetKind, tempBlob, content.getContentType(), content.getAttributes());
    }
  }

  @TransactionalStoreBlob
  protected Content doCreateOrSaveComponent(
      final HelmAttributes helmAttributes,
      final String fileName,
      final AssetKind assetKind,
      final TempBlob componentContent,
      final String contentType,
      final AttributesMap contentAttributes) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = helmFacet.findOrCreateAsset(tx, fileName, assetKind, helmAttributes);
    return helmFacet.saveAsset(tx, asset, componentContent, contentType, contentAttributes);
  }

  @TransactionalTouchBlob
  protected Content getAsset(final String name) {
    StorageTx tx = UnitOfWork.currentTx();

    Optional<Asset> assetOpt = helmFacet.findAsset(tx, name);
    if (!assetOpt.isPresent()) {
      return null;
    }

    Asset asset = assetOpt.get();
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return helmFacet.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
  {
    setCacheInfo(content, cacheInfo);
  }

  @TransactionalTouchMetadata
  public void setCacheInfo(final Content content, final CacheInfo cacheInfo) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent Helm asset {}", content.getAttributes().require(Asset.class)
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
