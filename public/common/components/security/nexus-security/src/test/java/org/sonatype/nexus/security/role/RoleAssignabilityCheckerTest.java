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
package org.sonatype.nexus.security.role;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.security.Roles;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignabilityCheckerTest
{
  @Mock
  private RolePermissionResolver rolePermissionResolver;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private Subject subject;

  @Mock
  private SecurityConfigurationManager configurationManager;

  @Mock
  @SuppressWarnings("rawtypes")
  private PrivilegeDescriptor privilegeDescriptor;

  @Mock
  private CPrivilege cPrivilege;

  private RoleAssignabilityChecker underTest;

  @BeforeEach
  void setup() {
    // lenient: the checker short-circuits without consulting the subject when there is nothing being granted
    lenient().when(securityHelper.subject()).thenReturn(subject);
    underTest = new RoleAssignabilityChecker(
        rolePermissionResolver, securityHelper, configurationManager, List.of(privilegeDescriptor));
  }

  @Test
  void adminRole_assignableToAdminSubject() {
    when(subject.hasRole(Roles.ADMIN_ROLE_ID)).thenReturn(true);

    assertThat(underTest.isRoleAssignable(Roles.ADMIN_ROLE_ID), is(true));
  }

  @Test
  void adminRole_notAssignableToNonAdminSubject() {
    when(subject.hasRole(Roles.ADMIN_ROLE_ID)).thenReturn(false);

    assertThat(underTest.isRoleAssignable(Roles.ADMIN_ROLE_ID), is(false));
  }

  @Test
  void nonAdminRole_assignableWhenSubjectHoldsAllPermissions() {
    Permission perm = new WildcardPermission("nexus:users:read");
    when(rolePermissionResolver.resolvePermissionsInRole("nx-viewer")).thenReturn(Collections.singletonList(perm));
    when(subject.isPermitted(perm)).thenReturn(true);

    assertThat(underTest.isRoleAssignable("nx-viewer"), is(true));
  }

  @Test
  void nonAdminRole_notAssignableWhenSubjectLacksAPermission() {
    Permission allowed = new WildcardPermission("nexus:users:read");
    Permission denied = new WildcardPermission("nexus:users:create");
    when(rolePermissionResolver.resolvePermissionsInRole("nx-editor"))
        .thenReturn(Arrays.asList(allowed, denied));
    when(subject.isPermitted(allowed)).thenReturn(true);
    when(subject.isPermitted(denied)).thenReturn(false);

    assertThat(underTest.isRoleAssignable("nx-editor"), is(false));
  }

  @Test
  void roleWithNoPermissions_alwaysAssignable() {
    when(rolePermissionResolver.resolvePermissionsInRole("empty-role")).thenReturn(Collections.emptyList());

    assertThat(underTest.isRoleAssignable("empty-role"), is(true));
  }

  @Test
  void privilege_assignableWhenSubjectHoldsThePermissionItConfers() {
    Permission perm = new WildcardPermission("nexus:users:read");
    stubPrivilege("nx-users-read", "application", perm);
    when(subject.isPermitted(perm)).thenReturn(true);

    assertThat(underTest.isPrivilegeAssignable("nx-users-read"), is(true));
  }

  @Test
  void privilege_notAssignableWhenSubjectLacksThePermissionItConfers() {
    Permission perm = new WildcardPermission("nexus:*");
    stubPrivilege("nx-all", "wildcard", perm);
    when(subject.isPermitted(perm)).thenReturn(false);

    assertThat(underTest.isPrivilegeAssignable("nx-all"), is(false));
  }

  /**
   * Fail closed: an unrecognised privilege type means we cannot determine what authority it confers, so it must not be
   * grantable. Returning true here would let an attacker bypass the check with a crafted type.
   */
  @Test
  void privilege_notAssignableWhenNoDescriptorMatchesItsType() {
    when(configurationManager.readPrivilege("odd-privilege")).thenReturn(cPrivilege);
    when(cPrivilege.getType()).thenReturn("no-such-type");
    when(privilegeDescriptor.getType()).thenReturn("application");

    assertThat(underTest.isPrivilegeAssignable("odd-privilege"), is(false));
  }

  /**
   * Fail closed: a descriptor that cannot produce a permission tells us nothing about the authority conferred.
   */
  @Test
  void privilege_notAssignableWhenDescriptorProducesNoPermission() {
    stubPrivilege("broken-privilege", "application", null);

    assertThat(underTest.isPrivilegeAssignable("broken-privilege"), is(false));
  }

  /**
   * A privilege that does not exist confers no authority, so it is not a grant to reject. Existing downstream
   * validation already owns the nonexistent-privilege case, and this check must not change that behaviour.
   */
  @Test
  void privilege_nonexistentConfersNoAuthorityAndIsNotRejected() {
    when(configurationManager.readPrivilege("ghost")).thenThrow(new NoSuchPrivilegeException("ghost"));

    assertThat(underTest.isPrivilegeAssignable("ghost"), is(true));
  }

  /**
   * Mirror of {@link #privilege_nonexistentConfersNoAuthorityAndIsNotRejected}: a nonexistent role confers no
   * authority either. resolvePermissionsInRole catches NoSuchRoleException and returns an empty set, so allMatch is
   * vacuously true. Safe for the same reason -- the downstream create/update validation rejects a dangling child-role
   * id rather than stripping it. Kept as its own case so a future reader cannot "fix" one half into a hard failure
   * while leaving the other passing silently.
   */
  @Test
  void role_nonexistentConfersNoAuthorityAndIsNotRejected() {
    when(rolePermissionResolver.resolvePermissionsInRole("ghost-role")).thenReturn(Collections.emptyList());

    assertThat(underTest.isRoleAssignable("ghost-role"), is(true));
  }

  /**
   * An administrator holds nexus:* and may grant any privilege, so the descriptor is never consulted. Without the
   * admin short-circuit, a privilege whose descriptor is absent (a feature-flagged or unloaded plugin) would fail
   * closed and hand the administrator a hard denial on any role edit that touches it (NEXUS-53397 review).
   */
  @Test
  void privilege_assignableToAdminEvenWhenItsPermissionCannotBeResolved() {
    when(subject.hasRole(Roles.ADMIN_ROLE_ID)).thenReturn(true);

    assertThat(underTest.isPrivilegeAssignable("privilege-from-unloaded-plugin"), is(true));
  }

  @Test
  void ungrantableAdditions_emptyWhenNothingIsBeingAdded() {
    Role current = role(Set.of("nx-viewer"), Set.of("nx-users-read"));
    Role desired = role(Set.of("nx-viewer"), Set.of("nx-users-read"));

    assertThat(underTest.findUngrantableAdditions(desired, current), is(Collections.emptySet()));
  }

  @Test
  void ungrantableAdditions_ignoresRemovals() {
    Role current = role(Set.of("nx-viewer", "nx-admin"), Set.of("nx-all"));
    Role desired = role(Set.of("nx-viewer"), Set.of());

    assertThat(underTest.findUngrantableAdditions(desired, current), is(Collections.emptySet()));
  }

  @Test
  void ungrantableAdditions_rejectsAddedAdminRoleForNonAdminCaller() {
    when(subject.hasRole(Roles.ADMIN_ROLE_ID)).thenReturn(false);
    Role current = role(Set.of("nx-viewer"), Set.of());
    Role desired = role(Set.of("nx-viewer", Roles.ADMIN_ROLE_ID), Set.of());

    assertThat(underTest.findUngrantableAdditions(desired, current), is(Set.of(Roles.ADMIN_ROLE_ID)));
  }

  @Test
  void ungrantableAdditions_allowsAddedAdminRoleForAdminCaller() {
    when(subject.hasRole(Roles.ADMIN_ROLE_ID)).thenReturn(true);
    Role current = role(Set.of(), Set.of());
    Role desired = role(Set.of(Roles.ADMIN_ROLE_ID), Set.of());

    assertThat(underTest.findUngrantableAdditions(desired, current), is(Collections.emptySet()));
  }

  @Test
  void ungrantableAdditions_rejectsAddedPrivilegeTheCallerDoesNotHold() {
    Permission perm = new WildcardPermission("nexus:*");
    CPrivilege privilege = batchPrivilege("nx-all", "wildcard", perm);
    when(configurationManager.readPrivileges(Set.of("nx-all"))).thenReturn(List.of(privilege));
    when(subject.isPermitted(perm)).thenReturn(false);
    Role current = role(Set.of(), Set.of());
    Role desired = role(Set.of(), Set.of("nx-all"));

    assertThat(underTest.findUngrantableAdditions(desired, current), is(Set.of("nx-all")));
  }

  @Test
  void ungrantableAdditions_onCreateEveryRequestedGrantIsChecked() {
    Permission perm = new WildcardPermission("nexus:repository-admin:*:*:*");
    CPrivilege privilege = batchPrivilege("nx-repository-admin-all", "application", perm);
    when(configurationManager.readPrivileges(Set.of("nx-repository-admin-all"))).thenReturn(List.of(privilege));
    when(subject.isPermitted(perm)).thenReturn(false);
    Role desired = role(Set.of(), Set.of("nx-repository-admin-all"));

    assertThat(underTest.findUngrantableAdditions(desired, null), is(Set.of("nx-repository-admin-all")));
  }

  @Test
  void ungrantableAdditions_allowsAddedPrivilegeForAdminCaller() {
    when(subject.hasRole(Roles.ADMIN_ROLE_ID)).thenReturn(true);
    Role current = role(Set.of(), Set.of());
    Role desired = role(Set.of(), Set.of("nx-all"));

    // admin short-circuits before any descriptor resolution, so no privilege read is stubbed
    assertThat(underTest.findUngrantableAdditions(desired, current), is(Collections.emptySet()));
  }

  /**
   * Nested containment: a custom role that transitively contains nx-admin. RolePermissionResolverImpl flattens the
   * hierarchy (BFS with a visited set), so by the time the checker sees the role it sees nx-admin's permissions, not
   * the literal id "nx-admin". Stubbing that flattening here proves the rejection is driven by the resolved
   * permissions, not the ADMIN_ROLE_ID fast path -- deleting that fast path must not make this pass.
   */
  @Test
  void ungrantableAdditions_rejectsAddedRoleThatTransitivelyContainsAdmin() {
    Permission adminPermission = new WildcardPermission("nexus:*");
    when(rolePermissionResolver.resolvePermissionsInRole("looks-innocent"))
        .thenReturn(Collections.singletonList(adminPermission));
    when(subject.isPermitted(adminPermission)).thenReturn(false);
    Role current = role(Set.of(), Set.of());
    Role desired = role(Set.of("looks-innocent"), Set.of());

    assertThat(underTest.findUngrantableAdditions(desired, current), is(Set.of("looks-innocent")));
  }

  /**
   * Real-logic proof that the two stream passes are unioned into one result: an ungrantable child role and an
   * ungrantable privilege in the same call must both surface. The call sites only ever see this against a mocked
   * checker, so the union is asserted here against the real implementation.
   */
  @Test
  void ungrantableAdditions_unionsUngrantableRolesAndPrivileges() {
    Permission rolePermission = new WildcardPermission("nexus:users:create");
    when(rolePermissionResolver.resolvePermissionsInRole("privileged-role"))
        .thenReturn(Collections.singletonList(rolePermission));
    when(subject.isPermitted(rolePermission)).thenReturn(false);

    Permission privilegePermission = new WildcardPermission("nexus:*");
    CPrivilege privilege = batchPrivilege("nx-all", "wildcard", privilegePermission);
    when(configurationManager.readPrivileges(Set.of("nx-all"))).thenReturn(List.of(privilege));
    when(subject.isPermitted(privilegePermission)).thenReturn(false);

    Role current = role(Set.of(), Set.of());
    Role desired = role(Set.of("privileged-role"), Set.of("nx-all"));

    assertThat(underTest.findUngrantableAdditions(desired, current), is(Set.of("privileged-role", "nx-all")));
  }

  @Test
  void ungrantableAdditions_toleratesNullGrantCollections() {
    Role desired = new Role();

    assertThat(underTest.findUngrantableAdditions(desired, null), is(Collections.emptySet()));
  }

  private static Role role(final Set<String> roles, final Set<String> privileges) {
    Role role = new Role();
    role.setRoles(new HashSet<>(roles));
    role.setPrivileges(new HashSet<>(privileges));
    return role;
  }

  @SuppressWarnings("unchecked")
  private void stubPrivilege(final String id, final String type, final Permission permission) {
    when(configurationManager.readPrivilege(id)).thenReturn(cPrivilege);
    when(cPrivilege.getType()).thenReturn(type);
    when(privilegeDescriptor.getType()).thenReturn(type);
    when(privilegeDescriptor.createPermission(cPrivilege)).thenReturn(permission);
  }

  /**
   * Builds a distinct {@link CPrivilege} mock for the batch path exercised by {@code findUngrantableAdditions}, which
   * resolves added privileges through {@code readPrivileges(Set)} rather than the per-id {@code readPrivilege}. Uses a
   * fresh mock per call so a single test can carry more than one privilege.
   */
  @SuppressWarnings("unchecked")
  private CPrivilege batchPrivilege(final String id, final String type, final Permission permission) {
    CPrivilege privilege = mock(CPrivilege.class);
    lenient().when(privilege.getId()).thenReturn(id);
    lenient().when(privilege.getType()).thenReturn(type);
    lenient().when(privilegeDescriptor.getType()).thenReturn(type);
    lenient().when(privilegeDescriptor.createPermission(privilege)).thenReturn(permission);
    return privilege;
  }
}
