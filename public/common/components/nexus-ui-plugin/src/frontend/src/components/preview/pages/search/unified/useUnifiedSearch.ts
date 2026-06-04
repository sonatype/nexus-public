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
 * Unified Search Hook (XState-backed)
 *
 * State management via searchMachine. Replaces useState + useCallback + setTimeout
 * with a deterministic XState machine that handles format switching, search lifecycle,
 * and pagination via AbortController-based Axios services.
 *
 * The public interface (UseUnifiedSearchReturn) is unchanged — callers don't need changes.
 */

import { useRef, useEffect, useCallback, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import Axios from 'axios';
import type {
  SearchFormat,
  FilterValues,
  SearchResult,
  UnifiedSearchState,
  SortField,
  SortDirection,
} from './unified.types';
import { buildQueryParams, getPlaceholderForFormat, getApiFormat } from './searchFilters';
import { createSearchMachine } from './searchMachine';
import { isMockMode } from '../../../config/featureFlags';
import { getMockSearchResults } from '../../browse/mockData';

// Error name for cancelled Axios requests
const ABORT_ERROR_NAME = 'CanceledError';

/** Map SortField to API sort param (snake_case). Omit for relevance. */
const SORT_FIELD_TO_API: Record<string, string> = {
  lastUpdated: 'last_updated',
  name: 'name',
  version: 'version',
  repository: 'repository',
};

function addSortParams(
  params: URLSearchParams,
  sortField: string,
  sortDirection: string
): void {
  const apiSort = SORT_FIELD_TO_API[sortField];
  if (apiSort) {
    params.set('sort', apiSort);
    params.set('direction', sortDirection || 'asc');
  }
}

// =============================================================================
// API TYPES
// =============================================================================

interface RawSearchItem {
  id: string;
  repository: string;
  format: string;
  group: string | null;
  name: string;
  version: string;
  assets: Array<{
    id: string;
    path: string;
    downloadUrl: string;
    contentType?: string;
    lastModified?: string;
  }>;
}

interface RawSearchResponse {
  items: RawSearchItem[];
  continuationToken?: string;
}

// =============================================================================
// HOOK RETURN TYPE
// =============================================================================

export interface UseUnifiedSearchReturn {
  /** Current search state */
  state: UnifiedSearchState;
  /** Current placeholder text based on format */
  placeholder: string;
  /** Set the selected format */
  setFormat: (format: SearchFormat) => void;
  /** Set the search query */
  setQuery: (query: string) => void;
  /** Update a single filter value */
  setFilter: (filterId: string, value: string) => void;
  /** Set multiple filter values at once */
  setFilters: (filters: FilterValues) => void;
  /** Execute the search */
  search: () => Promise<void>;
  /** Load more results (pagination) */
  loadMore: () => Promise<void>;
  /** Reset all filters and results */
  reset: () => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
  /** Set sort field */
  setSortField: (field: SortField) => void;
  /** Set sort direction */
  setSortDirection: (direction: SortDirection) => void;
  /** Set sort field and direction together (e.g. when user selects from dropdown) */
  setSort: (field: SortField, direction: SortDirection) => void;
}

// =============================================================================
// TRANSFORM FUNCTIONS
// =============================================================================

function transformResult(item: RawSearchItem): SearchResult {
  const primaryAsset = item.assets[0];
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

// =============================================================================
// HOOK IMPLEMENTATION
// =============================================================================

export function useUnifiedSearch(): UseUnifiedSearchReturn {
  // AbortController ref for cancelling in-flight requests
  const abortControllerRef = useRef<AbortController | null>(null);

  // Create machine once (stable across renders)
  const machine = useMemo(() => createSearchMachine(), []);

  // Wire the machine with Axios-based service overrides
  const [machineState, send] = useMachine(machine, {
    services: {
      search: (ctx) => {
        // Cancel any in-flight request before starting a new one
        if (abortControllerRef.current) {
          abortControllerRef.current.abort();
        }
        const abortController = new AbortController();
        abortControllerRef.current = abortController;

        const searchQuery = ctx.query.trim();
        const nameOrVersion = ctx.filters['nameOrVersion']?.trim() || '';
        // Effective search: combine main query + name filter (matches buildQueryParams logic)
        const effectiveSearch = [searchQuery, nameOrVersion].filter(Boolean).join(' ').trim();

        if (isMockMode()) {
          const apiFormat =
            ctx.format !== 'all' ? getApiFormat(ctx.format) : undefined;
          const repositoryFilter = ctx.filters['repository'] || undefined;
          const { items, continuationToken } = getMockSearchResults(
            effectiveSearch,
            apiFormat,
            repositoryFilter,
          );
          return Promise.resolve({
            results: items.map(transformResult),
            continuationToken,
          });
        }

        const params = buildQueryParams(ctx.format, searchQuery, ctx.filters);
        addSortParams(params, ctx.sortField, ctx.sortDirection);
        const url = `/service/rest/v1/search?${params.toString()}`;

        return Axios.get<RawSearchResponse>(url, { signal: abortController.signal })
          .then((response) => ({
            results: response.data.items.map(transformResult),
            continuationToken: response.data.continuationToken,
          }))
          .catch((error) => {
            // Swallow AbortError — return empty results instead of propagating to onError.
            // XState ignores stale invocation results anyway, but this prevents
            // the machine from entering the error state on cancellation.
            if (error instanceof Error && error.name === ABORT_ERROR_NAME) {
              return { results: [] as SearchResult[], continuationToken: undefined };
            }
            throw error;
          });
      },

      loadMore: (ctx) => {
        // Cancel any in-flight request before starting a new one
        if (abortControllerRef.current) {
          abortControllerRef.current.abort();
        }
        const abortController = new AbortController();
        abortControllerRef.current = abortController;

        const searchQuery = ctx.query.trim();

        if (isMockMode()) {
          return Promise.resolve({
            results: [] as SearchResult[],
            continuationToken: undefined,
          });
        }

        const params = buildQueryParams(ctx.format, searchQuery, ctx.filters);
        addSortParams(params, ctx.sortField, ctx.sortDirection);
        if (ctx.continuationToken) {
          params.set('continuationToken', ctx.continuationToken);
        }
        const url = `/service/rest/v1/search?${params.toString()}`;

        return Axios.get<RawSearchResponse>(url, { signal: abortController.signal })
          .then((response) => ({
            results: response.data.items.map(transformResult),
            continuationToken: response.data.continuationToken,
          }))
          .catch((error) => {
            if (error instanceof Error && error.name === ABORT_ERROR_NAME) {
              return { results: [] as SearchResult[], continuationToken: undefined };
            }
            throw error;
          });
      },
    },
  });

  // Cleanup AbortController on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  // ---------------------------------------------------------------------------
  // Derive UI state from machine state + context
  // ---------------------------------------------------------------------------

  const state: UnifiedSearchState = {
    format: machineState.context.format,
    query: machineState.context.query,
    filters: machineState.context.filters,
    results: machineState.context.results,
    totalCount: machineState.context.totalCount,
    loading:
      machineState.matches({ lifecycle: 'searching' }) ||
      machineState.matches({ lifecycle: 'loadingMore' }),
    error: machineState.context.error,
    continuationToken: machineState.context.continuationToken,
    sortField: machineState.context.sortField as SortField,
    sortDirection: machineState.context.sortDirection as SortDirection,
  };

  const placeholder = getPlaceholderForFormat(machineState.context.format);
  const hasMore = Boolean(machineState.context.continuationToken);

  // ---------------------------------------------------------------------------
  // Event dispatchers (thin wrappers around send)
  // ---------------------------------------------------------------------------

  const setFormat = useCallback(
    (format: SearchFormat) => {
      send({ type: 'SELECT_FORMAT', format });
    },
    [send],
  );

  const setQuery = useCallback(
    (query: string) => {
      send({ type: 'SET_QUERY', query });
    },
    [send],
  );

  const setFilter = useCallback(
    (filterId: string, value: string) => {
      send({ type: 'UPDATE_FILTER', name: filterId, value });
    },
    [send],
  );

  const setFilters = useCallback(
    (filters: FilterValues) => {
      send({ type: 'SET_FILTERS', filters });
    },
    [send],
  );

  const setSortField = useCallback(
    (field: SortField) => {
      send({ type: 'SET_SORT', field });
    },
    [send],
  );

  const setSortDirection = useCallback(
    (direction: SortDirection) => {
      send({ type: 'SET_SORT', direction });
    },
    [send],
  );

  const setSort = useCallback(
    (field: SortField, direction: SortDirection) => {
      send({ type: 'SET_SORT', field, direction });
    },
    [send],
  );

  /**
   * Execute search. Sends the SEARCH event to the machine which invokes
   * the search service (Axios call with AbortController).
   * Pass overrideFormats when triggering from format change to avoid stale closure.
   */
  const search = useCallback(async (): Promise<void> => {
    send({ type: 'SEARCH' });
  }, [send]);

  /**
   * Load more results via continuation token. The machine's hasMore guard
   * prevents the transition if there's no continuation token.
   */
  const loadMore = useCallback(
    async (): Promise<void> => {
      send({ type: 'LOAD_MORE' });
    },
    [send],
  );

  /**
   * Reset all state — aborts any in-flight request and sends RESET event
   * which returns the machine to idle with default context.
   */
  const reset = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    send({ type: 'RESET' });
  }, [send]);

  return {
    state,
    placeholder,
    setFormat,
    setQuery,
    setFilter,
    setFilters,
    search,
    loadMore,
    reset,
    hasMore,
    setSortField,
    setSortDirection,
    setSort,
  };
}

export default useUnifiedSearch;
