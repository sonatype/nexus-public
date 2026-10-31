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
  // Tracks request lifecycle rather than result count, so a refetch over an empty result is still
  // recognised as a refetch. Set once the first non-stale request settles and never cleared.
  const [hasLoadedOnce, setHasLoadedOnce] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);

  // Filter changes can overlap, and responses are not guaranteed to arrive in the order they
  // were requested. Each fetch takes the next request id and only the newest one is allowed to
  // write to state, so the list always reflects the latest filters.
  const requestIdRef = useRef<number>(0);

  // Fetch repositories from server
  const fetchRepositories = useCallback(async (params: RepositoryFilterParams) => {
    const requestId = ++requestIdRef.current;
    const isStale = () => requestId !== requestIdRef.current;

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
        if (isStale()) return;
        setRepositories(pageData.data ?? []);
        setTotalCount(pageData.totalCount ?? 0);
        setTotalPages(pageData.totalPages ?? 0);
        return;
      }

      const queryString = buildQueryString(params);
      const response = await Axios.get<RepositoryPageResponse>(
        `${FILTERED_REPOSITORIES_API}${queryString}`
      );

      if (isStale()) return;
      const pageData = response.data;
      setRepositories(pageData.data ?? []);
      setTotalCount(pageData.totalCount ?? 0);
      setTotalPages(pageData.totalPages ?? 0);
    } catch (err) {
      if (isStale()) return;
      const errorMessage = err instanceof Error ? err.message : 'Failed to load repositories';
      setError(errorMessage);
      setRepositories([]);
      setTotalCount(0);
      setTotalPages(0);
    } finally {
      if (!isStale()) {
        setLoading(false);
        setHasLoadedOnce(true);
      }
    }
  }, []);

  // This effect is the *only* fetch trigger. Do not also fetch from inside the
  // setFilterParamsState updater: that duplicated every filter change into two requests and
  // defeated any debouncing upstream, since one request went out immediately regardless.
  // Keeping the fetch here also leaves the state updater pure.
  useEffect(() => {
    fetchRepositories(filterParams);
  }, [fetchRepositories, filterParams]);

  // Update filter params. Callers own any debouncing of the value they feed in; every applied
  // change lands here exactly once and is merged into the previous params, so overlapping
  // changes accumulate into one combined filter state rather than replacing each other.
  const setFilterParams = useCallback((newParams: Partial<RepositoryFilterParams>) => {
    setFilterParamsState((prev) => {
      const updated = { ...prev, ...newParams };

      // Reset to page 1 when filters change (except page itself)
      if (!('page' in newParams)) {
        updated.page = 1;
      }

      return updated;
    });
  }, []);

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

  return {
    repositories,
    loading,
    hasLoadedOnce,
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

