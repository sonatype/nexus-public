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
import type { AptResult, AptSearchFilters, AptSearchState, AptSearchResponse } from './apt.types';
import { mockAptSearchApi } from './mockData';

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
    attributes?: {
      apt?: {
        architecture?: string;
        distribution?: string;
        component?: string;
        description?: string;
        maintainer?: string;
        section?: string;
        priority?: string;
        installedSize?: number;
      };
    };
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
 * Search Apt packages using the real API.
 * Endpoint: GET /service/rest/v1/search?format=apt
 */
async function searchAptApi(
  filters: AptSearchFilters,
  continuationToken?: string
): Promise<AptSearchResponse> {
  const queryParams = new URLSearchParams();
  queryParams.set('format', 'apt');

  // Build query string from filters
  if (filters.name) {
    queryParams.set('q', filters.name);
  }
  if (filters.version) {
    queryParams.set('version', filters.version);
  }
  if (filters.architecture) {
    queryParams.set('apt.architecture', filters.architecture);
  }
  if (filters.distribution) {
    queryParams.set('apt.distribution', filters.distribution);
  }
  if (filters.component) {
    queryParams.set('apt.component', filters.component);
  }
  if (filters.repository) {
    queryParams.set('repository', filters.repository);
  }
  if (continuationToken) {
    queryParams.set('continuationToken', continuationToken);
  }

  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get<RawSearchResponse>(url);

  // Aggregate results by package name
  const packageMap = new Map<string, AptResult>();

  for (const item of response.data.items) {
    const packageKey = item.name;

    // Extract Apt attributes from first asset (if available)
    const aptAttrs = item.assets?.[0]?.attributes?.apt;

    const existing = packageMap.get(packageKey);
    if (existing) {
      // Update existing entry
      const updatedResult: AptResult = {
        ...existing,
        versionsCount: existing.versionsCount + 1,
        latestVersion: item.version > existing.latestVersion ? item.version : existing.latestVersion,
      };
      packageMap.set(packageKey, updatedResult);
    } else {
      // Create new entry
      packageMap.set(packageKey, {
        id: `apt:${item.name}`,
        name: item.name,
        displayName: item.name,
        latestVersion: item.version,
        versionsCount: 1,
        architecture: aptAttrs?.architecture || 'all',
        distribution: aptAttrs?.distribution,
        component: aptAttrs?.component,
        description: aptAttrs?.description,
        maintainer: aptAttrs?.maintainer,
        section: aptAttrs?.section,
        priority: aptAttrs?.priority,
        installedSize: aptAttrs?.installedSize,
        repositoriesCount: 1,
        lastUpdated: new Date().toISOString(),
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
 * Hook return type.
 */
export interface UseAptSearchReturn {
  /** Current search state */
  state: AptSearchState;
  /** Execute a search with filters */
  search: (filters: AptSearchFilters) => Promise<void>;
  /** Load more results */
  loadMore: () => Promise<void>;
  /** Clear search results */
  clear: () => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
}

const initialState: AptSearchState = {
  filters: {},
  loading: false,
  error: undefined,
  results: [],
  totalCount: 0,
  continuationToken: undefined,
};

/**
 * React hook for Apt search state management.
 */
export function useAptSearch(): UseAptSearchReturn {
  const [state, setState] = useState<AptSearchState>(initialState);
  const currentFiltersRef = useRef<AptSearchFilters>({});

  /**
   * Execute a search with new filters.
   */
  const search = useCallback(async (filters: AptSearchFilters): Promise<void> => {
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
        ? await searchAptApi(filters)
        : await mockAptSearchApi(filters);

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
        ? await searchAptApi(currentFiltersRef.current, state.continuationToken)
        : await mockAptSearchApi(currentFiltersRef.current);

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

  const hasMore = Boolean(state.continuationToken);

  return {
    state,
    search,
    loadMore,
    clear,
    hasMore,
  };
}

export default useAptSearch;


