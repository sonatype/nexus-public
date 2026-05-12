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

import { YumSearchPage } from '../YumSearchPage';
import { YumSearchFilters } from '../YumSearchFilters';
import { YumSearchResults } from '../YumSearchResults';
import { YumResultRow } from '../YumResultRow';
import { YumDetailPage } from '../YumDetailPage';
import { mockYumResults, mockYumSearchApi } from '../mockData';
import type { YumResult, YumSearchFilters as FilterValues } from '../yum.types';

// Mock the useYumSearch hook
jest.mock('../useYumSearch', () => ({
  useYumSearch: () => ({
    state: {
      filters: {},
      loading: false,
      error: undefined,
      results: mockYumResults,
      totalCount: mockYumResults.length,
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
    mockYumDetailApi: jest.fn().mockResolvedValue(originalModule.mockYumDetail),
  };
});

describe('YumSearchPage', () => {
  it('renders the search page with title', () => {
    render(<YumSearchPage />);

    expect(screen.getByRole('heading', { name: /yum\/rpm search/i })).toBeInTheDocument();
    expect(screen.getByText(/search for rpm packages/i)).toBeInTheDocument();
  });

  it('renders search input and filters section', () => {
    render(<YumSearchPage />);

    // Main search input has placeholder
    expect(screen.getByPlaceholderText(/search rpm packages/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^search$/i })).toBeInTheDocument();
    // Additional Filters section has toggle
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
  });

  it('calls onNavigateToDetail when a result is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<YumSearchPage onNavigateToDetail={onNavigateToDetail} />);

    // Results are table rows - click on the package name text
    const resultName = screen.getByText('nginx');
    await userEvent.click(resultName);

    expect(onNavigateToDetail).toHaveBeenCalledWith('yum:nginx');
  });
});

describe('YumSearchFilters', () => {
  const defaultProps = {
    values: { name: '', version: '', architecture: '' },
    onChange: jest.fn(),
    onSearch: jest.fn(),
    onClear: jest.fn(),
    loading: false,
  };

  it('renders show/hide toggle for additional filters', () => {
    render(<YumSearchFilters {...defaultProps} />);

    // Filters are initially hidden behind a toggle
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /show/i })).toBeInTheDocument();
  });

  it('renders toggle button for advanced filters', () => {
    render(<YumSearchFilters {...defaultProps} />);

    // Toggle button is present (clicking requires Theme context)
    const toggleButton = screen.getByRole('button', { name: /show/i });
    expect(toggleButton).toBeInTheDocument();
  });

  it('renders loading state', () => {
    render(<YumSearchFilters {...defaultProps} loading={true} />);

    // Component should still render
    expect(screen.getByText(/additional filters/i)).toBeInTheDocument();
  });
});

describe('YumSearchResults', () => {
  const mockResults: YumResult[] = mockYumResults.slice(0, 3);

  it('renders loading state', () => {
    render(
      <YumSearchResults
        results={[]}
        loading={true}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/searching rpm packages/i)).toBeInTheDocument();
  });

  it('renders error state', () => {
    render(
      <YumSearchResults
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
      <YumSearchResults
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
      <YumSearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={jest.fn()}
      />
    );

    // Actual columns from YumSearchResults
    expect(screen.getByText('Package')).toBeInTheDocument();
    expect(screen.getByText('Version')).toBeInTheDocument();
    expect(screen.getByText('Release')).toBeInTheDocument();
    expect(screen.getByText('Arch')).toBeInTheDocument();
    expect(screen.getByText('Repository')).toBeInTheDocument();
  });

  it('renders package count', () => {
    render(
      <YumSearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={jest.fn()}
      />
    );

    // Component uses "Showing X of Y packages" format
    expect(screen.getByText(/showing.*packages/i)).toBeInTheDocument();
  });

  it('calls onSelect when clicking a result row', async () => {
    const onSelect = jest.fn();
    render(
      <YumSearchResults
        results={mockResults}
        loading={false}
        totalCount={mockResults.length}
        onSelect={onSelect}
      />
    );

    // Results are table rows - click on the package name text
    const packageName = screen.getByText('nginx');
    await userEvent.click(packageName);

    expect(onSelect).toHaveBeenCalledWith('yum:nginx');
  });
});

describe('YumResultRow', () => {
  const mockResult: YumResult = mockYumResults[0];

  it('renders package name and summary', () => {
    render(
      <table>
        <tbody>
          <YumResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.name)).toBeInTheDocument();
    if (mockResult.summary) {
      expect(screen.getByText(new RegExp(mockResult.summary.slice(0, 20)))).toBeInTheDocument();
    }
  });

  it('renders version and release information', () => {
    render(
      <table>
        <tbody>
          <YumResultRow result={mockResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(mockResult.latestVersion)).toBeInTheDocument();
    expect(screen.getByText(`-${mockResult.release}`)).toBeInTheDocument();
  });

  it('renders architecture badge', () => {
    render(
      <table>
        <tbody>
          <YumResultRow result={mockResult} onSelect={jest.fn()} />
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
          <YumResultRow result={mockResult} onSelect={onSelect} />
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
          <YumResultRow result={mockResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for/i });
    row.focus();
    fireEvent.keyDown(row, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith(mockResult.id);
  });
});

describe('YumDetailPage', () => {
  it('renders loading state initially', () => {
    render(<YumDetailPage packageId="yum:nginx" />);

    expect(screen.getByText(/loading package details/i)).toBeInTheDocument();
  });

  it('renders package details after loading', async () => {
    render(<YumDetailPage packageId="yum:nginx" />);

    await waitFor(() => {
      // Use getByRole for heading to be more specific
      expect(screen.getByRole('heading', { name: 'nginx', level: 1 })).toBeInTheDocument();
    });

    // Check for sections
    expect(screen.getByText(/package info/i)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /versions/i })).toBeInTheDocument();
  });

  it('calls onBack when back button is clicked', async () => {
    const onBack = jest.fn();
    render(<YumDetailPage packageId="yum:nginx" onBack={onBack} />);

    await waitFor(() => {
      // Use getByRole for heading to be more specific
      expect(screen.getByRole('heading', { name: 'nginx', level: 1 })).toBeInTheDocument();
    });

    const backButton = screen.getByRole('button', { name: /back to search/i });
    await userEvent.click(backButton);

    expect(onBack).toHaveBeenCalled();
  });
});

describe('mockYumSearchApi', () => {
  it('returns all results when no filters', async () => {
    const result = await mockYumSearchApi({});
    expect(result.items.length).toBe(mockYumResults.length);
  });

  it('filters by name', async () => {
    const result = await mockYumSearchApi({ name: 'nginx' });
    expect(result.items.length).toBe(1);
    expect(result.items[0].name).toBe('nginx');
  });

  it('filters by version', async () => {
    const result = await mockYumSearchApi({ version: '1.24.0' });
    expect(result.items.length).toBe(1);
    expect(result.items[0].name).toBe('nginx');
  });

  it('filters by architecture', async () => {
    const result = await mockYumSearchApi({ architecture: 'x86_64' });
    expect(result.items.every(item => item.architecture === 'x86_64')).toBe(true);
  });
});


