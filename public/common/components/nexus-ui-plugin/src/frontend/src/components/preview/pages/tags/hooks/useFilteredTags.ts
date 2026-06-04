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

import { useCallback, useEffect, useMemo, useState } from 'react';
import { restClient } from '../../../../../interface/api';

export interface TagWithCount {
  name: string;
  attributes: Record<string, unknown> | null;
  firstCreated: string | null;
  lastUpdated: string | null;
  componentCount: number;
}

export interface TagPage {
  items: TagWithCount[];
  totalCount: number;
  continuationToken: string | null;
}

export interface TagsFilters {
  nameFilter: string;
  componentCountRanges: string[];
  activityDays: number[];
}

export type TagSortField = 'name' | 'componentCount' | 'firstCreated' | 'lastUpdated';
export type SortDirection = 'asc' | 'desc';

const TAGS_FILTERED_URL = '/service/rest/internal/ui/tags/filtered';

/**
 * Custom hook for managing the state of the filtered and paginated tags list.
 */
export function useFilteredTags() {
  const [tags, setTags] = useState<TagWithCount[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<TagsFilters>({
    nameFilter: '',
    componentCountRanges: [],
    activityDays: [],
  });
  const [sortField, setSortField] = useState<TagSortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);
  const [totalItems, setTotalItems] = useState<number>(0);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (filters.nameFilter) {
        params.append('nameFilter', filters.nameFilter);
      }
      filters.componentCountRanges.forEach((range) => {
        params.append('componentCountRanges', range);
      });
      filters.activityDays.forEach((days) => {
        params.append('activityDays', String(days));
      });
      params.append('sortField', sortField);
      params.append('sortDirection', sortDirection);
      params.append('page', String(currentPage));
      params.append('pageSize', String(pageSize));

      const data = await restClient.get<TagPage>(`${TAGS_FILTERED_URL}?${params.toString()}`);
      setTags(data.items);
      setTotalItems(data.totalCount);
    } catch (err) {
      console.error('Failed to fetch tags:', err);
      setError('Failed to load tags. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [filters, sortField, sortDirection, currentPage, pageSize]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSetFilters = useCallback((newFilters: TagsFilters) => {
    setFilters(newFilters);
    setCurrentPage(0);
  }, []);

  const handleToggleSort = useCallback(
    (field: TagSortField) => {
      if (sortField === field) {
        setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
      } else {
        setSortField(field);
        setSortDirection('asc');
      }
      setCurrentPage(0);
    },
    [sortField]
  );

  const handleSetPage = useCallback((page: number) => {
    setCurrentPage(page);
  }, []);

  const handleSetPageSize = useCallback((size: number) => {
    setPageSize(size);
    setCurrentPage(0);
  }, []);

  const handleRetry = useCallback(() => {
    fetchData();
  }, [fetchData]);

  return {
    tags,
    loading,
    error,
    filters,
    sortField,
    sortDirection,
    currentPage,
    pageSize,
    totalItems,
    setFilters: handleSetFilters,
    toggleSort: handleToggleSort,
    setPage: handleSetPage,
    setPageSize: handleSetPageSize,
    retry: handleRetry,
  };
}

export default useFilteredTags;
