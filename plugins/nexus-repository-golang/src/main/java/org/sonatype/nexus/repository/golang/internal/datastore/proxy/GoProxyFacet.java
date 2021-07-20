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
package org.sonatype.nexus.repository.golang.internal.datastore.proxy;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.golang.AssetKind;
import org.sonatype.nexus.repository.golang.internal.datastore.GoContentFacet;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.golang.internal.util.GolangPathUtils;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

/**
 * @since 3.next
 */
@Named
public class GoProxyFacet
    extends ProxyFacetSupport
{
  @Nullable
  @Override
  protected Content getCachedContent(final Context context) {
    String assetPath = getAssetPath(context);

    return facet(GoContentFacet.class)
        .getAsset(assetPath)
        .map(FluentAsset::download)
        .orElse(null);
  }

  @Override
  protected Content store(final Context context, final Content content) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState = GolangPathUtils.matcherState(context);
    String assetPath = getAssetPath(context);

    switch (assetKind) {
      case INFO:
      case MODULE:
      case PACKAGE:
        GolangAttributes golangAttributes = GolangPathUtils.getAttributesFromMatcherState(matcherState);
        return facet(GoContentFacet.class)
            .saveComponentAndAsset(assetPath, content, assetKind, golangAttributes)
            .markAsCached(content)
            .download();
      case LIST:
      case LATEST:
        return facet(GoContentFacet.class)
            .saveAsset(assetPath, content, assetKind)
            .markAsCached(content)
            .download();
      default:
        throw new IllegalStateException(String.format(
            "Could not store content. Received an unsupported AssetKind of type: %s on the repository %s",
            assetKind.name(), getRepository().getName()
        ));
    }
  }

  @Override
  protected void indicateVerified(
      final Context context, final Content content, final CacheInfo cacheInfo)
  {
    String assetPath = getAssetPath(context);
    Optional<FluentAsset> fluentAssetOptional = facet(GoContentFacet.class)
        .getAsset(assetPath);

    if (!fluentAssetOptional.isPresent()) {
      log.debug("Attempting to set cache info for non-existent go asset: {} on the repository: {}",
          assetPath, getRepository().getName());
      return;
    }

    FluentAsset fluentAsset = fluentAssetOptional.get();

    log.debug("Updating cacheInfo of {} to {} on the repository {}",
        fluentAsset.path(), cacheInfo, getRepository().getName()
    );
    fluentAsset.markAsCached(cacheInfo);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1);
  }

  private String getAssetPath(final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState = GolangPathUtils.matcherState(context);
    switch (assetKind) {
      case INFO:
      case MODULE:
      case PACKAGE:
        return GolangPathUtils.assetPath(matcherState);
      case LIST:
        return GolangPathUtils.listPath(matcherState);
      case LATEST:
        return GolangPathUtils.latestPath(matcherState);
      default:
        throw new IllegalStateException(String.format(
            "Could not get asset path. Received an unsupported AssetKind of type: %s on the repository %s",
            assetKind.name(), getRepository().getName()
        ));
    }
  }
}
