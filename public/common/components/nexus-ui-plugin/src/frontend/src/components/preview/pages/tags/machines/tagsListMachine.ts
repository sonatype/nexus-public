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

import { assign, createMachine } from 'xstate';

import type { TagWithCount, TagSortField, TagsFilters } from '../tags.types';
import { fetchTagsFiltered } from '../tags.api';
import ExtJS from '../../../../../interface/ExtJS';

// =============================================================================
// Types
// =============================================================================

export interface TagsListMachineContext {
  /** List of tags (filtered and paginated) */
  tags: TagWithCount[];
  /** Whether data is currently loading */
  loading: boolean;
  /** Error message if fetch failed */
  error: string | null;
  /** Current filters */
  filters: TagsFilters;
  /** Current sort field */
  sortField: TagSortField;
  /** Current sort direction */
  sortDirection: 'asc' | 'desc';
  /** Current page (0-indexed) */
  currentPage: number;
  /** Number of items per page */
  pageSize: number;
  /** Total number of filtered items */
  totalItems: number;
  /** Total unfiltered items (for display) */
  totalUnfilteredItems: number | null;
}

export type TagsListMachineEvent =
  | { type: 'FETCH' }
  | { type: 'RETRY' }
  | { type: 'REFRESH' }
  | { type: 'SET_FILTERS'; filters: TagsFilters }
  | { type: 'SET_PAGE'; page: number }
  | { type: 'SET_PAGE_SIZE'; pageSize: number }
  | { type: 'TOGGLE_SORT'; field: TagSortField };

interface FetchResult {
  tags: TagWithCount[];
  totalItems: number;
  totalUnfilteredItems: number | null;
}

// =============================================================================
// Initial Context
// =============================================================================

// Not exported: the machine's initial state is the single source of truth.
// Tests assert against tagsListMachine.initialState.context rather than this constant.
const initialContext: TagsListMachineContext = {
  tags: [],
  loading: true,
  error: null,
  filters: {
    nameFilter: '',
    componentCountRanges: [],
    activityDays: [],
  },
  sortField: 'name',
  sortDirection: 'asc',
  currentPage: 0,
  pageSize: 20,
  totalItems: 0,
  totalUnfilteredItems: null,
};

// =============================================================================
// Services
// =============================================================================

async function fetchTagsService(context: TagsListMachineContext): Promise<FetchResult> {
  await ExtJS.waitForPermissions();
  const result = await fetchTagsFiltered({
    filters: context.filters,
    sortField: context.sortField,
    sortDirection: context.sortDirection,
    page: context.currentPage,
    pageSize: context.pageSize,
  });

  // Determine if this is an unfiltered fetch
  const noActiveFilters =
    context.filters.nameFilter.trim() === '' &&
    context.filters.componentCountRanges.length === 0 &&
    context.filters.activityDays.length === 0;

  return {
    tags: result.items,
    totalItems: result.totalCount,
    totalUnfilteredItems: noActiveFilters ? result.totalCount : context.totalUnfilteredItems,
  };
}

// =============================================================================
// Actions
// =============================================================================

const setLoading = assign<TagsListMachineContext>({
  loading: true,
  error: null,
});

const setSuccess = assign<TagsListMachineContext, { data: FetchResult }>({
  loading: false,
  error: null,
  tags: (_context, event) => event.data.tags,
  totalItems: (_context, event) => event.data.totalItems,
  totalUnfilteredItems: (_context, event) => event.data.totalUnfilteredItems,
});

const setError = assign<TagsListMachineContext, { data: Error }>({
  loading: false,
  error: (_context, event) =>
    event.data instanceof Error ? event.data.message : 'Failed to load tags. Please try again.',
  tags: [],
});

const setFilters = assign<TagsListMachineContext, { filters: TagsFilters }>({
  filters: (_context, event) => event.filters,
  currentPage: 0,
});

const setPage = assign<TagsListMachineContext, { page: number }>({
  currentPage: (_context, event) => event.page,
});

const setPageSize = assign<TagsListMachineContext, { pageSize: number }>({
  pageSize: (_context, event) => event.pageSize,
  currentPage: 0,
});

const toggleSort = assign<TagsListMachineContext, { field: TagSortField }>((context, event) => {
  const { field } = event;
  if (context.sortField === field) {
    return {
      sortDirection: context.sortDirection === 'asc' ? 'desc' : 'asc',
      currentPage: 0,
    };
  }
  return {
    sortField: field,
    sortDirection: 'asc',
    currentPage: 0,
  };
});

// =============================================================================
// Machine
// =============================================================================

export const tagsListMachine = createMachine<TagsListMachineContext, TagsListMachineEvent>(
  {
    id: 'tagsList',
    initial: 'loading',
    context: initialContext,
    states: {
      loading: {
        entry: 'setLoading',
        invoke: {
          id: 'fetchTags',
          src: 'fetchTags',
          onDone: {
            target: 'ready',
            actions: 'setSuccess',
          },
          onError: {
            target: 'error',
            actions: 'setError',
          },
        },
      },
      ready: {
        on: {
          FETCH: 'loading',
          REFRESH: 'loading',
          SET_FILTERS: {
            actions: 'setFilters',
            target: 'loading',
          },
          SET_PAGE: {
            actions: 'setPage',
            target: 'loading',
          },
          SET_PAGE_SIZE: {
            actions: 'setPageSize',
            target: 'loading',
          },
          TOGGLE_SORT: {
            actions: 'toggleSort',
            target: 'loading',
          },
        },
      },
      error: {
        on: {
          RETRY: 'loading',
          REFRESH: 'loading',
        },
      },
    },
  },
  {
    services: {
      fetchTags: (context) => fetchTagsService(context),
    },
    actions: {
      setLoading,
      setSuccess,
      setError,
      setFilters,
      setPage,
      setPageSize,
      toggleSort,
    },
  }
);
