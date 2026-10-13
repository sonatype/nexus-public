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
 * Files tab pagination reset (NEXUS-54560).
 *
 * The tab used to carry `useEffect(() => setCurrentPage(1), [])`, which runs once on mount when
 * `currentPage` is already 1 — a no-op. The behaviour it was reaching for is the acceptance
 * criterion asserted here: after the selected version changes, the Files list starts on page 1.
 *
 * Both versions used below are deliberately multi-page. A version whose page count *shrinks*
 * cannot distinguish a real reset from out-of-range clamping, and page 3 clamped to page 2 is not
 * page 1. (GAFilesTab does not clamp at all — it slices blindly — so an unreset page renders an
 * empty table body, but the test is written not to depend on that.)
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { GADetailPage } from '../GADetailPage';
import { GAFilesTab } from '../GAFilesTab';
import { mockDetail, mockVersions } from '../mockData';
import type { GAAsset } from '../../core';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

// ─── Router / hook mocks, mirroring GADetailPage.test.tsx ────────────────────

const mockGo = jest.fn();
const mockRouterState = { currentName: 'preview.browse.search.component.files' };
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: { go: mockGo },
    globals: {
      get current() {
        return { name: mockRouterState.currentName };
      },
      params: {},
    },
  }),
  useCurrentStateAndParams: () => ({
    state: { name: mockRouterState.currentName },
    params: {},
  }),
}));

jest.mock('../useGADetail', () => ({
  useGADetail: jest.fn(),
}));

jest.mock('../useComponentVersions', () => ({
  useComponentVersions: jest.fn(),
}));

jest.mock('../useComponentSecurity', () => ({
  useComponentSecurity: jest.fn(() => ({
    data: null,
    error: null,
    iqConnected: true,
    refetch: jest.fn(),
  })),
}));

jest.mock('../useGARepositoriesForVersion', () => ({
  useGARepositoriesForVersion: jest.fn(() => ({
    rows: [],
    totalCount: 0,
    loading: false,
    error: null,
    refresh: jest.fn(),
  })),
}));

jest.mock('../../core', () => ({
  GA_SEARCH_ROUTE_NAMES: {
    MAVEN: 'preview.browse.search.maven',
    MAVEN_DETAIL: 'preview.browse.search.component',
    MAVEN_DETAIL_OVERVIEW: 'preview.browse.search.component.overview',
    MAVEN_DETAIL_VERSIONS: 'preview.browse.search.component.versions',
    MAVEN_DETAIL_REPOS: 'preview.browse.search.component.repos',
    MAVEN_DETAIL_FILES: 'preview.browse.search.component.files',
    MAVEN_DETAIL_SECURITY: 'preview.browse.search.component.security',
  },
  TAB_ROUTE_MAP: {
    overview: 'preview.browse.search.component.overview',
    versions: 'preview.browse.search.component.versions',
    repositories: 'preview.browse.search.component.repos',
    files: 'preview.browse.search.component.files',
    security: 'preview.browse.search.component.security',
  },
  getTabFromRoute: jest.fn((route: string) => {
    if (route.includes('versions')) return 'versions';
    if (route.includes('repos')) return 'repositories';
    if (route.includes('files')) return 'files';
    if (route.includes('security')) return 'security';
    return 'overview';
  }),
  getFormatLabel: jest.fn(() => 'Maven'),
  parseGaId: jest.fn((id: string) => decodeURIComponent(id)),
}));

import { useGADetail } from '../useGADetail';
import { useComponentVersions } from '../useComponentVersions';

const mockUseGADetail = useGADetail as jest.MockedFunction<typeof useGADetail>;
const mockUseComponentVersions = useComponentVersions as jest.MockedFunction<
  typeof useComponentVersions
>;

// ─── Fixtures ───────────────────────────────────────────────────────────────

/**
 * `count` assets whose filenames carry both an ordinal and the version, so a row identifies which
 * page of which version is on screen. Page size is 20, so 45 assets is three pages.
 */
function makeAssets(version: string, count: number): readonly GAAsset[] {
  return Array.from({ length: count }, (_, i) => {
    const n = String(i + 1).padStart(3, '0');
    const fileName = `file-${n}-v${version}.jar`;
    return {
      id: `${version}-${n}`,
      repository: 'repo-a',
      path: `com/example/lib/${version}/${fileName}`,
      downloadUrl: `http://localhost/repository/repo-a/${fileName}`,
      format: 'jar',
      extension: i % 2 === 0 ? 'jar' : 'pom',
      size: 1024 + i,
      contentType: 'application/java-archive',
      lastModified: '2024-01-15T10:00:00Z',
      checksums: { sha1: 'a', sha256: 'b', md5: 'c' },
    };
  });
}

const V1 = '1.0.0';
const V2 = '2.0.0';
const v1Assets = makeAssets(V1, 45);
const v2Assets = makeAssets(V2, 45);

// Default sort is path/desc, so file-045 heads page 1 and file-001 tails the last page.
const firstRowOf = (version: string) => `file-045-v${version}.jar`;
const lastRowOf = (version: string) => `file-001-v${version}.jar`;

/** The pagination bar's accessible name is the only place the current page is exposed. */
const expectPage = (page: number, totalPages: number) =>
  expect(
    screen.getByRole('navigation', { name: `Pagination, page ${page} of ${totalPages}` }),
  ).toBeInTheDocument();

const gaDetailState = (selectedVersion: string | null, assets: readonly GAAsset[]) => ({
  detail: { ...mockDetail, repositories: [], versions: [] },
  selectedVersion,
  assets,
  versionRepositories: [],
  versionLastUpdated: null,
  assetsLoading: false,
  selectVersion: jest.fn(),
});

const versionsState = () => ({
  versions: mockVersions,
  total: mockVersions.length,
  newestVersion: mockVersions[0].version,
  totalVersions: mockVersions.length,
  totalPages: 1,
  currentPage: 1,
  itemsPerPage: 20,
  sortKey: 'version' as const,
  sortDirection: 'desc' as const,
  searchQuery: '',
  error: null,
  onPageChange: jest.fn(),
  onItemsPerPageChange: jest.fn(),
  onSortChange: jest.fn(),
  onSearchQueryChange: jest.fn(),
  retry: jest.fn(),
});

beforeEach(() => {
  jest.clearAllMocks();
  mockRouterState.currentName = 'preview.browse.search.component.files';
  mockUseComponentVersions.mockReturnValue(versionsState());
  // TablePagination scrolls to the top on every page change; jsdom has no implementation.
  window.scrollTo = jest.fn();
});

// ─── Component Detail boundary ──────────────────────────────────────────────

describe('Files tab pagination across a selected-version change (Component Detail)', () => {
  const gaId = 'maven%3Acom.example%3Alib';

  /**
   * Lifecycle evidence for the fix choice, and the reason the mount-only effect looked harmless.
   * Choosing a version in the Versions tab and returning to Files *does* remount this component
   * (Radix unmounts inactive Tabs.Content), which resets the page for free. This test covers the
   * path that does not: `version` is a `dynamic` route param (previewBrowseRoutes), so a
   * URL-originated change while Files is already active — deep link, hand-edited hash,
   * Back/Forward — does not re-enter the state, GADetailPage is not remounted, and GAFilesTab
   * (rendered with no `key`) keeps its local state. The sort selection is that local state: a
   * remount would restore the default path/descending.
   */
  it('keeps the Files tab mounted across a selected-version change', async () => {
    mockUseGADetail.mockReturnValue(gaDetailState(V1, v1Assets));
    const { rerender } = renderWithTheme(<GADetailPage gaId={gaId} version={V1} />);

    await userEvent.click(screen.getByRole('button', { name: 'Size' }));
    expect(screen.getByRole('columnheader', { name: /Size/ })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );

    mockUseGADetail.mockReturnValue(gaDetailState(V2, v2Assets));
    rerender(
      <Theme>
        <GADetailPage gaId={gaId} version={V2} />
      </Theme>,
    );

    // Same instance: the sort the user chose survived the version change.
    expect(screen.getByRole('columnheader', { name: /Size/ })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );
  });

  it('starts the newly selected version on page 1', async () => {
    mockUseGADetail.mockReturnValue(gaDetailState(V1, v1Assets));
    const { rerender } = renderWithTheme(<GADetailPage gaId={gaId} version={V1} />);

    await userEvent.click(screen.getByRole('button', { name: '3' }));
    expectPage(3, 3);
    expect(screen.getByText(lastRowOf(V1))).toBeInTheDocument();

    mockUseGADetail.mockReturnValue(gaDetailState(V2, v2Assets));
    rerender(
      <Theme>
        <GADetailPage gaId={gaId} version={V2} />
      </Theme>,
    );

    expectPage(1, 3);
    expect(screen.getByText(firstRowOf(V2))).toBeInTheDocument();
    expect(screen.queryByText(lastRowOf(V2))).not.toBeInTheDocument();
  });
});

// ─── GAFilesTab directly ────────────────────────────────────────────────────

describe('GAFilesTab pagination reset', () => {
  const rerenderTab = (
    rerender: (ui: React.ReactElement) => void,
    version: string,
    assets: readonly GAAsset[],
  ) =>
    rerender(
      <Theme>
        <GAFilesTab assets={assets} selectedVersion={version} loading={false} />
      </Theme>,
    );

  it('starts on page 1 on the first render', () => {
    renderWithTheme(<GAFilesTab assets={v1Assets} selectedVersion={V1} loading={false} />);
    expectPage(1, 3);
    expect(screen.getByText(firstRowOf(V1))).toBeInTheDocument();
  });

  it('resets to page 1 when the selected version changes to another multi-page version', async () => {
    const { rerender } = renderWithTheme(
      <GAFilesTab assets={v1Assets} selectedVersion={V1} loading={false} />,
    );

    await userEvent.click(screen.getByRole('button', { name: '2' }));
    expectPage(2, 3);

    rerenderTab(rerender, V2, v2Assets);

    expectPage(1, 3);
    expect(screen.getByText(firstRowOf(V2))).toBeInTheDocument();
  });

  // A version with fewer pages must land on page 1, not on an empty page and not merely clamped
  // to the new last page.
  it('starts a version with fewer pages on page 1 rather than an empty or clamped page', async () => {
    const { rerender } = renderWithTheme(
      <GAFilesTab assets={v1Assets} selectedVersion={V1} loading={false} />,
    );
    await userEvent.click(screen.getByRole('button', { name: '3' }));
    expectPage(3, 3);

    const shortAssets = makeAssets(V2, 5);
    rerenderTab(rerender, V2, shortAssets);

    expectPage(1, 1);
    expect(screen.getByText(firstRowOf(V2).replace('045', '005'))).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(shortAssets.length + 1); // + header row
  });

  // Same logical version, new array instance: a refetch of the version already on screen must not
  // yank the user back to page 1.
  it('preserves the current page when the same version refreshes with a new assets array', async () => {
    const { rerender } = renderWithTheme(
      <GAFilesTab assets={v1Assets} selectedVersion={V1} loading={false} />,
    );
    await userEvent.click(screen.getByRole('button', { name: '2' }));
    expectPage(2, 3);

    rerenderTab(rerender, V1, [...v1Assets]);

    expectPage(2, 3);
  });

  it('resets to page 1 when a column sort is applied', async () => {
    renderWithTheme(<GAFilesTab assets={v1Assets} selectedVersion={V1} loading={false} />);
    await userEvent.click(screen.getByRole('button', { name: '2' }));
    expectPage(2, 3);

    await userEvent.click(screen.getByRole('button', { name: 'Size' }));

    expectPage(1, 3);
    // Ascending by size puts the smallest — file-001 — first, so the page is neither stale nor empty.
    expect(screen.getByText(lastRowOf(V1))).toBeInTheDocument();
  });

  it('resets to page 1 when the filter text changes', async () => {
    renderWithTheme(<GAFilesTab assets={v1Assets} selectedVersion={V1} loading={false} />);
    await userEvent.click(screen.getByRole('button', { name: '2' }));
    expectPage(2, 3);

    await userEvent.type(screen.getByPlaceholderText('Filter'), 'pom');

    // The fixture's `extension` field alternates jar/pom independently of the filename, which is
    // always `.jar`, so 'pom' matches 22 assets via the extension field alone (GAFilesTab matches
    // the query against filename, extension and repository separately). 22 rows is two pages, and
    // the row asserted below is the head of the filtered list under the default path/desc sort —
    // the highest odd-indexed file, whose name still ends in `.jar`.
    expectPage(1, 2);
    expect(screen.getByText('file-044-v1.0.0.jar')).toBeInTheDocument();
  });

  it('resets to page 1 when an extension filter is toggled', async () => {
    renderWithTheme(<GAFilesTab assets={v1Assets} selectedVersion={V1} loading={false} />);
    await userEvent.click(screen.getByRole('button', { name: '2' }));
    expectPage(2, 3);

    // Sidebar Type checkboxes, in extensionOptions order: jar, pom.
    const [jarCheckbox] = screen.getAllByRole('checkbox');
    await userEvent.click(jarCheckbox);

    expectPage(1, 2);
    expect(screen.getByText(firstRowOf(V1))).toBeInTheDocument();
  });

  it('starts a newly selected version on page 1 under StrictMode', async () => {
    const { rerender } = render(
      <React.StrictMode>
        <Theme>
          <GAFilesTab assets={v1Assets} selectedVersion={V1} loading={false} />
        </Theme>
      </React.StrictMode>,
    );

    await userEvent.click(screen.getByRole('button', { name: '2' }));
    expectPage(2, 3);

    rerender(
      <React.StrictMode>
        <Theme>
          <GAFilesTab assets={v2Assets} selectedVersion={V2} loading={false} />
        </Theme>
      </React.StrictMode>,
    );

    expectPage(1, 3);
    expect(screen.getByText(firstRowOf(V2))).toBeInTheDocument();
  });
});
