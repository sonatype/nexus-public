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
import {
  Box,
  Button,
  Callout,
  Flex,
  Spinner,
  Table,
  Text,
} from '@radix-ui/themes';
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronRight, RefreshCw, X } from 'lucide-react';

import type { Tag, TagSortField, SortDirection, TagsListProps } from '../tags.types';

/**
 * UI Strings for the TagsList component.
 */
const STRINGS = {
  columns: {
    name: 'Tag Name',
    firstCreated: 'First Created',
    lastUpdated: 'Last Updated',
  },
  emptyMessage: 'No tags found.',
  loadingMessage: 'Loading tags...',
  retryButton: 'Retry',
};

/**
 * Format a date string for display.
 */
function formatDate(dateString: string): string {
  try {
    return new Date(dateString).toLocaleString();
  } catch {
    return dateString;
  }
}

/**
 * Get the sort icon for a column header.
 */
function SortIcon({
  field,
  currentField,
  direction,
}: {
  field: TagSortField;
  currentField: TagSortField;
  direction: SortDirection;
}): JSX.Element {
  if (field !== currentField) {
    return <ArrowUpDown size={14} className="sort-icon sort-icon--inactive" />;
  }
  return direction === 'asc' ? (
    <ArrowUp size={14} className="sort-icon sort-icon--active" />
  ) : (
    <ArrowDown size={14} className="sort-icon sort-icon--active" />
  );
}

/**
 * Sortable column header component.
 */
function SortableHeader({
  label,
  field,
  currentField,
  direction,
  onSort,
}: {
  label: string;
  field: TagSortField;
  currentField: TagSortField;
  direction: SortDirection;
  onSort: (field: TagSortField) => void;
}): JSX.Element {
  return (
    <Table.ColumnHeaderCell
      className="tags-list__header-cell tags-list__header-cell--sortable"
      onClick={() => onSort(field)}
      style={{ cursor: 'pointer' }}
    >
      <Flex align="center" gap="1">
        <Text size="2" weight="medium">
          {label}
        </Text>
        <SortIcon field={field} currentField={currentField} direction={direction} />
      </Flex>
    </Table.ColumnHeaderCell>
  );
}

/**
 * TagsList component displays a sortable table of tags.
 *
 * Features:
 * - Sortable columns (name, firstCreated, lastUpdated)
 * - Loading state with spinner
 * - Error state with retry button
 * - Empty state message
 * - Click row to navigate to tag detail
 */
export function TagsList({
  tags,
  loading,
  error,
  sortField,
  sortDirection,
  onSort,
  onSelect,
  onRetry,
}: TagsListProps): JSX.Element {
  // Handle row click
  const handleRowClick = (tagName: string) => () => {
    onSelect(tagName);
  };

  // Handle row keyboard navigation
  const handleRowKeyDown = (tagName: string) => (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSelect(tagName);
    }
  };

  // Loading state
  if (loading) {
    return (
      <Flex
        direction="column"
        align="center"
        justify="center"
        gap="3"
        p="9"
        data-testid="tags-list-loading"
      >
        <Spinner size="3" />
        <Text color="gray">{STRINGS.loadingMessage}</Text>
      </Flex>
    );
  }

  // Error state
  if (error) {
    return (
      <Box p="4" data-testid="tags-list-error">
        <Callout.Root color="red">
          <Callout.Icon>
            <X size={16} />
          </Callout.Icon>
          <Callout.Text>{error}</Callout.Text>
        </Callout.Root>
        <Flex justify="center" mt="4">
          <Button variant="soft" onClick={onRetry}>
            <RefreshCw size={16} />
            {STRINGS.retryButton}
          </Button>
        </Flex>
      </Box>
    );
  }

  return (
    <Table.Root className="tags-list" data-testid="tags-list">
      <Table.Header>
        <Table.Row>
          <SortableHeader
            label={STRINGS.columns.name}
            field="id"
            currentField={sortField}
            direction={sortDirection}
            onSort={onSort}
          />
          <SortableHeader
            label={STRINGS.columns.firstCreated}
            field="firstCreatedTime"
            currentField={sortField}
            direction={sortDirection}
            onSort={onSort}
          />
          <SortableHeader
            label={STRINGS.columns.lastUpdated}
            field="lastUpdatedTime"
            currentField={sortField}
            direction={sortDirection}
            onSort={onSort}
          />
          <Table.ColumnHeaderCell style={{ width: '40px' }} />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tags.length === 0 ? (
          <Table.Row>
            <Table.Cell colSpan={4}>
              <Flex justify="center" p="6">
                <Text color="gray">{STRINGS.emptyMessage}</Text>
              </Flex>
            </Table.Cell>
          </Table.Row>
        ) : (
          tags.map((tag) => (
            <Table.Row
              key={tag.id}
              className="tags-list__row"
              onClick={handleRowClick(tag.id)}
              onKeyDown={handleRowKeyDown(tag.id)}
              tabIndex={0}
              role="button"
              aria-label={`View tag ${tag.id}`}
              data-testid={`tag-row-${tag.id}`}
            >
              <Table.Cell>
                <Text weight="medium">{tag.id}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text>{formatDate(tag.firstCreatedTime)}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text>{formatDate(tag.lastUpdatedTime)}</Text>
              </Table.Cell>
              <Table.Cell>
                <ChevronRight size={16} className="chevron-icon" aria-hidden="true" />
              </Table.Cell>
            </Table.Row>
          ))
        )}
      </Table.Body>
    </Table.Root>
  );
}

export default TagsList;

