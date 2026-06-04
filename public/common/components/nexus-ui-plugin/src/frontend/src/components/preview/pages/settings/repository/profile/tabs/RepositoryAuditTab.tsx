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

import React, { useMemo } from 'react';
import {
  Box,
  Flex,
  Text,
  Card,
  Badge,
  Button,
  Separator,
  Spinner,
  Table,
} from '@radix-ui/themes';
import { ExternalLink, ScrollText } from 'lucide-react';
import { useAuditLogApi } from '../../../../../../../utils/audit/useAuditLogApi';
import { formatAuditEvent, formatTimestamp } from '../../../../../../../utils/audit/auditEventFormatter';
import { CATEGORY_COLORS, CATEGORY_LABELS } from '../../../../../../../utils/audit/audit.constants';
import type { AuditFilters } from '../../../../../../../utils/audit/audit.types';

interface RepositoryAuditTabProps {
  repositoryName: string;
}

export function RepositoryAuditTab({ repositoryName }: RepositoryAuditTabProps): JSX.Element {
  const filters: AuditFilters = useMemo(() => ({
    categories: [],
    domains: [],
    eventTypes: [],
    dateRange: 'last-30-days' as const,
    initiator: '',
    initiators: [],
    searchQuery: '',
    repositoryName,
  }), [repositoryName]);

  const { data, loading, error } = useAuditLogApi({ filters, page: 1, limit: 5 });

  const auditUrl = `#preview/browse/audit?repositoryName=${encodeURIComponent(repositoryName)}`;

  if (loading) {
    return (
      <Flex justify="center" align="center" py="9">
        <Spinner size="3" />
      </Flex>
    );
  }

  if (error) {
    return (
      <Card size="2">
        <Flex direction="column" align="center" gap="3" py="6">
          <Text color="red" size="2">Failed to load audit events.</Text>
          <Button variant="outline" size="2" asChild>
            <a href={auditUrl}>Open Full Audit Log <ExternalLink size={14} /></a>
          </Button>
        </Flex>
      </Card>
    );
  }

  const totalEvents = data?.pagination.totalItems ?? 0;
  const recentEvents = data?.items ?? [];

  if (totalEvents === 0) {
    return (
      <Card size="2">
        <Flex direction="column" align="center" gap="3" py="8">
          <ScrollText size={40} color="var(--gray-6)" />
          <Text size="3" weight="medium" color="gray">No Audit Events</Text>
          <Text size="2" color="gray">No audit activity recorded for this repository in the last 30 days.</Text>
          <Button variant="outline" size="2" mt="2" asChild>
            <a href={auditUrl}>Open Full Audit Log <ExternalLink size={14} /></a>
          </Button>
        </Flex>
      </Card>
    );
  }

  return (
    <Flex direction="column" gap="4">
      <Card size="2">
        <Flex direction="column" gap="3">
          <Flex justify="between" align="center">
            <Flex align="center" gap="2">
              <ScrollText size={18} color="var(--accent-9)" />
              <Text size="3" weight="bold">Recent Activity</Text>
              <Badge variant="soft" color="gray" size="1">
                {totalEvents.toLocaleString()} event{totalEvents !== 1 ? 's' : ''} (30 days)
              </Badge>
            </Flex>
            <Button variant="solid" size="2" asChild>
              <a href={auditUrl}>
                View Full Audit Log <ExternalLink size={14} />
              </a>
            </Button>
          </Flex>

          <Separator size="4" />

          <Table.Root variant="surface" size="1">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Time</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Category</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Event</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Summary</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Initiator</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {recentEvents.map((event) => {
                const display = formatAuditEvent(event);
                return (
                  <Table.Row key={event.id}>
                    <Table.Cell>
                      <Text size="1" color="gray">{formatTimestamp(event.timestamp)}</Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Badge
                        color={CATEGORY_COLORS[display.category] as any}
                        variant="soft"
                        size="1"
                      >
                        {CATEGORY_LABELS[display.category]}
                      </Badge>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="1" weight="medium">{display.eventLabel}</Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="1">{display.summary}</Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="1" color="gray">{event.initiator || 'system'}</Text>
                    </Table.Cell>
                  </Table.Row>
                );
              })}
            </Table.Body>
          </Table.Root>

          {totalEvents > 5 && (
            <Box>
              <Text size="1" color="gray">
                Showing 5 of {totalEvents.toLocaleString()} events.{' '}
                <a href={auditUrl} style={{ color: 'var(--accent-9)' }}>
                  View all in Audit Log
                </a>
              </Text>
            </Box>
          )}
        </Flex>
      </Card>
    </Flex>
  );
}

export default RepositoryAuditTab;
