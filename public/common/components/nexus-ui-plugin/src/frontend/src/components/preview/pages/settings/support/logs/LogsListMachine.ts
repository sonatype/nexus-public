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
import { LogFile, LogSortField, SortDirection, LOGS_API } from './types';

export interface LogsListContext {
  logs: LogFile[];
  filter: string;
  sortField: LogSortField;
  sortDirection: SortDirection;
  error: string | null;
}

type LogsListEvent =
  | { type: 'RETRY' }
  | { type: 'SET_FILTER'; value: string }
  | { type: 'SORT'; field: LogSortField }
  | { type: 'done.invoke.fetchLogs'; data: LogFile[] }
  | { type: 'error.platform.fetchLogs'; data: Error };

// Typed event aliases used in assign actions below
type FetchDoneEvent = Extract<LogsListEvent, { type: 'done.invoke.fetchLogs' }>;
type FetchErrorEvent = Extract<LogsListEvent, { type: 'error.platform.fetchLogs' }>;
type SetFilterEvent = Extract<LogsListEvent, { type: 'SET_FILTER' }>;
type SortEvent = Extract<LogsListEvent, { type: 'SORT' }>;

export const logsListMachine = createMachine<LogsListContext, LogsListEvent>(
  {
    id: 'logsList',
    initial: 'loading',
    predictableActionArguments: true,
    context: {
      logs: [],
      filter: '',
      sortField: 'fileName',
      sortDirection: 'asc',
      error: null,
    },
    states: {
      loading: {
        invoke: {
          id: 'fetchLogs',
          src: 'fetchLogs',
          onDone: {
            target: 'loaded',
            actions: 'setLogs',
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
      setLogs: assign({
        logs: (_, event) => (event as FetchDoneEvent).data ?? [],
        error: null,
      }),
      setError: assign({
        error: (_, event) => (event as FetchErrorEvent).data?.message ?? 'Failed to load log files',
      }),
      setFilter: assign({
        filter: (_, event) => (event as SetFilterEvent).value,
      }),
      setSort: assign((context, event) => {
        const { field } = event as SortEvent;
        if (context.sortField === field) {
          return { ...context, sortDirection: context.sortDirection === 'asc' ? 'desc' as SortDirection : 'asc' as SortDirection };
        }
        return { ...context, sortField: field, sortDirection: 'asc' as SortDirection };
      }),
    },
    services: {
      fetchLogs: async () => {
        try {
          const data = await restClient.get<LogFile[]>(LOGS_API.LIST);
          return Array.isArray(data) ? data : [];
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to load log files');
        }
      },
    },
  },
);
