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

import org.sonatype.nexus.security.Roles;
import org.sonatype.nexus.security.SecurityHelper;

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

  private RoleAssignabilityChecker underTest;

  @BeforeEach
  void setup() {
    when(securityHelper.subject()).thenReturn(subject);
    underTest = new RoleAssignabilityChecker(rolePermissionResolver, securityHelper);
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
}
