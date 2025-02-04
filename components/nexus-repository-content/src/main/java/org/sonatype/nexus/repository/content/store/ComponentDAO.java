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

import java.util.Collection;
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
   * Count all components in the given repository.
   *
   * @param repositoryId the repository to count
   * @param kind optional kind of components to count
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return count of components in the repository
   */
  int countComponents(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams);

  /**
   * Count all Nuget components with an associated blob_id in the given repository.
   *
   * @param repositoryId the repository to count
   * @param kind optional kind of components to count
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return count of components in the repository
   */
  int countComponentsWithAssetsBlobs(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams);

  /**
   * Count all components without normalized_version
   *
   * @return count of components in the table
   */
  int countUnnormalized();

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
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams);

  Continuation<ComponentData> browseComponentsEager(
      @Param("repositoryIds") Set<Integer> repositoryIds,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams);

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
      @Nullable @Param("continuationToken") String continuationToken);

  /**
   * Browse all components in the given repository ids in a paged fashion.
   *
   * @param repositoryIds the ids repositories to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Component> browseComponentsInRepositories(
      @Param("repositoryIds") Set<Integer> repositoryIds,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken);

  /**
   * Browse all components in the given repository, namespace and name, in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param namespace the component namespace to browse
   * @param name the component name to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Component> browseComponentsBySet(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param("name") String name,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken);

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
  Collection<String> browseNamespaces(@Param("repositoryId") int repositoryId);

  /**
   * Browse the names of all components under a namespace in the given repository.
   *
   * @param repositoryId the repository to browse
   * @param namespace the namespace to browse (empty string to browse components that don't have a namespace)
   * @return collection of component names
   */
  Collection<String> browseNames(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace);

  /**
   * Browse all component sets (distinct namespace+name) in the given repository.
   *
   * @param repositoryId the repository to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of componentSets and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  Continuation<ComponentSetData> browseSets(
      @Param("repositoryId") int repositoryId,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken);

  /**
   * Browse the versions of a component with the given namespace and name in the given repository.
   * <P/>
   * The result will include the empty string if there are any components that don't have a version.
   *
   * @param repositoryId the repository to browse
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @return collection of component versions
   */
  Collection<String> browseVersions(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param("name") String name);

  /**
   * Creates the given component in the content data store.
   *
   * @param component the component to create
   * @param entityVersionEnabled whether to version this component
   */
  void createComponent(
      @Param("component") ComponentData component,
      @Param("entityVersionEnabled") boolean entityVersionEnabled);

  /**
   * Retrieves a component from the content data store.
   *
   * @param componentId the internal id of the component
   * @return component if it was found
   */
  Optional<Component> readComponent(@Param("componentId") int componentId);

  /**
   * Retrieves a component located at the given coordinate in the content data store.
   *
   * @param repositoryId the repository containing the component
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @param version the version of the component
   * @return component if it was found
   */
  Optional<Component> readCoordinate(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param("name") String name,
      @Param("version") String version);

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
   *
   * @param component the component to read
   * @return component attributes if found
   */
  Optional<NestedAttributesMap> readComponentAttributes(Component component);

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
      @Param("limit") int limit);

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
}
