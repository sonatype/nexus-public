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
import type { User } from '../../../security/users/types';

export interface SecurityDirectorySession {
  roles: Role[];
  privileges: Privilege[];
  users: User[];
}

let sessionCache: SecurityDirectorySession | null = null;
let loadPromise: Promise<void> | null = null;

/** Clears cached roles, privileges, and users (e.g. after granting access). */
export function invalidateEndpointAccessCache(): void {
  sessionCache = null;
  loadPromise = null;
}

export function getSecurityDirectoryCache(): SecurityDirectorySession | null {
  return sessionCache;
}

export async function fetchAllUsersAcrossSources(
  fetchSources: () => Promise<Array<{ id: string }>>,
  fetchUsers: (filter: string, source: string) => Promise<User[]>
): Promise<User[]> {
  const sources = await fetchSources();
  const merged: User[] = [];
  const seen = new Set<string>();
  for (const s of sources) {
    try {
      const batch = await fetchUsers('', s.id);
      for (const u of batch) {
        const key = `${u.userId}\0${u.source}`;
        if (!seen.has(key)) {
          seen.add(key);
          merged.push(u);
        }
      }
    } catch {
      // omit failing realm
    }
  }
  return merged;
}

export interface SecurityDirectoryLoaders {
  fetchRoles: () => Promise<Role[]>;
  fetchAllPrivileges: () => Promise<Privilege[]>;
  fetchSources: () => Promise<Array<{ id: string }>>;
  fetchUsers: (filter: string, source: string) => Promise<User[]>;
}

/**
 * Loads roles, privileges, and all users once per page session (shared by Who Has Access and Grant wizard).
 */
export async function ensureSecurityDirectoryLoaded(loaders: SecurityDirectoryLoaders): Promise<SecurityDirectorySession> {
  if (sessionCache) {
    return sessionCache;
  }
  if (loadPromise) {
    await loadPromise;
    return sessionCache!;
  }
  loadPromise = (async () => {
    const [roleList, privileges, userList] = await Promise.all([
      loaders.fetchRoles(),
      loaders.fetchAllPrivileges(),
      fetchAllUsersAcrossSources(loaders.fetchSources, loaders.fetchUsers),
    ]);
    sessionCache = {
      roles: roleList,
      privileges,
      users: userList,
    };
  })();
  try {
    await loadPromise;
  } finally {
    loadPromise = null;
  }
  return sessionCache!;
}
