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
package org.sonatype.nexus.repository.r.datastore.internal.proxy;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.r.datastore.RContentFacet;
import org.sonatype.nexus.repository.r.AssetKind;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.extractRequestPath;

/**
 * The implementation of a logic specific to the proxy repository.
 *
 * @since 3.32
 */
@Named
@Exposed
public class RProxyFacet
    extends ContentProxyFacetSupport
{
  @Nullable
  @Override
  protected Content getCachedContent(final Context context) {
    AssetKind assetKind = getAssetKind(context);
    switch (assetKind) {
      case RDS_METADATA:
      case PACKAGES:
      case ARCHIVE:
        return getContent(context);
      default:
        throw new IllegalArgumentException("Unsupported asset kind " + assetKind);
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = getAssetKind(context);
    switch (assetKind) {
      case RDS_METADATA:
      case PACKAGES:
        return storeMetadata(content, assetKind, extractRequestPath(context));
      case ARCHIVE:
        return storeArchive(content, extractRequestPath(context));
      default:
        throw new IllegalArgumentException("Unsupported asset kind " + assetKind);
    }
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1);
  }

  private static AssetKind getAssetKind(final Context context) {
    return context.getAttributes().require(AssetKind.class);
  }

  private Content getContent(final Context context) {
    return facet(RContentFacet.class).getAsset(extractRequestPath(context))
        .map(FluentAsset::download)
        .orElse(null);
  }

  private Content storeMetadata(final Content content, final AssetKind assetKind, final String assetPath) {
    return facet(RContentFacet.class).putMetadata(content, assetPath, assetKind)
        .markAsCached(content)
        .download();
  }

  private Content storeArchive(final Content content, final String assetPath) {
    return facet(RContentFacet.class).putPackage(content, assetPath)
        .markAsCached(content)
        .download();
  }
}
