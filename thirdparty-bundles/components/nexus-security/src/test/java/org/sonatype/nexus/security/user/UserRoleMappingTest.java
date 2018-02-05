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

import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.internal.SecurityConfigurationManagerImpl;

import org.junit.Assert;
import org.junit.Test;

public class UserRoleMappingTest
    extends AbstractSecurityTest
{
  @Override
  protected MemorySecurityConfiguration initialSecurityConfiguration() {
    return UserRoleMappingTestSecurity.securityModel();
  }

  public SecurityConfigurationManager getConfigManager() throws Exception {
    return this.lookup(SecurityConfigurationManagerImpl.class);
  }

  @Test
  public void testGetUser() throws Exception {
    SecurityConfigurationManager config = this.getConfigManager();

    CUser user = config.readUser("test-user");
    Assert.assertEquals(user.getId(), "test-user");
    Assert.assertEquals(user.getEmail(), "test-user@example.org");
    Assert.assertEquals(user.getFirstName(), "Test");
    Assert.assertEquals(user.getLastName(), "User");
    Assert.assertEquals(user.getPassword(), "b2a0e378437817cebdf753d7dff3dd75483af9e0");
    Assert.assertEquals(user.getStatus(), "active");

    CUserRoleMapping mapping = config.readUserRoleMapping("test-user", "default");

    Assert.assertTrue(mapping.getRoles().contains("role1"));
    Assert.assertTrue(mapping.getRoles().contains("role2"));
    Assert.assertEquals(2, mapping.getRoles().size());
  }

  @Test
  public void testGetUserWithEmptyRole() throws Exception {
    SecurityConfigurationManager config = this.getConfigManager();

    CUser user = config.readUser("test-user-with-empty-role");
    Assert.assertEquals(user.getId(), "test-user-with-empty-role");
    Assert.assertEquals(user.getEmail(), "test-user-with-empty-role@example.org");
    Assert.assertEquals(user.getFirstName(), "Test");
    Assert.assertEquals(user.getLastName(), "User With Empty Role");
    Assert.assertEquals(user.getPassword(), "b2a0e378437817cebdf753d7dff3dd75483af9e0");
    Assert.assertEquals(user.getStatus(), "active");

    CUserRoleMapping mapping = config.readUserRoleMapping("test-user-with-empty-role", "default");

    Assert.assertTrue(mapping.getRoles().contains("empty-role"));
    Assert.assertTrue(mapping.getRoles().contains("role1"));
    Assert.assertTrue(mapping.getRoles().contains("role2"));
    Assert.assertEquals(3, mapping.getRoles().size());

    // try to update empty role
    config.updateUserRoleMapping(mapping);

    // make sure we still have the role mappings
    mapping = config.readUserRoleMapping("test-user-with-empty-role", "default");

    Assert.assertTrue(mapping.getRoles().contains("empty-role"));
    Assert.assertTrue(mapping.getRoles().contains("role1"));
    Assert.assertTrue(mapping.getRoles().contains("role2"));
    Assert.assertEquals(3, mapping.getRoles().size());
  }

  @Test
  public void testUpdateUsersRoles() throws Exception {
    SecurityConfigurationManager config = this.getConfigManager();

    // make sure we have exactly 4 user role mappings
    Assert.assertEquals(5, config.listUserRoleMappings().size());

    // get the test-user and add a role
    CUser user = config.readUser("test-user");

    CUserRoleMapping roleMapping = config.readUserRoleMapping("test-user", "default");
    Set<String> roles = roleMapping.getRoles();
    roles.add("role3");

    // update the user
    config.updateUser(user, new HashSet<String>(roles));

    // make sure we have exactly 4 user role mappings
    Assert.assertEquals(5, config.listUserRoleMappings().size());
  }
}
