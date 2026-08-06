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

import { useCallback, useMemo } from 'react';
import { useMachine } from '@xstate/react';

import type {
  TagWithCount,
  TagsFilters,
  TagSortField,
} from '../tags.types';
import { tagsListMachine } from '../machines/tagsListMachine';

export type { TagWithCount, TagSortField };

/**
 * Return type for useFilteredTags hook.
 */
export interface UseFilteredTagsReturn {
  tags: TagWithCount[];
  loading: boolean;
  error: string | null;
  filters: TagsFilters;
  sortField: TagSortField;
  sortDirection: 'asc' | 'desc';
  currentPage: number;
  pageSize: number;
  totalItems: number;
  totalUnfilteredItems: number | null;
  setFilters: (filters: TagsFilters) => void;
  toggleSort: (field: TagSortField) => void;
  setPage: (page: number) => void;
  setPageSize: (size: number) => void;
  retry: () => void;
  refresh: () => void;
}

/**
 * Custom hook for managing the state of the filtered and paginated tags list.
 *
 * This hook follows the three-layer architecture:
 * - Layer 1: tagsListMachine (XState state machine)
 * - Layer 2: useFilteredTags (this integration hook)
 * - Layer 3: TagsPageRadix component
 *
 * The hook uses an XState machine to handle:
 * - Loading states and error handling
 * - Server-side filtering
 * - Server-side sorting
 * - Server-side pagination
 *
 * @returns Filtered tags state and actions
 */
export function useFilteredTags(): UseFilteredTagsReturn {
  // tagsListMachine already declares its initial context; no override needed.
  const machine = useMemo(() => tagsListMachine, []);

  // Use the XState machine
  const [state, send] = useMachine(machine);

  // Extract context values for easier access
  const {
    tags,
    loading,
    error,
    filters,
    sortField,
    sortDirection,
    currentPage,
    pageSize,
    totalItems,
    totalUnfilteredItems,
  } = state.context;

  /**
   * Update filters and trigger a refetch.
   * Resets to page 0 automatically.
   */
  const handleSetFilters = useCallback(
    (newFilters: TagsFilters) => {
      send({ type: 'SET_FILTERS', filters: newFilters });
    },
    [send]
  );

  /**
   * Toggle sort on a field.
   * If clicking the same field, toggles direction.
   * If clicking a different field, sorts ascending.
   * Resets to page 0.
   */
  const handleToggleSort = useCallback(
    (field: TagSortField) => {
      send({ type: 'TOGGLE_SORT', field });
    },
    [send]
  );

  /**
   * Go to a specific page.
   */
  const handleSetPage = useCallback(
    (page: number) => {
      send({ type: 'SET_PAGE', page });
    },
    [send]
  );

  /**
   * Change page size.
   * Resets to page 0 automatically.
   */
  const handleSetPageSize = useCallback(
    (size: number) => {
      send({ type: 'SET_PAGE_SIZE', pageSize: size });
    },
    [send]
  );

  /**
   * Retry fetching after an error.
   */
  const handleRetry = useCallback(() => {
    send({ type: 'RETRY' });
  }, [send]);

  /**
   * Refresh the list (e.g., after creating a new tag).
   * Works from both ready and error states.
   */
  const handleRefresh = useCallback(() => {
    send({ type: 'REFRESH' });
  }, [send]);

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
    totalUnfilteredItems,
    setFilters: handleSetFilters,
    toggleSort: handleToggleSort,
    setPage: handleSetPage,
    setPageSize: handleSetPageSize,
    retry: handleRetry,
    refresh: handleRefresh,
  };
}

export default useFilteredTags;
