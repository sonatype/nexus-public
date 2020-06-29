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

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link FluentAssets} implementation.
 *
 * @since 3.24
 */
public class FluentAssetsImpl
    implements FluentAssets
{
  private final ContentFacetSupport facet;

  public FluentAssetsImpl(final ContentFacetSupport facet) {
    this.facet = checkNotNull(facet);
  }

  @Override
  public FluentAssetBuilder path(final String path) {
    return new FluentAssetBuilderImpl(facet, path);
  }

  @Override
  public FluentAsset with(final Asset asset) {
    return asset instanceof FluentAsset ? (FluentAsset) asset
        : new FluentAssetImpl(facet, asset);
  }

  @Override
  public int count() {
    return facet.stores().assetStore.countAssets(facet.contentRepositoryId());
  }

  @Override
  public Continuation<FluentAsset> browse(
      @Nullable final String kind,
      final int limit,
      final String continuationToken)
  {
    return new FluentContinuation<>(
        facet.stores().assetStore.browseAssets(facet.contentRepositoryId(), kind, limit, continuationToken),
        this::with);
  }
}
