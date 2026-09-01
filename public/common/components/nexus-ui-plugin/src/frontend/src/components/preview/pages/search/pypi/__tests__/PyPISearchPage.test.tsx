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

import { PyPISearchPage } from '../PyPISearchPage';
import { PyPISearchFilters } from '../PyPISearchFilters';
import { PyPISearchResults } from '../PyPISearchResults';
import { PyPIResultRow } from '../PyPIResultRow';
import { PyPIDetailPage } from '../PyPIDetailPage';
import { mockPyPIResults, mockPyPISearchApi } from '../mockData';
import type { PyPIResult, } from '../pypi.types';

// Mock the usePyPISearch hook
jest.mock('../usePyPISearch', () => ({
  usePyPISearch: () => ({
    state: {
      filters: {},
      loading: false,
      error: undefined,
      results: mockPyPIResults,
      totalCount: mockPyPIResults.length,
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
    mockPyPIDetailApi: jest.fn().mockResolvedValue(originalModule.mockPyPIDetail),
  };
});

describe('PyPISearchPage', () => {
  it('renders the search page with title', () => {
    render(<PyPISearchPage />);

    expect(screen.getByRole('heading', { name: /pypi search/i })).toBeInTheDocument();
    expect(screen.getByText(/search for python packages/i)).toBeInTheDocument();
  });

  it('renders search input and button', () => {
    render(<PyPISearchPage />);

    // Main search input has placeholder, not a label
    expect(screen.getByPlaceholderText(/search python packages/i)).toBeInTheDocument();
    // Search button exists
    expect(screen.getByRole('button', { name: /^search$/i })).toBeInTheDocument();
  });

  it('calls onNavigateToDetail when a result is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<PyPISearchPage onNavigateToDetail={onNavigateToDetail} />);

    // Results are rendered as table rows - click on the package name text
    const resultName = screen.getByText('requests');
    await userEvent.click(resultName);

    expect(onNavigateToDetail).toHaveBeenCalledWith('pypi:requests');
  });
});

describe('PyPISearchFilters', () => {
  const defaultProps = {
    values: { name: '', version: '', summary: '', keywords: '', classifiers: '' },
    onChange: jest.fn(),
    onSearch: jest.fn(),
    onClear: jest.fn(),
    loading: false,
  };

  it('renders the filters header', () => {
    render(<PyPISearchFilters {...defaultProps} />);

    // PyPISearchFilters shows "Additional Filters" with Show/Hide toggle
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
  });

  it('toggles advanced filters visibility', async () => {
    render(<PyPISearchFilters {...defaultProps} />);

    // Initially filters are hidden - click to show
    const showButton = screen.getByRole('button', { name: /show/i });
    await userEvent.click(showButton);

    // Now advanced filter inputs should be visible - use placeholder to find them
    expect(screen.getByPlaceholderText(/2\.31\.0/i)).toBeInTheDocument();  // Version
  });

  it('calls onChange when typing in version field', async () => {
    const onChange = jest.fn();
    render(<PyPISearchFilters {...defaultProps} onChange={onChange} />);

    // First show the filters
    const showButton = screen.getByRole('button', { name: /show/i });
    await userEvent.click(showButton);

    const versionInput = screen.getByPlaceholderText(/2\.31\.0/i);
    await userEvent.type(versionInput, '1.0.0');

    expect(onChange).toHaveBeenCalled();
  });

  it('calls onSearch when pressing Enter in version input', async () => {
    const onSearch = jest.fn();
    render(<PyPISearchFilters {...defaultProps} onSearch={onSearch} />);

    // First show the filters
    const showButton = screen.getByRole('button', { name: /show/i });
    await userEvent.click(showButton);

    const versionInput = screen.getByPlaceholderText(/2\.31\.0/i);
    fireEvent.keyDown(versionInput, { key: 'Enter', code: 'Enter' });

    expect(onSearch).toHaveBeenCalled();
  });

  it('shows Hide button when filters are expanded', async () => {
    render(<PyPISearchFilters {...defaultProps} />);

    // Click to show
    const showButton = screen.getByRole('button', { name: /show/i });
    await userEvent.click(showButton);

    // Now should say Hide
    expect(screen.getByRole('button', { name: /hide/i })).toBeInTheDocument();
  });

  it('renders with values in filters', async () => {
    render(
      <PyPISearchFilters
        {...defaultProps}
        values={{ name: '', version: '1.0.0', summary: 'test', keywords: '', classifiers: '' }}
      />
    );

    // Component renders successfully with filter values
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
  });

  it('advanced filters show correct placeholders', async () => {
    render(<PyPISearchFilters {...defaultProps} />);

    // Show filters
    const showButton = screen.getByRole('button', { name: /show/i });
    await userEvent.click(showButton);

    expect(screen.getByPlaceholderText(/2\.31\.0/i)).toBeInTheDocument();  // Version
    expect(screen.getByPlaceholderText(/short description/i)).toBeInTheDocument();  // Summary
    expect(screen.getByPlaceholderText(/http.*testing.*web/i)).toBeInTheDocument();  // Keywords
    expect(screen.getByPlaceholderText(/development status/i)).toBeInTheDocument();  // Classifiers
  });

  it('renders Additional Filters heading when loading', () => {
    render(<PyPISearchFilters {...defaultProps} loading={true} />);

    // Component still renders the header even when loading
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
  });
});

describe('PyPISearchResults', () => {
  const mockResults: PyPIResult[] = mockPyPIResults.slice(0, 3);

  it('renders loading state', () => {
    render(
      <PyPISearchResults
        results={[]}
        loading={true}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/searching pypi packages/i)).toBeInTheDocument();
  });

  it('renders error state', () => {
    render(
      <PyPISearchResults
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
      <PyPISearchResults
        results={[]}
        loading={false}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/no packages found/i)).toBeInTheDocument();
  });

  it('renders results table with correct columns', () => {
    render(
      <PyPISearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText('Package')).toBeInTheDocument();
    expect(screen.getByText('Latest Version')).toBeInTheDocument();
    expect(screen.getByText('Versions')).toBeInTheDocument();
    expect(screen.getByText('Author')).toBeInTheDocument();
    expect(screen.getByText('Last Updated')).toBeInTheDocument();
  });

  it('renders package count', () => {
    render(
      <PyPISearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={jest.fn()}
      />
    );

    // Component shows "Showing X of Y packages"
    expect(screen.getByText(/showing.*packages/i)).toBeInTheDocument();
  });

  it('calls onSelect when clicking a result row', async () => {
    const onSelect = jest.fn();
    render(
      <PyPISearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={onSelect}
      />
    );

    // Results are table rows - click on the package name text
    const firstPackageName = screen.getByText('requests');
    await userEvent.click(firstPackageName);

    expect(onSelect).toHaveBeenCalledWith('pypi:requests');
  });
});

describe('PyPIResultRow', () => {
  const mockResult: PyPIResult = mockPyPIResults[0];

  it('renders package name and summary', () => {
    render(
      <table>
        <tbody>
          <PyPIResultRow result={mockResult} onSelect={jest.fn()} />
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
          <PyPIResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.latestVersion)).toBeInTheDocument();
    expect(screen.getByText(String(mockResult.versionsCount))).toBeInTheDocument();
  });

  it('calls onSelect when clicked', async () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <PyPIResultRow result={mockResult} onSelect={onSelect} />
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
          <PyPIResultRow result={mockResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for/i });
    row.focus();
    fireEvent.keyDown(row, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith(mockResult.id);
  });
});

describe('PyPIDetailPage', () => {
  it('renders loading state initially', () => {
    render(<PyPIDetailPage packageId="pypi:requests" />);

    expect(screen.getByText(/loading package details/i)).toBeInTheDocument();
  });

  it('renders package details after loading', async () => {
    render(<PyPIDetailPage packageId="pypi:requests" />);

    await waitFor(() => {
      // Use getByRole for heading to be more specific since 'requests' appears as both title and keyword
      expect(screen.getByRole('heading', { name: 'requests', level: 1 })).toBeInTheDocument();
    });

    // Check for sections
    expect(screen.getByText(/package info/i)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /versions/i })).toBeInTheDocument();
  });

  it('calls onBack when back button is clicked', async () => {
    const onBack = jest.fn();
    render(<PyPIDetailPage packageId="pypi:requests" onBack={onBack} />);

    await waitFor(() => {
      // Use getByRole for heading to be more specific
      expect(screen.getByRole('heading', { name: 'requests', level: 1 })).toBeInTheDocument();
    });

    const backButton = screen.getByRole('button', { name: /back to search/i });
    await userEvent.click(backButton);

    expect(onBack).toHaveBeenCalled();
  });
});

describe('mockPyPISearchApi', () => {
  it('returns all results when no filters', async () => {
    const result = await mockPyPISearchApi({});
    expect(result.items.length).toBe(mockPyPIResults.length);
  });

  it('filters by name', async () => {
    const result = await mockPyPISearchApi({ name: 'flask' });
    expect(result.items.length).toBe(1);
    expect(result.items[0].name).toBe('Flask');
  });

  it('filters by version', async () => {
    const result = await mockPyPISearchApi({ version: '2.31.0' });
    expect(result.items.length).toBe(1);
    expect(result.items[0].name).toBe('requests');
  });

  it('filters by summary', async () => {
    const result = await mockPyPISearchApi({ summary: 'http' });
    expect(result.items.some(item => item.name === 'requests')).toBe(true);
  });

  it('filters by keywords', async () => {
    const result = await mockPyPISearchApi({ keywords: 'machine learning' });
    expect(result.items.some(item => item.name === 'tensorflow')).toBe(true);
  });
});

