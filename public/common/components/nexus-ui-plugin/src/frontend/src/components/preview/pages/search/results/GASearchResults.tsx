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
import { Box, Flex, Text, Table, Badge, Spinner, Callout, } from '@radix-ui/themes';
import { AlertCircle, ChevronRight, ArrowUp, ArrowDown, Package } from 'lucide-react';
import { FormatBadge } from '../../../shared';
import type { GAResult } from '../core';

export interface GASearchResultsProps {
  results: readonly GAResult[];
  loading: boolean;
  error?: string;
  totalCount: number;
  onSelect: (gaId: string) => void;
  sortField?: 'relevance' | 'lastUpdated' | 'name';
  sortDirection?: 'asc' | 'desc';
  onSortChange?: (field: 'relevance' | 'lastUpdated' | 'name', direction: 'asc' | 'desc') => void;
}

export function GASearchResults({
  results,
  loading,
  error,
  totalCount,
  onSelect,
  sortField,
  sortDirection,
  onSortChange,
}: GASearchResultsProps): JSX.Element {
  const handleSort = (field: 'relevance' | 'lastUpdated' | 'name'): void => {
    if (!onSortChange) return;
    const newDirection = sortField === field && sortDirection === 'asc' ? 'desc' : 'asc';
    onSortChange(field, newDirection);
  };

  const getSortIndicator = (field: 'relevance' | 'lastUpdated' | 'name'): JSX.Element | null => {
    if (sortField !== field) return null;
    return sortDirection === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} />;
  };

  if (loading && results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="3">
          <Spinner size="3" />
          <Text color="gray">Searching Maven artifacts...</Text>
        </Flex>
      </Flex>
    );
  }

  if (error) {
    return (
      <Callout.Root color="red">
        <Callout.Icon><AlertCircle size={16} /></Callout.Icon>
        <Callout.Text>{error}</Callout.Text>
      </Callout.Root>
    );
  }

  if (results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="2">
          <Package size={32} />
          <Text color="gray">No artifacts found</Text>
          <Text size="1" color="gray">Try adjusting your search criteria</Text>
        </Flex>
      </Flex>
    );
  }

  return (
    <Box>
      <Flex justify="between" align="center" mb="3">
        <Text size="2" color="gray">Showing {results.length} of {totalCount} artifacts</Text>
      </Flex>

      <Table.Root>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell onClick={() => handleSort('name')} style={{ cursor: 'pointer' }}>
              <Flex align="center" gap="1">
                Artifact {getSortIndicator('name')}
              </Flex>
            </Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Latest Version</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Versions</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell onClick={() => handleSort('lastUpdated')} style={{ cursor: 'pointer' }}>
              <Flex align="center" gap="1">
                Last Updated {getSortIndicator('lastUpdated')}
              </Flex>
            </Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell></Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {results.map((result) => (
            <Table.Row key={result.gaId} onClick={() => onSelect(result.gaId)} style={{ cursor: 'pointer' }}>
              <Table.Cell>
                <Flex direction="column" gap="1">
                  <Flex align="center" gap="2">
                    <FormatBadge format={result.format} size={16} showLabel={false} />
                    <Text weight="medium">{result.displayName}</Text>
                  </Flex>
                  <Text size="1" color="gray">{result.namespace}</Text>
                </Flex>
              </Table.Cell>
              <Table.Cell>
                <Badge color="orange" variant="soft">{result.latestVersion || '-'}</Badge>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{result.versionsCount || '-'}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{result.repositoriesCount} repos</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">
                  {result.lastUpdated ? new Date(result.lastUpdated).toLocaleDateString() : '-'}
                </Text>
              </Table.Cell>
              <Table.Cell><ChevronRight size={16} color="var(--gray-8)" /></Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}

export default GASearchResults;
