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
package org.sonatype.nexus.internal.capability.storage;

import java.util.Map;
import java.util.Objects;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.HasEntityId;

/**
 * {@link CapabilityStorageItem} data.
 *
 * @since 3.21
 */
public class CapabilityStorageItemData
    implements HasEntityId, CapabilityStorageItem
{
  private EntityId id;

  private int version;

  private String type;

  private boolean enabled;

  private String notes;

  private Map<String, String> properties;

  @Override
  public EntityId getId() {
    return id;
  }

  @Override
  public void setId(final EntityId id) {
    this.id = id;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(final int version) {
    this.version = version;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(final String type) {
    this.type = type;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getNotes() {
    return notes;
  }

  @Override
  public void setNotes(final String notes) {
    this.notes = notes;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public void setProperties(final Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CapabilityStorageItemData that = (CapabilityStorageItemData) o;

    return Objects.equals(type, that.type) &&
        Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, properties);
  }
}
