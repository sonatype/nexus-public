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

import React, { useCallback } from 'react';
import { Box, Flex, Text, Table } from '@radix-ui/themes';
import { Search, Pencil, ArrowUp, ArrowDown, FileText, Loader2 } from 'lucide-react';
import { HumanReadableUtils } from '../../../../../../interface/HumanReadableUtils';
import { DateTime } from 'luxon';

import { SettingsTextInput, SettingsAlert } from '../../../../shared/form';
import { useLogsList } from './useLogsList';
import { LogSortField } from './types';

import './LogsList.scss';

interface LogsListProps {
  onSelect: (filename: string) => void;
}

/**
 * LogsList - Displays a searchable, sortable list of log files
 */
export function LogsList({ onSelect }: LogsListProps) {
  const {
    filteredLogs,
    filter,
    sortField,
    sortDirection,
    error,
    isLoading,
    setFilter,
    handleSort,
  } = useLogsList();

  const renderSortIcon = useCallback((field: LogSortField) => {
    if (sortField !== field) return null;
    return sortDirection === 'asc' ? (
      <ArrowUp size={14} className="logs-list__sort-icon" />
    ) : (
      <ArrowDown size={14} className="logs-list__sort-icon" />
    );
  }, [sortField, sortDirection]);

  const formatSize = (bytes: number): string => {
    return HumanReadableUtils.bytesToString(bytes);
  };

  const formatDate = (timestamp: number): string => {
    return DateTime.fromMillis(timestamp).toLocaleString(DateTime.DATETIME_SHORT_WITH_SECONDS);
  };

  if (isLoading) {
    return (
      <Box className="logs-list logs-list--loading">
        <Flex align="center" justify="center" gap="2" py="9">
          <Loader2 size={20} className="logs-list__spinner" />
          <Text size="2">Loading log files...</Text>
        </Flex>
      </Box>
    );
  }

  return (
    <Box className="logs-list">
      {/* Filter */}
      <Box className="logs-list__toolbar">
        <SettingsTextInput
          value={filter}
          onChange={setFilter}
          placeholder="Filter by file name"
          className="logs-list__filter"
          icon={<Search size={16} />}
        />
      </Box>

      {/* Error alert */}
      {error && (
        <Box mb="3">
          <SettingsAlert type="error">
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Table */}
      <Table.Root className="logs-list__table">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell
              className="logs-list__header-cell logs-list__header-cell--sortable"
              onClick={() => handleSort('fileName')}
            >
              <Flex align="center" gap="1">
                File Name
                {renderSortIcon('fileName')}
              </Flex>
            </Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell
              className="logs-list__header-cell logs-list__header-cell--sortable logs-list__header-cell--size"
              onClick={() => handleSort('size')}
            >
              <Flex align="center" gap="1">
                Size
                {renderSortIcon('size')}
              </Flex>
            </Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell
              className="logs-list__header-cell logs-list__header-cell--sortable logs-list__header-cell--modified"
              onClick={() => handleSort('lastModified')}
            >
              <Flex align="center" gap="1">
                Last Modified
                {renderSortIcon('lastModified')}
              </Flex>
            </Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell className="logs-list__header-cell logs-list__header-cell--action" />
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {filteredLogs.length === 0 ? (
            <Table.Row>
              <Table.Cell colSpan={4} className="logs-list__empty-cell">
                <Flex align="center" justify="center" py="6">
                  <Text size="2" color="gray">
                    {filter ? 'No log files match your filter' : 'No log files found'}
                  </Text>
                </Flex>
              </Table.Cell>
            </Table.Row>
          ) : (
            filteredLogs.map((log) => (
              <Table.Row
                key={log.fileName}
                className="logs-list__row"
                onClick={() => onSelect(log.fileName)}
              >
                <Table.Cell className="logs-list__cell">
                  <Flex align="center" gap="2">
                    <FileText size={16} className="logs-list__file-icon" />
                    <Text>{log.fileName}</Text>
                  </Flex>
                </Table.Cell>
                <Table.Cell className="logs-list__cell logs-list__cell--size">
                  {formatSize(log.size)}
                </Table.Cell>
                <Table.Cell className="logs-list__cell logs-list__cell--modified">
                  {formatDate(log.lastModified)}
                </Table.Cell>
                <Table.Cell className="logs-list__cell logs-list__cell--action">
                  <Pencil size={16} className="logs-list__edit-icon" />
                </Table.Cell>
              </Table.Row>
            ))
          )}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}

export default LogsList;
