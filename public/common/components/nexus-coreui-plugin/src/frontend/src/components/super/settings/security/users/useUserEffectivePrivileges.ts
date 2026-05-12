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

import { useState, useCallback, useMemo, useEffect } from 'react';
import { useRolesApi } from '../roles/useRolesApi';
import { usePrivilegesApi } from '../privileges/usePrivilegesApi';
import { User } from './types';
import { Privilege } from '../privileges/types';
import { Role } from '../roles/types';

/**
 * Effective privilege with granted-by role information for User Profile Privileges tab
 */
export interface EffectivePrivilege extends Privilege {
  grantedByRoleIds: string[];
}

export interface UseUserEffectivePrivilegesResult {
  privileges: EffectivePrivilege[];
  roleMap: Map<string, Role>;
  loading: boolean;
  error: string | null;
}

function calculateEffectivePrivilegesForRole(
  roleId: string,
  roleMap: Map<string, Role>,
  privilegeMap: Map<string, Privilege>,
  visited: Set<string>
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
        newVisited
      )
    );
  });

  return result;
}

/**
 * Hook: Compute effective privileges for a user from their roles, with granted-by attribution.
 * Used by User Profile Privileges tab.
 */
export function useUserEffectivePrivileges(user: User | null): UseUserEffectivePrivilegesResult {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [allRoles, setAllRoles] = useState<Role[]>([]);
  const [allPrivileges, setAllPrivileges] = useState<Privilege[]>([]);

  const { fetchRoles } = useRolesApi();
  const { fetchPrivileges } = usePrivilegesApi();

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [roles, privResponse] = await Promise.all([
        fetchRoles(),
        fetchPrivileges(),
      ]);
      setAllRoles(roles);
      setAllPrivileges(privResponse.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load security data');
    } finally {
      setLoading(false);
    }
  }, [fetchRoles, fetchPrivileges]);

  useEffect(() => {
    if (user) {
      loadData();
    } else {
      setLoading(false);
    }
  }, [user, loadData]);

  const roleMap = useMemo(
    () => new Map(allRoles.map((r) => [r.id, r])),
    [allRoles]
  );
  const privilegeMap = useMemo(
    () => new Map(allPrivileges.map((p) => [p.id, p])),
    [allPrivileges]
  );

  const privileges = useMemo(() => {
    if (loading || !user || allRoles.length === 0) return [];

    const roleIds = user.roles || [];
    const seen = new Map<string, EffectivePrivilege>();

    roleIds.forEach((roleId) => {
      const items = calculateEffectivePrivilegesForRole(
        roleId,
        roleMap,
        privilegeMap,
        new Set()
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
    });

    return Array.from(seen.values());
  }, [loading, user, allRoles.length, roleMap, privilegeMap]);

  return {
    privileges,
    roleMap,
    loading,
    error,
  };
}
