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
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.Component;

import org.apache.ibatis.annotations.Param;

/**
 * Component {@link ContentDataAccess}.
 *
 * @since 3.20
 */
@Expects(ContentRepositoryDAO.class)
@SchemaTemplate("format")
public interface ComponentDAO
    extends ContentDataAccess
{
  /**
   * Count all components in the given repository.
   *
   * @param repositoryId the repository to count
   * @return count of components in the repository
   */
  int countComponents(@Param("repositoryId") int repositoryId);

  /**
   * Browse all components in the given repository in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param kind the kind of components to return
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Component> browseComponents(@Param("repositoryId") int repositoryId,
                                           @Param("kind") @Nullable String kind,
                                           @Param("limit") int limit,
                                           @Param("continuationToken") @Nullable String continuationToken);

  /**
   * Browse all component namespaces in the given repository.
   *
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
  Collection<String> browseNames(@Param("repositoryId") int repositoryId,
                                 @Param("namespace") String namespace);

  /**
   * Browse the versions of a component with the given namespace and name in the given repository.
   *
   * The result will include the empty string if there are any components that don't have a version.
   *
   * @param repositoryId the repository to browse
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @return collection of component versions
   */
  Collection<String> browseVersions(@Param("repositoryId") int repositoryId,
                                    @Param("namespace") String namespace,
                                    @Param("name") String name);

  /**
   * Creates the given component in the content data store.
   *
   * @param component the component to create
   */
  void createComponent(ComponentData component);

  /**
   * Retrieves a component from the content data store.
   *
   * @param repositoryId the repository containing the component
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @param version the version of the component
   * @return component if it was found
   */
  Optional<Component> readComponent(@Param("repositoryId") int repositoryId,
                                    @Param("namespace") String namespace,
                                    @Param("name") String name,
                                    @Param("version") String version);

  /**
   * Updates the kind of the given component in the content data store.
   *
   * @param component the component to update
   *
   * @since 3.25.0
   */
  void updateComponentKind(Component component);

  /**
   * Updates the attributes of the given component in the content data store.
   *
   * @param component the component to update
   */
  void updateComponentAttributes(Component component);

  /**
   * Deletes a component from the content data store.
   *
   * @param component the component to delete
   * @return {@code true} if the component was deleted
   */
  boolean deleteComponent(Component component);

  /**
   * Deletes the component located at the given coordinate in the content data store.
   *
   * @param repositoryId the repository containing the component
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @param version the version of the component
   * @return {@code true} if the component was deleted
   */
  boolean deleteCoordinate(@Param("repositoryId") int repositoryId,
                           @Param("namespace") String namespace,
                           @Param("name") String name,
                           @Param("version") String version);

  /**
   * Deletes all components in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the components
   * @param limit when positive limits the number of components deleted per-call
   * @return {@code true} if any components were deleted
   */
  boolean deleteComponents(@Param("repositoryId") int repositoryId, @Param("limit") int limit);

  /**
   * Creates a temporary table local to the session for holding purge data.
   *
   * @since 3.25.0
   */
  void createTemporaryPurgeTable();

  /**
   * Purge components in the given repository whose assets were last downloaded more than given number of days ago
   *
   * @param repositoryId the repository to browse
   * @param daysAgo last downloaded more than this
   * @param limit at most items to delete
   * @return number of components deleted
   *
   * @since 3.24
   */
  int purgeNotRecentlyDownloaded(@Param("repositoryId") int repositoryId,
                                 @Param("daysAgo") int daysAgo,
                                 @Param("limit") int limit);
}
