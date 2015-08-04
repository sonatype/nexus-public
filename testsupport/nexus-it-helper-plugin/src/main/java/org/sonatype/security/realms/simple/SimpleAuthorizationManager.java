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
package org.sonatype.security.realms.simple;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.authorization.AbstractReadOnlyAuthorizationManager;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;

import org.eclipse.sisu.Description;

/**
 * A AuthorizationManager is used if an external Realm wants to use its Group/Roles in Nexus. For example, your realm
 * might
 * already contain a group for all of your developers. Exposing these roles will allow Nexus to map your Realms roles
 * to
 * Nexus roles more easily.
 */
// This class must have a role of 'AuthorizationManager', and the hint, must match the result of getSource() and the hint
// of the corresponding Realm.
@Singleton
@Named("Simple")
@Typed(AuthorizationManager.class)
@Description("Simple Authorization Manager")
public class SimpleAuthorizationManager
    extends AbstractReadOnlyAuthorizationManager
{

  public static final String SOURCE = "Simple";

  public String getSource() {
    return SOURCE;
  }

  private Set<String> listRoleIds() {
    Set<String> roleIds = new HashSet<String>();
    roleIds.add("role-xyz");
    roleIds.add("role-abc");
    roleIds.add("role-123");

    return roleIds;
  }

  public Set<Role> listRoles() {
    Set<Role> roles = new HashSet<Role>();
    for (String roleId : this.listRoleIds()) {
      roles.add(this.toRole(roleId));
    }

    return roles;
  }

  private Role toRole(String roleId) {
    Role role = new Role();
    role.setRoleId(roleId);
    role.setSource(this.getSource());
    role.setName("Role " + roleId);
    role.setReadOnly(true);

    return role;
  }

  public Privilege getPrivilege(String privilegeId)
      throws NoSuchPrivilegeException
  {
    return null;
  }

  public Role getRole(String roleId)
      throws NoSuchRoleException
  {
    for (Role role : this.listRoles()) {
      if (role.getRoleId().equals(roleId)) {
        return role;
      }
    }
    throw new NoSuchRoleException("Role '" + roleId + "' not found.");
  }

  public Set<Privilege> listPrivileges() {
    return null;
  }

}
