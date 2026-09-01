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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.manager.internal.GroupMemberMappingCache;
import org.sonatype.nexus.repository.routing.RoutingRuleInvalidatedEvent;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Invalidates group repository caches when a routing rule change affects any of their members.
 *
 * <p>
 * Fixes NEXUS-33892. Previously, adding, removing, or editing a routing rule on a member of a
 * group repository did not invalidate the group's cache, so the group continued to serve
 * pre-change content until an operator manually invoked the "Invalidate cache" action.
 *
 * <p>
 * This listener reacts to two events:
 * <ul>
 * <li>{@link RoutingRuleInvalidatedEvent} - fired when a rule's matchers/mode change or the
 * rule is deleted. Every repository currently assigned that rule is treated as affected.</li>
 * <li>{@link RepositoryUpdatedEvent} - fired when a repository's configuration changes.
 * Only handled when the {@code routingRuleId} assignment actually changed, avoiding
 * spurious cache invalidations on unrelated config edits.</li>
 * </ul>
 *
 * <p>
 * For each affected member repository, all enclosing groups (including nested/transitive
 * parents, as resolved by {@link GroupMemberMappingCache#getGroups(String)}) have their group
 * caches invalidated via {@link GroupFacet#invalidateGroupCaches()}.
 */
@Component
public class RoutingRuleGroupCacheInvalidator
    implements EventAware, Asynchronous
{
  private static final Logger log = LoggerFactory.getLogger(RoutingRuleGroupCacheInvalidator.class);

  private final RepositoryManager repositoryManager;

  private final GroupMemberMappingCache groupMemberMappingCache;

  @Autowired
  public RoutingRuleGroupCacheInvalidator(
      final RepositoryManager repositoryManager,
      final GroupMemberMappingCache groupMemberMappingCache)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.groupMemberMappingCache = checkNotNull(groupMemberMappingCache);
  }

  /**
   * Invalidate enclosing group caches when a routing rule's content changes or the rule is deleted.
   */
  @Subscribe
  public void on(final RoutingRuleInvalidatedEvent event) {
    String routingRuleId = event.getRoutingRuleId();
    if (routingRuleId == null) {
      return;
    }
    log.debug("Routing rule {} invalidated; invalidating group caches for affected members",
        routingRuleId);

    // visitedGroups is local to this invocation. Concurrent calls each carry their own set,
    // so two simultaneous events may both invalidate the same group. That is intentional —
    // cache invalidation is idempotent and the overlap is harmless.
    Set<String> visitedGroups = new HashSet<>();
    for (Repository repository : repositoryManager.browse()) {
      Configuration configuration = repository.getConfiguration();
      if (configuration == null) {
        continue;
      }
      EntityId assigned = configuration.getRoutingRuleId();
      if (assigned != null && routingRuleId.equals(assigned.getValue())) {
        invalidateEnclosingGroups(repository.getName(), visitedGroups);
      }
    }
  }

  /**
   * Invalidate enclosing group caches when a repository's routing-rule assignment changes.
   * Skipped when the routing rule assignment is unchanged, so unrelated repository edits do not
   * churn group caches.
   */
  @Subscribe
  public void on(final RepositoryUpdatedEvent event) {
    Configuration oldConfig = event.getOldConfiguration();
    Configuration newConfig = event.getRepository().getConfiguration();
    if (oldConfig == null || newConfig == null) {
      return;
    }
    if (Objects.equals(idValue(oldConfig.getRoutingRuleId()), idValue(newConfig.getRoutingRuleId()))) {
      return;
    }
    log.debug("Routing rule assignment changed on {}; invalidating enclosing group caches",
        event.getRepository().getName());
    invalidateEnclosingGroups(event.getRepository().getName(), new HashSet<>());
  }

  private void invalidateEnclosingGroups(final String memberName, final Set<String> visitedGroups) {
    for (String groupName : groupMemberMappingCache.getGroups(memberName)) {
      if (!visitedGroups.add(groupName)) {
        continue;
      }
      Repository group = repositoryManager.get(groupName);
      if (group == null) {
        continue;
      }
      Optional<GroupFacet> facet = group.optionalFacet(GroupFacet.class);
      if (facet.isEmpty()) {
        continue;
      }
      try {
        facet.get().invalidateGroupCaches();
      }
      catch (RuntimeException e) {
        log.warn("Failed to invalidate group cache for {}", groupName, e);
      }
    }
  }

  private static String idValue(final EntityId id) {
    return id == null ? null : id.getValue();
  }
}
