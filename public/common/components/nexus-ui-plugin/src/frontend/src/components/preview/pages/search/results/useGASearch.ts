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

import { useState, useCallback, useEffect, useRef } from 'react';
import type {
  GAResult,
  GASearchRequest,
  GASearchResponse,
  GASearchState,
} from '../core';
import { searchMavenGA } from '../core/searchApi';
import { mockSearchApi } from './mockData';

// Feature flag to switch between real API and mock data
const USE_REAL_API = true;

/**
 * Search parameters that can be set via URL or programmatically.
 */
export interface SearchParams {
  query?: string;
  groupId?: string;
  artifactId?: string;
  repository?: string;
  sort?: 'relevance' | 'lastUpdated' | 'name';
  sortDirection?: 'asc' | 'desc';
}

/**
 * Hook return type.
 */
export interface UseGASearchReturn {
  /** Current search state */
  state: GASearchState;
  /** Execute a search with new parameters */
  search: (params: SearchParams) => Promise<void>;
  /** Load more results (pagination) */
  loadMore: () => Promise<void>;
  /** Clear search results */
  clear: () => void;
  /** Update sort settings */
  setSort: (sort: 'relevance' | 'lastUpdated' | 'name', direction: 'asc' | 'desc') => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
}

const initialState: GASearchState = {
  query: '',
  format: 'maven',
  sort: 'relevance',
  sortDirection: 'desc',
  loading: false,
  error: undefined,
  results: [],
  totalCount: 0,
  continuationToken: undefined,
};

/**
 * React hook for GA search state management.
 * 
 * Features:
 * - URL-driven state (reads/writes query params)
 * - Pagination via continuation token
 * - Loading and error states
 * - Sort management
 * 
 * @param initialParams - Initial search parameters (usually from URL)
 */
export function useGASearch(initialParams?: SearchParams): UseGASearchReturn {
  const [state, setState] = useState<GASearchState>(() => ({
    ...initialState,
    query: initialParams?.query ?? '',
    sort: initialParams?.sort ?? 'relevance',
    sortDirection: initialParams?.sortDirection ?? 'desc',
  }));

  // Track current search params for the continuation token
  const currentParamsRef = useRef<SearchParams>({});

  /**
   * Execute a search with new parameters.
   */
  const search = useCallback(async (params: SearchParams): Promise<void> => {
    // Save current params for pagination
    currentParamsRef.current = params;

    setState((prev) => ({
      ...prev,
      loading: true,
      error: undefined,
      query: params.query ?? '',
      // Reset results for new search
      results: [],
      totalCount: 0,
      continuationToken: undefined,
    }));

    try {
      // Use real API or mock data based on feature flag
      const response = USE_REAL_API
        ? await searchMavenGA({
            query: params.query,
            groupId: params.groupId,
            artifactId: params.artifactId,
            repository: params.repository,
          })
        : await mockSearchApi({
            query: params.query,
            groupId: params.groupId,
            artifactId: params.artifactId,
            repository: params.repository,
          });

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
        ? await searchMavenGA({
            ...currentParamsRef.current,
            continuationToken: state.continuationToken,
          })
        : await mockSearchApi({
            ...currentParamsRef.current,
            continuationToken: state.continuationToken,
          });

      setState((prev) => ({
        ...prev,
        loading: false,
        // Append new results to existing
        results: [...prev.results, ...response.items],
        continuationToken: response.continuationToken,
      }));
    } catch (error) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: error instanceof Error ? error.message : 'Failed to load more results',
      }));
    }
  }, [state.continuationToken, state.loading]);

  /**
   * Clear search results.
   */
  const clear = useCallback((): void => {
    currentParamsRef.current = {};
    setState(initialState);
  }, []);

  /**
   * Update sort settings.
   */
  const setSort = useCallback(
    (sort: 'relevance' | 'lastUpdated' | 'name', direction: 'asc' | 'desc'): void => {
      setState((prev) => ({
        ...prev,
        sort,
        sortDirection: direction,
      }));

      // Re-search with new sort if we have results
      if (currentParamsRef.current.query || currentParamsRef.current.groupId || currentParamsRef.current.artifactId) {
        search({
          ...currentParamsRef.current,
          sort,
          sortDirection: direction,
        });
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

export default useGASearch;

