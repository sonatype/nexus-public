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
import java.util.Set;

import org.sonatype.nexus.common.entity.Entity;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

/**
 * Persistent role.
 */
public class CRole
    extends Entity
    implements Serializable, Cloneable
{
  private String id;

  private String name;

  private String description;

  private Set<String> privileges;

  private Set<String> roles;

  private boolean readOnly = false;

  private String version;

  public void addPrivilege(String string) {
    getPrivileges().add(string);
  }

  public void addRole(String string) {
    getRoles().add(string);
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

  public Set<String> getPrivileges() {
    if (this.privileges == null) {
      this.privileges = Sets.newHashSet();
    }

    return this.privileges;
  }

  public Set<String> getRoles() {
    if (this.roles == null) {
      this.roles = Sets.newHashSet();
    }

    return this.roles;
  }

  public boolean isReadOnly() {
    return this.readOnly;
  }

  public void removePrivilege(String string) {
    getPrivileges().remove(string);
  }

  public void removeRole(String string) {
    getRoles().remove(string);
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

  public void setPrivileges(final Set<String> privileges) {
    this.privileges = privileges;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  @Override
  public CRole clone() {
    try {
      CRole copy = (CRole) super.clone();

      if (this.privileges != null) {
        copy.privileges = Sets.newHashSet(this.privileges);
      }

      if (this.roles != null) {
        copy.roles = Sets.newHashSet(this.roles);
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
        ", privileges=" + privileges +
        ", roles=" + roles +
        ", readOnly=" + readOnly +
        ", version='" + version + '\'' +
        '}';
  }
}
