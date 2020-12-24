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
package org.sonatype.nexus.repository.npm.internal;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType;

import static com.google.common.base.Preconditions.checkNotNull;

@Facet.Exposed
public interface NpmProxyFacet
    extends Facet
{

  void invalidateProxyCaches();

  enum ProxyTarget
  {
    SEARCH_INDEX(CacheControllerHolder.METADATA),
    SEARCH_V1_RESULTS(CacheControllerHolder.METADATA),
    PACKAGE(CacheControllerHolder.METADATA),
    DIST_TAGS(CacheControllerHolder.METADATA),
    TARBALL(CacheControllerHolder.CONTENT);

    private final CacheType cacheType;

    ProxyTarget(final CacheType cacheType) {
      this.cacheType = checkNotNull(cacheType);
    }

    @Nonnull
    public CacheType getCacheType() {
      return cacheType;
    }
  }
}
