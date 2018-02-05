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
package org.sonatype.nexus.security.authz;

import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;

// FIXME: Appears unused

public class MockAuthorizationManager
    extends AbstractReadOnlyAuthorizationManager
{
  @Override
  public String getSource() {
    return "Mock";
  }

  @Override
  public Set<Role> listRoles() {
    Set<Role> roles = new HashSet<Role>();

    roles.add(new Role("mockrole1", "MockRole1", "Mock Role1", "Mock", true, null, null));
    roles.add(new Role("mockrole2", "MockRole2", "Mock Role2", "Mock", true, null, null));
    roles.add(new Role("mockrole3", "MockRole3", "Mock Role3", "Mock", true, null, null));

    return roles;
  }

  @Override
  public Role getRole(String roleId) throws NoSuchRoleException {
    for (Role role : this.listRoles()) {
      if (roleId.equals(role.getRoleId())) {
        return role;
      }
    }
    throw new NoSuchRoleException(roleId);
  }

  @Override
  public Set<Privilege> listPrivileges() {
    return new HashSet<Privilege>();
  }

  @Override
  public Privilege getPrivilege(String privilegeId) throws NoSuchPrivilegeException {
    throw new NoSuchPrivilegeException(privilegeId);
  }
}
