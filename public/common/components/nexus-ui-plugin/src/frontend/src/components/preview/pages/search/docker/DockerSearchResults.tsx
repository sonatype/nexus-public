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
import { Container, AlertCircle } from 'lucide-react';

import type { DockerResult } from './docker.types';

export interface DockerSearchResultsProps {
  /** Search results to display */
  results: readonly DockerResult[];
  /** Whether results are loading */
  loading: boolean;
  /** Error message if any */
  error?: string;
  /** Total count of results */
  totalCount: number;
  /** Callback when a result is selected */
  onSelect: (id: string) => void;
  /** Current sort field */
  sortField?: 'relevance' | 'lastUpdated' | 'name';
  /** Current sort direction */
  sortDirection?: 'asc' | 'desc';
  /** Callback when sort changes */
  onSortChange?: (field: 'relevance' | 'lastUpdated' | 'name', direction: 'asc' | 'desc') => void;
}

/**
 * Format file size for display.
 */
function formatSize(bytes?: number): string {
  if (!bytes) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
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
 * Results table component for Docker search.
 */
export function DockerSearchResults({
  results,
  loading,
  error,
  totalCount,
  onSelect,
}: DockerSearchResultsProps): JSX.Element {
  // Loading state
  if (loading && results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="3">
          <Spinner size="3" />
          <Text color="gray">Searching Docker images...</Text>
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
          <Container size={32} />
          <Text color="gray">No Docker images found</Text>
          <Text size="1" color="gray">Try adjusting your search criteria</Text>
        </Flex>
      </Flex>
    );
  }

  return (
    <Box>
      <Flex justify="between" align="center" mb="3">
        <Text size="2" color="gray">
          {totalCount} image{totalCount !== 1 ? 's' : ''} found
        </Text>
      </Flex>

      <Table.Root>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Image Name</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Latest Tag</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Tags</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Size</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
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
              {/* Image Name */}
              <Table.Cell>
                <Flex align="center" gap="2">
                  <Container size={16} />
                  <Text weight="medium">{result.imageName}</Text>
                </Flex>
              </Table.Cell>

              {/* Latest Tag */}
              <Table.Cell>
                <Badge color="cyan" variant="soft">
                  {result.latestTag || 'latest'}
                </Badge>
              </Table.Cell>

              {/* Tags Count */}
              <Table.Cell>
                <Badge variant="outline">
                  {result.tagsCount || 1}
                </Badge>
              </Table.Cell>

              {/* Size */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {formatSize(result.size)}
                </Text>
              </Table.Cell>

              {/* Repository */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {result.repository || '-'}
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
          <Text size="2" color="gray">Loading more images...</Text>
        </Flex>
      )}
    </Box>
  );
}

export default DockerSearchResults;
