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
package org.sonatype.nexus.repository.cocoapods.internal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.cocoapods.CocoapodsFacet;
import org.sonatype.nexus.repository.cocoapods.internal.AssetKind;
import org.sonatype.nexus.repository.cocoapods.internal.pod.PodPathParser;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.cache.CacheControllerHolder.CONTENT;
import static org.sonatype.nexus.repository.cocoapods.internal.AssetKind.SPEC;
import static org.sonatype.nexus.repository.cocoapods.internal.CocoapodsFormat.CDN_METADATA_PREFIX;
import static org.sonatype.nexus.repository.cocoapods.internal.CocoapodsFormat.removeInitialSlashFromPath;
import static org.sonatype.nexus.repository.cocoapods.internal.pod.PodsUtils.extractPodPath;

/**
 * @since 3.19
 */
@Named
@Facet.Exposed
public class CocoapodsProxyFacet
    extends ProxyFacetSupport
{
  private static String ASSET_KIND_ERROR = "Received an invalid AssetKind of type: ";

  private PodPathParser podPathParser;

  private SpecTransformer specTransformer;

  @Inject
  public CocoapodsProxyFacet(final SpecTransformer specTransformer, final PodPathParser podPathParser) {
    this.podPathParser = podPathParser;
    this.specTransformer = specTransformer;
  }

  private Content transformSpecFile(final Payload payload) throws IOException {
    try (InputStream data = payload.openInputStream()) {
      String specFile = IOUtils.toString(data, Charsets.UTF_8);
      try {
        return new Content(
            new StringPayload(specTransformer.toProxiedSpec(specFile, URI.create(getRepository().getUrl() + "/")),
                "application/json"));
      }
      catch (InvalidSpecFileException e) {
        log.info("Invalid Spec file", e);
        return null;
      }
    }
  }

  @Override
  public Content get(final Context context) throws IOException {
    Content ret = super.get(context);
    if (ret == null) {
      return null;
    }
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    if (assetKind == SPEC) {
      ret = transformSpecFile(ret);
    }
    return ret;
  }

  @Override
  protected Content getCachedContent(final Context context) {
    final String path = removeInitialSlashFromPath(context.getRequest().getPath());
    Content ret;
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case CDN_METADATA:
        if (!path.startsWith(CDN_METADATA_PREFIX)) {
          ret = facet(CocoapodsFacet.class).get(CDN_METADATA_PREFIX + path);
          break;
        }
      case SPEC:
      case POD:
        ret = facet(CocoapodsFacet.class).get(extractPodPath(context));
        break;
      default:
        throw new IllegalStateException(ASSET_KIND_ERROR + assetKind.name());
    }
    return ret;
  }

  private boolean isComponentSupported(AssetKind assetKind) {
    return assetKind.getCacheType() == CONTENT;
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    final String path = removeInitialSlashFromPath(context.getRequest().getPath());
    Content ret;
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    boolean toAttachComponent = isComponentSupported(assetKind);
    switch (assetKind) {
      case CDN_METADATA:
        ret = facet(CocoapodsFacet.class).getOrCreateAsset(CDN_METADATA_PREFIX + path, content, toAttachComponent);
        cacheControllerHolder.getMetadataCacheController().invalidateCache();
        break;
      case SPEC:
        ret = facet(CocoapodsFacet.class).getOrCreateAsset(path, content, toAttachComponent);
        cacheControllerHolder.getMetadataCacheController().invalidateCache();
        break;
      case POD:
        ret = facet(CocoapodsFacet.class).getOrCreateAsset(extractPodPath(context), content, toAttachComponent);
        break;
      default:
        throw new IllegalStateException(ASSET_KIND_ERROR + assetKind.name());
    }
    return ret;
  }

  @Override
  protected String getUrl(final Context context) {
    String path = removeInitialSlashFromPath(context.getRequest().getPath());
    String ret;
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case CDN_METADATA:
      case SPEC:
        ret = path;
        break;
      case POD:
        ret = podPathParser.parse(extractPodPath(context)).getUri().toString();
        break;
      default:
        throw new IllegalStateException(ASSET_KIND_ERROR + assetKind.name());
    }
    return ret;
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
          "Attempting to set cache info for non-existent Cocoapods asset {}",
          content.getAttributes().require(Asset.class)
      );
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Override
  @Nonnull
  protected CacheController getCacheController(final Context context) {
    final AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    return checkNotNull(cacheControllerHolder.get(assetKind.getCacheType()));
  }
}
