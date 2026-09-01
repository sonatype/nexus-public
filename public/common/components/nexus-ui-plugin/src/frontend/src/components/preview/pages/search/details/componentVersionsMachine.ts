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
 * Component Versions Machine - XState state machine for the paginated Versions tab.
 *
 * Owns a page cache keyed by request identity (gaId, sort, direction, size, versionFilter) —
 * deliberately excluding the selected version, since the version list does not depend on which
 * version is selected. This machine must be instantiated above Tabs.Root in GADetailPage: Radix
 * unmounts inactive tab content (no forceMount is passed), so a tab-owned machine would lose its
 * cache on every tab switch.
 */

import { assign, createMachine } from 'xstate';

import type { ComponentVersionSort, ComponentVersionsPage, GAVersion } from '../core/search.types';

// =============================================================================
// CONTEXT & EVENT TYPES
// =============================================================================

export interface ComponentVersionsCacheEntry {
  readonly pages: Record<number, readonly GAVersion[]>;
  readonly total: number;
}

export interface ComponentVersionsContext {
  readonly gaId: string;
  readonly sort: ComponentVersionSort;
  readonly direction: 'asc' | 'desc';
  readonly size: number;
  readonly versionFilter: string;
  /** 0-based, matching the API. */
  readonly page: number;
  readonly cache: Record<string, ComponentVersionsCacheEntry>;
  /**
   * Newest version and the component's true version count, captured from the default-ordered
   * first page and then left alone.
   *
   * These deliberately do NOT track `cache`/`page`. The cached page the rest of the hook exposes
   * is a window onto the user's current sort, page, and filter, so its first row is only the
   * newest version while all three are untouched — sort ascending and it is the oldest, page to
   * 2 and it is that page's first row, filter and it is the first match. The same applies to
   * `total`, which counts the filtered set. Anything outside the tab (the page header,
   * copy-path, the Overview default) needs values that are stable against tab interaction, so
   * they are recorded once from the one response known to be unfiltered and newest-first.
   */
  readonly newestVersion: string | null;
  readonly totalVersions: number;
  readonly loading: boolean;
  readonly error: string | null;
}

export type ComponentVersionsEvent =
  | { type: 'SET_PAGE'; page: number }
  | { type: 'SET_SORT'; sort: ComponentVersionSort; direction: 'asc' | 'desc' }
  | { type: 'SET_SIZE'; size: number }
  | { type: 'SET_VERSION_FILTER'; versionFilter: string }
  | { type: 'RETRY' };

// =============================================================================
// CACHE KEY
// =============================================================================

/**
 * Identity of a request set. Deliberately excludes the selected version: the version list does
 * not depend on which version is selected, so switching versions must not invalidate the cache.
 */
export function cacheKeyOf(
  ctx: Pick<ComponentVersionsContext, 'gaId' | 'sort' | 'direction' | 'size' | 'versionFilter'>,
): string {
  return `${ctx.gaId}|${ctx.sort}|${ctx.direction}|${ctx.size}|${ctx.versionFilter}`;
}

const isPageCached = (ctx: ComponentVersionsContext, page: number): boolean =>
  ctx.cache[cacheKeyOf(ctx)]?.pages[page] !== undefined;

// =============================================================================
// SHARED TRANSITIONS (used from idle, loading, and failed alike)
// =============================================================================

const REQUEST_TRANSITIONS = {
  SET_PAGE: [
    // Targetless, so this is an internal transition: the current state is not re-entered and
    // an in-flight loadPage invoke keeps running. TablePagination's page-size handler always
    // calls onPageChange(1) right after onItemsPerPageChange, and SET_SIZE has already reset
    // the page to 0 — without this the redundant event would restart 'loading' and fire a
    // second fetch for the page just requested. The comparison has to live here rather than
    // in the hook: only the machine sees the page SET_SIZE just wrote, since both events are
    // dispatched from one synchronous handler before React re-renders.
    { cond: 'pageIsUnchanged' },
    { cond: 'pageIsCached', actions: 'setPage', target: 'idle' },
    { actions: 'setPage', target: 'loading' },
  ],
  SET_SORT: { actions: 'setSort', target: 'loading' },
  SET_SIZE: { actions: 'setSize', target: 'loading' },
  SET_VERSION_FILTER: { actions: 'setVersionFilter', target: 'loading' },
};

// =============================================================================
// COMPONENT VERSIONS MACHINE FACTORY
// =============================================================================

/**
 * Create a component-versions machine for a specific gaId. The first page (page=0) loads
 * eagerly: it supplies the badge total and the newest-version fallback other parts of the
 * detail view need, both of which are latched into context (see `newestVersion`) rather than
 * read back off the page cache, which moves with the user's sort, page, and filter.
 *
 * Service (override via interpret/withConfig or useMachine):
 * - `loadPage`: fetches one page of versions for the current context
 */
export function createComponentVersionsMachine(gaId: string) {
  return createMachine<ComponentVersionsContext, ComponentVersionsEvent>(
    {
      id: `component-versions-${gaId}`,
      initial: 'loading',
      context: {
        gaId,
        sort: 'version',
        direction: 'desc',
        size: 20,
        versionFilter: '',
        page: 0,
        cache: {},
        newestVersion: null,
        totalVersions: 0,
        loading: false,
        error: null,
      },
      states: {
        idle: {
          on: REQUEST_TRANSITIONS,
        },
        loading: {
          entry: assign<ComponentVersionsContext>({ loading: true, error: null }),
          invoke: {
            src: 'loadPage',
            onDone: { actions: 'storePage', target: 'idle' },
            onError: { actions: 'setError', target: 'failed' },
          },
          exit: assign<ComponentVersionsContext>({ loading: false }),
          on: REQUEST_TRANSITIONS,
        },
        failed: {
          on: {
            RETRY: { target: 'loading' },
            ...REQUEST_TRANSITIONS,
          },
        },
      },
    },
    {
      guards: {
        pageIsUnchanged: (ctx, event) => event.type === 'SET_PAGE' && event.page === ctx.page,
        pageIsCached: (ctx, event) =>
          event.type === 'SET_PAGE' && isPageCached(ctx, event.page),
      },
      actions: {
        setPage: assign((_ctx, event) =>
          event.type === 'SET_PAGE' ? { page: event.page } : {},
        ),
        // Changing the request identity drops the previous cache entry, so memory stays
        // bounded by one key's loaded pages.
        setSort: assign((_ctx, event) =>
          event.type === 'SET_SORT'
            ? { sort: event.sort, direction: event.direction, page: 0, cache: {} }
            : {},
        ),
        setSize: assign((_ctx, event) =>
          event.type === 'SET_SIZE' ? { size: event.size, page: 0, cache: {} } : {},
        ),
        setVersionFilter: assign((_ctx, event) =>
          event.type === 'SET_VERSION_FILTER'
            ? { versionFilter: event.versionFilter, page: 0, cache: {} }
            : {},
        ),
        storePage: assign((ctx, event) => {
          const result = (event as { data: ComponentVersionsPage }).data;
          const key = cacheKeyOf(ctx);
          const existing = ctx.cache[key] ?? { pages: {}, total: 0 };
          // Guarded rather than assumed: the first load always satisfies this (the initial
          // context is default sort, no filter, page 0), so stating the condition costs nothing
          // and keeps it true if that ever changes. `size` is absent on purpose — a page-size
          // change re-fetches page 0 of the same ordering, whose first row is still the newest
          // version and whose total is still the unfiltered count.
          const isDefaultFirstPage =
            ctx.sort === 'version' &&
            ctx.direction === 'desc' &&
            ctx.versionFilter === '' &&
            result.page === 0;
          return {
            error: null,
            newestVersion: isDefaultFirstPage
              ? (result.items[0]?.version ?? null)
              : ctx.newestVersion,
            totalVersions: isDefaultFirstPage ? result.total : ctx.totalVersions,
            cache: {
              ...ctx.cache,
              [key]: {
                total: result.total,
                pages: { ...existing.pages, [result.page]: result.items },
              },
            },
          };
        }),
        setError: assign((_ctx, event) => ({
          error: (event as { data?: Error }).data?.message ?? 'Failed to load versions',
        })),
      },
      services: {
        // Default: reject. Override in useMachine/withConfig.
        loadPage: (_ctx) => Promise.reject(new Error('loadPage service not configured')),
      },
    },
  );
}
