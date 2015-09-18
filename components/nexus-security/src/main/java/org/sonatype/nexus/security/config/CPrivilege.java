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
package org.sonatype.nexus.security.config;

import java.io.Serializable;
import java.util.Map;

import org.sonatype.nexus.common.entity.Entity;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * Persistent privilege.
 */
public class CPrivilege
    extends Entity
    implements Serializable, Cloneable
{
  private String id;

  private String name;

  private String description;

  private String type;

  private Map<String, String> properties;

  private boolean readOnly = false;

  private String version;

  public void setProperty(final String key, final String value) {
    getProperties().put(key, value);
  }

  public String getDescription() {
    return this.description;
  }

  public String getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public Map<String, String> getProperties() {
    if (this.properties == null) {
      this.properties = Maps.newHashMap();
    }
    return this.properties;
  }

  public String getProperty(final String key) {
    return getProperties().get(key);
  }

  public String getType() {
    return this.type;
  }

  public boolean isReadOnly() {
    return this.readOnly;
  }

  public void removeProperty(final String key) {
    getProperties().remove(key);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  @Override
  public CPrivilege clone() {
    try {
      CPrivilege copy = (CPrivilege) super.clone();

      if (this.properties != null) {
        copy.properties = Maps.newHashMap(this.properties);
      }

      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
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
