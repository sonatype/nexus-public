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
package org.sonatype.repository.helm.internal;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType;

/**
 * Asset kinds for Helm
 *
 * Examples of all can be found at: https://github.com/kubernetes/helm/blob/master/docs/chart_repository.md#the-chart-repository-structure
 * @since 3.28
 */
public enum AssetKind
{
  HELM_INDEX(CacheControllerHolder.METADATA, ".yaml"),
  HELM_PROVENANCE(CacheControllerHolder.CONTENT, ".tgz.prov"),
  HELM_PACKAGE(CacheControllerHolder.CONTENT, ".tgz");

  private final CacheType cacheType;
  private final String extension;

  AssetKind(final CacheType cacheType, final String extension) {
    this.cacheType = cacheType;
    this.extension = extension;
  }

  public static AssetKind getAssetKindByFileName(final String name) {
    if (name.endsWith(HELM_PACKAGE.getExtension())) {
      return AssetKind.HELM_PACKAGE;
    }
    else if (name.endsWith(HELM_PROVENANCE.getExtension())) {
      return AssetKind.HELM_PROVENANCE;
    }
    return AssetKind.HELM_INDEX;
  }

  @Nonnull
  public CacheType getCacheType() {
    return cacheType;
  }

  @Nonnull
  public String getExtension() {
    return extension;
  }
}
