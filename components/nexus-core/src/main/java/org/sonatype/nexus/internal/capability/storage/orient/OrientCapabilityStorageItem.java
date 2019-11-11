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
package org.sonatype.nexus.internal.capability.storage.orient;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Orient centric implementation of {@link CapabilityStorageItem}.
 * 
 * @since 2.8
 */
public class OrientCapabilityStorageItem
    extends AbstractEntity
    implements CapabilityStorageItem
{
  private int version;

  private String type;

  private boolean enabled;

  private String notes;

  private Map<String, String> properties;

  OrientCapabilityStorageItem() {
    // empty
  }

  public OrientCapabilityStorageItem(final int version,
                                     final String type,
                                     final boolean enabled,
                                     final String notes,
                                     final Map<String, String> properties)
  {
    this.version = version;
    this.type = checkNotNull(type);
    this.enabled = enabled;
    this.notes = notes;
    this.properties = properties;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(final int version) {
    this.version = version;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(final String notes) {
    this.notes = notes;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(final Map<String, String> properties) {
    this.properties = properties;
  }

  /**
   * Returns the {@link CapabilityIdentity} for the persisted {@link EntityId}.
   *
   * @since 3.1
   */
  static CapabilityIdentity identity(EntityId entityId) {
    return new CapabilityIdentity(entityId.getValue());
  }
}
