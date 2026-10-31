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


import { useEffect, useState } from 'react';

import { fetchBrowsableRepositories } from './browse.api';

/**
 * Repository name → type ('hosted' | 'proxy' | 'group').
 *
 * The repository list is session-scoped *configuration*, not an immutable fact:
 * an administrator can create, delete, or retype a repository while the Browse
 * page is open, and Delete gating must follow without an application reload. So
 * a successful response is cached only as a short-lived snapshot, never as
 * permanently authoritative:
 *
 *  - the snapshot expires after {@link REPOSITORY_TYPE_TTL_MS};
 *  - a lookup for a repository absent from the snapshot refetches immediately,
 *    so a repository created mid-session resolves on first selection rather
 *    than after the TTL;
 *  - {@link invalidateRepositoryTypeCache} drops it on demand.
 *
 * Concurrent lookups still share one in-flight request, which is the point of
 * caching at all: it keeps the type off the critical path when a deep link
 * lands directly on a repository.
 *
 * The types are read from {@link fetchBrowsableRepositories}, the *browse*-gated
 * repository listing. That matters because the gate below denies deletion when
 * the type is unresolved: a listing filtered by an administration permission
 * would resolve nothing for a delete-capable non-administrator and hide Delete
 * from exactly the delete-capable non-administrators the gate serves. See
 * `fetchBrowsableRepositories` for the server-side filters involved.
 */
export const REPOSITORY_TYPE_TTL_MS = 30_000;

interface RepositoryTypeSnapshot {
  readonly types: Map<string, string>;
  readonly fetchedAt: number;
}

let snapshot: RepositoryTypeSnapshot | null = null;
let inFlight: Promise<Map<string, string>> | null = null;

/**
 * Bumped on every invalidation. A request that was already in flight when the
 * cache was dropped carries pre-invalidation data, so publishing its result
 * would silently restore the very snapshot the invalidation removed — and stamp
 * it with a fresh timestamp, hiding the staleness for a whole TTL. Such a
 * response is still handed to its callers, it just does not become the cache.
 */
let epoch = 0;

function fetchRepositoryTypes(): Promise<Map<string, string>> {
  // Join an in-flight request rather than issuing a duplicate.
  if (inFlight) {
    return inFlight;
  }
  const requestEpoch = epoch;
  inFlight = fetchBrowsableRepositories()
    .then((repositories) => {
      const types = new Map(repositories.map((r) => [r.name, r.type]));
      if (requestEpoch === epoch) {
        snapshot = { types, fetchedAt: Date.now() };
      }
      return types;
    })
    .catch(() => {
      // Do not poison the cache: a transient failure should be retryable, and
      // an unresolved type simply means capability-gated actions stay hidden.
      return new Map<string, string>();
    })
    .finally(() => {
      inFlight = null;
    });
  return inFlight;
}

/**
 * Resolve the type map, reusing the cached snapshot only while it is fresh and
 * actually answers the question being asked.
 */
function loadRepositoryTypes(repositoryName: string): Promise<Map<string, string>> {
  if (
    snapshot &&
    Date.now() - snapshot.fetchedAt < REPOSITORY_TYPE_TTL_MS &&
    snapshot.types.has(repositoryName)
  ) {
    return Promise.resolve(snapshot.types);
  }
  return fetchRepositoryTypes();
}

/**
 * Drop the cached repository-type snapshot so the next lookup re-reads the
 * repository list. Call after any operation that changes the set of
 * repositories or their types.
 */
export function invalidateRepositoryTypeCache(): void {
  snapshot = null;
  epoch += 1;
}

/**
 * Test-only hook to drop the cached repository-type lookup between cases.
 */
export function __resetRepositoryTypeCache(): void {
  snapshot = null;
  inFlight = null;
  epoch += 1;
}

/**
 * Resolves the type of the given repository, or undefined while it is unknown.
 *
 * Undefined is a meaningful value here — capability decisions such as
 * {@link isDeletableRepositoryType} must treat an unresolved type as "not
 * permitted" so actions are never offered optimistically. The state is cleared
 * when `repositoryName` changes so the previously selected repository can never
 * lend its type to the new selection. The clear happens in the effect, so the
 * first render after a switch still returns the old type; that is invisible in
 * Browse because selecting a repository nulls the selected node in the same
 * commit, and every action gated on the type also requires a selected node.
 *
 * @param repositoryName Selected repository name, or undefined when none is selected
 */
export function useRepositoryType(repositoryName: string | undefined): string | undefined {
  const [repositoryType, setRepositoryType] = useState<string | undefined>(undefined);

  useEffect(() => {
    setRepositoryType(undefined);

    if (!repositoryName) {
      return;
    }

    let cancelled = false;
    loadRepositoryTypes(repositoryName).then((types) => {
      if (!cancelled) {
        setRepositoryType(types.get(repositoryName));
      }
    });

    return () => {
      cancelled = true;
    };
  }, [repositoryName]);

  return repositoryType;
}
