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

import java.time.OffsetDateTime;
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

import org.sonatype.nexus.common.db.DatabaseCheck;
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
import org.sonatype.nexus.repository.content.event.component.ComponentRestoredEvent;
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
import org.sonatype.nexus.repository.content.upgrades.SoftDeleteAnchorMigrationStep_2_169;
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
   * Optional dependency: when present and the database schema has reached
   * {@link SoftDeleteAnchorMigrationStep_2_169#VERSION}, queries include the {@code deleted_on}
   * predicate; otherwise the predicate is omitted, giving correct pre-migration behaviour.
   * Setter-injected so it can be left null in unit tests and during early bootstrap.
   */
  private DatabaseCheck databaseCheck;

  /**
   * Sticky cache for the result of {@link #softDeleteAvailable()}. Once the schema version
   * reaches 2.154 the boolean flips to {@code true} permanently, so subsequent queries see a
   * plain field read with no further calls into {@link DatabaseCheck}. Matches the pattern used
   * in {@code JwtSessionStore#hasTypeColumn()} and {@code ApiKeyServiceImpl#isOnDBVersion()}.
   */
  private volatile boolean softDeleteAvailable;

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
   * Optional setter-injected so unit tests that construct the store directly aren't forced to
   * provide a {@link DatabaseCheck}. When null, {@link #softDeleteAvailable()} returns
   * {@code false}, which matches pre-migration behaviour exactly.
   */
  @Autowired(required = false)
  public void setDatabaseCheck(final DatabaseCheck databaseCheck) {
    this.databaseCheck = databaseCheck;
  }

  /**
   * Returns {@code true} once the schema has been migrated to at least version
   * {@link SoftDeleteAnchorMigrationStep_2_169#VERSION 2.169}, i.e. the soft-delete columns
   * exist on every dynamic-format table. The result is sticky: once it flips to true it stays
   * true for the life of the store, so per-query overhead in steady state is a single volatile
   * read.
   */
  protected boolean softDeleteAvailable() {
    // NEXUS-54969: soft-delete is disabled at the DB layer. Returning false unconditionally keeps every
    // read/write from referencing the deleted_on/deleted_by columns, so the SELECT * result shape cannot
    // drift when the (now no-op) soft-delete migration would otherwise have added those columns on a live
    // connection -- which triggered "cached plan must not change result type" (SQLState 0A000) 500s.
    // The databaseCheck wiring and sticky field are retained so this is a one-line revert to re-enable.
    return false;
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
    return dao().countComponents(repositoryId, kind, filter, filterParams, softDeleteAvailable());
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
    return dao().countComponentsWithAssetsBlobs(repositoryId, kind, filter, filterParams, softDeleteAvailable());
  }

  /**
   * Count all components without normalized_version
   *
   * @return count of components in the table
   */
  @Transactional
  public int countUnnormalized() {
    return dao().countUnnormalized(softDeleteAvailable());
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
        filterParams, softDeleteAvailable());
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
    return dao().browseComponentsEager(repositoryIds, limit, continuationToken, kind, filter,
        filterParams, softDeleteAvailable());
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
    return dao().browseUnnormalized(limit, continuationToken, softDeleteAvailable());
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
    return dao().browseComponentsInRepositories(repositoryIds, limit, continuationToken, softDeleteAvailable());
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
        componentSet.namespace(), componentSet.name(), limit, continuationToken, softDeleteAvailable());
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
    return dao().browseNamespaces(repositoryId, softDeleteAvailable());
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
    return dao().browseNames(repositoryId, namespace, softDeleteAvailable());
  }

  /**
   * Check whether a component with the given namespace and name exists in the given repository.
   * <p>
   * Index-backed existence probe; prefer this over {@link #browseNames} when only presence (not the
   * full name list) is needed.
   *
   * @param repositoryId the repository to query
   * @param namespace the namespace to match (empty string for formats without a namespace)
   * @param name the component name to match
   * @return {@code true} if at least one matching active component exists
   */
  @Transactional
  public boolean existsByName(final int repositoryId, final String namespace, final String name) {
    return dao().existsByName(repositoryId, namespace, name, softDeleteAvailable());
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
    return dao().browseSets(repositoryId, limit, continuationToken, softDeleteAvailable());
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
    return dao().browseComponentsWithAssets(repositoryId, limit, continuationToken, softDeleteAvailable());
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
    return dao().browseVersions(repositoryId, namespace, name, softDeleteAvailable());
  }

  @Transactional
  public Collection<String> browseVersionsByRepoIds(
      final String namespace,
      final String name,
      final Set<Integer> repositoryIds)
  {
    return dao().browseVersionsByRepoIds(namespace, name, repositoryIds, softDeleteAvailable());
  }

  /**
   * Creates the given component in the content data store.
   *
   * @param component the component to create
   */
  @Transactional
  public void createComponent(final ComponentData component) {
    dao().createComponent(component, clustered, softDeleteAvailable());

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
    return dao().readComponent(componentId, softDeleteAvailable());
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
    return dao().readCoordinate(repositoryId, namespace, name, version, softDeleteAvailable());
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
    return dao().readCoordinateInRepoIds(namespace, name, version, repositoryIds, softDeleteAvailable());
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
   * Updates the normalized_version of each of the given components in a single transaction, rather than one
   * transaction per component - callers repairing/backfilling many rows at once (e.g. a full-table re-scan)
   * should batch a page of components through this rather than calling {@link #updateComponentNormalizedVersion}
   * in a loop.
   *
   * @param components the components to update
   */
  @Transactional
  public void updateComponentsNormalizedVersion(final Iterable<? extends Component> components) {
    for (Component component : components) {
      dao().updateComponentNormalizedVersion(component, clustered);
    }
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
    dao().readComponentAttributes(component, softDeleteAvailable()).ifPresent(attributes -> {
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
    return dao().readCoordinate(repositoryId, namespace, name, version, softDeleteAvailable())
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
      int[] componentIds =
          dao().selectNotRecentlyDownloaded(repositoryId, daysAgo, deleteBatchSize(), softDeleteAvailable());
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
   * Soft-deletes a component by setting deletion metadata.
   * Fires {@link ComponentDeletedEvent} to trigger search purge.
   *
   * @param component the component to soft-delete
   * @param deletedBy the user who performed the deletion
   */
  @Transactional
  public void softDeleteComponent(
      final Component component,
      final String deletedBy)
  {
    int componentId = InternalIds.internalComponentId(component);
    dao().softDeleteComponent(componentId, deletedBy);
    postCommitEvent(() -> new ComponentDeletedEvent(component));
  }

  /**
   * Restores a soft-deleted component by clearing deletion metadata.
   *
   * @param component the component to restore
   */
  @Transactional
  public void undoSoftDeleteComponent(final Component component) {
    int componentId = InternalIds.internalComponentId(component);
    dao().undoSoftDeleteComponent(componentId);
    postCommitEvent(() -> new ComponentRestoredEvent(component));
  }

  /**
   * Browse soft-deleted components, optionally filtered by repository.
   */
  @Transactional
  public Continuation<ComponentData> browseDeletedComponents(
      @Nullable final Integer repositoryId,
      final int limit,
      @Nullable final String continuationToken)
  {
    return dao().browseDeletedComponents(repositoryId, limit, continuationToken);
  }

  /**
   * Count soft-deleted components, optionally filtered by repository.
   */
  @Transactional
  public int countDeletedComponents(@Nullable final Integer repositoryId) {
    return dao().countDeletedComponents(repositoryId);
  }

  /**
   * Reads a soft-deleted component by its internal id.
   */
  @Transactional
  public Optional<ComponentData> readDeletedComponent(final int componentId) {
    return dao().readDeletedComponent(componentId);
  }

  /**
   * Returns one drain batch (up to {@code deleteBatchSize}) of soft-deleted component ids older than
   * {@code olderThan}, oldest-id first. {@code ComponentSoftDeletedEntityService.purgeExpired} loops
   * over this, cleaning the {@code {format}_component_tag} junction (no FK cascade on
   * {@code component_id}) and then deleting each batch via {@link #purgeDeletedComponentsByIds}.
   */
  @Transactional
  public List<Integer> selectDeletedComponentIdsBefore(final OffsetDateTime olderThan) {
    return dao().selectDeletedComponentIdsBefore(olderThan, deleteBatchSize());
  }

  /**
   * Permanently deletes the given components and their assets (assets first, to satisfy FK ordering)
   * in a single transaction. Used by the expire task's bounded drain loop; ids should already be a
   * single batch ({@code <= deleteBatchSize}).
   * <p>
   * Only components still soft-deleted ({@code deleted_on IS NOT NULL}) are removed, so a component
   * restored between the drain's id selection and this delete is left intact rather than being
   * permanently destroyed by the racing purge (NEXUS-49147). The returned count therefore reflects
   * the rows actually purged, which may be fewer than {@code componentIds.length}.
   *
   * @return the number of components purged
   */
  @Transactional
  public int purgeDeletedComponentsByIds(final int[] componentIds) {
    if (componentIds.length == 0) {
      return 0;
    }
    if ("H2".equals(thisSession().sqlDialect())) {
      // workaround lack of primitive array support in H2
      return dao().purgeSelectedSoftDeletedComponents(stream(componentIds).boxed().toArray(Integer[]::new));
    }
    return dao().purgeSelectedSoftDeletedComponents(componentIds);
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
