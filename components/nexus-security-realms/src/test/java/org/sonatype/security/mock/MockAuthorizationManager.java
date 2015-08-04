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
package org.sonatype.security.mock;

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

@Singleton
@Named("Mock")
@Typed(AuthorizationManager.class)
public class MockAuthorizationManager
    extends AbstractReadOnlyAuthorizationManager
{

  public String getSource() {
    return "Mock";
  }

  public Set<Role> listRoles() {
    Set<Role> roles = new HashSet<Role>();

    roles.add(new Role("mockrole1", "MockRole1", "Mock Role1", "Mock", true, null, null));
    roles.add(new Role("mockrole2", "MockRole2", "Mock Role2", "Mock", true, null, null));
    roles.add(new Role("mockrole3", "MockRole3", "Mock Role3", "Mock", true, null, null));

    return roles;
  }

  public Role getRole(String roleId)
      throws NoSuchRoleException
  {
    for (Role role : this.listRoles()) {
      if (roleId.equals(role.getRoleId())) {
        return role;
      }
    }
    throw new NoSuchRoleException("Role: " + roleId + " could not be found.");
  }

  public Set<Privilege> listPrivileges() {
    return new HashSet<Privilege>();
  }

  public Privilege getPrivilege(String privilegeId)
      throws NoSuchPrivilegeException
  {
    throw new NoSuchPrivilegeException("Privilege: " + privilegeId + " could not be found.");
  }

}
