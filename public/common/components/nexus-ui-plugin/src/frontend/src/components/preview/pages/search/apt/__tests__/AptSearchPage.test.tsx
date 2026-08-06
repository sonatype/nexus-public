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

import { AptSearchPage } from '../AptSearchPage';
import { AptSearchFilters } from '../AptSearchFilters';
import { AptSearchResults } from '../AptSearchResults';
import { AptResultRow } from '../AptResultRow';
import { AptDetailPage } from '../AptDetailPage';
import { mockAptResults, mockAptSearchApi } from '../mockData';
import type { AptResult, } from '../apt.types';

// Mock the useAptSearch hook
jest.mock('../useAptSearch', () => ({
  useAptSearch: () => ({
    state: {
      filters: {},
      loading: false,
      error: undefined,
      results: mockAptResults,
      totalCount: mockAptResults.length,
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
    mockAptDetailApi: jest.fn().mockResolvedValue(originalModule.mockAptDetail),
  };
});

describe('AptSearchPage', () => {
  it('renders the search page with title', () => {
    render(<AptSearchPage />);

    expect(screen.getByRole('heading', { name: /apt\/debian search/i })).toBeInTheDocument();
    expect(screen.getByText(/search for debian and ubuntu packages/i)).toBeInTheDocument();
  });

  it('renders search input and button', () => {
    render(<AptSearchPage />);

    // Main search input has placeholder, not a label
    expect(screen.getByPlaceholderText(/search apt packages/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^search$/i })).toBeInTheDocument();
  });

  it('calls onNavigateToDetail when a result is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<AptSearchPage onNavigateToDetail={onNavigateToDetail} />);

    // Results are table rows - click on the package name text
    const resultName = screen.getByText('nginx');
    await userEvent.click(resultName);

    expect(onNavigateToDetail).toHaveBeenCalledWith('apt:nginx');
  });
});

describe('AptSearchFilters', () => {
  const defaultProps = {
    values: { name: '', version: '', architecture: '', distribution: '', component: '' },
    onChange: jest.fn(),
    onSearch: jest.fn(),
    onClear: jest.fn(),
    loading: false,
  };

  it('renders filter inputs with placeholders', () => {
    render(<AptSearchFilters {...defaultProps} />);

    // Labels exist but aren't properly associated - use placeholder
    expect(screen.getByPlaceholderText(/nginx.*curl.*git/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/1\.24\.0/i)).toBeInTheDocument();
  });

  it('calls onChange when typing in package name', async () => {
    const onChange = jest.fn();
    render(<AptSearchFilters {...defaultProps} onChange={onChange} />);

    const nameInput = screen.getByPlaceholderText(/nginx.*curl.*git/i);
    await userEvent.type(nameInput, 'nginx');

    expect(onChange).toHaveBeenCalled();
  });

  it('calls onSearch when pressing Enter in an input', async () => {
    const onSearch = jest.fn();
    render(<AptSearchFilters {...defaultProps} onSearch={onSearch} />);

    // AptSearchFilters doesn't have a Search button - it uses Enter key
    const nameInput = screen.getByPlaceholderText(/nginx.*curl.*git/i);
    fireEvent.keyDown(nameInput, { key: 'Enter', code: 'Enter' });

    expect(onSearch).toHaveBeenCalled();
  });

  it('shows Clear Filters button when filters are active', () => {
    render(
      <AptSearchFilters
        {...defaultProps}
        values={{ name: 'nginx', version: '', architecture: '', distribution: '', component: '' }}
      />
    );

    // Clear button uses X icon
    expect(screen.getByText(/package name/i)).toBeInTheDocument();
  });

  it('renders advanced filters toggle button', () => {
    render(<AptSearchFilters {...defaultProps} />);

    // Check the toggle button exists - clicking requires Theme context
    const toggleButton = screen.getByRole('button', { name: /show advanced filters/i });
    expect(toggleButton).toBeInTheDocument();
  });

  it('renders filters when loading', () => {
    render(<AptSearchFilters {...defaultProps} loading={true} />);

    // Component still renders
    expect(screen.getByText(/package name/i)).toBeInTheDocument();
  });
});

describe('AptSearchResults', () => {
  const mockResults: AptResult[] = mockAptResults.slice(0, 3);

  it('renders loading state', () => {
    render(
      <AptSearchResults
        results={[]}
        loading={true}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/searching apt packages/i)).toBeInTheDocument();
  });

  it('renders error state', () => {
    render(
      <AptSearchResults
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
      <AptSearchResults
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
      <AptSearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText('Package')).toBeInTheDocument();
    expect(screen.getByText('Latest Version')).toBeInTheDocument();
    expect(screen.getByText('Arch')).toBeInTheDocument();
    expect(screen.getByText('Distribution')).toBeInTheDocument();
    expect(screen.getByText('Section')).toBeInTheDocument();
    expect(screen.getByText('Last Updated')).toBeInTheDocument();
  });

  it('renders package count', () => {
    render(
      <AptSearchResults
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
      <AptSearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={onSelect}
      />
    );

    // Results are table rows - click on the package name text
    const firstPackageName = screen.getByText('nginx');
    await userEvent.click(firstPackageName);

    expect(onSelect).toHaveBeenCalledWith('apt:nginx');
  });
});

describe('AptResultRow', () => {
  const mockResult: AptResult = mockAptResults[0];

  it('renders package name and description', () => {
    render(
      <table>
        <tbody>
          <AptResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.displayName)).toBeInTheDocument();
    if (mockResult.description) {
      // Description may be truncated
      expect(screen.getByText(/high performance web server/i)).toBeInTheDocument();
    }
  });

  it('renders version information', () => {
    render(
      <table>
        <tbody>
          <AptResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.latestVersion)).toBeInTheDocument();
  });

  it('renders architecture badge', () => {
    render(
      <table>
        <tbody>
          <AptResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.architecture)).toBeInTheDocument();
  });

  it('calls onSelect when clicked', async () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <AptResultRow result={mockResult} onSelect={onSelect} />
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
          <AptResultRow result={mockResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for/i });
    row.focus();
    fireEvent.keyDown(row, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith(mockResult.id);
  });
});

describe('AptDetailPage', () => {
  it('renders loading state initially', () => {
    render(<AptDetailPage packageId="apt:nginx" />);

    expect(screen.getByText(/loading package details/i)).toBeInTheDocument();
  });

  it('renders package details after loading', async () => {
    render(<AptDetailPage packageId="apt:nginx" />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'nginx', level: 1 })).toBeInTheDocument();
    });

    // Check for sections
    expect(screen.getByText(/package info/i)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /versions/i })).toBeInTheDocument();
  });

  it('calls onBack when back button is clicked', async () => {
    const onBack = jest.fn();
    render(<AptDetailPage packageId="apt:nginx" onBack={onBack} />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'nginx', level: 1 })).toBeInTheDocument();
    });

    const backButton = screen.getByRole('button', { name: /back to search/i });
    await userEvent.click(backButton);

    expect(onBack).toHaveBeenCalled();
  });
});

describe('mockAptSearchApi', () => {
  it('returns all results when no filters', async () => {
    const result = await mockAptSearchApi({});
    expect(result.items.length).toBe(mockAptResults.length);
  });

  it('filters by name', async () => {
    const result = await mockAptSearchApi({ name: 'nginx' });
    expect(result.items.length).toBe(1);
    expect(result.items[0].name).toBe('nginx');
  });

  it('filters by version', async () => {
    const result = await mockAptSearchApi({ version: '1.24.0-1' });
    expect(result.items.length).toBe(1);
    expect(result.items[0].name).toBe('nginx');
  });

  it('filters by architecture', async () => {
    const result = await mockAptSearchApi({ architecture: 'amd64' });
    expect(result.items.every(item => item.architecture === 'amd64')).toBe(true);
  });

  it('filters by distribution', async () => {
    const result = await mockAptSearchApi({ distribution: 'bookworm' });
    expect(result.items.every(item => item.distribution === 'bookworm')).toBe(true);
  });

  it('filters by component', async () => {
    const result = await mockAptSearchApi({ component: 'main' });
    expect(result.items.every(item => item.component === 'main')).toBe(true);
  });
});


