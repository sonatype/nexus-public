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

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.AttributeChange;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link FluentComponent} implementation.
 *
 * @since 3.next
 */
public class FluentComponentImpl
    implements FluentComponent
{
  private final ContentFacetSupport facet;

  private final Component component;

  public FluentComponentImpl(final ContentFacetSupport facet, final Component component) {
    this.facet = checkNotNull(facet);
    this.component = checkNotNull(component);
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
  public String version() {
    return component.version();
  }

  @Override
  public NestedAttributesMap attributes() {
    return component.attributes();
  }

  @Override
  public DateTime created() {
    return component.created();
  }

  @Override
  public DateTime lastUpdated() {
    return component.lastUpdated();
  }

  @Override
  public FluentComponent attributes(final AttributeChange change, final String key, final Object value) {
    FluentAttributesHelper.apply(component, change, key, value);
    facet.componentStore().updateComponentAttributes(component);
    return this;
  }

  @Override
  public Collection<FluentAsset> assets() {
    return new ContinuationArrayList(
        /* facet.assetStore().browseComponentAssets(component) */);
  }

  @Override
  public boolean delete() {
    return facet.componentStore().deleteComponent(component);
  }
}
