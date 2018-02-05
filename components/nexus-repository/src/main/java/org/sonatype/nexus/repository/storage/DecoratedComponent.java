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
package org.sonatype.nexus.repository.storage;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.decorator.DecoratedObject;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;

import org.joda.time.DateTime;

/**
 * Base abstract decorator for the {@link Component} class
 *
 * @since 3.8
 */
public abstract class DecoratedComponent
    implements Component, DecoratedObject<Component>
{
  protected final Component component;

  protected DecoratedComponent(final Component component) {
    this.component = component;
  }

  @Override
  public Component getWrappedObject() {
    return component;
  }

  @Nullable
  @Override
  public String group() {
    return component.group();
  }

  @Override
  public String requireGroup() {
    return component.requireGroup();
  }

  @Override
  public Component group(@Nullable final String group) {
    return component.group(group);
  }

  @Nullable
  @Override
  public String version() {
    return component.version();
  }

  @Override
  public String requireVersion() {
    return component.requireVersion();
  }

  @Override
  public Component version(@Nullable final String version) {
    return component.version(version);
  }

  @Override
  public boolean isNew() {
    return component.isNew();
  }

  @Override
  public Component newEntity(final boolean newEntity) {
    return component.newEntity(newEntity);
  }

  @Override
  public EntityId bucketId() {
    return component.bucketId();
  }

  @Override
  public Component bucketId(final EntityId bucketId) {
    return component.bucketId(bucketId);
  }

  @Override
  public String name() {
    return component.name();
  }

  @Override
  public Component name(final String name) {
    return component.name(name);
  }

  @Nullable
  @Override
  public DateTime lastUpdated() {
    return component.lastUpdated();
  }

  @Override
  public DateTime requireLastUpdated() {
    return component.requireLastUpdated();
  }

  @Override
  public Component lastUpdated(final DateTime lastUpdated) {
    return component.lastUpdated(lastUpdated);
  }

  @Override
  public String format() {
    return component.format();
  }

  @Override
  public Component format(final String format) {
    return component.format(format);
  }

  @Override
  public NestedAttributesMap attributes() {
    return component.attributes();
  }

  @Override
  public Component attributes(final NestedAttributesMap attributes) {
    return component.attributes(attributes);
  }

  @Override
  public NestedAttributesMap formatAttributes() {
    return component.formatAttributes();
  }

  @Nullable
  @Override
  public EntityMetadata getEntityMetadata() {
    return component.getEntityMetadata();
  }

  @Override
  public void setEntityMetadata(@Nullable final EntityMetadata metadata) {
    component.setEntityMetadata(metadata);
  }
}
