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
import type { RawResult, RawSearchFilters, RawSearchState, RawSearchResponse } from './raw.types';
import { mockRawSearchApi } from './mockData';

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
    contentType?: string;
    fileSize?: number;
    lastModified?: string;
    checksum?: {
      sha1?: string;
      sha256?: string;
      sha512?: string;
      md5?: string;
    };
  }>;
}

/**
 * Raw search response from the API.
 */
interface RawApiResponse {
  items: RawSearchItem[];
  continuationToken?: string;
}

/**
 * Search raw assets using the real API.
 */
async function searchRawApi(
  filters: RawSearchFilters,
  continuationToken?: string
): Promise<RawSearchResponse> {
  const queryParams = new URLSearchParams();
  queryParams.set('format', 'raw');

  // Build query string from filters
  if (filters.keyword) {
    queryParams.set('q', filters.keyword);
  }
  if (filters.name) {
    queryParams.set('name', filters.name);
  }
  if (filters.group) {
    queryParams.set('group', filters.group);
  }
  if (filters.repository) {
    queryParams.set('repository', filters.repository);
  }
  if (continuationToken) {
    queryParams.set('continuationToken', continuationToken);
  }

  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get<RawApiResponse>(url);

  // Transform raw results to RawResult format
  const results: RawResult[] = [];

  for (const item of response.data.items) {
    // Raw format typically has one asset per item
    const asset = item.assets[0];
    if (asset) {
      results.push({
        id: item.id,
        path: asset.path,
        name: item.name,
        group: item.group || undefined,
        repository: item.repository,
        contentType: asset.contentType,
        size: asset.fileSize,
        lastModified: asset.lastModified,
        downloadUrl: asset.downloadUrl,
        checksums: asset.checksum,
      });
    }
  }

  return {
    items: results,
    totalCount: results.length,
    continuationToken: response.data.continuationToken,
  };
}

const initialState: RawSearchState = {
  filters: {},
  results: [],
  totalCount: 0,
  loading: false,
  error: undefined,
  continuationToken: undefined,
};

/**
 * Hook return type for raw search.
 */
export interface UseRawSearchReturn {
  /** Current search state */
  state: RawSearchState;
  /** Update filter values */
  setFilters: (filters: Partial<RawSearchFilters>) => void;
  /** Execute the search */
  search: () => Promise<void>;
  /** Load more results */
  loadMore: () => Promise<void>;
  /** Clear all filters and results */
  clear: () => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
}

/**
 * React hook for raw search state management.
 */
export function useRawSearch(): UseRawSearchReturn {
  const [state, setState] = useState<RawSearchState>(initialState);

  // Track current filters for pagination
  const currentFiltersRef = useRef<RawSearchFilters>({});

  /**
   * Update filter values.
   */
  const setFilters = useCallback((filters: Partial<RawSearchFilters>): void => {
    setState((prev) => ({
      ...prev,
      filters: { ...prev.filters, ...filters },
    }));
  }, []);

  /**
   * Execute the search with current filters.
   */
  const search = useCallback(async (): Promise<void> => {
    currentFiltersRef.current = state.filters;

    setState((prev) => ({
      ...prev,
      loading: true,
      error: undefined,
      results: [],
      totalCount: 0,
      continuationToken: undefined,
    }));

    try {
      const response = USE_REAL_API
        ? await searchRawApi(state.filters)
        : await mockRawSearchApi(state.filters);

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
  }, [state.filters]);

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
        ? await searchRawApi(currentFiltersRef.current, state.continuationToken)
        : await mockRawSearchApi(currentFiltersRef.current, state.continuationToken);

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
   * Clear all filters and results.
   */
  const clear = useCallback((): void => {
    currentFiltersRef.current = {};
    setState(initialState);
  }, []);

  const hasMore = Boolean(state.continuationToken);

  return {
    state,
    setFilters,
    search,
    loadMore,
    clear,
    hasMore,
  };
}

export default useRawSearch;


