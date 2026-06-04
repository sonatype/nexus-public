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
import { File, AlertCircle, Folder } from 'lucide-react';

import type { RawResult } from './raw.types';

export interface RawSearchResultsProps {
  results: RawResult[];
  loading?: boolean;
  error?: string;
  onResultClick?: (result: RawResult) => void;
  onLoadMore?: () => void;
  hasMore?: boolean;
}

/**
 * Format file size for display.
 */
function formatFileSize(bytes?: number): string {
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

export function RawSearchResults({
  results,
  loading = false,
  error,
  onResultClick,
}: RawSearchResultsProps): React.ReactElement {
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

  if (loading && results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="3">
          <Spinner size="3" />
          <Text color="gray">Searching files...</Text>
        </Flex>
      </Flex>
    );
  }

  if (results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="2">
          <Folder size={32} />
          <Text color="gray">No files found</Text>
          <Text size="1" color="gray">Try adjusting your search filters</Text>
        </Flex>
      </Flex>
    );
  }

  return (
    <Box>
      <Flex justify="between" align="center" mb="3">
        <Text size="2" color="gray">
          {results.length} file{results.length !== 1 ? 's' : ''} found
        </Text>
      </Flex>

      <Table.Root>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>File</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Path</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Size</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Last Modified</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {results.map((result) => (
            <Table.Row
              key={result.id}
              onClick={() => onResultClick?.(result)}
              style={{ cursor: onResultClick ? 'pointer' : 'default' }}
            >
              {/* File Name */}
              <Table.Cell>
                <Flex align="center" gap="2">
                  <File size={16} />
                  <Text weight="medium">{result.name}</Text>
                </Flex>
              </Table.Cell>

              {/* Path */}
              <Table.Cell>
                <Text size="2" color="gray" style={{ fontFamily: 'monospace' }}>
                  {result.path || '-'}
                </Text>
              </Table.Cell>

              {/* Repository */}
              <Table.Cell>
                <Badge variant="soft" color="gray">
                  {result.repository || '-'}
                </Badge>
              </Table.Cell>

              {/* Size */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {formatFileSize(result.size)}
                </Text>
              </Table.Cell>

              {/* Last Modified */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {formatDate(result.lastModified)}
                </Text>
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>

      {loading && results.length > 0 && (
        <Flex justify="center" p="3">
          <Text size="2" color="gray">Loading more files...</Text>
        </Flex>
      )}
    </Box>
  );
}

export default RawSearchResults;
