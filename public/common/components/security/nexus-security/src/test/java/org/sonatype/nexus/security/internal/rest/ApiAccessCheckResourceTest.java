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
package org.sonatype.nexus.security.internal.rest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.WildcardPermission2;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class ApiAccessCheckResourceTest
{
  private static final String TEST_USER_ID = "testuser";

  private static final String TEST_ROLE_ID = "nx-admin";

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private RolePermissionResolver rolePermissionResolver;

  @Mock
  private AuthorizationManager authorizationManager;

  @Mock
  private EndpointPermissionRegistry endpointPermissionRegistry;

  private ApiAccessCheckResource underTest;

  @BeforeEach
  void setup() throws Exception {
    underTest =
        new ApiAccessCheckResource(securitySystem, securityHelper, endpointPermissionRegistry, rolePermissionResolver);

    // Default: users endpoint resolves to nexus:users:read
    lenient().when(endpointPermissionRegistry.getEndpointInfo("/service/rest/v1/security/users", "GET"))
        .thenReturn(createEndpointInfo("GET", "/service/rest/v1/security/users", "nexus:users:read"));
    lenient().when(endpointPermissionRegistry.getEndpointInfo("/service/rest/v1/security/users", "POST"))
        .thenReturn(createEndpointInfo("POST", "/service/rest/v1/security/users", "nexus:users:create"));
    lenient().when(endpointPermissionRegistry.getEndpointInfo("/service/rest/v1/status", "GET"))
        .thenReturn(null);
    lenient().when(endpointPermissionRegistry.getEndpointsForPermission("nexus:users:read"))
        .thenReturn(List.of());
    lenient().when(endpointPermissionRegistry.getEndpointsForPermission("nexus:users:*"))
        .thenReturn(List.of());
    // Default: registry is ready — tests that need the not-ready path override this explicitly
    lenient().when(endpointPermissionRegistry.isReady()).thenReturn(true);

    // Setup default user
    User testUser = createUser(TEST_USER_ID, "Test", "User");
    lenient().when(securitySystem.getUser(TEST_USER_ID)).thenReturn(testUser);
    lenient().when(securitySystem.currentUser()).thenReturn(testUser);

    // Setup authorization manager
    lenient().when(securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE)).thenReturn(authorizationManager);

    // Setup role
    Role testRole = createRole(TEST_ROLE_ID, "Administrator");
    lenient().when(authorizationManager.getRole(TEST_ROLE_ID)).thenReturn(testRole);

    // Setup privileges
    Set<Privilege> privileges = new HashSet<>();
    Privilege usersRead = createPrivilege("nx-users-all", "All users", "nexus:users:*");
    privileges.add(usersRead);
    lenient().when(securitySystem.listPrivileges()).thenReturn(privileges);

    // Default: user is admin (for backward compat with existing tests)
    lenient().when(securityHelper.isAllPermitted()).thenReturn(true);
  }

  @Test
  void testCheckAccess_userHasAccess() throws Exception {
    // Setup user with permission
    SimplePrincipalCollection principals = new SimplePrincipalCollection(TEST_USER_ID, "NexusAuthorizingRealm");
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:users:read"))).thenReturn(true);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.isHasAccess(), is(true));
    assertThat(result.getRequiredPermission(), is(equalTo("nexus:users:read")));
  }

  @Test
  void testCheckAccess_userDoesNotHaveAccess() throws Exception {
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:users:read"))).thenReturn(false);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.isHasAccess(), is(false));
  }

  @Test
  void testCheckAccess_currentUser() throws Exception {
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:users:read"))).thenReturn(true);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        null, null, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.isHasAccess(), is(true));
  }

  @Test
  void testCheckAccess_roleHasAccess() {
    // Setup role with permission
    Collection<Permission> permissions = Collections.singleton(new WildcardPermission2("nexus:users:*"));
    when(rolePermissionResolver.resolvePermissionsInRole(TEST_ROLE_ID)).thenReturn(permissions);

    Role role = createRole(TEST_ROLE_ID, "Administrator");
    lenient().when(authorizationManager.getRole(TEST_ROLE_ID)).thenReturn(role);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        null, TEST_ROLE_ID, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.isHasAccess(), is(true));
    assertThat(result.getRequiredPermission(), is(equalTo("nexus:users:read")));
  }

  @Test
  void testCheckAccess_roleDoesNotHaveAccess() {
    // Setup role without required permission
    Collection<Permission> permissions = Collections.singleton(new WildcardPermission2("nexus:roles:read"));
    when(rolePermissionResolver.resolvePermissionsInRole(TEST_ROLE_ID)).thenReturn(permissions);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        null, TEST_ROLE_ID, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.isHasAccess(), is(false));
  }

  @Test
  void testCheckAccess_userAndRoleMutuallyExclusive() {
    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, TEST_ROLE_ID, "/service/rest/v1/security/users", "GET");

    WebApplicationMessageException exception = assertThrows(
        WebApplicationMessageException.class,
        () -> underTest.checkAccess(request));

    assertThat(exception.getResponse().getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
  }

  @Test
  void testCheckAccess_userNotFound_genericError() throws Exception {
    // MEDIUM #4: Should return generic error to prevent user enumeration
    when(securitySystem.getUser("unknown")).thenThrow(new UserNotFoundException("unknown"));

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        "unknown", null, "/service/rest/v1/security/users", "GET");

    WebApplicationMessageException exception = assertThrows(
        WebApplicationMessageException.class,
        () -> underTest.checkAccess(request));

    // Should be BAD_REQUEST with generic message, not NOT_FOUND with user ID
    assertThat(exception.getResponse().getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
  }

  @Test
  void testCheckAccess_publicEndpoint() throws Exception {
    // Status endpoint doesn't require authentication
    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/status", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.isHasAccess(), is(true));
    assertThat(result.getRequiredPermission(), is(equalTo(null)));
  }

  @Test
  void testCheckAccess_relatedEndpoints() throws Exception {
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:users:read"))).thenReturn(true);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.getRelatedEndpoints(), is(notNullValue()));
  }

  private ApiEndpointPermission createEndpointInfo(
      final String method,
      final String path,
      final String permission)
  {
    List<ApiPermissionRequirement> reqs = List.of(new ApiPermissionRequirement(permission, "AND"));
    return new ApiEndpointPermission(method, path, reqs, null, null, true);
  }

  private User createUser(final String userId, final String firstName, final String lastName) {
    User user = new User();
    user.setUserId(userId);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmailAddress(userId + "@example.com");
    user.setSource(UserManager.DEFAULT_SOURCE);
    user.setStatus(UserStatus.active);

    Set<RoleIdentifier> roles = new HashSet<>();
    roles.add(new RoleIdentifier(UserManager.DEFAULT_SOURCE, TEST_ROLE_ID));
    user.setRoles(roles);

    return user;
  }

  private Role createRole(final String roleId, final String name) {
    Role role = mock(Role.class);
    lenient().when(role.getRoleId()).thenReturn(roleId);
    lenient().when(role.getName()).thenReturn(name);
    lenient().when(role.getPrivileges()).thenReturn(Collections.singleton("nx-users-all"));
    lenient().when(role.getRoles()).thenReturn(Collections.emptySet());
    return role;
  }

  private Privilege createPrivilege(final String id, final String name, final String permissionStr) {
    Privilege privilege = new Privilege();
    privilege.setId(id);
    privilege.setName(name);
    privilege.setPermission(new WildcardPermission2(permissionStr));
    return privilege;
  }

  // ===== NEXUS-51956: Drift fix tests =====

  @Test
  void testCheckAccess_sslEndpointResolvesPermission_driftFixed() throws Exception {
    // NEXUS-51956: /security/ssl was missing from static PermissionMappingService catalog.
    // Now that ApiAccessCheckResource uses EndpointPermissionRegistry (runtime-scanned),
    // any endpoint annotated with @RequiresPermissions is resolvable.
    when(endpointPermissionRegistry.getEndpointInfo("/service/rest/v1/security/ssl", "GET"))
        .thenReturn(createEndpointInfo("GET", "/service/rest/v1/security/ssl", "nexus:ssl-truststore:read"));
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:ssl-truststore:read")))
        .thenReturn(true);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/security/ssl", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.getRequiredPermission(), is(equalTo("nexus:ssl-truststore:read")));
    assertThat(result.isHasAccess(), is(true));
  }

  @Test
  void testCheckAccess_systemNodeEndpointResolvesPermission_driftFixed() throws Exception {
    // NEXUS-51956: /system/node was also missing from the static catalog.
    when(endpointPermissionRegistry.getEndpointInfo("/service/rest/v1/system/node", "GET"))
        .thenReturn(createEndpointInfo("GET", "/service/rest/v1/system/node", "nexus:nodes:read"));
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:nodes:read")))
        .thenReturn(false);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/system/node", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.getRequiredPermission(), is(equalTo("nexus:nodes:read")));
    assertThat(result.isHasAccess(), is(false));
  }

  // ===== Registry not-ready guard =====

  @Test
  void testCheckAccess_registryNotReady_returnsServiceUnavailable() {
    when(endpointPermissionRegistry.isReady()).thenReturn(false);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/security/users", "GET");

    WebApplicationMessageException exception = assertThrows(
        WebApplicationMessageException.class,
        () -> underTest.checkAccess(request));

    assertThat(exception.getResponse().getStatus(), is(Status.SERVICE_UNAVAILABLE.getStatusCode()));
  }

  // ===== Security Tests (CRITICAL #1 & #2) =====

  @Test
  void testCheckAccess_nonAdminCannotCheckOtherUser() throws Exception {
    // CRITICAL #1: Non-admin trying to check another user should be forbidden
    when(securityHelper.isAllPermitted()).thenReturn(false);

    // Create a different user to check
    User otherUser = createUser("otheruser", "Other", "User");
    lenient().when(securitySystem.getUser("otheruser")).thenReturn(otherUser);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        "otheruser", null, "/service/rest/v1/security/users", "GET");

    WebApplicationMessageException exception = assertThrows(
        WebApplicationMessageException.class,
        () -> underTest.checkAccess(request));

    assertThat(exception.getResponse().getStatus(), is(Status.FORBIDDEN.getStatusCode()));
  }

  @Test
  void testCheckAccess_nonAdminCanCheckOwnPermissions() throws Exception {
    // CRITICAL #1: Non-admin CAN check their own permissions
    when(securityHelper.isAllPermitted()).thenReturn(false);
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:users:read"))).thenReturn(true);

    // Request for current user (same as TEST_USER_ID)
    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.isHasAccess(), is(true));
  }

  @Test
  void testCheckAccess_nonAdminCannotCheckRole() throws Exception {
    // CRITICAL #1: Non-admin cannot check ANY role permissions
    when(securityHelper.isAllPermitted()).thenReturn(false);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        null, TEST_ROLE_ID, "/service/rest/v1/security/users", "GET");

    WebApplicationMessageException exception = assertThrows(
        WebApplicationMessageException.class,
        () -> underTest.checkAccess(request));

    assertThat(exception.getResponse().getStatus(), is(Status.FORBIDDEN.getStatusCode()));
  }

  @Test
  void testCheckAccess_nonAdminDoesNotSeeChainsOrRelatedEndpoints() throws Exception {
    // CRITICAL #2: Non-admin should not see chains or related endpoints
    when(securityHelper.isAllPermitted()).thenReturn(false);
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:users:read"))).thenReturn(true);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    // Non-admin should only see hasAccess and requiredPermission
    assertThat(result.isHasAccess(), is(true));
    assertThat(result.getRequiredPermission(), is(equalTo("nexus:users:read")));
    assertThat(result.getChains(), is(nullValue()));
    assertThat(result.getRelatedEndpoints(), is(nullValue()));
  }

  @Test
  void testCheckAccess_adminSeesChainsAndRelatedEndpoints() throws Exception {
    // CRITICAL #2: Admin should see full details
    when(securityHelper.isAllPermitted()).thenReturn(true);
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:users:read"))).thenReturn(true);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        TEST_USER_ID, null, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    assertThat(result.isHasAccess(), is(true));
    // Admin should see chains and related endpoints
    assertThat(result.getChains(), is(notNullValue()));
    assertThat(result.getRelatedEndpoints(), is(notNullValue()));
  }

  @Test
  void testCheckAccess_adminCanCheckOtherUser() throws Exception {
    // CRITICAL #1: Admin CAN check other users
    when(securityHelper.isAllPermitted()).thenReturn(true);

    User otherUser = createUser("otheruser", "Other", "User");
    when(securitySystem.getUser("otheruser")).thenReturn(otherUser);
    when(securitySystem.isPermitted(any(SimplePrincipalCollection.class), eq("nexus:users:read"))).thenReturn(false);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        "otheruser", null, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    // Should not throw - admin can check other users
    assertThat(result.isHasAccess(), is(false));
  }

  @Test
  void testCheckAccess_adminCanCheckRole() throws Exception {
    // CRITICAL #1: Admin CAN check roles
    when(securityHelper.isAllPermitted()).thenReturn(true);
    Collection<Permission> permissions = Collections.singleton(new WildcardPermission2("nexus:users:*"));
    when(rolePermissionResolver.resolvePermissionsInRole(TEST_ROLE_ID)).thenReturn(permissions);

    ApiAccessCheckXo request = new ApiAccessCheckXo(
        null, TEST_ROLE_ID, "/service/rest/v1/security/users", "GET");

    ApiAccessResultXo result = underTest.checkAccess(request);

    // Should not throw - admin can check roles
    assertThat(result.isHasAccess(), is(true));
  }
}
