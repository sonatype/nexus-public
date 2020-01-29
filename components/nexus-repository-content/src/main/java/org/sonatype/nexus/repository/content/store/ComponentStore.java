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
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.transaction.Transactional;

/**
 * {@link Component} store.
 *
 * @since 3.21
 */
public abstract class ComponentStore<T extends ComponentDAO>
    extends ContentStoreSupport<T>
{
  public ComponentStore(final DataSessionSupplier sessionSupplier, final String storeName) {
    super(sessionSupplier, storeName);
  }

  /**
   * Browse all components in the given repository in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param limit maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of components and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Component> browseComponents(final int repositoryId,
                                                  final int limit,
                                                  @Nullable final String continuationToken)
  {
    return dao().browseComponents(repositoryId, limit, continuationToken);
  }

  /**
   * Browse all component namespaces in the given repository.
   *
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
   * Browse the versions of a component with the given namespace and name in the given repository.
   *
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

  /**
   * Creates the given component in the content data store.
   *
   * @param component the component to create
   */
  @Transactional
  public void createComponent(final ComponentData component) {
    dao().createComponent(component);
  }

  /**
   * Retrieves a component from the content data store.
   *
   * @param repositoryId the repository containing the component
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @param version the version of the component
   * @return component if it was found
   */
  @Transactional
  public Optional<Component> readComponent(final int repositoryId,
                                           final String namespace,
                                           final String name,
                                           final String version)
  {
    return dao().readComponent(repositoryId, namespace, name, version);
  }

  /**
   * Updates the attributes of the given component in the content data store.
   *
   * @param component the component to update
   */
  @Transactional
  public void updateComponentAttributes(final ComponentData component) {
    dao().updateComponentAttributes(component);
  }

  /**
   * Deletes a component from the content data store.
   *
   * @param repositoryId the repository containing the component
   * @param namespace the namespace of the component
   * @param name the name of the component
   * @param version the version of the component
   * @return {@code true} if the component was deleted
   */
  @Transactional
  public boolean deleteComponent(final int repositoryId,
                                 final String namespace,
                                 final String name,
                                 final String version)
  {
    return dao().deleteComponent(repositoryId, namespace, name, version);
  }

  /**
   * Deletes all components in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the components
   * @return {@code true} if any components were deleted
   */
  @Transactional
  public boolean deleteComponents(final int repositoryId) {
    return dao().deleteComponents(repositoryId);
  }
}
