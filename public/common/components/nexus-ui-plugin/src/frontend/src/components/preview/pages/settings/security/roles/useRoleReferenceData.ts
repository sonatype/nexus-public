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
import type { PrivilegeReference, RoleReference } from './types';

// Fetches privileges + roles once; excludes the current role and dedupes by id.

interface ReferencePayload {
  privileges: PrivilegeReference[];
  roles: RoleReference[];
}

export interface UseRoleReferenceDataResult {
  allPrivileges: PrivilegeReference[];
  allRoles: RoleReference[];
  loading: boolean;
  error: string | null;
  errorDetails: ResourceError | null;
}

export function useRoleReferenceData(currentRoleId: string): UseRoleReferenceDataResult {
  const { fetchRoleReferences, fetchPrivilegeReferences } = useRolesApi();

  const { data, loading, error, errorMessage } = useResourceMachine<ReferencePayload>({
    id: 'role-reference-data',
    initialData: { privileges: [], roles: [] },
    fetch: () =>
      Promise.all([fetchPrivilegeReferences(), fetchRoleReferences()]).then(
        ([privileges, roles]) => ({ privileges, roles }),
      ),
  });

  // Exclude current role (prevents cycles) and dedupe by id.
  const allRoles = useMemo(() => {
    const map = new Map<string, RoleReference>();
    for (const r of data.roles) {
      if (r.id !== currentRoleId && !map.has(r.id)) {
        map.set(r.id, { id: r.id, name: r.name });
      }
    }
    return Array.from(map.values());
  }, [data.roles, currentRoleId]);

  return {
    allPrivileges: data.privileges,
    allRoles,
    loading,
    error: errorMessage,
    errorDetails: error,
  };
}
