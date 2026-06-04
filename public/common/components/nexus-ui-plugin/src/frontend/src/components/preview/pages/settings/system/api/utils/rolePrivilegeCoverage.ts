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

import type { Privilege } from '../../../security/privileges/types';
import type { Role } from '../../../security/roles/types';

import { wildcardImplies } from './shiroWildcardImplies';

/**
 * All privilege identifiers attached to a role, including nested roles (cycle-safe).
 */
export function collectPrivilegeIdsForRole(roleId: string, roleById: Map<string, Role>): Set<string> {
  const out = new Set<string>();
  const visited = new Set<string>();

  const walk = (rid: string) => {
    if (visited.has(rid)) {
      return;
    }
    visited.add(rid);
    const role = roleById.get(rid);
    if (!role) {
      return;
    }
    for (const p of role.privileges || []) {
      out.add(p);
    }
    for (const nested of role.roles || []) {
      walk(nested);
    }
  };

  walk(roleId);
  return out;
}

export function privilegeSetImpliesPermission(
  privilegeIds: Set<string>,
  privilegeById: Map<string, Privilege>,
  requiredPermission: string
): boolean {
  const req = (requiredPermission || '').toLowerCase();
  if (!req) {
    return false;
  }
  for (const pid of privilegeIds) {
    const priv = privilegeById.get(pid);
    if (!priv?.permission) {
      continue;
    }
    if (wildcardImplies(priv.permission.toLowerCase(), req)) {
      return true;
    }
  }
  return false;
}

/**
 * Whether a role's effective privileges imply the endpoint's required permissions (AND/OR).
 */
export function roleSatisfiesRequiredPermissions(
  roleId: string,
  roleById: Map<string, Role>,
  privilegeById: Map<string, Privilege>,
  requiredPermissions: string[],
  mode: 'AND' | 'OR'
): boolean {
  if (requiredPermissions.length === 0) {
    return false;
  }
  const privIds = collectPrivilegeIdsForRole(roleId, roleById);
  const check = (req: string) => privilegeSetImpliesPermission(privIds, privilegeById, req);
  if (mode === 'OR') {
    return requiredPermissions.some(check);
  }
  return requiredPermissions.every(check);
}

export function buildPrivilegeByIdMap(privileges: Privilege[]): Map<string, Privilege> {
  const m = new Map<string, Privilege>();
  for (const p of privileges) {
    m.set(p.id, p);
    if (p.name && p.name !== p.id) {
      m.set(p.name, p);
    }
  }
  return m;
}

export function buildRoleByIdMap(roles: Role[]): Map<string, Role> {
  const m = new Map<string, Role>();
  for (const r of roles) {
    m.set(r.id, r);
  }
  return m;
}
