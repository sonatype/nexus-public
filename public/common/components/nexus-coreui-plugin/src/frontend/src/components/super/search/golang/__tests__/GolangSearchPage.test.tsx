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
import { GolangSearchPage } from '../GolangSearchPage';

// Mock the useGolangSearch hook
jest.mock('../useGolangSearch', () => ({
  useGolangSearch: () => ({
    state: {
      loading: false,
      error: undefined,
      results: [
        {
          id: 'go:github.com/gin-gonic/gin',
          module: 'github.com/gin-gonic/gin',
          latestVersion: 'v1.9.1',
          versionCount: 45,
          description: 'Gin is a HTTP web framework written in Go',
          license: 'MIT',
          lastUpdated: '2024-01-15T00:00:00Z',
        },
        {
          id: 'go:github.com/gorilla/mux',
          module: 'github.com/gorilla/mux',
          latestVersion: 'v1.8.1',
          versionCount: 32,
          description: 'A powerful HTTP router and URL matcher',
          license: 'BSD-3-Clause',
          lastUpdated: '2024-01-10T00:00:00Z',
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

describe('GolangSearchPage', () => {
  it('renders the Go search page title', () => {
    render(<GolangSearchPage />);
    expect(screen.getByText(/Go Search/i)).toBeInTheDocument();
  });

  it('displays search results', async () => {
    render(<GolangSearchPage />);

    await waitFor(() => {
      expect(screen.getByText('github.com/gin-gonic/gin')).toBeInTheDocument();
      expect(screen.getByText('github.com/gorilla/mux')).toBeInTheDocument();
    });
  });

  it('shows version information in results', async () => {
    render(<GolangSearchPage />);

    await waitFor(() => {
      expect(screen.getByText('v1.9.1')).toBeInTheDocument();
      expect(screen.getByText('v1.8.1')).toBeInTheDocument();
    });
  });

  it('renders search filters', async () => {
    render(<GolangSearchPage />);

    expect(screen.getByText('Additional Filters')).toBeInTheDocument();
    const toggle = screen.getByRole('button', { name: /show/i });
    fireEvent.click(toggle);

    await waitFor(() => {
      expect(screen.getByText('Version')).toBeInTheDocument();
      expect(screen.getByText('Keyword')).toBeInTheDocument();
    });
  });

  it('calls onNavigateToDetail when row is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<GolangSearchPage onNavigateToDetail={onNavigateToDetail} />);

    await waitFor(() => {
      const ginRow = screen.getByText('github.com/gin-gonic/gin').closest('tr');
      if (ginRow) {
        fireEvent.click(ginRow);
      }
    });

    expect(onNavigateToDetail).toHaveBeenCalledWith('go:github.com/gin-gonic/gin');
  });

  it('displays module count', async () => {
    render(<GolangSearchPage />);

    await waitFor(() => {
      expect(screen.getByText(/Showing 2 of 2 modules/i)).toBeInTheDocument();
    });
  });
});


