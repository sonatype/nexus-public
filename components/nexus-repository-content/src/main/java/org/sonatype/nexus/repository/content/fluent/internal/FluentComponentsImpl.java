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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ComponentSet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentSetData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.types.GroupType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.fluent.internal.RepositoryContentUtil.getLeafRepositoryIds;
import static org.sonatype.nexus.repository.content.fluent.internal.RepositoryContentUtil.isGroupRepository;
import static org.sonatype.nexus.repository.content.store.InternalIds.contentRepositoryId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toInternalId;

/**
 * {@link FluentComponents} implementation.
 *
 * @since 3.24
 */
public class FluentComponentsImpl
    implements FluentComponents
{
  private final ContentFacetSupport facet;

  private final ComponentStore<?> componentStore;

  public FluentComponentsImpl(final ContentFacetSupport facet, final ComponentStore<?> componentStore) {
    this.facet = checkNotNull(facet);
    this.componentStore = checkNotNull(componentStore);
  }

  @Override
  public FluentComponentBuilder name(final String name) {
    return new FluentComponentBuilderImpl(facet, componentStore, name);
  }

  @Override
  public FluentComponent with(final Component component) {
    return component instanceof FluentComponent ? (FluentComponent) component
        : new FluentComponentImpl(facet, component);
  }

  @Override
  public FluentComponent with(final Component component, @Nullable final Collection<Asset> assets) {
    if (component instanceof FluentComponent) {
      return (FluentComponent) component;
    }

    if (assets == null) {
      return new FluentComponentImpl(facet, component, null);
    }

    List<FluentAsset> fluentAssets = assets
        .stream()
        .map(it -> facet.assets().with(it))
        .collect(Collectors.toList());

    return new FluentComponentImpl(facet, component, fluentAssets);
  }

  @Override
  public int count() {
    return doCount(null, null, null);
  }

  int doCount(
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return componentStore.countComponents(facet.contentRepositoryId(), kind, filter, filterParams);
  }

  @Override
  public Continuation<FluentComponent> browse(final int limit, final String continuationToken) {
    return doBrowse(limit, continuationToken, null, null, null);
  }

  @Override
  public Continuation<FluentComponent> browseEager(final int limit, @Nullable final String continuationToken) {
    return doBrowseEager(limit, continuationToken, null, null, null);
  }

  Continuation<FluentComponent> doBrowseEager(final int limit,
                                              @Nullable final String continuationToken,
                                              @Nullable final String kind,
                                              @Nullable final String filter,
                                              @Nullable final Map<String, Object> filterParams) {
    Set<Integer> repositoryIds = Collections.singleton(facet.contentRepositoryId());
    if (isGroupRepository(facet.repository())) {
      repositoryIds = getLeafRepositoryIds(facet.repository());
    }

    Continuation<ComponentData> componentAssetsData = componentStore
        .browseComponentsEager(repositoryIds, limit, continuationToken, kind, filter, filterParams);

    return new FluentContinuation<>(
        componentAssetsData,
        componentData -> {
          assert componentData != null;

          List<Asset> assets = componentData.getAssets();

          return facet.components().with(componentData, assets);
        });
  }

  Continuation<FluentComponent> doBrowse(final int limit,
                                         @Nullable final String continuationToken,
                                         @Nullable final String kind,
                                         @Nullable final String filter,
                                         @Nullable final Map<String, Object> filterParams)
  {
    if (isGroupRepository(facet.repository())) {
      Set<Integer> leafRepositoryIds = getLeafRepositoryIds(facet.repository());
      if (!leafRepositoryIds.isEmpty()) {
        return new FluentContinuation<>(componentStore.browseComponents(
            leafRepositoryIds, limit, continuationToken), this::with);
      }
    }
    return new FluentContinuation<>(componentStore.browseComponents(facet.contentRepositoryId(),
        limit, continuationToken, kind, filter, filterParams), this::with);
  }

  @Override
  public FluentQuery<FluentComponent> byKind(final String kind) {
    return new FluentComponentQueryImpl(this, kind);
  }

  @Override
  public FluentQuery<FluentComponent> byFilter(final String filter, final Map<String, Object> filterParams) {
    return new FluentComponentQueryImpl(this, filter, filterParams);
  }

  @Override
  public Continuation<FluentComponent> bySet(
      final ComponentSet componentSet,
      final int limit,
      final String continuationToken)
  {
    return new FluentContinuation<>(componentStore.browseComponentsBySet(facet.contentRepositoryId(),
        componentSet, limit, continuationToken), this::with);
  }

  @Override
  public Continuation<FluentComponent> byCleanupCriteria(
      final ComponentSet componentSet,
      final Map<String, String> criteria,
      final int limit,
      final String continuationToken)
  {
    return new FluentContinuation<>(componentStore.browseComponentsForCleanup(facet.contentRepositoryId(),
        componentSet, criteria, limit, continuationToken), this::with);
  }

  @Override
  public Set<String> getProcessedCleanupCriteria() {
    return componentStore.getProcessedCleanupCriteria();
  }

  @Override
  public Collection<String> namespaces() {
    return componentStore.browseNamespaces(facet.contentRepositoryId());
  }

  @Override
  public Collection<String> names(final String namespace) {
    return componentStore.browseNames(facet.contentRepositoryId(), namespace);
  }

  @Override
  public Continuation<ComponentSetData> sets(final int limit, final String continuationToken) {
    return componentStore.browseSets(facet.contentRepositoryId(), limit, continuationToken);
  }

  @Override
  public Collection<String> versions(final String namespace, final String name) {
    return componentStore.browseVersions(facet.contentRepositoryId(), namespace, name);
  }

  @Override
  public Optional<FluentComponent> find(final EntityId externalId) {
    return componentStore.readComponent(toInternalId(externalId))
        .filter(this::containedInRepository)
        .map(component -> new FluentComponentImpl(facet, component));
  }

  /**
   * Returns {@code true} if this component is contained in this repository or any of its members.
   */
  private boolean containedInRepository(final Component component) {
    int expectedContentRepositoryId = contentRepositoryId(component);
    if (expectedContentRepositoryId == facet.contentRepositoryId()) {
      return true;
    }
    else if (facet.repository().getType() instanceof GroupType) {
      return facet.repository().facet(GroupFacet.class).allMembers().stream()
          .map(InternalIds::contentRepositoryId)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .anyMatch(id -> id == expectedContentRepositoryId);
    }
    return false;
  }
}
