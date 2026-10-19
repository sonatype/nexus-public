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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.SqlAdapter;
import org.sonatype.nexus.repository.content.SqlGenerator;
import org.sonatype.nexus.repository.content.SqlQueryParameters;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * Component {@link ContentDataAccess}.
 */
@Expects(ContentRepositoryDAO.class)
@SchemaTemplate("format")
public interface ComponentDAO
    extends ContentDataAccess
{
  String FILTER_PARAMS = "filterParams";

  /**
   * MyBatis parameter name used by every read query to gate the {@code deleted_on IS NULL}
   * predicate. When the schema has not yet been migrated to the soft-delete columns the store
   * layer passes {@code false}, so the predicate is omitted (it would crash) and the query
   * still returns the correct logical result (pre-migration no row is soft-deleted).
   *
   * <p>
   * See {@code SoftDeleteColumnsMigrationStep_2_172} and
   * {@link org.sonatype.nexus.repository.content.store.ComponentStore#softDeleteAvailable()}.
   */
  String SOFT_DELETE_AVAILABLE = "softDeleteAvailable";

  /**
   * Count all components in the given repository.
   *
   * @param repositoryId the repository to count
   * @param kind optional kind of components to count
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @param softDeleteAvailable whether the {@code deleted_on} column exists; see
   *          {@link #SOFT_DELETE_AVAILABLE}
   * @return count of components in the repository
   */
  int countComponents(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Count all Nuget components with an associated blob_id in the given repository.
   */
  int countComponentsWithAssetsBlobs(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Count all components without normalized_version.
   */
  int countUnnormalized(@Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

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
  Continuation<Component> browseComponents(
      @Param("repositoryId") int repositoryId,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  Continuation<ComponentData> browseComponentsEager(
      @Param("repositoryIds") Set<Integer> repositoryIds,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse all components without normalized_version
   *
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  Continuation<ComponentData> browseUnnormalized(
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse all components in the given repository ids in a paged fashion.
   */
  Continuation<Component> browseComponentsInRepositories(
      @Param("repositoryIds") Set<Integer> repositoryIds,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse all components that have at least one associated asset in the given repository.
   */
  Continuation<Component> browseComponentsWithAssets(
      @Param("repositoryId") int repositoryId,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse all components in the given repository, namespace and name, in a paged fashion.
   */
  Continuation<Component> browseComponentsBySet(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param("name") String name,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Select components using the provided query generator and parameters.
   *
   * @param generator generator for the select
   * @param params parameters for the select
   */
  @ResultMap("ComponentDataMap")
  @ResultType(ComponentData.class)
  @SelectProvider(type = SqlAdapter.class, method = "select")
  Continuation<Component> selectComponents(
      final SqlGenerator<? extends SqlQueryParameters> generator,
      @Param("params") final SqlQueryParameters params);

  @ResultMap("ComponentAssetsDataMap")
  @ResultType(ComponentData.class)
  @SelectProvider(type = SqlAdapter.class, method = "select")
  Continuation<Component> selectComponentsWithAssets(
      final SqlGenerator<? extends SqlQueryParameters> generator,
      @Param("params") final SqlQueryParameters params);

  /**
   * Browse all component namespaces in the given repository.
   * <P/>
   * The result will include the empty string if there are any components that don't have a namespace.
   *
   * @param repositoryId the repository to browse
   * @return collection of component namespaces
   */
  Collection<String> browseNamespaces(
      @Param("repositoryId") int repositoryId,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse the names of all components under a namespace in the given repository.
   */
  Collection<String> browseNames(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Check whether a component with the given namespace and name exists in the repository.
   * <p>
   * A targeted existence probe that uses the coordinate index prefix and short-circuits on the
   * first match, avoiding a full {@code DISTINCT(name)} scan over the repository.
   */
  boolean existsByName(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param("name") String name,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse all component sets (distinct namespace+name) in the given repository.
   */
  Continuation<ComponentSetData> browseSets(
      @Param("repositoryId") int repositoryId,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse the versions of a component with the given namespace and name in the given repository.
   */
  Collection<String> browseVersions(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param("name") String name,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse the versions of a component across the given set of repositories.
   */
  Collection<String> browseVersionsByRepoIds(
      @Param("namespace") String namespace,
      @Param("name") String name,
      @Param("repositoryIds") Set<Integer> repositoryIds,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Creates the given component in the content data store.
   *
   * @param component the component to create
   * @param entityVersionEnabled whether to version this component
   */
  default void createComponent(
      final ComponentData component,
      final boolean entityVersionEnabled)
  {
    createComponent(component, entityVersionEnabled, false);
  }

  /**
   * Adds a new component to the content data store.
   *
   * @param component the component to create
   * @param entityVersionEnabled whether to version this component
   * @param softDeleteAvailable whether the {@code deleted_on}/{@code deleted_by} columns exist (see
   *          {@link #SOFT_DELETE_AVAILABLE}); when {@code false} those columns are omitted from the write so it
   *          stays valid against a pre-migration schema during a zero-downtime upgrade
   */
  void createComponent(
      @Param("component") ComponentData component,
      @Param("entityVersionEnabled") boolean entityVersionEnabled,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Retrieves a component from the content data store.
   *
   * @param componentId the internal id of the component
   * @return component if it was found
   */
  Optional<Component> readComponent(
      @Param("componentId") int componentId,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Backward-compatible overload retained so callers that predate soft-delete keep compiling and behave as
   * before. Delegates with {@code softDeleteAvailable=false}, which omits the {@code deleted_on IS NULL}
   * predicate -- the pre-soft-delete behavior, and safe when the column is not present.
   */
  default Optional<Component> readComponent(final int componentId) {
    return readComponent(componentId, false);
  }

  /**
   * Retrieves a component located at the given coordinate in the content data store.
   */
  Optional<Component> readCoordinate(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param("name") String name,
      @Param("version") String version,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Retrieves a component located at the given coordinate in within the given repository ids.
   */
  Optional<Component> readCoordinateInRepoIds(
      @Param("namespace") String namespace,
      @Param("name") String name,
      @Param("version") String version,
      @Param("repositoryIds") Set<Integer> repositoryIds,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Updates the kind of the given component in the content data store.
   *
   * @param component the component to update
   * @param entityVersionEnabled whether to version this component
   * @since 3.25
   */
  void updateComponentKind(
      @Param("component") Component component,
      @Param("entityVersionEnabled") boolean entityVersionEnabled);

  /**
   * Updates the normalized_version of the given component in the content data store.
   *
   * @param component the component to update
   * @param entityVersionEnabled whether to version this component
   * @since 3.61
   */
  void updateComponentNormalizedVersion(
      @Param("component") Component component,
      @Param("entityVersionEnabled") boolean entityVersionEnabled);

  /**
   * Retrieves the latest attributes of the given component in the content data store.
   */
  Optional<NestedAttributesMap> readComponentAttributes(
      @Param("component") Component component,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Updates the attributes of the given component in the content data store.
   *
   * @param component the component to update
   * @param entityVersionEnabled whether to version this component
   */
  void updateComponentAttributes(
      @Param("component") Component component,
      @Param("entityVersionEnabled") boolean entityVersionEnabled);

  /**
   * Deletes a component from the content data store.
   *
   * @param component the component to delete
   * @return {@code true} if the component was deleted
   */
  boolean deleteComponent(Component component);

  /**
   * Deletes all components in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the components
   * @param limit when positive limits the number of components deleted per-call
   * @return {@code true} if any components were deleted
   */
  int deleteComponents(@Param("repositoryId") int repositoryId, @Param("limit") int limit);

  /**
   * Selects components in the given repository whose assets were last downloaded more than given number of days ago.
   *
   * @param repositoryId the repository to check
   * @param daysAgo the number of days ago to check
   * @param limit when positive limits the number of components selected per-call
   * @return selected component ids
   * @since 3.26
   */
  int[] selectNotRecentlyDownloaded(
      @Param("repositoryId") int repositoryId,
      @Param("daysAgo") int daysAgo,
      @Param("limit") int limit,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Purges the selected components along with their assets.
   * <P/>
   * This version of the method is for databases that support primitive arrays.
   *
   * @param componentIds the components to purge
   * @return the number of purged components
   * @since 3.26
   */
  int purgeSelectedComponents(@Param("componentIds") int[] componentIds);

  /**
   * Purges the selected components along with their assets.
   * <P/>
   * This version of the method is for databases that don't yet support primitive arrays.
   *
   * @param componentIds the components to purge
   * @return the number of purged components
   * @since 3.26
   */
  int purgeSelectedComponents(@Param("componentIds") Integer[] componentIds);

  /**
   * Purges the selected components (and their assets) but only those still soft-deleted
   * ({@code deleted_on IS NOT NULL}). The predicate makes both DELETEs no-ops if a concurrent
   * restore clears {@code deleted_on} between the caller's id selection and this delete, preventing
   * a just-restored live component from being permanently destroyed by a racing purge. The unguarded
   * {@link #purgeSelectedComponents(int[])} remains for the GC cleanup path, which purges
   * non-soft-deleted components (NEXUS-49147 / NEXUS-51733).
   * <P/>
   * This version of the method is for databases that support primitive arrays.
   *
   * @param componentIds the components to purge
   * @return the number of purged components
   */
  int purgeSelectedSoftDeletedComponents(@Param("componentIds") int[] componentIds);

  /**
   * Guarded purge variant that only removes components still soft-deleted ({@code deleted_on IS NOT
   * NULL}). See {@link #purgeSelectedSoftDeletedComponents(int[])}.
   * <P/>
   * This version of the method is for databases that don't yet support primitive arrays.
   *
   * @param componentIds the components to purge
   * @return the number of purged components
   */
  int purgeSelectedSoftDeletedComponents(@Param("componentIds") Integer[] componentIds);

  /**
   * Marks a component as soft-deleted.
   *
   * @param componentId the internal id of the component
   * @param deletedBy the user who performed the deletion
   */
  void softDeleteComponent(
      @Param("componentId") int componentId,
      @Param("deletedBy") String deletedBy);

  /**
   * Restores a soft-deleted component by clearing the deletion metadata.
   *
   * @param componentId the internal id of the component
   */
  void undoSoftDeleteComponent(@Param("componentId") int componentId);

  /**
   * Browse soft-deleted components, optionally filtered by repository.
   *
   * @param repositoryId optional repository to filter by
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of soft-deleted components
   */
  Continuation<ComponentData> browseDeletedComponents(
      @Nullable @Param("repositoryId") Integer repositoryId,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken);

  /**
   * Count soft-deleted components, optionally filtered by repository.
   *
   * @param repositoryId optional repository to filter by
   * @return count of soft-deleted components
   */
  int countDeletedComponents(@Nullable @Param("repositoryId") Integer repositoryId);

  /**
   * Reads a soft-deleted component by its internal id.
   *
   * @param componentId the internal id of the component
   * @return the soft-deleted component if found
   */
  Optional<ComponentData> readDeletedComponent(@Param("componentId") int componentId);

  /**
   * Returns up to {@code limit} internal ids of components soft-deleted before the given timestamp,
   * ordered by id. The expire task purges these in bounded batches, cleaning the junction-table rows
   * that have no FK cascade on {@code component_id} (notably {@code {format}_component_tag}) before
   * deleting each batch via {@link #purgeSelectedComponents}. (NEXUS-49147 / NEXUS-51733)
   *
   * @param olderThan select components soft-deleted before this time
   * @param limit maximum number of ids to return (one drain batch)
   * @return list of internal component ids
   */
  List<Integer> selectDeletedComponentIdsBefore(
      @Param("olderThan") OffsetDateTime olderThan,
      @Param("limit") int limit);
}
