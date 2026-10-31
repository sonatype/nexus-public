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
  | 'alpine'
  | 'ansiblegalaxy'
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
  | 'oci'
  | 'p2'
  | 'pub'
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
 * Alias labels for the three generic filters (group / name / version).
 * A format that uses domain-specific vocabulary (e.g. Maven "Group ID",
 * Docker "Image Name") sets the keys it wants to relabel.  The sidebar
 * renders the alias label instead of the generic default.
 */
export type GenericAliasLabels = Partial<Record<'group' | 'name' | 'version', string>>;

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
  /** Filters available for this format (always starts with repository + generic group/name/version) */
  filters: readonly FilterDefinition[];
  /**
   * Optional alias labels for the three generic filters.
   * When a format uses domain-specific vocabulary (e.g. Maven "Group ID" for
   * group, Docker "Image Name" for name) set only the keys that differ from
   * the generic defaults.  The sidebar reads this map and relabels the generic
   * input in place — no duplicate rows are rendered.
   */
  aliasLabels?: GenericAliasLabels;
  /**
   * Optional placeholder overrides paired with `aliasLabels`.  When the alias
   * label uses format-specific vocabulary the generic placeholder text (e.g.
   * "e.g., commons-lang3" for the Name field) can contradict the label; set
   * this map to display placeholders that match the aliased label.
   */
  aliasPlaceholders?: GenericAliasLabels;
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
  /** Count of results in this response */
  totalCount?: number;
  /** True server-side match count for the whole query (if available) */
  total?: number;
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
 * A single selectable sort option, combining a field and a direction.
 *
 * The results-header dropdown renders this option list (see `sortOptions`),
 * which is also the allowlist URL and stored sort values are validated against.
 */
export interface SortOption {
  /** Composite `field:direction` value used by the sort controls */
  value: string;
  /**
   * Full label naming field and direction, e.g. "Last updated — Newest first".
   * Shown in the dropdown's closed state, where no group heading is present to
   * supply the field name.
   */
  label: string;
  /** Field this option sorts by */
  field: SortField;
  /** Direction this option sorts in */
  direction: SortDirection;
  /**
   * Direction-only label, e.g. "Newest first". Used where the field name is
   * already shown as a group heading.
   */
  directionLabel: string;
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
  /** Count of results loaded so far */
  totalCount: number;
  /** True server-side match count for the whole query (undefined until known) */
  total?: number;
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
  /** Count of results loaded so far */
  totalCount: number;
  /** True server-side match count; shown exactly when known, never as a cap */
  total?: number;
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
  /**
   * Callback when the card is selected, by click or by keyboard. Receives the
   * result so one stable handler can serve every card, which is what allows
   * `memo` to skip unchanged cards.
   */
  onSelect: (result: SearchResult) => void;
}

