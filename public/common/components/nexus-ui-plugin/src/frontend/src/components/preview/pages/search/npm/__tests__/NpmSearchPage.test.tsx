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
import { NpmSearchPage } from '../NpmSearchPage';

// Mock the useNpmSearch hook
jest.mock('../useNpmSearch', () => ({
  useNpmSearch: () => ({
    state: {
      loading: false,
      error: undefined,
      results: [
        {
          id: 'npm:lodash',
          scope: '',
          name: 'lodash',
          displayName: 'lodash',
          latestVersion: '4.17.21',
          versionsCount: 100,
          description: 'Lodash modular utilities',
          lastUpdated: '2024-01-01T00:00:00Z',
        },
        {
          id: 'npm:@types/node',
          scope: '@types',
          name: 'node',
          displayName: '@types/node',
          latestVersion: '20.10.0',
          versionsCount: 500,
          description: 'TypeScript definitions for Node.js',
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

describe('NpmSearchPage', () => {
  it('renders the npm search page title', () => {
    render(<NpmSearchPage />);
    expect(screen.getByText(/npm Search/i)).toBeInTheDocument();
  });

  it('displays search results', async () => {
    render(<NpmSearchPage />);
    
    await waitFor(() => {
      expect(screen.getByText('lodash')).toBeInTheDocument();
      expect(screen.getByText('@types/node')).toBeInTheDocument();
    });
  });

  it('shows version counts in results', async () => {
    render(<NpmSearchPage />);
    
    await waitFor(() => {
      expect(screen.getByText('4.17.21')).toBeInTheDocument();
      expect(screen.getByText('20.10.0')).toBeInTheDocument();
    });
  });

  it('calls onNavigateToDetail when row is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<NpmSearchPage onNavigateToDetail={onNavigateToDetail} />);

    await waitFor(() => {
      const lodashRow = screen.getByText('lodash').closest('tr');
      if (lodashRow) {
        fireEvent.click(lodashRow);
      }
    });

    expect(onNavigateToDetail).toHaveBeenCalledWith('npm:lodash');
  });

  it('shows loading spinner when loading with no results', () => {
    // Use NpmSearchResults directly to test loading state
    const { NpmSearchResults } = require('../NpmSearchResults');
    render(
      <NpmSearchResults
        results={[]}
        loading={true}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );
    expect(screen.getByText(/searching npm packages/i)).toBeInTheDocument();
  });

  it('shows error callout when error is set', () => {
    const { NpmSearchResults } = require('../NpmSearchResults');
    render(
      <NpmSearchResults
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
    const { NpmSearchResults } = require('../NpmSearchResults');
    render(
      <NpmSearchResults
        results={[]}
        loading={false}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );
    expect(screen.getByText(/no packages found/i)).toBeInTheDocument();
  });
});


