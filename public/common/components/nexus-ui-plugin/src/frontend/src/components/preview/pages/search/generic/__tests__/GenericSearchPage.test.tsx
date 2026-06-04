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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { GenericSearchPage } from '../GenericSearchPage';
import { mockGenericResults } from '../mockData';

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
}));

jest.mock('../../../../shared/security/MalwareBanner', () => ({
  __esModule: true,
  default: () => null,
}));

jest.mock('../useGenericSearch', () => ({
  useGenericSearch: jest.fn(),
}));

import { useGenericSearch } from '../useGenericSearch';
const mockUseGenericSearch = useGenericSearch as jest.Mock;

const defaultHookReturn = {
  state: {
    filters: {},
    loading: false,
    error: undefined,
    results: mockGenericResults.slice(0, 3),
    totalCount: 3,
    continuationToken: undefined,
    sort: 'relevance' as const,
    sortDirection: 'desc' as const,
  },
  search: jest.fn(),
  loadMore: jest.fn(),
  clear: jest.fn(),
  hasMore: false,
};

const wrap = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('GenericSearchPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseGenericSearch.mockReturnValue(defaultHookReturn);
  });

  it('renders the page heading', () => {
    wrap(<GenericSearchPage />);

    expect(screen.getByRole('heading', { name: /^search$/i })).toBeInTheDocument();
  });

  it('renders search input', () => {
    wrap(<GenericSearchPage />);

    expect(screen.getByRole('button', { name: /^search$/i })).toBeInTheDocument();
  });

  it('shows loading state when loading', () => {
    mockUseGenericSearch.mockReturnValue({
      ...defaultHookReturn,
      state: {
        ...defaultHookReturn.state,
        loading: true,
        results: [],
      },
    });

    wrap(<GenericSearchPage />);

    expect(screen.getByText(/searching components/i)).toBeInTheDocument();
  });

  it('shows error state when error is set', () => {
    mockUseGenericSearch.mockReturnValue({
      ...defaultHookReturn,
      state: {
        ...defaultHookReturn.state,
        loading: false,
        results: [],
        error: 'Search failed',
      },
    });

    wrap(<GenericSearchPage />);

    expect(screen.getByText(/search failed/i)).toBeInTheDocument();
  });

  it('shows empty state when no results and not loading', () => {
    mockUseGenericSearch.mockReturnValue({
      ...defaultHookReturn,
      state: {
        ...defaultHookReturn.state,
        loading: false,
        results: [],
        totalCount: 0,
      },
    });

    wrap(<GenericSearchPage />);

    expect(screen.getByText(/no components found/i)).toBeInTheDocument();
  });

  it('calls onNavigateToDetail when a result is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    wrap(<GenericSearchPage onNavigateToDetail={onNavigateToDetail} />);

    const row = screen.getByText('commons-lang3').closest('tr');
    if (row) {
      await userEvent.click(row);
    }

    expect(onNavigateToDetail).toHaveBeenCalledWith(mockGenericResults[0].id);
  });
});
