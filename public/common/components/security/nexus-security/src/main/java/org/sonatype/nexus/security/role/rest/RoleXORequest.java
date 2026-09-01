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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public class RoleXORequest
{
  @NotEmpty
  @Schema(description = NexusSecurityApiConstants.ROLE_ID_DESCRIPTION)
  private String id;

  @NotEmpty
  @Schema(description = NexusSecurityApiConstants.ROLE_NAME_DESCRIPTION)
  private String name;

  @Schema(description = NexusSecurityApiConstants.ROLE_DESCRIPTION_DESCRIPTION)
  private String description;

  @Schema(description = NexusSecurityApiConstants.ROLE_PRIVILEGES_DESCRIPTION)
  private Set<String> privileges;

  @Schema(description = NexusSecurityApiConstants.ROLE_ROLES_DESCRIPTION)
  private Set<String> roles;

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

  @Override
  public String toString() {
    return "id: " + id + " name: " + name + " description: " + description + " roles: " + roles
        + " privileges: " + privileges;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, roles, privileges);
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof RoleXORequest other)) {
      return false;
    }

    return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects
        .equals(description, other.description) && Objects.equals(roles, other.roles)
        && Objects
            .equals(privileges, other.privileges);
  }
}
