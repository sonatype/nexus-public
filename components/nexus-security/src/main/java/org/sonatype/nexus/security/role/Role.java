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
package org.sonatype.nexus.security.role;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple bean that represents a security Role.
 */
public class Role
    implements Comparable<Role>
{
  private String roleId;

  private String name;

  private String description;

  private String source;

  private boolean readOnly;

  private Set<String> roles = new HashSet<String>();

  private Set<String> privileges = new HashSet<String>();

  private String version;

  public Role() {
  }

  public Role(final String roleId,
              final String name,
              final String description,
              final String source,
              final boolean readOnly,
              final Set<String> roles,
              final Set<String> privileges)
  {
    this.roleId = roleId;
    this.name = name;
    this.description = description;
    this.source = source;
    this.readOnly = readOnly;
    this.roles = roles;
    this.privileges = privileges;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void addRole(String role) {
    this.roles.add(role);
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  public Set<String> getPrivileges() {
    return privileges;
  }

  public void addPrivilege(String privilege) {
    this.privileges.add(privilege);
  }

  public void setPrivileges(Set<String> privilege) {
    this.privileges = privilege;
  }

  public int compareTo(Role o) {
    final int before = -1;
    final int equal = 0;
    final int after = 1;

    if (this == o) {
      return equal;
    }

    if (o == null) {
      return after;
    }

    if (getRoleId() == null && o.getRoleId() != null) {
      return before;
    }
    else if (getRoleId() != null && o.getRoleId() == null) {
      return after;
    }

    // the roleIds are not null
    int result = getRoleId().compareTo(o.getRoleId());
    if (result != equal) {
      return result;
    }

    if (getSource() == null) {
      return before;
    }

    // if we are all the way to this point, the RoleIds are equal and this.getSource != null, so just return a
    // compareTo on the source
    return getSource().compareTo(o.getSource());
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Role role = (Role) o;

    if (readOnly != role.readOnly) {
      return false;
    }
    if (description != null ? !description.equals(role.description) : role.description != null) {
      return false;
    }
    if (name != null ? !name.equals(role.name) : role.name != null) {
      return false;
    }
    if (privileges != null ? !privileges.equals(role.privileges) : role.privileges != null) {
      return false;
    }
    if (roleId != null ? !roleId.equals(role.roleId) : role.roleId != null) {
      return false;
    }
    if (roles != null ? !roles.equals(role.roles) : role.roles != null) {
      return false;
    }
    if (source != null ? !source.equals(role.source) : role.source != null) {
      return false;
    }
    if (version != null ? !version.equals(role.version) : role.version != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = roleId != null ? roleId.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (source != null ? source.hashCode() : 0);
    result = 31 * result + (readOnly ? 1 : 0);
    result = 31 * result + (roles != null ? roles.hashCode() : 0);
    result = 31 * result + (privileges != null ? privileges.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "roleId='" + roleId + '\'' +
        ", name='" + name + '\'' +
        ", source='" + source + '\'' +
        '}';
  }
}
