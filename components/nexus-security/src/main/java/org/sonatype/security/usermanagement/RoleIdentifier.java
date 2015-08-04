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
package org.sonatype.security.usermanagement;

import java.util.HashSet;
import java.util.Set;

/**
 * Identifies a role and what source it comes from. Its basically a just a complex key for a role.
 *
 * @author Brian Demers
 */
public class RoleIdentifier
{

  private String source;

  private String roleId;

  /**
   * @param source
   * @param roleId
   */
  public RoleIdentifier(String source, String roleId) {
    this.source = source;
    this.roleId = roleId;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((roleId == null) ? 0 : roleId.hashCode());
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RoleIdentifier other = (RoleIdentifier) obj;
    if (roleId == null) {
      if (other.roleId != null) {
        return false;
      }
    }
    else if (!roleId.equals(other.roleId)) {
      return false;
    }
    if (source == null) {
      if (other.source != null) {
        return false;
      }
    }
    else if (!source.equals(other.source)) {
      return false;
    }
    return true;
  }

  public static Set<RoleIdentifier> getRoleIdentifiersForSource(String source, Set<RoleIdentifier> roleIdentifiers) {
    Set<RoleIdentifier> sourceRoleIdentifiers = new HashSet<RoleIdentifier>();

    if (roleIdentifiers != null) {
      for (RoleIdentifier roleIdentifier : roleIdentifiers) {
        if (roleIdentifier.getSource().equals(source)) {
          sourceRoleIdentifiers.add(roleIdentifier);
        }
      }
    }

    return sourceRoleIdentifiers;
  }

  @Override
  public String toString() {
    return "source: " + this.source + ", roleId: " + this.roleId;
  }

}
