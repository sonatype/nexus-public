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
import type {
  GenericResult,
  GenericSearchFilters,
  GenericSearchResponse,
  GenericSearchState,
} from './generic.types';
import { mockGenericSearchApi } from './mockData';

// Feature flag to switch between real API and mock data
const USE_REAL_API = true;

/**
 * Hook return type.
 */
export interface UseGenericSearchReturn {
  /** Current search state */
  state: GenericSearchState;
  /** Execute a search with filters */
  search: (filters: GenericSearchFilters) => Promise<void>;
  /** Load more results */
  loadMore: () => Promise<void>;
  /** Clear search results */
  clear: () => void;
  /** Update sort settings */
  setSort: (sort: 'relevance' | 'lastUpdated' | 'name', direction: 'asc' | 'desc') => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
}

const initialState: GenericSearchState = {
  filters: {},
  sort: 'relevance',
  sortDirection: 'desc',
  loading: false,
  error: undefined,
  results: [],
  totalCount: 0,
  continuationToken: undefined,
};

/**
 * Raw search response from the API.
 */
interface RawSearchResponse {
  items: Array<{
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
      checksum?: {
        sha1?: string;
        sha256?: string;
        md5?: string;
      };
      contentType?: string;
      lastModified?: string;
    }>;
  }>;
  continuationToken?: string;
}

/**
 * Call the real search API.
 */
async function fetchGenericSearch(
  filters: GenericSearchFilters,
  continuationToken?: string
): Promise<GenericSearchResponse> {
  const queryParams = new URLSearchParams();

  // Add filters to query params
  if (filters.q) {
    queryParams.set('q', filters.q);
  }
  if (filters.format) {
    queryParams.set('format', filters.format);
  }
  if (filters.repository) {
    queryParams.set('repository', filters.repository);
  }
  if (filters.group) {
    queryParams.set('group', filters.group);
  }
  if (filters.name) {
    queryParams.set('name', filters.name);
  }
  if (filters.version) {
    queryParams.set('version', filters.version);
  }
  if (continuationToken) {
    queryParams.set('continuationToken', continuationToken);
  }

  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get<RawSearchResponse>(url);

  // Transform raw response to typed response
  const items: GenericResult[] = response.data.items.map((item) => ({
    id: `${item.format}:${item.group ? item.group + ':' : ''}${item.name}:${item.version}`,
    format: item.format,
    repository: item.repository,
    group: item.group,
    name: item.name,
    version: item.version,
    displayName: item.group ? `${item.group}:${item.name}` : item.name,
    assets: item.assets.map((asset) => ({
      id: asset.id,
      path: asset.path,
      downloadUrl: asset.downloadUrl,
      checksum: asset.checksum,
      contentType: asset.contentType,
      lastModified: asset.lastModified,
    })),
  }));

  return {
    items,
    totalCount: items.length,
    continuationToken: response.data.continuationToken,
  };
}

/**
 * React hook for generic search state management.
 */
export function useGenericSearch(initialFilters?: GenericSearchFilters): UseGenericSearchReturn {
  const [state, setState] = useState<GenericSearchState>(() => ({
    ...initialState,
    filters: initialFilters ?? {},
  }));

  const currentFiltersRef = useRef<GenericSearchFilters>({});

  /**
   * Execute a search with new filters.
   */
  const search = useCallback(async (filters: GenericSearchFilters): Promise<void> => {
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
      const response = USE_REAL_API
        ? await fetchGenericSearch(filters)
        : await mockGenericSearchApi(filters);

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
      const response = USE_REAL_API
        ? await fetchGenericSearch(currentFiltersRef.current, state.continuationToken)
        : await mockGenericSearchApi(currentFiltersRef.current);

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
    setState(initialState);
  }, []);

  /**
   * Update sort settings.
   */
  const setSort = useCallback(
    (sort: 'relevance' | 'lastUpdated' | 'name', direction: 'asc' | 'desc'): void => {
      setState((prev) => ({ ...prev, sort, sortDirection: direction }));

      // Re-search with current filters
      if (Object.keys(currentFiltersRef.current).length > 0) {
        search(currentFiltersRef.current);
      }
    },
    [search]
  );

  const hasMore = Boolean(state.continuationToken);

  return {
    state,
    search,
    loadMore,
    clear,
    setSort,
    hasMore,
  };
}

export default useGenericSearch;


