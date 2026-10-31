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

import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Box,
  Card,
  Checkbox,
  DropdownMenu,
  Flex,
  IconButton,
  Inset,
  Spinner,
  Table,
  Text,
} from '@radix-ui/themes';
import { ChevronDown, ChevronRight, MoreHorizontal, RotateCcw, Trash2 } from 'lucide-react';

import {
  SortableTableHeader,
  TablePagination,
  type SortDirection,
} from '../../../../shared';
import {
  DeletedItem,
  DELETED_ITEM_TYPE_LABELS,
} from './types';
import { fetchDeletedItemChildren } from './useTrashApi';

// =============================================================================
// CONSTANTS
// =============================================================================

const MAX_SELECTION = 250;

/** Number of columns in the table, used for full-width child status rows (colSpan). */
const TOTAL_COLUMNS = 8;

/**
 * Lazily-loaded children of an expanded row.
 * `children === null` means "not fetched yet" (distinct from fetched-but-empty `[]`).
 */
interface ChildState {
  children: DeletedItem[] | null;
  loading: boolean;
  error?: string;
}

// =============================================================================
// TYPES
// =============================================================================

export interface TrashTableProps {
  /** Current page items (already sliced by server) */
  items: DeletedItem[];
  /** Total count of items matching the current query */
  total: number;
  /** Current offset in the result set */
  offset: number;
  /** Page size */
  limit: number;
  /** Current sort string, e.g. 'deletedOn:desc' */
  sort: string;
  /** Currently selected item IDs */
  selectedIds: Set<string>;
  /** Called when selection changes */
  onSelectionChange: (ids: Set<string>) => void;
  /** Called when user clicks Restore on a single row */
  onRestoreClick: (item: DeletedItem) => void;
  /** Called when user clicks Purge on a single row */
  onPurgeClick: (item: DeletedItem) => void;
  /** Called when page changes; receives new offset */
  onPageChange: (offset: number) => void;
  /** Called when page size changes */
  onPageSizeChange?: (pageSize: number) => void;
  /** Called when sort changes; receives combined sort string e.g. 'name:asc' */
  onSort: (sort: string) => void;
}

// =============================================================================
// HELPERS
// =============================================================================

/** Format ISO date string to a readable date */
function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

/** Extract just the filename from a full path */
function extractFilename(name: string): string {
  const lastSlash = name.lastIndexOf('/');
  return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
}

/** Compute days remaining until retention expires */
function getDaysUntilPurge(retentionExpiresOn: string): number {
  const now = Date.now();
  const expiry = new Date(retentionExpiresOn).getTime();
  return Math.ceil((expiry - now) / (1000 * 60 * 60 * 24));
}


// =============================================================================
// SUB-COMPONENTS
// =============================================================================

interface RetentionCountdownProps {
  retentionExpiresOn: string;
}

function RetentionCountdown({ retentionExpiresOn }: RetentionCountdownProps) {
  const days = getDaysUntilPurge(retentionExpiresOn);
  const isImminent = days <= 3;

  if (days <= 0) {
    return (
      <Text size="1" color="red" weight="medium">
        Purge imminent
      </Text>
    );
  }

  return (
    <Text size="1" color={isImminent ? 'red' : 'gray'} weight={isImminent ? 'medium' : 'regular'}>
      {days}d until purge
    </Text>
  );
}

interface ChildAssetRowsProps {
  childState: ChildState | undefined;
}

/**
 * Renders the expanded child asset rows of a soft-deleted component (indented, display-only). Shows a
 * loading spinner, an error, or an empty-state row as appropriate. Children are never checkbox-selectable
 * and carry no per-row actions — Restore/Purge is driven from the parent component row.
 */
function ChildAssetRows({ childState }: ChildAssetRowsProps) {
  const statusRow = (content: React.ReactNode) => (
    <Table.Row>
      <Table.Cell />
      <Table.Cell colSpan={TOTAL_COLUMNS - 1} style={{ paddingLeft: 40 }}>
        {content}
      </Table.Cell>
    </Table.Row>
  );

  if (!childState || childState.loading) {
    return statusRow(
      <Flex align="center" gap="2">
        <Spinner size="1" />
        <Text size="1" color="gray">Loading assets…</Text>
      </Flex>,
    );
  }
  if (childState.error) {
    return statusRow(<Text size="1" color="red">{childState.error}</Text>);
  }
  const children = childState.children ?? [];
  if (children.length === 0) {
    return statusRow(<Text size="1" color="gray">No soft-deleted assets under this component.</Text>);
  }

  return (
    <>
      {children.map((child) => (
        <Table.Row key={child.id} data-testid={`child-row-${child.id}`} style={{ background: 'var(--gray-a1)' }}>
          {/* No checkbox: children are display-only, not part of bulk selection. */}
          <Table.Cell pl="3" />
          <Table.Cell style={{ maxWidth: 420, paddingLeft: 40 }}>
            <Flex direction="column" gap="1" style={{ minWidth: 0 }}>
              <Text size="2" truncate title={child.name}>
                {child.path ? extractFilename(child.name) : child.name}
              </Text>
              {child.path && (
                <Text size="1" color="gray" truncate title={child.path} style={{ fontFamily: 'monospace' }}>
                  {child.path}
                </Text>
              )}
            </Flex>
          </Table.Cell>
          <Table.Cell>
            <Text size="2" color="gray">{DELETED_ITEM_TYPE_LABELS[child.entityType]}</Text>
          </Table.Cell>
          <Table.Cell>
            <Text size="2" color={child.repository ? undefined : 'gray'}>{child.repository ?? '—'}</Text>
          </Table.Cell>
          <Table.Cell>
            <Text size="2" color={child.format ? undefined : 'gray'}>{child.format ?? '—'}</Text>
          </Table.Cell>
          <Table.Cell>
            <Flex direction="column" gap="1">
              <Text size="2">{formatDate(child.deletedOn)}</Text>
              <RetentionCountdown retentionExpiresOn={child.retentionExpiresOn} />
            </Flex>
          </Table.Cell>
          <Table.Cell>
            <Text size="2" title={child.deletedByName ? child.deletedBy : undefined}>
              {child.deletedByName || child.deletedBy}
            </Text>
          </Table.Cell>
          {/* No per-row actions: child assets are display-only. Restore/Purge is driven from the
              parent component row. Empty cell keeps column alignment with the header's action column. */}
          <Table.Cell justify="end" pr="3" />
        </Table.Row>
      ))}
    </>
  );
}

// =============================================================================
// MAIN COMPONENT
// =============================================================================

/**
 * TrashTable
 *
 * Radix Table.Root with checkbox selection, per-row Restore button, and
 * TablePagination. Does not manage its own filter state — receives pre-filtered
 * items from the parent page.
 */
export function TrashTable({
  items,
  total,
  offset,
  limit,
  sort,
  selectedIds,
  onSelectionChange,
  onRestoreClick,
  onPurgeClick,
  onPageChange,
  onPageSizeChange,
  onSort,
}: TrashTableProps) {
  const currentPage = Math.floor(offset / limit) + 1;
  const totalPages = Math.max(1, Math.ceil(total / limit));

  const [currentSortKey, currentSortDirection] = sort.split(':') as [string, SortDirection | undefined];

  const handleSortChange = useCallback(
    (key: string, direction: SortDirection) => {
      onSort(`${key}:${direction ?? 'asc'}`);
    },
    [onSort],
  );

  const handlePageChange = useCallback(
    (page: number) => {
      onPageChange((page - 1) * limit);
    },
    [onPageChange, limit],
  );

  const allPageSelected =
    items.length > 0 && items.every((item) => selectedIds.has(item.id));
  const somePageSelected = items.some((item) => selectedIds.has(item.id));

  const handleSelectAll = useCallback(
    (checked: boolean) => {
      const next = new Set(selectedIds);
      for (const item of items) {
        if (checked) {
          if (next.size >= MAX_SELECTION) break;
          next.add(item.id);
        } else {
          next.delete(item.id);
        }
      }
      onSelectionChange(next);
    },
    [items, selectedIds, onSelectionChange]
  );

  const handleRowSelect = useCallback(
    (id: string, checked: boolean) => {
      const next = new Set(selectedIds);
      if (checked) {
        if (next.size >= MAX_SELECTION) return;
        next.add(id);
      } else {
        next.delete(id);
      }
      onSelectionChange(next);
    },
    [selectedIds, onSelectionChange]
  );

  // Expandable component rows: a soft-deleted component can reveal its soft-deleted child assets.
  // Children are display-only -- neither checkbox-selectable nor individually actionable (Restore/Purge
  // is driven from the parent component row) -- so top-level selection and pagination are unaffected.
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [childrenCache, setChildrenCache] = useState<Map<string, ChildState>>(new Map());

  // Refs let toggleExpand read the current expand/cache/in-flight state without stale closures while keeping
  // the state updaters pure. The fetch must live OUTSIDE the updater: React can replay an updater (StrictMode
  // in dev, or a rebased concurrent render) with the same base state, which would fire duplicate requests if
  // the fetch were inside it (NEXUS-51733).
  const expandedIdsRef = useRef(expandedIds);
  const childrenCacheRef = useRef(childrenCache);
  const inFlightRef = useRef<Set<string>>(new Set());
  useEffect(() => {
    expandedIdsRef.current = expandedIds;
  }, [expandedIds]);
  useEffect(() => {
    childrenCacheRef.current = childrenCache;
  }, [childrenCache]);

  // Collapse and drop cached children whenever the page's items change (page/sort/filter change, or a
  // refetch after a restore/purge). Re-expanding then fetches fresh children, so no stale rows persist.
  useEffect(() => {
    setExpandedIds(new Set());
    setChildrenCache(new Map());
    inFlightRef.current.clear();
  }, [items]);

  const toggleExpand = useCallback((item: DeletedItem) => {
    const id = item.id;
    const isExpanding = !expandedIdsRef.current.has(id);
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });

    if (!isExpanding) {
      return;
    }
    // Fetch on first expand only. Guard on the in-flight ref and the cache; a previously-errored entry is NOT
    // treated as fetched, so collapsing and re-expanding retries the fetch (the error row stays visible until then).
    const cached = childrenCacheRef.current.get(id);
    if (inFlightRef.current.has(id) || (cached != null && cached.error == null)) {
      return;
    }
    inFlightRef.current.add(id);
    setChildrenCache((c) => new Map(c).set(id, { children: null, loading: true }));
    fetchDeletedItemChildren(id)
      .then((children) =>
        setChildrenCache((c) => new Map(c).set(id, { children, loading: false })))
      .catch((err) =>
        setChildrenCache((c) => new Map(c).set(id, {
          children: null,
          loading: false,
          error: err instanceof Error ? err.message : 'Failed to load child assets.',
        })))
      .finally(() => inFlightRef.current.delete(id));
  }, []);

  return (
    <Box>
      <Card size="1" data-testid="trash-table">
        <Inset clip="padding-box" side="bottom">
          <Box style={{ overflowX: 'auto' }}>
      <Table.Root size="2">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell style={{ width: 52 }} pl="3">
              <Checkbox
                checked={allPageSelected ? true : somePageSelected ? 'indeterminate' : false}
                onCheckedChange={(checked) => handleSelectAll(checked === true)}
                aria-label="Select all items on this page"
                data-testid="select-all-checkbox"
              />
            </Table.ColumnHeaderCell>
            <SortableTableHeader
              sortKey="name"
              currentSortKey={currentSortKey}
              currentSortDirection={currentSortDirection ?? null}
              onSort={handleSortChange}
            >
              Name
            </SortableTableHeader>
            <SortableTableHeader
              sortKey="entityType"
              currentSortKey={currentSortKey}
              currentSortDirection={currentSortDirection ?? null}
              onSort={handleSortChange}
            >
              Type
            </SortableTableHeader>
            <SortableTableHeader
              sortKey="repository"
              currentSortKey={currentSortKey}
              currentSortDirection={currentSortDirection ?? null}
              onSort={handleSortChange}
            >
              Repository
            </SortableTableHeader>
            <SortableTableHeader
              sortKey="format"
              currentSortKey={currentSortKey}
              currentSortDirection={currentSortDirection ?? null}
              onSort={handleSortChange}
            >
              Format
            </SortableTableHeader>
            <SortableTableHeader
              sortKey="deletedOn"
              currentSortKey={currentSortKey}
              currentSortDirection={currentSortDirection ?? null}
              onSort={handleSortChange}
            >
              Deleted On
            </SortableTableHeader>
            <SortableTableHeader
              sortKey="deletedBy"
              currentSortKey={currentSortKey}
              currentSortDirection={currentSortDirection ?? null}
              onSort={handleSortChange}
            >
              Deleted By
            </SortableTableHeader>
            <Table.ColumnHeaderCell justify="end" aria-label="Row actions" pr="3" />
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {items.map((item) => {
            const isSelected = selectedIds.has(item.id);
            const isExpandable = item.entityType === 'component';
            const isExpanded = expandedIds.has(item.id);
            const childState = childrenCache.get(item.id);
            return (
              <React.Fragment key={item.id}>
              <Table.Row
                style={{ background: isSelected ? 'var(--blue-a2)' : undefined }}
              >
                {/* Checkbox */}
                <Table.Cell pl="3">
                  <Checkbox
                    checked={isSelected}
                    onCheckedChange={(checked) => handleRowSelect(item.id, checked === true)}
                    disabled={!isSelected && selectedIds.size >= MAX_SELECTION}
                    aria-label={`Select ${item.name}`}
                  />
                </Table.Cell>

                {/* Name — a leading chevron expands components to reveal their soft-deleted child assets.
                    Cap width so a long path/coordinates truncates with ellipsis instead of stretching the
                    column and shoving other columns right. */}
                <Table.Cell style={{ maxWidth: 420 }}>
                  <Flex align="center" gap="1" style={{ minWidth: 0 }}>
                    {isExpandable ? (
                      <IconButton
                        variant="ghost"
                        color="gray"
                        size="1"
                        onClick={() => toggleExpand(item)}
                        aria-label={isExpanded ? `Collapse ${item.name}` : `Expand ${item.name}`}
                        aria-expanded={isExpanded}
                        data-testid={`expand-toggle-${item.id}`}
                        style={{ flexShrink: 0 }}
                      >
                        {isExpanded
                          ? <ChevronDown size={16} aria-hidden="true" />
                          : <ChevronRight size={16} aria-hidden="true" />}
                      </IconButton>
                    ) : (
                      <Box style={{ width: 24, flexShrink: 0 }} />
                    )}
                    <Flex direction="column" gap="1" style={{ minWidth: 0 }}>
                      <Text size="2" weight="medium" truncate title={item.name}>
                        {item.entityType === 'asset' && item.path ? extractFilename(item.name) : item.name}
                      </Text>
                      {item.componentCoordinates && item.componentCoordinates !== item.name && (
                        <Text size="1" color="gray" truncate title={item.componentCoordinates}>
                          {item.componentCoordinates}
                        </Text>
                      )}
                      {item.path && (
                        <Text size="1" color="gray" truncate title={item.path} style={{ fontFamily: 'monospace' }}>
                          {item.path}
                        </Text>
                      )}
                    </Flex>
                  </Flex>
                </Table.Cell>

                {/* Type */}
                <Table.Cell>
                  <Text size="2">{DELETED_ITEM_TYPE_LABELS[item.entityType]}</Text>
                </Table.Cell>

                {/* Repository */}
                <Table.Cell>
                  <Text size="2" color={item.repository ? undefined : 'gray'}>
                    {item.repository ?? '—'}
                  </Text>
                </Table.Cell>

                {/* Format */}
                <Table.Cell>
                  <Text size="2" color={item.format ? undefined : 'gray'}>
                    {item.format ?? '—'}
                  </Text>
                </Table.Cell>

                {/* Deleted On + countdown */}
                <Table.Cell>
                  <Flex direction="column" gap="1">
                    <Text size="2">{formatDate(item.deletedOn)}</Text>
                    <RetentionCountdown retentionExpiresOn={item.retentionExpiresOn} />
                  </Flex>
                </Table.Cell>

                {/* Deleted By: show the friendly name when resolved, with the raw principal on hover. */}
                <Table.Cell>
                  <Text size="2" title={item.deletedByName ? item.deletedBy : undefined}>
                    {item.deletedByName || item.deletedBy}
                  </Text>
                </Table.Cell>

                {/* Row actions */}
                <Table.Cell justify="end" pr="3">
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger>
                      <IconButton
                        variant="ghost"
                        color="gray"
                        size="1"
                        onClick={(e) => e.stopPropagation()}
                        aria-label={`Actions for ${item.name}`}
                        data-testid={`row-actions-${item.id}`}
                      >
                        <MoreHorizontal size={16} aria-hidden="true" />
                      </IconButton>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Content align="end" onClick={(e) => e.stopPropagation()}>
                      <DropdownMenu.Item
                        onClick={() => onRestoreClick(item)}
                        data-testid={`restore-button-${item.id}`}
                      >
                        <RotateCcw size={14} aria-hidden="true" />
                        Restore
                      </DropdownMenu.Item>
                      <DropdownMenu.Separator />
                      <DropdownMenu.Item
                        color="red"
                        onClick={() => onPurgeClick(item)}
                        data-testid={`purge-button-${item.id}`}
                      >
                        <Trash2 size={14} aria-hidden="true" />
                        Purge
                      </DropdownMenu.Item>
                    </DropdownMenu.Content>
                  </DropdownMenu.Root>
                </Table.Cell>
              </Table.Row>
              {isExpandable && isExpanded && (
                <ChildAssetRows childState={childState} />
              )}
              </React.Fragment>
            );
          })}
        </Table.Body>
      </Table.Root>
          </Box>
        </Inset>
      </Card>

      {total > 0 && (
        <TablePagination
          currentPage={currentPage}
          totalPages={totalPages}
          itemsPerPage={limit}
          totalItems={total}
          onPageChange={handlePageChange}
          onItemsPerPageChange={onPageSizeChange}
          mt="4"
        />
      )}
    </Box>
  );
}

export default TrashTable;

