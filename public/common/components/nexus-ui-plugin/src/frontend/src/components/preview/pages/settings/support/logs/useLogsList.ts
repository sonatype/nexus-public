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

import { useCallback, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import { logsListMachine } from './LogsListMachine';
import { LogFile, LogSortField, SortDirection } from './types';

export interface UseLogsListReturn {
  filteredLogs: LogFile[];
  filter: string;
  sortField: LogSortField;
  sortDirection: SortDirection;
  error: string | null;
  isLoading: boolean;
  setFilter: (value: string) => void;
  handleSort: (field: LogSortField) => void;
}

export function useLogsList(): UseLogsListReturn {
  const [state, send] = useMachine(logsListMachine);
  const { logs, filter, sortField, sortDirection, error } = state.context;

  const isLoading = state.matches('loading');

  const filteredLogs = useMemo(() => {
    let result = [...logs];

    if (filter) {
      const lowerFilter = filter.toLowerCase();
      result = result.filter((log) =>
        log.fileName.toLowerCase().includes(lowerFilter)
      );
    }

    result.sort((a, b) => {
      let comparison = 0;
      switch (sortField) {
        case 'fileName':
          comparison = a.fileName.localeCompare(b.fileName);
          break;
        case 'size':
          comparison = a.size - b.size;
          break;
        case 'lastModified':
          comparison = a.lastModified - b.lastModified;
          break;
      }
      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return result;
  }, [logs, filter, sortField, sortDirection]);

  const setFilter = useCallback((value: string) => {
    send({ type: 'SET_FILTER', value });
  }, [send]);

  const handleSort = useCallback((field: LogSortField) => {
    send({ type: 'SORT', field });
  }, [send]);

  return {
    filteredLogs,
    filter,
    sortField,
    sortDirection,
    error,
    isLoading,
    setFilter,
    handleSort,
  };
}
