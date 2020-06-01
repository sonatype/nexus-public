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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.util.Optional;

import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link FluentAssetBuilder} implementation.
 *
 * @since 3.24
 */
public class FluentAssetBuilderImpl
    implements FluentAssetBuilder
{
  private final ContentFacetSupport facet;

  private final String path;

  private String kind = "";

  private Component component;

  public FluentAssetBuilderImpl(final ContentFacetSupport facet, final String path) {
    this.facet = checkNotNull(facet);
    this.path = checkNotNull(path);
  }

  @Override
  public FluentAssetBuilder kind(final String kind) {
    this.kind = checkNotNull(kind);
    return this;
  }

  @Override
  public FluentAssetBuilder component(final Component component) {
    this.component = checkNotNull(component);
    return this;
  }

  @Override
  public FluentAsset getOrCreate() {
    return new FluentAssetImpl(facet,
        facet.stores().assetStore.readAsset(facet.contentRepositoryId(), path)
        .orElseGet(this::createAsset));
  }

  @Override
  public Optional<FluentAsset> find() {
    return facet.stores().assetStore
        .readAsset(facet.contentRepositoryId(), path)
        .map(asset -> new FluentAssetImpl(facet, asset));
  }

  private Asset createAsset() {
    AssetData asset = new AssetData();
    asset.setRepositoryId(facet.contentRepositoryId());
    asset.setPath(path);
    asset.setKind(kind);
    asset.setComponent(component);

    if (ProxyFacetSupport.isDownloading()) {
      asset.setLastDownloaded(UTC.now());
    }

    facet.stores().assetStore.createAsset(asset);

    return asset;
  }
}
