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
import { Package, Download, AlertCircle, ChevronRight } from 'lucide-react';

import type { NuGetResult } from './nuget.types';

interface NuGetSearchResultsProps {
  results: readonly NuGetResult[];
  loading: boolean;
  error?: string;
  totalCount: number;
  onSelect: (packageId: string) => void;
}

/**
 * NuGet search results table.
 */
export function NuGetSearchResults({
  results,
  loading,
  error,
  totalCount,
  onSelect,
}: NuGetSearchResultsProps): JSX.Element {
  if (loading && results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="3">
          <Spinner size="3" />
          <Text color="gray">Searching NuGet packages...</Text>
        </Flex>
      </Flex>
    );
  }

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
            <Table.ColumnHeaderCell>Downloads</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Versions</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Last Updated</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>License</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell aria-hidden="true" />
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {results.map((result) => (
            <Table.Row 
              key={result.id}
              onClick={() => onSelect(result.packageId)}
              style={{ cursor: 'pointer' }}
              className="nuget-result-row"
            >
              {/* Package Info */}
              <Table.Cell>
                <Flex direction="column" gap="1">
                  <Flex align="center" gap="2">
                    <Package size={16} />
                    <Text weight="medium">{result.displayName}</Text>
                  </Flex>
                  {result.description && (
                    <Text size="1" color="gray" style={{ 
                      maxWidth: '400px', 
                      overflow: 'hidden', 
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}>
                      {result.description}
                    </Text>
                  )}
                  {result.tags && result.tags.length > 0 && (
                    <Flex gap="1" wrap="wrap">
                      {result.tags.slice(0, 3).map((tag) => (
                        <Badge key={tag} size="1" variant="soft" color="gray">
                          {tag}
                        </Badge>
                      ))}
                    </Flex>
                  )}
                </Flex>
              </Table.Cell>

              {/* Version */}
              <Table.Cell>
                <Badge color="blue" variant="soft">
                  {result.latestVersion}
                </Badge>
              </Table.Cell>

              {/* Downloads */}
              <Table.Cell>
                {result.totalDownloads ? (
                  <Flex align="center" gap="1">
                    <Download size={12} />
                    <Text size="2">{formatDownloads(result.totalDownloads)}</Text>
                  </Flex>
                ) : (
                  <Text size="2" color="gray">-</Text>
                )}
              </Table.Cell>

              {/* Versions Count */}
              <Table.Cell>
                <Badge variant="outline">
                  {result.versionsCount}
                </Badge>
              </Table.Cell>

              {/* Last Updated */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {result.lastUpdated ? new Date(result.lastUpdated).toLocaleDateString() : '-'}
                </Text>
              </Table.Cell>

              {/* License */}
              <Table.Cell>
                <Text size="2" color="gray">
                  {result.license || '-'}
                </Text>
              </Table.Cell>

              <Table.Cell>
                <ChevronRight size={16} color="var(--gray-8)" aria-hidden="true" />
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}

/**
 * Format download count for display.
 */
function formatDownloads(downloads: number): string {
  if (downloads >= 1000000000) {
    return `${(downloads / 1000000000).toFixed(1)}B`;
  }
  if (downloads >= 1000000) {
    return `${(downloads / 1000000).toFixed(1)}M`;
  }
  if (downloads >= 1000) {
    return `${(downloads / 1000).toFixed(1)}K`;
  }
  return String(downloads);
}

export default NuGetSearchResults;

