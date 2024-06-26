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
package org.sonatype.nexus.internal.security.model;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Named;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.DuplicateRoleException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.DuplicateUserException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.inject.Guice;
import com.google.inject.Provides;
import org.apache.shiro.authc.credential.PasswordService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.security.config.CUser.STATUS_ACTIVE;

/**
 * Tests for {@link SecurityConfigurationSourceImpl}.
 */
@Category(SQLTestGroup.class)
public class SecurityConfigurationSourceImplTest
    extends TestSupport
{
  final String PASSWORD1 =
      "$shiro1$SHA-512$1024$NYQKemFvZqat9CepP2xO9A==$4m4dBi9f/EtJLpJSW6/7+IVxW3wHR4RNeGtbopiH+D5tlVDFqNKo667eMnqWUxFrRz4Y4IQvn5hv/BnWmEfN0Q==";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule()
      .access(CPrivilegeDAO.class)
      .access(CRoleDAO.class)
      .access(CUserDAO.class)
      .access(CUserRoleMappingDAO.class);

  @Mock
  private PasswordService passwordService;

  @Mock
  private AdminPasswordFileManager adminPasswordFileManager;

  private SecurityConfigurationSourceImpl underTest;

  @Before
  public void setup() throws Exception {
    when(passwordService.encryptPassword(any())).thenReturn("encrypted");
    when(adminPasswordFileManager.readFile()).thenReturn("password");

    MemorySecurityConfiguration defaults = new MemorySecurityConfiguration();

    CPrivilegeData privilegeData = new CPrivilegeData();
    privilegeData.setId("privilege1");
    privilegeData.setName("Privilege1");
    privilegeData.setDescription("Privilege One");
    privilegeData.setType("application");
    defaults.addPrivilege(privilegeData);

    privilegeData = new CPrivilegeData();
    privilegeData.setId("privilege2");
    privilegeData.setName("Privilege2");
    privilegeData.setDescription("Privilege Two");
    privilegeData.setType("application");
    defaults.addPrivilege(privilegeData);

    privilegeData = new CPrivilegeData();
    privilegeData.setId("privilege3");
    privilegeData.setName("Privilege3");
    privilegeData.setDescription("Privilege Three");
    privilegeData.setType("application");
    defaults.addPrivilege(privilegeData);

    CRoleData roleData = new CRoleData();
    roleData.setId("role1");
    roleData.setName("Role1");
    roleData.setDescription("Role One");
    defaults.addRole(roleData);

    roleData = new CRoleData();
    roleData.setId("role2");
    roleData.setName("Role2");
    roleData.setDescription("Role Two");
    defaults.addRole(roleData);

    CUserData userData = new CUserData();
    userData.setId("user1");
    userData.setFirstName("User");
    userData.setLastName("One");
    userData.setEmail("test@example.com");
    userData.setStatus(STATUS_ACTIVE);
    userData.setPassword(PASSWORD1);
    defaults.addUser(userData, emptySet());

    SecurityConfigurationSource defaultSource = mock(SecurityConfigurationSource.class);
    when(defaultSource.getConfiguration()).thenReturn(defaults);

    underTest = Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule;
      }

      @Provides
      @Named("static")
      SecurityConfigurationSource getStaticSecurityConfigurationSource() {
        return defaultSource;
      }

      @Override
      protected void configure() {
        super.configure();
        bind(SecurityConfiguration.class).to(SecurityConfigurationImpl.class);
      }
    }).getInstance(SecurityConfigurationSourceImpl.class);

    UnitOfWork.beginBatch(() -> sessionRule.openSession(DEFAULT_DATASTORE_NAME));
    System.setProperty(ORIENT_ENABLED, "false");
    underTest.start();
    underTest.loadConfiguration();
  }

  @After
  public void cleanup() {
    System.clearProperty(ORIENT_ENABLED);
    UnitOfWork.end();
  }

  @Test
  public void testLoadingOfDefaults() {
    assertThat(underTest.getConfiguration().getPrivileges().size(), is(3));
    assertThat(underTest.getConfiguration().getRoles().size(), is(2));
    assertThat(underTest.getConfiguration().getUsers().size(), is(1));
  }

  @Test
  public void testUpdatePrivilege_persistence() {
    CPrivilegeData privilegeData = new CPrivilegeData();
    privilegeData.setId("test");
    privilegeData.setName("test");
    privilegeData.setType("test");
    underTest.getConfiguration().addPrivilege(privilegeData);
    CPrivilege privilege = underTest.getConfiguration().getPrivilege("test");
    CPrivilege newPrivilege = new CPrivilegeData();
    newPrivilege.setId("new");
    newPrivilege.setName("new");
    newPrivilege.setType("test");

    privilege.setName("foo");
    underTest.getConfiguration().updatePrivilege(privilege);

    assertThat(underTest.getConfiguration().getPrivilege("test").getName(), is("foo"));

    assertThat(underTest.getConfiguration().getPrivileges().size(), is(4));

    try {
      underTest.getConfiguration().updatePrivilege(newPrivilege);
      fail("NoSuchPrivilegeException should have been thrown");
    }
    catch (NoSuchPrivilegeException e) {
      //good
    }
  }

  @Test
  public void testAddPrivilege_duplicateId() {
    CPrivilegeData privilegeData = new CPrivilegeData();
    privilegeData.setId("test");
    privilegeData.setName("test");
    privilegeData.setType("test");
    underTest.getConfiguration().addPrivilege(privilegeData);

    privilegeData = new CPrivilegeData();
    privilegeData.setId("test");
    privilegeData.setName("test2");
    privilegeData.setType("test2");

    try {
      underTest.getConfiguration().addPrivilege(privilegeData);
      fail("DuplicatePrivilegeException should have been thrown");
    }
    catch (DuplicatePrivilegeException e) {
      //good
    }
  }

  @Test
  public void testUpdateRole_persistence() {
    CRoleData roleData = new CRoleData();
    roleData.setId("test");
    roleData.setName("test");
    roleData.setDescription("test");
    roleData.setPrivileges(singleton("priv1"));
    roleData.setPrivileges(Stream.of("role1", "role2").collect(toSet()));
    underTest.getConfiguration().addRole(roleData);
    CRole role = underTest.getConfiguration().getRole("test");
    CRole newRole = new CRoleData();
    newRole.setId("new");
    newRole.setName("new");
    newRole.setDescription("test");
    newRole.setPrivileges(emptySet());
    newRole.setPrivileges(singleton("role3"));

    role.setName("foo");
    underTest.getConfiguration().updateRole(role);

    assertThat(underTest.getConfiguration().getRole("test").getName(), is("foo"));

    assertThat(underTest.getConfiguration().getRoles().size(), is(3));

    try {
      underTest.getConfiguration().updateRole(newRole);
      fail("NoSuchRoleException should have been thrown");
    }
    catch (NoSuchRoleException e) {
      //good
    }
  }

  @Test
  public void testAddRole_duplicateId() {
    CRoleData roleData = new CRoleData();
    roleData.setId("test");
    roleData.setName("test");
    roleData.setDescription("test");
    roleData.setPrivileges(emptySet());
    roleData.setRoles(emptySet());
    underTest.getConfiguration().addRole(roleData);

    roleData = new CRoleData();
    roleData.setId("test");
    roleData.setName("test2");
    roleData.setDescription("test2");
    roleData.setPrivileges(emptySet());
    roleData.setRoles(emptySet());

    try {
      underTest.getConfiguration().addRole(roleData);
      fail("DuplicateRoleException should have been thrown");
    }
    catch (DuplicateRoleException e) {
      //good
    }
  }

  @Test
  public void testUpdateUser_persistence() throws Exception {
    CUser user1 = underTest.getConfiguration().getUser("user1");
    CUser newUser = new CUserData();
    newUser.setId("new");

    user1.setFirstName("foo");
    underTest.getConfiguration().updateUser(user1);

    assertThat(underTest.getConfiguration().getUser("user1").getFirstName(), is("foo"));

    try {
      underTest.getConfiguration().updateUser(newUser);
      fail("UserNotFoundException should have been thrown");
    }
    catch (UserNotFoundException e) {
      //good
    }
  }

  @Test
  public void testAddUser_duplicateId() throws Exception {
    CUserData userData = new CUserData();
    userData.setId("test");
    userData.setFirstName("first");
    userData.setLastName("last");
    userData.setEmail("test@example.com");
    userData.setStatus(STATUS_ACTIVE);
    userData.setPassword(PASSWORD1);

    underTest.getConfiguration().addUser(userData, emptySet());

    userData.setFirstName("first2");
    userData.setLastName("last2");

    try {
      underTest.getConfiguration().addUser(userData, emptySet());
      fail("DuplicateUserException should have been thrown");
    }
    catch (DuplicateUserException e) {
      //good
    }
  }

  @Test
  public void testUserRoleMappings_userIdsCaseSensitive() {
    Set<String> roles = singleton("test-role");
    String userId = "userid";
    String src = "other";
    CUserRoleMappingData newUserRoleMapping = new CUserRoleMappingData();
    newUserRoleMapping.setUserId(userId);
    newUserRoleMapping.setSource(src);
    newUserRoleMapping.setRoles(roles);
    underTest.getConfiguration().addUserRoleMapping(newUserRoleMapping);

    CUserRoleMapping roleMapping =
        underTest.getConfiguration().getUserRoleMapping(userId.toUpperCase(Locale.ENGLISH), src);

    assertThat(roleMapping, nullValue());

    roleMapping = underTest.getConfiguration().getUserRoleMapping(userId, src);
    roleMapping.setUserId("USERID");
    roleMapping.setRoles(singleton("new-role"));
    try {
      underTest.getConfiguration().updateUserRoleMapping(roleMapping);
      fail("NoSuchRoleMappingException should have been thrown");
    }
    catch (NoSuchRoleMappingException e) {
      //good
    }

    underTest.getConfiguration().removeUserRoleMapping(userId.toUpperCase(Locale.ENGLISH), src);

    assertThat(underTest.getConfiguration().getUserRoleMapping(userId, src), not(nullValue()));
  }

  @Test
  public void testUserRoleMappings_notCaseSensitive_bySource() throws Exception {
    testUserRoleMappings_notCaseSensitive("ldap");
    testUserRoleMappings_notCaseSensitive("crowd");
  }

  private void testUserRoleMappings_notCaseSensitive(final String src) throws Exception {
    Set<String> roles = singleton("test-role");
    String userId = "userid";
    CUserRoleMapping newUserRoleMapping = new MemoryCUserRoleMapping();
    newUserRoleMapping.setUserId(userId);
    newUserRoleMapping.setSource(src);
    newUserRoleMapping.setRoles(roles);
    underTest.getConfiguration().addUserRoleMapping(newUserRoleMapping);

    CUserRoleMapping roleMapping =
        underTest.getConfiguration().getUserRoleMapping(userId.toUpperCase(Locale.ENGLISH), src);
    assertThat(roleMapping, not(nullValue()));

    roleMapping.setUserId("USERID");
    roleMapping.setRoles(singleton("new-role"));
    underTest.getConfiguration().updateUserRoleMapping(roleMapping);
    roleMapping = underTest.getConfiguration().getUserRoleMapping(userId, src);

    assertThat(roleMapping.getRoles(), is(singleton("new-role")));

    underTest.getConfiguration().removeUserRoleMapping(userId.toUpperCase(Locale.ENGLISH), src);

    assertThat(underTest.getConfiguration().getUserRoleMapping(userId, src), nullValue());
  }
}
