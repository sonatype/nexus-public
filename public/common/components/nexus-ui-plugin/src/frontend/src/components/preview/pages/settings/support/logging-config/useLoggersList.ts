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
import { loggersListMachine } from './LoggersListMachine';
import { Logger, LoggerSortField, LogLevel } from './types';
import type { FilterSection } from '../../../../shared';

const LOG_LEVELS: LogLevel[] = ['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'];

export interface UseLoggersListReturn {
  filteredLoggers: Logger[];
  loggers: Logger[];
  filter: string;
  levelFilter: string[];
  sortField: LoggerSortField;
  sortDirection: 'asc' | 'desc';
  error: string | null;
  isLoading: boolean;
  filterSections: FilterSection[];
  setFilter: (value: string) => void;
  setLevelFilter: (value: string[]) => void;
  handleSort: (field: LoggerSortField) => void;
  handleFilterChange: (sectionId: string, value: string | string[]) => void;
  handleClearFilters: () => void;
}

export function useLoggersList(): UseLoggersListReturn {
  const [state, send] = useMachine(loggersListMachine);
  const { loggers, filter, levelFilter, sortField, sortDirection, error } = state.context;

  const isLoading = state.matches('loading');

  const filteredLoggers = useMemo(() => {
    let result = [...loggers];

    if (filter) {
      const lowerFilter = filter.toLowerCase();
      result = result.filter((l) => l.name.toLowerCase().includes(lowerFilter));
    }

    if (levelFilter.length > 0) {
      result = result.filter((l) => levelFilter.includes(l.level));
    }

    result.sort((a, b) => {
      let comparison = 0;
      if (sortField === 'name') comparison = a.name.localeCompare(b.name);
      else if (sortField === 'level') comparison = a.level.localeCompare(b.level);
      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return result;
  }, [loggers, filter, levelFilter, sortField, sortDirection]);

  const filterSections: FilterSection[] = useMemo(() => {
    const levelCounts: Record<string, number> = {};
    loggers.forEach((l) => {
      levelCounts[l.level] = (levelCounts[l.level] || 0) + 1;
    });

    return [
      {
        id: 'level',
        label: 'Log Level',
        type: 'checkbox' as const,
        options: LOG_LEVELS.map((level) => ({
          value: level,
          label: level,
          count: levelCounts[level] || 0,
        })),
        value: levelFilter,
        defaultExpanded: true,
      },
    ];
  }, [loggers, levelFilter]);

  const setFilter = useCallback(
    (value: string) => {
      send({ type: 'SET_FILTER', value });
    },
    [send]
  );

  const setLevelFilter = useCallback(
    (value: string[]) => {
      send({ type: 'SET_LEVEL_FILTER', value });
    },
    [send]
  );

  const handleSort = useCallback(
    (field: LoggerSortField) => {
      send({ type: 'SORT', field });
    },
    [send]
  );

  const handleFilterChange = useCallback(
    (sectionId: string, value: string | string[]) => {
      if (sectionId === 'level') {
        send({ type: 'SET_LEVEL_FILTER', value: value as string[] });
      }
    },
    [send]
  );

  const handleClearFilters = useCallback(() => {
    send({ type: 'SET_LEVEL_FILTER', value: [] });
  }, [send]);

  return {
    filteredLoggers,
    loggers,
    filter,
    levelFilter,
    sortField,
    sortDirection,
    error,
    isLoading,
    filterSections,
    setFilter,
    setLevelFilter,
    handleSort,
    handleFilterChange,
    handleClearFilters,
  };
}
