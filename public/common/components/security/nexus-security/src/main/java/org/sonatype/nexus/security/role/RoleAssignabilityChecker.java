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

import org.sonatype.nexus.security.Roles;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Determines whether the current subject is permitted to assign a given role.
 *
 * <p>
 * A non-admin role is assignable if the caller already holds all of the permissions it contains.
 * The {@code nx-admin} role is special-cased: only subjects that already have it may assign it.
 */
@Component
public class RoleAssignabilityChecker
{
  private final RolePermissionResolver rolePermissionResolver;

  private final SecurityHelper securityHelper;

  @Autowired
  public RoleAssignabilityChecker(
      final RolePermissionResolver rolePermissionResolver,
      final SecurityHelper securityHelper)
  {
    this.rolePermissionResolver = checkNotNull(rolePermissionResolver);
    this.securityHelper = checkNotNull(securityHelper);
  }

  public boolean isRoleAssignable(final String roleId) {
    Subject subject = securityHelper.subject();
    if (Roles.ADMIN_ROLE_ID.equals(roleId)) {
      return subject.hasRole(Roles.ADMIN_ROLE_ID);
    }
    return rolePermissionResolver.resolvePermissionsInRole(roleId).stream().allMatch(subject::isPermitted);
  }
}
