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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.WrappedContent;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;

/**
 * {@link FluentComponent} implementation.
 *
 * @since 3.24
 */
public class FluentComponentImpl
    implements FluentComponent, WrappedContent<Component>
{
  private final ContentFacetSupport facet;

  private final Component component;

  private final Collection<FluentAsset> assets;

  public FluentComponentImpl(final ContentFacetSupport facet, final Component component) {
    this(facet, component, null);
  }

  public FluentComponentImpl(final ContentFacetSupport facet, final Component component, @Nullable final Collection<FluentAsset> assets) {
    this.facet = checkNotNull(facet);
    this.component = checkNotNull(component);
    this.assets = assets == null ? null : Collections.unmodifiableCollection(assets);
  }

  @Override
  public Repository repository() {
    return facet.repository();
  }

  @Override
  public String namespace() {
    return component.namespace();
  }

  @Override
  public String name() {
    return component.name();
  }

  @Override
  public String kind() {
    return component.kind();
  }

  @Override
  public String version() {
    return component.version();
  }

  @Override
  public String normalizedVersion() {
    return component.normalizedVersion();
  }

  @Override
  public Integer entityVersion() {
    return component.entityVersion();
  }

  @Override
  public NestedAttributesMap attributes() {
    return component.attributes();
  }

  @Override
  public OffsetDateTime created() {
    return component.created();
  }

  @Override
  public OffsetDateTime lastUpdated() {
    return component.lastUpdated();
  }

  @Override
  public FluentComponent attributes(final AttributeOperation change, final String key, final Object value) {
    facet.stores().componentStore.updateComponentAttributes(component, change, key, value);
    return this;
  }

  @Override
  public FluentAssetBuilder asset(final String path) {
    return new FluentAssetBuilderImpl(facet, facet.stores().assetStore, path).component(this);
  }

  @Override
  public Collection<FluentAsset> assets() {
    if (assets != null) {
      return assets;
    }

    return transform(facet.stores().assetStore.browseComponentAssets(component),
        asset -> new FluentAssetImpl(facet, asset));
  }

  @Override
  public Collection<FluentAsset> assets(boolean useCache) {
    if(useCache && component instanceof ComponentData && ((ComponentData) component).getAssets() != null){
      return transform(((ComponentData) component).getAssets(), asset -> new FluentAssetImpl(facet, asset));
    }
    return assets();
  }

  @Override
  public FluentComponent kind(final String kind) {
    if (!Objects.equals(kind, component.kind())) {
      ((ComponentData) component).setKind(kind);
      facet.stores().componentStore.updateComponentKind(component);
    }
    return this;
  }

  @Override
  public boolean delete() {
    return facet.stores().componentStore.deleteComponent(component);
  }

  @Override
  public Component unwrap() {
    return component;
  }

  @Override
  public String toString() {
    return toStringExternal();
  }
}
