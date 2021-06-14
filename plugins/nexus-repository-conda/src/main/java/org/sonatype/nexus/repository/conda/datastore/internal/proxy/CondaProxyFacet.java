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
package org.sonatype.nexus.repository.conda.datastore.internal.proxy;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.conda.AssetKind;
import org.sonatype.nexus.repository.conda.datastore.internal.CondaContentFacet;
import org.sonatype.nexus.repository.conda.util.CondaPathUtils;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import static org.sonatype.nexus.repository.conda.util.CondaPathUtils.*;

/**
 * The implementation of a logic specific to the proxy repository.
 *
 * @since 3.next
 */
@Named
@Exposed
public class CondaProxyFacet
    extends ContentProxyFacetSupport
{
  @Nullable
  @Override
  protected Content getCachedContent(final Context context) {
    AssetKind assetKind = getAssetKind(context);
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
      case ARCH_CURRENT_REPODATA_JSON:
        assetPath = buildArchAssetPath(matcherState, CURRENT_REPODATA_JSON);
        break;
      case ARCH_CURRENT_REPODATA_JSON_BZ2:
        assetPath = buildArchAssetPath(matcherState, CURRENT_REPODATA_JSON_BZ2);
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
    return facet(CondaContentFacet.class).getAsset(assetPath)
        .map(FluentAsset::download)
        .orElse(null);
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = getAssetKind(context);
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
      case ARCH_CURRENT_REPODATA_JSON:
        assetPath = buildArchAssetPath(matcherState, CURRENT_REPODATA_JSON);
        break;
      case ARCH_CURRENT_REPODATA_JSON_BZ2:
        assetPath = buildArchAssetPath(matcherState, CURRENT_REPODATA_JSON_BZ2);
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

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1);
  }

  private Content putMetadata(final Content content, final AssetKind assetKind, final String assetPath) {
    return facet(CondaContentFacet.class).findOrCreateAsset(content, assetKind, assetPath)
        .markAsCached(content)
        .download();
  }

  private Content putCondaPackage(final Content content,
                                  final AssetKind assetKind,
                                  final TokenMatcher.State matcherState)
  {
    String assetPath = buildCondaPackagePath(matcherState);
    String name = CondaPathUtils.name(matcherState);
    String namespace = CondaPathUtils.arch(matcherState);
    String version = CondaPathUtils.version(matcherState);
    return facet(CondaContentFacet.class)
        .findOrCreateAssetWithComponent(content, assetPath, name, namespace, version, assetKind)
        .markAsCached(content)
        .download();
  }

  private AssetKind getAssetKind(final Context context) {
    return context.getAttributes().require(AssetKind.class);
  }
}
