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

import org.sonatype.nexus.security.internal.DefaultSecuritySystemTest;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;

/**
 * @see DefaultSecuritySystemTest
 */
public class MockAuthorizationManagerB
    extends AbstractReadOnlyAuthorizationManager
{
  @Override
  public String getSource() {
    return "sourceB";
  }

  @Override
  public Set<Role> listRoles() {
    Set<Role> roles = new HashSet<Role>();

    Role role1 = new Role();
    role1.setSource(this.getSource());
    role1.setName("Role 1");
    role1.setRoleId("test-role1");
    role1.addPrivilege("from-role1:read");
    role1.addPrivilege("from-role1:delete");

    Role role2 = new Role();
    role2.setSource(this.getSource());
    role2.setName("Role 2");
    role2.setRoleId("test-role2");
    role2.addPrivilege("from-role2:read");
    role2.addPrivilege("from-role2:delete");

    roles.add(role1);
    roles.add(role2);

    return roles;
  }

  @Override
  public Privilege getPrivilege(String privilegeId) throws NoSuchPrivilegeException {
    return null;
  }

  @Override
  public Role getRole(String roleId) throws NoSuchRoleException {
    return null;
  }

  @Override
  public Set<Privilege> listPrivileges() {
    return null;
  }
}
