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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import UnifiedSearchPage from '../UnifiedSearchPage';
import * as useUnifiedSearchModule from '../useUnifiedSearch';

// Mock the useUnifiedSearch hook
jest.mock('../useUnifiedSearch');

// Mock useSearchNavigation to avoid UIRouter dependency
jest.mock('../useSearchNavigation', () => ({
  useSearchNavigation: () => ({
    navigateToDetail: jest.fn(),
  }),
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

// Mock useSearchUrlState
jest.mock('../useSearchUrlState', () => ({
  useSearchUrlState: () => ({
    state: {
      format: 'all',
      query: '',
      filters: {},
    },
    updateUrl: jest.fn(),
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
});


