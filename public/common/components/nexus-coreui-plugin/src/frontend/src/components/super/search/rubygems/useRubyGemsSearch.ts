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
import type { RubyGemsResult, RubyGemsSearchFilters, RubyGemsSearchState, RubyGemsSearchResponse } from './rubygems.types';
import { mockRubyGemsSearchApi } from './mockData';

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
      rubygems?: {
        summary?: string;
        description?: string;
        authors?: string;
        licenses?: string[];
        platform?: string;
        homepage?: string;
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
 * Search RubyGems using the real API.
 * Endpoint: GET /service/rest/v1/search?format=rubygems
 */
async function searchRubyGemsApi(
  filters: RubyGemsSearchFilters,
  continuationToken?: string
): Promise<RubyGemsSearchResponse> {
  const queryParams = new URLSearchParams();
  queryParams.set('format', 'rubygems');

  // Build query string from filters
  if (filters.name) {
    queryParams.set('q', filters.name);
  }
  if (filters.version) {
    queryParams.set('version', filters.version);
  }
  if (filters.platform) {
    queryParams.set('rubygems.platform', filters.platform);
  }
  if (filters.repository) {
    queryParams.set('repository', filters.repository);
  }
  if (continuationToken) {
    queryParams.set('continuationToken', continuationToken);
  }

  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get<RawSearchResponse>(url);

  // Aggregate results by gem name
  const gemMap = new Map<string, RubyGemsResult>();

  for (const item of response.data.items) {
    const gemKey = item.name;

    // Extract RubyGems attributes from first asset (if available)
    const rubygemsAttrs = item.assets?.[0]?.attributes?.rubygems;

    const existing = gemMap.get(gemKey);
    if (existing) {
      // Update existing entry
      const updatedResult: RubyGemsResult = {
        ...existing,
        versionsCount: existing.versionsCount + 1,
        latestVersion: item.version > existing.latestVersion ? item.version : existing.latestVersion,
      };
      gemMap.set(gemKey, updatedResult);
    } else {
      // Create new entry
      gemMap.set(gemKey, {
        id: `rubygems:${item.name}`,
        name: item.name,
        displayName: item.name,
        latestVersion: item.version,
        versionsCount: 1,
        platform: rubygemsAttrs?.platform || 'ruby',
        summary: rubygemsAttrs?.summary,
        description: rubygemsAttrs?.description,
        authors: rubygemsAttrs?.authors,
        licenses: rubygemsAttrs?.licenses,
        homepage: rubygemsAttrs?.homepage,
        repositoriesCount: 1,
        lastUpdated: new Date().toISOString(),
      });
    }
  }

  const items = Array.from(gemMap.values());

  return {
    items,
    totalCount: items.length,
    continuationToken: response.data.continuationToken,
  };
}

/**
 * Hook return type.
 */
export interface UseRubyGemsSearchReturn {
  /** Current search state */
  state: RubyGemsSearchState;
  /** Execute a search with filters */
  search: (filters: RubyGemsSearchFilters) => Promise<void>;
  /** Load more results */
  loadMore: () => Promise<void>;
  /** Clear search results */
  clear: () => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
}

const initialState: RubyGemsSearchState = {
  filters: {},
  loading: false,
  error: undefined,
  results: [],
  totalCount: 0,
  continuationToken: undefined,
};

/**
 * React hook for RubyGems search state management.
 */
export function useRubyGemsSearch(): UseRubyGemsSearchReturn {
  const [state, setState] = useState<RubyGemsSearchState>(initialState);
  const currentFiltersRef = useRef<RubyGemsSearchFilters>({});

  /**
   * Execute a search with new filters.
   */
  const search = useCallback(async (filters: RubyGemsSearchFilters): Promise<void> => {
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
        ? await searchRubyGemsApi(filters)
        : await mockRubyGemsSearchApi(filters);

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
        ? await searchRubyGemsApi(currentFiltersRef.current, state.continuationToken)
        : await mockRubyGemsSearchApi({
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

export default useRubyGemsSearch;


