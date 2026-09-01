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
 * Search Machine - XState state machine for unified search.
 *
 * Models the full search lifecycle with format sub-states:
 * - Lifecycle: idle -> searching -> results/error, with pagination (loadingMore)
 * - Format: 25 format sub-states in a parallel region, each declaring filter metadata
 *
 * Format sub-states enable:
 * 1. Tests that verify every format's filter config (it.each over 25 formats)
 * 2. Tests that verify API param mapping (catches npm scope->group, nuget nugetId->nuget.id)
 * 3. Tests that verify format switching clears stale filters
 * 4. Component can read active format metadata from state.meta
 *
 * This is the reference implementation for page-level XState machines.
 * Forms use createFormMachine(); pages use custom machines like this.
 */

import { createMachine, assign } from 'xstate';
import type { SearchFormat, FilterValues, SearchResult, FilterDefinition } from './unified.types';
import { FORMAT_FILTERS, FORMATS, buildQueryParams, } from './searchFilters';
import { parseApiError } from '../../../../../interface/api/error-handler';

// =============================================================================
// CONTEXT & EVENT TYPES
// =============================================================================

export interface SearchMachineContext {
  /** Selected search format */
  format: SearchFormat;
  /** Search query text */
  query: string;
  /** Current filter values (keyed by filter ID) */
  filters: FilterValues;
  /** Search results */
  results: SearchResult[];
  /** Total result count */
  totalCount: number;
  /** Error message from last operation */
  error: string | undefined;
  /** Pagination continuation token */
  continuationToken: string | undefined;
  /** Sort field */
  sortField: string;
  /** Sort direction */
  sortDirection: string;
}

export type SearchMachineEvent =
  | { type: 'SELECT_FORMAT'; format: SearchFormat }
  | { type: 'SET_QUERY'; query: string }
  | { type: 'UPDATE_FILTER'; name: string; value: string }
  | { type: 'SET_FILTERS'; filters: FilterValues }
  | { type: 'SET_SORT'; field?: string; direction?: string }
  | { type: 'SEARCH'; formats?: SearchFormat[] }
  | { type: 'LOAD_MORE' }
  | { type: 'RETRY' }
  | { type: 'RESET' };

// =============================================================================
// FORMAT SUB-STATES (generated from searchFilters.ts)
// =============================================================================

/**
 * Generate format sub-state definitions from the existing FORMAT_FILTERS config.
 * Each sub-state's meta declares the format's filters, apiFormat, and placeholder.
 * This is the single source of truth for "what filters does this format have?"
 */
function buildFormatStates(): Record<string, { meta: FormatStateMeta }> {
  const states: Record<string, { meta: FormatStateMeta }> = {};

  for (const [format, config] of Object.entries(FORMAT_FILTERS)) {
    const customFilters = config.filters.filter((f: FilterDefinition) => !f.global);
    states[format] = {
      meta: {
        formatId: format as SearchFormat,
        label: config.format.label,
        apiFormat: config.format.apiFormat,
        placeholder: config.format.placeholder,
        filters: [...config.filters],
        customFilters,
        filterIds: config.filters.map((f: FilterDefinition) => f.id),
        apiParamMap: config.filters.reduce((map: Record<string, string>, f: FilterDefinition) => {
          map[f.id] = f.apiParam;
          return map;
        }, {}),
      },
    };
  }

  return states;
}

/**
 * Metadata for a format sub-state.
 * Available via state.meta when the machine is in that format.
 */
export interface FormatStateMeta {
  formatId: SearchFormat;
  label: string;
  apiFormat: string;
  placeholder: string;
  filters: FilterDefinition[];
  customFilters: FilterDefinition[];
  filterIds: string[];
  /** Maps filter ID to API parameter name (the tricky mappings live here) */
  apiParamMap: Record<string, string>;
}

/**
 * Generate SELECT_FORMAT guard functions for each format.
 * Each guard checks if event.format matches the target format.
 */
function buildFormatGuards(): Record<string, (ctx: SearchMachineContext, event: SearchMachineEvent) => boolean> {
  const guards: Record<string, (ctx: SearchMachineContext, event: SearchMachineEvent) => boolean> = {};

  for (const format of Object.keys(FORMAT_FILTERS)) {
    guards[`isFormat_${format}`] = (_ctx, event) =>
      event.type === 'SELECT_FORMAT' && (event as { format: string }).format === format;
  }

  return guards;
}

/**
 * Generate SELECT_FORMAT transitions for the format region.
 * Each transition targets a specific format sub-state with a guard.
 */
function buildFormatTransitions() {
  return Object.keys(FORMAT_FILTERS).map((format) => ({
    target: `.${format}` as string,
    cond: `isFormat_${format}`,
    actions: 'changeFormat' as const,
  }));
}

// Pre-build at module load time
const FORMAT_STATES = buildFormatStates();
const FORMAT_GUARDS = buildFormatGuards();
const FORMAT_TRANSITIONS = buildFormatTransitions();

// =============================================================================
// SEARCH MACHINE FACTORY
// =============================================================================

/**
 * Create the search machine.
 *
 * Uses parallel states:
 * - `lifecycle`: idle -> searching -> results -> loadingMore (search flow)
 * - `format`: one sub-state per format, each with filter metadata
 *
 * When SELECT_FORMAT fires:
 * - lifecycle region: goes back to idle, clears results
 * - format region: transitions to the new format sub-state, clears filters
 */
export function createSearchMachine() {
  return createMachine<SearchMachineContext, SearchMachineEvent>(
    {
      id: 'search',
      type: 'parallel',
      context: {
        format: 'all',
        query: '',
        filters: {},
        results: [],
        totalCount: 0,
        error: undefined,
        continuationToken: undefined,
        sortField: 'lastUpdated',
        sortDirection: 'desc',
      },

      states: {
        // ==================================================
        // Lifecycle Region - search flow
        // ==================================================
        lifecycle: {
          initial: 'idle',
          states: {
            idle: {},

            searching: {
              invoke: {
                src: 'search',
                onDone: {
                  target: 'results',
                  actions: 'setResults',
                },
                onError: {
                  target: 'error',
                  actions: 'setError',
                },
              },
            },

            results: {
              on: {
                LOAD_MORE: {
                  target: 'loadingMore',
                  cond: 'hasMore',
                },
              },
            },

            loadingMore: {
              invoke: {
                src: 'loadMore',
                onDone: {
                  target: 'results',
                  actions: 'appendResults',
                },
                onError: {
                  target: 'results',
                  actions: 'setError',
                },
              },
            },

            error: {},
          },

          on: {
            // SEARCH always (re-)starts a search from any lifecycle state
            SEARCH: {
              target: '.searching',
              actions: 'prepareSearch',
            },
            RETRY: {
              target: '.searching',
            },
            // Format change resets lifecycle to idle
            SELECT_FORMAT: {
              target: '.idle',
              actions: 'clearResults',
            },
            RESET: {
              target: '.idle',
              actions: ['clearResults', 'resetQuery'],
            },
          },
        },

        // ==================================================
        // Format Region - 25 format sub-states with metadata
        // ==================================================
        format: {
          initial: 'all',
          states: FORMAT_STATES,
          on: {
            SELECT_FORMAT: FORMAT_TRANSITIONS,
            RESET: {
              target: '.all',
              actions: 'changeFormat',
            },
          },
        },
      },

      // ==================================================
      // Global events (affect context only, no state transitions)
      // ==================================================
      on: {
        SET_QUERY: {
          actions: 'setQuery',
        },
        UPDATE_FILTER: {
          actions: 'updateFilter',
        },
        SET_FILTERS: {
          actions: 'setFilters',
        },
        SET_SORT: {
          actions: 'setSort',
        },
      },
    },
    {
      // ==================================================
      // Actions
      // ==================================================
      actions: {
        setQuery: assign({
          query: (_ctx, event) => {
            const e = event as { type: 'SET_QUERY'; query: string };
            return e.query;
          },
        }),

        updateFilter: assign({
          filters: (ctx, event) => {
            const e = event as { type: 'UPDATE_FILTER'; name: string; value: string };
            return { ...ctx.filters, [e.name]: e.value };
          },
        }),

        setFilters: assign({
          filters: (_ctx, event) => {
            const e = event as { type: 'SET_FILTERS'; filters: FilterValues };
            return e.filters;
          },
        }),

        setSort: assign((ctx, event) => {
          const e = event as { type: 'SET_SORT'; field?: string; direction?: string };
          return {
            sortField: e.field ?? ctx.sortField,
            sortDirection: e.direction ?? ctx.sortDirection,
          };
        }),

        changeFormat: assign((_ctx, event) => {
          const e = event as { type: 'SELECT_FORMAT'; format?: SearchFormat } | { type: 'RESET' };
          return {
            format: e.type === 'RESET' ? 'all' as SearchFormat : (e as { format: SearchFormat }).format,
            filters: {} as FilterValues,
          };
        }),

        clearResults: assign({
          results: [] as SearchResult[],
          totalCount: 0,
          error: undefined as string | undefined,
          continuationToken: undefined as string | undefined,
        }),

        resetQuery: assign({
          query: '',
          sortField: 'lastUpdated',
          sortDirection: 'desc',
        }),

        prepareSearch: assign({
          results: [] as SearchResult[],
          totalCount: 0,
          error: undefined as string | undefined,
          continuationToken: undefined as string | undefined,
        }),

        setResults: assign((_ctx, event) => {
          const data = (event as any).data;
          return {
            results: data.results as SearchResult[],
            totalCount: data.results.length as number,
            continuationToken: data.continuationToken as string | undefined,
            error: undefined as string | undefined,
          };
        }),

        appendResults: assign((ctx, event) => {
          const data = (event as any).data;
          return {
            results: [...ctx.results, ...data.results] as SearchResult[],
            totalCount: ctx.totalCount + data.results.length,
            continuationToken: data.continuationToken as string | undefined,
          };
        }),

        setError: assign({
          error: (_ctx, event) => {
            const err = (event as any).data;
            if (!err) return 'Search failed';
            return parseApiError(err).message;
          },
        }),
      },

      // ==================================================
      // Guards
      // ==================================================
      guards: {
        hasMore: (ctx) => Boolean(ctx.continuationToken),
        ...FORMAT_GUARDS,
      },

      // ==================================================
      // Services (override via useMachine options)
      // ==================================================
      services: {
        search: (ctx) => {
          const params = buildQueryParams(ctx.format, ctx.query, ctx.filters);
          // Default implementation uses fetch; override in hook for Axios
          return fetch(`/service/rest/v1/search?${params.toString()}`)
            .then((res) => res.json())
            .then((data) => ({
              results: (data.items || []).map(transformResult),
              continuationToken: data.continuationToken,
            }));
        },
        loadMore: (ctx) => {
          const params = buildQueryParams(ctx.format, ctx.query, ctx.filters);
          if (ctx.continuationToken) {
            params.set('continuationToken', ctx.continuationToken);
          }
          return fetch(`/service/rest/v1/search?${params.toString()}`)
            .then((res) => res.json())
            .then((data) => ({
              results: (data.items || []).map(transformResult),
              continuationToken: data.continuationToken,
            }));
        },
      },
    }
  );
}

// =============================================================================
// HELPERS
// =============================================================================

/**
 * Transform a raw API search item to a SearchResult.
 */
function transformResult(item: any): SearchResult {
  const primaryAsset = item.assets?.[0];
  return {
    id: item.id,
    name: item.name,
    format: item.format,
    repository: item.repository,
    group: item.group || undefined,
    version: item.version,
    downloadUrl: primaryAsset?.downloadUrl,
    path: primaryAsset?.path,
    lastUpdated: primaryAsset?.lastModified,
  };
}

/**
 * Get the active format's metadata from a machine state.
 * Reads from state.meta which contains metadata from all active state nodes.
 */
export function getActiveFormatMeta(state: any): FormatStateMeta | undefined {
  if (!state?.meta) return undefined;
  return Object.values(state.meta).find(
    (m: any) => m && typeof m === 'object' && 'formatId' in m
  ) as FormatStateMeta | undefined;
}

/**
 * All format IDs for test iteration.
 */
export const ALL_FORMATS: SearchFormat[] = Object.keys(FORMATS) as SearchFormat[];

/**
 * Formats that have custom filters (beyond just repository).
 */
export const FORMATS_WITH_CUSTOM_FILTERS: SearchFormat[] = ALL_FORMATS.filter((f) => {
  const config = FORMAT_FILTERS[f];
  return config.filters.some((filter: FilterDefinition) => !filter.global);
});

/**
 * Formats with only global filters (repository only).
 */
export const SIMPLE_FORMATS: SearchFormat[] = ALL_FORMATS.filter((f) => {
  const config = FORMAT_FILTERS[f];
  return config.filters.every((filter: FilterDefinition) => filter.global || filter.id === 'repository');
});
