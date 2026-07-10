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
package org.sonatype.nexus.coreui.internal.atlas;

import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class SecurityDiagnosticResourceTest
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AuthorizationManager authorizationManager;

  private SecurityDiagnosticResource underTest;

  @BeforeEach
  void setup() throws NoSuchAuthorizationManagerException {
    underTest = new SecurityDiagnosticResource(securitySystem);
    when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);
  }

  @Test
  void userDiagnostic_returnsUserData() throws Exception {
    User user = createUser("testuser", "Test", "User", "test@example.com", "default", UserStatus.active, 1);
    when(securitySystem.getUser("testuser")).thenReturn(user);

    Map<String, Object> result = underTest.userDiagnostic("testuser");

    assertThat(result, is(notNullValue()));
    assertThat(result.containsKey("user"), is(true));

    @SuppressWarnings("unchecked")
    Map<String, Object> userData = (Map<String, Object>) result.get("user");
    assertThat(userData.get("userId"), is("testuser"));
    assertThat(userData.get("name"), is("Test User"));
    assertThat(userData.get("firstName"), is("Test"));
    assertThat(userData.get("lastName"), is("User"));
    assertThat(userData.get("emailAddress"), is("test@example.com"));
    assertThat(userData.get("source"), is("default"));
    assertThat(userData.get("status"), is(UserStatus.active));
    assertThat(userData.get("version"), is(1));
    assertThat(userData.get("roles"), is(notNullValue()));
  }

  @Test
  void userDiagnostic_includesRoleData() throws Exception {
    User user = createUser("testuser", "Test", "User", "test@example.com", "default", UserStatus.active, 1);
    user.addRole(new RoleIdentifier("default", "admin-role"));
    when(securitySystem.getUser("testuser")).thenReturn(user);

    Role role = new Role("admin-role", "Admin Role", "Admin role description", "default", false,
        Set.of(), Set.of());
    role.setVersion(2);
    when(authorizationManager.getRole("admin-role")).thenReturn(role);

    Map<String, Object> result = underTest.userDiagnostic("testuser");

    @SuppressWarnings("unchecked")
    Map<String, Object> userData = (Map<String, Object>) result.get("user");
    @SuppressWarnings("unchecked")
    Map<String, Object> roles = (Map<String, Object>) userData.get("roles");
    assertThat(roles.containsKey("admin-role"), is(true));

    @SuppressWarnings("unchecked")
    Map<String, Object> roleData = (Map<String, Object>) roles.get("admin-role");
    assertThat(roleData.get("name"), is("Admin Role"));
    assertThat(roleData.get("source"), is("default"));
    assertThat(roleData.get("description"), is("Admin role description"));
    assertThat(roleData.get("version"), is(2));
  }

  @Test
  void userDiagnostic_includesPrivilegeData() throws Exception {
    User user = createUser("testuser", "Test", "User", "test@example.com", "default", UserStatus.active, 1);
    user.addRole(new RoleIdentifier("default", "admin-role"));
    when(securitySystem.getUser("testuser")).thenReturn(user);

    Role role = new Role("admin-role", "Admin Role", "Admin role description", "default", false,
        Set.of(), Set.of("priv1"));
    when(authorizationManager.getRole("admin-role")).thenReturn(role);

    Privilege privilege = new Privilege();
    privilege.setId("priv1");
    privilege.setName("Privilege One");
    privilege.setDescription("Test privilege");
    Permission permission = mock(Permission.class);
    privilege.setPermission(permission);
    privilege.setVersion(3);
    when(authorizationManager.getPrivilege("priv1")).thenReturn(privilege);

    Map<String, Object> result = underTest.userDiagnostic("testuser");

    @SuppressWarnings("unchecked")
    Map<String, Object> userData = (Map<String, Object>) result.get("user");
    @SuppressWarnings("unchecked")
    Map<String, Object> roles = (Map<String, Object>) userData.get("roles");
    @SuppressWarnings("unchecked")
    Map<String, Object> roleData = (Map<String, Object>) roles.get("admin-role");
    @SuppressWarnings("unchecked")
    Map<String, Object> privileges = (Map<String, Object>) roleData.get("privileges");
    assertThat(privileges.containsKey("priv1"), is(true));

    @SuppressWarnings("unchecked")
    Map<String, Object> privilegeData = (Map<String, Object>) privileges.get("priv1");
    assertThat(privilegeData.get("name"), is("Privilege One"));
    assertThat(privilegeData.get("description"), is("Test privilege"));
    assertThat(privilegeData.get("version"), is(3));
    assertThat(privilegeData.get("permission"), is(permission));
  }

  @Test
  void userDiagnostic_includesNestedRoles() throws Exception {
    User user = createUser("testuser", "Test", "User", "test@example.com", "default", UserStatus.active, 1);
    user.addRole(new RoleIdentifier("default", "parent-role"));
    when(securitySystem.getUser("testuser")).thenReturn(user);

    Role parentRole = new Role("parent-role", "Parent Role", "Parent description", "default", false,
        Set.of("child-role"), Set.of());
    when(authorizationManager.getRole("parent-role")).thenReturn(parentRole);

    Role childRole = new Role("child-role", "Child Role", "Child description", "default", false,
        Set.of(), Set.of());
    when(authorizationManager.getRole("child-role")).thenReturn(childRole);

    Map<String, Object> result = underTest.userDiagnostic("testuser");

    @SuppressWarnings("unchecked")
    Map<String, Object> userData = (Map<String, Object>) result.get("user");
    @SuppressWarnings("unchecked")
    Map<String, Object> roles = (Map<String, Object>) userData.get("roles");
    @SuppressWarnings("unchecked")
    Map<String, Object> parentRoleData = (Map<String, Object>) roles.get("parent-role");
    @SuppressWarnings("unchecked")
    Map<String, Object> childRoles = (Map<String, Object>) parentRoleData.get("roles");
    assertThat(childRoles.containsKey("child-role"), is(true));

    @SuppressWarnings("unchecked")
    Map<String, Object> childRoleData = (Map<String, Object>) childRoles.get("child-role");
    assertThat(childRoleData.get("name"), is("Child Role"));
  }

  @Test
  void userDiagnostic_throwsNotFoundForMissingUser() throws Exception {
    when(securitySystem.getUser("unknown")).thenThrow(new UserNotFoundException("unknown"));

    assertThrows(NotFoundException.class, () -> underTest.userDiagnostic("unknown"));
  }

  @Test
  void userDiagnostic_throwsRuntimeExceptionWhenAuthorizationManagerNotFound() throws NoSuchAuthorizationManagerException {
    when(securitySystem.getAuthorizationManager("default"))
        .thenThrow(new NoSuchAuthorizationManagerException("default"));

    assertThrows(RuntimeException.class, () -> underTest.userDiagnostic("testuser"));
  }

  @Test
  void userDiagnostic_handlesFailedRoleResolution() throws Exception {
    User user = createUser("testuser", "Test", "User", "test@example.com", "default", UserStatus.active, 1);
    user.addRole(new RoleIdentifier("default", "missing-role"));
    when(securitySystem.getUser("testuser")).thenReturn(user);

    when(authorizationManager.getRole("missing-role"))
        .thenThrow(new NoSuchRoleException("missing-role"));

    Map<String, Object> result = underTest.userDiagnostic("testuser");

    @SuppressWarnings("unchecked")
    Map<String, Object> userData = (Map<String, Object>) result.get("user");
    @SuppressWarnings("unchecked")
    Map<String, Object> roles = (Map<String, Object>) userData.get("roles");
    assertThat(roles.get("missing-role"), is(instanceOf(String.class)));
    assertThat((String) roles.get("missing-role"), containsString("ERROR: Failed to resolve role"));
  }

  @Test
  void userDiagnostic_handlesFailedPrivilegeResolution() throws Exception {
    User user = createUser("testuser", "Test", "User", "test@example.com", "default", UserStatus.active, 1);
    user.addRole(new RoleIdentifier("default", "role1"));
    when(securitySystem.getUser("testuser")).thenReturn(user);

    Role role = new Role("role1", "Role One", "description", "default", false,
        Set.of(), Set.of("missing-priv"));
    when(authorizationManager.getRole("role1")).thenReturn(role);
    when(authorizationManager.getPrivilege("missing-priv"))
        .thenThrow(new NoSuchPrivilegeException("missing-priv"));

    Map<String, Object> result = underTest.userDiagnostic("testuser");

    @SuppressWarnings("unchecked")
    Map<String, Object> userData = (Map<String, Object>) result.get("user");
    @SuppressWarnings("unchecked")
    Map<String, Object> roles = (Map<String, Object>) userData.get("roles");
    @SuppressWarnings("unchecked")
    Map<String, Object> roleData = (Map<String, Object>) roles.get("role1");
    @SuppressWarnings("unchecked")
    Map<String, Object> privileges = (Map<String, Object>) roleData.get("privileges");
    assertThat(privileges.get("missing-priv"), is(instanceOf(String.class)));
    assertThat((String) privileges.get("missing-priv"), containsString("ERROR: Failed to resolve privilege"));
  }

  @Test
  void userDiagnostic_userWithNoRoles() throws Exception {
    User user = createUser("testuser", "Test", "User", "test@example.com", "default", UserStatus.active, 1);
    when(securitySystem.getUser("testuser")).thenReturn(user);

    Map<String, Object> result = underTest.userDiagnostic("testuser");

    @SuppressWarnings("unchecked")
    Map<String, Object> userData = (Map<String, Object>) result.get("user");
    @SuppressWarnings("unchecked")
    Map<String, Object> roles = (Map<String, Object>) userData.get("roles");
    assertThat(roles.isEmpty(), is(true));
  }

  private User createUser(
      final String userId,
      final String firstName,
      final String lastName,
      final String email,
      final String source,
      final UserStatus status,
      final int version)
  {
    User user = new User();
    user.setUserId(userId);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmailAddress(email);
    user.setSource(source);
    user.setStatus(status);
    user.setVersion(version);
    return user;
  }
}
