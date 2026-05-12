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

import { useState, useEffect, useCallback, useMemo } from 'react';
import { APIConstants } from '@sonatype/nexus-ui-plugin';

import { fetchTags } from '../tags.api';
import type { Tag, TagSortField, SortDirection, TagsState, TagsActions } from '../tags.types';

const DEFAULT_PAGE_SIZE = APIConstants.EXT.SMALL_PAGE_SIZE || 25;

/**
 * Initial state for the useTags hook.
 */
const initialState: TagsState = {
  tags: [],
  loading: true,
  error: null,
  filter: '',
  sortField: 'id',
  sortDirection: 'asc',
  currentPage: 0,
  pageSize: DEFAULT_PAGE_SIZE,
  totalItems: 0,
};

/**
 * Compare function for sorting tags.
 */
function compareTags(a: Tag, b: Tag, field: TagSortField, direction: SortDirection): number {
  let comparison = 0;

  switch (field) {
    case 'id':
      comparison = a.id.localeCompare(b.id);
      break;
    case 'firstCreatedTime':
      comparison = new Date(a.firstCreatedTime).getTime() - new Date(b.firstCreatedTime).getTime();
      break;
    case 'lastUpdatedTime':
      comparison = new Date(a.lastUpdatedTime).getTime() - new Date(b.lastUpdatedTime).getTime();
      break;
    default:
      comparison = 0;
  }

  return direction === 'desc' ? -comparison : comparison;
}

/**
 * Custom hook for managing tags list state.
 *
 * Features:
 * - Fetches all tags on mount
 * - Client-side filtering by name
 * - Client-side sorting (name, firstCreated, lastUpdated)
 * - Client-side pagination
 * - Error handling with retry
 *
 * @returns Object with state and actions
 */
export function useTags(): { state: TagsState; actions: TagsActions } {
  // Raw data from API
  const [rawTags, setRawTags] = useState<Tag[]>([]);

  // UI state
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState('');
  const [sortField, setSortField] = useState<TagSortField>('id');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [currentPage, setCurrentPage] = useState(0);

  // Fetch tags
  const loadTags = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const tags = await fetchTags();
      setRawTags(tags);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load tags';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  // Load on mount
  useEffect(() => {
    loadTags();
  }, [loadTags]);

  // Process tags: filter, sort, paginate
  const processedState = useMemo(() => {
    // Filter
    let filtered = rawTags;
    if (filter.trim()) {
      const searchTerm = filter.toLowerCase();
      filtered = rawTags.filter((tag) => tag.id.toLowerCase().includes(searchTerm));
    }

    // Sort
    const sorted = [...filtered].sort((a, b) => compareTags(a, b, sortField, sortDirection));

    // Paginate
    const totalItems = sorted.length;
    const pageCount = Math.ceil(totalItems / DEFAULT_PAGE_SIZE);
    const safePage = Math.min(currentPage, Math.max(0, pageCount - 1));
    const startIndex = safePage * DEFAULT_PAGE_SIZE;
    const paginated = sorted.slice(startIndex, startIndex + DEFAULT_PAGE_SIZE);

    return {
      tags: paginated,
      totalItems,
      currentPage: safePage,
    };
  }, [rawTags, filter, sortField, sortDirection, currentPage]);

  // Actions
  const handleSetFilter = useCallback((newFilter: string) => {
    setFilter(newFilter);
    setCurrentPage(0); // Reset to first page when filtering
  }, []);

  const handleSetSort = useCallback((field: TagSortField, direction: SortDirection) => {
    setSortField(field);
    setSortDirection(direction);
  }, []);

  const handleToggleSort = useCallback(
    (field: TagSortField) => {
      if (field === sortField) {
        // Toggle direction
        setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
      } else {
        // New field, default to ascending
        setSortField(field);
        setSortDirection('asc');
      }
    },
    [sortField]
  );

  const handleSetPage = useCallback((page: number) => {
    setCurrentPage(page);
  }, []);

  const handleRetry = useCallback(() => {
    loadTags();
  }, [loadTags]);

  // Build state object
  const state: TagsState = {
    tags: processedState.tags,
    loading,
    error,
    filter,
    sortField,
    sortDirection,
    currentPage: processedState.currentPage,
    pageSize: DEFAULT_PAGE_SIZE,
    totalItems: processedState.totalItems,
  };

  // Build actions object
  const actions: TagsActions = {
    setFilter: handleSetFilter,
    setSort: handleSetSort,
    toggleSort: handleToggleSort,
    setPage: handleSetPage,
    retry: handleRetry,
  };

  return { state, actions };
}

export default useTags;

