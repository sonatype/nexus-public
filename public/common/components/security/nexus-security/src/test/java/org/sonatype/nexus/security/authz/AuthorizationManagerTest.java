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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.authz.AuthorizationManagerTest.AuthorizationManagerTestConfiguration;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.PreconfiguredSecurityConfigurationSource;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

/**
 * Tests for {@link AuthorizationManager}.
 */
@Import(AuthorizationManagerTestConfiguration.class)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
class AuthorizationManagerTest
    extends AbstractSecurityTest
{
  protected static class AuthorizationManagerTestConfiguration
  {
    @Qualifier("default")
    @Primary
    @Bean
    SecurityConfigurationSource securityConfigurationSource() {
      return new PreconfiguredSecurityConfigurationSource(AuthorizationManagerTestSecurity.securityModel());
    }
  }

  AuthorizationManager getAuthorizationManager() throws Exception {
    return this.lookup(AuthorizationManager.class);
  }

  SecurityConfigurationManager getConfigurationManager() throws Exception {
    return lookup(SecurityConfigurationManager.class);
  }

  // ROLES

  @Test
  void testListRoles() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();
    Set<Role> roles = authzManager.listRoles();

    Map<String, Role> roleMap = this.toRoleMap(roles);
    assertTrue(roleMap.containsKey("role1"));
    assertTrue(roleMap.containsKey("role2"));
    assertTrue(roleMap.containsKey("role3"));
    assertEquals(3, roles.size());

    Role role3 = roleMap.get("role3");

    assertEquals("role3", role3.getRoleId());
    assertEquals("RoleThree", role3.getName());
    assertEquals("Role Three", role3.getDescription());
    assertTrue(role3.getPrivileges().contains("1"));
    assertTrue(role3.getPrivileges().contains("4"));
    assertEquals(2, role3.getPrivileges().size());
  }

  @Test
  void testGetRole() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Role role1 = authzManager.getRole("role1");

    assertEquals("role1", role1.getRoleId());
    assertEquals("RoleOne", role1.getName());
    assertEquals("Role One", role1.getDescription());
    assertTrue(role1.getPrivileges().contains("1"));
    assertTrue(role1.getPrivileges().contains("2"));
    assertEquals(2, role1.getPrivileges().size());
  }

  @Test
  void testAddRole() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Role role = new Role();
    role.setRoleId("new-role");
    role.setName("new-name");
    role.setDescription("new-description");
    role.addPrivilege("2");
    role.addPrivilege("4");

    authzManager.addRole(role);

    CRole secRole = this.getConfigurationManager().readRole(role.getRoleId());

    assertEquals(role.getRoleId(), secRole.getId());
    assertEquals(role.getName(), secRole.getName());
    assertEquals(role.getDescription(), secRole.getDescription());
    assertTrue(secRole.getPrivileges().contains("2"));
    assertTrue(secRole.getPrivileges().contains("4"));
    assertEquals(2, secRole.getPrivileges().size());
  }

  @Test
  void testUpdateRole() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Role role2 = authzManager.getRole("role2");
    role2.setDescription("new description");
    role2.setName("new name");

    Set<String> permissions = new HashSet<String>();
    permissions.add("2");
    role2.setPrivileges(permissions);

    authzManager.updateRole(role2);

    CRole secRole = this.getConfigurationManager().readRole(role2.getRoleId());

    assertEquals(role2.getRoleId(), secRole.getId());
    assertEquals(role2.getName(), secRole.getName());
    assertEquals(role2.getDescription(), secRole.getDescription());
    assertTrue(secRole.getPrivileges().contains("2"));
    assertEquals(1, secRole.getPrivileges().size());
  }

  @Test
  void testDeleteRole() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();
    try {
      authzManager.deleteRole("INVALID-ROLENAME");
      fail("Expected NoSuchRoleException");
    }
    catch (NoSuchRoleException e) {
      // expected
    }

    // this one will work
    authzManager.deleteRole("role2");

    // this one should fail
    try {
      authzManager.deleteRole("role2");
      fail("Expected NoSuchRoleException");
    }
    catch (NoSuchRoleException e) {
      // expected
    }

    try {
      authzManager.getRole("role2");
      fail("Expected NoSuchRoleException");
    }
    catch (NoSuchRoleException e) {
      // expected
    }

    try {
      this.getConfigurationManager().readRole("role2");
      fail("Expected NoSuchRoleException");
    }
    catch (NoSuchRoleException e) {
      // expected
    }
  }

  private Map<String, Role> toRoleMap(final Set<Role> roles) {
    Map<String, Role> roleMap = new HashMap<String, Role>();

    for (Role role : roles) {
      roleMap.put(role.getRoleId(), role);
    }

    return roleMap;
  }

  // Privileges

  @Test
  void testListPrivileges() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();
    Set<Privilege> privileges = authzManager.listPrivileges();

    Map<String, Privilege> roleMap = this.toPrivilegeMap(privileges);
    assertTrue(roleMap.containsKey("1"));
    assertTrue(roleMap.containsKey("2"));
    assertTrue(roleMap.containsKey("3"));
    assertTrue(roleMap.containsKey("4"));
    assertEquals(5, privileges.size());

    Privilege priv3 = roleMap.get("3");

    assertEquals("3", priv3.getId());
    assertEquals("3-name", priv3.getName());
    assertEquals("Privilege Three", priv3.getDescription());
    assertEquals("method", priv3.getType());
    assertEquals("read", priv3.getPrivilegeProperty("method"));
    assertEquals("/some/path/", priv3.getPrivilegeProperty("permission"));
  }

  @Test
  void testGetPrivilege() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Privilege priv3 = authzManager.getPrivilege("3");

    assertEquals("3", priv3.getId());
    assertEquals("3-name", priv3.getName());
    assertEquals("Privilege Three", priv3.getDescription());
    assertEquals("method", priv3.getType());
    assertEquals("read", priv3.getPrivilegeProperty("method"));
    assertEquals("/some/path/", priv3.getPrivilegeProperty("permission"));
  }

  @Test
  void testGetPrivilegeByName() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Privilege priv3 = authzManager.getPrivilegeByName("3-name");

    assertEquals("3", priv3.getId());
    assertEquals("3-name", priv3.getName());
    assertEquals("Privilege Three", priv3.getDescription());
    assertEquals("method", priv3.getType());
    assertEquals("read", priv3.getPrivilegeProperty("method"));
    assertEquals("/some/path/", priv3.getPrivilegeProperty("permission"));
  }

  @Test
  void testAddPrivilege() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Privilege privilege = new Privilege();
    privilege.addProperty("foo1", "bar2");
    privilege.addProperty("bar1", "foo2");
    privilege.setId("new-priv");
    privilege.setName("new-name");
    privilege.setDescription("new-description");
    privilege.setReadOnly(true);
    privilege.setType("TEST");

    authzManager.addPrivilege(privilege);

    CPrivilege secPriv = this.getConfigurationManager().readPrivilege(privilege.getId());

    assertEquals(privilege.getId(), secPriv.getId());
    assertEquals(privilege.getName(), secPriv.getName());
    assertEquals(privilege.getDescription(), secPriv.getDescription());
    assertEquals(privilege.getType(), secPriv.getType());
    assertEquals(privilege.getProperties().size(), secPriv.getProperties().size());

    assertEquals("bar2", secPriv.getProperty("foo1"));
    assertEquals("foo2", secPriv.getProperty("bar1"));
  }

  @Test
  void testUpdatePrivilege() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Privilege priv2 = authzManager.getPrivilege("2");
    priv2.setDescription("new description");

    authzManager.updatePrivilege(priv2);

    CPrivilege secPriv = this.getConfigurationManager().readPrivilege(priv2.getId());

    assertEquals(priv2.getId(), secPriv.getId());
    assertEquals(priv2.getName(), secPriv.getName());
    assertEquals(priv2.getDescription(), secPriv.getDescription());
    assertEquals(priv2.getType(), secPriv.getType());

    assertEquals("read", secPriv.getProperty("method"));
    assertEquals("/some/path/", secPriv.getProperty("permission"));
    assertEquals(2, secPriv.getProperties().size());
  }

  @Test
  void testUpdatePrivilegeByName() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Privilege privilege = authzManager.getPrivilegeByName("3-name");
    privilege.setDescription("updated");

    authzManager.updatePrivilegeByName(privilege);

    CPrivilege persistenPrivilege = this.getConfigurationManager().readPrivilegeByName(privilege.getName());

    assertEquals(privilege.getId(), persistenPrivilege.getId());
    assertEquals(privilege.getName(), persistenPrivilege.getName());
    assertEquals(privilege.getDescription(), persistenPrivilege.getDescription());
    assertEquals(privilege.getType(), persistenPrivilege.getType());

    assertEquals("read", persistenPrivilege.getProperty("method"));
    assertEquals("/some/path/", persistenPrivilege.getProperty("permission"));
    assertEquals(2, persistenPrivilege.getProperties().size());
  }

  @Test
  void testDeletePrivilegeUser() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();
    try {
      authzManager.deletePrivilege("INVALID-PRIVILEGENAME");
      fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }

    // this one will work
    authzManager.deletePrivilege("2");

    // this one should fail
    try {
      authzManager.deletePrivilege("2");
      fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }

    try {
      authzManager.getPrivilege("2");
      fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }

    try {
      this.getConfigurationManager().readPrivilege("2");
      fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }
  }

  @Test
  void testDeletePrivilegeByName() throws Exception {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    authzManager.deletePrivilegeByName("3-name");

    try {
      this.getConfigurationManager().readPrivilegeByName("3-name");
      fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }
  }

  private Map<String, Privilege> toPrivilegeMap(final Set<Privilege> privileges) {
    Map<String, Privilege> roleMap = new HashMap<String, Privilege>();

    for (Privilege privilege : privileges) {
      roleMap.put(privilege.getId(), privilege);
    }

    return roleMap;
  }
}
