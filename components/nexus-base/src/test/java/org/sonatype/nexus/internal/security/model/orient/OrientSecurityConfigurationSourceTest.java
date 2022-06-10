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
package org.sonatype.nexus.internal.security.model.orient;

import java.util.ConcurrentModificationException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.StaticSecurityConfigurationSource;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authc.credential.PasswordService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OrientSecurityConfigurationSource}.
 */
public class OrientSecurityConfigurationSourceTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("security");

  @Mock
  private PasswordService passwordService;

  @Mock
  AdminPasswordFileManager adminPasswordFileManager;

  OrientSecurityConfigurationSource source;

  @Before
  public void setup() throws Exception {
    when(passwordService.encryptPassword(any())).thenReturn("encrypted");
    when(adminPasswordFileManager.readFile()).thenReturn("password");

    source = new OrientSecurityConfigurationSource(database.getInstanceProvider(),
        new StaticSecurityConfigurationSource(passwordService, adminPasswordFileManager, false),
        new OrientCUserEntityAdapter(), new OrientCRoleEntityAdapter(), new OrientCPrivilegeEntityAdapter(),
        new OrientCUserRoleMappingEntityAdapter());
    source.start();
    source.loadConfiguration();
  }

  @Test
  public void testUpdateUser_shouldPersist() throws Exception {
    CUser admin = source.getConfiguration().getUser("admin");
    CUser newUser = new OrientCUser();
    newUser.setId("new");

    admin.setFirstName("foo");
    source.getConfiguration().updateUser(admin);

    assertThat(source.getConfiguration().getUser("admin").getFirstName(), is("foo"));

    try {
      source.getConfiguration().updateUser(newUser);
      fail("UserNotFoundException should have been thrown");
    }
    catch (UserNotFoundException e) {
      //expected
    }
  }

  @Test
  public void testUpdatePrivilege_shouldPersist() {
    CPrivilege privilege = new OrientCPrivilege();
    privilege.setId("test");
    privilege.setName("test");
    privilege.setType("test");
    source.getConfiguration().addPrivilege(privilege);

    CPrivilege newPrivilege = new OrientCPrivilege();
    newPrivilege.setId("new");
    newPrivilege.setName("new");
    newPrivilege.setType("test");

    privilege.setName("foo");
    source.getConfiguration().updatePrivilege(privilege);

    assertThat(source.getConfiguration().getPrivilege("test").getName(), is("foo"));

    try {
      source.getConfiguration().updatePrivilege(newPrivilege);
      fail("NoSuchPrivilegeException should have been thrown");
    }
    catch (NoSuchPrivilegeException e) {
      //good
    }
  }

  @Test
  public void testUpdatePrivilegeByName() {
    CPrivilege toUpdate = createFakePrivilege("test-privilege-1");

    toUpdate.setDescription("a test privilege");

    source.getConfiguration().updatePrivilegeByName(toUpdate);

    CPrivilege updated = source.getConfiguration().getPrivilegeByName(toUpdate.getName());

    assertNotNull(updated);
    assertEquals(toUpdate.getId() , updated.getId());
    assertEquals(toUpdate.getName() , updated.getName());
    assertEquals(toUpdate.getDescription() , updated.getDescription());
    assertThat(updated.getVersion() , is(2));
  }

  @Test
  public void testDeletePrivilegeByName(){
    CPrivilege toDelete = createFakePrivilege("test-privilege-delete");

    boolean deleted = source.getConfiguration().removePrivilegeByName(toDelete.getName());

    assertTrue(deleted);

    //try to get the deleted privilege again , should fail

    try {
      source.getConfiguration().getPrivilegeByName(toDelete.getName());
    }catch (NoSuchPrivilegeException e){
      //expected
    }
  }

  @Test
  public void testUpdateRole_shouldPersistAndPreventConcurrentModification() {
    CRole role = new OrientCRole();
    role.setId("test");
    role.setName("test");
    source.getConfiguration().addRole(role);
    role = source.getConfiguration().getRole("test");

    CRole newRole = new OrientCRole();
    newRole.setId("new");
    newRole.setName("new");

    role.setName("foo");
    source.getConfiguration().updateRole(role);

    assertThat(source.getConfiguration().getRole("test").getName(), is("foo"));

    role.setName("bar");

    try {
      source.getConfiguration().updateRole(role);
      fail("ConcurrentModificationException should have been thrown");
    }
    catch (ConcurrentModificationException e) {
      //good
    }

    try {
      source.getConfiguration().updateRole(newRole);
      fail("NoSuchRoleException should have been thrown");
    }
    catch (NoSuchRoleException e) {
      //good
    }
  }

  @Test
  public void testUpdateUserRoleMapping_shouldPersistAndPreventConccurentModification() throws Exception {
    CUserRoleMapping adminMapping = source.getConfiguration().getUserRoleMapping("admin", "default");

    assertThat(adminMapping.getRoles(), is(singleton("nx-admin")));

    CUserRoleMapping newUserRoleMapping = new OrientCUserRoleMapping();
    newUserRoleMapping.setUserId("badid");
    newUserRoleMapping.setSource("badsource");
    newUserRoleMapping.setRoles(emptySet());

    adminMapping.setRoles(emptySet());
    source.getConfiguration().updateUserRoleMapping(adminMapping);

    assertThat(source.getConfiguration().getUserRoleMapping("admin", "default").getRoles(), is(emptySet()));

    adminMapping.setRoles(singleton("nx-admin"));

    try {
      source.getConfiguration().updateUserRoleMapping(adminMapping);
      fail("ConcurrentModificationException should have been thrown");
    }
    catch (ConcurrentModificationException e) {
      //good
    }

    assertThat(source.getConfiguration().getUserRoleMapping("admin", "default").getRoles(), is(emptySet()));

    try {
      source.getConfiguration().updateUserRoleMapping(newUserRoleMapping);
      fail("NoSuchRoleMappingException should have been thrown");
    }
    catch (NoSuchRoleMappingException e) {
      //good
    }
  }

  @Test
  public void testUserRoleMappings_caseSensitive() {
    Set<String> roles = singleton("test-role");
    String userId = "userid";
    String src = "other";
    CUserRoleMapping newUserRoleMapping = new OrientCUserRoleMapping();
    newUserRoleMapping.setUserId(userId);
    newUserRoleMapping.setSource(src);
    newUserRoleMapping.setRoles(roles);
    source.getConfiguration().addUserRoleMapping(newUserRoleMapping);

    CUserRoleMapping roleMapping =
        source.getConfiguration().getUserRoleMapping(userId.toUpperCase(Locale.ENGLISH), src);

    assertThat(roleMapping, nullValue());

    roleMapping = source.getConfiguration().getUserRoleMapping(userId, src);
    roleMapping.setUserId("USERID");
    roleMapping.setRoles(singleton("new-role"));

    try {
      source.getConfiguration().updateUserRoleMapping(roleMapping);
      fail("NoSuchRoleMappingException should have been thrown");
    }
    catch (NoSuchRoleMappingException e) {
      //good
    }

    source.getConfiguration().removeUserRoleMapping(userId.toUpperCase(Locale.ENGLISH), src);

    assertThat(source.getConfiguration().getUserRoleMapping(userId, src), not(nullValue()));
  }

  @Test
  public void testUserRoleMappings_notCaseSensitive_bySource() throws Exception {
    testUserRoleMappings_notCaseSensitive("ldap");
    testUserRoleMappings_notCaseSensitive("crowd");
  }

  @Test
  public void testGetPrivileges() {
    Set<String> ids = ImmutableSet.of("1", "2", "3");
    for (String id : ids) {
      CPrivilege privilege = new OrientCPrivilege();
      privilege.setId(id);
      privilege.setName("name" + id);
      privilege.setType("type");
      source.getConfiguration().addPrivilege(privilege);
    }

    assertThat(source.getConfiguration().getPrivileges(ids).size(), is(ids.size()));
  }

  @Test
  public void testGetPrivilegeByName(){
   CPrivilege privilege = createFakePrivilege("test-privilege-2");

    CPrivilege read = source.getConfiguration().getPrivilegeByName(privilege.getName());

    assertNotNull(read);
    assertEquals(read.getId() , privilege.getId());
    assertEquals(read.getName() , privilege.getName());
  }

  private void testUserRoleMappings_notCaseSensitive(final String src) throws Exception {
    Set<String> roles = singleton("test-role");
    String userId = "userid";
    CUserRoleMapping newUserRoleMapping = new OrientCUserRoleMapping();
    newUserRoleMapping.setUserId(userId);
    newUserRoleMapping.setSource(src);
    newUserRoleMapping.setRoles(roles);
    source.getConfiguration().addUserRoleMapping(newUserRoleMapping);

    CUserRoleMapping roleMapping =
        source.getConfiguration().getUserRoleMapping(userId.toUpperCase(Locale.ENGLISH), src);
    assertThat(roleMapping, not(nullValue()));

    roleMapping.setUserId("USERID");
    roleMapping.setRoles(singleton("new-role"));
    source.getConfiguration().updateUserRoleMapping(roleMapping);
    roleMapping = source.getConfiguration().getUserRoleMapping(userId, src);

    assertThat(roleMapping.getRoles(), is(singleton("new-role")));

    source.getConfiguration().removeUserRoleMapping(userId.toUpperCase(Locale.ENGLISH), src);

    assertThat(source.getConfiguration().getUserRoleMapping(userId, src), nullValue());
  }

  private CPrivilege createFakePrivilege(String privilegeName){
    CPrivilege privilege = new OrientCPrivilege();
    privilege.setId(UUID.randomUUID().toString());
    privilege.setName(privilegeName);
    privilege.setType("application");

    source.getConfiguration().addPrivilege(privilege);

    return privilege;
  }
}
