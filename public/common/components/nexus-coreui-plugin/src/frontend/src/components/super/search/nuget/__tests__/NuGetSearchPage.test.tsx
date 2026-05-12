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

// Mock the useNuGetSearch hook
jest.mock('../useNuGetSearch', () => ({
  useNuGetSearch: () => ({
    state: {
      loading: false,
      error: undefined,
      results: [
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
        },
      ],
      totalCount: 2,
    },
    search: jest.fn(),
    loadMore: jest.fn(),
    clear: jest.fn(),
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

  it('renders search filters', () => {
    renderWithTheme(<NuGetSearchPage />);
    
    // Check for main search input - it's a textbox with the search placeholder
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

