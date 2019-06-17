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
package org.sonatype.nexus.repository.routing.internal;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An in-memory cache of the RoutingRule assigned to a Repository. Uses events to know when to invalidate the
 * cache.
 *
 * @since 3.17
 */
@Named
@Singleton
public class RoutingRuleCache
    extends ComponentSupport
    implements EventAware
{
  private final LoadingCache<Repository, Optional<EntityId>> repositoryAssignedCache =
      CacheBuilder.newBuilder().build(new RepositoryMappingCacheLoader());

  private final LoadingCache<EntityId, Optional<RoutingRule>> routingRuleCache =
      CacheBuilder.newBuilder().build(new RoutingRuleCacheLoader());

  private final RoutingRuleStore routingRuleStore;

  @Inject
  public RoutingRuleCache(final RoutingRuleStore routingRuleStore) {
    this.routingRuleStore = checkNotNull(routingRuleStore);
  }

  /**
   * Retrieves the routing rule assigned to a repository or null if one is not assigned.
   *
   * @param repository
   * @return
   */
  @Nullable
  public RoutingRule getRoutingRule(final Repository repository) {
    try {
      return repositoryAssignedCache.get(repository).map(this::getRoutingRule).orElse(null);
    }
    catch (ExecutionException e) {
      log.error("An error occurred retrieving the routing rule for repository: {}", repository.getName(), e);
      return null;
    }
  }

  /**
   * Retrieves the id of the routing rule assigned to a repository or null if one is not assigned.
   *
   * @param repository
   * @return
   */
  @Nullable
  public EntityId getRoutingRuleId(final Repository repository) {
    try {
      return repositoryAssignedCache.get(repository).orElse(null);
    }
    catch (ExecutionException e) {
      log.error("An error occurred retrieving the routing rule for repository: {}", repository.getName(), e);
      return null;
    }
  }

  private RoutingRule getRoutingRule(final EntityId id) {
    try {
      return routingRuleCache.get(id).orElse(null);
    }
    catch (ExecutionException e) {
      log.error("An error occurred retrieving the routing rule id {}", id, e);
      return null;
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryDeletedEvent event) {
    repositoryAssignedCache.invalidate(event.getRepository());
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryUpdatedEvent event) {
    repositoryAssignedCache.invalidate(event.getRepository());
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RoutingRuleDeletedEvent event) {
    routingRuleCache.invalidate(((EntityEvent) event).getId());
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RoutingRuleUpdatedEvent event) {
    routingRuleCache.invalidate(((EntityEvent) event).getId());
  }

  private static class RepositoryMappingCacheLoader
      extends CacheLoader<Repository, Optional<EntityId>>
  {
    @Override
    public Optional<EntityId> load(final Repository repository) throws Exception {
      return Optional.ofNullable(repository.getConfiguration().getRoutingRuleId());
    }
  }

  private class RoutingRuleCacheLoader
      extends CacheLoader<EntityId, Optional<RoutingRule>>
  {
    @Override
    public Optional<RoutingRule> load(final EntityId key) throws Exception {
      return Optional.ofNullable(routingRuleStore.getById(key.getValue()));
    }
  }
}
