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

import { useState, useCallback, useRef } from 'react';
import Axios from 'axios';
import type { GolangResult, GolangSearchFilters, GolangSearchState, GolangSearchResponse } from './golang.types';
import { mockGolangSearchApi } from './mockData';

// Feature flag to switch between real API and mock data
const USE_REAL_API = true;

/**
 * Raw search item from the API.
 */
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
  }>;
}

/**
 * Raw search response from the API.
 */
interface RawSearchResponse {
  items: RawSearchItem[];
  continuationToken?: string;
}

/**
 * Search Go modules using the real API.
 */
async function searchGolangApi(
  filters: GolangSearchFilters,
  continuationToken?: string
): Promise<GolangSearchResponse> {
  const queryParams = new URLSearchParams();
  queryParams.set('format', 'go');

  // Build query string from filters
  if (filters.module) {
    queryParams.set('go.module', filters.module);
  }
  if (filters.version) {
    queryParams.set('go.version', filters.version);
  }
  if (filters.keyword) {
    queryParams.set('q', filters.keyword);
  }
  if (filters.repository) {
    queryParams.set('repository', filters.repository);
  }
  if (continuationToken) {
    queryParams.set('continuationToken', continuationToken);
  }

  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get<RawSearchResponse>(url);

  // Aggregate results by module path
  const moduleMap = new Map<string, GolangResult>();

  for (const item of response.data.items) {
    // For Go modules, the name field contains the module path
    const modulePath = item.name;
    const moduleKey = modulePath;

    const existing = moduleMap.get(moduleKey);
    if (existing) {
      // Update existing entry
      const updatedResult: GolangResult = {
        ...existing,
        versionsCount: existing.versionsCount + 1,
        latestVersion: item.version > existing.latestVersion ? item.version : existing.latestVersion,
      };
      moduleMap.set(moduleKey, updatedResult);
    } else {
      // Create new entry
      moduleMap.set(moduleKey, {
        id: `go:${modulePath}`,
        module: modulePath,
        latestVersion: item.version,
        versionsCount: 1,
        repositoriesCount: 1,
        lastUpdated: new Date().toISOString(),
      });
    }
  }

  const items = Array.from(moduleMap.values());

  return {
    items,
    totalCount: items.length,
    continuationToken: response.data.continuationToken,
  };
}

/**
 * Hook return type.
 */
export interface UseGolangSearchReturn {
  /** Current search state */
  state: GolangSearchState;
  /** Execute a search with filters */
  search: (filters: GolangSearchFilters) => Promise<void>;
  /** Load more results */
  loadMore: () => Promise<void>;
  /** Clear search results */
  clear: () => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
}

const initialState: GolangSearchState = {
  filters: {},
  loading: false,
  error: undefined,
  results: [],
  totalCount: 0,
  continuationToken: undefined,
};

/**
 * React hook for Go module search state management.
 */
export function useGolangSearch(): UseGolangSearchReturn {
  const [state, setState] = useState<GolangSearchState>(initialState);
  const currentFiltersRef = useRef<GolangSearchFilters>({});

  /**
   * Execute a search with new filters.
   */
  const search = useCallback(async (filters: GolangSearchFilters): Promise<void> => {
    currentFiltersRef.current = filters;

    setState((prev) => ({
      ...prev,
      loading: true,
      error: undefined,
      filters,
      results: [],
      totalCount: 0,
      continuationToken: undefined,
    }));

    try {
      // Use real API or mock data based on feature flag
      const response = USE_REAL_API
        ? await searchGolangApi(filters)
        : await mockGolangSearchApi(filters);

      setState((prev) => ({
        ...prev,
        loading: false,
        results: response.items,
        totalCount: response.totalCount,
        continuationToken: response.continuationToken,
      }));
    } catch (error) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: error instanceof Error ? error.message : 'Search failed',
      }));
    }
  }, []);

  /**
   * Load more results using continuation token.
   */
  const loadMore = useCallback(async (): Promise<void> => {
    if (!state.continuationToken || state.loading) {
      return;
    }

    setState((prev) => ({
      ...prev,
      loading: true,
    }));

    try {
      // Use real API or mock data based on feature flag
      const response = USE_REAL_API
        ? await searchGolangApi(currentFiltersRef.current, state.continuationToken)
        : await mockGolangSearchApi({
            ...currentFiltersRef.current,
          });

      setState((prev) => ({
        ...prev,
        loading: false,
        results: [...prev.results, ...response.items],
        continuationToken: response.continuationToken,
      }));
    } catch (error) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: error instanceof Error ? error.message : 'Failed to load more',
      }));
    }
  }, [state.continuationToken, state.loading]);

  /**
   * Clear search results.
   */
  const clear = useCallback((): void => {
    currentFiltersRef.current = {};
    setState({
      filters: {},
      loading: false,
      error: undefined,
      results: [],
      totalCount: 0,
      continuationToken: undefined,
    });
  }, []);

  const hasMore = Boolean(state.continuationToken);

  return {
    state,
    search,
    loadMore,
    clear,
    hasMore,
  };
}

export default useGolangSearch;


