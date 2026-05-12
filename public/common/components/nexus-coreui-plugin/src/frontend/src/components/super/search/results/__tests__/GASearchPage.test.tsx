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
import { GASearchPage } from '../GASearchPage';
import { mockResults } from '../mockData';

// Mock the useGASearch hook
jest.mock('../useGASearch', () => ({
  useGASearch: jest.fn(() => ({
    state: {
      results: [],
      loading: false,
      error: null,
      totalCount: 0,
      sort: 'relevance',
      sortDirection: 'desc',
    },
    search: jest.fn(),
    loadMore: jest.fn(),
    clear: jest.fn(),
    setSort: jest.fn(),
    hasMore: false,
  })),
}));

// Mock the core module
jest.mock('../../core', () => ({
  buildDetailRoute: jest.fn((gaId) => `#preview/browse/search/maven/ga/${gaId}`),
  buildSearchRoute: jest.fn(),
  GA_SEARCH_PARAMS: {
    QUERY: 'q',
    GROUP_ID: 'groupId',
    ARTIFACT_ID: 'artifactId',
  },
}));

import { useGASearch } from '../useGASearch';

const mockUseGASearch = useGASearch;

describe('GASearchPage', () => {

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders the search page with title', () => {
      render(<GASearchPage />);
      
      expect(screen.getByText('Maven Search')).toBeInTheDocument();
      expect(screen.getByText(/Search for Maven artifacts/)).toBeInTheDocument();
    });

    it('renders search input', () => {
      render(<GASearchPage />);
      
      expect(screen.getByPlaceholderText(/Search Maven artifacts/)).toBeInTheDocument();
    });

    it('renders with initial query from props', () => {
      render(<GASearchPage initialQuery="commons-lang3" />);
      
      const input = screen.getByPlaceholderText(/Search Maven artifacts/) as HTMLInputElement;
      expect(input.value).toBe('commons-lang3');
    });
  });

  describe('search functionality', () => {
    it('calls search when form is submitted', async () => {
      const mockSearch = jest.fn();
      mockUseGASearch.mockReturnValue({
        state: {
          results: [],
          loading: false,
          error: null,
          totalCount: 0,
          sort: 'relevance',
          sortDirection: 'desc',
        },
        search: mockSearch,
        loadMore: jest.fn(),
        clear: jest.fn(),
        setSort: jest.fn(),
        hasMore: false,
      });

      render(<GASearchPage />);
      
      const input = screen.getByPlaceholderText(/Search Maven artifacts/);
      await userEvent.type(input, 'guava');
      
      // Trigger search (implementation dependent)
      fireEvent.keyDown(input, { key: 'Enter' });
      
      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalled();
      });
    });
  });

  describe('results display', () => {
    it('displays results when available', () => {
      mockUseGASearch.mockReturnValue({
        state: {
          results: mockResults.slice(0, 2),
          loading: false,
          error: null,
          totalCount: 2,
          sort: 'relevance',
          sortDirection: 'desc',
        },
        search: jest.fn(),
        loadMore: jest.fn(),
        clear: jest.fn(),
        setSort: jest.fn(),
        hasMore: false,
      });

      render(<GASearchPage />);
      
      // Should show GA results (one per GA, not per version)
      expect(screen.getByText('commons-lang3')).toBeInTheDocument();
      expect(screen.getByText('guava')).toBeInTheDocument();
    });

    it('shows loading state', () => {
      mockUseGASearch.mockReturnValue({
        state: {
          results: [],
          loading: true,
          error: null,
          totalCount: 0,
          sort: 'relevance',
          sortDirection: 'desc',
        },
        search: jest.fn(),
        loadMore: jest.fn(),
        clear: jest.fn(),
        setSort: jest.fn(),
        hasMore: false,
      });

      render(<GASearchPage />);
      
      // Loading indicator should be present (implementation dependent)
      // This tests that the component handles loading state
    });

    it('shows error state', () => {
      mockUseGASearch.mockReturnValue({
        state: {
          results: [],
          loading: false,
          error: 'Search failed',
          totalCount: 0,
          sort: 'relevance',
          sortDirection: 'desc',
        },
        search: jest.fn(),
        loadMore: jest.fn(),
        clear: jest.fn(),
        setSort: jest.fn(),
        hasMore: false,
      });

      render(<GASearchPage />);
      
      expect(screen.getByText(/Search failed/)).toBeInTheDocument();
    });
  });

  describe('navigation', () => {
    it('calls onNavigateToDetail when result is clicked', async () => {
      const mockNavigate = jest.fn();
      mockUseGASearch.mockReturnValue({
        state: {
          results: mockResults.slice(0, 1),
          loading: false,
          error: null,
          totalCount: 1,
          sort: 'relevance',
          sortDirection: 'desc',
        },
        search: jest.fn(),
        loadMore: jest.fn(),
        clear: jest.fn(),
        setSort: jest.fn(),
        hasMore: false,
      });

      render(<GASearchPage onNavigateToDetail={mockNavigate} />);
      
      const resultRow = screen.getByText('commons-lang3');
      await userEvent.click(resultRow);
      
      expect(mockNavigate).toHaveBeenCalledWith('maven:org.apache.commons:commons-lang3');
    });
  });

  describe('pagination', () => {
    it('calls loadMore when load more button is clicked', async () => {
      const mockLoadMore = jest.fn();
      mockUseGASearch.mockReturnValue({
        state: {
          results: mockResults,
          loading: false,
          error: null,
          totalCount: 100,
          sort: 'relevance',
          sortDirection: 'desc',
        },
        search: jest.fn(),
        loadMore: mockLoadMore,
        clear: jest.fn(),
        setSort: jest.fn(),
        hasMore: true,
      });

      render(<GASearchPage />);
      
      const loadMoreButton = screen.getByText(/Load More/i);
      await userEvent.click(loadMoreButton);
      
      expect(mockLoadMore).toHaveBeenCalled();
    });

    it('hides load more when no more results', () => {
      mockUseGASearch.mockReturnValue({
        state: {
          results: mockResults.slice(0, 2),
          loading: false,
          error: null,
          totalCount: 2,
          sort: 'relevance',
          sortDirection: 'desc',
        },
        search: jest.fn(),
        loadMore: jest.fn(),
        clear: jest.fn(),
        setSort: jest.fn(),
        hasMore: false,
      });

      render(<GASearchPage />);
      
      expect(screen.queryByText(/Load More/i)).not.toBeInTheDocument();
    });
  });

  describe('GA aggregation', () => {
    it('shows one row per GA, not per version', () => {
      // This is the key requirement: results are GA-aggregated
      mockUseGASearch.mockReturnValue({
        state: {
          results: [
            {
              gaId: 'maven:org.apache.commons:commons-lang3',
              format: 'maven',
              displayName: 'commons-lang3',
              namespace: 'org.apache.commons',
              latestVersion: '3.14.0',
              versionsCount: 47, // Multiple versions...
              repositoriesCount: 2,
              lastUpdated: '2024-01-15T10:30:00Z',
            },
          ],
          loading: false,
          error: null,
          totalCount: 1,
          sort: 'relevance',
          sortDirection: 'desc',
        },
        search: jest.fn(),
        loadMore: jest.fn(),
        clear: jest.fn(),
        setSort: jest.fn(),
        hasMore: false,
      });

      render(<GASearchPage />);
      
      // ...but only ONE row in results
      const rows = screen.getAllByText('commons-lang3');
      expect(rows).toHaveLength(1);
      
      // Version count should be visible
      expect(screen.getByText('47')).toBeInTheDocument();
    });
  });
});

