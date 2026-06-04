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
import { Box, Flex, Text, Table, Badge, Spinner, Callout, Code } from '@radix-ui/themes';
import { Search, AlertCircle } from 'lucide-react';
import type { CustomSearchResult } from './custom.types';

export interface CustomSearchResultsProps {
  results: readonly CustomSearchResult[];
  loading: boolean;
  error?: string;
  totalCount: number;
  isInitial?: boolean;
}

const FORMAT_COLORS: Record<string, 'orange' | 'red' | 'blue' | 'purple' | 'gray'> = {
  maven2: 'orange',
  npm: 'red',
  docker: 'blue',
  nuget: 'purple',
};

function getFormatColor(format: string): 'orange' | 'red' | 'blue' | 'purple' | 'gray' {
  return FORMAT_COLORS[format] || 'gray';
}

function formatDate(isoDate: string): string {
  const date = new Date(isoDate);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function CustomSearchResults({
  results,
  loading,
  error,
  totalCount,
  isInitial = true,
}: CustomSearchResultsProps): JSX.Element {
  // Initial state
  if (isInitial && results.length === 0 && !loading && !error) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="2">
          <Search size={48} color="var(--gray-8)" />
          <Text size="3" color="gray">Build Your Search</Text>
          <Text size="2" color="gray">Add filter criteria above and click Search to find components.</Text>
        </Flex>
      </Flex>
    );
  }

  // Loading state
  if (loading && results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="3">
          <Spinner size="3" />
          <Text color="gray">Searching...</Text>
        </Flex>
      </Flex>
    );
  }

  // Error state
  if (error) {
    return (
      <Callout.Root color="red">
        <Callout.Icon><AlertCircle size={16} /></Callout.Icon>
        <Callout.Text>{error}</Callout.Text>
      </Callout.Root>
    );
  }

  // Empty state
  if (results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="2">
          <Search size={32} />
          <Text color="gray">No components found</Text>
          <Text size="1" color="gray">Try adjusting your search criteria</Text>
        </Flex>
      </Flex>
    );
  }

  return (
    <Box>
      <Flex justify="between" align="center" mb="3">
        <Text size="2" color="gray">
          {totalCount} component{totalCount !== 1 ? 's' : ''} found
        </Text>
      </Flex>

      <Table.Root>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Format</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Name</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Group</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Version</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Last Modified</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {results.map((result) => (
            <Table.Row key={result.id}>
              <Table.Cell>
                <Badge color={getFormatColor(result.format)} variant="soft">
                  {result.format}
                </Badge>
              </Table.Cell>
              <Table.Cell>
                <Text weight="medium">{result.name}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{result.group || '—'}</Text>
              </Table.Cell>
              <Table.Cell>
                <Code size="2">{result.version}</Code>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{result.repository}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{formatDate(result.lastModified)}</Text>
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>

      {loading && results.length > 0 && (
        <Flex justify="center" p="3">
          <Text size="2" color="gray">Loading more results...</Text>
        </Flex>
      )}
    </Box>
  );
}

export default CustomSearchResults;
