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

import React from 'react';
import { Box, Flex, Text, Table, Badge } from '@radix-ui/themes';
import { Search, Pencil, ArrowUp, ArrowDown, Loader2 } from 'lucide-react';

import { SettingsTextInput, SettingsAlert } from '../../../../shared/form';
import { FilterSidebar } from '../../../../shared';
import { useLoggersList } from './useLoggersList';
import { LoggerSortField, LogLevel } from './types';

import './LoggersList.scss';

interface LoggersListProps {
  onSelect: (name: string) => void;
}

function getLevelColor(level: LogLevel): 'red' | 'orange' | 'blue' | 'green' | 'purple' | 'gray' {
  switch (level) {
    case 'ERROR':
      return 'red';
    case 'WARN':
      return 'orange';
    case 'INFO':
      return 'blue';
    case 'DEBUG':
      return 'green';
    case 'TRACE':
      return 'purple';
    case 'OFF':
      return 'gray';
    default:
      return 'gray';
  }
}

/**
 * LoggersList - Displays a searchable, sortable list of loggers
 */
export function LoggersList({ onSelect }: LoggersListProps) {
  const {
    filteredLoggers,
    loggers,
    filter,
    sortField,
    sortDirection,
    error,
    isLoading,
    filterSections,
    setFilter,
    handleSort,
    handleFilterChange,
    handleClearFilters,
  } = useLoggersList();

  const renderSortIcon = (field: LoggerSortField) => {
    if (sortField !== field) return null;
    return sortDirection === 'asc' ? (
      <ArrowUp size={14} className="loggers-list__sort-icon" />
    ) : (
      <ArrowDown size={14} className="loggers-list__sort-icon" />
    );
  };

  if (isLoading) {
    return (
      <Box className="loggers-list loggers-list--loading">
        <Flex align="center" justify="center" gap="2" py="9">
          <Loader2 size={20} className="loggers-list__spinner" />
          <Text size="2">Loading loggers...</Text>
        </Flex>
      </Box>
    );
  }

  const hasActiveFilters = filter.trim() !== '' || filterSections[0]?.value?.length > 0;

  return (
    <Flex className="loggers-list loggers-list__layout" gap="4">
      {/* Sidebar */}
      <FilterSidebar
        sections={filterSections}
        onFilterChange={handleFilterChange}
        onClear={handleClearFilters}
        disabled={isLoading}
        className="loggers-list__sidebar"
      />

      {/* Main content */}
      <Box className="loggers-list__main">
        {/* Filter */}
        <Box className="loggers-list__toolbar">
          <SettingsTextInput
            value={filter}
            onChange={setFilter}
            placeholder="Filter by logger name"
            className="loggers-list__filter"
            icon={<Search size={16} />}
          />
        </Box>

        {/* Counter */}
        {(hasActiveFilters || loggers.length > 0) && (
          <Text size="1" color="gray" className="loggers-list__counter" data-testid="loggers-counter">
            Showing {filteredLoggers.length} of {loggers.length} loggers
          </Text>
        )}

        {/* Error alert */}
        {error && (
          <Box mb="3">
            <SettingsAlert type="error">{error}</SettingsAlert>
          </Box>
        )}

        {/* Table */}
        <Table.Root className="loggers-list__table">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell
                className="loggers-list__header-cell loggers-list__header-cell--sortable"
                onClick={() => handleSort('name')}
              >
                <Flex align="center" gap="1">
                  Logger Name
                  {renderSortIcon('name')}
                </Flex>
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell
                className="loggers-list__header-cell loggers-list__header-cell--sortable loggers-list__header-cell--level"
                onClick={() => handleSort('level')}
              >
                <Flex align="center" gap="1">
                  Logger Level
                  {renderSortIcon('level')}
                </Flex>
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell className="loggers-list__header-cell loggers-list__header-cell--action" />
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {filteredLoggers.length === 0 ? (
              <Table.Row>
                <Table.Cell colSpan={3} className="loggers-list__empty-cell">
                  <Flex align="center" justify="center" py="6">
                    <Text size="2" color="gray">
                      {hasActiveFilters ? 'No loggers match your filters' : 'No loggers found'}
                    </Text>
                  </Flex>
                </Table.Cell>
              </Table.Row>
            ) : (
              filteredLoggers.map((logger) => (
                <Table.Row
                  key={logger.name}
                  className="loggers-list__row"
                  onClick={() => onSelect(logger.name)}
                >
                  <Table.Cell className="loggers-list__cell">
                    <Text>{logger.name}</Text>
                  </Table.Cell>
                  <Table.Cell className="loggers-list__cell loggers-list__cell--level">
                    <Badge color={getLevelColor(logger.level)} variant="soft">
                      {logger.level}
                    </Badge>
                  </Table.Cell>
                  <Table.Cell className="loggers-list__cell loggers-list__cell--action">
                    <Pencil size={16} className="loggers-list__edit-icon" />
                  </Table.Cell>
                </Table.Row>
              ))
            )}
          </Table.Body>
        </Table.Root>
      </Box>
    </Flex>
  );
}

export default LoggersList;
