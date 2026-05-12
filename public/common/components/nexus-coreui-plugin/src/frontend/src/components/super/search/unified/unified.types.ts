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
 * Unified Search Types
 * 
 * Shared types for the unified search page that supports all formats.
 */

// =============================================================================
// FORMAT TYPES
// =============================================================================

/**
 * All supported search formats.
 */
export type SearchFormat =
  | 'all'
  | 'apt'
  | 'cargo'
  | 'cocoapods'
  | 'composer'
  | 'conan'
  | 'conda'
  | 'docker'
  | 'gitlfs'
  | 'go'
  | 'helm'
  | 'huggingface'
  | 'maven'
  | 'npm'
  | 'nuget'
  | 'p2'
  | 'pypi'
  | 'r'
  | 'raw'
  | 'rubygems'
  | 'swift'
  | 'terraform'
  | 'yum';

/**
 * Format display information.
 */
export interface FormatInfo {
  /** Format ID used in API */
  id: SearchFormat;
  /** Display name */
  label: string;
  /** API format value (may differ from id) */
  apiFormat: string;
  /** Search bar placeholder text */
  placeholder: string;
}

// =============================================================================
// FILTER TYPES
// =============================================================================

/**
 * Filter input type.
 */
export type FilterType = 'text' | 'select';

/**
 * A single filter definition.
 */
export interface FilterDefinition {
  /** Unique filter ID */
  id: string;
  /** Display label */
  label: string;
  /** Input type */
  type: FilterType;
  /** API parameter name */
  apiParam: string;
  /** Options for select type */
  options?: readonly string[];
  /** Placeholder text for text inputs */
  placeholder?: string;
  /** Whether this filter is always shown (vs format-specific) */
  global?: boolean;
}

/**
 * Current filter values.
 */
export interface FilterValues {
  [filterId: string]: string;
}

/**
 * Format-specific filter configuration.
 */
export interface FormatFilterConfig {
  /** Format info */
  format: FormatInfo;
  /** Filters available for this format */
  filters: readonly FilterDefinition[];
}

// =============================================================================
// SEARCH RESULT TYPES
// =============================================================================

/**
 * A single search result.
 */
export interface SearchResult {
  /** Component ID */
  id: string;
  /** Component name */
  name: string;
  /** Format (npm, maven, etc.) */
  format: string;
  /** Repository name */
  repository: string;
  /** Group/namespace (for Maven, npm scope, etc.) */
  group?: string;
  /** Version */
  version: string;
  /** Description */
  description?: string;
  /** License */
  license?: string;
  /** Last updated ISO timestamp */
  lastUpdated?: string;
  /** Download URL for primary asset */
  downloadUrl?: string;
  /** Asset path */
  path?: string;
}

/**
 * Search response from API.
 */
export interface SearchResponse {
  /** Results */
  items: SearchResult[];
  /** Total count (if available) */
  totalCount?: number;
  /** Continuation token for pagination */
  continuationToken?: string;
}

// =============================================================================
// SEARCH STATE TYPES
// =============================================================================

/**
 * Sort options.
 */
export type SortField = 'name' | 'version' | 'lastUpdated' | 'repository';
export type SortDirection = 'asc' | 'desc';

/**
 * Sort option for dropdown.
 */
export interface SortOption {
  value: string;
  label: string;
}

/**
 * Complete search state.
 */
export interface UnifiedSearchState {
  /** Selected format */
  format: SearchFormat;
  /** Search query (main search bar) */
  query: string;
  /** Current filter values */
  filters: FilterValues;
  /** Search results */
  results: SearchResult[];
  /** Total result count */
  totalCount: number;
  /** Loading state */
  loading: boolean;
  /** Error message */
  error?: string;
  /** Continuation token for pagination */
  continuationToken?: string;
  /** Sort field */
  sortField: SortField;
  /** Sort direction */
  sortDirection: SortDirection;
}

// =============================================================================
// COMPONENT PROPS
// =============================================================================

/**
 * Props for SearchHeader component.
 */
export interface SearchHeaderProps {
  /** Currently selected format */
  format: SearchFormat;
  /** Callback when format changes */
  onFormatChange: (format: SearchFormat) => void;
  /** Current search query */
  query: string;
  /** Callback when search is submitted */
  onSearch: (query: string) => void;
  /** Placeholder text (derived from format) */
  placeholder?: string;
}

/**
 * Props for SearchSidebar component.
 */
export interface SearchSidebarProps {
  /** Currently selected formats (multi-select) */
  selectedFormats: SearchFormat[];
  /** Callback when format selection changes */
  onFormatsChange: (formats: SearchFormat[]) => void;
  /** Callback to trigger search */
  onSearch?: () => void;
  /** Set of formats that have at least one accessible repository */
  availableFormats?: Set<string>;
  /** Whether filters are disabled */
  disabled?: boolean;
  /** Current filter values (e.g. repository) */
  filters: FilterValues;
  /** Callback when a filter changes */
  onFilterChange: (filterId: string, value: string) => void;
  /** Callback to reset all filters */
  onReset: () => void;
  /** Available repositories for dropdown */
  repositories?: readonly string[];
}

/**
 * Props for SearchResults component.
 */
export interface SearchResultsProps {
  /** Search results to display */
  results: readonly SearchResult[];
  /** Loading state */
  loading: boolean;
  /** Error message */
  error?: string;
  /** Total count for display */
  totalCount: number;
  /** Whether more results are available */
  hasMore: boolean;
  /** Callback to load more results */
  onLoadMore: () => void;
  /** Callback when a result is selected */
  onSelect: (result: SearchResult) => void;
  /** Callback to retry after error */
  onRetry?: () => void;
}

/**
 * Props for SearchResultCard component.
 */
export interface SearchResultCardProps {
  /** The result to display */
  result: SearchResult;
  /** Callback when card is clicked */
  onClick: () => void;
}


