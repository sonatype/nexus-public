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
import { Package, AlertCircle } from 'lucide-react';

import type { AptResult } from './apt.types';

export interface AptSearchResultsProps {
  /** Search results to display */
  results: readonly AptResult[];
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
 * Format a date string for display.
 */
function formatDate(isoDate?: string): string {
  if (!isoDate) return '-';
  const date = new Date(isoDate);
  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

/**
 * Results table component for Apt search.
 */
export function AptSearchResults({
  results,
  loading,
  error,
  totalCount,
  onSelect,
}: AptSearchResultsProps): JSX.Element {
  // Loading state
  if (loading && results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="3">
          <Spinner size="3" />
          <Text color="gray">Searching Apt packages...</Text>
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

  // Empty state
  if (results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="2">
          <Package size={32} />
          <Text color="gray">No packages found</Text>
          <Text size="1" color="gray">Try adjusting your search criteria</Text>
        </Flex>
      </Flex>
    );
  }

  return (
    <Box>
      <Flex justify="between" align="center" mb="3">
        <Text size="2" color="gray">
          Showing {results.length} of {totalCount} packages
        </Text>
      </Flex>

      <Table.Root>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Package</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Latest Version</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Arch</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Distribution</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Section</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Last Updated</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {results.map((result) => (
            <Table.Row
              key={result.id}
              onClick={() => onSelect(result.id)}
              style={{ cursor: 'pointer' }}
            >
              {/* Package Info */}
              <Table.Cell>
                <Flex align="center" gap="2">
                  <Package size={16} />
                  <Text weight="medium">{result.displayName}</Text>
                </Flex>
              </Table.Cell>

              {/* Version */}
              <Table.Cell>
                <Badge color="purple" variant="soft">
                  {result.latestVersion}
                </Badge>
              </Table.Cell>

              {/* Architecture */}
              <Table.Cell>
                <Badge variant="outline">
                  {result.architecture || '-'}
                </Badge>
              </Table.Cell>

              {/* Distribution */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {result.distribution || '-'}
                </Text>
              </Table.Cell>

              {/* Section */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {result.section || '-'}
                </Text>
              </Table.Cell>

              {/* Last Updated */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {formatDate(result.lastUpdated)}
                </Text>
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>

      {loading && results.length > 0 && (
        <Flex justify="center" p="3">
          <Text size="2" color="gray">Loading more packages...</Text>
        </Flex>
      )}
    </Box>
  );
}

export default AptSearchResults;
