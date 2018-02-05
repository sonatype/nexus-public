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
package org.sonatype.nexus.security.user;

import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.role.RoleIdentifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EmptyRoleManagementTest
    extends AbstractSecurityTest
{
  @Override
  protected MemorySecurityConfiguration initialSecurityConfiguration() {
    return EmptyRoleManagementTestSecurity.securityModel();
  }

  @Test
  public void testDeleteUserWithEmptyRole()
      throws Exception
  {
    String userId = "test-user-with-empty-role";

    UserManager userManager = this.getUserManager();
    userManager.deleteUser(userId);

    SecurityConfiguration securityModel = this.getSecurityConfiguration();

    for (CUser tmpUser : securityModel.getUsers()) {
      if (userId.equals(tmpUser.getId())) {
        fail("User " + userId + " was not removed.");
      }
    }

    for (CUserRoleMapping userRoleMapping : securityModel.getUserRoleMappings()) {
      if (userId.equals(userRoleMapping.getUserId()) && "default".equals(userRoleMapping.getSource())) {
        fail("User Role Mapping was not deleted when user: " + userId + " was removed.");
      }
    }
  }

  @Test
  public void testDeleteEmptyRoleFromUser() throws Exception {
    String userId = "test-user-with-empty-role";
    String roleId = "empty-role";

    RoleIdentifier emptyRole = new RoleIdentifier("default", roleId);

    UserManager userManager = this.getUserManager();
    User user = userManager.getUser(userId);

    assertEquals(3, user.getRoles().size());
    assertTrue(user.getRoles().contains(emptyRole));

    user.removeRole(emptyRole);

    assertEquals(2, user.getRoles().size());
    assertFalse(user.getRoles().contains(emptyRole));

    userManager.updateUser(user);

    SecurityConfiguration securityModel = this.getSecurityConfiguration();
    for (CUserRoleMapping userRoleMapping : securityModel.getUserRoleMappings()) {
      if (userId.equals(userRoleMapping.getUserId()) && "default".equals(userRoleMapping.getSource())) {
        Set<String> configuredRoles = userRoleMapping.getRoles();
        assertEquals(2, configuredRoles.size());
        assertFalse(configuredRoles.contains(roleId));
      }
    }
  }

  @Test
  public void testUpdateUser() throws Exception {
    String userId = "test-user-with-empty-role";

    UserManager userManager = this.getUserManager();
    User user = userManager.getUser(userId);

    String value = "value";
    user.setEmailAddress(String.format("%s@%s", value, value));
    user.setFirstName(value);
    user.setLastName(value);

    userManager.updateUser(user);

    SecurityConfiguration securityModel = this.getSecurityConfiguration();

    boolean found = false;
    for (CUser tmpUser : securityModel.getUsers()) {
      if (userId.equals(tmpUser.getId())) {
        assertEquals(String.format("%s@%s", value, value), user.getEmailAddress());
        assertEquals(value, user.getFirstName());
        assertEquals(value, user.getLastName());
        found = true;
      }
    }
    assertTrue("user not found", found);

    found = false;
    for (CUserRoleMapping userRoleMapping : securityModel.getUserRoleMappings()) {
      if (userId.equals(userRoleMapping.getUserId()) && "default".equals(userRoleMapping.getSource())) {
        assertEquals(3, userRoleMapping.getRoles().size());
        found = true;
      }
    }

    assertTrue("userRoleMapping not found", found);
  }

  @Test
  public void testDeleteOtherRoleFromUser() throws Exception {
    String userId = "test-user-with-empty-role";
    String roleId = "role1";

    RoleIdentifier emptyRole = new RoleIdentifier("default", roleId);

    UserManager userManager = this.getUserManager();
    User user = userManager.getUser(userId);

    assertEquals(3, user.getRoles().size());
    assertTrue(user.getRoles().contains(emptyRole));

    user.removeRole(emptyRole);

    assertEquals(2, user.getRoles().size());
    assertFalse(user.getRoles().contains(emptyRole));

    userManager.updateUser(user);

    SecurityConfiguration securityModel = this.getSecurityConfiguration();
    for (CUserRoleMapping userRoleMapping : securityModel.getUserRoleMappings()) {
      if (userId.equals(userRoleMapping.getUserId()) && "default".equals(userRoleMapping.getSource())) {
        Set<String> configuredRoles = userRoleMapping.getRoles();
        assertEquals(2, configuredRoles.size());
        assertFalse(configuredRoles.contains(roleId));
      }
    }
  }
}
