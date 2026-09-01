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
import { useMachine } from '@xstate/react';

import { usePrivilegesApi } from '../../../security/privileges/usePrivilegesApi';
import type { Role } from '../../../security/roles/types';
import { useRolesApi } from '../../../security/roles/useRolesApi';
import type { User, UserFormData } from '../../../security/users/types';
import { useUsersApi } from '../../../security/users/useUsersApi';

import {
  ensureSecurityDirectoryLoaded,
  getSecurityDirectoryCache,
  invalidateEndpointAccessCache,
} from '../hooks/securityDirectoryCache';
import { computeEndpointAccessSummaries } from '../hooks/useEndpointAccess';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';
import { pickPrivilegeIdsForRequirements } from '../utils/grantPrivilegePicker';
import { rankRolesForGrantRecommendations } from '../utils/grantRoleRecommendations';

import { createGrantWizardMachine, type GrantWizardContext } from './grantWizardMachine';

export function userDirectoryKey(u: User): string {
  return `${u.userId}\0${u.source}`;
}

export function suggestNewRoleId(row: MergedApiEndpoint): string {
  const tag = (row.tag || 'api')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40);
  const safeTag = tag || 'api';
  return `api-${safeTag}-${row.httpMethod.toLowerCase()}`;
}

export function suggestNewRoleName(row: MergedApiEndpoint): string {
  return `API — ${row.tag || 'Endpoint'} (${row.httpMethod})`;
}

export function useGrantWizard(row: MergedApiEndpoint | null, active: boolean) {
  const { fetchRoles, createRole } = useRolesApi();
  const { fetchSources, fetchUsers, updateUser } = useUsersApi();
  const { fetchPrivileges } = usePrivilegesApi();

  const [_cacheGen, setCacheGen] = useState(0);
  const [dirError, setDirError] = useState<string | null>(null);
  const [loadingDir, setLoadingDir] = useState(false);

  const fetchAllPrivileges = useCallback(async () => {
    const res = await fetchPrivileges(undefined, undefined, undefined, 0, undefined);
    return res.data;
  }, [fetchPrivileges]);

  useEffect(() => {
    if (!(active && row)) {
      return undefined;
    }
    if (getSecurityDirectoryCache()) {
      return undefined;
    }
    let cancelled = false;
    setLoadingDir(true);
    setDirError(null);
    ensureSecurityDirectoryLoaded({
      fetchRoles,
      fetchAllPrivileges,
      fetchSources,
      fetchUsers,
    })
      .then(() => {
        if (!cancelled) {
          setCacheGen((g) => g + 1);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setDirError(e instanceof Error ? e.message : 'Failed to load security data');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoadingDir(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [active, row, fetchRoles, fetchAllPrivileges, fetchSources, fetchUsers]);

  const session = useMemo(() => getSecurityDirectoryCache(), []);

  const { qualifyingRoles, noMappedPermissions } = useMemo(() => {
    if (!(row && session)) {
      return { qualifyingRoles: [] as { role: Role; userCount: number }[], noMappedPermissions: false };
    }
    return computeEndpointAccessSummaries(row, session.roles, session.privileges, session.users);
  }, [row, session]);

  const recommendedRoles = useMemo(() => {
    const roles = qualifyingRoles.map((q) => q.role);
    if (!session || roles.length === 0) {
      return [] as Role[];
    }
    return rankRolesForGrantRecommendations(roles, session.roles);
  }, [qualifyingRoles, session]);

  const userCountByRoleId = useMemo(() => {
    const m = new Map<string, number>();
    for (const { role, userCount } of qualifyingRoles) {
      m.set(role.id, userCount);
    }
    return m;
  }, [qualifyingRoles]);

  const applyGrant = useCallback(
    async (ctx: GrantWizardContext) => {
      if (!row) {
        throw new Error('No endpoint selected');
      }
      const snap = getSecurityDirectoryCache();
      if (!snap) {
        throw new Error('Security data not loaded');
      }
      const reqs = row.permission?.permissions?.map((p) => p.permission).filter(Boolean) ?? [];
      if (reqs.length === 0) {
        throw new Error('No mapped permissions for this endpoint');
      }
      const logical = row.permission?.permissions?.[0]?.logical === 'OR' ? 'OR' : 'AND';

      let targetRoleId: string;
      if (ctx.mode === 'create') {
        const { ids, missing } = pickPrivilegeIdsForRequirements(reqs, logical, snap.privileges);
        if (missing.length > 0) {
          throw new Error(
            `No privilege found in Nexus for: ${missing.join(', ')}. Create a matching privilege under Security first.`
          );
        }
        const created = await createRole({
          id: ctx.newRoleId.trim(),
          name: ctx.newRoleName.trim(),
          description: ctx.newRoleDescription.trim(),
          privileges: ids,
          roles: [],
        });
        targetRoleId = created.id;
      } else {
        if (!ctx.existingRoleId) {
          throw new Error('No role selected');
        }
        targetRoleId = ctx.existingRoleId;
      }

      const results: Array<{ userKey: string; ok: boolean; message?: string }> = [];
      for (const key of ctx.selectedUserKeys) {
        const [userId, source] = key.split('\0');
        const user = snap.users.find((u) => u.userId === userId && u.source === source);
        if (!user) {
          results.push({ userKey: key, ok: false, message: 'User not found in directory' });
          continue;
        }
        try {
          const roles = [...new Set([...(user.roles || []), targetRoleId])];
          const form: UserFormData = {
            userId: user.userId,
            firstName: user.firstName,
            lastName: user.lastName,
            emailAddress: user.emailAddress,
            status: user.status === 'active',
            roles,
          };
          await updateUser(user.userId, form, user.source);
          results.push({ userKey: key, ok: true });
        } catch (e: unknown) {
          results.push({
            userKey: key,
            ok: false,
            message: e instanceof Error ? e.message : String(e),
          });
        }
      }

      invalidateEndpointAccessCache();
      setCacheGen((g) => g + 1);
      return { results };
    },
    [row, createRole, updateUser]
  );

  const machine = useMemo(() => createGrantWizardMachine(), []);

  const [state, send] = useMachine(machine, {
    services: {
      applyGrant: (ctx) => applyGrant(ctx),
    },
  });

  useEffect(() => {
    if (!state.matches('step2')) {
      return;
    }
    if (state.context.mode !== 'existing') {
      return;
    }
    if (state.context.existingRoleId) {
      return;
    }
    const top = recommendedRoles[0];
    if (!top) {
      return;
    }
    send({ type: 'SELECT_EXISTING', roleId: top.id });
  }, [state, recommendedRoles, send]);

  return {
    state,
    send,
    session,
    loadingDir,
    dirError,
    recommendedRoles,
    qualifyingRoles,
    userCountByRoleId,
    noMappedPermissions,
    suggestedRoleId: row ? suggestNewRoleId(row) : '',
    suggestedRoleName: row ? suggestNewRoleName(row) : '',
  };
}

export type { GrantWizardEvent } from './grantWizardMachine';
