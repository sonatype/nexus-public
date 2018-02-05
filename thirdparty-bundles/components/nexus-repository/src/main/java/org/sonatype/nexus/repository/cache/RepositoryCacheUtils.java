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
package org.sonatype.nexus.repository.cache;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.ProxyType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for consolidating repeated cache-related logic not exclusive to individual facets and components.
 *
 * @since 3.0
 */
public final class RepositoryCacheUtils
{
  private RepositoryCacheUtils() {
    // empty
  }

  /**
   * Invalidates the group or proxy caches of the specified repository based on type.
   *
   * This is a no-op for hosted repositories.
   */
  public static void invalidateCaches(final Repository repository) {
    checkNotNull(repository);
    if (GroupType.NAME.equals(repository.getType().getValue())) {
      invalidateGroupCaches(repository);
    } else if (ProxyType.NAME.equals(repository.getType().getValue())) {
      invalidateProxyAndNegativeCaches(repository);
    }
  }

  /**
   * Invalidates the group caches for given repository.
   */
  public static void invalidateGroupCaches(final Repository repository) {
    checkNotNull(repository);
    checkArgument(GroupType.NAME.equals(repository.getType().getValue()));
    GroupFacet groupFacet = repository.facet(GroupFacet.class);
    groupFacet.invalidateGroupCaches();
  }

  /**
   * Invalidates the proxy and negative caches for given repository.
   */
  public static void invalidateProxyAndNegativeCaches(final Repository repository) {
    checkNotNull(repository);
    checkArgument(ProxyType.NAME.equals(repository.getType().getValue()));
    ProxyFacet proxyFacet = repository.facet(ProxyFacet.class);
    proxyFacet.invalidateProxyCaches();
    NegativeCacheFacet negativeCacheFacet = repository.facet(NegativeCacheFacet.class);
    negativeCacheFacet.invalidate();
  }
}
