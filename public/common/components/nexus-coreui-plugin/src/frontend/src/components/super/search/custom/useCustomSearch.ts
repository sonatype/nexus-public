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
  CustomFilter,
  CustomSearchResult,
  CustomSearchState,
  FilterField,
  FilterOperator,
} from './custom.types';
import { createEmptyFilter } from './custom.types';
import { mockCustomSearchApi } from './mockData';

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
 * Map filter field to API parameter name.
 */
function getApiParamName(field: FilterField): string {
  switch (field) {
    case 'keyword':
      return 'q';
    case 'format':
      return 'format';
    case 'repository':
      return 'repository';
    case 'group':
      return 'group';
    case 'name':
      return 'name';
    case 'version':
      return 'version';
    case 'tag':
      return 'tag';
    default:
      return field;
  }
}

/**
 * Build search value based on operator.
 * The Nexus search API supports wildcards (*) for partial matching.
 */
function buildSearchValue(operator: FilterOperator, value: string): string {
  switch (operator) {
    case 'equals':
      return value;
    case 'contains':
      return `*${value}*`;
    case 'startsWith':
      return `${value}*`;
    case 'endsWith':
      return `*${value}`;
    default:
      return value;
  }
}

/**
 * Search using the real Nexus API.
 */
async function searchCustomApi(
  filters: CustomFilter[],
  continuationToken?: string
): Promise<{ items: CustomSearchResult[]; totalCount: number; continuationToken?: string }> {
  // Build query string manually to avoid URL encoding of wildcards
  const params: string[] = [];

  // Apply each filter
  for (const filter of filters) {
    if (filter.value.trim()) {
      const paramName = getApiParamName(filter.field);
      const paramValue = buildSearchValue(filter.operator, filter.value.trim());
      // Encode the value but preserve wildcards (*)
      const encodedValue = encodeURIComponent(paramValue).replace(/%2A/g, '*');
      params.push(`${paramName}=${encodedValue}`);
    }
  }

  if (continuationToken) {
    params.push(`continuationToken=${encodeURIComponent(continuationToken)}`);
  }

  const url = `/service/rest/v1/search?${params.join('&')}`;
  const response = await Axios.get<RawSearchResponse>(url);

  // Transform raw results to CustomSearchResult format
  const results: CustomSearchResult[] = response.data.items.map((item) => ({
    id: item.id,
    format: item.format,
    repository: item.repository,
    group: item.group || undefined,
    name: item.name,
    version: item.version,
    path: item.assets[0]?.path,
  }));

  return {
    items: results,
    totalCount: results.length, // API doesn't return total, use items length
    continuationToken: response.data.continuationToken,
  };
}

/**
 * Hook return type for custom search.
 */
export interface UseCustomSearchReturn {
  /** Current search state */
  state: CustomSearchState;
  /** Add a new empty filter row */
  addFilter: () => void;
  /** Remove a filter by ID */
  removeFilter: (id: string) => void;
  /** Update a filter field */
  updateFilter: (id: string, updates: Partial<CustomFilter>) => void;
  /** Execute the search */
  search: () => Promise<void>;
  /** Load more results */
  loadMore: () => Promise<void>;
  /** Clear all filters and results */
  clear: () => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
  /** Whether any filters have values */
  hasFilters: boolean;
}

const initialState: CustomSearchState = {
  filters: [createEmptyFilter()],
  results: [],
  totalCount: 0,
  loading: false,
  error: undefined,
  continuationToken: undefined,
};

/**
 * React hook for custom search state management.
 *
 * Features:
 * - Dynamic filter management (add/remove/update)
 * - Search execution with current filters
 * - Pagination support
 * - Loading and error states
 */
export function useCustomSearch(): UseCustomSearchReturn {
  const [state, setState] = useState<CustomSearchState>(initialState);

  // Track current filters for pagination
  const currentFiltersRef = useRef<readonly CustomFilter[]>([]);

  /**
   * Add a new empty filter row.
   */
  const addFilter = useCallback((): void => {
    setState((prev) => ({
      ...prev,
      filters: [...prev.filters, createEmptyFilter()],
    }));
  }, []);

  /**
   * Remove a filter by ID.
   * Always keeps at least one filter.
   */
  const removeFilter = useCallback((id: string): void => {
    setState((prev) => {
      const newFilters = prev.filters.filter((f) => f.id !== id);
      // Always keep at least one filter
      if (newFilters.length === 0) {
        return {
          ...prev,
          filters: [createEmptyFilter()],
        };
      }
      return {
        ...prev,
        filters: newFilters,
      };
    });
  }, []);

  /**
   * Update a filter's properties.
   */
  const updateFilter = useCallback(
    (id: string, updates: Partial<CustomFilter>): void => {
      setState((prev) => ({
        ...prev,
        filters: prev.filters.map((f) =>
          f.id === id ? { ...f, ...updates } : f
        ),
      }));
    },
    []
  );

  /**
   * Execute the search with current filters.
   */
  const search = useCallback(async (): Promise<void> => {
    // Save filters for pagination
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
      // Filter out empty filters
      const validFilters = state.filters.filter((f) => f.value.trim() !== '');

      const response = USE_REAL_API
        ? await searchCustomApi(validFilters)
        : await mockCustomSearchApi({ filters: validFilters });

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
      const validFilters = currentFiltersRef.current.filter(
        (f) => f.value.trim() !== ''
      );

      const response = USE_REAL_API
        ? await searchCustomApi(validFilters, state.continuationToken)
        : await mockCustomSearchApi({
            filters: validFilters,
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
   * Clear all filters and results.
   */
  const clear = useCallback((): void => {
    currentFiltersRef.current = [];
    // Create fresh state with new filter to ensure React re-renders
    setState({
      filters: [createEmptyFilter()],
      results: [],
      totalCount: 0,
      loading: false,
      error: undefined,
      continuationToken: undefined,
    });
  }, []);

  const hasMore = Boolean(state.continuationToken);
  const hasFilters = state.filters.some((f) => f.value.trim() !== '');

  return {
    state,
    addFilter,
    removeFilter,
    updateFilter,
    search,
    loadMore,
    clear,
    hasMore,
    hasFilters,
  };
}

export default useCustomSearch;

