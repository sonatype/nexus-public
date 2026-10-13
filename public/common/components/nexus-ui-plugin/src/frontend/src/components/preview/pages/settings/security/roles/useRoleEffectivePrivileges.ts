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

import { useMemo } from 'react';
import { useResourceMachine, type ResourceError } from '../../../../../../interface/resource';
import { useRolesApi } from './useRolesApi';
import { usePrivilegesApi } from '../privileges/usePrivilegesApi';
import type { Privilege } from '../privileges/types';
import type { Role } from './types';

// Walks the role graph from a snapshot and returns effective privileges with
// granted-by attribution.

export interface EffectivePrivilege extends Privilege {
  grantedByRoleIds: string[];
}

export interface RoleSnapshot {
  id: string;
  privileges: string[];
  roles: string[];
}

interface SecurityDataPayload {
  roles: Role[];
  privileges: Privilege[];
}

export interface UseRoleEffectivePrivilegesResult {
  privileges: EffectivePrivilege[];
  roleMap: Map<string, Role>;
  loading: boolean;
  error: string | null;
  errorDetails: ResourceError | null;
}

function calculateEffectivePrivilegesForRole(
  roleId: string,
  roleMap: Map<string, Role>,
  privilegeMap: Map<string, Privilege>,
  visited: Set<string>,
): Array<{ privilege: Privilege; grantedByRoleId: string }> {
  if (visited.has(roleId)) return [];
  const role = roleMap.get(roleId);
  if (!role) return [];

  const newVisited = new Set(visited);
  newVisited.add(roleId);

  const result: Array<{ privilege: Privilege; grantedByRoleId: string }> = [];

  (role.privileges || []).forEach((pId) => {
    const privilege = privilegeMap.get(pId);
    if (privilege) {
      result.push({ privilege, grantedByRoleId: roleId });
    }
  });

  (role.roles || []).forEach((nestedRoleId) => {
    result.push(
      ...calculateEffectivePrivilegesForRole(
        nestedRoleId,
        roleMap,
        privilegeMap,
        newVisited,
      ),
    );
  });

  return result;
}

// Consumers pass either the committed or pending snapshot for live recompute.
export function useRoleEffectivePrivileges(
  snapshot: RoleSnapshot | null,
): UseRoleEffectivePrivilegesResult {
  const { fetchRoles } = useRolesApi();
  const { fetchPrivileges } = usePrivilegesApi();

  const { data, loading, loaded, error, errorMessage } = useResourceMachine<SecurityDataPayload>({
    id: 'role-effective-privileges',
    initialData: { roles: [], privileges: [] },
    fetch: () =>
      Promise.all([fetchRoles(), fetchPrivileges()]).then(([roles, privResponse]) => ({
        roles,
        privileges: privResponse.data,
      })),
  });

  const allRoles = data.roles;
  const allPrivileges = data.privileges;

  const roleMap = useMemo(() => new Map(allRoles.map((r) => [r.id, r])), [allRoles]);
  const privilegeMap = useMemo(
    () => new Map(allPrivileges.map((p) => [p.id, p])),
    [allPrivileges],
  );

  // Seed roleMap with the snapshot shape so pending edits recompute live.
  const effectiveRoleMap = useMemo(() => {
    if (!snapshot) return roleMap;
    const cloned = new Map(roleMap);
    const existing = roleMap.get(snapshot.id);
    cloned.set(snapshot.id, {
      id: snapshot.id,
      name: existing?.name ?? snapshot.id,
      version: existing?.version ?? '1',
      source: existing?.source ?? '',
      description: existing?.description ?? '',
      readOnly: existing?.readOnly ?? false,
      privileges: snapshot.privileges,
      roles: snapshot.roles,
    });
    return cloned;
  }, [roleMap, snapshot]);

  const privileges = useMemo(() => {
    if (!loaded || !snapshot) return [];

    const seen = new Map<string, EffectivePrivilege>();

    const items = calculateEffectivePrivilegesForRole(
      snapshot.id,
      effectiveRoleMap,
      privilegeMap,
      new Set(),
    );
    items.forEach(({ privilege, grantedByRoleId }) => {
      const existing = seen.get(privilege.id);
      if (existing) {
        if (!existing.grantedByRoleIds.includes(grantedByRoleId)) {
          existing.grantedByRoleIds.push(grantedByRoleId);
        }
      } else {
        seen.set(privilege.id, {
          ...privilege,
          grantedByRoleIds: [grantedByRoleId],
        });
      }
    });

    return Array.from(seen.values());
  }, [loaded, snapshot, effectiveRoleMap, privilegeMap]);

  return {
    privileges,
    roleMap: effectiveRoleMap,
    loading,
    error: errorMessage,
    errorDetails: error,
  };
}
