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

import org.sonatype.nexus.common.entity.Entity;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO: Rename CapabilityStorageItem -> StorageItem?

public class CapabilityStorageItem
  extends Entity
{
  private int version;

  private String type;

  private boolean enabled;

  private String notes;

  private Map<String, String> properties;

  /**
   * @since 2.8
   */
  public CapabilityStorageItem() {
  }

  public CapabilityStorageItem(final int version,
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

  /**
   * @since 2.8
   */
  public int getVersion() {
    return version;
  }

  /**
   * @since 2.8
   */
  public void setVersion(final int version) {
    this.version = version;
  }

  /**
   * @since 2.8
   */
  public String getType() {
    return type;
  }

  /**
   * @since 2.8
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * @since 2.8
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * @since 2.8
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * @since 2.8
   */
  public String getNotes() {
    return notes;
  }

  /**
   * @since 2.8
   */
  public void setNotes(final String notes) {
    this.notes = notes;
  }

  /**
   * @since 2.8
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * @since 2.8
   */
  public void setProperties(final Map<String, String> properties) {
    this.properties = properties;
  }

}
