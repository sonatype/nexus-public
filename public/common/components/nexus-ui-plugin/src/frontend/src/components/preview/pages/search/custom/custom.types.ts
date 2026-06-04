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
 * Custom Search Types
 *
 * Types for building dynamic search queries with user-defined filters.
 */

/**
 * Available filter fields for custom search.
 */
export type FilterField =
  | 'format'
  | 'repository'
  | 'group'
  | 'name'
  | 'version'
  | 'tag'
  | 'keyword';

/**
 * Available filter operators for custom search.
 */
export type FilterOperator =
  | 'equals'
  | 'contains'
  | 'startsWith'
  | 'endsWith';

/**
 * A single filter criterion in the custom search builder.
 */
export interface CustomFilter {
  /** Unique identifier for React key and state management */
  readonly id: string;
  /** The field to filter on */
  field: FilterField;
  /** The comparison operator */
  operator: FilterOperator;
  /** The filter value */
  value: string;
}

/**
 * Configuration for a filter field option.
 */
export interface FilterFieldOption {
  /** Field value */
  readonly value: FilterField;
  /** Display label */
  readonly label: string;
  /** Placeholder text for value input */
  readonly placeholder: string;
}

/**
 * Configuration for a filter operator option.
 */
export interface FilterOperatorOption {
  /** Operator value */
  readonly value: FilterOperator;
  /** Display label */
  readonly label: string;
}

/**
 * Custom search result item.
 * Generic result that works across all formats.
 */
export interface CustomSearchResult {
  /** Unique identifier */
  readonly id: string;
  /** Repository name */
  readonly repository: string;
  /** Format (maven2, npm, docker, etc.) */
  readonly format: string;
  /** Group/namespace */
  readonly group?: string;
  /** Name/artifact ID */
  readonly name: string;
  /** Version */
  readonly version: string;
  /** ISO 8601 timestamp */
  readonly lastModified: string;
  /** Tags if applicable */
  readonly tags?: readonly string[];
}

/**
 * Custom search response from API.
 */
export interface CustomSearchResponse {
  /** Search results */
  readonly items: readonly CustomSearchResult[];
  /** Total count of matching items */
  readonly totalCount: number;
  /** Continuation token for pagination */
  readonly continuationToken?: string;
}

/**
 * Custom search state.
 */
export interface CustomSearchState {
  /** Array of filter criteria */
  readonly filters: readonly CustomFilter[];
  /** Search results */
  readonly results: readonly CustomSearchResult[];
  /** Total count of results */
  readonly totalCount: number;
  /** Whether search is in progress */
  readonly loading: boolean;
  /** Error message if any */
  readonly error?: string;
  /** Continuation token for pagination */
  readonly continuationToken?: string;
}

/**
 * Available filter field options.
 */
export const FILTER_FIELD_OPTIONS: readonly FilterFieldOption[] = [
  { value: 'keyword', label: 'Keyword', placeholder: 'e.g., spring-boot' },
  { value: 'format', label: 'Format', placeholder: 'e.g., maven2, npm, docker' },
  { value: 'repository', label: 'Repository', placeholder: 'e.g., maven-central' },
  { value: 'group', label: 'Group', placeholder: 'e.g., org.apache.commons' },
  { value: 'name', label: 'Name', placeholder: 'e.g., commons-lang3' },
  { value: 'version', label: 'Version', placeholder: 'e.g., 3.14.0' },
  { value: 'tag', label: 'Tag', placeholder: 'e.g., latest' },
];

/**
 * Available filter operator options.
 */
export const FILTER_OPERATOR_OPTIONS: readonly FilterOperatorOption[] = [
  { value: 'equals', label: 'Equals' },
  { value: 'contains', label: 'Contains' },
  { value: 'startsWith', label: 'Starts with' },
  { value: 'endsWith', label: 'Ends with' },
];

/**
 * Create a new empty filter with a unique ID.
 */
export function createEmptyFilter(): CustomFilter {
  return {
    id: `filter-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
    field: 'keyword',
    operator: 'contains',
    value: '',
  };
}


