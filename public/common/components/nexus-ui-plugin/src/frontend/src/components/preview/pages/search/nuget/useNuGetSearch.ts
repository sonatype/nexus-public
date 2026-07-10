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
import type { NuGetSearchState, NuGetSearchFilters, NuGetResult, NuGetSearchResponse } from './nuget.types';
import { mockNuGetSearchApi } from './mockData';

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
    lastModified?: string;
  }>;
}

/**
 * Raw search response from the API.
 */
interface RawSearchResponse {
  items: RawSearchItem[];
  continuationToken?: string;
}

const toMs = (s: string): number => (s ? new Date(s).getTime() : 0);

/**
 * Search NuGet packages using the real API.
 */
async function searchNuGetApi(
  params: NuGetSearchParams,
  continuationToken?: string
): Promise<NuGetSearchResponse> {
  const queryParams = new URLSearchParams();
  queryParams.set('format', 'nuget');

  // Build query string from params
  if (params.query) {
    queryParams.set('q', params.query);
  }
  if (params.packageId) {
    queryParams.set('nuget.id', params.packageId);
  }
  if (params.version) {
    queryParams.set('version', params.version);
  }
  if (continuationToken) {
    queryParams.set('continuationToken', continuationToken);
  }

  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get<RawSearchResponse>(url);

  // Aggregate results by package ID
  const packageMap = new Map<string, NuGetResult>();

  for (const item of response.data.items) {
    const packageId = item.name;
    const packageKey = packageId.toLowerCase();

    const existing = packageMap.get(packageKey);
    const itemLastModified = item.assets?.reduce((max, asset) => {
      const t = asset.lastModified ?? '';
      return toMs(t) > toMs(max) ? t : max;
    }, '') ?? '';

    if (existing) {
      packageMap.set(packageKey, {
        ...existing,
        versionsCount: existing.versionsCount + 1,
        latestVersion: item.version > existing.latestVersion ? item.version : existing.latestVersion,
        lastUpdated: toMs(itemLastModified) > toMs(existing.lastUpdated) ? itemLastModified : existing.lastUpdated,
      });
    } else {
      packageMap.set(packageKey, {
        id: `nuget:${packageId}`,
        packageId,
        displayName: packageId,
        latestVersion: item.version,
        versionsCount: 1,
        repositoriesCount: 1,
        lastUpdated: itemLastModified,
      });
    }
  }

  const items = Array.from(packageMap.values());

  return {
    items,
    totalCount: items.length,
    continuationToken: response.data.continuationToken,
  };
}

/**
 * Search parameters for NuGet.
 */
export interface NuGetSearchParams {
  query?: string;
  packageId?: string;
  version?: string;
  prerelease?: boolean;
  targetFramework?: string;
  sort?: 'relevance' | 'downloads' | 'recent';
  sortDirection?: 'asc' | 'desc';
  continuationToken?: string;
}

/**
 * Hook return type.
 */
export interface UseNuGetSearchReturn {
  state: NuGetSearchState;
  search: (params: NuGetSearchParams) => Promise<void>;
  loadMore: () => Promise<void>;
  clear: () => void;
  setSort: (sort: 'relevance' | 'downloads' | 'recent', direction: 'asc' | 'desc') => void;
  hasMore: boolean;
}

const initialState: NuGetSearchState = {
  query: '',
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
 * React hook for NuGet search state management.
 */
export function useNuGetSearch(initialParams?: NuGetSearchParams): UseNuGetSearchReturn {
  const [state, setState] = useState<NuGetSearchState>(() => ({
    ...initialState,
    query: initialParams?.query ?? '',
    sort: initialParams?.sort ?? 'relevance',
    sortDirection: initialParams?.sortDirection ?? 'desc',
  }));

  const currentParamsRef = useRef<NuGetSearchParams>({});

  /**
   * Execute a search.
   */
  const search = useCallback(async (params: NuGetSearchParams): Promise<void> => {
    currentParamsRef.current = params;

    setState((prev) => ({
      ...prev,
      loading: true,
      error: undefined,
      query: params.query ?? '',
      results: [],
      totalCount: 0,
      continuationToken: undefined,
    }));

    try {
      // Use real API or mock data based on feature flag
      const response = USE_REAL_API
        ? await searchNuGetApi(params)
        : await mockNuGetSearchApi({
            query: params.query,
            packageId: params.packageId,
            version: params.version,
            prerelease: params.prerelease,
            targetFramework: params.targetFramework,
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
   * Load more results.
   */
  const loadMore = useCallback(async (): Promise<void> => {
    if (!state.continuationToken || state.loading) {
      return;
    }

    setState((prev) => ({ ...prev, loading: true }));

    try {
      // Use real API or mock data based on feature flag
      const response = USE_REAL_API
        ? await searchNuGetApi(currentParamsRef.current, state.continuationToken)
        : await mockNuGetSearchApi({
            ...currentParamsRef.current,
            continuationToken: state.continuationToken,
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
   * Clear search.
   */
  const clear = useCallback((): void => {
    currentParamsRef.current = {};
    setState(initialState);
  }, []);

  /**
   * Update sort.
   */
  const setSort = useCallback(
    (sort: 'relevance' | 'downloads' | 'recent', direction: 'asc' | 'desc'): void => {
      setState((prev) => ({ ...prev, sort, sortDirection: direction }));
      
      if (currentParamsRef.current.query || currentParamsRef.current.packageId) {
        search({ ...currentParamsRef.current, sort, sortDirection: direction });
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

export default useNuGetSearch;
