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

import { useCallback, useEffect, useState } from 'react';
import { restClient, parseApiError } from '../../../../../../interface/api';
import type {
  PrivilegeInfo,
  RoleInfo,
  UserWithAccess,
  AnonymousAccess,
} from '../profile/types';

const PRIVILEGES_URL = '/service/rest/v1/security/privileges';
const ROLES_URL = '/service/rest/v1/security/roles';
const USERS_URL = '/service/rest/v1/security/users';
const ANONYMOUS_URL = '/service/rest/v1/security/anonymous';

interface UseRepositoryAccessSecurityResult {
  privileges: PrivilegeInfo[];
  roles: RoleInfo[];
  users: UserWithAccess[];
  anonymousAccess: AnonymousAccess | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

interface UseRepositoryAccessSecurityOptions {
  /** When false, the privileges REST call is skipped. Defaults to true. */
  canReadPrivileges?: boolean;
  /** When false, the roles REST call is skipped. Defaults to true. */
  canReadRoles?: boolean;
  /** When false, the users REST call is skipped. Defaults to true. */
  canReadUsers?: boolean;
  /** When false, the anonymous-access REST call is skipped. Defaults to true. */
  canReadAnonymous?: boolean;
  /** Repository format, used to match wildcard privileges (repository="*"). */
  repositoryFormat?: string;
}

function filterPrivileges(
  all: PrivilegeInfo[],
  repositoryName: string,
  repositoryFormat: string | undefined
): PrivilegeInfo[] {
  const repoNameLower = repositoryName.toLowerCase();
  return all.filter((p) => {
    if (p.repository === repositoryName) return true;
    const privNameLower = p.name.toLowerCase();
    if (
      privNameLower.includes(`-${repoNameLower}-`) ||
      privNameLower.includes(`-${repoNameLower}*`) ||
      privNameLower.endsWith(`-${repoNameLower}`)
    ) {
      return true;
    }
    if (p.repository === '*' && p.format === repositoryFormat) return true;
    return false;
  });
}

/**
 * Hook for fetching repo-scoped access & security data (privileges, roles,
 * users, anonymous access) for the Repository Settings page. The chain is
 * privileges -> roles (filtered by privilege names) -> users (filtered by
 * matched role ids); anonymous access is independent and fires in parallel.
 * Endpoints the caller is not allowed to read are skipped so we don't emit
 * 403s. Cancels in flight when the component unmounts or the repo changes.
 */
export function useRepositoryAccessSecurity(
  repositoryName: string,
  {
    canReadPrivileges = true,
    canReadRoles = true,
    canReadUsers = true,
    canReadAnonymous = true,
    repositoryFormat,
  }: UseRepositoryAccessSecurityOptions = {}
): UseRepositoryAccessSecurityResult {
  const [privileges, setPrivileges] = useState<PrivilegeInfo[]>([]);
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [users, setUsers] = useState<UserWithAccess[]>([]);
  const [anonymousAccess, setAnonymousAccess] = useState<AnonymousAccess | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [refetchTrigger, setRefetchTrigger] = useState(0);

  const refetch = useCallback(() => {
    setRefetchTrigger((n) => n + 1);
  }, []);

  useEffect(() => {
    let cancelled = false;

    setLoading(true);
    setError(null);

    (async () => {
      try {
        const anonymousPromise: Promise<AnonymousAccess | null> = canReadAnonymous
          ? restClient.get<AnonymousAccess>(ANONYMOUS_URL).catch((err) => {
              // Anonymous access is optional on some deployments (cloudExcluded
              // in settingsConfig; endpoint availability varies by edition).
              // Degrade to null so a missing anonymous endpoint does not hide
              // privileges/roles/users. Mirrors repositoryProfileMachine.
              console.warn('Could not fetch anonymous access:', err);
              return null;
            })
          : Promise.resolve(null);

        const filteredPrivileges = canReadPrivileges
          ? filterPrivileges(
              (await restClient.get<PrivilegeInfo[]>(PRIVILEGES_URL)) || [],
              repositoryName,
              repositoryFormat
            )
          : [];

        if (cancelled) return;

        const matchedRoles = canReadRoles && filteredPrivileges.length > 0
          ? (async () => {
              const allRoles = (await restClient.get<RoleInfo[]>(ROLES_URL)) || [];
              const privNames = filteredPrivileges.map((p) => p.name);
              return allRoles.filter((r) => r.privileges.some((rp) => privNames.includes(rp)));
            })()
          : Promise.resolve<RoleInfo[]>([]);

        const rolesResolved = await matchedRoles;
        if (cancelled) return;

        const matchedUsers = canReadUsers && rolesResolved.length > 0
          ? (async () => {
              const allUsers = (await restClient.get<UserWithAccess[]>(USERS_URL)) || [];
              const roleIds = rolesResolved.map((r) => r.id);
              return allUsers.filter((u) => u.roles?.some((ur) => roleIds.includes(ur)));
            })()
          : Promise.resolve<UserWithAccess[]>([]);

        const [usersResolved, anonymousResolved] = await Promise.all([
          matchedUsers,
          anonymousPromise,
        ]);

        if (cancelled) return;

        setPrivileges(filteredPrivileges);
        setRoles(rolesResolved);
        setUsers(usersResolved);
        setAnonymousAccess(anonymousResolved ?? null);
      } catch (err) {
        if (cancelled) return;
        setError(parseApiError(err as Error).message);
        setPrivileges([]);
        setRoles([]);
        setUsers([]);
        setAnonymousAccess(null);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [
    repositoryName,
    repositoryFormat,
    refetchTrigger,
    canReadPrivileges,
    canReadRoles,
    canReadUsers,
    canReadAnonymous,
  ]);

  return { privileges, roles, users, anonymousAccess, loading, error, refetch };
}

export default useRepositoryAccessSecurity;
