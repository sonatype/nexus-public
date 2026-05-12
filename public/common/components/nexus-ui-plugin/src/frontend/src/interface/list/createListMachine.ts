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
import type {
  ListMachineConfig,
  ListMachineContext,
  ListMachineEvent,
  ListMachineWithConfig,
  SortDirection,
} from './types';

/**
 * Default filter function - filters by filters object
 *
 * Handles common filter patterns:
 * - String filters (e.g., `filter: 'search text'`) - searches across all string/number fields
 * - Array filters (e.g., `typeFilter: ['type1', 'type2']`) - matches if item value is in array
 * - Equality filters (e.g., `status: 'active'`) - matches exact value
 */
function defaultFilterData<T>(context: ListMachineContext<T, any>): T[] {
  const { pristineData, filters } = context;

  return pristineData.filter((item) => {
    for (const [key, filterValue] of Object.entries(filters)) {
      if (filterValue !== undefined && filterValue !== null) {
        // Array filter (e.g., typeFilter: ['type1', 'type2'])
        if (Array.isArray(filterValue)) {
          // Skip empty arrays
          if (filterValue.length === 0) continue;
          const itemValue = (item as Record<string, unknown>)[key];
          if (!filterValue.includes(itemValue)) {
            return false;
          }
        }
        // String filter - if key is 'filter' or 'search', do text search across all fields
        else if (typeof filterValue === 'string' && filterValue !== '') {
          if (key === 'filter' || key === 'search') {
            // Text search across all string/number fields
            const searchLower = filterValue.toLowerCase();
            const matchesText = Object.values(item as Record<string, unknown>).some((value) => {
              if (typeof value === 'string' || typeof value === 'number') {
                return String(value).toLowerCase().includes(searchLower);
              }
              return false;
            });
            if (!matchesText) {
              return false;
            }
          } else {
            // Equality filter for other string values
            const itemValue = (item as Record<string, unknown>)[key];
            if (itemValue !== filterValue) {
              return false;
            }
          }
        }
      }
    }

    return true;
  });
}

/**
 * Default sort function - sorts by field and direction
 */
function defaultSortData<T>(context: ListMachineContext<T, any>): T[] {
  const { data, sortField, sortDirection } = context;

  if (!sortDirection) {
    return data;
  }

  return [...data].sort((a, b) => {
    const aVal = (a as Record<string, unknown>)[sortField];
    const bVal = (b as Record<string, unknown>)[sortField];

    // Handle null/undefined
    if (aVal == null && bVal == null) return 0;
    if (aVal == null) return 1;
    if (bVal == null) return -1;

    // String comparison (case-insensitive)
    if (typeof aVal === 'string' && typeof bVal === 'string') {
      const comparison = aVal.toLowerCase().localeCompare(bVal.toLowerCase());
      return sortDirection === 'asc' ? comparison : -comparison;
    }

    // Number/other comparison
    const comparison = aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
    return sortDirection === 'asc' ? comparison : -comparison;
  });
}

/**
 * Get next sort direction based on current state
 */
function getNextSortDirection(currentField: string, newField: string, currentDirection: SortDirection): SortDirection {
  if (currentField === newField) {
    if (currentDirection === 'asc') return 'desc';
    if (currentDirection === 'desc') return null;
    return 'asc';
  }
  return 'asc';
}

/**
 * Create a list machine for managing list state with XState (builder pattern)
 *
 * Features:
 * - Loading state with data fetching
 * - Flexible filtering via filters object
 * - Sortable columns with asc/desc/none states
 * - Error handling with retry
 * - Automatic filter+sort on data changes
 *
 * @example
 * ```typescript
 * const privilegesMachine = createListMachine({
 *   id: 'privileges',
 *   context: {
 *     sortField: 'name',
 *     sortDirection: 'asc',
 *     filters: { filter: '', typeFilter: [] }
 *   }
 * }).withConfig({
 *   services: {
 *     fetchData: async () => {
 *       const response = await fetch('/api/privileges');
 *       return response.json();
 *     }
 *   },
 *   actions: {
 *     filterData: (context) => {
 *       return context.pristineData.filter((priv) => {
 *         // Custom filtering logic
 *         if (context.filters.filter && !priv.name.includes(context.filters.filter)) return false;
 *         if (context.filters.typeFilter?.length && !context.filters.typeFilter.includes(priv.type)) return false;
 *         return true;
 *       });
 *     }
 *   }
 * });
 * ```
 */
export function createListMachine<T = unknown, TFilters = Record<string, unknown>>(
  config: ListMachineConfig<T, TFilters>
) {
  const { id, context: contextConfig = {} } = config;
  const sortField = contextConfig.sortField || 'name';
  const sortDirection = contextConfig.sortDirection || 'asc';
  const initialFilters = (contextConfig.filters || {}) as TFilters;

  type Context = ListMachineContext<T, TFilters>;
  type Event = ListMachineEvent<TFilters>;

  return {
    withConfig: (withConfig: ListMachineWithConfig<T, TFilters> = {}) => {
      const fetchData = withConfig.services?.fetchData || (() => Promise.resolve([] as T[]));
      const filterData = withConfig.actions?.filterData || defaultFilterData;
      const sortData = withConfig.actions?.sortData || defaultSortData;

      return createMachine<Context, Event>(
        {
          id,
          initial: 'loading',
          predictableActionArguments: true,
          schema: {
            context: {} as Context,
            events: {} as Event,
          },
          context: {
            pristineData: [],
            data: [],
            sortField,
            sortDirection,
            filters: initialFilters,
            error: null,
          } as Context,
          states: {
            loading: {
              invoke: {
                src: 'fetchData',
                onDone: {
                  target: 'loaded',
                  actions: ['setData', 'clearError'],
                },
                onError: {
                  target: 'error',
                  actions: 'setError',
                },
              },
            },
            loaded: {
              entry: ['filterData', 'sortData'],
              on: {
                LOAD: 'loading',
                SET_FILTERS: {
                  actions: 'setFilters',
                },
                CLEAR_FILTERS: {
                  actions: 'clearFilters',
                },
                SORT: {
                  actions: 'setSort',
                },
                SET_DATA: {
                  actions: 'setData',
                },
              },
            },
            error: {
              on: {
                LOAD: 'loading',
              },
            },
          },
        },
        {
          actions: {
            setData: assign({
              pristineData: (_, event: any) => event.data,
              data: (_, event: any) => event.data,
            }),
            clearError: assign({
              error: null,
            }),
            setError: assign({
              error: (_, event: any) => event.data?.message || 'Failed to load data',
            }),
            setFilters: assign((context, event: any) => {
              const newContext = {
                ...context,
                filters: { ...context.filters, ...event.filters },
              };
              const filtered = filterData(newContext);
              const sorted = sortData({ ...newContext, data: filtered });
              return { ...newContext, data: sorted };
            }),
            clearFilters: assign((context) => {
              const newContext = {
                ...context,
                filters: initialFilters,
              };
              const filtered = filterData(newContext);
              const sorted = sortData({ ...newContext, data: filtered });
              return { ...newContext, data: sorted };
            }),
            setSort: assign((context, event: any) => {
              const newSortDirection = getNextSortDirection(
                context.sortField,
                event.field,
                context.sortDirection
              );
              const newContext = {
                ...context,
                sortField: event.field,
                sortDirection: newSortDirection,
              };
              const sorted = sortData(newContext);
              return { ...newContext, data: sorted };
            }),
            filterData: assign((context) => {
              const filtered = filterData(context);
              return { ...context, data: filtered };
            }),
            sortData: assign((context) => {
              const sorted = sortData(context);
              return { ...context, data: sorted };
            }),
          },
          services: {
            fetchData,
          },
        }
      );
    },
  };
}

