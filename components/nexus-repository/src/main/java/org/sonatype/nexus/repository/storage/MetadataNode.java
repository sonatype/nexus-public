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
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityId;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_BUCKET;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_FORMAT;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_LAST_UPDATED;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_NAME;

/**
 * Wraps an {@code ODocument} to provide a simpler API for working with stored component and asset metadata.
 *
 * @since 3.0
 */
public abstract class MetadataNode<T>
    extends Entity
{
  private boolean newEntity = true;

  private EntityId bucketId;

  private String name;

  private DateTime lastUpdated;

  private String format;

  private NestedAttributesMap attributes;

  /**
   * Is this entity new as of this transaction?
   */
  public boolean isNew() {
    return newEntity;
  }

  T newEntity(final boolean newEntity) {
    this.newEntity = newEntity;
    return self();
  }

  /**
   * Gets the bucket this is part of.
   */
  @Nullable
  public EntityId bucketId() {
    return require(bucketId, P_BUCKET);
  }

  T bucketId(final EntityId bucketId) {
    this.bucketId = checkNotNull(bucketId);
    return self();
  }

  /**
   * Gets the name.
   */
  public String name() {
    return require(name, P_NAME);
  }

  /**
   * Sets the name.
   */
  public T name(final String name) {
    this.name = checkNotNull(name);
    return self();
  }

  /**
   * Gets the last updated date or {@code null} if undefined (the node has never been saved).
   */
  @Nullable
  public DateTime lastUpdated() {
    return lastUpdated;
  }

  /**
   * Gets the last updated date or throws a runtime exception if undefined.
   */
  public DateTime requireLastUpdated() {
    return require(lastUpdated, P_LAST_UPDATED);
  }

  /**
   * Sets the last updated date.
   */
  T lastUpdated(final DateTime lastUpdated) {
    this.lastUpdated = lastUpdated;
    return self();
  }

  /**
   * Gets the format property, which is immutable.
   */
  public String format() {
    return require(format, P_FORMAT);
  }

  /**
   * Sets the format.
   */
  T format(final String format) {
    this.format = format;
    return self();
  }

  /**
   * Gets the "attributes" property of this node, a map of maps that is possibly empty, but never {@code null}.
   */
  public NestedAttributesMap attributes() {
    return require(attributes, P_ATTRIBUTES);
  }

  /**
   * Sets the attributes.
   */
  T attributes(final NestedAttributesMap attributes) {
    this.attributes = attributes;
    return self();
  }

  /**
   * Gets the format-specific attributes of this node ("attributes.formatName").
   */
  public NestedAttributesMap formatAttributes() {
    return attributes().child(format());
  }

  protected <V> V require(final V value, final String name) {
    checkState(value != null, "Missing property: %s", name);
    return value;
  }

  @SuppressWarnings("unchecked")
  private T self() {
    return (T) this;
  }

}
