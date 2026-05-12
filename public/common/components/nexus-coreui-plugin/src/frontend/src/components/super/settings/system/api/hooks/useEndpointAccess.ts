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

import { useCallback, useEffect, useMemo, useState } from 'react';

import { usePrivilegesApi } from '../../../security/privileges/usePrivilegesApi';
import type { Role } from '../../../security/roles/types';
import { useRolesApi } from '../../../security/roles/useRolesApi';
import type { User } from '../../../security/users/types';
import { useUsersApi } from '../../../security/users/useUsersApi';

import {
  ensureSecurityDirectoryLoaded,
  getSecurityDirectoryCache,
  invalidateEndpointAccessCache,
} from './securityDirectoryCache';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';
import {
  buildPrivilegeByIdMap,
  buildRoleByIdMap,
  roleSatisfiesRequiredPermissions,
} from '../utils/rolePrivilegeCoverage';

export type { SecurityDirectorySession } from './securityDirectoryCache';
export { invalidateEndpointAccessCache };

export interface RoleWithAccessSummary {
  role: Role;
  userCount: number;
}

export interface UserWithAccessSummary {
  user: User;
  grantingRoleIds: string[];
}

export interface UseEndpointAccessResult {
  loading: boolean;
  error: string | null;
  qualifyingRoles: RoleWithAccessSummary[];
  usersWithAccess: UserWithAccessSummary[];
  noMappedPermissions: boolean;
  refetch: () => Promise<void>;
}

/** Shared by Who Has Access and Grant wizard recommendations. */
export function computeEndpointAccessSummaries(
  row: MergedApiEndpoint,
  roles: Role[],
  privileges: import('../../../security/privileges/types').Privilege[],
  users: User[]
): {
  qualifyingRoles: RoleWithAccessSummary[];
  usersWithAccess: UserWithAccessSummary[];
  noMappedPermissions: boolean;
} {
  const reqs = row.permission?.permissions?.map((p) => p.permission).filter(Boolean) ?? [];
  if (reqs.length === 0) {
    return { qualifyingRoles: [], usersWithAccess: [], noMappedPermissions: true };
  }

  const mode = row.permission?.permissions?.[0]?.logical === 'OR' ? 'OR' : 'AND';
  const roleById = buildRoleByIdMap(roles);
  const privilegeById = buildPrivilegeByIdMap(privileges);

  const qualifying = roles.filter((r) =>
    roleSatisfiesRequiredPermissions(r.id, roleById, privilegeById, reqs, mode)
  );
  const qualifyingIds = new Set(qualifying.map((r) => r.id));

  const userCountByRole = new Map<string, number>();
  for (const r of qualifying) {
    userCountByRole.set(
      r.id,
      users.filter((u) => (u.roles || []).includes(r.id)).length
    );
  }

  const qualifyingRoles: RoleWithAccessSummary[] = [...qualifying]
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((role) => ({
      role,
      userCount: userCountByRole.get(role.id) ?? 0,
    }));

  const usersWithAccess: UserWithAccessSummary[] = [];
  for (const u of users) {
    const granting = (u.roles || []).filter((rid) => qualifyingIds.has(rid));
    if (granting.length > 0) {
      usersWithAccess.push({ user: u, grantingRoleIds: granting });
    }
  }
  usersWithAccess.sort((a, b) => a.user.userId.localeCompare(b.user.userId));

  return { qualifyingRoles, usersWithAccess, noMappedPermissions: false };
}

/**
 * Loads roles, privileges, and users once per session; derives who can call the selected endpoint.
 */
export function useEndpointAccess(row: MergedApiEndpoint | null, enabled: boolean): UseEndpointAccessResult {
  const { fetchRoles } = useRolesApi();
  const { fetchPrivileges } = usePrivilegesApi();
  const { fetchSources, fetchUsers } = useUsersApi();

  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);
  /** Bumps when module sessionCache is populated so useMemo sees fresh data */
  const [cacheGen, setCacheGen] = useState(0);

  const fetchAllPrivileges = useCallback(async () => {
    const res = await fetchPrivileges(undefined, undefined, undefined, 0, undefined);
    return res.data;
  }, [fetchPrivileges]);

  useEffect(() => {
    if (!enabled || !row) {
      return undefined;
    }
    if (getSecurityDirectoryCache()) {
      return undefined;
    }
    let cancelled = false;
    setFetching(true);
    setError(null);
    ensureSecurityDirectoryLoaded({
      fetchRoles,
      fetchAllPrivileges,
      fetchSources,
      fetchUsers,
    })
      .then(() => {
        if (!cancelled && getSecurityDirectoryCache()) {
          setCacheGen((g) => g + 1);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Failed to load security data');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setFetching(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [enabled, row, fetchRoles, fetchAllPrivileges, fetchSources, fetchUsers, tick]);

  const refetch = useCallback(async () => {
    invalidateEndpointAccessCache();
    setCacheGen(0);
    setTick((t) => t + 1);
  }, []);

  const derived = useMemo(() => {
    if (!row) {
      return {
        qualifyingRoles: [] as RoleWithAccessSummary[],
        usersWithAccess: [] as UserWithAccessSummary[],
        noMappedPermissions: false,
      };
    }
    const session = getSecurityDirectoryCache();
    if (!session) {
      return {
        qualifyingRoles: [] as RoleWithAccessSummary[],
        usersWithAccess: [] as UserWithAccessSummary[],
        noMappedPermissions: false,
      };
    }
    return computeEndpointAccessSummaries(row, session.roles, session.privileges, session.users);
  }, [row, cacheGen]);

  const loading = enabled && !!row && fetching && !getSecurityDirectoryCache();

  return {
    loading,
    error,
    qualifyingRoles: derived.qualifyingRoles,
    usersWithAccess: derived.usersWithAccess,
    noMappedPermissions: derived.noMappedPermissions,
    refetch,
  };
}
