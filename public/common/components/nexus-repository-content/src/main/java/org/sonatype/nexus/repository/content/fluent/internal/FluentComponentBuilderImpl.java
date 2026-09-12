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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.sonatype.nexus.datastore.api.ForeignKeyViolationException;
import org.sonatype.nexus.repository.ConcurrentOperationException;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.constraints.FluentQueryConstraint;
import org.sonatype.nexus.repository.content.fluent.constraints.GroupRepositoryConstraint;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.content.fluent.constraints.GroupRepositoryConstraint.GroupRepositoryLocation.MEMBERS;
import static org.sonatype.nexus.repository.content.fluent.internal.RepositoryContentUtil.getRepositoryIds;
import static org.sonatype.nexus.repository.content.fluent.internal.RepositoryContentUtil.isGroupRepository;

/**
 * {@link FluentComponentBuilder} implementation.
 *
 * @since 3.24
 */
public class FluentComponentBuilderImpl
    implements FluentComponentBuilder
{
  private static final String CONCURRENT_DELETE_MESSAGE =
      "Upload failed: repository was concurrently deleted. Please retry.";

  private final ContentFacetSupport facet;

  private final ComponentStore<?> componentStore;

  private final Function<String, String> versionNormalizer;

  private final String name;

  private String kind = "";

  private String namespace = "";

  private String version = "";

  private String normalizedVersion = "";

  private Map<String, Object> attributes;

  public FluentComponentBuilderImpl(
      final ContentFacetSupport facet,
      final ComponentStore<?> componentStore,
      final Function<String, String> versionNormalizer,
      final String name)
  {
    this.facet = checkNotNull(facet);
    this.componentStore = checkNotNull(componentStore);
    this.versionNormalizer = checkNotNull(versionNormalizer);
    this.name = checkNotNull(name);
  }

  @Override
  public FluentComponentBuilder namespace(final String namespace) {
    this.namespace = checkNotNull(namespace);
    return this;
  }

  @Override
  public FluentComponentBuilder kind(final String kind) {
    this.kind = checkNotNull(kind);
    return this;
  }

  @Override
  public FluentComponentBuilder kind(final Optional<String> optionalKind) {
    optionalKind.ifPresent(k -> this.kind = k);
    return this;
  }

  @Override
  public FluentComponentBuilder version(final String version) {
    this.version = checkNotNull(version);
    this.normalizedVersion = versionNormalizer.apply(version);
    return this;
  }

  @Override
  public FluentComponentBuilder normalizedVersion(final String normalizedVersion) {
    this.normalizedVersion = checkNotNull(normalizedVersion);
    return this;
  }

  @Override
  public FluentComponentBuilder attributes(final String key, final Object value) {
    checkNotNull(key);
    checkNotNull(value);
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    attributes.put(key, value);
    return this;
  }

  @Override
  public FluentComponent getOrCreate() {
    return new FluentComponentImpl(facet, componentStore.getOrCreate(this::findComponent, this::createComponent));
  }

  @Override
  public Optional<FluentComponent> find() {
    return findComponent().map(component -> new FluentComponentImpl(facet, component));
  }

  private Optional<Component> findComponent() {
    return componentStore.readCoordinate(facet.contentRepositoryId(), namespace, name, version);
  }

  @Override
  public Optional<FluentComponent> findInMembers() {
    if (!isGroupRepository(facet.repository())) {
      throw new IllegalArgumentException(facet.repository() + " is not a group repository");
    }

    List<FluentQueryConstraint> membersConstraint = singletonList(new GroupRepositoryConstraint(MEMBERS));
    Set<Integer> repositoryIds = getRepositoryIds(membersConstraint, facet, facet.repository());

    return componentStore.readCoordinateInRepoIds(namespace, name, version, repositoryIds)
        .map(component -> new FluentComponentImpl(facet, component));
  }

  private Component createComponent() {
    ComponentData component = new ComponentData();
    component.setRepositoryId(facet.contentRepositoryId());
    component.setNamespace(namespace);
    component.setName(name);
    component.setKind(kind);
    component.setVersion(version);
    component.setNormalizedVersion(normalizedVersion);

    if (attributes != null && !attributes.isEmpty()) {
      component.attributes().backing().putAll(attributes);
    }

    try {
      componentStore.createComponent(component);
    }
    catch (ForeignKeyViolationException e) {
      throw new ConcurrentOperationException(CONCURRENT_DELETE_MESSAGE, e);
    }

    if (component.componentId() == null) {
      throw new ConcurrentOperationException(CONCURRENT_DELETE_MESSAGE);
    }

    return component;
  }
}
