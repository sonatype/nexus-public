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
package org.sonatype.nexus.internal.security.model;

import java.util.Map;

import org.sonatype.nexus.common.entity.HasStringId;
import org.sonatype.nexus.security.config.CPrivilege;

import com.google.common.collect.Maps;

/**
 * {@link CPrivilege} data.
 *
 * @since 3.21
 */
public class CPrivilegeData
    implements HasStringId, CPrivilege
{
  private String description;

  private String id;

  private String name;

  private Map<String, String> properties;

  private boolean readOnly = false;

  private String type;

  private int version = 1;

  @Override
  public CPrivilegeData clone() {
    try {
      CPrivilegeData copy = (CPrivilegeData) super.clone();

      if (this.properties != null) {
        copy.properties = Maps.newHashMap(this.properties);
      }

      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Map<String, String> getProperties() {
    if (this.properties == null) {
      this.properties = Maps.newHashMap();
    }
    return this.properties;
  }

  @Override
  public String getProperty(final String key) {
    return getProperties().get(key);
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public boolean isReadOnly() {
    return this.readOnly;
  }

  @Override
  public void removeProperty(final String key) {
    getProperties().remove(key);
  }

  @Override
  public void setDescription(final String description) {
    this.description = description;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  @Override
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public void setProperties(final Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public void setProperty(final String key, final String value) {
    getProperties().put(key, value);
  }

  @Override
  public void setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
  }

  @Override
  public void setType(final String type) {
    this.type = type;
  }

  @Override
  public void setVersion(final int version) {
    this.version = Math.max(version, 1);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", type='" + type + '\'' +
        ", properties=" + properties +
        ", readOnly=" + readOnly +
        ", version='" + version + '\'' +
        '}';
  }
}
