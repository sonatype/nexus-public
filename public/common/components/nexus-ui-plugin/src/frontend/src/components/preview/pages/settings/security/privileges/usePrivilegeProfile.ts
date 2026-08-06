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

import { useState, useEffect, } from 'react';
import { useRolesApi } from '../roles/useRolesApi';
import { useUsersApi } from '../users/useUsersApi';
import { usePrivilegesApi } from './usePrivilegesApi';
import { Role } from '../roles/types';
import { User } from '../users/types';
import { Privilege } from './types';

/**
 * Get effective privilege IDs for a role (including from nested roles).
 * Handles circular references by tracking visited roles.
 */
function getEffectivePrivilegeIds(
  role: Role,
  roleMap: Map<string, Role>,
  visited: Set<string> = new Set()
): Set<string> {
  if (visited.has(role.id)) return new Set();
  visited.add(role.id);
  const ids = new Set<string>(role.privileges || []);
  for (const childId of role.roles || []) {
    const child = roleMap.get(childId);
    if (child) {
      const childIds = getEffectivePrivilegeIds(child, roleMap, visited);
      childIds.forEach((id) => ids.add(id));
    }
  }
  return ids;
}

export interface UsePrivilegeProfileResult {
  privilege: Privilege | null;
  rolesUsing: Role[];
  usersWithAccess: User[];
  loading: boolean;
  error: string | null;
}

/**
 * Hook to load privilege profile data: privilege details, roles that grant it,
 * and users who have access via those roles.
 */
export function usePrivilegeProfile(privilegeId: string | null): UsePrivilegeProfileResult {
  const [privilege, setPrivilege] = useState<Privilege | null>(null);
  const [rolesUsing, setRolesUsing] = useState<Role[]>([]);
  const [usersWithAccess, setUsersWithAccess] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { findPrivilege } = usePrivilegesApi();
  const { fetchRoles } = useRolesApi();
  const { fetchUsers } = useUsersApi();

  useEffect(() => {
    if (!privilegeId) {
      setPrivilege(null);
      setRolesUsing([]);
      setUsersWithAccess([]);
      setLoading(false);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    const load = async () => {
      try {
        const [privData, allRoles, allUsers] = await Promise.all([
          findPrivilege(privilegeId),
          fetchRoles(),
          fetchUsers(),
        ]);

        if (cancelled) return;

        if (!privData) {
          setError('Privilege not found');
          setPrivilege(null);
          setRolesUsing([]);
          setUsersWithAccess([]);
          return;
        }

        setPrivilege(privData);

        const roleMapForCompute = new Map(allRoles.map((r) => [r.id, r]));
        const rolesThatGrant: Role[] = [];
        for (const role of allRoles) {
          const effectiveIds = getEffectivePrivilegeIds(role, roleMapForCompute);
          if (effectiveIds.has(privilegeId)) {
            rolesThatGrant.push(role);
          }
        }
        setRolesUsing(rolesThatGrant);

        const roleIdsSet = new Set(rolesThatGrant.map((r) => r.id));
        const usersWithRole = allUsers.filter((u) =>
          (u.roles || []).some((rId) => roleIdsSet.has(rId))
        );
        setUsersWithAccess(usersWithRole);
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load privilege profile');
          setPrivilege(null);
          setRolesUsing([]);
          setUsersWithAccess([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => {
      cancelled = true;
    };
  }, [privilegeId, findPrivilege, fetchRoles, fetchUsers]);

  return {
    privilege,
    rolesUsing,
    usersWithAccess,
    loading,
    error,
  };
}
