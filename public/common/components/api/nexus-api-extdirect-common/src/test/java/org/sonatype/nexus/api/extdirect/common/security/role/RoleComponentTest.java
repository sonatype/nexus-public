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
package org.sonatype.nexus.api.extdirect.common.security.role;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.api.extdirect.common.security.role.model.RoleXO;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.PrivilegesExistValidator;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleAssignabilityChecker;
import org.sonatype.nexus.security.role.RoleNotContainSelfValidator;
import org.sonatype.nexus.security.role.RolesExistValidator;
import org.sonatype.nexus.security.role.UniqueRoleIdValidator;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.RegisterValidator;

import jakarta.validation.ConstraintValidator;
import org.apache.shiro.authz.AuthorizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * Grant-authorization coverage for the ExtDirect role editor path (NEXUS-53397 / SVH-280).
 *
 * <p>
 * The REST and ExtDirect paths share {@link RoleAssignabilityChecker} for the decision itself, which is unit-tested
 * directly. What these tests pin down is the wiring: that both mutation methods consult the checker with the correct
 * pre-change snapshot, refuse to persist when it reports an ungrantable addition, and name the offending ids.
 */
@ExtendWith({MockitoExtension.class, ValidationExtension.class, AuthenticationExtension.class})
@WithUser(permissions = {"nexus:roles:create", "nexus:roles:update"})
class RoleComponentTest
{
  private static final String EXISTING_ROLE_ID = "custom-role";

  private static final String NEW_ROLE_ID = "new-role";

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AuthorizationManager authorizationManager;

  @Mock
  private RoleAssignabilityChecker roleAssignabilityChecker;

  private RoleComponent underTest;

  @BeforeEach
  void setup() {
    underTest = new RoleComponent(securitySystem, List.of(authorizationManager), roleAssignabilityChecker);
  }

  @Test
  void update_rejectedWhenTheCallerCannotGrantAnAddedChildRole() {
    when(authorizationManager.getRole(EXISTING_ROLE_ID)).thenReturn(role(EXISTING_ROLE_ID, Set.of("nx-viewer")));
    when(roleAssignabilityChecker.findUngrantableAdditions(any(), any())).thenReturn(Set.of("nx-admin"));

    RoleXO roleXO = roleXO(EXISTING_ROLE_ID, Set.of());

    AuthorizationException e = assertThrows(AuthorizationException.class, () -> underTest.update(roleXO));

    assertThat(e.getMessage(), containsString("nx-admin"));
    verify(authorizationManager, never()).updateRole(any());
  }

  @Test
  void update_rejectedWhenTheCallerCannotGrantAnAddedPrivilege() {
    when(authorizationManager.getRole(EXISTING_ROLE_ID)).thenReturn(role(EXISTING_ROLE_ID, Set.of()));
    when(roleAssignabilityChecker.findUngrantableAdditions(any(), any())).thenReturn(Set.of("nx-all"));

    RoleXO roleXO = roleXO(EXISTING_ROLE_ID, Set.of());

    AuthorizationException e = assertThrows(AuthorizationException.class, () -> underTest.update(roleXO));

    assertThat(e.getMessage(), containsString("nx-all"));
    verify(authorizationManager, never()).updateRole(any());
  }

  @Test
  void update_reportsEveryUngrantableAddition() {
    when(authorizationManager.getRole(EXISTING_ROLE_ID)).thenReturn(role(EXISTING_ROLE_ID, Set.of()));
    when(roleAssignabilityChecker.findUngrantableAdditions(any(), any())).thenReturn(Set.of("nx-admin", "nx-all"));

    AuthorizationException e =
        assertThrows(AuthorizationException.class, () -> underTest.update(roleXO(EXISTING_ROLE_ID, Set.of())));

    assertThat(e.getMessage(), containsString("nx-admin"));
    assertThat(e.getMessage(), containsString("nx-all"));
  }

  /**
   * The snapshot handed to the checker must be the role as it exists now, so that only genuine additions count as new
   * grants. Unlike the REST path, this method previously read no prior state at all.
   */
  @Test
  void update_checksAdditionsAgainstThePreUpdateSnapshot() throws Exception {
    when(authorizationManager.getRole(EXISTING_ROLE_ID))
        .thenReturn(role(EXISTING_ROLE_ID, Set.of("nx-viewer"), Set.of("nx-users-read")));
    when(roleAssignabilityChecker.findUngrantableAdditions(any(), any())).thenReturn(Collections.emptySet());
    when(authorizationManager.updateRole(any())).thenReturn(role(EXISTING_ROLE_ID, Set.of()));

    underTest.update(roleXO(EXISTING_ROLE_ID, Set.of("nx-anonymous")));

    ArgumentCaptor<Role> desired = ArgumentCaptor.forClass(Role.class);
    ArgumentCaptor<Role> snapshot = ArgumentCaptor.forClass(Role.class);
    verify(roleAssignabilityChecker).findUngrantableAdditions(desired.capture(), snapshot.capture());
    assertThat(desired.getValue().getRoles(), containsInAnyOrder("nx-anonymous"));
    assertThat(snapshot.getValue().getRoles(), containsInAnyOrder("nx-viewer"));
    assertThat(snapshot.getValue().getPrivileges(), containsInAnyOrder("nx-users-read"));
  }

  @Test
  void update_permittedWhenTheCallerHoldsEverythingBeingGranted() throws Exception {
    when(authorizationManager.getRole(EXISTING_ROLE_ID)).thenReturn(role(EXISTING_ROLE_ID, Set.of()));
    when(roleAssignabilityChecker.findUngrantableAdditions(any(), any())).thenReturn(Collections.emptySet());
    when(authorizationManager.updateRole(any())).thenReturn(role(EXISTING_ROLE_ID, Set.of("nx-anonymous")));

    underTest.update(roleXO(EXISTING_ROLE_ID, Set.of("nx-anonymous")));

    // the checker must be consulted even on the happy path, or deleting the check would go undetected
    verify(roleAssignabilityChecker).findUngrantableAdditions(any(), any());
    verify(authorizationManager).updateRole(any());
  }

  @Test
  void create_rejectedWhenTheCallerCannotGrantARequestedAuthority() {
    when(roleAssignabilityChecker.findUngrantableAdditions(any(), any())).thenReturn(Set.of("nx-admin"));

    RoleXO roleXO = roleXO(NEW_ROLE_ID, Set.of());

    AuthorizationException e = assertThrows(AuthorizationException.class, () -> underTest.create(roleXO));

    assertThat(e.getMessage(), containsString("nx-admin"));
    verify(authorizationManager, never()).addRole(any());
  }

  /**
   * A new role has no prior state, so the checker must be told there is no snapshot rather than being handed an empty
   * role, which would silently treat every requested grant as pre-existing.
   */
  @Test
  void create_passesNoSnapshotToTheChecker() throws Exception {
    when(roleAssignabilityChecker.findUngrantableAdditions(any(), any())).thenReturn(Collections.emptySet());
    when(authorizationManager.addRole(any())).thenReturn(role(NEW_ROLE_ID, Set.of()));

    underTest.create(roleXO(NEW_ROLE_ID, Set.of()));

    verify(roleAssignabilityChecker).findUngrantableAdditions(any(), isNull());
  }

  @Test
  void create_permittedWhenTheCallerHoldsEverythingBeingGranted() throws Exception {
    when(roleAssignabilityChecker.findUngrantableAdditions(any(), any())).thenReturn(Collections.emptySet());
    when(authorizationManager.addRole(any())).thenReturn(role(NEW_ROLE_ID, Set.of("nx-anonymous")));

    underTest.create(roleXO(NEW_ROLE_ID, Set.of("nx-anonymous")));

    // the checker must be consulted even on the happy path, or deleting the check would go undetected
    verify(roleAssignabilityChecker).findUngrantableAdditions(any(), any());
    verify(authorizationManager).addRole(any());
  }

  // The DTO parameters are validated by woven bean-validation, so the constraint validators must be supplied real
  // instances. They are incidental to what these tests assert; the stubs below only let validation pass.

  @RegisterValidator
  private ConstraintValidator<?, ?> rolesExistValidator() throws Exception {
    return new RolesExistValidator(validationSupport());
  }

  @RegisterValidator
  private ConstraintValidator<?, ?> uniqueRoleIdValidator() throws Exception {
    return new UniqueRoleIdValidator(validationSupport());
  }

  @RegisterValidator
  private ConstraintValidator<?, ?> roleNotContainSelfValidator() throws Exception {
    return new RoleNotContainSelfValidator(validationSupport());
  }

  @RegisterValidator
  private ConstraintValidator<?, ?> privilegesExistValidator() throws Exception {
    return new PrivilegesExistValidator(validationSupport());
  }

  /**
   * Stubs the minimum needed for the woven constraint validators to pass, then hands back the mocked security system
   * they resolve their collaborators from. Idempotent: each registered validator calls it.
   */
  private SecuritySystem validationSupport() throws Exception {
    lenient().doReturn(authorizationManager).when(securitySystem).getAuthorizationManager(DEFAULT_SOURCE);
    lenient().doReturn(Collections.emptySet()).when(securitySystem).listPrivileges();
    // child roles named by the test DTOs must resolve, and must not contain the role under edit
    lenient().doReturn(role("nx-anonymous", Set.of())).when(authorizationManager).getRole("nx-anonymous");
    // a created role must not already exist, or @UniqueRoleId rejects it. doThrow, not thenThrow: the latter would fire
    // the stubbed throw while stubbing.
    lenient().doThrow(new NoSuchRoleException(NEW_ROLE_ID)).when(authorizationManager).getRole(NEW_ROLE_ID);
    return securitySystem;
  }

  private static Role role(final String id, final Set<String> roles) {
    return role(id, roles, Set.of());
  }

  private static Role role(final String id, final Set<String> roles, final Set<String> privileges) {
    Role role = new Role();
    role.setRoleId(id);
    role.setName(id);
    role.setDescription("description");
    role.setSource(DEFAULT_SOURCE);
    role.setReadOnly(false);
    role.setVersion(1);
    role.setRoles(roles);
    role.setPrivileges(privileges);
    return role;
  }

  private static RoleXO roleXO(final String id, final Set<String> roles) {
    RoleXO roleXO = new RoleXO();
    roleXO.setId(id);
    roleXO.setName(id);
    roleXO.setDescription("description");
    roleXO.setSource(DEFAULT_SOURCE);
    roleXO.setVersion("1");
    roleXO.setRoles(roles);
    roleXO.setPrivileges(Set.of());
    return roleXO;
  }
}
