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
package org.sonatype.nexus.security.ldap.realms.api.dto;

import java.util.ArrayList;

public class LdapUserRequestDTO
{
  /**
   * User ID.  The id of the user.
   */
  private String userId;

  /**
   * Field roles.
   */
  private java.util.List<String> roles = new ArrayList<String>();

  /**
   * @return the userId
   */
  public String getUserId() {
    return userId;
  }

  /**
   * @param userId the userId to set
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * @return the roles
   */
  public java.util.List<String> getRoles() {
    return roles;
  }

  /**
   * @param roles the roles to set
   */
  public void setRoles(java.util.List<String> roles) {
    this.roles = roles;
  }

  public void addRole(String role) {
    this.roles.add(role);
  }


  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((roles == null) ? 0 : roles.hashCode());
    result = prime * result + ((userId == null) ? 0 : userId.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
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
    final LdapUserRequestDTO other = (LdapUserRequestDTO) obj;
    if (roles == null) {
      if (other.roles != null) {
        return false;
      }
    }
    else if (!roles.equals(other.roles)) {
      return false;
    }
    if (userId == null) {
      if (other.userId != null) {
        return false;
      }
    }
    else if (!userId.equals(other.userId)) {
      return false;
    }
    return true;
  }


}
