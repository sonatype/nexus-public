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
 * Sort direction for list items
 */
export type SortDirection = 'asc' | 'desc' | null;

/**
 * Base context for list machine
 */
export interface ListMachineContext<T = unknown, TFilters = Record<string, unknown>> {
  /** Raw data loaded from API */
  pristineData: T[];
  /** Filtered and sorted data for display */
  data: T[];
  /** Current sort field */
  sortField: string;
  /** Current sort direction */
  sortDirection: SortDirection;
  /** All filters (text search and structured filters) */
  filters: TFilters;
  /** Error message if load failed */
  error: string | null;
}

/**
 * Events for list machine
 */
export type ListMachineEvent<TFilters = Record<string, unknown>> =
  | { type: 'LOAD' }
  | { type: 'SET_FILTERS'; filters: Partial<TFilters> }
  | { type: 'CLEAR_FILTERS' }
  | { type: 'SORT'; field: string }
  | { type: 'SET_DATA'; data: unknown[] };

/**
 * Initial configuration for creating a list machine (builder pattern)
 */
export interface ListMachineConfig<_T = unknown, TFilters = Record<string, unknown>> {
  /** Unique machine ID */
  id: string;
  /** Initial context values */
  context?: {
    /** Initial sort field */
    sortField?: string;
    /** Initial sort direction */
    sortDirection?: SortDirection;
    /** Initial filters */
    filters?: TFilters;
  };
}

/**
 * Configuration for withConfig() method
 */
export interface ListMachineWithConfig<T = unknown, TFilters = Record<string, unknown>> {
  /** Services for the machine */
  services?: {
    /** Service to fetch data */
    fetchData?: () => Promise<T[]>;
  };
  /** Actions for the machine */
  actions?: {
    /** Custom filter function (filters pristineData based on filters) */
    filterData?: (context: ListMachineContext<T, TFilters>) => T[];
    /** Custom sort function (sorts filtered data) */
    sortData?: (context: ListMachineContext<T, TFilters>) => T[];
  };
}
