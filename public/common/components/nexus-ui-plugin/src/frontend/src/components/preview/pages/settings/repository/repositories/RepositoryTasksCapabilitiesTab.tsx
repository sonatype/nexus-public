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
  Flex,
  Text,
  Badge,
  Button,
  Card,
  Spinner,
  Table,
  Separator,
} from '@radix-ui/themes';
import {
  CheckCircle,
  Clock,
  XCircle,
  Zap,
} from 'lucide-react';
import ExtJS from '../../../../../../interface/ExtJS';
import { useRepositoryTasksCapabilities } from './useRepositoryTasksCapabilities';

interface RepositoryTasksCapabilitiesTabProps {
  repositoryName: string;
}

// Maps a task's `lastRunResult` value to a badge. Do NOT feed `currentState`
// values (e.g. WAITING/RUNNING) in here — those belong to a different field
// with a different vocabulary.
function getLastRunResultBadge(lastRunResult: string | undefined): JSX.Element {
  switch (lastRunResult?.toLowerCase()) {
    case 'ok':
    case 'success':
    case 'completed':
      return (
        <Badge color="green" size="1">
          <CheckCircle size={12} /> Success
        </Badge>
      );
    case 'failed':
    case 'error':
    case 'errorrunning':
      return (
        <Badge color="red" size="1">
          <XCircle size={12} /> Failed
        </Badge>
      );
    default:
      return (
        <Badge color="gray" size="1">
          {lastRunResult || '—'}
        </Badge>
      );
  }
}

function formatRelativeTime(dateString: string | undefined): string {
  if (!dateString) return '—';
  const date = new Date(dateString);
  const diffMs = Date.now() - date.getTime();
  if (diffMs < 0) return 'just now';
  const diffMins = Math.floor(diffMs / (1000 * 60));
  if (diffMins < 1) return 'just now';
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (diffMins < 60) return `${diffMins} min ago`;
  if (diffHours < 24) return `${diffHours} hours ago`;
  return `${diffDays} days ago`;
}

/**
 * RepositoryTasksCapabilitiesTab — Settings-page tab that surfaces
 * repo-scoped scheduled tasks and capabilities. Self-fetching; mounts only
 * when the tab is opened (Radix Tabs unmounts inactive content), so the
 * two REST calls do not block Settings-page first paint.
 *
 * The tab does NOT surface HTTP settings (instance-wide, not repo-scoped).
 * Each section is gated on its own read permission so a user with only
 * `nexus:tasks:read` won't see an empty capabilities table (and vice versa).
 */
export function RepositoryTasksCapabilitiesTab({
  repositoryName,
}: RepositoryTasksCapabilitiesTabProps): JSX.Element {
  const canReadTasks = ExtJS.checkPermission('nexus:tasks:read');
  const canReadCapabilities = ExtJS.checkPermission('nexus:capabilities:read');

  const { tasks, capabilities, loading, error, refetch } =
    useRepositoryTasksCapabilities(repositoryName, {
      canReadTasks,
      canReadCapabilities,
    });

  if (loading) {
    return (
      <Flex justify="center" align="center" py="9" data-testid="tasks-capabilities-loading">
        <Spinner size="3" />
      </Flex>
    );
  }

  if (error) {
    return (
      <Card size="2">
        <Flex direction="column" align="center" gap="3" py="6">
          <Text color="red" size="2">
            Failed to load tasks and capabilities.
          </Text>
          <Text size="1" color="gray">
            {error}
          </Text>
          <Button variant="outline" size="2" onClick={refetch}>
            Retry
          </Button>
        </Flex>
      </Card>
    );
  }

  return (
    <Flex direction="column" gap="4">
      {canReadTasks && (
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <Clock size={18} color="var(--accent-9)" />
              <Text size="3" weight="bold">
                Scheduled Tasks
              </Text>
              <Badge variant="soft" color="gray" size="1">
                {tasks.length} task{tasks.length === 1 ? '' : 's'}
              </Badge>
            </Flex>

            <Separator size="4" />

            {tasks.length > 0 ? (
              <Table.Root variant="surface" size="1">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell>Task</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Schedule</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Last Run</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {tasks.map((task) => (
                    <Table.Row key={task.id}>
                      <Table.Cell>
                        <Text size="1" weight="medium">
                          {task.name}
                        </Text>
                        <Text size="1" color="gray" as="div">
                          {task.type}
                        </Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="1">{task.schedule || 'Manual'}</Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="1">{formatRelativeTime(task.lastRun)}</Text>
                      </Table.Cell>
                      <Table.Cell>{getLastRunResultBadge(task.lastRunResult)}</Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            ) : (
              <Box py="4">
                <Text size="2" color="gray">
                  No scheduled tasks target this repository.
                </Text>
              </Box>
            )}
          </Flex>
        </Card>
      )}

      {canReadCapabilities && (
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <Zap size={18} color="var(--accent-9)" />
              <Text size="3" weight="bold">
                Capabilities
              </Text>
              <Badge variant="soft" color="gray" size="1">
                {capabilities.length} capabilit{capabilities.length === 1 ? 'y' : 'ies'}
              </Badge>
            </Flex>

            <Separator size="4" />

            {capabilities.length > 0 ? (
              <Table.Root variant="surface" size="1">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell>Capability</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Notes</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {capabilities.map((capability) => (
                    <Table.Row key={capability.id}>
                      <Table.Cell>
                        <Text size="1" weight="medium">
                          {capability.type}
                        </Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Badge color={capability.enabled ? 'green' : 'gray'} size="1">
                          {capability.enabled ? 'Active' : 'Disabled'}
                        </Badge>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="1" color="gray">
                          {capability.notes || '—'}
                        </Text>
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            ) : (
              <Box py="4">
                <Text size="2" color="gray">
                  No capabilities are scoped to this repository.
                </Text>
              </Box>
            )}
          </Flex>
        </Card>
      )}
    </Flex>
  );
}

export default RepositoryTasksCapabilitiesTab;
