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

import React, { useCallback, useMemo, useState } from 'react';
import { Badge, Box, Button, Flex, Table, Text, TextField } from '@radix-ui/themes';
import { Search, RefreshCw, Package } from 'lucide-react';
import { useRouter } from '@uirouter/react';

import { SortableTableHeader, type SortDirection, EmptyState } from '../../../../shared';
import { ReconcileTask } from './types';

import './RecoveryModeTasksTable.scss';

type SortKey = 'name' | 'type' | 'status' | 'lastRun' | 'lastRunResult';

/** Maps raw reconcile task type ids to the labels shown in the Type column. */
const TYPE_LABELS: Record<string, string> = {
  'blobstore.planReconciliation': 'PlanReconciliationTask',
  'blobstore.executeReconciliationPlan': 'ExecuteReconcilePlanTask',
};

/** UI-Router state name for the Tasks detail page. */
const TASK_DETAIL_STATE = 'preview.admin.system.tasks.detail';

const PLACEHOLDER = '—';

function typeLabel(type: string): string {
  return TYPE_LABELS[type] || type;
}

function formatLastRun(lastRun?: string | null): string {
  if (!lastRun) {
    return PLACEHOLDER;
  }
  const date = new Date(lastRun);
  if (Number.isNaN(date.getTime())) {
    return PLACEHOLDER;
  }
  return date.toLocaleString();
}

/** Radix Badge color for a task's current state. */
function statusColor(state?: string | null): 'blue' | 'green' | 'red' | 'amber' | 'gray' {
  if (!state) {
    return 'gray';
  }
  const s = state.toUpperCase();
  if (s.startsWith('RUNNING')) {
    return 'blue';
  }
  if (s.startsWith('OK') || s.startsWith('COMPLETED')) {
    return 'green';
  }
  if (s.startsWith('FAILED') || s.startsWith('ERROR')) {
    return 'red';
  }
  if (s.startsWith('WAITING') || s.startsWith('BLOCKED')) {
    return 'amber';
  }
  return 'gray';
}

/** Comparable value for a task under a given sort key. */
function sortValue(task: ReconcileTask, key: SortKey): string | number {
  switch (key) {
    case 'name':
      return task.name.toLowerCase();
    case 'type':
      return typeLabel(task.type).toLowerCase();
    case 'status':
      return (task.currentState || '').toLowerCase();
    case 'lastRun': {
      const t = task.lastRun ? new Date(task.lastRun).getTime() : NaN;
      // Sort missing dates last (treat as -Infinity, then handled by direction).
      return Number.isNaN(t) ? -Infinity : t;
    }
    case 'lastRunResult':
      return (task.lastRunResult || '').toLowerCase();
    default:
      return '';
  }
}

export interface RecoveryModeTasksTableProps {
  tasks: ReconcileTask[];
}

/**
 * Data Repair Tasks table for the Recovery Mode page.
 * Renders the reconcile tasks with client-side search filtering by name/type,
 * and a "No tasks found" state when a search yields no matches.
 */
export function RecoveryModeTasksTable({ tasks }: RecoveryModeTasksTableProps): React.ReactElement {
  const router = useRouter();
  const [query, setQuery] = useState('');
  // Default sort: Name ascending (matches the design).
  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [sortDir, setSortDir] = useState<SortDirection>('asc');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) {
      return tasks;
    }
    return tasks.filter(
      (t) =>
        t.name.toLowerCase().includes(q) ||
        typeLabel(t.type).toLowerCase().includes(q)
    );
  }, [tasks, query]);

  const sorted = useMemo(() => {
    if (!sortKey || !sortDir) {
      return filtered;
    }
    const dir = sortDir === 'asc' ? 1 : -1;
    return [...filtered].sort((a, b) => {
      const av = sortValue(a, sortKey);
      const bv = sortValue(b, sortKey);
      if (av < bv) return -1 * dir;
      if (av > bv) return 1 * dir;
      return 0;
    });
  }, [filtered, sortKey, sortDir]);

  const isSearching = query.trim().length > 0;
  const noResults = isSearching && sorted.length === 0;
  // No reconcile tasks at all (not a search miss) -> show the empty state.
  const isEmpty = !isSearching && sorted.length === 0;

  const handleSort = useCallback((key: string, direction: SortDirection) => {
    setSortKey(key as SortKey);
    setSortDir(direction);
  }, []);

  // Clicking a row navigates to that task's detail page (mirrors classic UI),
  // using UI-Router so the transition lifecycle (guards, analytics) applies.
  const handleRowClick = useCallback((taskId: string) => {
    router.stateService.go(TASK_DETAIL_STATE, { taskId });
  }, [router]);

  return (
    <Box className="recovery-mode-tasks">
      <Box mb="3" maxWidth="360px">
        <TextField.Root
          placeholder="Search tasks…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Search tasks"
          data-testid="recovery-tasks-search"
        >
          <TextField.Slot>
            <Search size={16} />
          </TextField.Slot>
        </TextField.Root>
      </Box>

      <Table.Root variant="surface" className="recovery-mode-tasks__table">
        <Table.Header>
          <Table.Row>
            <SortableTableHeader sortKey="name" currentSortKey={sortKey} currentSortDirection={sortDir} onSort={handleSort}>
              Name
            </SortableTableHeader>
            <SortableTableHeader sortKey="type" currentSortKey={sortKey} currentSortDirection={sortDir} onSort={handleSort}>
              Type
            </SortableTableHeader>
            <SortableTableHeader sortKey="status" currentSortKey={sortKey} currentSortDirection={sortDir} onSort={handleSort}>
              Status
            </SortableTableHeader>
            <SortableTableHeader sortKey="lastRun" currentSortKey={sortKey} currentSortDirection={sortDir} onSort={handleSort}>
              Last run
            </SortableTableHeader>
            <SortableTableHeader sortKey="lastRunResult" currentSortKey={sortKey} currentSortDirection={sortDir} onSort={handleSort}>
              Last result
            </SortableTableHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {isEmpty ? (
            <Table.Row>
              <Table.Cell colSpan={5}>
                <EmptyState
                  icon={Package}
                  title="No data repair tasks"
                  description="Data repair tasks appear here when recovery mode creates reconciliation work."
                  className="recovery-mode-tasks__empty-state"
                />
              </Table.Cell>
            </Table.Row>
          ) : noResults ? (
            <Table.Row>
              <Table.Cell colSpan={5}>
                <Flex
                  direction="column"
                  align="center"
                  justify="center"
                  gap="3"
                  py="6"
                  className="recovery-mode-tasks__empty"
                  data-testid="recovery-tasks-no-results"
                >
                  <Text size="4" weight="bold">No tasks found</Text>
                  <Text size="2" color="gray">
                    No tasks match &quot;{query.trim()}&quot;. Try a different search term.
                  </Text>
                  <Button variant="solid" onClick={() => setQuery('')}>
                    <RefreshCw size={16} />
                    Clear Search
                  </Button>
                </Flex>
              </Table.Cell>
            </Table.Row>
          ) : (
            sorted.map((task) => (
              <Table.Row
                key={task.id}
                data-testid="recovery-task-row"
                className="recovery-mode-tasks__row recovery-mode-tasks__row--clickable"
                onClick={() => handleRowClick(task.id)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleRowClick(task.id);
                  }
                }}
                tabIndex={0}
                role="button"
                aria-label={`Open task ${task.name}`}
              >
                <Table.Cell>{task.name}</Table.Cell>
                <Table.Cell>{typeLabel(task.type)}</Table.Cell>
                <Table.Cell>
                  {task.currentState ? (
                    <Badge color={statusColor(task.currentState)} radius="full">
                      {task.currentState}
                    </Badge>
                  ) : (
                    PLACEHOLDER
                  )}
                </Table.Cell>
                <Table.Cell>{formatLastRun(task.lastRun)}</Table.Cell>
                <Table.Cell>{task.lastRunResult || PLACEHOLDER}</Table.Cell>
              </Table.Row>
            ))
          )}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}

export default RecoveryModeTasksTable;
