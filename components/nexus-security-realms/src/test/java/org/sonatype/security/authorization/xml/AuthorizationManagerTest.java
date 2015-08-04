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
package org.sonatype.security.authorization.xml;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sonatype.security.AbstractSecurityTestCase;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CProperty;
import org.sonatype.security.model.CRole;
import org.sonatype.security.realms.tools.ConfigurationManager;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;

public class AuthorizationManagerTest
    extends AbstractSecurityTestCase
{

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    // copy the securityConf into place
    String securityXml = this.getClass().getName().replaceAll("\\.", "\\/") + "-security.xml";
    FileUtils.copyURLToFile(Thread.currentThread().getContextClassLoader().getResource(securityXml),
        new File(CONFIG_DIR, "security.xml"));
  }

  public AuthorizationManager getAuthorizationManager()
      throws Exception
  {
    return this.lookup(AuthorizationManager.class);
  }

  public ConfigurationManager getConfigurationManager()
      throws Exception
  {
    return lookup(ConfigurationManager.class, "resourceMerging");
  }

  // ROLES

  public void testListRoles()
      throws Exception
  {
    AuthorizationManager authzManager = this.getAuthorizationManager();
    Set<Role> roles = authzManager.listRoles();

    Map<String, Role> roleMap = this.toRoleMap(roles);
    Assert.assertTrue(roleMap.containsKey("role1"));
    Assert.assertTrue(roleMap.containsKey("role2"));
    Assert.assertTrue(roleMap.containsKey("role3"));
    Assert.assertEquals(3, roles.size());

    Role role3 = roleMap.get("role3");

    Assert.assertEquals("role3", role3.getRoleId());
    Assert.assertEquals("RoleThree", role3.getName());
    Assert.assertEquals("Role Three", role3.getDescription());
    Assert.assertTrue(role3.getPrivileges().contains("1"));
    Assert.assertTrue(role3.getPrivileges().contains("4"));
    Assert.assertEquals(2, role3.getPrivileges().size());
  }

  public void testGetRole()
      throws Exception
  {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Role role1 = authzManager.getRole("role1");

    Assert.assertEquals("role1", role1.getRoleId());
    Assert.assertEquals("RoleOne", role1.getName());
    Assert.assertEquals("Role One", role1.getDescription());
    Assert.assertTrue(role1.getPrivileges().contains("1"));
    Assert.assertTrue(role1.getPrivileges().contains("2"));
    Assert.assertEquals(2, role1.getPrivileges().size());
  }

  public void testAddRole()
      throws Exception
  {

    AuthorizationManager authzManager = this.getAuthorizationManager();

    Role role = new Role();
    role.setRoleId("new-role");
    role.setName("new-name");
    role.setDescription("new-description");
    role.addPrivilege("2");
    role.addPrivilege("4");

    authzManager.addRole(role);

    CRole secRole = this.getConfigurationManager().readRole(role.getRoleId());

    Assert.assertEquals(role.getRoleId(), secRole.getId());
    Assert.assertEquals(role.getName(), secRole.getName());
    Assert.assertEquals(role.getDescription(), secRole.getDescription());
    Assert.assertTrue(secRole.getPrivileges().contains("2"));
    Assert.assertTrue(secRole.getPrivileges().contains("4"));
    Assert.assertEquals(2, secRole.getPrivileges().size());

  }

  public void testUpdateRole()
      throws Exception
  {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Role role2 = authzManager.getRole("role2");
    role2.setDescription("new description");
    role2.setName("new name");

    Set<String> permissions = new HashSet<String>();
    permissions.add("2");
    role2.setPrivileges(permissions);

    authzManager.updateRole(role2);

    CRole secRole = this.getConfigurationManager().readRole(role2.getRoleId());

    Assert.assertEquals(role2.getRoleId(), secRole.getId());
    Assert.assertEquals(role2.getName(), secRole.getName());
    Assert.assertEquals(role2.getDescription(), secRole.getDescription());
    Assert.assertTrue(secRole.getPrivileges().contains("2"));
    Assert.assertEquals(1, secRole.getPrivileges().size());
  }

  public void testDeleteRole()
      throws Exception
  {
    AuthorizationManager authzManager = this.getAuthorizationManager();
    try {
      authzManager.deleteRole("INVALID-ROLENAME");
      Assert.fail("Expected NoSuchRoleException");
    }
    catch (NoSuchRoleException e) {
      // expected
    }

    // this one will work
    authzManager.deleteRole("role2");

    // this one should fail
    try {
      authzManager.deleteRole("role2");
      Assert.fail("Expected NoSuchRoleException");
    }
    catch (NoSuchRoleException e) {
      // expected
    }

    try {
      authzManager.getRole("role2");
      Assert.fail("Expected NoSuchRoleException");
    }
    catch (NoSuchRoleException e) {
      // expected
    }

    try {
      this.getConfigurationManager().readRole("role2");
      Assert.fail("Expected NoSuchRoleException");
    }
    catch (NoSuchRoleException e) {
      // expected
    }

  }

  private Map<String, Role> toRoleMap(Set<Role> roles) {
    Map<String, Role> roleMap = new HashMap<String, Role>();

    for (Role role : roles) {
      roleMap.put(role.getRoleId(), role);
    }

    return roleMap;
  }

  // Privileges

  public void testListPrivileges()
      throws Exception
  {
    AuthorizationManager authzManager = this.getAuthorizationManager();
    Set<Privilege> privileges = authzManager.listPrivileges();

    Map<String, Privilege> roleMap = this.toPrivilegeMap(privileges);
    Assert.assertTrue(roleMap.containsKey("1"));
    Assert.assertTrue(roleMap.containsKey("2"));
    Assert.assertTrue(roleMap.containsKey("3"));
    Assert.assertTrue(roleMap.containsKey("4"));
    Assert.assertEquals(4, privileges.size());

    Privilege priv3 = roleMap.get("3");

    Assert.assertEquals("3", priv3.getId());
    Assert.assertEquals("3-name", priv3.getName());
    Assert.assertEquals("Privilege Three", priv3.getDescription());
    Assert.assertEquals("method", priv3.getType());
    Assert.assertEquals("read", priv3.getPrivilegeProperty("method"));
    Assert.assertEquals("/some/path/", priv3.getPrivilegeProperty("permission"));
  }

  public void testGetPrivilege()
      throws Exception
  {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Privilege priv3 = authzManager.getPrivilege("3");

    Assert.assertEquals("3", priv3.getId());
    Assert.assertEquals("3-name", priv3.getName());
    Assert.assertEquals("Privilege Three", priv3.getDescription());
    Assert.assertEquals("method", priv3.getType());
    Assert.assertEquals("read", priv3.getPrivilegeProperty("method"));
    Assert.assertEquals("/some/path/", priv3.getPrivilegeProperty("permission"));
  }

  public void testAddPrivilege()
      throws Exception
  {
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

    Assert.assertEquals(privilege.getId(), secPriv.getId());
    Assert.assertEquals(privilege.getName(), secPriv.getName());
    Assert.assertEquals(privilege.getDescription(), secPriv.getDescription());
    Assert.assertEquals(privilege.getType(), secPriv.getType());
    Assert.assertEquals(privilege.getProperties().size(), secPriv.getProperties().size());

    Map<String, String> props = this.getPropertyMap(secPriv);
    Assert.assertEquals("bar2", props.get("foo1"));
    Assert.assertEquals("foo2", props.get("bar1"));

  }

  public void testUpdatePrivilege()
      throws Exception
  {
    AuthorizationManager authzManager = this.getAuthorizationManager();

    Privilege priv2 = authzManager.getPrivilege("2");
    priv2.setDescription("new description");

    authzManager.updatePrivilege(priv2);

    CPrivilege secPriv = this.getConfigurationManager().readPrivilege(priv2.getId());

    Assert.assertEquals(priv2.getId(), secPriv.getId());
    Assert.assertEquals(priv2.getName(), secPriv.getName());
    Assert.assertEquals(priv2.getDescription(), secPriv.getDescription());
    Assert.assertEquals(priv2.getType(), secPriv.getType());

    Map<String, String> props = this.getPropertyMap(secPriv);
    Assert.assertEquals("read", props.get("method"));
    Assert.assertEquals("/some/path/", props.get("permission"));
    Assert.assertEquals(2, secPriv.getProperties().size());
  }

  public void testDeleteUser()
      throws Exception
  {
    AuthorizationManager authzManager = this.getAuthorizationManager();
    try {
      authzManager.deletePrivilege("INVALID-PRIVILEGENAME");
      Assert.fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }

    // this one will work
    authzManager.deletePrivilege("2");

    // this one should fail
    try {
      authzManager.deletePrivilege("2");
      Assert.fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }

    try {
      authzManager.getPrivilege("2");
      Assert.fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }

    try {
      this.getConfigurationManager().readPrivilege("2");
      Assert.fail("Expected NoSuchPrivilegeException");
    }
    catch (NoSuchPrivilegeException e) {
      // expected
    }

  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getPropertyMap(CPrivilege secPriv) {
    Map<String, String> props = new HashMap<String, String>();

    for (CProperty prop : secPriv.getProperties()) {
      props.put(prop.getKey(), prop.getValue());
    }

    return props;
  }

  private Map<String, Privilege> toPrivilegeMap(Set<Privilege> privileges) {
    Map<String, Privilege> roleMap = new HashMap<String, Privilege>();

    for (Privilege privilege : privileges) {
      roleMap.put(privilege.getId(), privilege);
    }

    return roleMap;
  }

}
