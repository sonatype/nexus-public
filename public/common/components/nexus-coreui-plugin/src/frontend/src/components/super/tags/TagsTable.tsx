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
import {
  Box,
  Button,
  Callout,
  Card,
  DropdownMenu,
  Flex,
  IconButton,
  Inset,
  Spinner,
  Table,
  Text,
} from '@radix-ui/themes';
import { ChevronRight, X, MoreHorizontal } from 'lucide-react';

import { SortableTableHeader, type SortDirection } from '../../../components/shared';
import type { TagWithCount, TagSortField } from './hooks/useFilteredTags';

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-';
  try {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return '-';
  }
}

const STRINGS = {
  columns: {
    name: 'Name',
    components: 'Components',
    created: 'Created',
    lastUpdated: 'Last Updated',
    actions: '',
  },
  loadingMessage: 'Loading tags...',
  emptyMessage: 'No tags found',
  emptyMessageFiltered: 'No tags match your current filters',
};

export interface TagsTableProps {
  tags: TagWithCount[];
  loading?: boolean;
  error?: string | null;
  hasFilters?: boolean;
  sortField: TagSortField;
  sortDirection: SortDirection;
  onSort: (field: TagSortField) => void;
  onRetry: () => void;
}

/**
 * Tags table matching RepositoryListTable structure: Card + Inset + Radix Table.
 */
export function TagsTable({
  tags,
  loading = false,
  error,
  hasFilters = false,
  sortField,
  sortDirection,
  onSort,
  onRetry,
}: TagsTableProps): JSX.Element {
  const handleSort = useCallback(
    (key: string, _direction: SortDirection) => {
      onSort(key as TagSortField);
    },
    [onSort]
  );

  const handleRowClick = useCallback((tagName: string) => {
    window.location.hash = `#preview/browse/tags/${encodeURIComponent(tagName)}`;
  }, []);

  const handleRowKeyDown = useCallback((tagName: string) => (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleRowClick(tagName);
    }
  }, [handleRowClick]);

  if (loading) {
    return (
      <Flex direction="column" align="center" justify="center" gap="3" p="9">
        <Spinner size="3" />
        <Text color="gray">{STRINGS.loadingMessage}</Text>
      </Flex>
    );
  }

  if (error) {
    return (
      <Box p="4">
        <Callout.Root color="red" mb="3">
          <Callout.Icon>
            <X size={16} />
          </Callout.Icon>
          <Callout.Text>{error}</Callout.Text>
        </Callout.Root>
        <Flex justify="center">
          <Button variant="outline" size="2" onClick={onRetry}>
            Retry
          </Button>
        </Flex>
      </Box>
    );
  }

  return (
    <Card size="1">
      <Inset clip="padding-box" side="bottom">
        <Box style={{ overflowX: 'auto' }}>
          <Table.Root size="2">
            <Table.Header>
              <Table.Row>
                <SortableTableHeader
                  sortKey="name"
                  currentSortKey={sortField}
                  currentSortDirection={sortDirection}
                  onSort={handleSort}
                  align="left"
                >
                  {STRINGS.columns.name}
                </SortableTableHeader>
                <SortableTableHeader
                  sortKey="componentCount"
                  currentSortKey={sortField}
                  currentSortDirection={sortDirection}
                  onSort={handleSort}
                  align="right"
                >
                  {STRINGS.columns.components}
                </SortableTableHeader>
                <SortableTableHeader
                  sortKey="firstCreated"
                  currentSortKey={sortField}
                  currentSortDirection={sortDirection}
                  onSort={handleSort}
                  align="left"
                >
                  {STRINGS.columns.created}
                </SortableTableHeader>
                <SortableTableHeader
                  sortKey="lastUpdated"
                  currentSortKey={sortField}
                  currentSortDirection={sortDirection}
                  onSort={handleSort}
                  align="left"
                >
                  {STRINGS.columns.lastUpdated}
                </SortableTableHeader>
                <Table.ColumnHeaderCell justify="end" aria-label="Row actions" pr="2" />
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {tags.length === 0 ? (
                <Table.Row>
                  <Table.Cell colSpan={5}>
                    <Flex justify="center" p="6">
                      <Text color="gray">
                        {hasFilters ? STRINGS.emptyMessageFiltered : STRINGS.emptyMessage}
                      </Text>
                    </Flex>
                  </Table.Cell>
                </Table.Row>
              ) : (
                tags.map((tag) => (
                  <Table.Row
                    key={tag.name}
                    onClick={() => handleRowClick(tag.name)}
                    onKeyDown={handleRowKeyDown(tag.name)}
                    tabIndex={0}
                    role="button"
                    aria-label={`View tag ${tag.name}`}
                    style={{ cursor: 'pointer' }}
                  >
                    <Table.Cell>
                      <Text size="2" weight="medium" color="blue">
                        {tag.name}
                      </Text>
                    </Table.Cell>
                    <Table.Cell style={{ textAlign: 'right' }}>
                      <Text size="2">{tag.componentCount}</Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="2">{formatDate(tag.firstCreated)}</Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="2">{formatDate(tag.lastUpdated)}</Text>
                    </Table.Cell>
                    <Table.Cell justify="end" pr="2">
                      <DropdownMenu.Root>
                        <DropdownMenu.Trigger>
                          <IconButton
                            variant="ghost"
                            color="gray"
                            size="1"
                            onClick={(e) => e.stopPropagation()}
                            aria-label="Row actions"
                          >
                            <MoreHorizontal size={16} />
                          </IconButton>
                        </DropdownMenu.Trigger>
                        <DropdownMenu.Content align="end" onClick={(e) => e.stopPropagation()}>
                          <DropdownMenu.Item onClick={() => handleRowClick(tag.name)}>
                            View Tag Details
                          </DropdownMenu.Item>
                        </DropdownMenu.Content>
                      </DropdownMenu.Root>
                    </Table.Cell>
                  </Table.Row>
                ))
              )}
            </Table.Body>
          </Table.Root>
        </Box>
      </Inset>
    </Card>
  );
}
