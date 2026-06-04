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

/**
 * Tag data from the list API (ExtDirect proui_TagList.readTags).
 * @deprecated Use TagWithCount for the new filtered API
 */
export interface Tag {
  /** Tag name (unique identifier) */
  id: string;
  /** Timestamp when tag was first created */
  firstCreatedTime: string;
  /** Timestamp when tag was last updated */
  lastUpdatedTime: string;
}

/**
 * Tag with component count from the new filtered API.
 */
export interface TagWithCount {
  /** Tag name */
  name: string;
  /** Number of components tagged with this tag */
  componentCount: number;
  /** When the tag was first created */
  firstCreated: string | null;
  /** When the tag was last updated */
  lastUpdated: string | null;
}

/**
 * Paginated response for filtered tags.
 */
export interface TagPageResponse {
  items: TagWithCount[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
  hasNextPage: boolean;
  hasPreviousPage: boolean;
}

/**
 * Tag detail from the REST API (GET /service/rest/v1/tags/{tagName}).
 */
export interface TagDetail {
  /** Tag name */
  name: string;
  /** Timestamp when first component was tagged */
  firstCreated: string;
  /** Timestamp when last component was tagged */
  lastUpdated: string;
  /** Custom attributes associated with the tag */
  attributes: Record<string, unknown>;
}

/**
 * Sort field options for the tags list.
 */
export type TagSortField = 'name' | 'componentCount' | 'firstCreated' | 'lastUpdated';

/**
 * Sort direction.
 */
export type SortDirection = 'asc' | 'desc';

/**
 * Component count filter ranges.
 */
export type ComponentCountRange = '0' | '1-10' | '11-100' | '101-1000' | '1001+';

/**
 * Activity filter options.
 */
export type ActivityFilter = '30' | '90' | '91';

/**
 * Filters for the tags list.
 */
export interface TagsFilters {
  /** Name filter (contains) */
  nameFilter: string;
  /** Selected component count ranges */
  componentCounts: ComponentCountRange[];
  /** Selected activity filters */
  activityDays: ActivityFilter[];
}

/**
 * State for the useTags hook.
 */
export interface TagsState {
  /** List of tags (filtered and sorted) */
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
  sortDirection: SortDirection;
  /** Current page (0-indexed) */
  currentPage: number;
  /** Number of items per page */
  pageSize: number;
  /** Total number of filtered items */
  totalItems: number;
  /** Total pages */
  totalPages: number;
}

/**
 * Actions returned by the useTags hook.
 */
export interface TagsActions {
  /** Set the name filter */
  setNameFilter: (filter: string) => void;
  /** Toggle a component count range */
  toggleComponentCount: (range: ComponentCountRange) => void;
  /** Toggle an activity filter */
  toggleActivity: (activity: ActivityFilter) => void;
  /** Clear all filters */
  clearFilters: () => void;
  /** Toggle sort on a field */
  toggleSort: (field: TagSortField) => void;
  /** Go to a specific page */
  setPage: (page: number) => void;
  /** Retry fetching data after an error */
  retry: () => void;
}

/**
 * Props for the TagsPage component.
 */
export interface TagsPageProps {
  /** Optional pre-selected tag name from URL */
  tagName?: string;
}

/**
 * UI strings for tags.
 */
export const TAGS_STRINGS = {
  pageTitle: 'Tags',
  pageDescription: 'Manage component tags across repositories',
  filterPlaceholder: 'Filter by name...',
  createTag: '+ Create Tag',
  columns: {
    name: 'Name',
    componentCount: 'Components',
    firstCreated: 'Created',
    lastUpdated: 'Last Updated',
  },
  filters: {
    componentCount: 'Component Count',
    activity: 'Activity',
    empty: 'Empty (0)',
    range1to10: '1-10',
    range11to100: '11-100',
    range101to1000: '101-1000',
    range1000plus: '1000+',
    active: 'Active (< 30 days)',
    stale: 'Stale (30-90 days)',
    abandoned: 'Abandoned (90+ days)',
    clearFilters: 'Clear Filters',
  },
  emptyState: 'No tags found',
  loadingMessage: 'Loading tags...',
  errorPrefix: 'Error loading tags:',
}

