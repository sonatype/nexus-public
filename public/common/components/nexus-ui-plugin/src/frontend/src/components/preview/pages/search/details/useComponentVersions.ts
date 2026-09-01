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

/**
 * Component Versions Hook (XState-backed)
 *
 * Wires componentVersionsMachine to the real fetchComponentVersions API call, and adapts the
 * machine's 0-based page/context to the 1-based TablePagination control GAVersionsTab renders.
 * That conversion happens ONLY at the boundary in this hook — the machine and the API stay 0-based.
 */

import { useCallback, useEffect, useMemo, useRef } from 'react';

import { useMachine } from '@xstate/react';

import { fetchComponentVersions } from '../core/componentVersionsApi';
import type { ComponentVersionSort, GAVersion } from '../core/search.types';
import { cacheKeyOf, createComponentVersionsMachine } from './componentVersionsMachine';
import { parseGaId } from './gaIdUtils';

/**
 * Delay before a keystroke commits to a server query.
 *
 * 500ms matches the Classic UI search debounce, the same value
 * `iq-server/RepositoriesTab` adopted for that reason. Filters here are worth waiting slightly
 * longer for than a simple client-side list: each commit is a server round trip whose version
 * predicate is a non-indexable substring match, and version strings are long enough
 * ("1.0.101") that a shorter delay fires several throwaway queries mid-word.
 *
 * Exported so tests advance timers by exactly this amount instead of hardcoding it.
 */
export const SEARCH_DEBOUNCE_MS = 500;

export interface UseComponentVersionsArgs {
  readonly gaId: string;
}

export interface UseComponentVersionsReturn {
  readonly versions: readonly GAVersion[];
  /** Rows matching the current request identity — the filtered count once a filter is active. */
  readonly total: number;
  /**
   * The component's newest version and its unfiltered version count, both latched from the
   * default-ordered first page. Use these, not `versions[0]` and `total`, for anything outside
   * the versions table: those two follow the user's sort, page, and filter.
   */
  readonly newestVersion: string | null;
  readonly totalVersions: number;
  readonly totalPages: number;
  /** 1-based, matching TablePagination. */
  readonly currentPage: number;
  readonly itemsPerPage: number;
  readonly sortKey: ComponentVersionSort;
  readonly sortDirection: 'asc' | 'desc';
  readonly searchQuery: string;
  readonly loading: boolean;
  readonly error: string | null;
  readonly onPageChange: (page1Based: number) => void;
  readonly onItemsPerPageChange: (size: number) => void;
  readonly onSortChange: (sort: ComponentVersionSort, direction: 'asc' | 'desc') => void;
  readonly onSearchQueryChange: (value: string) => void;
  readonly retry: () => void;
}

export function useComponentVersions({ gaId }: UseComponentVersionsArgs): UseComponentVersionsReturn {
  const { format, group, name } = parseGaId(gaId);

  // Create the machine once per gaId — machine identity must be stable across renders.
  const machine = useMemo(() => createComponentVersionsMachine(gaId), [gaId]);

  const [state, send] = useMachine(machine, {
    services: {
      loadPage: (ctx) =>
        fetchComponentVersions({
          format,
          group: group || undefined,
          name,
          versionFilter: ctx.versionFilter || undefined,
          page: ctx.page,
          size: ctx.size,
          sort: ctx.sort,
          direction: ctx.direction,
        }),
    },
  });

  const ctx = state.context;
  const entry = ctx.cache[cacheKeyOf(ctx)];
  const versions: readonly GAVersion[] = entry?.pages[ctx.page] ?? [];
  const total = entry?.total ?? 0;
  const totalPages = ctx.size > 0 ? Math.ceil(total / ctx.size) : 0;

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Drop any keystroke still waiting to commit when the tab unmounts, otherwise the timer fires
  // against a stopped machine.
  useEffect(
    () => () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    },
    [],
  );

  const onSearchQueryChange = useCallback(
    (value: string) => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
      debounceRef.current = setTimeout(() => {
        send({ type: 'SET_VERSION_FILTER', versionFilter: value });
      }, SEARCH_DEBOUNCE_MS);
    },
    [send],
  );

  // No same-page guard here: the machine's pageIsUnchanged transition drops a SET_PAGE that
  // asks for the page already loaded. Guarding in the hook cannot work, because the page
  // TablePagination's size handler competes with is one SET_SIZE newer than any value this
  // render closed over.
  const onPageChange = useCallback(
    (page1Based: number) => send({ type: 'SET_PAGE', page: Math.max(0, page1Based - 1) }),
    [send],
  );

  const onItemsPerPageChange = useCallback(
    (size: number) => send({ type: 'SET_SIZE', size }),
    [send],
  );

  const onSortChange = useCallback(
    (sort: ComponentVersionSort, direction: 'asc' | 'desc') => send({ type: 'SET_SORT', sort, direction }),
    [send],
  );

  const retry = useCallback(() => send({ type: 'RETRY' }), [send]);

  return {
    versions,
    total,
    newestVersion: ctx.newestVersion,
    totalVersions: ctx.totalVersions,
    totalPages,
    // TablePagination is 1-based; the machine and API are 0-based. This is the only conversion point.
    currentPage: ctx.page + 1,
    itemsPerPage: ctx.size,
    sortKey: ctx.sort,
    sortDirection: ctx.direction,
    searchQuery: ctx.versionFilter,
    loading: ctx.loading,
    error: ctx.error,
    onPageChange,
    onItemsPerPageChange,
    onSortChange,
    onSearchQueryChange,
    retry,
  };
}
