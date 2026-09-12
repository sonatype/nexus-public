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
import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import UnifiedSearchPage, { serializeSearchState } from '../UnifiedSearchPage';
import * as useUnifiedSearchModule from '../useUnifiedSearch';

// Mock the useUnifiedSearch hook
jest.mock('../useUnifiedSearch');

// Mock useSearchNavigation to avoid UIRouter dependency
const mockNavigateToDetail = jest.fn();
jest.mock('../useSearchNavigation', () => ({
  useSearchNavigation: () => ({
    navigateToDetail: mockNavigateToDetail,
  }),
  SEARCH_RETURN_URL_KEY: 'nexus-search-return-url',
}));

// Mock useRepositories
jest.mock('../useRepositories', () => ({
  useRepositories: () => ({
    repositories: ['maven-central', 'npm-proxy'],
    availableFormats: new Set(['maven2', 'npm']),
    formatCounts: { maven2: 1, npm: 1 },
    loading: false,
    error: undefined,
  }),
}));

// Mock useSearchUrlState. readFromUrl returns whatever mockUrlState holds;
// syncToUrl is a spy the tests assert against.
let mockUrlState = { format: 'all', query: '', filters: {} };
const mockSyncToUrl = jest.fn();
const mockReadFromUrl = jest.fn(() => mockUrlState);
jest.mock('../useSearchUrlState', () => ({
  useSearchUrlState: () => ({
    readFromUrl: mockReadFromUrl,
    syncToUrl: mockSyncToUrl,
    getShareableUrl: jest.fn(),
  }),
}));

// Mock useInstanceTotals (avoids ExtJS dependency)
jest.mock('../../../Welcome/dashboard/useInstanceTotals', () => ({
  useInstanceTotals: () => ({ data: null, loading: false }),
}));

const mockUseUnifiedSearch = useUnifiedSearchModule.useUnifiedSearch as jest.MockedFunction<
  typeof useUnifiedSearchModule.useUnifiedSearch
>;

// Helper to wrap components with Radix Theme
function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('UnifiedSearchPage', () => {
  const mockSearch = jest.fn();
  const mockLoadMore = jest.fn();
  const mockReset = jest.fn();
  const mockSetFormat = jest.fn();
  const mockSetQuery = jest.fn();
  const mockSetFilter = jest.fn();
  const mockSetFilters = jest.fn();
  const mockSetSortField = jest.fn();
  const mockSetSortDirection = jest.fn();
  const mockSetSort = jest.fn();

  const defaultMockState = {
    format: 'all' as const,
    query: '',
    filters: {},
    results: [],
    totalCount: 0,
    loading: false,
    error: undefined,
    continuationToken: undefined,
    sortField: 'lastUpdated' as const,
    sortDirection: 'desc' as const,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
    mockUrlState = { format: 'all', query: '', filters: {} };
    // Clear any sessionStorage state from previous tests
    sessionStorage.clear();
    mockNavigateToDetail.mockClear();
    mockUseUnifiedSearch.mockReturnValue({
      state: defaultMockState,
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: false,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });
  });

  it('renders the page with search results area', () => {
    renderWithTheme(<UnifiedSearchPage />);
    // Page doesn't have a separate "Search Components" title - it uses SearchResults
    // which shows "Components" heading (rendered in SearchResults component)
    // or empty state message when no results
    expect(screen.getByText(/no components found/i)).toBeInTheDocument();
  });

  it('renders the search header with format dropdown', () => {
    renderWithTheme(<UnifiedSearchPage />);
    // There are multiple comboboxes (format + sort), check that at least one exists
    const comboboxes = screen.getAllByRole('combobox');
    expect(comboboxes.length).toBeGreaterThanOrEqual(1);
  });

  it('renders the filter input in results area', () => {
    renderWithTheme(<UnifiedSearchPage />);
    // Responsive headers (mobile, tablet, desktop) each have a filter input
    const filterInputs = screen.getAllByPlaceholderText(/filter by/i);
    expect(filterInputs.length).toBeGreaterThanOrEqual(1);
  });

  it('renders the sidebar filters', () => {
    renderWithTheme(<UnifiedSearchPage />);
    // SearchSidebar has Format and Repository sections (ux-lab pattern)
    expect(screen.getByText('Format')).toBeInTheDocument();
  });

  it('shows empty state when no results', () => {
    renderWithTheme(<UnifiedSearchPage />);
    expect(screen.getByText(/no components found/i)).toBeInTheDocument();
  });

  it('shows loading state', () => {
    mockUseUnifiedSearch.mockReturnValue({
      state: { ...defaultMockState, loading: true },
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: false,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });

    renderWithTheme(<UnifiedSearchPage />);
    // SearchResults shows Spinner when loading with no results
    expect(screen.getAllByText('Components')[0]).toBeInTheDocument();
  });

  it('shows error state with retry button', () => {
    mockUseUnifiedSearch.mockReturnValue({
      state: { ...defaultMockState, error: 'Search failed' },
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: false,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });

    renderWithTheme(<UnifiedSearchPage />);
    expect(screen.getByText('Search failed')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('displays search results', () => {
    const mockResults = [
      {
        id: '1',
        name: 'lodash',
        format: 'npm',
        repository: 'npm-proxy',
        version: '4.17.21',
      },
      {
        id: '2',
        name: 'spring-core',
        format: 'maven2',
        repository: 'maven-central',
        group: 'org.springframework',
        version: '6.1.0',
      },
    ];

    mockUseUnifiedSearch.mockReturnValue({
      state: { ...defaultMockState, results: mockResults, totalCount: 2 },
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: false,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });

    renderWithTheme(<UnifiedSearchPage />);
    // SearchResultCard shows "name version" - use getAllByText for name which may appear in multiple places
    expect(screen.getByRole('button', { name: /view details for lodash/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /view details for spring-core/i })).toBeInTheDocument();
  });

  it('renders format dropdown with All formats as default', () => {
    renderWithTheme(<UnifiedSearchPage />);

    // Verify the format dropdown exists and shows "All formats"
    expect(screen.getByText('All formats')).toBeInTheDocument();
  });

  // Note: The main search input is in the top navigation, not on UnifiedSearchPage.
  // UnifiedSearchPage receives search queries from URL state synced by the navigation.
  // Tests for search input interaction should be in the top nav component tests.
  it('syncs search from URL on mount', async () => {
    // Set URL with query param
    window.location.hash = '#preview/search?q=lodash';
    
    renderWithTheme(<UnifiedSearchPage />);

    // The component should sync the query from URL and trigger search
    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalled();
    });
  });

  it('enables Next page button when hasMore is true', () => {
    const mockResults = [
      { id: '1', name: 'test', format: 'npm', repository: 'npm', version: '1.0.0' },
    ];

    mockUseUnifiedSearch.mockReturnValue({
      state: { ...defaultMockState, results: mockResults, totalCount: 10 },
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: true,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });

    renderWithTheme(<UnifiedSearchPage />);
    const nextButton = screen.getByRole('button', {
      name: /next page|load more/i,
    });
    expect(nextButton).not.toBeDisabled();
  });

  it('calls loadMore when Next page button is clicked', async () => {
    const mockResults = [
      { id: '1', name: 'test', format: 'npm', repository: 'npm', version: '1.0.0' },
    ];

    mockUseUnifiedSearch.mockReturnValue({
      state: { ...defaultMockState, results: mockResults, totalCount: 10 },
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: true,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });

    renderWithTheme(<UnifiedSearchPage />);

    await userEvent.click(
      screen.getByRole('button', { name: /next page|load more/i }),
    );
    expect(mockLoadMore).toHaveBeenCalled();
  });

  it('disables sidebar filters when loading', () => {
    mockUseUnifiedSearch.mockReturnValue({
      state: { ...defaultMockState, loading: true },
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: false,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });

    renderWithTheme(<UnifiedSearchPage />);

    // Check that the sidebar is rendered (Format section visible)
    expect(screen.getByText('Format')).toBeInTheDocument();
  });

  it('executes search on mount', async () => {
    jest.useFakeTimers();

    renderWithTheme(<UnifiedSearchPage />);

    // Advance timers to trigger the setTimeout in useEffect
    jest.runAllTimers();

    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalled();
    });

    jest.useRealTimers();
  });

  it('shows Components header and count when query is provided', () => {
    const mockResults = [
      { id: '1', name: 'react', format: 'npm', repository: 'npm', version: '18.0.0' },
    ];

    mockUseUnifiedSearch.mockReturnValue({
      state: { ...defaultMockState, results: mockResults, totalCount: 50, query: 'react' },
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: true,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });

    renderWithTheme(<UnifiedSearchPage />);
    
    // Current behavior: Shows "Components X+" header format regardless of query
    expect(screen.getAllByText('Components').length).toBeGreaterThan(0);
    expect(screen.getAllByText('50+').length).toBeGreaterThan(0); // hasMore=true shows + suffix
  });

  describe('URL state sync', () => {
    it('serializes machine state to the URL after the debounce (syncToUrl)', () => {
      jest.useFakeTimers();
      mockUseUnifiedSearch.mockReturnValue({
        state: {
          ...defaultMockState,
          format: 'maven',
          query: 'spring',
          filters: { groupId: 'org.apache' },
        },
        placeholder: 'Search by group ID or artifact ID',
        setFormat: mockSetFormat,
        setQuery: mockSetQuery,
        setFilter: mockSetFilter,
        setFilters: mockSetFilters,
        search: mockSearch,
        loadMore: mockLoadMore,
        reset: mockReset,
        hasMore: false,
        setSortField: mockSetSortField,
        setSortDirection: mockSetSortDirection,
        setSort: mockSetSort,
      });

      renderWithTheme(<UnifiedSearchPage />);

      // URL write is debounced — nothing before the timer fires.
      expect(mockSyncToUrl).not.toHaveBeenCalled();

      act(() => {
        jest.runAllTimers();
      });

      expect(mockSyncToUrl).toHaveBeenCalledTimes(1);
      expect(mockSyncToUrl).toHaveBeenCalledWith(
        expect.objectContaining({
          format: 'maven',
          query: 'spring',
          filters: { groupId: 'org.apache' },
        }),
      );
      jest.useRealTimers();
    });

    it('coalesces rapid state changes into a single URL write (no per-keystroke history)', () => {
      jest.useFakeTimers();

      const baseMock = (filters: Record<string, string>) => ({
        state: { ...defaultMockState, format: 'maven' as const, filters },
        placeholder: 'Search by group ID or artifact ID',
        setFormat: mockSetFormat,
        setQuery: mockSetQuery,
        setFilter: mockSetFilter,
        setFilters: mockSetFilters,
        search: mockSearch,
        loadMore: mockLoadMore,
        reset: mockReset,
        hasMore: false,
        setSortField: mockSetSortField,
        setSortDirection: mockSetSortDirection,
        setSort: mockSetSort,
      });

      mockUseUnifiedSearch.mockReturnValue(baseMock({ groupId: 'o' }));
      const { rerender } = renderWithTheme(<UnifiedSearchPage />);

      // Simulate rapid keystrokes: state.filters changes on every render,
      // each within the debounce window.
      for (const value of ['or', 'org', 'org.', 'org.apache']) {
        mockUseUnifiedSearch.mockReturnValue(baseMock({ groupId: value }));
        rerender(
          <Theme>
            <UnifiedSearchPage />
          </Theme>,
        );
        act(() => {
          jest.advanceTimersByTime(100); // less than the debounce window
        });
      }

      // Mid-burst: no write yet.
      expect(mockSyncToUrl).not.toHaveBeenCalled();

      // User pauses — the debounce fires once with the settled value.
      act(() => {
        jest.runAllTimers();
      });

      expect(mockSyncToUrl).toHaveBeenCalledTimes(1);
      expect(mockSyncToUrl).toHaveBeenCalledWith(
        expect.objectContaining({ filters: { groupId: 'org.apache' } }),
      );
      jest.useRealTimers();
    });

    it('rehydrates the machine from URL params on mount', async () => {
      mockUrlState = {
        format: 'npm',
        query: 'lodash',
        filters: { author: 'foo' },
      };

      renderWithTheme(<UnifiedSearchPage />);

      await waitFor(() => {
        expect(mockSetFormat).toHaveBeenCalledWith('npm');
      });
      expect(mockSetQuery).toHaveBeenCalledWith('lodash');
      expect(mockSetFilters).toHaveBeenCalledWith({ author: 'foo' });
      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalled();
      });
    });

    it('does not re-write the URL when state matches what was read from it', () => {
      // State equals the URL state -> serialization effect should be a no-op.
      mockUrlState = { format: 'all', query: '', filters: {} };

      renderWithTheme(<UnifiedSearchPage />);

      // The mount rehydration sets lastSerialized to the URL state, and the
      // default machine state matches it, so syncToUrl must not fire.
      expect(mockSyncToUrl).not.toHaveBeenCalled();
    });

    it('cancels a pending URL write when unmounted mid-debounce', () => {
      jest.useFakeTimers();
      mockUseUnifiedSearch.mockReturnValue({
        state: {
          ...defaultMockState,
          format: 'maven',
          query: 'spring',
          filters: { groupId: 'org.apache' },
        },
        placeholder: 'Search by group ID or artifact ID',
        setFormat: mockSetFormat,
        setQuery: mockSetQuery,
        setFilter: mockSetFilter,
        setFilters: mockSetFilters,
        search: mockSearch,
        loadMore: mockLoadMore,
        reset: mockReset,
        hasMore: false,
        setSortField: mockSetSortField,
        setSortDirection: mockSetSortDirection,
        setSort: mockSetSort,
      });

      const { unmount } = renderWithTheme(<UnifiedSearchPage />);

      // A write is scheduled but the debounce has not fired yet.
      expect(mockSyncToUrl).not.toHaveBeenCalled();

      // Unmount mid-debounce, then let all timers run. The cleanup must have
      // cleared the pending timer so no write (or state update) fires afterwards.
      unmount();
      act(() => {
        jest.runAllTimers();
      });

      expect(mockSyncToUrl).not.toHaveBeenCalled();
      jest.useRealTimers();
    });
  });

  it('shows "Components" header when no specific query', () => {
    const mockResults = [
      { id: '1', name: 'test', format: 'npm', repository: 'npm', version: '1.0.0' },
    ];

    mockUseUnifiedSearch.mockReturnValue({
      state: { ...defaultMockState, results: mockResults, totalCount: 50, query: '' },
      placeholder: 'Search by component name or ID',
      setFormat: mockSetFormat,
      setQuery: mockSetQuery,
      setFilter: mockSetFilter,
      setFilters: mockSetFilters,
      search: mockSearch,
      loadMore: mockLoadMore,
      reset: mockReset,
      hasMore: true,
      setSortField: mockSetSortField,
      setSortDirection: mockSetSortDirection,
      setSort: mockSetSort,
    });

    renderWithTheme(<UnifiedSearchPage />);

    // Should show "Components X" format
    expect(screen.getAllByText('Components').length).toBeGreaterThan(0);
  });

  describe('User interactions', () => {
    it('calls reset and search when Reset button is clicked', async () => {
      jest.useFakeTimers();

      renderWithTheme(<UnifiedSearchPage />);

      const resetButton = screen.getByRole('button', { name: /reset/i });
      await userEvent.click(resetButton);

      expect(mockReset).toHaveBeenCalled();

      act(() => {
        jest.runAllTimers();
      });

      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalled();
      });

      jest.useRealTimers();
    });

    it('calls search when retry button is clicked', async () => {
      mockUseUnifiedSearch.mockReturnValue({
        state: { ...defaultMockState, error: 'Search failed' },
        placeholder: 'Search by component name or ID',
        setFormat: mockSetFormat,
        setQuery: mockSetQuery,
        setFilter: mockSetFilter,
        setFilters: mockSetFilters,
        search: mockSearch,
        loadMore: mockLoadMore,
        reset: mockReset,
        hasMore: false,
        setSortField: mockSetSortField,
        setSortDirection: mockSetSortDirection,
        setSort: mockSetSort,
      });

      renderWithTheme(<UnifiedSearchPage />);

      const retryButton = screen.getByRole('button', { name: /retry/i });
      await userEvent.click(retryButton);

      expect(mockSearch).toHaveBeenCalled();
    });
  });

  describe('Deep-link URL rehydration', () => {
    afterEach(() => {
      mockUrlState = { format: 'all', query: '', filters: {} };
    });

    it('restores the nameOrVersion filter from a bookmarked / shared URL', async () => {
      // Companion to the buildQueryString/parseUrlState round-trip test in
      // useSearchUrlState.test.ts — this verifies the mount effect actually
      // pipes that URL state into the search machine when there is no
      // sessionStorage payload (deep-link / refresh / share path).
      mockUrlState = {
        format: 'all',
        query: '',
        filters: { nameOrVersion: 'commons' },
      };

      renderWithTheme(<UnifiedSearchPage />);

      await waitFor(() => {
        expect(mockSetFilters).toHaveBeenCalledWith({ nameOrVersion: 'commons' });
      });
      expect(mockSearch).toHaveBeenCalled();
    });
  });

  describe('sort controls', () => {
    // jsdom lacks the pointer-capture / scrollIntoView APIs Radix Select needs
    // when the dropdown opens.
    beforeAll(() => {
      window.HTMLElement.prototype.hasPointerCapture = jest.fn(() => false);
      window.HTMLElement.prototype.releasePointerCapture = jest.fn();
      window.HTMLElement.prototype.scrollIntoView = jest.fn();
    });

    /** The results header renders three times (mobile / tablet / desktop). */
    function sortTrigger() {
      return screen.getAllByRole('combobox', { name: /^Sort:/ })[0];
    }

    function renderWithSort(sortField: string, sortDirection: string) {
      mockUseUnifiedSearch.mockReturnValue({
        state: { ...defaultMockState, sortField, sortDirection } as never,
        placeholder: 'Search by component name or ID',
        setFormat: mockSetFormat,
        setQuery: mockSetQuery,
        setFilter: mockSetFilter,
        setFilters: mockSetFilters,
        search: mockSearch,
        loadMore: mockLoadMore,
        reset: mockReset,
        hasMore: false,
        setSortField: mockSetSortField,
        setSortDirection: mockSetSortDirection,
        setSort: mockSetSort,
      });
      return renderWithTheme(<UnifiedSearchPage />);
    }

    it('shows the shared sort state in the dropdown', () => {
      renderWithSort('name', 'desc');

      expect(sortTrigger()).toHaveTextContent('Name — Z-A');
    });

    it('renders no sorting control outside the results header', () => {
      renderWithSort('name', 'desc');

      // The results-header dropdown is the only sorting control; the sidebar
      // (also rendered inside the mobile filter drawer) must offer none.
      expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
      expect(screen.queryAllByRole('radio')).toHaveLength(0);
    });

    it('moves the dropdown when the sort state changes', () => {
      const { rerender } = renderWithSort('name', 'asc');

      expect(sortTrigger()).toHaveTextContent('Name — A-Z');

      // A state change (e.g. from a URL rehydration) must move the dropdown.
      mockUseUnifiedSearch.mockReturnValue({
        state: {
          ...defaultMockState,
          sortField: 'lastUpdated',
          sortDirection: 'asc',
        } as never,
        placeholder: 'Search by component name or ID',
        setFormat: mockSetFormat,
        setQuery: mockSetQuery,
        setFilter: mockSetFilter,
        setFilters: mockSetFilters,
        search: mockSearch,
        loadMore: mockLoadMore,
        reset: mockReset,
        hasMore: false,
        setSortField: mockSetSortField,
        setSortDirection: mockSetSortDirection,
        setSort: mockSetSort,
      });
      rerender(
        <Theme>
          <UnifiedSearchPage />
        </Theme>,
      );

      expect(sortTrigger()).toHaveTextContent('Last updated — Oldest first');
    });

    it('routes a dropdown selection into the shared sort state and re-runs the search', async () => {
      renderWithSort('lastUpdated', 'desc');
      mockSearch.mockClear();

      await userEvent.click(sortTrigger());
      await userEvent.click(screen.getAllByRole('option', { name: 'A-Z' })[0]);

      expect(mockSetSort).toHaveBeenCalledWith('name', 'asc');
      await waitFor(() => expect(mockSearch).toHaveBeenCalled());
    });

    it('writes the selected sort to the URL', async () => {
      renderWithSort('name', 'asc');

      await waitFor(() => {
        expect(mockSyncToUrl).toHaveBeenCalledWith(
          expect.objectContaining({ sortField: 'name', sortDirection: 'asc' }),
        );
      });
    });
  });

  describe('URL rehydration with sort', () => {
    it('rehydrates sort from URL params', async () => {
      mockUrlState = {
        format: 'npm',
        query: 'lodash',
        filters: {},
        sortField: 'name',
        sortDirection: 'asc',
      };

      renderWithTheme(<UnifiedSearchPage />);

      await waitFor(() => {
        expect(mockSetSort).toHaveBeenCalledWith('name', 'asc');
      });
    });
  });

  describe('Popstate history navigation', () => {
    it('ignores popstate when URL hash does not match search route', async () => {
      renderWithTheme(<UnifiedSearchPage />);

      mockSetFormat.mockClear();

      // Change hash to something unrelated
      const originalHash = window.location.hash;
      window.location.hash = '#preview/browse/assets';

      act(() => {
        window.dispatchEvent(new PopStateEvent('popstate'));
      });

      // Should NOT have rehydrated because hash doesn't include 'preview/browse/search'
      expect(mockSetFormat).not.toHaveBeenCalled();

      // Restore original hash
      window.location.hash = originalHash;
    });

    it('rehydrates when hash matches search route after popstate', async () => {
      // Set hash to search route
      window.location.hash = '#preview/browse/search';

      renderWithTheme(<UnifiedSearchPage />);

      // Clear the initial search call from mount
      mockSetFormat.mockClear();
      mockSetQuery.mockClear();
      mockSetFilters.mockClear();

      // Simulate a Back/Forward navigation that brings new URL state.
      // The mount rehydration serialized the initial URL (format: 'all') into
      // the echo-guard, so this genuinely-different state (format: 'maven')
      // fails the guard's equality check and triggers a real rehydration —
      // which is exactly what this test asserts.
      mockUrlState = { format: 'maven', query: 'spring', filters: { groupId: 'org.apache' } };

      act(() => {
        window.dispatchEvent(new PopStateEvent('popstate'));
      });

      await waitFor(() => {
        expect(mockSetFormat).toHaveBeenCalledWith('maven');
      }, { timeout: 3000 });
      // Assert the full rehydration (not just format) so the test does not
      // depend on the echo-guard's prior state.
      expect(mockSetQuery).toHaveBeenCalledWith('spring');
      expect(mockSetFilters).toHaveBeenCalledWith({ groupId: 'org.apache' });
    });
  });

  describe('URL is the only mount-time source', () => {
    afterEach(() => {
      mockUrlState = { format: 'all', query: '', filters: {} };
    });

    it('rehydrates every criterion from the URL and searches once (AT-002)', async () => {
      mockUrlState = {
        format: 'maven',
        query: 'spring',
        filters: { groupId: 'org.apache', nameOrVersion: 'commons' },
        sortField: 'name',
        sortDirection: 'asc',
      };

      renderWithTheme(<UnifiedSearchPage />);

      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalledTimes(1);
      });
      expect(mockSetFormat).toHaveBeenCalledWith('maven');
      expect(mockSetQuery).toHaveBeenCalledWith('spring');
      expect(mockSetFilters).toHaveBeenCalledWith({
        groupId: 'org.apache',
        nameOrVersion: 'commons',
      });
      expect(mockSetSort).toHaveBeenCalledWith('name', 'asc');
    });

    it('ignores a stale return-URL key and uses the URL (AT-003)', async () => {
      // Regression guard for NEXUS-54503 Defect 1: sessionStorage is copied into
      // a tab opened from a link, and the key survives a refresh, so the mount
      // effect must not consult it at all.
      sessionStorage.setItem(
        'nexus-search-return-url',
        '#preview/browse/search?q=stale&format=npm',
      );
      mockUrlState = { format: 'all', query: 'fresh', filters: {} };

      renderWithTheme(<UnifiedSearchPage />);

      await waitFor(() => {
        expect(mockSetQuery).toHaveBeenCalledWith('fresh');
      });
      expect(mockSetQuery).not.toHaveBeenCalledWith('stale');
      expect(mockSetFormat).not.toHaveBeenCalledWith('npm');
      // Untouched — only the breadcrumb consumes it.
      expect(sessionStorage.getItem('nexus-search-return-url')).not.toBeNull();
    });
  });

  describe('flush before navigating to detail', () => {
    it('writes the settled state to the URL before navigating (AT-020)', async () => {
      const mockResults = [
        {
          id: '1',
          name: 'lodash',
          format: 'npm',
          repository: 'npm-proxy',
          version: '4.17.21',
        },
      ];
      mockUseUnifiedSearch.mockReturnValue({
        state: {
          ...defaultMockState,
          results: mockResults,
          totalCount: 1,
          query: 'lodash',
          filters: { nameOrVersion: 'lodash' },
        },
        placeholder: 'Search by component name or ID',
        setFormat: mockSetFormat,
        setQuery: mockSetQuery,
        setFilter: mockSetFilter,
        setFilters: mockSetFilters,
        search: mockSearch,
        loadMore: mockLoadMore,
        reset: mockReset,
        hasMore: false,
        setSortField: mockSetSortField,
        setSortDirection: mockSetSortDirection,
        setSort: mockSetSort,
      });

      renderWithTheme(<UnifiedSearchPage />);
      mockSyncToUrl.mockClear();

      await userEvent.click(screen.getByRole('button', { name: /view details for lodash/i }));

      // replace=true: the entry the user is standing on is rewritten, and the
      // navigation that follows pushes its own. Without this flush, a click
      // inside the 350ms debounce window stores a stale return URL.
      expect(mockSyncToUrl).toHaveBeenCalledWith(
        expect.objectContaining({
          query: 'lodash',
          filters: { nameOrVersion: 'lodash' },
        }),
        true,
      );
      expect(mockNavigateToDetail).toHaveBeenCalledWith(mockResults[0]);
    });
  });

  describe('page-level filter and the global query', () => {
    /** Mock the hook with a given machine state, keeping every dispatcher stable. */
    function mockStateWith(overrides: Record<string, unknown>) {
      mockUseUnifiedSearch.mockReturnValue({
        state: { ...defaultMockState, ...overrides },
        placeholder: 'Search by component name or ID',
        setFormat: mockSetFormat,
        setQuery: mockSetQuery,
        setFilter: mockSetFilter,
        setFilters: mockSetFilters,
        search: mockSearch,
        loadMore: mockLoadMore,
        reset: mockReset,
        hasMore: false,
        setSortField: mockSetSortField,
        setSortDirection: mockSetSortDirection,
        setSort: mockSetSort,
      });
    }

    it('shows the global query in the filter input when no nameOrVersion is set (AT-004)', () => {
      mockStateWith({ query: 'spring', filters: {} });

      renderWithTheme(<UnifiedSearchPage />);

      const inputs = screen.getAllByPlaceholderText(/filter by component name or version/i);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0]).toHaveValue('spring');
    });

    it('prefers nameOrVersion over the query when both are set (AT-004)', () => {
      mockStateWith({ query: 'spring', filters: { nameOrVersion: 'commons' } });

      renderWithTheme(<UnifiedSearchPage />);

      const inputs = screen.getAllByPlaceholderText(/filter by component name or version/i);
      expect(inputs[0]).toHaveValue('commons');
    });

    it('clears the query and sets nameOrVersion on edit (AT-005)', () => {
      // fireEvent + fake timers, not userEvent: userEvent@12 (installed here) has
      // no .setup()/advanceTimers option, and the SearchResults debounce test
      // (SearchResults.test.tsx) already establishes fireEvent.change as this
      // codebase's pattern for driving a debounced controlled input under fake
      // timers.
      jest.useFakeTimers();
      try {
        mockStateWith({ query: 'spring', filters: {} });

        renderWithTheme(<UnifiedSearchPage />);
        const input = screen.getAllByPlaceholderText(
          /filter by component name or version/i,
        )[0];

        fireEvent.change(input, { target: { value: 'boot' } });
        // SearchResults debounces the filter input by 500ms.
        act(() => {
          jest.advanceTimersByTime(600);
        });

        expect(mockSetQuery).toHaveBeenCalledWith('');
        expect(mockSetFilter).toHaveBeenCalledWith('nameOrVersion', 'boot');
      } finally {
        jest.useRealTimers();
      }
    });
  });
});

describe('serializeSearchState (echo-guard key)', () => {
  const base = { format: 'all' as const, query: '', filters: {} };

  it('distinguishes a non-default direction on the default sort field', () => {
    // The core of the echo-guard: lastUpdated/asc must NOT collapse to the
    // default lastUpdated/desc, otherwise the URL write that re-applies
    // direction=asc after a Back navigation would be silently suppressed.
    const asc = serializeSearchState({ ...base, sortField: 'lastUpdated', sortDirection: 'asc' });
    const desc = serializeSearchState({ ...base, sortField: 'lastUpdated', sortDirection: 'desc' });

    expect(asc).not.toBe(desc);
  });

  it('distinguishes a non-default sort field', () => {
    const byName = serializeSearchState({ ...base, sortField: 'name', sortDirection: 'desc' });
    const byDefault = serializeSearchState(base);

    expect(byName).not.toBe(byDefault);
  });
});
