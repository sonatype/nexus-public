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

import type { EndpointAccessDot } from './endpointAccess';
import type { MergedApiEndpoint } from './mergeSwaggerPermissions';
import {
  buildPrivilegeByIdMap,
  buildRoleByIdMap,
  roleSatisfiesRequiredPermissions,
} from './rolePrivilegeCoverage';

function endpointRowKey(row: MergedApiEndpoint): string {
  return `${row.httpMethod}|${row.fullPath}`;
}

export function computeRoleLensAccessById(
  endpoints: MergedApiEndpoint[],
  roleId: string,
  allRoles: Role[],
  privileges: Privilege[]
): Record<string, EndpointAccessDot> {
  const roleById = buildRoleByIdMap(allRoles);
  const privilegeById = buildPrivilegeByIdMap(privileges);
  const out: Record<string, EndpointAccessDot> = {};
  for (const row of endpoints) {
    const id = endpointRowKey(row);
    const reqs = row.permission?.permissions?.map((p) => p.permission).filter(Boolean) ?? [];
    if (reqs.length === 0) {
      out[id] = row.permission?.authenticated === false ? 'granted' : 'unknown';
      continue;
    }
    const mode = row.permission?.permissions?.[0]?.logical === 'OR' ? 'OR' : 'AND';
    const ok = roleSatisfiesRequiredPermissions(roleId, roleById, privilegeById, reqs, mode);
    if (ok) {
      out[id] = 'granted';
    } else {
      const partial =
        mode === 'AND' &&
        reqs.some((req) =>
          roleSatisfiesRequiredPermissions(roleId, roleById, privilegeById, [req], 'AND')
        );
      out[id] = partial ? 'partial' : 'denied';
    }
  }
  return out;
}
