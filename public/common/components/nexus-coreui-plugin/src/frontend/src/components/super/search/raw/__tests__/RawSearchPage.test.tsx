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
import '@testing-library/jest-dom';

import { RawSearchPage } from '../RawSearchPage';
import { RawSearchFilters } from '../RawSearchFilters';
import { RawSearchResults } from '../RawSearchResults';
import { RawResultRow } from '../RawResultRow';
import { mockRawResults } from '../mockData';

// Mock the useRawSearch hook
jest.mock('../useRawSearch', () => ({
  useRawSearch: () => ({
    state: {
      filters: {},
      loading: false,
      error: undefined,
      results: mockRawResults,
      totalCount: mockRawResults.length,
      continuationToken: undefined,
    },
    setFilters: jest.fn(),
    search: jest.fn(),
    loadMore: jest.fn(),
    clear: jest.fn(),
    hasMore: false,
  }),
}));

describe('RawSearchPage', () => {
  it('renders the search page with title', () => {
    render(<RawSearchPage />);

    expect(screen.getByRole('heading', { name: /raw search/i })).toBeInTheDocument();
    expect(screen.getByText(/search for files in raw format repositories/i)).toBeInTheDocument();
  });

  it('renders search filters', () => {
    render(<RawSearchPage />);

    // Use placeholder text since labels aren't properly associated with inputs
    expect(screen.getByPlaceholderText(/search across all fields/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/readme\.md/i)).toBeInTheDocument();
  });

  it('calls onNavigateToDetail when a result is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<RawSearchPage onNavigateToDetail={onNavigateToDetail} />);

    // Results are rendered in a table row, not a button - click on the file name text
    const resultName = screen.getByText('readme.md');
    await userEvent.click(resultName);

    expect(onNavigateToDetail).toHaveBeenCalledWith('raw:raw-1');
  });
});

describe('RawSearchFilters', () => {
  it('renders all filter inputs', () => {
    const mockProps = {
      filters: {},
      onFiltersChange: jest.fn(),
      onSearch: jest.fn(),
    };

    render(<RawSearchFilters {...mockProps} />);

    // Use placeholder text since labels aren't properly associated with inputs
    expect(screen.getByPlaceholderText(/search across all fields/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/readme\.md/i)).toBeInTheDocument();  // File name
    expect(screen.getByPlaceholderText(/\/docs/i)).toBeInTheDocument();  // Path/Group
    expect(screen.getByPlaceholderText(/raw-hosted/i)).toBeInTheDocument();  // Repository
  });

  it('calls onFiltersChange when input changes', async () => {
    const onFiltersChange = jest.fn();
    const mockProps = {
      filters: {},
      onFiltersChange,
      onSearch: jest.fn(),
    };

    render(<RawSearchFilters {...mockProps} />);

    const keywordInput = screen.getByPlaceholderText(/search across all fields/i);
    await userEvent.type(keywordInput, 'test');

    expect(onFiltersChange).toHaveBeenCalled();
  });

  it('calls onSearch when Enter is pressed', () => {
    const onSearch = jest.fn();
    const mockProps = {
      filters: {},
      onFiltersChange: jest.fn(),
      onSearch,
    };

    render(<RawSearchFilters {...mockProps} />);

    const keywordInput = screen.getByPlaceholderText(/search across all fields/i);
    fireEvent.keyDown(keywordInput, { key: 'Enter', code: 'Enter' });

    expect(onSearch).toHaveBeenCalled();
  });

  it('calls onSearch when Search button is clicked', async () => {
    const onSearch = jest.fn();
    const mockProps = {
      filters: {},
      onFiltersChange: jest.fn(),
      onSearch,
    };

    render(<RawSearchFilters {...mockProps} />);

    const searchButton = screen.getByRole('button', { name: /search/i });
    await userEvent.click(searchButton);

    expect(onSearch).toHaveBeenCalled();
  });
});

describe('RawSearchResults', () => {
  it('renders results list', () => {
    render(
      <RawSearchResults
        results={mockRawResults}
        onResultClick={jest.fn()}
      />
    );

    expect(screen.getByText(/5 files found/i)).toBeInTheDocument();
    expect(screen.getByText('readme.md')).toBeInTheDocument();
  });

  it('renders empty state when no results', () => {
    render(
      <RawSearchResults
        results={[]}
        onResultClick={jest.fn()}
      />
    );

    expect(screen.getByText(/no files found/i)).toBeInTheDocument();
  });

  it('renders loading state', () => {
    render(
      <RawSearchResults
        results={[]}
        loading={true}
        onResultClick={jest.fn()}
      />
    );

    expect(screen.getByText(/searching/i)).toBeInTheDocument();
  });

  it('renders error state', () => {
    render(
      <RawSearchResults
        results={[]}
        error="Search failed"
        onResultClick={jest.fn()}
      />
    );

    expect(screen.getByText(/search failed/i)).toBeInTheDocument();
  });

  it('renders results count when results exist', () => {
    render(
      <RawSearchResults
        results={mockRawResults}
        hasMore={true}
        onLoadMore={jest.fn()}
        onResultClick={jest.fn()}
      />
    );

    // RawSearchResults shows results count, load more button is in RawSearchPage
    expect(screen.getByText(/files? found/i)).toBeInTheDocument();
  });
});

describe('RawResultRow', () => {
  const mockResult = mockRawResults[0];

  it('renders result information', () => {
    render(<RawResultRow result={mockResult} />);

    expect(screen.getByText('readme.md')).toBeInTheDocument();
    expect(screen.getByText('/docs/readme.md')).toBeInTheDocument();
    expect(screen.getByText('raw-hosted')).toBeInTheDocument();
  });

  it('calls onClick when clicked', async () => {
    const onClick = jest.fn();
    render(<RawResultRow result={mockResult} onClick={onClick} />);

    const row = screen.getByRole('button', { name: /view details for readme.md/i });
    await userEvent.click(row);

    expect(onClick).toHaveBeenCalledWith(mockResult);
  });

  it('calls onClick when Enter is pressed', () => {
    const onClick = jest.fn();
    render(<RawResultRow result={mockResult} onClick={onClick} />);

    const row = screen.getByRole('button', { name: /view details for readme.md/i });
    fireEvent.keyDown(row, { key: 'Enter', code: 'Enter' });

    expect(onClick).toHaveBeenCalledWith(mockResult);
  });
});

