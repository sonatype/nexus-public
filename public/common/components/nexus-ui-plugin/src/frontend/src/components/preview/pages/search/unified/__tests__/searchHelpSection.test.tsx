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

/**
 * Search documentation help section (NEXUS-54433 Part A / BDD-024).
 *
 * Behavioural coverage for the help section on the unified search page: that it
 * is present and readable, that it points at the canonical Nexus search
 * documentation with a safely-configured external link, that it survives every
 * results state, and that it displaces none of the existing search controls.
 *
 * The documentation URL is compared against the page's own SEARCH_DOCS_URL
 * export — deliberately no network request is made from this suite.
 */

import React from 'react';
import { render, screen, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import UnifiedSearchPage from '../UnifiedSearchPage';
import * as useUnifiedSearchModule from '../useUnifiedSearch';
import { SEARCH_DOCS_URL } from '../SearchStrings';

jest.mock('../useUnifiedSearch');

jest.mock('../useSearchNavigation', () => ({
  useSearchNavigation: () => ({ navigateToDetail: jest.fn() }),
  SEARCH_RETURN_URL_KEY: 'nexus-search-return-url',
}));

jest.mock('../useRepositories', () => ({
  useRepositories: () => ({
    repositories: ['maven-central', 'npm-proxy'],
    availableFormats: new Set(['maven2', 'npm']),
    formatCounts: { maven2: 1, npm: 1 },
    loading: false,
    error: undefined,
  }),
}));

// Only the hook is replaced: the module's DEFAULT_SORT_FIELD /
// DEFAULT_SORT_DIRECTION constants are consumed directly by the page, and
// stubbing them away would let it call setSort(undefined, undefined) — a state
// unreachable in production.
jest.mock('../useSearchUrlState', () => ({
  ...jest.requireActual('../useSearchUrlState'),
  useSearchUrlState: () => ({
    readFromUrl: jest.fn(() => ({ format: 'all', query: '', filters: {} })),
    syncToUrl: jest.fn(),
    getShareableUrl: jest.fn(),
  }),
}));

jest.mock('../../../Welcome/dashboard/useInstanceTotals', () => ({
  useInstanceTotals: () => ({ data: null, loading: false }),
}));

const mockUseUnifiedSearch = useUnifiedSearchModule.useUnifiedSearch as jest.MockedFunction<
  typeof useUnifiedSearchModule.useUnifiedSearch
>;

const RESULTS = [
  {
    id: '1',
    name: 'lodash',
    format: 'npm',
    repository: 'npm-proxy',
    group: undefined,
    version: '4.17.21',
    lastUpdated: '2024-01-15T10:00:00Z',
  },
  {
    id: '2',
    name: 'spring-core',
    format: 'maven2',
    repository: 'maven-central',
    group: 'org.springframework',
    version: '6.1.0',
    lastUpdated: '2024-01-10T08:00:00Z',
  },
];

const mockSearch = jest.fn();

const baseState = {
  format: 'all' as const,
  query: '',
  filters: {},
  results: [],
  totalCount: 0,
  loading: false,
  error: undefined,
  continuationToken: undefined,
  sortField: 'lastUpdated' as const,
  sortDirection: 'desc' as const,
};

function mockState(overrides: Partial<typeof baseState> = {}) {
  mockUseUnifiedSearch.mockReturnValue({
    state: { ...baseState, ...overrides },
    placeholder: 'Search by component name or ID',
    setFormat: jest.fn(),
    setQuery: jest.fn(),
    setFilter: jest.fn(),
    setFilters: jest.fn(),
    search: mockSearch,
    loadMore: jest.fn(),
    reset: jest.fn(),
    hasMore: false,
    setSortField: jest.fn(),
    setSortDirection: jest.fn(),
    setSort: jest.fn(),
  } as unknown as ReturnType<typeof useUnifiedSearchModule.useUnifiedSearch>);
}

function renderPage() {
  return render(
    <Theme>
      <UnifiedSearchPage />
    </Theme>,
  );
}

/** The section's single link, located by role so its accessible name is what is matched. */
function docsLink(): HTMLAnchorElement {
  return screen.getByRole('link', { name: /search documentation/i }) as HTMLAnchorElement;
}

describe('search documentation help section', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockState();
  });

  it('renders a help section on the search page', () => {
    renderPage();
    expect(screen.getByTestId('help-section')).toBeInTheDocument();
  });

  it('invites the user to learn more about the search module', () => {
    renderPage();
    const help = screen.getByTestId('help-section');
    expect(help).toHaveTextContent(/learn more/i);
    expect(help).toHaveTextContent(/search module/i);
  });

  it('mentions search wildcards and the search syntax', () => {
    renderPage();
    const help = screen.getByTestId('help-section');
    expect(help).toHaveTextContent(/wildcards/i);
    expect(help).toHaveTextContent(/search syntax/i);
  });

  it('renders the help section inside the main search landmark', () => {
    renderPage();
    // The section must belong to the main region, not sit beside it, so screen
    // reader users reach it while navigating the page's main content.
    const main = screen.getByRole('main', { name: /page content/i });
    expect(within(main).getByTestId('help-section')).toBeInTheDocument();
    expect(within(main).getByRole('link', { name: /search documentation/i })).toBeInTheDocument();
  });

  it('tags the documentation link with its Gainsight analytics id', () => {
    renderPage();
    // Asserted on the rendered anchor itself, which is what Gainsight queries.
    expect(docsLink()).toHaveAttribute('data-analytics-id', 'nxrm-search-documentation-lnk');
  });

  it('links to the canonical Nexus search documentation', () => {
    renderPage();
    expect(docsLink()).toHaveAttribute('href', SEARCH_DOCS_URL);
  });

  it('gives the link an accessible name that describes its destination', () => {
    renderPage();
    const name = docsLink().textContent ?? '';
    // Must say where it goes; the generic placeholders are not acceptable names.
    expect(name).toMatch(/search documentation/i);
    expect(name).not.toMatch(/^\s*(click here|more|learn more|here)\s*$/i);
  });

  it('opens the documentation in a new tab', () => {
    renderPage();
    expect(docsLink()).toHaveAttribute('target', '_blank');
  });

  it('protects the new tab with both noopener and noreferrer', () => {
    renderPage();
    const rel = docsLink().getAttribute('rel') ?? '';
    expect(rel).toMatch(/\bnoopener\b/);
    expect(rel).toMatch(/\bnoreferrer\b/);
  });

  it('renders alongside populated results', () => {
    mockState({ results: RESULTS, totalCount: RESULTS.length });
    renderPage();
    // A card's name is split across elements, so locate it by its accessible name.
    expect(screen.getByRole('button', { name: /view details for lodash/i })).toBeInTheDocument();
    expect(screen.getByTestId('help-section')).toBeInTheDocument();
    expect(docsLink()).toHaveAttribute('href', SEARCH_DOCS_URL);
  });

  it('renders in the empty state', () => {
    renderPage();
    expect(screen.getByText(/no components found/i)).toBeInTheDocument();
    // Singular query: only this render is mounted, so it cannot be satisfied by
    // a help section belonging to some other render.
    expect(screen.getByTestId('help-section')).toBeInTheDocument();
  });

  it('renders in the loading state', () => {
    mockState({ loading: true });
    renderPage();
    expect(screen.getByRole('main', { name: /page content/i })).toBeInTheDocument();
    expect(screen.getByTestId('help-section')).toBeInTheDocument();
  });

  it('does not displace the filter or sort controls', () => {
    mockState({ results: RESULTS, totalCount: RESULTS.length });
    renderPage();
    // Sidebar filters remain reachable...
    expect(screen.getByRole('complementary', { name: /filter bar/i })).toBeInTheDocument();
    expect(screen.getAllByText('Format').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Repository').length).toBeGreaterThan(0);
    // ...as does the results-header sort control, and the result cards.
    expect(screen.getAllByRole('combobox', { name: /^sort:/i }).length).toBeGreaterThan(0);
    expect(
      screen.getByRole('button', { name: /view details for spring-core/i }),
    ).toBeInTheDocument();
  });
});
