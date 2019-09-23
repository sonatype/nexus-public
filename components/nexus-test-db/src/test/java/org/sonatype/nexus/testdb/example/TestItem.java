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
package org.sonatype.nexus.testdb.example;

import java.util.Map;
import java.util.Objects;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.HasEntityId;
import org.sonatype.nexus.common.entity.HasVersion;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.hash;

public class TestItem
    implements HasEntityId, HasVersion
{
  private EntityId id;

  private int version;

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
    this.notes = checkNotNull(notes);
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = checkNotNull(properties);
  }

  @Override
  public int hashCode() {
    return hash(id, version, enabled, notes, properties);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof TestItem) {
      TestItem item = (TestItem) obj;
      return Objects.equals(item.id, id)
          && Objects.equals(item.version, version)
          && Objects.equals(item.enabled, enabled)
          && Objects.equals(item.notes, notes)
          && Objects.equals(item.properties, properties);
    }
    return false;
  }
}
