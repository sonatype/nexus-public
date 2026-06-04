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
  Table,
  Text,
  Badge,
  Flex,
  Spinner,
  Callout,
} from '@radix-ui/themes';
import { Package, AlertCircle, Search } from 'lucide-react';

import { FORMAT_CONFIG, type GenericResult } from './generic.types';

export interface GenericSearchResultsProps {
  /** Search results to display */
  results: readonly GenericResult[];
  /** Whether results are loading */
  loading: boolean;
  /** Error message if any */
  error?: string;
  /** Total count of results */
  totalCount: number;
  /** Callback when a result is selected */
  onSelect: (id: string) => void;
}

/**
 * Get format badge color based on format type.
 */
function getFormatColor(format: string): 'red' | 'blue' | 'purple' | 'cyan' | 'orange' | 'green' | 'gray' {
  const colorMap: Record<string, 'red' | 'blue' | 'purple' | 'cyan' | 'orange' | 'green' | 'gray'> = {
    maven2: 'red',
    npm: 'red',
    nuget: 'blue',
    pypi: 'blue',
    docker: 'cyan',
    helm: 'purple',
    go: 'cyan',
    rubygems: 'red',
    yum: 'orange',
    apt: 'purple',
    raw: 'gray',
    conan: 'blue',
    conda: 'green',
    cargo: 'orange',
    cocoapods: 'red',
    composer: 'orange',
    terraform: 'purple',
  };
  return colorMap[format] || 'gray';
}

/**
 * Results table component for generic search.
 */
export function GenericSearchResults({
  results,
  loading,
  error,
  totalCount,
  onSelect,
}: GenericSearchResultsProps): JSX.Element {
  // Loading state (initial load)
  if (loading && results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="3">
          <Spinner size="3" />
          <Text color="gray">Searching components...</Text>
        </Flex>
      </Flex>
    );
  }

  // Error state
  if (error) {
    return (
      <Callout.Root color="red">
        <Callout.Icon>
          <AlertCircle size={16} />
        </Callout.Icon>
        <Callout.Text>{error}</Callout.Text>
      </Callout.Root>
    );
  }

  // Empty state (after search with no results)
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
            <Table.ColumnHeaderCell>Name</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Format</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Version</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {results.map((result) => (
            <Table.Row
              key={result.id}
              onClick={() => onSelect(result.id)}
              style={{ cursor: 'pointer' }}
            >
              {/* Name */}
              <Table.Cell>
                <Flex direction="column" gap="1">
                  <Flex align="center" gap="2">
                    <Package size={16} />
                    <Text weight="medium">{result.name}</Text>
                  </Flex>
                  {result.group && (
                    <Text size="1" color="gray" style={{ fontFamily: 'monospace' }}>
                      {result.group}
                    </Text>
                  )}
                </Flex>
              </Table.Cell>

              {/* Format */}
              <Table.Cell>
                <Badge color={getFormatColor(result.format)} variant="soft">
                  {FORMAT_CONFIG[result.format]?.label || result.format}
                </Badge>
              </Table.Cell>

              {/* Version */}
              <Table.Cell>
                <Badge variant="outline">
                  {result.version}
                </Badge>
              </Table.Cell>

              {/* Repository */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {result.repository}
                </Text>
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>

      {loading && results.length > 0 && (
        <Flex justify="center" p="3">
          <Text size="2" color="gray">Loading more components...</Text>
        </Flex>
      )}
    </Box>
  );
}

export default GenericSearchResults;
