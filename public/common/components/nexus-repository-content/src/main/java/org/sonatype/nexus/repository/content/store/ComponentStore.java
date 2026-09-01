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
package org.sonatype.nexus.repository.content.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.event.Event;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ComponentSet;
import org.sonatype.nexus.repository.content.SqlGenerator;
import org.sonatype.nexus.repository.content.SqlQueryParameters;
import org.sonatype.nexus.repository.content.event.component.ComponentAttributesEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentKindEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPreDeleteEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPrePurgeEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentsPurgedAuditEvent;
import org.sonatype.nexus.repository.content.event.component.RepositoryDeletedComponentEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Value;

import static java.util.Arrays.stream;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.repository.content.AttributesHelper.applyAttributeChange;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * {@link Component} store.
 *
 * @since 3.21
 */
public class ComponentStore<T extends ComponentDAO>
    extends ContentStoreEventSupport<T>
{
  private static final int BATCH_SIZE = SystemPropertiesHelper.getInteger("nexus.component.purge.size", 100);

  private final boolean clustered;

  private static final int ASSET_BROWSE_LIMIT = 1000;

  /**
   * Caps how many unresolved assets are named in the warning logged by {@link #resolveAssetComponents}.
   */
  private static final int UNRESOLVED_LOG_LIMIT = 10;

  @Autowired
  public ComponentStore(
      final DataSessionSupplier sessionSupplier,
      @Value(DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE) final boolean clustered,
      final String contentStoreName,
      final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
    this.clustered = clustered;
  }

  /**
   * Count all components in the given repository.
   *
   * @param repositoryId the repository to count
   * @param kind optional kind of components to count
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return count of components in the repository
   */
  @Transactional
  public int countComponents(
      final int repositoryId,
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return dao().countComponents(repositoryId, kind, filter, filterParams);
  }

  /**
   * Count all Nuget components with an associated blob_id in the given repository.
   *
   * @param repositoryId the repository to count
   * @param kind optional kind of components to count
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return count of components in the repository
   */
  @Transactional
  public int countComponentsWithAssetsBlobs(
      final int repositoryId,
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return dao().countComponentsWithAssetsBlobs(repositoryId, kind, filter, filterParams);
  }

  /**
   * Count all components without normalized_version
   *
   * @return count of components in the table
   */
  @Transactional
  public int countUnnormalized() {
    return dao().countUnnormalized();
  }

  /**
   * Browse all components in the given repository in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @param kind optional kind of components to return
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return collection of components and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Component> browseComponents(
      final int repositoryId,
      final int limit,
      @Nullable final String continuationToken,
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return dao().browseComponents(repositoryId, limit, continuationToken, kind, filter,
        filterParams);
  }

  @Transactional
  public Continuation<ComponentData> browseComponentsEager(
      final Set<Integer> repositoryIds,
      final int limit,
      @Nullable final String continuationToken,
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return dao().browseComponentsEager(repositoryIds, limit, continuationToken, kind, filter, filterParams);
  }

  /**
   * Browse all components without normalized_version
   *
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<ComponentData> browseUnnormalized(
      final int limit,
      @Nullable final String continuationToken)
  {
    return dao().browseUnnormalized(limit, continuationToken);
  }

  /**
   * Browse all components in the given repository ids in a paged fashion.
   *
   * @param repositoryIds the ids repositories to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Component> browseComponents(
      final Set<Integer> repositoryIds,
      final int limit,
      @Nullable final String continuationToken)
  {
    return dao().browseComponentsInRepositories(repositoryIds, limit, continuationToken);
  }

  /**
   * Browse all components in the given repository and component set, in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param componentSet the component set to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Component> browseComponentsBySet(
      final int repositoryId,
      final ComponentSet componentSet,
      final int limit,
      @Nullable final String continuationToken)
  {
    return dao().browseComponentsBySet(repositoryId,
        componentSet.namespace(), componentSet.name(), limit, continuationToken);
  }

  /**
   * Select components using the provided query generator and parameters.
   *
   * @param generator generator for the select
   * @param params parameters for the select
   */
  @Transactional
  public Continuation<Component> selectComponents(
      final SqlGenerator<? extends SqlQueryParameters> generator,
      final SqlQueryParameters params)
  {
    return dao().selectComponents(generator, params);
  }

  /**
   * Select components with its related assets using the provided query generator and parameters.
   *
   * @param generator generator for the select
   * @param params parameters for the select
   */
  @Transactional
  public Continuation<Component> selectComponentsWithAssets(
      final SqlGenerator<? extends SqlQueryParameters> generator,
      final SqlQueryParameters params)
  {
    return dao().selectComponentsWithAssets(generator, params);
  }

  /**
   * Browse all component namespaces in the given repository.
   * <p>
   * The result will include the empty string if there are any components that don't have a namespace.
   *
   * @param repositoryId the repository to browse
   * @return collection of component namespaces
   */
  @Transactional
  public Collection<String> browseNamespaces(final int repositoryId) {
    return dao().browseNamespaces(repositoryId);
  }

  /**
   * Browse the names of all components under a namespace in the given repository.
   *
   * @param repositoryId the repository to browse
   * @param namespace the namespace to browse (empty string to browse components that don't have a namespace)
   * @return collection of component names
   */
  @Transactional
  public Collection<String> browseNames(final int repositoryId, final String namespace) {
    return dao().browseNames(repositoryId, namespace);
  }

  /**
   * Browse all component sets (distinct namespace & name) in the given repository.
   *
   * @param repositoryId the repository to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of componentSets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<ComponentSetData> browseSets(
      @Param("repositoryId") final int repositoryId,
      @Param("limit") final int limit,
      @Nullable @Param("continuationToken") final String continuationToken)
  {
    return dao().browseSets(repositoryId, limit, continuationToken);
  }

  /**
   * Browse all components that have associated assets in the given repository.
   *
   * @param repositoryId the repository to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components with assets
   */
  @Transactional
  public Continuation<Component> browseComponentsWithAssets(
      final int repositoryId,
      final int limit,
      @Nullable final String continuationToken)
  {
    return dao().browseComponentsWithAssets(repositoryId, limit, continuationToken);
  }

  /**
   * Browse the versions of a component with the given namespace and name in the given repository.
   * <p>
   * The result will include the empty string if there are any components that don't have a version.
   *
   * @param repositoryId the repository to browse
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @return collection of component versions
   */
  @Transactional
  public Collection<String> browseVersions(final int repositoryId, final String namespace, final String name) {
    return dao().browseVersions(repositoryId, namespace, name);
  }

  @Transactional
  public Collection<String> browseVersionsByRepoIds(
      final String namespace,
      final String name,
      final Set<Integer> repositoryIds)
  {
    return dao().browseVersionsByRepoIds(namespace, name, repositoryIds);
  }

  /**
   * Creates the given component in the content data store.
   *
   * @param component the component to create
   */
  @Transactional
  public void createComponent(final ComponentData component) {
    dao().createComponent(component, clustered);

    postCommitEvent(() -> new ComponentCreatedEvent(component));
  }

  /**
   * Retrieves a component from the content data store.
   *
   * @param componentId the internal id of the component
   * @return component if it was found
   */
  @Transactional
  public Optional<Component> readComponent(final int componentId) {
    return dao().readComponent(componentId);
  }

  /**
   * Retrieves a component located at the given coordinate in the content data store.
   *
   * @param repositoryId the repository containing the component
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @param version the version of the component
   * @return component if it was found
   */
  @Transactional
  public Optional<Component> readCoordinate(
      final int repositoryId,
      final String namespace,
      final String name,
      final String version)
  {
    return dao().readCoordinate(repositoryId, namespace, name, version);
  }

  /**
   * Retrieves a component located at the given coordinate in within the given repository ids.
   *
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @param version the version of the component
   * @param repositoryIds the repository ids containing the component
   * @return component if it was found
   */
  @Transactional
  public Optional<Component> readCoordinateInRepoIds(
      final String namespace,
      final String name,
      final String version,
      final Set<Integer> repositoryIds)
  {
    return dao().readCoordinateInRepoIds(namespace, name, version, repositoryIds);
  }

  /**
   * Updates the kind of the given component in the content data store.
   *
   * @param component the component to update
   */
  @Transactional
  public void updateComponentKind(final Component component) {
    dao().updateComponentKind(component, clustered);

    postCommitEvent(() -> new ComponentKindEvent(component));
  }

  /**
   * Updates the normalized_version of the given component in the content data store.
   *
   * @param component the component to update
   */
  @Transactional
  public void updateComponentNormalizedVersion(final Component component) {
    dao().updateComponentNormalizedVersion(component, clustered);
  }

  /**
   * Updates the attributes of the given component in the content data store.
   *
   * @param component the component to update
   */
  @Transactional
  public void updateComponentAttributes(
      final Component component,
      final AttributeOperation change,
      final String key,
      final @Nullable Object value)
  {
    // reload latest attributes, apply change, then update database if necessary
    dao().readComponentAttributes(component).ifPresent(attributes -> {
      ((ComponentData) component).setAttributes(attributes);

      if (applyAttributeChange(attributes, change, key, value)) {
        dao().updateComponentAttributes(component, clustered);

        postCommitEvent(() -> new ComponentAttributesEvent(component, change, key, value));
      }
    });
  }

  /**
   * Deletes a component from the content data store.
   *
   * @param component the component to delete
   * @return {@code true} if the component was deleted
   */
  @Transactional
  public boolean deleteComponent(final Component component) {
    preCommitEvent(() -> new ComponentPreDeleteEvent(component));

    boolean deleted = dao().deleteComponent(component);
    if (deleted) {
      postCommitEvent(() -> new ComponentDeletedEvent(component));
    }
    return deleted;
  }

  /**
   * Deletes the component located at the given coordinate in the content data store.
   *
   * @param repositoryId the repository containing the component
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @param version the version of the component
   * @return {@code true} if the component was deleted
   */
  @Transactional
  public boolean deleteCoordinate(
      final int repositoryId,
      final String namespace,
      final String name,
      final String version)
  {
    return dao().readCoordinate(repositoryId, namespace, name, version)
        .map(this::deleteComponent)
        .orElse(false);
  }

  /**
   * Deletes all components in the given repository from the content data store.
   * <p>
   *
   * @param repositoryId the repository containing the components
   * @return {@code true} if any components were deleted
   */
  @Transactional
  public void deleteComponents(final int repositoryId) {
    log.debug("Deleting all components in repository {}", repositoryId);
    int deletedCount;
    while ((deletedCount = dao().deleteComponents(repositoryId, deleteBatchSize())) > 0) {
      final int finalDeletedCount = deletedCount;
      postCommitEvent(() -> new RepositoryDeletedComponentEvent(repositoryId, finalDeletedCount));
      checkCancellation();
    }
    log.debug("Deleted all components in repository {}", repositoryId);
  }

  /**
   * Purge components in the given repository whose assets were last downloaded more than given number of days ago
   *
   * @param repositoryId the repository to check
   * @param daysAgo the number of days ago to check
   * @return number of purged components
   * @since 3.24
   */
  @Transactional
  public int purgeNotRecentlyDownloaded(final int repositoryId, final int daysAgo) {
    int purged = 0;
    while (true) {
      int[] componentIds = dao().selectNotRecentlyDownloaded(repositoryId, daysAgo, deleteBatchSize());
      if (componentIds.length == 0) {
        break; // nothing left to purge
      }
      purged += purge(repositoryId, componentIds);

      checkCancellation();
    }
    return purged;
  }

  /**
   * Purge the specified components in the given repository
   *
   * @param repositoryId the repository to check
   * @param componentIds ids of the components to purge
   * @return number of purged components
   * @since 3.29
   */
  public int purge(final int repositoryId, final int[] componentIds) {
    final int iterations = componentIds.length / BATCH_SIZE + 1;

    int purged = 0;
    for (int i = 0; i < iterations; i++) {
      int startIndex = i * BATCH_SIZE;
      int[] page = new int[Math.min(BATCH_SIZE, componentIds.length - startIndex)];

      System.arraycopy(componentIds, startIndex, page, 0, page.length);
      purged += purgeBatch(repositoryId, page, Optional.empty());
    }
    return purged;
  }

  public int purge(
      final Integer repositoryId,
      final List<FluentComponent> components)
  {
    final int iterations = components.size() / BATCH_SIZE + 1;

    int purged = 0;
    for (int i = 0; i < iterations; i++) {
      int start = i * BATCH_SIZE;
      int end = Math.min(start + BATCH_SIZE, components.size());
      List<FluentComponent> page = components.subList(start, end);
      int[] componentIds = page.stream()
          .mapToInt(InternalIds::internalComponentId)
          .toArray();

      purged += purgeBatch(repositoryId, componentIds, Optional.of(page));
    }

    return purged;
  }

  @Transactional
  protected int purgeBatch(
      final int repositoryId,
      final int[] componentIds,
      final Optional<List<FluentComponent>> components)
  {
    int purged = 0;

    if (componentIds.length == 0) {
      return purged; // nothing to purge
    }

    List<FluentAsset> assets = fetchAssetsFromComponents(components);
    components.ifPresent(componentList -> resolveAssetComponents(assets, componentList));

    if ("H2".equals(thisSession().sqlDialect())) {
      // workaround lack of primitive array support in H2 (should be fixed in H2 1.4.201?)
      purged += dao().purgeSelectedComponents(stream(componentIds).boxed().toArray(Integer[]::new));
    }
    else {
      purged += dao().purgeSelectedComponents(componentIds);
    }

    components.ifPresent(componentList -> postCommitEvent(
        () -> new ComponentsPurgedAuditEvent(repositoryId, Collections.unmodifiableList(componentList))));

    preCommitEvent(() -> new ComponentPrePurgeEvent(repositoryId, componentIds));
    Supplier<Event> eventSupplier = components.<Supplier<Event>>map(
        componentList -> () -> new ComponentPurgedEvent(repositoryId, componentIds, assets))
        .orElseGet(() -> () -> new ComponentPurgedEvent(repositoryId, componentIds));
    postCommitEvent(eventSupplier);

    return purged;
  }

  /**
   * Populates the {@code component} association of each asset about to be purged, using the components already held in
   * memory.
   * <p>
   * The assets are read with lazy associations, but {@link ComponentPurgedEvent} is only posted once this transaction
   * has committed, and subscribers may handle it asynchronously on another thread. By then both the asset and the
   * component rows are gone, so a deferred load resolves to nothing and {@code asset.component()} is empty - unlike a
   * single asset delete
   * ({@link org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent AssetDeletedEvent}), where the owning
   * component is left in place and the deferred load still resolves. Every asset fetched here belongs to one of the
   * components being purged, so the association is satisfied from memory without issuing any additional queries.
   */
  private void resolveAssetComponents(final List<FluentAsset> assets, final List<FluentComponent> componentList) {
    if (assets.isEmpty() || componentList.isEmpty()) {
      return;
    }

    // Unwrap each component up front via a type-safe pattern match, so no unchecked cast can abort the purge
    // transaction. A component not backed by ComponentData cannot take part in the in-memory resolution; its assets
    // are counted below as having no matching component.
    Map<Integer, ComponentData> componentsById = new HashMap<>(componentList.size());
    for (FluentComponent component : componentList) {
      if (InternalIds.unwrap(component) instanceof ComponentData componentData && componentData.componentId != null) {
        componentsById.putIfAbsent(componentData.componentId, componentData);
      }
    }

    int noMatchingComponent = 0;
    int unexpectedDataType = 0;
    List<String> unresolvedAssets = new ArrayList<>();

    for (FluentAsset asset : assets) {
      if (!(InternalIds.unwrap(asset) instanceof AssetData assetData)) {
        unexpectedDataType++;
        recordUnresolvedAsset(unresolvedAssets, asset, OptionalInt.empty());
        continue;
      }
      // assetData.componentId is the materialized column, not the lazy association. It should always be populated
      // for assets fetched via the component_id IN (...) filter, but guard defensively: a mismatch here would
      // indicate a logic error elsewhere, and skipping the asset degrades to the pre-existing behaviour.
      ComponentData component = assetData.componentId == null ? null : componentsById.get(assetData.componentId);
      if (component == null) {
        noMatchingComponent++;
        recordUnresolvedAsset(unresolvedAssets, asset,
            assetData.componentId == null ? OptionalInt.empty() : OptionalInt.of(assetData.componentId));
        continue;
      }
      // the component was looked up by the asset's own component id, so it is by construction the component this
      // asset belongs to; assigning the association also cancels the pending lazy load
      assetData.setComponent(component);
    }

    int unresolved = noMatchingComponent + unexpectedDataType;
    if (unresolved > 0) {
      // Neither case is expected: every asset here was fetched by the component ids of componentList. Subscribers of
      // ComponentPurgedEvent see an empty component() for these assets, so downstream systems such as the IQ Server
      // are never told that they were removed.
      log.warn("Could not resolve the component association of {} of {} assets being purged; {} had no matching"
          + " component in the purge batch and {} were not backed by the expected data object."
          + " Affected assets (up to {} shown): {}",
          unresolved, assets.size(), noMatchingComponent, unexpectedDataType, UNRESOLVED_LOG_LIMIT,
          unresolvedAssets);
    }
  }

  private static void recordUnresolvedAsset(
      final List<String> unresolvedAssets,
      final FluentAsset asset,
      final OptionalInt componentId)
  {
    if (unresolvedAssets.size() < UNRESOLVED_LOG_LIMIT) {
      unresolvedAssets.add(asset.path() + " (component id "
          + (componentId.isPresent() ? componentId.getAsInt() : "unknown") + ")");
    }
  }

  private List<FluentAsset> fetchAssetsFromComponents(final Optional<List<FluentComponent>> components) {
    if (components.isEmpty()) {
      return Collections.emptyList();
    }

    List<FluentComponent> componentList = components.get();
    if (componentList.isEmpty()) {
      return Collections.emptyList();
    }

    Repository repository = componentList.get(0).repository();
    if (repository == null) {
      return Collections.emptyList();
    }

    ContentFacetSupport contentFacet = (ContentFacetSupport) repository.facet(ContentFacet.class);

    String componentIdsStr = componentList.stream()
        .map(InternalIds::internalComponentId)
        .map(String::valueOf)
        .collect(Collectors.joining(", "));

    List<FluentAsset> allAssets = new ArrayList<>();
    String continuationToken = null;

    do {
      Continuation<FluentAsset> assetPage = contentFacet.assets()
          // byFilter has no parameterized IN form: the filter is inlined via MyBatis ${} substitution. Safe here
          // because every value is a DB-assigned component id read back as an int, so nothing user-supplied reaches
          // the SQL - do not extend this filter with values from any other source.
          .byFilter("component_id IN (" + componentIdsStr + ")", Map.of())
          .browse(ASSET_BROWSE_LIMIT, continuationToken);

      // Get the next continuation token BEFORE consuming the results. An empty page means there are
      // no more results, which ContinuationArrayList signals by throwing IllegalStateException from
      // nextContinuationToken() - guard on isEmpty() rather than catching it, so that an unexpected
      // IllegalStateException surfaces instead of being silently read as "no more pages".
      continuationToken = assetPage.isEmpty() ? null : assetPage.nextContinuationToken();
      allAssets.addAll(assetPage);
    }
    while (continuationToken != null);

    return allAssets;
  }
}
