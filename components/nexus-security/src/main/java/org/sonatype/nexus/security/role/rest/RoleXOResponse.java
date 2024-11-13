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
package org.sonatype.nexus.security.role.rest;

import java.util.Objects;
import java.util.Set;

import org.sonatype.nexus.security.internal.rest.NexusSecurityApiConstants;
import org.sonatype.nexus.security.role.Role;

import io.swagger.annotations.ApiModelProperty;

public class RoleXOResponse
{
  @ApiModelProperty(NexusSecurityApiConstants.ROLE_ID_DESCRIPTION)
  private String id;

  @ApiModelProperty(NexusSecurityApiConstants.ROLE_SOURCE_DESCRIPTION)
  private String source;

  @ApiModelProperty(NexusSecurityApiConstants.ROLE_NAME_DESCRIPTION)
  private String name;

  @ApiModelProperty(NexusSecurityApiConstants.ROLE_DESCRIPTION_DESCRIPTION)
  private String description;

  @ApiModelProperty(NexusSecurityApiConstants.ROLE_READONLY_DESCRIPTION)
  private boolean readOnly;

  @ApiModelProperty(NexusSecurityApiConstants.ROLE_PRIVILEGES_DESCRIPTION)
  private Set<String> privileges;

  @ApiModelProperty(NexusSecurityApiConstants.ROLE_ROLES_DESCRIPTION)
  private Set<String> roles;

  public void setSource(final String source) {
    this.source = source;
  }

  public void setRoles(final Set<String> roles) {
    this.roles = roles;
  }

  public void setPrivileges(final Set<String> privileges) {
    this.privileges = privileges;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public void setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getSource() {
    return source;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public Set<String> getPrivileges() {
    return privileges;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public Boolean getReadOnly() {
    return readOnly;
  }

  @Override
  public String toString() {
    return "id: " + id + " name: " + name + " source: " + source + " description: " + description
        + " readOnly: " + readOnly + " roles: " + roles + " privileges: " + privileges;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, source, description, roles, privileges);
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof RoleXOResponse)) {
      return false;
    }

    RoleXOResponse other = (RoleXOResponse) obj;

    return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(source, other.source)
        && Objects.equals(description, other.description) && Objects.equals(roles, other.roles) && Objects
        .equals(privileges, other.privileges);
  }

  public static RoleXOResponse fromRole(final Role input) {
    RoleXOResponse role = new RoleXOResponse();
    role.setId(input.getRoleId());
    role.setSource(input.getSource());
    role.setDescription(input.getDescription() != null ? input.getDescription() : input.getRoleId());
    role.setName(input.getName() != null ? input.getName() : input.getRoleId());
    role.setPrivileges(input.getPrivileges());
    role.setRoles(input.getRoles());
    role.setReadOnly(input.isReadOnly());

    return role;
  }
}
