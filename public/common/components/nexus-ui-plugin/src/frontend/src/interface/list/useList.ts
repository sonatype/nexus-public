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

import { useMemo, useCallback } from 'react';
import { useMachine } from '@xstate/react';
import type { StateMachine } from 'xstate';
import type { ListMachineContext, ListMachineEvent, SortDirection } from './types';

/**
 * Hook return type for useList
 */
export interface UseListResult<T, TFilters = Record<string, unknown>> {
  /** Current filtered and sorted data */
  data: T[];
  /** Raw unfiltered data */
  pristineData: T[];
  /** Current loading state */
  loading: boolean;
  /** Error message if load failed */
  error: string | null;
  /** Current sort field */
  sortField: string;
  /** Current sort direction */
  sortDirection: SortDirection;
  /** Current filters */
  filters: TFilters;
  /** Whether machine is in loaded state */
  loaded: boolean;
  /** Update filters (merges with existing) */
  setFilters: (filters: Partial<TFilters>) => void;
  /** Clear all filters */
  clearFilters: () => void;
  /** Toggle sort for a field */
  sort: (field: string) => void;
  /** Reload data */
  reload: () => void;
}

/**
 * Hook to use a list machine
 *
 * Provides a clean API for managing list state with XState
 *
 * @example
 * ```typescript
 * const privilegesMachine = createListMachine({
 *   id: 'privileges',
 *   context: {
 *     sortField: 'name',
 *     filters: { filter: '', typeFilter: [] },
 *   },
 * }).withConfig({
 *   services: { fetchData: async () => fetchPrivileges() },
 * });
 *
 * function PrivilegesList() {
 *   const {
 *     data,
 *     loading,
 *     error,
 *     filters,
 *     setFilters,
 *     sort,
 *     sortField,
 *     sortDirection,
 *     reload,
 *   } = useList(privilegesMachine);
 *
 *   return (
 *     <EntityTable
 *       data={data}
 *       loading={loading}
 *       error={error}
 *       sortBy={sortField}
 *       sortDirection={sortDirection}
 *       onSort={sort}
 *       onRetry={reload}
 *     />
 *   );
 * }
 * ```
 */
export function useList<T = unknown, TFilters = Record<string, unknown>>(
  machine: StateMachine<
    ListMachineContext<T, TFilters>,
    any,
    ListMachineEvent<TFilters>
  >
): UseListResult<T, TFilters> {
  const [state, send] = useMachine(machine);

  const setFilters = useCallback(
    (filters: Partial<TFilters>) => {
      send({ type: 'SET_FILTERS', filters });
    },
    [send]
  );

  const clearFilters = useCallback(() => {
    send({ type: 'CLEAR_FILTERS' });
  }, [send]);

  const sort = useCallback(
    (field: string) => {
      send({ type: 'SORT', field });
    },
    [send]
  );

  const reload = useCallback(() => {
    send({ type: 'LOAD' });
  }, [send]);

  const result = useMemo(
    () => ({
      data: state.context.data,
      pristineData: state.context.pristineData,
      loading: state.matches('loading'),
      error: state.context.error,
      sortField: state.context.sortField,
      sortDirection: state.context.sortDirection,
      filters: state.context.filters,
      loaded: state.matches('loaded'),
      setFilters,
      clearFilters,
      sort,
      reload,
    }),
    [state, setFilters, clearFilters, sort, reload]
  );

  return result;
}
