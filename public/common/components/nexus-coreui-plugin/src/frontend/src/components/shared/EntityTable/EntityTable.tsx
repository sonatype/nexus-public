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
import { Flex, Table, Text } from '@radix-ui/themes';
import { ArrowDown, ArrowUp, ChevronRight } from 'lucide-react';

import { LoadingState } from '../LoadingState';
import { ErrorState } from '../ErrorState';
import { EmptyState } from '../EmptyState';

import './EntityTable.scss';

/**
 * Table column definition.
 */
export interface TableColumn<T> {
  /** Unique column identifier */
  id: string;
  /** Column header text */
  header: string;
  /** Key of T or render function */
  accessor: keyof T | ((item: T) => React.ReactNode);
  /** Whether column is sortable */
  sortable?: boolean;
  /** Column width (CSS value) */
  width?: string;
  /** Text alignment */
  align?: 'left' | 'center' | 'right';
  /** Custom cell class name */
  className?: string;
}

export interface EntityTableProps<T> {
  /** Data items to display */
  data: T[];
  /** Column definitions */
  columns: TableColumn<T>[];
  /** Function to get unique key for each row */
  getRowKey: (item: T) => string;
  /** Callback when a row is clicked */
  onRowClick?: (item: T) => void;
  /** Loading state */
  loading?: boolean;
  /** Loading message */
  loadingMessage?: string;
  /** Error message */
  error?: string;
  /** Callback to retry on error */
  onRetry?: () => void;
  /** Custom empty state component */
  emptyState?: React.ReactNode;
  /** Current sort column */
  sortBy?: string;
  /** Current sort direction */
  sortDirection?: 'asc' | 'desc';
  /** Callback when sort changes */
  onSort?: (column: string) => void;
  /** Show row arrow indicator */
  showRowArrow?: boolean;
  /** Custom class name */
  className?: string;
  /** Whether rows are clickable */
  clickable?: boolean;
  /** Aria label for the table */
  ariaLabel?: string;
  /** Optional function to generate data-testid for each row */
  getRowTestId?: (item: T) => string;
}

/**
 * EntityTable provides a generic, sortable table component.
 *
 * Features:
 * - Generic type support for any data shape
 * - Sortable columns with visual indicators
 * - Loading, error, and empty states
 * - Clickable rows with keyboard navigation
 * - Accessible with proper ARIA attributes
 *
 * @example
 * ```tsx
 * interface Repository {
 *   name: string;
 *   type: string;
 *   format: string;
 * }
 *
 * <EntityTable<Repository>
 *   data={repositories}
 *   columns={[
 *     { id: 'name', header: 'Name', accessor: 'name', sortable: true },
 *     { id: 'type', header: 'Type', accessor: 'type' },
 *     { id: 'format', header: 'Format', accessor: (repo) => repo.format.toUpperCase() },
 *   ]}
 *   getRowKey={(repo) => repo.name}
 *   onRowClick={(repo) => navigate(`/repos/${repo.name}`)}
 *   sortBy="name"
 *   sortDirection="asc"
 *   onSort={(column) => setSortBy(column)}
 * />
 * ```
 */
export function EntityTable<T>({
  data,
  columns,
  getRowKey,
  onRowClick,
  loading = false,
  loadingMessage = 'Loading...',
  error,
  onRetry,
  emptyState,
  sortBy,
  sortDirection = 'asc',
  onSort,
  showRowArrow = true,
  className = '',
  clickable = true,
  ariaLabel = 'Data table',
  getRowTestId,
}: EntityTableProps<T>): JSX.Element {
  // Handle header click for sorting
  const handleHeaderClick = useCallback(
    (column: TableColumn<T>) => {
      if (column.sortable && onSort) {
        onSort(column.id);
      }
    },
    [onSort]
  );

  // Handle row click
  const handleRowClick = useCallback(
    (item: T) => () => {
      if (clickable && onRowClick) {
        onRowClick(item);
      }
    },
    [clickable, onRowClick]
  );

  // Handle row keyboard navigation
  const handleRowKeyDown = useCallback(
    (item: T) => (e: React.KeyboardEvent) => {
      if ((e.key === 'Enter' || e.key === ' ') && clickable && onRowClick) {
        e.preventDefault();
        onRowClick(item);
      }
    },
    [clickable, onRowClick]
  );

  // Render cell value
  const renderCellValue = (column: TableColumn<T>, item: T): React.ReactNode => {
    if (typeof column.accessor === 'function') {
      return column.accessor(item);
    }
    const value = item[column.accessor];
    return value !== null && value !== undefined ? String(value) : '';
  };

  // Loading state
  if (loading) {
    return (
      <div className={`entity-table entity-table--loading ${className}`}>
        <LoadingState message={loadingMessage} />
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className={`entity-table entity-table--error ${className}`}>
        <ErrorState message={error} onRetry={onRetry} />
      </div>
    );
  }

  // Empty state
  if (data.length === 0) {
    return (
      <div className={`entity-table entity-table--empty ${className}`}>
        {emptyState || (
          <Flex justify="center" p="6">
            <Text color="gray">No data available</Text>
          </Flex>
        )}
      </div>
    );
  }

  // Calculate total column count for colspan
  const totalColumns = columns.length + (showRowArrow && clickable ? 1 : 0);

  return (
    <Table.Root className={`entity-table ${className}`} aria-label={ariaLabel}>
      <Table.Header>
        <Table.Row>
          {columns.map((column) => {
            const isSorted = sortBy === column.id;
            const sortIcon =
              isSorted && sortDirection === 'asc' ? (
                <ArrowUp size={14} aria-hidden="true" />
              ) : isSorted && sortDirection === 'desc' ? (
                <ArrowDown size={14} aria-hidden="true" />
              ) : null;

            return (
              <Table.ColumnHeaderCell
                key={column.id}
                style={{ width: column.width }}
                className={`entity-table__header ${column.sortable ? 'entity-table__header--sortable' : ''} ${column.className || ''}`}
                onClick={column.sortable ? () => handleHeaderClick(column) : undefined}
                aria-sort={isSorted ? sortDirection === 'asc' ? 'ascending' : 'descending' : undefined}
              >
                <Flex
                  align="center"
                  gap="1"
                  justify={column.align === 'center' ? 'center' : column.align === 'right' ? 'end' : 'start'}
                >
                  <Text size="2" weight="medium">
                    {column.header}
                  </Text>
                  {sortIcon}
                </Flex>
              </Table.ColumnHeaderCell>
            );
          })}
          {showRowArrow && clickable && (
            <Table.ColumnHeaderCell style={{ width: '40px' }} />
          )}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((item) => {
          const key = getRowKey(item);
          const isRowClickable = clickable && onRowClick;

          return (
            <Table.Row
              key={key}
              className={`entity-table__row ${isRowClickable ? 'entity-table__row--clickable' : ''}`}
              onClick={isRowClickable ? handleRowClick(item) : undefined}
              onKeyDown={isRowClickable ? handleRowKeyDown(item) : undefined}
              tabIndex={isRowClickable ? 0 : undefined}
              role={isRowClickable ? 'button' : undefined}
              aria-label={isRowClickable ? `View ${key}` : undefined}
              data-testid={getRowTestId ? getRowTestId(item) : undefined}
            >
              {columns.map((column) => (
                <Table.Cell
                  key={column.id}
                  className={`entity-table__cell ${column.className || ''}`}
                  style={{ textAlign: column.align || 'left' }}
                >
                  {renderCellValue(column, item)}
                </Table.Cell>
              ))}
              {showRowArrow && clickable && (
                <Table.Cell className="entity-table__cell entity-table__cell--arrow">
                  <ChevronRight size={16} className="entity-table__arrow" aria-hidden="true" />
                </Table.Cell>
              )}
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table.Root>
  );
}

export default EntityTable;


