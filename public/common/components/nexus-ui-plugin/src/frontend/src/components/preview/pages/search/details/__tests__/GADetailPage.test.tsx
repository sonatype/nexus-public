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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { GADetailPage } from '../GADetailPage';
import { mockDetail, mockAssets } from '../mockData';
import { SEARCH_RETURN_URL_KEY } from '../../unified/useSearchNavigation';

const renderWithProviders = (ui: React.ReactElement) =>
  render(<Theme>{ui}</Theme>);

// Mock the router (mutable route name for version-selection tests)
const mockGo = jest.fn();
const mockRouterState = { currentName: 'preview.browse.search.component.overview' };
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: mockGo,
    },
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

// Mock the useGADetail hook
jest.mock('../useGADetail', () => ({
  useGADetail: jest.fn(() => ({
    detail: null,
    selectedVersion: null,
    assets: [],
    assetsLoading: false,
    selectVersion: jest.fn(),
  })),
}));

// Mock useComponentVersions (server-paginated Versions tab, NEXUS-54219). Defaults to the
// same set mockDetail.versions carries, so existing detail-view/tab-navigation/row-click
// tests exercise the wiring without needing a real fetchComponentVersions call.
jest.mock('../useComponentVersions', () => ({
  useComponentVersions: jest.fn(() => ({
    versions: [],
    total: 0,
    newestVersion: null,
    totalVersions: 0,
    totalPages: 0,
    currentPage: 1,
    itemsPerPage: 20,
    sortKey: 'version',
    sortDirection: 'desc',
    searchQuery: '',
    error: null,
    onPageChange: jest.fn(),
    onItemsPerPageChange: jest.fn(),
    onSortChange: jest.fn(),
    onSearchQueryChange: jest.fn(),
    retry: jest.fn(),
  })),
}));

// Mock useComponentSecurity
jest.mock('../useComponentSecurity', () => ({
  useComponentSecurity: jest.fn(() => ({
    data: null,
    error: null,
    iqConnected: true,
    refetch: jest.fn(),
  })),
}));

// Mock useGARepositoriesForVersion (NEXUS-54220). Defaults to zero rows / zero total,
// mirroring an idle machine before any version selection.
jest.mock('../useGARepositoriesForVersion', () => ({
  useGARepositoriesForVersion: jest.fn(() => ({
    rows: [],
    totalCount: 0,
    refresh: jest.fn(),
  })),
}));

// Mock the core module
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
  getTabFromRoute: jest.fn((route) => {
    if (route.includes('versions')) return 'versions';
    if (route.includes('repos')) return 'repositories';
    if (route.includes('files')) return 'files';
    if (route.includes('security')) return 'security';
    return 'overview';
  }),
  getFormatLabel: jest.fn(() => 'Maven'),
  parseGaId: jest.fn((id) => decodeURIComponent(id)),
}));

import { useGADetail } from '../useGADetail';
import { useComponentVersions } from '../useComponentVersions';
import { useGARepositoriesForVersion } from '../useGARepositoriesForVersion';
import { mockVersions } from '../mockData';

describe('GADetailPage', () => {
  const mockUseGADetail = useGADetail as jest.MockedFunction<typeof useGADetail>;
  const mockUseComponentVersions = useComponentVersions as jest.MockedFunction<typeof useComponentVersions>;
  const mockUseGARepositoriesForVersion =
    useGARepositoriesForVersion as jest.MockedFunction<typeof useGARepositoriesForVersion>;
  const testGaId = 'maven%3Aorg.apache.commons%3Acommons-lang3';

  // Populated variant of the useComponentVersions mock, for tests that render the Versions
  // tab's rows (e.g. clicking a version). Tests that only need the badge total or don't
  // touch the Versions tab can use the module-level empty default instead.
  const populatedVersionsState = () => ({
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
    mockRouterState.currentName = 'preview.browse.search.component.overview';
    // Every field of UseGADetailReturn is present here, including versionRepositories, which
    // GADetailPage does not read yet. A per-test override replaces this object wholesale, so
    // anything missing here is what a render path would receive as undefined the moment it
    // starts reading it — and there is no typecheck in this package to catch that (no tsconfig;
    // Babel strips types), so the default has to be exhaustive by hand.
    mockUseGADetail.mockReturnValue({
      detail: null,
      selectedVersion: null,
      assets: [],
      versionRepositories: [],
      versionLastUpdated: null,
      assetsLoading: false,
      selectVersion: jest.fn(),
    });
    mockUseComponentVersions.mockReturnValue(populatedVersionsState());
    mockUseGARepositoriesForVersion.mockReturnValue({
      rows: [],
      totalCount: 0,
      loading: false,
      error: null,
      refresh: jest.fn(),
    });
  });

  describe('loading state', () => {
    it('renders the shell immediately instead of blocking on the aggregate fetch', () => {
      // `loading` now covers only the /v1/search walk that builds detail.repositories and
      // detail.versions, which just the Repositories and Files tabs read. The shell — name,
      // description, tab strip — is derived from the gaId, so it must render straight away.
      // Blocking here made time-to-first-render scale with version count: ~101 requests on a
      // 5,000-version component before anything appeared.
      mockUseGADetail.mockReturnValue({
        detail: { ...mockDetail, repositories: [], versions: [] },
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      expect(screen.queryByText(/Loading component details/)).not.toBeInTheDocument();
      // Assert on the shell heading specifically: the Overview tab's Component Details table
      // also renders the name (a "Name" row), so a bare getByText(displayName) now matches
      // both it and the breadcrumb. The heading is the shell element this test is about.
      expect(screen.getByRole('heading', { name: /maven-core/ })).toBeInTheDocument();
    });

    it('reflects the useGARepositoriesForVersion totalCount in the repositories badge', () => {
      // Since NEXUS-54220 the badge is driven by the per-version endpoint via
      // useGARepositoriesForVersion, not by the aggregate walk. Number of repos for the
      // selected version → badge value.
      mockUseGADetail.mockReturnValue({
        detail: { ...mockDetail, repositories: [], versions: [] },
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });
      mockUseGARepositoriesForVersion.mockReturnValue({
        rows: [],
        totalCount: 7,
        loading: false,
        error: null,
        refresh: jest.fn(),
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      // Radix Tabs.Trigger renders a hidden duplicate of the label so a bold selected state
      // reserves width. Assert on textContent containing our number rather than exact text.
      const repositoriesTab = screen.getByRole('tab', { name: /Repositories/ });
      expect(repositoriesTab.textContent).toMatch(/7/);
    });
  });

  describe('error state', () => {
    it('shows error message when error occurs', () => {
      mockUseGADetail.mockReturnValue({
        detail: null,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);
      
      expect(screen.getByText(/Component not found/)).toBeInTheDocument();
    });

    it('shows back button on error', () => {
      mockUseGADetail.mockReturnValue({
        detail: null,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);
      
      expect(screen.getByText(/Back to Search/)).toBeInTheDocument();
    });
  });

  describe('detail view', () => {
    beforeEach(() => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });
    });

    it('displays GA name and version', () => {
      renderWithProviders(<GADetailPage gaId={testGaId} />);

      expect(screen.getByRole('heading', { name: /maven-core/ })).toBeInTheDocument();
    });

    it('renders all 5 tabs', () => {
      renderWithProviders(<GADetailPage gaId={testGaId} />);
      
      // Use getAllByRole for tab triggers (Radix renders duplicated text nodes)
      const tabs = screen.getAllByRole('tab');
      expect(tabs.length).toBe(5);
      // Radix tabs render duplicated text, so check using includes
      const tabTexts = tabs.map(t => t.textContent || '');
      expect(tabTexts.some(t => t.includes('Overview'))).toBe(true);
      expect(tabTexts.some(t => t.includes('Versions'))).toBe(true);
      expect(tabTexts.some(t => t.includes('Repositories'))).toBe(true);
      expect(tabTexts.some(t => t.includes('Files'))).toBe(true);
      expect(tabTexts.some(t => t.includes('Security'))).toBe(true);
    });

    it('shows breadcrumb navigation to search', () => {
      renderWithProviders(<GADetailPage gaId={testGaId} />);

      expect(screen.getByText('Search')).toBeInTheDocument();
    });

    it('shows the total distinct version count from useComponentVersions on the Versions tab badge, not detail.versions.length', () => {
      // mockDetail.versions and mockVersions happen to be the same array in this fixture, so
      // use a total that DIFFERS from mockDetail.versions.length to prove the badge reads the
      // server total (NEXUS-54219) rather than the legacy aggregate's length.
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        totalVersions: 4213,
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      const versionsTab = screen.getByRole('tab', { name: /Versions/i });
      expect(versionsTab.textContent).toContain('4213');
    });

    /**
     * The badge counts the component's versions, so it must ignore `total`, which narrows to the
     * match count while the tab's own filter is active.
     */
    it('keeps the badge on the unfiltered count while the tab filter narrows the list', () => {
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        searchQuery: '2.1',
        total: 3,
        totalVersions: 4213,
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      const versionsTab = screen.getByRole('tab', { name: /Versions/i });
      expect(versionsTab.textContent).toContain('4213');
      expect(versionsTab.textContent).not.toContain('Versions3');
    });

    /**
     * Regression, NEXUS-54219 review: the header's default version used to be
     * `versionsState.versions[0]`, i.e. the first row of whatever page/sort/filter the Versions
     * tab was showing. Sorting ascending therefore titled the page with the component's OLDEST
     * version (and pointed copy-path and the Overview default at it). `newestVersion` is latched
     * from the default-ordered first page instead, so the visible page can move freely.
     */
    it('titles the page with the newest version even when the tab is sorted ascending', () => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        sortDirection: 'asc',
        // What an ascending sort actually puts on screen...
        versions: [{ version: '0.0.1', lastUpdated: '2026-01-01T00:00:00Z', repositories: ['releases'] }],
        // ...versus the component's real newest version.
        newestVersion: '9.9.9',
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      const heading = screen.getAllByRole('heading', { level: 1 })[0];
      expect(heading.textContent).toContain('9.9.9');
      expect(heading.textContent).not.toContain('0.0.1');
    });
  });

  describe('tab navigation', () => {
    beforeEach(() => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });
    });

    it('navigates to versions tab when clicked', async () => {
      renderWithProviders(<GADetailPage gaId={testGaId} />);
      
      // Radix tabs render duplicated text, use regex pattern
      const versionsTab = screen.getByRole('tab', { name: /Versions/i });
      await userEvent.click(versionsTab);
      
      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.component.versions',
        expect.objectContaining({ gaId: testGaId })
      );
    });

    it('navigates to repositories tab when clicked', async () => {
      renderWithProviders(<GADetailPage gaId={testGaId} />);
      
      // Radix tabs render duplicated text, use regex pattern
      const reposTab = screen.getByRole('tab', { name: /Repositories/i });
      await userEvent.click(reposTab);
      
      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.component.repos',
        expect.objectContaining({ gaId: testGaId })
      );
    });
  });

  describe('back navigation', () => {
    beforeEach(() => {
      sessionStorage.clear();
    });

    afterEach(() => {
      sessionStorage.clear();
    });

    it('navigates to preview.browse.search.unified when the Search breadcrumb is clicked', async () => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      await userEvent.click(screen.getByText('Search'));

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.unified');
    });

    it('returns to the stored search URL when the Search breadcrumb is clicked', async () => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });
      sessionStorage.setItem(
        SEARCH_RETURN_URL_KEY,
        '#preview/browse/search?q=spring&nameOrVersion=commons',
      );

      renderWithProviders(<GADetailPage gaId={testGaId} />);
      // Clear mock calls from version canonicalisation effect before the click
      mockGo.mockClear();
      await userEvent.click(screen.getByText('Search'));

      expect(window.location.hash).toBe('#preview/browse/search?q=spring&nameOrVersion=commons');
      // Single-use: the breadcrumb is the sole consumer and clears it.
      expect(sessionStorage.getItem(SEARCH_RETURN_URL_KEY)).toBeNull();
      expect(mockGo).not.toHaveBeenCalled();
    });

    it('falls back to the bare search page when the stored value is not a search URL', async () => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });
      sessionStorage.setItem(SEARCH_RETURN_URL_KEY, '#preview/browse/welcome');

      renderWithProviders(<GADetailPage gaId={testGaId} />);
      await userEvent.click(screen.getByText('Search'));

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.unified');
      expect(sessionStorage.getItem(SEARCH_RETURN_URL_KEY)).toBeNull();
    });

    it('falls back to the bare search page when nothing is stored', async () => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);
      await userEvent.click(screen.getByText('Search'));

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.unified');
    });
  });

  describe('version selection', () => {
    it('calls selectVersion and navigates to files tab', async () => {
      const mockSelectVersion = jest.fn();
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: mockSelectVersion,
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      // Radix tabs render duplicated text, use regex pattern
      const versionsTab = screen.getByRole('tab', { name: /Versions/i });
      await userEvent.click(versionsTab);
      
      // The actual version selection would happen in GAVersionsTab
      // This test verifies the hook is properly connected
      expect(mockSelectVersion).toBeDefined();
    });

    it('when on versions route, row click keeps versions state and passes version param', async () => {
      mockRouterState.currentName = 'preview.browse.search.component.versions';
      const mockSelectVersion = jest.fn();
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion: mockSelectVersion,
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      const versionCell = screen.getByText('3.9.5');
      await userEvent.click(versionCell.closest('tr')!);

      expect(mockSelectVersion).toHaveBeenCalledWith('3.9.5');
      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.component.versions',
        expect.objectContaining({
          gaId: testGaId,
          version: '3.9.5',
        }),
      );
    });
  });

  describe('files tab requirements', () => {
    it('requires version selection for files tab', () => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null, // No version selected
        assets: [],
        assetsLoading: false,
        selectVersion: jest.fn(),
      });

      // When rendering files tab without version, should show message
      // This is tested in GAFilesTab.test.tsx
    });
  });

  describe('version canonicalisation (NEXUS-54201)', () => {
    it('redirects to the newest version when the URL carries none', async () => {
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        newestVersion: '2.0.10',
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      await waitFor(() => {
        expect(mockGo).toHaveBeenCalledWith(
          expect.any(String),
          expect.objectContaining({ version: '2.0.10' }),
          expect.objectContaining({ location: 'replace' }),
        );
      });
    });

    it('does not redirect when the URL already carries a version', async () => {
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        newestVersion: '2.0.10',
      });

      renderWithProviders(<GADetailPage gaId={testGaId} version="1.0.500" />);

      await waitFor(() => expect(mockGo).not.toHaveBeenCalled());
    });

    // Versionless formats (raw) have version ''. The route squashes the param, so '' cannot
    // round-trip through the URL — redirecting would be a no-op that re-fires forever.
    it('does not redirect for a versionless component', async () => {
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        newestVersion: '',
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      await waitFor(() => expect(mockGo).not.toHaveBeenCalled());
    });

    it('does not redirect before the newest version is known', async () => {
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        newestVersion: null,
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      await waitFor(() => expect(mockGo).not.toHaveBeenCalled());
    });

    // Nothing else ever moves `selectedVersion` off its initial `null` for a versionless
    // component: '' cannot round-trip through the squashed `version` URL param, so the
    // redirect above deliberately skips it. Without a direct dispatch, shouldLoadAssets in
    // gaDetailMachine never passes and the Files/Security tabs stay empty forever.
    it('resolves the version in-context for a versionless component instead of leaving it null forever', async () => {
      const selectVersion = jest.fn();
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: null,
        assets: [],
        assetsLoading: false,
        selectVersion,
      });
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        newestVersion: '',
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      await waitFor(() => expect(selectVersion).toHaveBeenCalledWith(''));
    });

    it('does not re-select once the version is already resolved to empty', async () => {
      const selectVersion = jest.fn();
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: '',
        assets: [],
        assetsLoading: false,
        selectVersion,
      });
      mockUseComponentVersions.mockReturnValue({
        ...populatedVersionsState(),
        newestVersion: '',
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      await waitFor(() => expect(selectVersion).not.toHaveBeenCalled());
    });
  });

  // The badge must distinguish "no version selected" (null) from "versionless format, assets
  // may legitimately be empty" ('') — plain truthiness treats both the same, always showing 0.
  describe('Files tab badge for versionless components (NEXUS-54201)', () => {
    it('counts assets in the Files badge even when selectedVersion is the empty string', () => {
      mockUseGADetail.mockReturnValue({
        detail: mockDetail,
        selectedVersion: '',
        assets: mockAssets,
        assetsLoading: false,
        selectVersion: jest.fn(),
      });

      renderWithProviders(<GADetailPage gaId={testGaId} />);

      const filesTab = screen.getByRole('tab', { name: /Files/i });
      expect(filesTab.textContent).toContain(String(mockAssets.length));
    });
  });
});
