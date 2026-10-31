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
import { restClient, parseApiError } from '../../../../../../interface/api';
import { Logger, LoggerSortField, SortDirection, LOGGING_CONFIG_API } from './types';

export interface LoggersListContext {
  loggers: Logger[];
  filter: string;
  levelFilter: string[];
  sortField: LoggerSortField;
  sortDirection: SortDirection;
  error: string | null;
}

type LoggersListEvent =
  | { type: 'RETRY' }
  | { type: 'SET_FILTER'; value: string }
  | { type: 'SET_LEVEL_FILTER'; value: string[] }
  | { type: 'SORT'; field: LoggerSortField }
  | { type: 'done.invoke.fetchLoggers'; data: Logger[] }
  | { type: 'error.platform.fetchLoggers'; data: Error };

// Typed event aliases used in assign actions below
type FetchDoneEvent = Extract<LoggersListEvent, { type: 'done.invoke.fetchLoggers' }>;
type FetchErrorEvent = Extract<LoggersListEvent, { type: 'error.platform.fetchLoggers' }>;
type SetFilterEvent = Extract<LoggersListEvent, { type: 'SET_FILTER' }>;
type SetLevelFilterEvent = Extract<LoggersListEvent, { type: 'SET_LEVEL_FILTER' }>;
type SortEvent = Extract<LoggersListEvent, { type: 'SORT' }>;

export const loggersListMachine = createMachine<LoggersListContext, LoggersListEvent>(
  {
    id: 'loggersList',
    initial: 'loading',
    predictableActionArguments: true,
    context: {
      loggers: [],
      filter: '',
      levelFilter: [],
      sortField: 'name',
      sortDirection: 'asc',
      error: null,
    },
    states: {
      loading: {
        invoke: {
          id: 'fetchLoggers',
          src: 'fetchLoggers',
          onDone: {
            target: 'loaded',
            actions: 'setLoggers',
          },
          onError: {
            target: 'error',
            actions: 'setError',
          },
        },
      },
      loaded: {
        on: {
          RETRY: 'loading',
          SET_FILTER: { actions: 'setFilter' },
          SET_LEVEL_FILTER: { actions: 'setLevelFilter' },
          SORT: { actions: 'setSort' },
        },
      },
      error: {
        on: {
          RETRY: 'loading',
        },
      },
    },
  },
  {
    actions: {
      setLoggers: assign({
        loggers: (_, event) => (event as FetchDoneEvent).data ?? [],
        error: null,
      }),
      setError: assign({
        error: (_, event) => (event as FetchErrorEvent).data?.message ?? 'Failed to load loggers',
      }),
      setFilter: assign({
        filter: (_, event) => (event as SetFilterEvent).value,
      }),
      setLevelFilter: assign({
        levelFilter: (_, event) => (event as SetLevelFilterEvent).value,
      }),
      setSort: assign((context, event) => {
        const { field } = event as SortEvent;
        if (context.sortField === field) {
          return {
            ...context,
            sortDirection: context.sortDirection === 'asc' ? ('desc' as SortDirection) : ('asc' as SortDirection),
          };
        }
        return { ...context, sortField: field, sortDirection: 'asc' as SortDirection };
      }),
    },
    services: {
      fetchLoggers: async () => {
        try {
          const data = await restClient.get<Logger[]>(LOGGING_CONFIG_API.LIST);
          return Array.isArray(data) ? data : [];
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to load loggers');
        }
      },
    },
  }
);
