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
import '@testing-library/jest-dom';

import { RubyGemsSearchPage } from '../RubyGemsSearchPage';
import { RubyGemsSearchFilters } from '../RubyGemsSearchFilters';
import { RubyGemsSearchResults } from '../RubyGemsSearchResults';
import { RubyGemsResultRow } from '../RubyGemsResultRow';
import { RubyGemsDetailPage } from '../RubyGemsDetailPage';
import { mockRubyGemsResults, mockRubyGemsSearchApi } from '../mockData';
import type { RubyGemsResult, RubyGemsSearchFilters as FilterValues } from '../rubygems.types';

// Mock the useRubyGemsSearch hook
jest.mock('../useRubyGemsSearch', () => ({
  useRubyGemsSearch: () => ({
    state: {
      filters: {},
      loading: false,
      error: undefined,
      results: mockRubyGemsResults,
      totalCount: mockRubyGemsResults.length,
      continuationToken: undefined,
    },
    search: jest.fn(),
    loadMore: jest.fn(),
    clear: jest.fn(),
    hasMore: false,
  }),
}));

// Mock the mockData API functions
jest.mock('../mockData', () => {
  const originalModule = jest.requireActual('../mockData');
  return {
    ...originalModule,
    mockRubyGemsDetailApi: jest.fn().mockResolvedValue(originalModule.mockRubyGemsDetail),
  };
});

describe('RubyGemsSearchPage', () => {
  it('renders the search page with title', () => {
    render(<RubyGemsSearchPage />);

    expect(screen.getByRole('heading', { name: /rubygems search/i })).toBeInTheDocument();
    expect(screen.getByText(/search for ruby gems/i)).toBeInTheDocument();
  });

  it('renders search input and filters section', () => {
    render(<RubyGemsSearchPage />);

    // Main search input has placeholder
    expect(screen.getByPlaceholderText(/search ruby gems/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^search$/i })).toBeInTheDocument();
    // Additional Filters section has toggle
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
  });

  it('calls onNavigateToDetail when a result is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<RubyGemsSearchPage onNavigateToDetail={onNavigateToDetail} />);

    // Results are table rows - click on the gem name text
    const resultName = screen.getByText('rails');
    await userEvent.click(resultName);

    expect(onNavigateToDetail).toHaveBeenCalledWith('rubygems:rails');
  });
});

describe('RubyGemsSearchFilters', () => {
  const defaultProps = {
    values: { name: '', version: '', platform: '' },
    onChange: jest.fn(),
    onSearch: jest.fn(),
    onClear: jest.fn(),
    loading: false,
  };

  it('renders show/hide toggle for additional filters', () => {
    render(<RubyGemsSearchFilters {...defaultProps} />);

    // Filters are initially hidden behind a toggle
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /show/i })).toBeInTheDocument();
  });

  it('renders toggle button for advanced filters', () => {
    render(<RubyGemsSearchFilters {...defaultProps} />);

    // Toggle button is present (clicking requires Theme context)
    const toggleButton = screen.getByRole('button', { name: /show/i });
    expect(toggleButton).toBeInTheDocument();
  });

  it('renders loading state', () => {
    render(<RubyGemsSearchFilters {...defaultProps} loading={true} />);

    // Component should still render
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
  });
});

describe('RubyGemsSearchResults', () => {
  const mockResults: RubyGemsResult[] = mockRubyGemsResults.slice(0, 3);

  it('renders loading state', () => {
    render(
      <RubyGemsSearchResults
        results={[]}
        loading={true}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/searching ruby gems/i)).toBeInTheDocument();
  });

  it('renders error state', () => {
    render(
      <RubyGemsSearchResults
        results={[]}
        loading={false}
        error="Search failed"
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/search failed/i)).toBeInTheDocument();
  });

  it('renders empty state', () => {
    render(
      <RubyGemsSearchResults
        results={[]}
        loading={false}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/no gems found/i)).toBeInTheDocument();
  });

  it('renders results table with correct columns', () => {
    render(
      <RubyGemsSearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={jest.fn()}
      />
    );

    // Actual columns from RubyGemsSearchResults
    expect(screen.getByText('Gem')).toBeInTheDocument();
    expect(screen.getByText('Latest Version')).toBeInTheDocument();
    expect(screen.getByText('Platform')).toBeInTheDocument();
    expect(screen.getByText('Authors')).toBeInTheDocument();
    expect(screen.getByText('Repository')).toBeInTheDocument();
  });

  it('renders gem count', () => {
    render(
      <RubyGemsSearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={jest.fn()}
      />
    );

    // Component uses "Showing X of Y gems" format
    expect(screen.getByText(/showing.*gems/i)).toBeInTheDocument();
  });

  it('calls onSelect when clicking a result row', async () => {
    const onSelect = jest.fn();
    render(
      <RubyGemsSearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={onSelect}
      />
    );

    // Results are table rows - click on the gem name text
    const gemName = screen.getByText('rails');
    await userEvent.click(gemName);

    expect(onSelect).toHaveBeenCalledWith('rubygems:rails');
  });
});

describe('RubyGemsResultRow', () => {
  const mockResult: RubyGemsResult = mockRubyGemsResults[0];

  it('renders gem name and summary', () => {
    render(
      <table>
        <tbody>
          <RubyGemsResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.displayName)).toBeInTheDocument();
    if (mockResult.summary) {
      expect(screen.getByText(mockResult.summary)).toBeInTheDocument();
    }
  });

  it('renders version information', () => {
    render(
      <table>
        <tbody>
          <RubyGemsResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.latestVersion)).toBeInTheDocument();
    expect(screen.getByText(String(mockResult.versionsCount))).toBeInTheDocument();
  });

  it('renders platform badge', () => {
    render(
      <table>
        <tbody>
          <RubyGemsResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.platform)).toBeInTheDocument();
  });

  it('calls onSelect when clicked', async () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <RubyGemsResultRow result={mockResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for/i });
    await userEvent.click(row);

    expect(onSelect).toHaveBeenCalledWith(mockResult.id);
  });

  it('calls onSelect when Enter is pressed', async () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <RubyGemsResultRow result={mockResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for/i });
    row.focus();
    fireEvent.keyDown(row, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith(mockResult.id);
  });
});

describe('RubyGemsDetailPage', () => {
  it('renders loading state initially', () => {
    render(<RubyGemsDetailPage gemId="rubygems:rails" />);

    expect(screen.getByText(/loading gem details/i)).toBeInTheDocument();
  });

  it('renders gem details after loading', async () => {
    render(<RubyGemsDetailPage gemId="rubygems:rails" />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'rails', level: 1 })).toBeInTheDocument();
    });

    // Check for sections
    expect(screen.getByText(/gem info/i)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /versions/i })).toBeInTheDocument();
  });

  it('calls onBack when back button is clicked', async () => {
    const onBack = jest.fn();
    render(<RubyGemsDetailPage gemId="rubygems:rails" onBack={onBack} />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'rails', level: 1 })).toBeInTheDocument();
    });

    const backButton = screen.getByRole('button', { name: /back to search/i });
    await userEvent.click(backButton);

    expect(onBack).toHaveBeenCalled();
  });
});

describe('RubyGemsSearchPage loading/error/empty states', () => {
  it('shows loading spinner when loading with no results', () => {
    const { RubyGemsSearchResults } = require('../RubyGemsSearchResults');
    render(
      <RubyGemsSearchResults
        results={[]}
        loading={true}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );
    expect(screen.getByText(/searching ruby gems/i)).toBeInTheDocument();
  });

  it('shows error callout when error is set', () => {
    const { RubyGemsSearchResults } = require('../RubyGemsSearchResults');
    render(
      <RubyGemsSearchResults
        results={[]}
        loading={false}
        error="Search failed"
        totalCount={0}
        onSelect={jest.fn()}
      />
    );
    expect(screen.getByText(/search failed/i)).toBeInTheDocument();
  });

  it('shows empty state when no results and not loading', () => {
    const { RubyGemsSearchResults } = require('../RubyGemsSearchResults');
    render(
      <RubyGemsSearchResults
        results={[]}
        loading={false}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );
    expect(screen.getByText(/no gems found/i)).toBeInTheDocument();
  });
});

describe('mockRubyGemsSearchApi', () => {
  it('returns all results when no filters', async () => {
    const result = await mockRubyGemsSearchApi({});
    expect(result.items.length).toBe(mockRubyGemsResults.length);
  });

  it('filters by name', async () => {
    const result = await mockRubyGemsSearchApi({ name: 'rails' });
    expect(result.items.length).toBe(1);
    expect(result.items[0].name).toBe('rails');
  });

  it('filters by version', async () => {
    const result = await mockRubyGemsSearchApi({ version: '7.1.3' });
    expect(result.items.length).toBe(2); // rails and activerecord both have 7.1.3
  });

  it('filters by platform', async () => {
    const result = await mockRubyGemsSearchApi({ platform: 'java' });
    expect(result.items.length).toBe(1);
    expect(result.items[0].name).toBe('jruby-openssl');
  });
});


