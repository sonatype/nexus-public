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

import java.util.Collection;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;

/**
 * A {@link RolePermissionResolver} that can resolve an entire set of role IDs in one batch, instead of one call per
 * role. Lets callers (notably {@link PermissionCachingAuthorizingRealm#getPermissions}) collapse O(roles) resolver
 * round-trips into O(depth) batch calls without depending on a concrete resolver implementation (NEXUS-52583).
 */
public interface BatchRolePermissionResolver
    extends RolePermissionResolver
{
  /**
   * Resolves the union of permissions granted by the given role IDs (transitively through nested roles).
   *
   * @param roleIds the role IDs to expand; may be empty
   * @return the resolved permissions; never {@code null}
   */
  Collection<Permission> resolvePermissionsForRoles(Collection<String> roleIds);
}
