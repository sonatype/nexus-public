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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.AttributeChange;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.component.ComponentAttributesEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreateEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeleteEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentKindEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgeEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryDeleteEvent;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.repository.content.AttributesHelper.applyAttributeChange;

/**
 * {@link Component} store.
 *
 * @since 3.21
 */
@Named
public class ComponentStore<T extends ComponentDAO>
    extends ContentStoreSupport<T>
{
  private final ContentStoreEventSender eventSender;

  @Inject
  public ComponentStore(final DataSessionSupplier sessionSupplier,
                        final ContentStoreEventSender eventSender,
                        @Assisted final String contentStoreName,
                        @Assisted final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
    this.eventSender = checkNotNull(eventSender);
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
  public int countComponents(final int repositoryId,
                             @Nullable final String kind,
                             @Nullable final String filter,
                             @Nullable final Map<String, Object> filterParams)
  {
    return dao().countComponents(repositoryId, kind, filter, filterParams);
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
   *
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Component> browseComponents(final int repositoryId,
                                                  final int limit,
                                                  @Nullable final String continuationToken,
                                                  @Nullable final String kind,
                                                  @Nullable final String filter,
                                                  @Nullable final Map<String, Object> filterParams)
  {
    return dao().browseComponents(repositoryId, limit, continuationToken, kind, filter, filterParams);
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

    eventSender.postCommit(
        () -> new ComponentCreateEvent(component));
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
  public Optional<Component> readCoordinate(final int repositoryId,
                                            final String namespace,
                                            final String name,
                                            final String version)
  {
    return dao().readCoordinate(repositoryId, namespace, name, version);
  }

  /**
   * Updates the kind of the given component in the content data store.
   *
   * @param component the component to update
   */
  @Transactional
  public void updateComponentKind(final Component component) {
    dao().updateComponentKind(component);

    eventSender.postCommit(
        () -> new ComponentKindEvent(component));
  }

  /**
   * Updates the attributes of the given component in the content data store.
   *
   * @param component the component to update
   */
  @Transactional
  public void updateComponentAttributes(final Component component,
                                        final AttributeChange change,
                                        final String key,
                                        final @Nullable Object value)
  {
    // reload latest attributes, apply change, then update database if necessary
    dao().readComponentAttributes(component).ifPresent(attributes -> {
      ((ComponentData) component).setAttributes(attributes);

      if (applyAttributeChange(attributes, change, key, value)) {
        dao().updateComponentAttributes(component);

        eventSender.postCommit(
            () -> new ComponentAttributesEvent(component, change, key, value));
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
    eventSender.preCommit(
        () -> new ComponentDeleteEvent(component));

    return dao().deleteComponent(component);
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
  public boolean deleteCoordinate(final int repositoryId,
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
   *
   * Events will not be sent for these deletes, instead listen for {@link ContentRepositoryDeleteEvent}.
   *
   * @param repositoryId the repository containing the components
   * @return {@code true} if any components were deleted
   */
  @Transactional
  public boolean deleteComponents(final int repositoryId) {
    log.debug("Deleting all components in repository {}", repositoryId);
    boolean deleted = false;
    while (dao().deleteComponents(repositoryId, deleteBatchSize())) {
      commitChangesSoFar();
      deleted = true;
    }
    log.debug("Deleted all components in repository {}", repositoryId);
    return deleted;
  }

  /**
   * Purge components in the given repository whose assets were last downloaded more than given number of days ago
   *
   * @param repositoryId the repository to check
   * @param daysAgo the number of days ago to check
   * @return number of purged components
   *
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
      if ("H2".equals(thisSession().sqlDialect())) {
        // workaround lack of primitive array support in H2 (should be fixed in H2 1.4.201?)
        purged += dao().purgeSelectedComponents(stream(componentIds).boxed().toArray(Integer[]::new));
      }
      else {
        purged += dao().purgeSelectedComponents(componentIds);
      }

      eventSender.preCommit(
          () -> new ComponentPurgeEvent(repositoryId, componentIds));

      commitChangesSoFar();
    }
    return purged;
  }
}
