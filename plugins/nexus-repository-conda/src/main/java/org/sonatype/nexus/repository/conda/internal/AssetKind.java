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
package org.sonatype.nexus.repository.conda.internal;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType;

import static org.sonatype.nexus.repository.cache.CacheControllerHolder.CONTENT;
import static org.sonatype.nexus.repository.cache.CacheControllerHolder.METADATA;

/**
 * @since 3.19
 */
public enum AssetKind
{
  CHANNEL_INDEX_HTML(METADATA),
  CHANNEL_DATA_JSON(METADATA),
  CHANNEL_RSS_XML(METADATA),
  ARCH_INDEX_HTML(METADATA),
  ARCH_REPODATA_JSON(METADATA),
  ARCH_REPODATA_JSON_BZ2(METADATA),
  ARCH_REPODATA2_JSON(METADATA),
  ARCH_TAR_PACKAGE(CONTENT),
  ARCH_CONDA_PACKAGE(CONTENT);

  private final CacheType cacheType;

  AssetKind(final CacheType cacheType) {
    this.cacheType = cacheType;
  }

  @Nonnull
  public CacheType getCacheType() {
    return cacheType;
  }
}
