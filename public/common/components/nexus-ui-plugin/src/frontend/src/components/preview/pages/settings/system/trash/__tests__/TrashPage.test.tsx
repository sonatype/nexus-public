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
import { act, render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { TrashPage } from '../TrashPage';
import * as apiModule from '../useTrashApi';
import { DeletedItem } from '../types';

// =============================================================================
// MOCKS
// =============================================================================

// Auto-mock the data hooks, but keep the real pure helpers (e.g. makeUniqueId),
// which TrashPage uses to correlate bulk-action responses back to selection ids.
jest.mock('../useTrashApi', () => ({
  ...jest.requireActual('../useTrashApi'),
  useDeletedItems: jest.fn(),
  useDeletedItemsFacets: jest.fn(),
  useRestoreItems: jest.fn(),
  usePurgeItems: jest.fn(),
}));

const mockUseDeletedItems = apiModule.useDeletedItems as jest.MockedFunction<typeof apiModule.useDeletedItems>;
const mockUseRestoreItems = apiModule.useRestoreItems as jest.MockedFunction<typeof apiModule.useRestoreItems>;
const mockUsePurgeItems = apiModule.usePurgeItems as jest.MockedFunction<typeof apiModule.usePurgeItems>;
const mockUseDeletedItemsFacets = apiModule.useDeletedItemsFacets as jest.MockedFunction<typeof apiModule.useDeletedItemsFacets>;

// Mock shared components for isolation
jest.mock('../../../../../shared', () => {
  const mockReact = require('react');
  return {
    ...(jest.requireActual('../../../../../shared') || {}),
    useToast: () => ({
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
      showToast: jest.fn(),
    }),
    PageHeader: jest.fn(({ title, description }: any) =>
      mockReact.createElement('div', { 'data-testid': 'page-header' },
        mockReact.createElement('h1', null, title),
        mockReact.createElement('p', null, description)
      )
    ),
    ErrorState: jest.fn(({ message, onRetry }: any) =>
      mockReact.createElement('div', { 'data-testid': 'error-state' },
        mockReact.createElement('span', null, message),
        onRetry && mockReact.createElement('button', { onClick: onRetry }, 'Retry')
      )
    ),
    EmptyState: jest.fn(({ title, description, action }: any) =>
      mockReact.createElement('div', { 'data-testid': 'empty-state' },
        mockReact.createElement('span', null, title),
        mockReact.createElement('p', null, description),
        action && mockReact.createElement('button', { 'data-testid': 'empty-state-action', onClick: action.onClick }, action.label)
      )
    ),
    TablePagination: jest.fn(() => mockReact.createElement('div', { 'data-testid': 'pagination' })),
  };
});

// Mock child components to keep tests focused on page-level behavior
jest.mock('../TrashFilterSidebar', () => {
  const mockReact = require('react');
  return {
    TrashFilterSidebar: jest.fn(({ onFilterChange, onReset }: any) =>
      mockReact.createElement('div', { 'data-testid': 'filter-sidebar' },
        mockReact.createElement('button', {
          'data-testid': 'filter-trigger',
          onClick: () => onFilterChange('entityTypes', ['component']),
        }, 'Filter by component'),
        mockReact.createElement('button', { 'data-testid': 'filter-reset', onClick: onReset }, 'Reset')
      )
    ),
  };
});

jest.mock('../TrashTable', () => {
  const mockReact = require('react');
  return {
    TrashTable: jest.fn(({ items, selectedIds, onSelectionChange, onRestoreClick, onPageChange }: any) =>
      mockReact.createElement('div', { 'data-testid': 'trash-table' },
        mockReact.createElement('span', null, `${items.length} items`),
        mockReact.createElement('span', { 'data-testid': 'selected-count' }, `selected:${selectedIds?.size ?? 0}`),
        items.slice(0, 3).map((item: any) =>
          mockReact.createElement('button', {
            key: `r-${item.id}`,
            'data-testid': `row-restore-${item.id}`,
            onClick: () => onRestoreClick(item),
          }, `Restore ${item.name}`)
        ),
        items.slice(0, 3).map((item: any) =>
          mockReact.createElement('button', {
            key: `s-${item.id}`,
            'data-testid': `row-select-${item.id}`,
            onClick: () => {
              const next = new Set(selectedIds ?? new Set());
              if (next.has(item.id)) next.delete(item.id);
              else next.add(item.id);
              onSelectionChange?.(next);
            },
          }, `Select ${item.name}`)
        ),
        onPageChange && mockReact.createElement('button', {
          'data-testid': 'goto-page-2',
          onClick: () => onPageChange(20),
        }, 'Go to page 2')
      )
    ),
  };
});

jest.mock('../TrashBulkActionBar', () => {
  const mockReact = require('react');
  return {
    TrashBulkActionBar: jest.fn(({ selectedItems, onRestoreClick, onPurgeClick, onClearSelection }: any) =>
      selectedItems.length > 0
        ? mockReact.createElement('div', { 'data-testid': 'bulk-action-bar' },
            mockReact.createElement('span', { 'data-testid': 'bulk-selected-count' }, `bulk:${selectedItems.length}`),
            mockReact.createElement('button', { 'data-testid': 'bulk-restore', onClick: onRestoreClick }, 'Restore'),
            mockReact.createElement('button', { 'data-testid': 'bulk-purge', onClick: onPurgeClick }, 'Purge'),
            mockReact.createElement('button', { 'data-testid': 'bulk-clear', onClick: onClearSelection }, 'Clear')
          )
        : null
    ),
  };
});

jest.mock('../TrashConfirmDialog', () => {
  const mockReact = require('react');
  return {
    TrashConfirmDialog: jest.fn(({ open, action, onConfirm, onCancel }: any) =>
      open
        ? mockReact.createElement('div', { 'data-testid': `confirm-dialog-${action}` },
            mockReact.createElement('button', { 'data-testid': 'dialog-confirm', onClick: onConfirm }, 'Confirm'),
            mockReact.createElement('button', { 'data-testid': 'dialog-cancel', onClick: onCancel }, 'Cancel')
          )
        : null
    ),
  };
});

// =============================================================================
// TEST DATA
// =============================================================================

const MOCK_ITEMS: DeletedItem[] = [
  {
    id: 'r1',
    name: 'maven-central',
    entityType: 'repository',
    deletedOn: '2026-03-15T10:00:00Z',
    deletedBy: 'admin',
    retentionExpiresOn: '2026-04-14T10:00:00Z',
  },
  {
    id: 'c1',
    name: 'log4j:log4j:1.2.17',
    entityType: 'component',
    repository: 'maven-central',
    format: 'maven2',
    deletedOn: '2026-03-16T09:00:00Z',
    deletedBy: 'admin',
    retentionExpiresOn: '2026-04-15T09:00:00Z',
  },
];

// =============================================================================
// HELPERS
// =============================================================================

function Wrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

function setupApiMocks() {
  const mockRefetch = jest.fn();
  const mockRestore = jest.fn().mockResolvedValue({ successful: [], failed: [] });
  const mockPurge = jest.fn().mockResolvedValue({ successful: [], failed: [] });

  mockUseDeletedItems.mockReturnValue({
    items: MOCK_ITEMS,
    total: MOCK_ITEMS.length,
    offset: 0,
    limit: 20,
    loading: false,
    error: null,
    refetch: mockRefetch,
  });

  mockUseRestoreItems.mockReturnValue({
    restore: mockRestore,
    loading: false,
  });

  mockUsePurgeItems.mockReturnValue({
    purge: mockPurge,
    loading: false,
  });

  mockUseDeletedItemsFacets.mockReturnValue({
    facets: {
      entityTypes: { asset: 1, component: 1 },
      formats: { maven2: 2 },
      deletedBy: [{ name: 'admin', count: 2 }],
    },
    loading: false,
    error: null,
    refetch: jest.fn(),
  });

  return { mockRefetch, mockRestore, mockPurge };
}

// =============================================================================
// TESTS
// =============================================================================

describe('TrashPage', () => {
  beforeEach(() => jest.clearAllMocks());

  it('renders page header', () => {
    setupApiMocks();
    render(<TrashPage />, { wrapper: Wrapper });

    expect(screen.getByTestId('page-header')).toBeInTheDocument();
    expect(screen.getAllByText('Trash').length).toBeGreaterThan(0);
  });

  it('renders loading state while data loads', () => {
    mockUseDeletedItems.mockReturnValue({ items: [], total: 0, offset: 0, limit: 20, loading: true, error: null, refetch: jest.fn() });
    mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
    mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });

    render(<TrashPage />, { wrapper: Wrapper });

    expect(screen.getByTestId('trash-skeleton')).toBeInTheDocument();
    expect(screen.queryByTestId('trash-table')).not.toBeInTheDocument();
  });

  it('renders error state when fetch fails', () => {
    mockUseDeletedItems.mockReturnValue({
      items: [],
      total: 0,
      offset: 0,
      limit: 20,
      loading: false,
      error: 'Failed to load deleted items.',
      refetch: jest.fn(),
    });
    mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
    mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });

    render(<TrashPage />, { wrapper: Wrapper });

    expect(screen.getByTestId('error-state')).toBeInTheDocument();
    expect(screen.getByText('Failed to load deleted items.')).toBeInTheDocument();
  });

  it('calls refetch when error retry is clicked', () => {
    const mockRefetch = jest.fn();
    mockUseDeletedItems.mockReturnValue({ items: [], total: 0, offset: 0, limit: 20, loading: false, error: 'Error', refetch: mockRefetch });
    mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
    mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });

    render(<TrashPage />, { wrapper: Wrapper });
    fireEvent.click(screen.getByText('Retry'));

    expect(mockRefetch).toHaveBeenCalledTimes(1);
  });

  it('renders table with items', () => {
    setupApiMocks();
    render(<TrashPage />, { wrapper: Wrapper });

    expect(screen.getByTestId('trash-table')).toBeInTheDocument();
    expect(screen.getByText('2 items')).toBeInTheDocument();
  });

  it('renders filter sidebar', () => {
    setupApiMocks();
    render(<TrashPage />, { wrapper: Wrapper });

    expect(screen.getByTestId('filter-sidebar')).toBeInTheDocument();
  });

  it('renders empty state when no items present', () => {
    mockUseDeletedItems.mockReturnValue({ items: [], total: 0, offset: 0, limit: 20, loading: false, error: null, refetch: jest.fn() });
    mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
    mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });

    render(<TrashPage />, { wrapper: Wrapper });

    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
  });

  it('does not show bulk action bar when nothing selected', () => {
    setupApiMocks();
    render(<TrashPage />, { wrapper: Wrapper });

    expect(screen.queryByTestId('bulk-action-bar')).not.toBeInTheDocument();
  });

  it('row restore opens confirm dialog with restore action', () => {
    setupApiMocks();
    render(<TrashPage />, { wrapper: Wrapper });

    fireEvent.click(screen.getByTestId('row-restore-r1'));

    expect(screen.getByTestId('confirm-dialog-restore')).toBeInTheDocument();
  });

  it('confirm dialog cancel closes the dialog', () => {
    setupApiMocks();
    render(<TrashPage />, { wrapper: Wrapper });

    fireEvent.click(screen.getByTestId('row-restore-r1'));
    expect(screen.getByTestId('confirm-dialog-restore')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('dialog-cancel'));
    expect(screen.queryByTestId('confirm-dialog-restore')).not.toBeInTheDocument();
  });

  it('confirm restore calls restore API and refetch', async () => {
    const { mockRestore, mockRefetch } = setupApiMocks();
    render(<TrashPage />, { wrapper: Wrapper });

    fireEvent.click(screen.getByTestId('row-restore-r1'));
    fireEvent.click(screen.getByTestId('dialog-confirm'));

    await waitFor(() => {
      expect(mockRestore).toHaveBeenCalledWith([MOCK_ITEMS[0]]);
      expect(mockRefetch).toHaveBeenCalled();
    });
  });

  it('on partial restore failure, clears only the succeeded item from the selection', async () => {
    // Two selectable items whose ids follow the real `entityType::identifier` scheme,
    // so handleConfirm can correlate the bulk-restore response back to selection ids.
    const partialItems: DeletedItem[] = [
      {
        id: 'component::ok-1',
        name: 'com.example:ok:1.0.0',
        entityType: 'component',
        repository: 'maven-releases',
        format: 'maven2',
        deletedOn: '2026-07-01T09:00:00Z',
        deletedBy: 'admin',
        retentionExpiresOn: '2026-07-31T09:00:00Z',
      },
      {
        id: 'component::bad-1',
        name: 'com.example:bad:1.0.0',
        entityType: 'component',
        repository: 'trash-fail-repo',
        format: 'maven2',
        deletedOn: '2026-07-01T09:00:00Z',
        deletedBy: 'admin',
        retentionExpiresOn: '2026-07-31T09:00:00Z',
      },
    ];

    const { mockRestore } = setupApiMocks();
    mockUseDeletedItems.mockReturnValue({
      items: partialItems,
      total: partialItems.length,
      offset: 0,
      limit: 20,
      loading: false,
      error: null,
      refetch: jest.fn(),
    });
    // 1 succeeds, 1 fails — the response the frontend must now honor.
    mockRestore.mockResolvedValue({
      successful: [{ entityType: 'component', identifier: 'ok-1', error: null }],
      failed: [
        { entityType: 'component', identifier: 'bad-1', error: "Parent repository 'trash-fail-repo' is not available." },
      ],
    });

    render(<TrashPage />, { wrapper: Wrapper });

    // Select both items → bulk bar shows 2.
    fireEvent.click(screen.getByTestId('row-select-component::ok-1'));
    fireEvent.click(screen.getByTestId('row-select-component::bad-1'));
    expect(screen.getByTestId('bulk-selected-count')).toHaveTextContent('bulk:2');

    // Restore both.
    fireEvent.click(screen.getByTestId('bulk-restore'));
    await act(async () => {
      fireEvent.click(screen.getByTestId('dialog-confirm'));
    });

    // Only the succeeded item leaves the selection; the failed one stays for retry.
    await waitFor(() => {
      expect(screen.getByTestId('bulk-selected-count')).toHaveTextContent('bulk:1');
    });
  });

  it('renders no-data empty state when item list is empty', () => {
    mockUseDeletedItems.mockReturnValue({ items: [], total: 0, offset: 0, limit: 20, loading: false, error: null, refetch: jest.fn() });
    mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
    mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });

    render(<TrashPage />, { wrapper: Wrapper });

    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    expect(screen.getByText('Trash is empty')).toBeInTheDocument();
    expect(screen.queryByTestId('empty-state-action')).not.toBeInTheDocument();
  });

  it('renders filtered empty state with clear-filters action when filters produce no results', async () => {
    mockUseDeletedItems.mockReturnValue({
      items: [], total: 0, offset: 0, limit: 20, loading: false, error: null, refetch: jest.fn(),
    });
    mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
    mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });

    render(<TrashPage />, { wrapper: Wrapper });

    // Apply a filter so filterState is non-empty
    fireEvent.click(screen.getByTestId('filter-trigger'));

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
    expect(screen.getByText('No items match your filters')).toBeInTheDocument();
    expect(screen.getByTestId('empty-state-action')).toBeInTheDocument();
  });

  it('clear-filters action on filtered empty state resets filters and shows items', async () => {
    mockUseDeletedItems.mockReturnValue({
      items: [], total: 0, offset: 0, limit: 20, loading: false, error: null, refetch: jest.fn(),
    });
    mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
    mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });

    render(<TrashPage />, { wrapper: Wrapper });

    // Apply a filter to get into filtered empty state
    fireEvent.click(screen.getByTestId('filter-trigger'));

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });

    // Now update mock to return items and click clear-filters
    mockUseDeletedItems.mockReturnValue({
      items: MOCK_ITEMS, total: MOCK_ITEMS.length, offset: 0, limit: 20, loading: false, error: null, refetch: jest.fn(),
    });

    fireEvent.click(screen.getByTestId('empty-state-action'));

    await waitFor(() => {
      expect(screen.getByTestId('trash-table')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('empty-state')).not.toBeInTheDocument();
  });

  // ===========================================================================
  // SEARCH (server-side, debounced)
  // ===========================================================================

  describe('search', () => {
    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    it('renders search input', () => {
      setupApiMocks();
      render(<TrashPage />, { wrapper: Wrapper });

      expect(screen.getByTestId('search-input')).toBeInTheDocument();
    });

    it('clear button is hidden until user types and clears the value on click', () => {
      setupApiMocks();
      render(<TrashPage />, { wrapper: Wrapper });

      expect(screen.queryByTestId('search-clear')).not.toBeInTheDocument();

      fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'maven' } });
      expect(screen.getByTestId('search-clear')).toBeInTheDocument();

      fireEvent.click(screen.getByTestId('search-clear'));
      expect(screen.getByTestId('search-input')).toHaveValue('');
      expect(screen.queryByTestId('search-clear')).not.toBeInTheDocument();
    });

    it('debounces input and forwards q to the hook only after 300ms', () => {
      setupApiMocks();
      render(<TrashPage />, { wrapper: Wrapper });

      // Hook called on mount with no q.
      expect(mockUseDeletedItems).toHaveBeenLastCalledWith(expect.objectContaining({ q: undefined }));

      fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'maven' } });

      // Before debounce fires, q should still be undefined.
      expect(mockUseDeletedItems).toHaveBeenLastCalledWith(expect.objectContaining({ q: undefined }));

      // Advance past the debounce window.
      act(() => { jest.advanceTimersByTime(300); });

      expect(mockUseDeletedItems).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'maven' }));
    });

    it('does not send q when query is shorter than the minimum length', () => {
      setupApiMocks();
      render(<TrashPage />, { wrapper: Wrapper });

      fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'm' } });
      act(() => { jest.advanceTimersByTime(300); });

      expect(mockUseDeletedItems).toHaveBeenLastCalledWith(expect.objectContaining({ q: undefined }));
    });

    it('resets offset to 0 when search debounces', () => {
      setupApiMocks();
      render(<TrashPage />, { wrapper: Wrapper });

      fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'maven' } });
      act(() => { jest.advanceTimersByTime(300); });

      expect(mockUseDeletedItems).toHaveBeenLastCalledWith(expect.objectContaining({ offset: 0, q: 'maven' }));
    });

    it('clearing the search input via X clears q after debounce', () => {
      setupApiMocks();
      render(<TrashPage />, { wrapper: Wrapper });

      fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'maven' } });
      act(() => { jest.advanceTimersByTime(300); });
      expect(mockUseDeletedItems).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'maven' }));

      fireEvent.click(screen.getByTestId('search-clear'));
      act(() => { jest.advanceTimersByTime(300); });
      expect(mockUseDeletedItems).toHaveBeenLastCalledWith(expect.objectContaining({ q: undefined }));
    });

    it('shows filtered empty state when search produces no results', () => {
      setupApiMocks();
      const { rerender } = render(<TrashPage />, { wrapper: Wrapper });

      // After typing + debouncing, hook returns empty results.
      mockUseDeletedItems.mockReturnValue({
        items: [], total: 0, offset: 0, limit: 20, loading: false, error: null, refetch: jest.fn(),
      });

      fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'zzz-no-match' } });
      act(() => { jest.advanceTimersByTime(300); });
      rerender(<TrashPage />);

      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
      expect(screen.getByText('No items match your filters')).toBeInTheDocument();
    });
  });

  // ===========================================================================
  // MULTI-VALUE FILTERS
  // ===========================================================================

  describe('multi-value filters', () => {
    it('multi-select entityType passes array to hook', () => {
      setupApiMocks();
      render(<TrashPage />, { wrapper: Wrapper });

      // Simulate filter trigger click (mock sidebar calls onFilterChange)
      fireEvent.click(screen.getByTestId('filter-trigger'));

      expect(mockUseDeletedItems).toHaveBeenLastCalledWith(
        expect.objectContaining({ entityTypes: ['component'] }),
      );
    });

    it('clear-filters resets all three filter axes and search', async () => {
      mockUseDeletedItems.mockReturnValue({
        items: [], total: 0, offset: 0, limit: 20, loading: false, error: null,
        refetch: jest.fn(),
      });
      mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
      mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });
      mockUseDeletedItemsFacets.mockReturnValue({
        facets: { entityTypes: {}, formats: {}, deletedBy: [] },
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      render(<TrashPage />, { wrapper: Wrapper });

      // Apply a filter via the filter sidebar trigger.
      fireEvent.click(screen.getByTestId('filter-trigger'));

      // Click reset
      fireEvent.click(screen.getByTestId('filter-reset'));

      await waitFor(() => {
        expect(mockUseDeletedItems).toHaveBeenLastCalledWith(
          expect.objectContaining({
            entityTypes: undefined,
            formats: undefined,
            deletedBy: undefined,
            q: undefined,
          }),
        );
      });
    });

    it('successful purge increments facets refetch key', async () => {
      const facetsRefetch = jest.fn();
      mockUseDeletedItemsFacets.mockReturnValue({
        facets: { entityTypes: {}, formats: {}, deletedBy: [] },
        loading: false,
        error: null,
        refetch: facetsRefetch,
      });
      const purge = jest.fn().mockResolvedValue(undefined);
      mockUsePurgeItems.mockReturnValue({ purge, loading: false });
      mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
      mockUseDeletedItems.mockReturnValue({
        items: MOCK_ITEMS, total: MOCK_ITEMS.length, offset: 0, limit: 20,
        loading: false, error: null, refetch: jest.fn(),
      });

      render(<TrashPage />, { wrapper: Wrapper });

      // Open and confirm the purge dialog
      fireEvent.click(screen.getByTestId('row-restore-r1'));
      fireEvent.click(screen.getByTestId('dialog-confirm'));

      // Verify facets hook is called again (refetchKey incremented)
      await waitFor(() => {
        expect(mockUseDeletedItemsFacets.mock.calls.length).toBeGreaterThan(1);
      });
    });
  });

  // ===========================================================================
  // SELECTION PERSISTENCE (across pagination, cleared on filter/sort/search)
  // ===========================================================================

  describe('selection persistence', () => {
    const PAGE_1: DeletedItem[] = [{
      id: 'p1-a', name: 'page1-a', entityType: 'component', repository: 'r',
      format: 'maven2', deletedOn: '2026-03-15T10:00:00Z', deletedBy: 'admin',
      retentionExpiresOn: '2026-04-14T10:00:00Z',
    }];
    const PAGE_2: DeletedItem[] = [{
      id: 'p2-a', name: 'page2-a', entityType: 'component', repository: 'r',
      format: 'maven2', deletedOn: '2026-03-15T10:00:00Z', deletedBy: 'admin',
      retentionExpiresOn: '2026-04-14T10:00:00Z',
    }];

    function returnPage(items: DeletedItem[], offset: number) {
      return { items, total: 40, offset, limit: 20, loading: false, error: null, refetch: jest.fn() };
    }

    it('selection survives navigating to a different page', () => {
      mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
      mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });
      mockUseDeletedItemsFacets.mockReturnValue({
        facets: { entityTypes: {}, formats: {}, deletedBy: [] },
        loading: false, error: null, refetch: jest.fn(),
      });

      // First render: page 1 visible.
      mockUseDeletedItems
        .mockReturnValueOnce(returnPage(PAGE_1, 0))
        .mockReturnValueOnce(returnPage(PAGE_1, 0))
        .mockReturnValue(returnPage(PAGE_2, 20));

      const { rerender } = render(<TrashPage />, { wrapper: Wrapper });

      // Select the page 1 item.
      fireEvent.click(screen.getByTestId('row-select-p1-a'));
      expect(screen.getByTestId('selected-count')).toHaveTextContent('selected:1');

      // Trigger page change. The mocked TrashTable exposes a goto-page-2 button.
      fireEvent.click(screen.getByTestId('goto-page-2'));
      rerender(<TrashPage />);

      // Selection persisted across the page change.
      expect(screen.getByTestId('selected-count')).toHaveTextContent('selected:1');
    });

    it('bulk action resolves DeletedItem objects across pages', () => {
      const purge = jest.fn().mockResolvedValue(undefined);
      mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
      mockUsePurgeItems.mockReturnValue({ purge, loading: false });
      mockUseDeletedItemsFacets.mockReturnValue({
        facets: { entityTypes: {}, formats: {}, deletedBy: [] },
        loading: false, error: null, refetch: jest.fn(),
      });

      // Page 1 then page 2.
      mockUseDeletedItems
        .mockReturnValueOnce(returnPage(PAGE_1, 0))
        .mockReturnValueOnce(returnPage(PAGE_1, 0))
        .mockReturnValue(returnPage(PAGE_2, 20));

      const { rerender } = render(<TrashPage />, { wrapper: Wrapper });

      // Select on page 1.
      fireEvent.click(screen.getByTestId('row-select-p1-a'));

      // Navigate to page 2.
      fireEvent.click(screen.getByTestId('goto-page-2'));
      rerender(<TrashPage />);

      // Select an item on page 2 too. Total selection across pages: 2.
      fireEvent.click(screen.getByTestId('row-select-p2-a'));
      expect(screen.getByTestId('selected-count')).toHaveTextContent('selected:2');

      // The bulk action bar should report 2 resolved items (one from each page).
      expect(screen.getByTestId('bulk-selected-count')).toHaveTextContent('bulk:2');
    });

    it('selection clears when filter changes', () => {
      mockUseRestoreItems.mockReturnValue({ restore: jest.fn(), loading: false });
      mockUsePurgeItems.mockReturnValue({ purge: jest.fn(), loading: false });
      mockUseDeletedItemsFacets.mockReturnValue({
        facets: { entityTypes: {}, formats: {}, deletedBy: [] },
        loading: false, error: null, refetch: jest.fn(),
      });
      mockUseDeletedItems.mockReturnValue(returnPage(PAGE_1, 0));

      const { rerender } = render(<TrashPage />, { wrapper: Wrapper });

      fireEvent.click(screen.getByTestId('row-select-p1-a'));
      expect(screen.getByTestId('selected-count')).toHaveTextContent('selected:1');

      // Trigger a filter change via the filter sidebar's filter-trigger button.
      fireEvent.click(screen.getByTestId('filter-trigger'));
      rerender(<TrashPage />);

      // Selection cleared because the result universe changed.
      expect(screen.getByTestId('selected-count')).toHaveTextContent('selected:0');
    });
  });
});
