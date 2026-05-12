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
import { Table, Text, Flex, Box, Heading, Button, Inset, Skeleton, Badge, Tooltip } from '@radix-ui/themes';
import { RefreshCw, AlertTriangle, Skull } from 'lucide-react';
import { RefreshCw, AlertTriangle, Skull, Check } from 'lucide-react';

// Import types from simplified.types.ts - REAL DATA ONLY
import type { RepositoriesByFormatPanelProps, RepositoryFormatSummary } from './simplified.types';
import { FormatBadge } from '../../../../shared';

import './RepositoriesByFormatPanel.scss';

function ThreatsCell({ row }: { row: RepositoryFormatSummary }) {
  if (row.malwareCountsAvailable !== true) {
    return <Text size="2" color="gray">-</Text>;
  }

  if (!row.rhcSupported || row.proxyCount === 0) {
    return (
      <Tooltip content="Malicious package scanning not available for this format">
        <Text size="2" color="gray">N/A</Text>
      </Tooltip>
    );
  }

  const hcEnabled = row.hcEnabledProxyCount ?? 0;
  if (hcEnabled === 0) {
    return (
      <Tooltip content="Enable Health Check to scan for malicious packages">
        <Text size="2" color="gray">N/A</Text>
      </Tooltip>
    );
  }

  const malwareCount = row.malwareCount ?? 0;
  if (malwareCount > 0) {
    return (
      <Badge color="red" variant="soft" size="1">
        <Flex align="center" gap="1" justify="center">
          <Skull size={12} strokeWidth={2} aria-hidden />
          <Text as="span" size="1" weight="medium">{malwareCount}</Text>
        </Flex>
      </Badge>
    );
  }

  return <Text size="2" color="gray">0</Text>;
}

/**
 * Repositories by Format Panel - SIMPLIFIED
 *
 * Displays REAL DATA ONLY:
 * - Format name
 * - Proxy/Hosted/Group counts
 * - Total count
 * - Malware aggregates (when Health Check malware API is available)
 * - Online/offline counts per format
 */
export function RepositoriesByFormatPanel({
  data,
  loading = false,
  error,
  onViewRepos,
  onRetry,
}: RepositoriesByFormatPanelProps) {
  // Loading state - skeleton mirrors table structure per Tables skill
  if (loading) {
    return (
      <Flex direction="column" gap="3">
        <Heading as="h2" size="4">Repositories by Format</Heading>
        <Box className="repos-by-format-panel repos-by-format-panel--loading">
          <Inset clip="padding-box" side="bottom">
            <Box className="repos-by-format-panel__table-scroll">
            <Table.Root size="2" variant="surface" className="repos-by-format-panel__table">
                  <Table.Header>
                    <Table.Row>
                      <Table.ColumnHeaderCell px="3">Format</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell justify="center">Proxy</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell justify="center">Hosted</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell justify="center">Group</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell justify="center">Total</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell justify="center">
                        <Tooltip content="Malicious packages detected by Health Check">
                          <Text as="span" size="2" weight="medium" style={{ cursor: 'help' }}>
                            Threats
                          </Text>
                        </Tooltip>
                      </Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell justify="center">Online / Offline</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Table.Row key={i}>
                      <Table.Cell px="3"><Skeleton width={60} height={20} /></Table.Cell>
                        <Table.Cell justify="center"><Skeleton width={32} height={20} /></Table.Cell>
                        <Table.Cell justify="center"><Skeleton width={32} height={20} /></Table.Cell>
                        <Table.Cell justify="center"><Skeleton width={32} height={20} /></Table.Cell>
                        <Table.Cell justify="center"><Skeleton width={32} height={20} /></Table.Cell>
                        <Table.Cell justify="center"><Skeleton width={48} height={20} /></Table.Cell>
                        <Table.Cell justify="center"><Skeleton width={56} height={20} /></Table.Cell>
                      </Table.Row>
                    ))}
                  </Table.Body>
                </Table.Root>
            </Box>
          </Inset>
        </Box>
      </Flex>
    );
  }

  // Error state
  if (error) {
    return (
      <Flex direction="column" gap="3">
        <Heading as="h2" size="4">Repositories by Format</Heading>
        <Box className="repos-by-format-panel repos-by-format-panel--error">
          <Flex direction="column" align="center" justify="center" py="6" gap="3" p="4">
            <AlertTriangle size={32} />
            <Text size="2" color="red">{error}</Text>
            {onRetry && (
              <Button size="1" variant="soft" onClick={onRetry}>
                <RefreshCw size={14} />
                Retry
              </Button>
            )}
          </Flex>
        </Box>
      </Flex>
    );
  }

  // Empty state - Box with centered text per Tables skill
  if (!data.length) {
    return (
      <Flex direction="column" gap="3">
        <Heading as="h2" size="4">Repositories by Format</Heading>
        <Box className="repos-by-format-panel repos-by-format-panel--empty">
          <Flex direction="column" align="center" justify="center" gap="2" p="4" style={{ minHeight: '160px' }}>
            <Text size="3" weight="medium">No repositories found</Text>
            <Text size="1" color="gray">No repositories are configured for this instance.</Text>
          </Flex>
        </Box>
      </Flex>
    );
  }

  return (
    <Flex direction="column" gap="3">
      <Heading as="h2" size="4">Repositories by Format</Heading>
      <Box className="repos-by-format-panel">
        <Inset clip="padding-box" side="bottom">
          <Box className="repos-by-format-panel__table-scroll">
          <Table.Root size="2" variant="surface" className="repos-by-format-panel__table">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell px="3">Format</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell justify="center">Proxy</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell justify="center">Hosted</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell justify="center">Group</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell justify="center">Total</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell justify="center">
                      <Tooltip content="Malicious packages detected by Health Check">
                        <Text as="span" size="2" weight="medium" style={{ cursor: 'help' }}>
                          Threats
                        </Text>
                      </Tooltip>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell justify="center">Online / Offline</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>

                <Table.Body>
                  {data.map((row) => (
                    <Table.Row
                      key={row.formatCode}
                      style={{ cursor: 'pointer' }}
                      onClick={() => onViewRepos?.(row.formatCode)}
                    >
                      {/* Format - left-aligned, blue text for link affordance */}
                      <Table.Cell px="3">
                        <FormatBadge format={row.formatCode} labelColor="blue" />
                      </Table.Cell>

                      {/* Proxy count - plain text, center */}
                      <Table.Cell justify="center">
                        <Text size="2" color="gray">{row.proxyCount}</Text>
                      </Table.Cell>

                      {/* Hosted count - plain text, center */}
                      <Table.Cell justify="center">
                        <Text size="2" color="gray">{row.hostedCount}</Text>
                      </Table.Cell>

                      {/* Group count - plain text, center */}
                      <Table.Cell justify="center">
                        <Text size="2" color="gray">{row.groupCount}</Text>
                      </Table.Cell>

                      {/* Total count - plain text, center */}
                      <Table.Cell justify="center">
                        <Text size="2" color="gray" weight="medium">{row.totalCount}</Text>
                      </Table.Cell>

                      <Table.Cell justify="center">
                        <Flex justify="center">
                          <ThreatsCell row={row} />
                        </Flex>
                      </Table.Cell>

                      <Table.Cell justify="center" className="repos-by-format-panel__online-cell">
                        <Flex align="center" gap="1" justify="center" wrap="nowrap" style={{ whiteSpace: 'nowrap' }}>
                          <Text size="2" color="green">{row.onlineCount}</Text>
                          <Text size="2" color="gray">/</Text>
                          <Text size="2" color={row.offlineCount > 0 ? 'red' : 'gray'}>
                            {row.offlineCount}
                          </Text>
                        </Flex>
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
          </Box>
        </Inset>
      </Box>
    </Flex>
  );
}

export default RepositoriesByFormatPanel;
