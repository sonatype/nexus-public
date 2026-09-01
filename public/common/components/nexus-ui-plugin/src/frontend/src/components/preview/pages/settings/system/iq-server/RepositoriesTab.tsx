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

import React, { useCallback, useEffect, useMemo, useReducer, useRef, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Callout,
  Card,
  Checkbox,
  Flex,
  Heading,
  IconButton,
  Link,
  Select,
  Table,
  Text,
  TextField,
} from '@radix-ui/themes';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, Info, Search, X } from 'lucide-react';

import { FORMAT_LABELS, useToast } from '../../../../shared';
import { SortableTableHeader, type SortDirection } from '../../../../shared/SortableTableHeader/SortableTableHeader';
import {
  DashboardRepository,
  GlobalEvaluationSettings,
  useHostedRepoEvaluation,
} from './useHostedRepoEvaluation';
import {
  MONITORING_FILTER_OPTIONS,
  type MonitoringFilter,
} from './types';
import { useDebounced } from '../../../../shared/hooks';

import './HostedRepoEvaluationSetupPage.scss';

const PAGE_SIZE = 25;
const SEARCH_DEBOUNCE_MS = 500; // matches Classic UI search debounce

const formatLabel = (f: string) => FORMAT_LABELS[f] || f;

type QueryState = {
  page: number;
  formatFilter: string;
  monitoringFilter: MonitoringFilter;
  sortBy: 'name' | 'format' | 'size' | 'componentCount' | 'monitoring';
  sortDir: 'asc' | 'desc';
};

type QueryAction =
  | { type: 'setPage'; page: number }
  | { type: 'setFormatFilter'; value: string }
  | { type: 'setMonitoringFilter'; value: MonitoringFilter }
  | { type: 'setSort'; sortBy: QueryState['sortBy']; sortDir: 'asc' | 'desc' };

function queryReducer(state: QueryState, action: QueryAction): QueryState {
  switch (action.type) {
    case 'setPage':
      return { ...state, page: action.page };
    case 'setFormatFilter':
      return { ...state, formatFilter: action.value, page: 1 };
    case 'setMonitoringFilter':
      return { ...state, monitoringFilter: action.value, page: 1 };
    case 'setSort':
      return { ...state, sortBy: action.sortBy, sortDir: action.sortDir, page: 1 };
    default:
      return state;
  }
}

export interface RepositoriesTabProps {
  hasExistingConfig: boolean;
  setHasExistingConfig: (v: boolean) => void;
  pristineSettings: GlobalEvaluationSettings;
  setSettingsStaged: (v: boolean) => void;
  formats: string[];
  onMonitoredCountChange: (n: number) => void;
  isActiveTab: boolean;
}

export function RepositoriesTab({
  hasExistingConfig,
  setHasExistingConfig,
  pristineSettings,
  setSettingsStaged,
  formats,
  onMonitoredCountChange,
  isActiveTab,
}: RepositoriesTabProps) {
  const {
    fetchRepositories,
    applySelectionDelta,
    putSettingsWithRepos,
  } = useHostedRepoEvaluation();

  const [query, dispatch] = useReducer(queryReducer, {
    page: 1,
    formatFilter: 'all',
    monitoringFilter: 'all',
    sortBy: 'name',
    sortDir: 'asc',
  });

  const [rows, setRows] = useState<DashboardRepository[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [monitoredCount, setMonitoredCount] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [selection, setSelection] = useState<Map<string, boolean>>(new Map());
  const [applyingAction, setApplyingAction] = useState(false);
  const [repoSaveError, setRepoSaveError] = useState<string | null>(null);
  const loadRepoPageRequestIdRef = useRef(0);
  // Abort the previous in-flight repository fetch when a new one fires,
  // so a slow backend doesn't waste cycles on results the user has already discarded.
  const abortControllerRef = useRef<AbortController | null>(null);
  const toast = useToast();

  // Cancel the in-flight request on unmount to release the network socket.
  useEffect(() => () => abortControllerRef.current?.abort(), []);

  const debouncedSearch = useDebounced(searchInput, SEARCH_DEBOUNCE_MS);

  // Reset page to 1 when debouncedSearch changes
  useEffect(() => {
    dispatch({ type: 'setPage', page: 1 });
  }, [debouncedSearch]);

  const loadRepositoryPage = useCallback(async () => {
    const requestId = ++loadRepoPageRequestIdRef.current;
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;
    const result = await fetchRepositories({
      page: query.page,
      pageSize: PAGE_SIZE,
      search: debouncedSearch || undefined,
      formatFilter: query.formatFilter,
      monitoringFilter: query.monitoringFilter,
      sortBy: query.sortBy,
      sortDir: query.sortDir,
    }, controller.signal);
    if (requestId !== loadRepoPageRequestIdRef.current) return; // stale
    setRows(result.rows);
    setTotalCount(result.totalCount);
    setMonitoredCount(result.monitoredCount);
    onMonitoredCountChange(result.monitoredCount);
  }, [fetchRepositories, query.page, debouncedSearch, query.formatFilter, query.monitoringFilter, query.sortBy, query.sortDir, onMonitoredCountChange]);

  useEffect(() => {
    loadRepositoryPage();
  }, [loadRepositoryPage]);

  const handleSort = useCallback((key: string, direction: SortDirection) => {
    dispatch({ type: 'setSort', sortBy: key as QueryState['sortBy'], sortDir: direction });
  }, []);

  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE));
  const startIdx = totalCount === 0 ? 0 : (query.page - 1) * PAGE_SIZE + 1;
  const endIdx = Math.min(query.page * PAGE_SIZE, totalCount);

  // Backend sorts name/format/size/componentCount; monitoring is client-side on the current page.
  const sortedRows = useMemo(() => {
    if (query.sortBy !== 'monitoring') return rows;
    const dir = query.sortDir === 'desc' ? -1 : 1;
    return [...rows].sort((a, b) => ((a.isMonitored ? 1 : 0) - (b.isMonitored ? 1 : 0)) * dir);
  }, [rows, query.sortBy, query.sortDir]);

  const pageRowIds = useMemo(() => sortedRows.map(r => r.id), [sortedRows]);
  const allOnPageSelected = pageRowIds.length > 0 && pageRowIds.every(id => selection.has(id));
  const someOnPageSelected = pageRowIds.some(id => selection.has(id)) && !allOnPageSelected;

  // All-Enabled → Disable only; All-Disabled → Enable only; Mixed → both.
  const hasMonitoredInSelection = useMemo(() => {
    for (const v of selection.values()) if (v) return true;
    return false;
  }, [selection]);
  const hasUnmonitoredInSelection = useMemo(() => {
    for (const v of selection.values()) if (!v) return true;
    return false;
  }, [selection]);
  const showEnableAction = hasUnmonitoredInSelection;
  const showDisableAction = hasMonitoredInSelection;

  const toggleRow = useCallback((id: string, isMonitored: boolean) => {
    setSelection(prev => {
      const next = new Map(prev);
      if (next.has(id)) next.delete(id);
      else next.set(id, isMonitored);
      return next;
    });
  }, []);

  const toggleSelectAllOnPage = useCallback(() => {
    setSelection(prev => {
      const next = new Map(prev);
      if (allOnPageSelected) {
        pageRowIds.forEach(id => next.delete(id));
      } else {
        for (const r of sortedRows) next.set(r.id, r.isMonitored);
      }
      return next;
    });
  }, [allOnPageSelected, pageRowIds, sortedRows]);

  const handleClearSelection = useCallback(() => {
    setSelection(new Map());
  }, []);

  const handleApplyAction = useCallback(
    async (action: 'enable' | 'disable') => {
      setApplyingAction(true);
      try {
        const ids = [...selection.keys()];
        if (ids.length === 0) {
          if (!hasExistingConfig) {
            setRepoSaveError('You have not selected any repositories for monitoring. Please select at least one repository.');
          }
          return;
        }
        setRepoSaveError(null);

        let result;
        if (action === 'enable' && !hasExistingConfig) {
          // First-time enable: atomically PUT staged settings + repo IDs.
          // pristineSettings holds whatever the user configured on the Settings tab
          // (or DEFAULT_SETTINGS if they proceeded without changing anything).
          result = await putSettingsWithRepos(pristineSettings, ids);
          if (result.ok) {
            setHasExistingConfig(true);
            setSettingsStaged(false);
          }
        } else {
          const delta = action === 'enable'
            ? { addRepositoryIds: ids }
            : { removeRepositoryIds: ids };
          result = await applySelectionDelta(delta);
        }

        if (!result.ok) {
          toast.error(result.message || `Failed to ${action} monitoring`);
          return;
        }
        toast.success(action === 'enable' ? 'Monitoring enabled' : 'Monitoring disabled');
        setSelection(new Map());
        await loadRepositoryPage();
      } finally {
        setApplyingAction(false);
      }
    },
    [applySelectionDelta, putSettingsWithRepos, hasExistingConfig, pristineSettings, loadRepositoryPage, selection, toast, setRepoSaveError, setHasExistingConfig, setSettingsStaged]
  );

  const selectionCount = selection.size;

  return (
    <>
      <Card mt="4">
        <Box p="4">
          <Heading size="4" as="h2" mb="1">Hosted Repositories</Heading>
          <Text size="2" color="gray" as="div" mb="3">
            {hasExistingConfig
              ? 'Choose the hosted repositories you want covered by global evaluation.'
              : 'All hosted repositories start as Disabled. Enable monitoring on the ones you want covered by global evaluation.'}
          </Text>

          {/* Gap 2: inline error when Enable clicked with 0 repos selected on first-time */}
          {repoSaveError && (
            <Box mb="3">
              <Callout.Root color="red" variant="soft" size="1">
                <Callout.Icon>
                  <Info size={16} aria-hidden="true" />
                </Callout.Icon>
                <Callout.Text>{repoSaveError}</Callout.Text>
              </Callout.Root>
            </Box>
          )}

          {/* Toolbar */}
          <Flex gap="3" align="center" mb="3" wrap="wrap">
            <Box style={{ flex: '1 1 280px' }}>
              <TextField.Root
                placeholder="Search repositories…"
                value={searchInput}
                onChange={e => setSearchInput(e.target.value)}
              >
                <TextField.Slot side="left"><Search size={14} /></TextField.Slot>
                {searchInput && (
                  <TextField.Slot side="right">
                    <IconButton
                      variant="ghost"
                      color="gray"
                      size="1"
                      onClick={() => setSearchInput('')}
                      aria-label="Clear search"
                    >
                      <X size={14} />
                    </IconButton>
                  </TextField.Slot>
                )}
              </TextField.Root>
            </Box>
            <Select.Root value={query.formatFilter} onValueChange={v => dispatch({ type: 'setFormatFilter', value: v })}>
              <Select.Trigger placeholder="All formats" style={{ minWidth: 140 }} aria-label="Filter by format" />
              <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                <Select.Item value="all">All formats</Select.Item>
                {[...formats]
                  .sort((a, b) => formatLabel(a).localeCompare(formatLabel(b)))
                  .map(f => (
                    <Select.Item key={f} value={f}>{formatLabel(f)}</Select.Item>
                  ))}
              </Select.Content>
            </Select.Root>
            <Select.Root value={query.monitoringFilter} onValueChange={v => dispatch({ type: 'setMonitoringFilter', value: v as MonitoringFilter })}>
              <Select.Trigger placeholder="All monitoring" style={{ minWidth: 170 }} aria-label="Filter by monitoring status" />
              <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                {MONITORING_FILTER_OPTIONS.map(o => (
                  <Select.Item key={o.value} value={o.value}>{o.label}</Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Flex>

          {/* Bulk-action success/error surfaces via toast */}

          {/* Table */}
          <Box style={{ overflowX: 'auto' }}>
            <Table.Root>
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell style={{ width: 44 }}>
                    <Checkbox
                      checked={allOnPageSelected ? true : someOnPageSelected ? 'indeterminate' : false}
                      onCheckedChange={toggleSelectAllOnPage}
                      aria-label="Select all on page"
                    />
                  </Table.ColumnHeaderCell>
                  <SortableTableHeader
                    sortKey="name"
                    currentSortKey={query.sortBy}
                    currentSortDirection={query.sortDir}
                    onSort={handleSort}
                  >
                    Repository Name
                  </SortableTableHeader>
                  <SortableTableHeader
                    sortKey="format"
                    currentSortKey={query.sortBy}
                    currentSortDirection={query.sortDir}
                    onSort={handleSort}
                  >
                    Format
                  </SortableTableHeader>
                  <SortableTableHeader
                    sortKey="size"
                    currentSortKey={query.sortBy}
                    currentSortDirection={query.sortDir}
                    onSort={handleSort}
                  >
                    Size
                  </SortableTableHeader>
                  <SortableTableHeader
                    sortKey="componentCount"
                    currentSortKey={query.sortBy}
                    currentSortDirection={query.sortDir}
                    onSort={handleSort}
                  >
                    No. Components
                  </SortableTableHeader>
                  <SortableTableHeader
                    sortKey="monitoring"
                    currentSortKey={query.sortBy}
                    currentSortDirection={query.sortDir}
                    onSort={handleSort}
                  >
                    Monitoring
                  </SortableTableHeader>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {sortedRows.length === 0 ? (
                  <Table.Row>
                    <Table.Cell colSpan={6}>
                      <Box py="6" style={{ textAlign: 'center' }}>
                        <Text size="2" color="gray">No hosted repositories found.</Text>
                      </Box>
                    </Table.Cell>
                  </Table.Row>
                ) : (
                  sortedRows.map(row => {
                    const checked = selection.has(row.id);
                    return (
                      <Table.Row key={row.id}>
                        <Table.Cell>
                          <Checkbox
                            checked={checked}
                            onCheckedChange={() => toggleRow(row.id, row.isMonitored)}
                            aria-label={`Select ${row.name}`}
                          />
                        </Table.Cell>
                        <Table.Cell>
                          <Flex align="center" gap="2">
                            {/* ?tab=evaluation is a hash query param parsed by readExplicitTabFromHash, not a URL query string */}
                            <Link
                              href={`#preview/admin/repository/repositories/${encodeURIComponent(row.name)}?tab=evaluation`}
                              size="2"
                              weight="medium"
                              title={`Open ${row.name} evaluation settings`}
                            >
                              {row.name}
                            </Link>
                            {row.hasCustomConfig && !(row.isMonitored && checked) && (
                              <Badge size="1" color="blue" variant="soft">Custom</Badge>
                            )}
                          </Flex>
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2" color="gray">{formatLabel(row.format)}</Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2" color="gray">{row.size != null && row.size > 0 ? formatBytes(row.size) : '—'}</Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2" color="gray">{row.componentCount != null && row.componentCount > 0 ? row.componentCount : '—'}</Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Badge
                            size="1"
                            variant={row.isMonitored ? 'soft' : 'outline'}
                            color={row.isMonitored ? 'green' : 'gray'}
                          >
                            {row.isMonitored ? 'Enabled' : 'Disabled'}
                          </Badge>
                        </Table.Cell>
                      </Table.Row>
                    );
                  })
                )}
              </Table.Body>
            </Table.Root>
          </Box>

          {/* Pagination */}
          {totalCount > 0 && (
            <Box
              mt="3"
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr auto 1fr',
                alignItems: 'center',
                gap: 'var(--space-4)',
                width: '100%',
              }}
            >
              <Text size="2" style={{ justifySelf: 'start', whiteSpace: 'nowrap' }}>
                {startIdx.toLocaleString()}-{endIdx.toLocaleString()} of {totalCount.toLocaleString()}
              </Text>
              <nav
                aria-label={`Pagination, page ${query.page} of ${totalPages}`}
                style={{ justifySelf: 'center' }}
              >
                <Flex align="center" gap="1">
                  <IconButton
                    variant="outline"
                    color="gray"
                    size="2"
                    disabled={query.page === 1}
                    onClick={() => dispatch({ type: 'setPage', page: 1 })}
                    aria-label="First page"
                  >
                    <ChevronsLeft size={16} />
                  </IconButton>
                  <IconButton
                    variant="outline"
                    color="gray"
                    size="2"
                    disabled={query.page === 1}
                    onClick={() => dispatch({ type: 'setPage', page: Math.max(1, query.page - 1) })}
                    aria-label="Previous page"
                  >
                    <ChevronLeft size={16} />
                  </IconButton>
                  {(() => {
                    const startPage = Math.max(1, query.page - 2);
                    const endPage = Math.min(totalPages, query.page + 2);
                    const nodes: React.ReactNode[] = [];
                    const pageBtn = (p: number) => (
                      <Button
                        key={p}
                        variant="outline"
                        color="gray"
                        size="2"
                        highContrast={query.page === p}
                        onClick={() => dispatch({ type: 'setPage', page: p })}
                        style={{
                          backgroundColor: query.page === p ? 'var(--gray-12)' : undefined,
                          color: query.page === p ? 'var(--gray-1)' : undefined,
                        }}
                      >
                        {p}
                      </Button>
                    );
                    if (startPage > 1) {
                      nodes.push(pageBtn(1));
                      if (startPage > 2) {
                        nodes.push(
                          <Text key="ellipsis-start" size="2" color="gray">…</Text>
                        );
                      }
                    }
                    for (let p = startPage; p <= endPage; p++) {
                      nodes.push(pageBtn(p));
                    }
                    if (endPage < totalPages) {
                      if (endPage < totalPages - 1) {
                        nodes.push(
                          <Text key="ellipsis-end" size="2" color="gray">…</Text>
                        );
                      }
                      nodes.push(pageBtn(totalPages));
                    }
                    return nodes;
                  })()}
                  <IconButton
                    variant="outline"
                    color="gray"
                    size="2"
                    disabled={query.page === totalPages}
                    onClick={() => dispatch({ type: 'setPage', page: Math.min(totalPages, query.page + 1) })}
                    aria-label="Next page"
                  >
                    <ChevronRight size={16} />
                  </IconButton>
                  <IconButton
                    variant="outline"
                    color="gray"
                    size="2"
                    disabled={query.page === totalPages}
                    onClick={() => dispatch({ type: 'setPage', page: totalPages })}
                    aria-label="Last page"
                  >
                    <ChevronsRight size={16} />
                  </IconButton>
                </Flex>
              </nav>
              <Box style={{ justifySelf: 'end' }} />
            </Box>
          )}
        </Box>
      </Card>

      {/* Floating action toast — only when this is the active tab and a selection exists */}
      {isActiveTab && selection.size > 0 && (
        <div
          className="hosted-repo-eval-setup-page__action-toast"
          role="toolbar"
          aria-label={`${selectionCount} repositor${selectionCount === 1 ? 'y' : 'ies'} selected`}
        >
          <Text size="2" weight="medium">{selectionCount} repositor{selectionCount === 1 ? 'y' : 'ies'} selected:</Text>
          {showEnableAction && (
            <Button
              size="1"
              variant="soft"
              color="gray"
              highContrast
              style={{
                background: 'transparent',
                color: 'var(--gray-1)',
                border: '1px solid var(--gray-6)',
              }}
              onClick={() => handleApplyAction('enable')}
              disabled={applyingAction}
              data-testid="hr-action-enable"
            >
              Enable Monitoring
            </Button>
          )}
          {showDisableAction && (
            <Button
              size="1"
              variant="soft"
              color="gray"
              highContrast
              style={{
                background: 'transparent',
                color: 'var(--gray-1)',
                border: '1px solid var(--gray-6)',
              }}
              onClick={() => handleApplyAction('disable')}
              disabled={applyingAction}
              data-testid="hr-action-disable"
            >
              Disable Monitoring
            </Button>
          )}
          <IconButton
            size="1"
            variant="ghost"
            color="gray"
            onClick={handleClearSelection}
            aria-label="Clear selection"
            style={{ color: 'var(--gray-1)' }}
          >
            <X size={14} />
          </IconButton>
        </div>
      )}
    </>
  );
}


function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} kB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
}
