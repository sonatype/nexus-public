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
package org.sonatype.security.mock.authorization;

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
@Typed(AuthorizationManager.class)
@Named("sourceB")
public class MockAuthorizationManagerB
    extends AbstractReadOnlyAuthorizationManager
{

  public String getSource() {
    return "sourceB";
  }

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

  public Privilege getPrivilege(String privilegeId)
      throws NoSuchPrivilegeException
  {
    return null;
  }

  public Role getRole(String roleId)
      throws NoSuchRoleException
  {
    return null;
  }

  public Set<Privilege> listPrivileges() {
    return null;
  }

}
