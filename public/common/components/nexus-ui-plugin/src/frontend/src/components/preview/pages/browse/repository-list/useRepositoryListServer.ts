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

import { useState, useEffect, useCallback, useRef } from 'react';
import Axios from 'axios';
import {
  RepositoryReference,
  RepositoryFilterParams,
  RepositoryPageResponse,
  UseRepositoryListResult,
} from '../browse.types';
import { isMockMode } from '../../../config/featureFlags';
import { getMockRepositoryPageResponse } from '../mockData';

/**
 * API endpoint for server-side filtered repository list.
 * Supports pagination, filtering by format/type/status, and sorting.
 */
const FILTERED_REPOSITORIES_API = '/service/rest/internal/ui/repositories/details/filtered';

/**
 * Default page size.
 */
const DEFAULT_PAGE_SIZE = 50;

/**
 * Default filter parameters.
 */
const DEFAULT_FILTER_PARAMS: RepositoryFilterParams = {
  formats: undefined,
  types: undefined,
  statuses: undefined,
  nameFilter: undefined,
  sortField: 'name',
  sortDirection: 'asc',
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
};

/**
 * Build URL query string from filter parameters.
 */
function buildQueryString(params: RepositoryFilterParams): string {
  const queryParts: string[] = [];

  if (params.formats) {
    queryParts.push(`formats=${encodeURIComponent(params.formats)}`);
  }
  if (params.types) {
    queryParts.push(`types=${encodeURIComponent(params.types)}`);
  }
  if (params.statuses) {
    queryParts.push(`statuses=${encodeURIComponent(params.statuses)}`);
  }
  if (params.nameFilter) {
    queryParts.push(`nameFilter=${encodeURIComponent(params.nameFilter)}`);
  }
  if (params.sortField) {
    queryParts.push(`sortField=${encodeURIComponent(params.sortField)}`);
  }
  if (params.sortDirection) {
    queryParts.push(`sortDirection=${encodeURIComponent(params.sortDirection)}`);
  }
  if (params.page) {
    queryParts.push(`page=${params.page}`);
  }
  if (params.pageSize) {
    queryParts.push(`pageSize=${params.pageSize}`);
  }

  return queryParts.length > 0 ? `?${queryParts.join('&')}` : '';
}

/**
 * Hook for server-side filtered repository list.
 * 
 * Features:
 * - Server-side filtering by format, type, status, name
 * - Server-side pagination
 * - Server-side sorting
 * - Debounced name filter
 * - Enterprise-scalable (only fetches matching data)
 * 
 * @param initialParams Initial filter parameters
 * @returns Repository list state and controls
 */
export function useRepositoryListServer(
  initialParams: Partial<RepositoryFilterParams> = {}
): UseRepositoryListResult {
  // Merge initial params with defaults
  const [filterParams, setFilterParamsState] = useState<RepositoryFilterParams>({
    ...DEFAULT_FILTER_PARAMS,
    ...initialParams,
  });

  // State
  const [repositories, setRepositories] = useState<RepositoryReference[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);

  // Debounce timer ref for name filter
  const debounceTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Fetch repositories from server
  const fetchRepositories = useCallback(async (params: RepositoryFilterParams) => {
    setLoading(true);
    setError(null);

    try {
      if (isMockMode()) {
        const pageData = getMockRepositoryPageResponse({
          page: params.page,
          pageSize: params.pageSize,
          formats: params.formats,
          nameFilter: params.nameFilter,
        });
        setRepositories(pageData.data ?? []);
        setTotalCount(pageData.totalCount ?? 0);
        setTotalPages(pageData.totalPages ?? 0);
        return;
      }

      const queryString = buildQueryString(params);
      const response = await Axios.get<RepositoryPageResponse>(
        `${FILTERED_REPOSITORIES_API}${queryString}`
      );

      const pageData = response.data;
      setRepositories(pageData.data ?? []);
      setTotalCount(pageData.totalCount ?? 0);
      setTotalPages(pageData.totalPages ?? 0);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load repositories';
      setError(errorMessage);
      setRepositories([]);
      setTotalCount(0);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial fetch
  useEffect(() => {
    fetchRepositories(filterParams);
  }, [fetchRepositories, filterParams]); // Only on mount

  // Update filter params with debounce for name filter
  const setFilterParams = useCallback((newParams: Partial<RepositoryFilterParams>) => {
    setFilterParamsState((prev) => {
      const updated = { ...prev, ...newParams };

      // Reset to page 1 when filters change (except page itself)
      if (!('page' in newParams)) {
        updated.page = 1;
      }

      // Debounce name filter changes
      if ('nameFilter' in newParams) {
        if (debounceTimerRef.current) {
          clearTimeout(debounceTimerRef.current);
        }
        debounceTimerRef.current = setTimeout(() => {
          fetchRepositories(updated);
        }, 300); // 300ms debounce
      } else {
        // Immediate fetch for other filter changes
        fetchRepositories(updated);
      }

      return updated;
    });
  }, [fetchRepositories]);

  // Pagination controls
  const nextPage = useCallback(() => {
    if (filterParams.page && filterParams.page < totalPages) {
      setFilterParams({ page: filterParams.page + 1 });
    }
  }, [filterParams.page, totalPages, setFilterParams]);

  const previousPage = useCallback(() => {
    if (filterParams.page && filterParams.page > 1) {
      setFilterParams({ page: filterParams.page - 1 });
    }
  }, [filterParams.page, setFilterParams]);

  const goToPage = useCallback((page: number) => {
    if (page >= 1 && page <= totalPages) {
      setFilterParams({ page });
    }
  }, [totalPages, setFilterParams]);

  // Retry
  const retry = useCallback(() => {
    fetchRepositories(filterParams);
  }, [fetchRepositories, filterParams]);

  // Cleanup debounce timer on unmount
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  return {
    repositories,
    loading,
    error,
    filterParams,
    setFilterParams,
    totalCount,
    page: filterParams.page ?? 1,
    totalPages,
    nextPage,
    previousPage,
    goToPage,
    retry,
  };
}

/**
 * Convert multi-select array to comma-separated string for API.
 */
export function arrayToFilterString(arr: string[]): string | undefined {
  return arr.length > 0 ? arr.join(',') : undefined;
}

/**
 * Convert comma-separated string from API to array.
 */
export function filterStringToArray(str: string | undefined): string[] {
  if (!str) return [];
  return str.split(',').map((s) => s.trim()).filter((s) => s.length > 0);
}

