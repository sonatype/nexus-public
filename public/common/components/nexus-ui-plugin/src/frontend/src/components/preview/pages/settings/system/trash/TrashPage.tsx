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

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Flex, Grid, IconButton, ScrollArea, Skeleton, Table, TextField } from '@radix-ui/themes';
import { FilterX, Search, Trash2, X } from 'lucide-react';

import {
  EmptyState,
  ErrorState,
  PageHeader,
  useToast,
} from '../../../../shared';

import { TrashFilterSidebar } from './TrashFilterSidebar';

import { TrashBulkActionBar } from './TrashBulkActionBar';
import { TrashConfirmDialog } from './TrashConfirmDialog';
import { TrashTable } from './TrashTable';
import {
  BulkActionResponseXO,
  makeUniqueId,
  useDeletedItems,
  useDeletedItemsFacets,
  usePurgeItems,
  useRestoreItems,
} from './useTrashApi';
import {
  BulkAction,
  DEFAULT_FILTER_STATE,
  DeletedItem,
  DeletedItemsFilterState,
} from './types';

// =============================================================================
// MAIN PAGE COMPONENT
// =============================================================================

const DEFAULT_LIMIT = 20;
const DEFAULT_SORT = 'deletedOn:desc';
const SEARCH_DEBOUNCE_MS = 300;
const SEARCH_MIN_LENGTH = 2;

/**
 * TrashPage
 *
 * Trash page (registered as the {@code preview.browse.trash} route) that lets repository admins view,
 * restore, and purge soft-deleted entities (NEXUS-51733).
 *
 * URL: #preview/browse/trash
 */
export function TrashPage() {
  // Server-side pagination & sorting state
  const [offset, setOffset] = useState(0);
  const [limit, setLimit] = useState(DEFAULT_LIMIT);
  const [sort, setSort] = useState(DEFAULT_SORT);

  // Filter state — drives server-side entityType param
  const [filterState, setFilterState] = useState<DeletedItemsFilterState>(DEFAULT_FILTER_STATE);

  // Search state — `searchQuery` is what the user types; `debouncedQuery`
  // is what we actually send to the server, after 300ms of inactivity.
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }
    debounceRef.current = setTimeout(() => {
      // Only reset pagination/selection when the effective server query actually changes. Typing below
      // SEARCH_MIN_LENGTH sends no query to the server, so it must not clear the user's page or selection
      // (NEXUS-51733). Compare the pre/post effective queries (both normalized to '' when under threshold).
      const prevEffective =
        debouncedQuery.trim().length >= SEARCH_MIN_LENGTH ? debouncedQuery.trim() : '';
      const nextEffective =
        searchQuery.trim().length >= SEARCH_MIN_LENGTH ? searchQuery.trim() : '';
      setDebouncedQuery(searchQuery);
      if (prevEffective !== nextEffective) {
        setOffset(0);
        setSelectedIds(new Set());
        setSelectedItemsMap(new Map());
      }
    }, SEARCH_DEBOUNCE_MS);
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [searchQuery]);

  // Only send `q` to the server once the user has typed at least 2 chars.
  const effectiveQuery =
    debouncedQuery.trim().length >= SEARCH_MIN_LENGTH ? debouncedQuery.trim() : undefined;

  const { items, total, loading, error, refetch } = useDeletedItems({
    offset,
    limit,
    sort,
    entityTypes: filterState.entityTypes.length > 0 ? filterState.entityTypes : undefined,
    formats: filterState.formats.length > 0 ? filterState.formats : undefined,
    deletedBy: filterState.deletedBy.length > 0 ? filterState.deletedBy : undefined,
    q: effectiveQuery,
  });

  // Clamp the offset back into range when the result set shrinks below the current page (e.g. after a
  // restore/purge removes the last item on a tail page). Otherwise the server returns an empty page and,
  // with no active filters, the UI renders a false "Trash is empty" while items remain on earlier pages
  // (NEXUS-51733). Stepping to the last non-empty page triggers a refetch at the clamped offset.
  useEffect(() => {
    if (offset > 0 && offset >= total) {
      setOffset(Math.max(0, Math.floor((total - 1) / limit) * limit));
    }
  }, [total, offset, limit]);

  const [facetsRefetchKey, setFacetsRefetchKey] = useState(0);
  const { facets, loading: facetsLoading } = useDeletedItemsFacets(facetsRefetchKey);

  const { restore, loading: restoreLoading } = useRestoreItems();
  const { purge, loading: purgeLoading } = usePurgeItems();
  const { success: toastSuccess, error: toastError, warning: toastWarning } = useToast();

  // Row selection. Persists across page navigation and page-size changes; cleared on
  // filter/search/sort changes (because the result universe is changing) and on bulk-action
  // success (only the items that succeeded leave the set). Capped at 250 by the underlying
  // bulk-action endpoint and the UI checkbox guard in TrashTable.
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // Cache of DeletedItem objects keyed by id, accumulated as pages load. Lets bulk actions
  // resolve full item records for selections made on previous pages without re-fetching.
  // Bounded by MAX_SELECTION (250) in practice — only items the user can select are kept
  // here long-term; non-selected items get garbage-collected on the next clearSelection().
  const [selectedItemsMap, setSelectedItemsMap] = useState<Map<string, DeletedItem>>(
    () => new Map()
  );

  // Confirmation dialog state
  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean;
    action: BulkAction;
    items: DeletedItem[];
  }>({ open: false, action: 'restore', items: [] });

  // Whenever a new page of items arrives, merge any currently-selected items into the cache
  // so they remain resolvable after the user paginates. Items not in selectedIds aren't
  // cached — selection drives retention.
  useEffect(() => {
    if (!items || items.length === 0) return;
    setSelectedItemsMap((prev) => {
      let changed = false;
      const next = new Map(prev);
      for (const item of items) {
        if (selectedIds.has(item.id) && !next.has(item.id)) {
          next.set(item.id, item);
          changed = true;
        }
      }
      return changed ? next : prev;
    });
  }, [items, selectedIds]);

  // Selected items resolved from the cross-page cache, not just the current page. This is
  // what the bulk-action handlers consume.
  const selectedItems = useMemo(
    () => Array.from(selectedIds)
      .map((id) => selectedItemsMap.get(id))
      .filter((item): item is DeletedItem => item !== undefined),
    [selectedIds, selectedItemsMap]
  );

  // Clear both the id set and the item cache. Used on filter/search/sort changes and from
  // the bulk-action bar's "Clear selection" affordance.
  const clearSelection = useCallback(() => {
    setSelectedIds(new Set());
    setSelectedItemsMap(new Map());
  }, []);

  // Wrap setSelectedIds for TrashTable so newly-checked items immediately get cached.
  // Without this, an item ticked on page 1 wouldn't appear in selectedItemsMap until the
  // next render of TrashPage's items-merge effect — fine in practice, but doing it eagerly
  // here keeps the contract simpler.
  const handleSelectionChange = useCallback((nextIds: Set<string>) => {
    setSelectedIds(nextIds);
    setSelectedItemsMap((prev) => {
      const next = new Map(prev);
      // Add any newly-selected items that are visible on the current page.
      for (const item of items || []) {
        if (nextIds.has(item.id) && !next.has(item.id)) {
          next.set(item.id, item);
        }
      }
      // Drop entries for ids no longer in the selection.
      for (const id of next.keys()) {
        if (!nextIds.has(id)) next.delete(id);
      }
      return next;
    });
  }, [items]);

  // =============================================================================
  // HANDLERS
  // =============================================================================

  const handleFilterChange = useCallback((sectionId: keyof DeletedItemsFilterState, values: string[]) => {
    setFilterState((prev) => ({ ...prev, [sectionId]: values }));
    setOffset(0);
    clearSelection();
  }, [clearSelection]);

  const handleFilterClear = useCallback(() => {
    setFilterState(DEFAULT_FILTER_STATE);
    setSearchQuery('');
    setDebouncedQuery('');
    setOffset(0);
    clearSelection();
  }, [clearSelection]);

  const handleSort = useCallback((newSort: string) => {
    setSort(newSort);
    setOffset(0);
    clearSelection();
  }, [clearSelection]);

  // Page change preserves selection so the user can multi-select across pages up to
  // MAX_SELECTION (250). The bulk action limit on the server is also 250.
  const handlePageChange = useCallback((newOffset: number) => {
    setOffset(newOffset);
  }, []);

  // Page-size change also preserves selection — the same items remain in the result set,
  // just laid out differently.
  const handlePageSizeChange = useCallback((newPageSize: number) => {
    setLimit(newPageSize);
    setOffset(0);
  }, []);

  // Row-level restore (single item)
  const handleRowRestore = useCallback((item: DeletedItem) => {
    setConfirmDialog({ open: true, action: 'restore', items: [item] });
  }, []);

  // Row-level purge (single item)
  const handleRowPurge = useCallback((item: DeletedItem) => {
    setConfirmDialog({ open: true, action: 'purge', items: [item] });
  }, []);

  // Bulk restore from bar
  const handleBulkRestore = useCallback(() => {
    if (selectedItems.length === 0) return;
    setConfirmDialog({ open: true, action: 'restore', items: selectedItems });
  }, [selectedItems]);

  // Bulk purge from bar
  const handleBulkPurge = useCallback(() => {
    if (selectedItems.length === 0) return;
    setConfirmDialog({ open: true, action: 'purge', items: selectedItems });
  }, [selectedItems]);

  const handleConfirm = useCallback(async () => {
    const { action, items } = confirmDialog;
    const isRestore = action === 'restore';
    const pastTense = isRestore ? 'restored' : 'permanently deleted';

    try {
      const response: BulkActionResponseXO = isRestore
        ? await restore(items)
        : await purge(items);

      // A partial success is not a total failure. Clear ONLY the items the server
      // reported as successful, so the failed ones stay selected for retry. Succeeded
      // entries are correlated back to selection ids via the same entityType::identifier
      // scheme used to build DeletedItem.id.
      const succeededIds = new Set(
        response.successful.map((r) => makeUniqueId(r.entityType, r.identifier))
      );
      if (succeededIds.size > 0) {
        setSelectedIds((prev) => new Set([...prev].filter((id) => !succeededIds.has(id))));
        setSelectedItemsMap((prev) => {
          const next = new Map(prev);
          for (const id of succeededIds) next.delete(id);
          return next;
        });
      }

      const okCount = response.successful.length;
      const failCount = response.failed.length;
      const detail = response.failed.find((f) => f.error)?.error;

      if (failCount === 0) {
        toastSuccess(`${okCount} item${okCount !== 1 ? 's' : ''} ${pastTense} successfully.`);
      } else if (okCount === 0) {
        toastError(
          `Failed to ${isRestore ? 'restore' : 'permanently delete'} items`,
          detail || 'Please try again.'
        );
      } else {
        toastWarning(
          `${okCount} ${pastTense}, ${failCount} failed`,
          detail || `${failCount} item${failCount !== 1 ? 's' : ''} could not be processed.`
        );
      }
    } catch (err: unknown) {
      // Transport / HTTP-level failure — nothing succeeded, keep the whole selection.
      const detail = err instanceof Error ? err.message : '';
      const verb = isRestore ? 'restore' : 'permanently delete';
      toastError(`Failed to ${verb} items`, detail || 'Please try again.');
    } finally {
      setConfirmDialog((d) => ({ ...d, open: false }));
      refetch();
      setFacetsRefetchKey((k) => k + 1);
    }
  }, [confirmDialog, restore, purge, toastSuccess, toastError, toastWarning, refetch]);

  const handleCancel = useCallback(() => {
    setConfirmDialog((d) => ({ ...d, open: false }));
  }, []);

  // =============================================================================
  // RENDER
  // =============================================================================

  const isActionLoading = restoreLoading || purgeLoading;

  // Show the full-page skeleton only on the first load. Subsequent refetches
  // (search debounce, sort, filter, pagination) keep the existing table visible
  // so the search input doesn't flicker between keystrokes. Tracked with a ref
  // so flipping the flag doesn't trigger an extra render.
  const hasLoadedOnceRef = useRef(false);
  if (!loading && !error) {
    hasLoadedOnceRef.current = true;
  }
  const isInitialLoading = loading && !hasLoadedOnceRef.current;

  return (
    <>
      <ScrollArea style={{ height: '100%' }}>
      <Box
        px={{ initial: '4', md: '6', lg: '6' }}
        py={{ initial: '4', md: '5', lg: '6' }}
        pb="8"
      >
        {isInitialLoading && (
          <Grid
            columns={{ initial: '1', sm: '220px 1fr' }}
            gap="6"
            width="100%"
            mt="4"
            style={{ minWidth: 0 }}
            data-testid="trash-skeleton"
            aria-busy="true"
            aria-label="Loading deleted items"
          >
            {/* Sidebar skeleton */}
            <Box display={{ initial: 'none', sm: 'block' }} pt="4">
              <Skeleton style={{ display: 'block', height: 32, width: 120, marginBottom: 16 }} />
              <Flex direction="column" gap="3">
                <Skeleton style={{ display: 'block', height: 16, width: 80 }} />
                <Skeleton style={{ display: 'block', height: 16, width: '90%' }} />
                <Skeleton style={{ display: 'block', height: 16, width: '75%' }} />
                <Skeleton style={{ display: 'block', height: 16, width: '85%' }} />
                <Box mt="2">
                  <Skeleton style={{ display: 'block', height: 16, width: 60, marginBottom: 12 }} />
                  <Skeleton style={{ display: 'block', height: 16, width: '80%' }} />
                  <Skeleton style={{ display: 'block', height: 16, width: '60%', marginTop: 8 }} />
                </Box>
              </Flex>
            </Box>

            {/* Table skeleton */}
            <Box style={{ minWidth: 0 }}>
              <Box mb="4">
                <Skeleton style={{ display: 'block', height: 28, width: 160, marginBottom: 8 }} />
                <Skeleton style={{ display: 'block', height: 16, width: 60 }} />
              </Box>
              <Table.Root variant="surface" size="1">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell style={{ width: 52 }} />
                    <Table.ColumnHeaderCell>Name</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Type</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Format</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Deleted On</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Deleted By</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell />
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {[0, 1, 2, 3, 4, 5, 6].map((i) => (
                    <Table.Row key={i}>
                      <Table.Cell><Skeleton style={{ display: 'block', height: 16, width: 16 }} /></Table.Cell>
                      <Table.Cell><Skeleton style={{ display: 'block', height: 16, width: '80%' }} /></Table.Cell>
                      <Table.Cell><Skeleton style={{ display: 'block', height: 16, width: 60 }} /></Table.Cell>
                      <Table.Cell><Skeleton style={{ display: 'block', height: 16, width: 100 }} /></Table.Cell>
                      <Table.Cell><Skeleton style={{ display: 'block', height: 16, width: 50 }} /></Table.Cell>
                      <Table.Cell><Skeleton style={{ display: 'block', height: 16, width: 90 }} /></Table.Cell>
                      <Table.Cell><Skeleton style={{ display: 'block', height: 16, width: 70 }} /></Table.Cell>
                      <Table.Cell><Skeleton style={{ display: 'block', height: 16, width: 24 }} /></Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            </Box>
          </Grid>
        )}

        {!isInitialLoading && error && (
          <Box mt="6">
            <ErrorState message={error} onRetry={refetch} />
          </Box>
        )}

        {!isInitialLoading && !error && (
          <Flex direction="column" gap="6" width="100%" style={{ minWidth: 0 }}>
            <Grid
              columns={{ initial: '1', sm: '220px 1fr' }}
              gap="6"
              width="100%"
              style={{ minWidth: 0 }}
            >
              {/* Filter sidebar — hidden on mobile */}
              <Box
                display={{ initial: 'none', sm: 'block' }}
                role="complementary"
                aria-label="Filters"
              >
                <TrashFilterSidebar
                  facets={facets}
                  facetsLoading={facetsLoading}
                  filterState={filterState}
                  onFilterChange={handleFilterChange}
                  onReset={handleFilterClear}
                  disabled={isActionLoading}
                />
              </Box>

              {/* Main content */}
              <Box style={{ minWidth: 0 }}>
                <Box mb="4">
                  <PageHeader
                    title="Trash"
                    description="View and manage soft-deleted repositories, components, and assets"
                  />
                </Box>
                <Box mb="4">
                  <TextField.Root
                    placeholder="Filter by name, path, or repository…"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    data-testid="search-input"
                    aria-label="Filter deleted items by name, path, or repository"
                  >
                    <TextField.Slot>
                      <Search size={16} aria-hidden="true" />
                    </TextField.Slot>
                    {searchQuery && (
                      <TextField.Slot>
                        <IconButton
                          variant="ghost"
                          size="1"
                          onClick={() => setSearchQuery('')}
                          aria-label="Clear filter"
                          data-testid="search-clear"
                        >
                          <X size={14} aria-hidden="true" />
                        </IconButton>
                      </TextField.Slot>
                    )}
                  </TextField.Root>
                </Box>
                {items.length === 0 ? (
                  (effectiveQuery !== undefined ||
                    Object.values(filterState).some((v) => Array.isArray(v) ? v.length > 0 : !!v)) ? (
                    <EmptyState
                      icon={FilterX}
                      title="No items match your filters"
                      description="Try adjusting or removing filters to see more results."
                      action={{
                        label: 'Clear filters',
                        onClick: handleFilterClear,
                        icon: FilterX,
                      }}
                    />
                  ) : (
                    <EmptyState
                      icon={Trash2}
                      title="Trash is empty"
                      description="Items you delete will appear here and can be restored or permanently removed."
                      tip="Trashed items are retained based on your configured retention policy."
                    />
                  )
                ) : (
                  <TrashTable
                    items={items}
                    total={total}
                    offset={offset}
                    limit={limit}
                    sort={sort}
                    selectedIds={selectedIds}
                    onSelectionChange={handleSelectionChange}
                    onRestoreClick={handleRowRestore}
                    onPurgeClick={handleRowPurge}
                    onPageChange={handlePageChange}
                    onPageSizeChange={handlePageSizeChange}
                    onSort={handleSort}
                  />
                )}
              </Box>
            </Grid>
          </Flex>
        )}
      </Box>
      </ScrollArea>

      {/* Rendered OUTSIDE ScrollArea: TrashBulkActionBar is position:fixed and must not sit inside a scroll
          container whose ancestor could establish a containing block (transform/will-change) and re-anchor
          it away from the viewport. TrashConfirmDialog portals to body but is kept alongside for clarity. */}
      <TrashBulkActionBar
        selectedItems={selectedItems}
        restoreLoading={restoreLoading}
        purgeLoading={purgeLoading}
        onRestoreClick={handleBulkRestore}
        onPurgeClick={handleBulkPurge}
        onClearSelection={clearSelection}
      />

      <TrashConfirmDialog
        open={confirmDialog.open}
        action={confirmDialog.action}
        items={confirmDialog.items}
        loading={isActionLoading}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    </>
  );
}

export default TrashPage;

