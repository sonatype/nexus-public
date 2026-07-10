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
import { Theme } from '@radix-ui/themes';
import { NuGetSearchPage } from '../NuGetSearchPage';
import { NuGetSearchResults } from '../NuGetSearchResults';

const mockSearch = jest.fn();
const mockLoadMore = jest.fn();
const mockClear = jest.fn();

const MOCK_RESULTS = [
  {
    id: 'nuget:Newtonsoft.Json',
    packageId: 'Newtonsoft.Json',
    displayName: 'Newtonsoft.Json',
    latestVersion: '13.0.3',
    versionsCount: 50,
    repositoriesCount: 1,
    description: 'Json.NET is a popular high-performance JSON framework for .NET',
    authors: ['James Newton-King'],
    lastUpdated: '2024-01-01T00:00:00Z',
    totalDownloads: 1500000,
  },
  {
    id: 'nuget:Serilog',
    packageId: 'Serilog',
    displayName: 'Serilog',
    latestVersion: '3.1.1',
    versionsCount: 30,
    repositoriesCount: 1,
    description: 'Simple .NET logging with fully-structured events',
    authors: ['Serilog Contributors'],
    lastUpdated: '2024-01-01T00:00:00Z',
    totalDownloads: undefined,
  },
];

jest.mock('../useNuGetSearch', () => ({
  useNuGetSearch: () => ({
    state: {
      loading: false,
      error: undefined,
      results: MOCK_RESULTS,
      totalCount: 2,
    },
    search: mockSearch,
    loadMore: mockLoadMore,
    clear: mockClear,
    hasMore: false,
  }),
}));

/**
 * Render component wrapped in Radix Theme provider.
 */
function renderWithTheme(ui: React.ReactElement) {
  return render(
    <Theme>
      {ui}
    </Theme>
  );
}

describe('NuGetSearchPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the NuGet search page title', () => {
    renderWithTheme(<NuGetSearchPage />);
    expect(screen.getByText(/NuGet Search/i)).toBeInTheDocument();
  });

  it('displays search results', async () => {
    renderWithTheme(<NuGetSearchPage />);

    await waitFor(() => {
      expect(screen.getByText('Newtonsoft.Json')).toBeInTheDocument();
      expect(screen.getByText('Serilog')).toBeInTheDocument();
    });
  });

  it('shows version information in results', async () => {
    renderWithTheme(<NuGetSearchPage />);

    await waitFor(() => {
      expect(screen.getByText('13.0.3')).toBeInTheDocument();
      expect(screen.getByText('3.1.1')).toBeInTheDocument();
    });
  });

  it('renders the Last Updated column header', async () => {
    renderWithTheme(<NuGetSearchPage />);

    await waitFor(() => {
      expect(screen.getByText('Last Updated')).toBeInTheDocument();
    });
  });

  it('displays formatted lastUpdated date for each result', async () => {
    renderWithTheme(<NuGetSearchPage />);

    await waitFor(() => {
      const formatted = new Date('2024-01-01T00:00:00Z').toLocaleDateString();
      expect(screen.getAllByText(formatted).length).toBeGreaterThanOrEqual(1);
    });
  });

  it('renders search filters', () => {
    renderWithTheme(<NuGetSearchPage />);

    const searchInput = screen.getByPlaceholderText(/Search NuGet packages/i);
    expect(searchInput).toBeInTheDocument();
  });

  it('calls onNavigateToDetail when row is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    renderWithTheme(<NuGetSearchPage onNavigateToDetail={onNavigateToDetail} />);

    await waitFor(() => {
      const newtonRow = screen.getByText('Newtonsoft.Json').closest('tr');
      if (newtonRow) {
        fireEvent.click(newtonRow);
      }
    });

    expect(onNavigateToDetail).toHaveBeenCalledWith('Newtonsoft.Json');
  });

});

describe('NuGetSearchResults', () => {
  it('shows loading spinner when loading with no results', () => {
    renderWithTheme(
      <NuGetSearchResults results={[]} loading={true} totalCount={0} onSelect={jest.fn()} />,
    );
    expect(screen.getByText(/searching nuget packages/i)).toBeInTheDocument();
  });

  it('shows error callout when error is set', () => {
    renderWithTheme(
      <NuGetSearchResults
        results={[]}
        loading={false}
        error="Search failed"
        totalCount={0}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByText(/search failed/i)).toBeInTheDocument();
  });

  it('shows empty state when no results and not loading', () => {
    renderWithTheme(
      <NuGetSearchResults results={[]} loading={false} totalCount={0} onSelect={jest.fn()} />,
    );
    expect(screen.getByText(/no packages found/i)).toBeInTheDocument();
  });

  it('renders dash for Last Updated when lastUpdated is empty string', () => {
    renderWithTheme(
      <NuGetSearchResults
        results={[{
          id: 'nuget:Pkg',
          packageId: 'Pkg',
          displayName: 'Pkg',
          latestVersion: '1.0.0',
          versionsCount: 1,
          repositoriesCount: 1,
          lastUpdated: '',
          totalDownloads: 42,
          license: 'MIT',
        }]}
        loading={false}
        totalCount={1}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders formatted date when lastUpdated is populated', () => {
    renderWithTheme(
      <NuGetSearchResults
        results={[{
          id: 'nuget:Pkg',
          packageId: 'Pkg',
          displayName: 'Pkg',
          latestVersion: '1.0.0',
          versionsCount: 1,
          repositoriesCount: 1,
          lastUpdated: '2024-06-15T10:00:00.000+00:00',
        }]}
        loading={false}
        totalCount={1}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByText(new Date('2024-06-15T10:00:00.000+00:00').toLocaleDateString())).toBeInTheDocument();
  });

  it('renders download count with K suffix', () => {
    renderWithTheme(
      <NuGetSearchResults
        results={[{
          id: 'nuget:Pkg',
          packageId: 'Pkg',
          displayName: 'Pkg',
          latestVersion: '1.0.0',
          versionsCount: 1,
          repositoriesCount: 1,
          lastUpdated: '',
          totalDownloads: 5000,
        }]}
        loading={false}
        totalCount={1}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByText('5.0K')).toBeInTheDocument();
  });

  it('renders download count with M suffix', () => {
    renderWithTheme(
      <NuGetSearchResults
        results={[{
          id: 'nuget:Pkg',
          packageId: 'Pkg',
          displayName: 'Pkg',
          latestVersion: '1.0.0',
          versionsCount: 1,
          repositoriesCount: 1,
          lastUpdated: '',
          totalDownloads: 2500000,
        }]}
        loading={false}
        totalCount={1}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByText('2.5M')).toBeInTheDocument();
  });

  it('renders download count with B suffix', () => {
    renderWithTheme(
      <NuGetSearchResults
        results={[{
          id: 'nuget:Pkg',
          packageId: 'Pkg',
          displayName: 'Pkg',
          latestVersion: '1.0.0',
          versionsCount: 1,
          repositoriesCount: 1,
          lastUpdated: '',
          totalDownloads: 1200000000,
        }]}
        loading={false}
        totalCount={1}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByText('1.2B')).toBeInTheDocument();
  });

  it('renders dash when totalDownloads is absent', () => {
    renderWithTheme(
      <NuGetSearchResults
        results={[{
          id: 'nuget:Pkg',
          packageId: 'Pkg',
          displayName: 'Pkg',
          latestVersion: '1.0.0',
          versionsCount: 1,
          repositoriesCount: 1,
          lastUpdated: '2024-01-01T00:00:00Z',
          license: 'MIT',
        }]}
        loading={false}
        totalCount={1}
        onSelect={jest.fn()}
      />,
    );
    // Only totalDownloads is absent; lastUpdated and license both have values, so exactly one '-'
    expect(screen.getAllByText('-')).toHaveLength(1);
  });

  it('calls onSelect with packageId when row is clicked', () => {
    const onSelect = jest.fn();
    renderWithTheme(
      <NuGetSearchResults
        results={[{
          id: 'nuget:Newtonsoft.Json',
          packageId: 'Newtonsoft.Json',
          displayName: 'Newtonsoft.Json',
          latestVersion: '13.0.3',
          versionsCount: 1,
          repositoriesCount: 1,
          lastUpdated: '',
        }]}
        loading={false}
        totalCount={1}
        onSelect={onSelect}
      />,
    );
    fireEvent.click(screen.getByText('Newtonsoft.Json').closest('tr')!);
    expect(onSelect).toHaveBeenCalledWith('Newtonsoft.Json');
  });
});

