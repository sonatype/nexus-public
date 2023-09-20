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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.distributed.event.service.api.common.RepositoryCacheInvalidationEvent;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.ProxyType;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service class for consolidating repeated cache-related logic not exclusive to individual facets and components.
 *
 * @since 3.41
 */
@Named
@Singleton
public class RepositoryCacheInvalidationService
    implements EventAware
{
  private final RepositoryManager repositoryManager;

  private final EventManager eventManager;

  @Inject
  public RepositoryCacheInvalidationService(
      final RepositoryManager repositoryManager,
      final EventManager eventManager)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.eventManager = checkNotNull(eventManager);
  }

  public void processCachesInvalidation(final Repository repository) {
    invalidateCaches(repository, true);
  }

  /**
   * Invalidates the group or proxy caches of the specified repository based on type and fires an DES event if cache
   * invalidation was triggered by current node.
   * This is a no-op for hosted repositories.
   */
  private void invalidateCaches(final Repository repository, final boolean isTriggeredByCurrentNode) {
    checkNotNull(repository);
    if (GroupType.NAME.equals(repository.getType().getValue())) {
      invalidateGroupCaches(repository);
    }
    else if (ProxyType.NAME.equals(repository.getType().getValue())) {
      invalidateProxyAndNegativeCaches(repository);
    }
    if (isTriggeredByCurrentNode) {
      postEvent(repository);
    }
  }

  /**
   * Invalidates the group caches for given repository.
   */
  private static void invalidateGroupCaches(final Repository repository) {
    checkNotNull(repository);
    checkArgument(GroupType.NAME.equals(repository.getType().getValue()));
    GroupFacet groupFacet = repository.facet(GroupFacet.class);
    groupFacet.invalidateGroupCaches();
  }

  /**
   * Invalidates the proxy and negative caches for given repository.
   */
  private static void invalidateProxyAndNegativeCaches(final Repository repository) {
    checkNotNull(repository);
    checkArgument(ProxyType.NAME.equals(repository.getType().getValue()));
    ProxyFacet proxyFacet = repository.facet(ProxyFacet.class);
    proxyFacet.invalidateProxyCaches();
    NegativeCacheFacet negativeCacheFacet = repository.facet(NegativeCacheFacet.class);
    negativeCacheFacet.invalidate();
  }

  private void postEvent(final Repository repository) {
    if (!EventHelper.isReplicating()) {
      eventManager.post(new RepositoryCacheInvalidationEvent(repository.getName()));
    }
  }

  @Subscribe
  public void on(final RepositoryCacheInvalidationEvent event) {
    invalidateCaches(repositoryManager.get(event.getRepositoryName()), false);
  }
}
