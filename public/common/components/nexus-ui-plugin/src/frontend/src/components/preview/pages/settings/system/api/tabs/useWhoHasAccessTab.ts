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

import { useMemo, useState } from 'react';

import type { User } from '../../../security/users/types';
import {
  useEndpointAccess,
  type RoleWithAccessSummary,
  type UserWithAccessSummary,
} from '../hooks/useEndpointAccess';
import {
  permissionRequirementMode,
  type PermissionRequirementMode,
} from '../utils/formatEndpointPermissions';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';

export type { PermissionRequirementMode };

export interface UseWhoHasAccessTabOptions {
  row: MergedApiEndpoint;
  /** When false, skip loading security data (tab not visible). */
  active: boolean;
}

export interface UseWhoHasAccessTabReturn {
  loading: boolean;
  error: string | null;
  qualifyingRoles: RoleWithAccessSummary[];
  filteredUsers: UserWithAccessSummary[];
  roleLabelById: Map<string, string>;
  isAnonymous: boolean;
  noMappedPermissions: boolean;
  userFilter: string;
  setUserFilter: (value: string) => void;
  requirementCount: number;
  requirementMode: PermissionRequirementMode;
}

function matchesUserFilter(user: User, query: string): boolean {
  const blob = `${user.userId} ${user.firstName} ${user.lastName} ${user.emailAddress}`.toLowerCase();
  return blob.includes(query);
}

/**
 * ViewModel for {@link ./WhoHasAccessTab}. Wraps {@link useEndpointAccess} and exposes the
 * interaction state (`userFilter`) plus every derivation the view previously computed
 * inline (`filteredUsers`, `roleLabelById`, `isAnonymous`, `requirementCount`,
 * `requirementMode`) so the view stays purely presentational.
 */
export function useWhoHasAccessTab(options: UseWhoHasAccessTabOptions): UseWhoHasAccessTabReturn {
  const { row, active } = options;
  const { loading, error, qualifyingRoles, usersWithAccess, noMappedPermissions } = useEndpointAccess(row, active);
  const [userFilter, setUserFilter] = useState('');

  const filteredUsers = useMemo(() => {
    const query = userFilter.trim().toLowerCase();
    if (!query) {
      return usersWithAccess;
    }
    return usersWithAccess.filter(({ user }) => matchesUserFilter(user, query));
  }, [usersWithAccess, userFilter]);

  const roleLabelById = useMemo(() => {
    const map = new Map<string, string>();
    for (const { role } of qualifyingRoles) {
      map.set(role.id, role.name);
    }
    return map;
  }, [qualifyingRoles]);

  const isAnonymous = row.permission != null && !row.permission.authenticated;
  const requirementCount = row.permission?.permissions?.length ?? 0;
  const requirementMode = permissionRequirementMode(row.permission);

  return {
    loading,
    error,
    qualifyingRoles,
    filteredUsers,
    roleLabelById,
    isAnonymous,
    noMappedPermissions,
    userFilter,
    setUserFilter,
    requirementCount,
    requirementMode,
  };
}
