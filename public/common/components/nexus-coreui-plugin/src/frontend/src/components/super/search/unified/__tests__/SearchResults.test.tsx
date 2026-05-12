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
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { SearchResults } from '../SearchResults';

// Helper to wrap components with Radix Theme
function renderWithTheme(ui) {
  return render(<Theme>{ui}</Theme>);
}

const mockResults = [
  {
    id: '1',
    name: 'lodash',
    format: 'npm',
    repository: 'npm-proxy',
    group: undefined,
    version: '4.17.21',
    lastUpdated: '2024-01-15T10:00:00Z',
  },
  {
    id: '2',
    name: 'spring-core',
    format: 'maven2',
    repository: 'maven-central',
    group: 'org.springframework',
    version: '6.1.0',
    lastUpdated: '2024-01-10T08:00:00Z',
  },
];

describe('SearchResults', () => {
  const defaultProps = {
    results: mockResults,
    loading: false,
    totalCount: 2,
    hasMore: false,
    onLoadMore: jest.fn(),
    onSelect: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading state', () => {
    it('renders loading state when loading with no results', () => {
      renderWithTheme(<SearchResults {...defaultProps} results={[]} loading={true} />);
      expect(screen.getAllByText('Components').length).toBeGreaterThan(0);
      expect(screen.queryByText(/lodash|spring-core/)).not.toBeInTheDocument();
    });

    it('shows loading indicator when loading more results', () => {
      renderWithTheme(<SearchResults {...defaultProps} loading={true} hasMore={true} />);
      expect(screen.getByText(/loading more components/i)).toBeInTheDocument();
    });
  });

  describe('empty state', () => {
    it('renders empty state when no results', () => {
      renderWithTheme(<SearchResults {...defaultProps} results={[]} totalCount={0} />);
      expect(screen.getByText(/no components found/i)).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('renders error state with message', () => {
      renderWithTheme(
        <SearchResults {...defaultProps} results={[]} error="Search failed" />
      );
      expect(screen.getByText('Search failed')).toBeInTheDocument();
    });

    it('renders retry button when onRetry is provided', async () => {
      const onRetry = jest.fn();
      renderWithTheme(
        <SearchResults
          {...defaultProps}
          results={[]}
          error="Search failed"
          onRetry={onRetry}
        />
      );
      const retryButton = screen.getByRole('button', { name: /retry/i });
      await userEvent.click(retryButton);
      expect(onRetry).toHaveBeenCalled();
    });
  });

  describe('header', () => {
    it('renders "Components" heading', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getAllByRole('heading', { name: 'Components' }).length).toBeGreaterThan(0);
    });

    it('renders total count', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      const countElements = screen.getAllByText('2');
      expect(countElements.length).toBeGreaterThan(0);
    });

    it('renders total count with + when hasMore is true', () => {
      renderWithTheme(
        <SearchResults {...defaultProps} hasMore={true} totalCount={50} />
      );
      expect(screen.getAllByText('50+').length).toBeGreaterThan(0);
    });

    it('renders filter input', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getAllByPlaceholderText(/filter by component name/i).length).toBeGreaterThan(0);
    });
  });

  describe('results list', () => {
    it('renders result cards for each result', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText(/lodash/)).toBeInTheDocument();
      expect(screen.getByText(/spring-core/)).toBeInTheDocument();
    });

    it('calls onSelect when result card is clicked', async () => {
      const onSelect = jest.fn();
      renderWithTheme(<SearchResults {...defaultProps} onSelect={onSelect} />);
      await userEvent.click(screen.getByRole('button', { name: /view details for lodash/i }));
      expect(onSelect).toHaveBeenCalledWith(mockResults[0]);
    });

    it('displays format badges', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText('npm')).toBeInTheDocument();
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('displays version', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText(/4\.17\.21/)).toBeInTheDocument();
      expect(screen.getByText(/6\.1\.0/)).toBeInTheDocument();
    });

    it('displays repository names', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText(/npm-proxy/)).toBeInTheDocument();
      expect(screen.getByText(/maven-central/)).toBeInTheDocument();
    });

    it('displays group/namespace when available', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText('org.springframework')).toBeInTheDocument();
    });
  });

  describe('pagination', () => {
    it('renders pagination controls', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getAllByRole('button', { name: /first page/i }).length).toBeGreaterThan(0);
      expect(screen.getAllByRole('button', { name: /previous page/i }).length).toBeGreaterThan(0);
      expect(screen.getAllByRole('button', { name: /next page/i }).length).toBeGreaterThan(0);
      expect(screen.getAllByRole('button', { name: /last page/i }).length).toBeGreaterThan(0);
    });

    it('enables Next page button when hasMore is true', () => {
      renderWithTheme(<SearchResults {...defaultProps} hasMore={true} />);
      const nextButton = screen.getByRole('button', { name: /load more|next page/i });
      expect(nextButton).not.toBeDisabled();
    });

    it('disables Next page button when hasMore is false', () => {
      renderWithTheme(<SearchResults {...defaultProps} hasMore={false} />);
      const nextButton = screen.getByRole('button', { name: /next page/i });
      expect(nextButton).toBeDisabled();
    });

    it('calls onLoadMore when Next page button is clicked', async () => {
      const onLoadMore = jest.fn();
      renderWithTheme(
        <SearchResults {...defaultProps} hasMore={true} onLoadMore={onLoadMore} />
      );
      // When on last page with hasMore, Next button has aria-label "Load more"
      const loadMoreButton = screen.getByRole('button', { name: /load more|next page/i });
      await userEvent.click(loadMoreButton);
      expect(onLoadMore).toHaveBeenCalled();
    });

    it('renders page info', () => {
      renderWithTheme(<SearchResults {...defaultProps} totalCount={50} />);
      // TablePagination shows "Showing X of Y" - with 2 results, 20 per page: "Showing 2 of 2"
      expect(screen.getByText(/Showing/)).toBeInTheDocument();
      expect(screen.getByText(/of 2/)).toBeInTheDocument();
    });
  });

  describe('sort dropdown', () => {
    it('renders sort dropdown', () => {
      renderWithTheme(<SearchResults {...defaultProps} sortBy="lastUpdated" />);
      expect(screen.getAllByRole('combobox').length).toBeGreaterThan(0);
    });

    it('displays current sort option', () => {
      renderWithTheme(<SearchResults {...defaultProps} sortBy="lastUpdated" />);
      expect(screen.getAllByText(/Latest Release/).length).toBeGreaterThan(0);
    });

    it('accepts sortBy and onSortChange props', () => {
      const onSortChange = jest.fn();
      renderWithTheme(
        <SearchResults {...defaultProps} sortBy="name" onSortChange={onSortChange} />
      );
      expect(screen.getAllByRole('combobox').length).toBeGreaterThan(0);
      expect(screen.getAllByText(/Name/).length).toBeGreaterThan(0);
    });
  });

  describe('filter input', () => {
    it('renders with current nameFilter value', () => {
      renderWithTheme(<SearchResults {...defaultProps} nameFilter="lodash" />);
      const filterInput = screen.getAllByPlaceholderText(/filter by component name/i)[0];
      expect(filterInput).toHaveValue('lodash');
    });

    it('calls onNameFilterChange after debounce when typing', async () => {
      jest.useFakeTimers();
      const onNameFilterChange = jest.fn();
      renderWithTheme(
        <SearchResults {...defaultProps} onNameFilterChange={onNameFilterChange} />
      );
      const filterInput = screen.getAllByPlaceholderText(/filter by component name/i)[0];
      
      // Simulate typing - each character triggers handleFilterChange
      fireEvent.change(filterInput, { target: { value: 'test' } });
      
      // Should not be called immediately (debounced)
      expect(onNameFilterChange).not.toHaveBeenCalled();
      
      // Fast-forward past debounce timeout (500ms)
      jest.advanceTimersByTime(500);
      
      expect(onNameFilterChange).toHaveBeenCalledWith('test');
      jest.useRealTimers();
    });

    it('triggers filter immediately on Enter key with initial value', () => {
      const onNameFilterChange = jest.fn();
      // Set initial nameFilter prop so localFilterValue is already set
      renderWithTheme(
        <SearchResults 
          {...defaultProps} 
          nameFilter="lodash" 
          onNameFilterChange={onNameFilterChange} 
        />
      );
      const filterInput = screen.getAllByPlaceholderText(/filter by component name/i)[0];
      
      // Press Enter - should trigger callback with current filter value
      fireEvent.keyDown(filterInput, { key: 'Enter' });
      
      // Should be called immediately
      expect(onNameFilterChange).toHaveBeenCalledWith('lodash');
    });
  });
});
