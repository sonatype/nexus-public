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
import { TagDetailPage } from '../TagDetailPage';
import * as tagsApi from '../tags.api';
import { ExtJS } from '../../../../../interface/ExtJS';

// Mock UIRouter
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: () => ({ params: { tagName: 'test-tag' } }),
  useRouter: () => ({ stateService: { go: mockGo } }),
}));

// Mock useRouteVisibility - default to true (permitted)
let mockCanOpenComponent = true;
jest.mock('../../../shared', () => ({
  ...jest.requireActual('../../../shared'),
  useRouteVisibility: () => mockCanOpenComponent,
}));

// Helper to toggle the mock
const setCanOpenComponent = (value: boolean) => {
  mockCanOpenComponent = value;
};

// Mock the interface/api module
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    delete: jest.fn(),
  },
  parseApiError: (err: unknown) => ({
    message: err instanceof Error ? err.message : 'Unknown error',
  }),
  urlBuilder: {
    tags: {
      delete: (name: string) => `/service/rest/v1/tags/${encodeURIComponent(name)}`,
    },
  },
}));

import { restClient } from '../../../../../interface/api';

const mockGet = restClient.get as jest.MockedFunction<typeof restClient.get>;
const mockDelete = restClient.delete as jest.MockedFunction<typeof restClient.delete>;

// Mock APIConstants
jest.mock('../../../../../constants/APIConstants', () => ({
  APIConstants: {
    REST: {
      PUBLIC: {
        TAGS: '/service/rest/v1/tags',
      },
    },
  },
}));

// Mock tags.api fetchTagDetail
jest.mock('../tags.api');
const mockFetchTagDetail = tagsApi.fetchTagDetail as jest.MockedFunction<typeof tagsApi.fetchTagDetail>;

// Mock Toast
const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
jest.mock('../../../shared/Toast', () => ({
  useToast: () => ({
    success: mockToastSuccess,
    error: mockToastError,
    warning: jest.fn(),
    info: jest.fn(),
  }),
}));

// Mock ConfirmDialog
jest.mock('../../../shared/form', () => ({
  ConfirmDialog: ({ open, onConfirm, onOpenChange, children, title }: any) =>
    open ? (
      <div role="alertdialog">
        <span>{title}</span>
        {children}
        <button onClick={onConfirm}>Confirm Delete</button>
        <button onClick={() => onOpenChange(false)}>Cancel</button>
      </div>
    ) : null,
}));

// Mock TagDetailPage.scss
jest.mock('../TagDetailPage.scss', () => ({}), { virtual: true });

// TagDetailPage uses ExtJS.usePermission (provider-independent) rather than the
// context-based usePermission — the latter returns false without a <PermissionsProvider>,
// which coreui never mounts, hiding the action from admins too (NEXUS-54212).
// Spy on the real ExtJS statics rather than mocking the whole module: the real shared
// components rendered here rely on other ExtJS methods (e.g. waitForPermissions).
const mockUsePermission = jest.spyOn(ExtJS, 'usePermission');
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

const mockTagDetail = {
  name: 'test-tag',
  firstCreated: '2026-01-15T10:30:45Z',
  lastUpdated: '2026-02-01T14:00:00Z',
  attributes: { env: 'production' },
};

const makeComponent = (overrides = {}) => ({
  id: 'comp-1',
  repository: 'repo-1',
  format: 'npm',
  group: null,
  name: 'my-package',
  version: '1.0.0',
  assets: [{ id: 'asset-1', downloadUrl: 'http://example.com', path: '/path' }],
  ...overrides,
});

const renderPage = () =>
  render(
    <Theme>
      <TagDetailPage />
    </Theme>
  );

const mockFilteredTagsResponse = {
  items: [{ name: 'test-tag', componentCount: 58, firstCreated: null, lastUpdated: null }],
  totalCount: 1,
  continuationToken: null,
};

describe('TagDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset permission mock to default (permitted)
    setCanOpenComponent(true);
    // ExtJS.usePermission just evaluates its getter synchronously in the test.
    mockUsePermission.mockImplementation((getValue: () => unknown) => getValue());
    mockCheckPermission.mockReturnValue(true);
    mockFetchTagDetail.mockResolvedValue(mockTagDetail as any);
    // Route mockGet by URL: filtered API returns tag count, search API returns components
    mockGet.mockImplementation((url: string) => {
      if ((url as string).includes('/internal/ui/tags/filtered')) {
        return Promise.resolve(mockFilteredTagsResponse);
      }
      return Promise.resolve({ items: [makeComponent()], continuationToken: null });
    });
    mockDelete.mockResolvedValue(undefined);
  });

  describe('loading state', () => {
    it('shows loading spinner while tag detail is loading', () => {
      mockFetchTagDetail.mockImplementation(() => new Promise(() => {}));
      renderPage();
      expect(screen.getByText('Loading tag details...')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shows error message and retry button when tag fetch fails', async () => {
      mockFetchTagDetail.mockRejectedValue(new Error('Network error'));
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Failed to load tag details')).toBeInTheDocument();
      });
      expect(screen.getByText('Network error')).toBeInTheDocument();
      expect(screen.getByText('Retry')).toBeInTheDocument();
    });

    it('retries loading when retry button is clicked', async () => {
      mockFetchTagDetail
        .mockRejectedValueOnce(new Error('fail'))
        .mockResolvedValueOnce(mockTagDetail as any);
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Retry')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Retry'));

      await waitFor(() => {
        expect(mockFetchTagDetail).toHaveBeenCalledTimes(2);
      });
    });

    it('shows back button in error state', async () => {
      mockFetchTagDetail.mockRejectedValue(new Error('fail'));
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Tags')).toBeInTheDocument();
      });
    });
  });

  describe('successful render', () => {
    it('shows tag name in breadcrumb and header', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getAllByText('test-tag').length).toBeGreaterThanOrEqual(1);
      });
    });

    it('shows formatted created and last updated dates', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Created:')).toBeInTheDocument();
        expect(screen.getByText('Last Updated:')).toBeInTheDocument();
      });
    });

    it('shows Total Components badge with count from filtered tags API', async () => {
      mockGet.mockImplementation((url: string) => {
        if ((url as string).includes('/internal/ui/tags/filtered')) {
          return Promise.resolve({ items: [{ name: 'test-tag', componentCount: 58 }] });
        }
        return Promise.resolve({ items: [makeComponent()], continuationToken: null });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('58')).toBeInTheDocument();
      });
    });

    it('shows an unknown-count indicator (not the loaded page count) when the filtered API returns no match', async () => {
      mockGet.mockImplementation((url: string) => {
        if ((url as string).includes('/internal/ui/tags/filtered')) {
          return Promise.resolve({ items: [] });
        }
        return Promise.resolve({ items: [makeComponent()], continuationToken: null });
      });

      renderPage();

      // The exact count is unknown, so the badge shows "—" rather than misrepresenting
      // the tag's total as the number of components on the first loaded page.
      await waitFor(() => {
        expect(screen.getByText('—')).toBeInTheDocument();
      });
    });

    it('shows Load More button when continuation token exists', async () => {
      mockGet.mockImplementation((url: string) => {
        if ((url as string).includes('/internal/ui/tags/filtered')) {
          return Promise.resolve(mockFilteredTagsResponse);
        }
        return Promise.resolve({ items: [makeComponent()], continuationToken: 'next-page-token' });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Load More')).toBeInTheDocument();
      });
    });

    it('shows components table after loading', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('my-package')).toBeInTheDocument();
      });
    });

    it('shows "No components tagged" message when components list is empty', async () => {
      mockGet.mockResolvedValue({ items: [], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('No components tagged with this tag yet.')).toBeInTheDocument();
      });
    });
  });

  describe('back navigation', () => {
    it('navigates back to tags list when back button is clicked', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getAllByText('Tags').length).toBeGreaterThanOrEqual(1);
      });

      const backBtn = screen.getByRole('button', { name: /Tags/i });
      fireEvent.click(backBtn);
      expect(mockGo).toHaveBeenCalledWith('preview.browse.tags');
    });
  });

  describe('component search filter', () => {
    it('filters components by search text', async () => {
      const components = [
        makeComponent({ id: 'c1', name: 'alpha-lib' }),
        makeComponent({ id: 'c2', name: 'beta-lib' }),
      ];
      mockGet.mockResolvedValue({ items: components, continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('alpha-lib')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Search components...');
      fireEvent.change(searchInput, { target: { value: 'alpha' } });

      expect(screen.getByText('alpha-lib')).toBeInTheDocument();
      expect(screen.queryByText('beta-lib')).not.toBeInTheDocument();
    });

    it('shows emptyFiltered message when search matches nothing', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('my-package')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Search components...');
      fireEvent.change(searchInput, { target: { value: 'zzznomatch' } });

      expect(screen.getByText('No components match your filters.')).toBeInTheDocument();
    });
  });

  describe('format filter', () => {
    it('renders All Formats option', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getAllByText('All Formats').length).toBeGreaterThanOrEqual(1);
      });
    });
  });

  describe('sorting', () => {
    it('renders sortable column headers', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Component')).toBeInTheDocument();
        expect(screen.getByText('Format')).toBeInTheDocument();
        expect(screen.getByText('Version')).toBeInTheDocument();
        expect(screen.getByText('Repository')).toBeInTheDocument();
      });
    });

    it('toggles sort direction when same column header clicked twice', async () => {
      const components = [
        makeComponent({ id: 'c1', name: 'zebra' }),
        makeComponent({ id: 'c2', name: 'alpha' }),
      ];
      mockGet.mockResolvedValue({ items: components, continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Component')).toBeInTheDocument();
      });

      const componentHeader = screen.getByText('Component');
      fireEvent.click(componentHeader);
      fireEvent.click(componentHeader);
    });
  });

  describe('export CSV', () => {
    it('renders Export CSV button', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Export CSV')).toBeInTheDocument();
      });
    });

    it('export CSV button is disabled when no components match filters', async () => {
      mockGet.mockResolvedValue({ items: [], continuationToken: null });

      renderPage();

      await waitFor(() => {
        const exportBtn = screen.getByText('Export CSV');
        expect(exportBtn.closest('button')).toBeDisabled();
      });
    });
  });

  describe('delete-tag permission gating (NEXUS-54212)', () => {
    it('shows the Delete Tag button when the user has tags:delete', async () => {
      mockCheckPermission.mockReturnValue(true);
      renderPage();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /delete tag/i })).toBeInTheDocument();
      });
    });

    it('disables the Delete Tag button when the user lacks tags:delete', async () => {
      mockCheckPermission.mockReturnValue(false);
      renderPage();

      // Wait for the page to finish loading the tag detail.
      await waitFor(() => {
        expect(mockFetchTagDetail).toHaveBeenCalled();
      });

      // Large delete button is shown but disabled (NEXUS-54212), not hidden.
      expect(screen.getByRole('button', { name: /delete tag/i })).toBeDisabled();
    });
  });

  describe('delete tag', () => {
    it('opens delete confirmation dialog when Delete Tag is clicked', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Delete Tag')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: /delete tag/i }));

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });
    });

    it('calls DELETE API, shows success toast, and navigates back on confirm', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /delete tag/i })).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: /delete tag/i }));

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Confirm Delete'));

      await waitFor(() => {
        expect(mockDelete).toHaveBeenCalledWith(
          expect.stringContaining('test-tag')
        );
        expect(mockToastSuccess).toHaveBeenCalledWith('Tag "test-tag" deleted');
        expect(mockGo).toHaveBeenCalledWith('preview.browse.tags');
      });
    });

    it('shows error toast when delete fails', async () => {
      mockDelete.mockRejectedValue(new Error('Forbidden'));
      renderPage();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /delete tag/i })).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: /delete tag/i }));

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Confirm Delete'));

      await waitFor(() => {
        expect(mockToastError).toHaveBeenCalledWith('Forbidden');
      });

      expect(mockGo).not.toHaveBeenCalled();
    });

    it('closes delete dialog when cancel is clicked', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /delete tag/i })).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: /delete tag/i }));

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Cancel'));

      await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
      });
    });
  });

  describe('components loading error', () => {
    it('shows an error banner (not the empty state) when the search API fails', async () => {
      mockGet.mockRejectedValue(new Error('Search API error'));
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Failed to load tagged components')).toBeInTheDocument();
      });
      // The error message is surfaced, and the empty state is NOT shown — the user
      // can distinguish "search API is down" from "tag has no components".
      expect(screen.getByText('Search API error')).toBeInTheDocument();
      expect(
        screen.queryByText('No components tagged with this tag yet.')
      ).not.toBeInTheDocument();
    });
  });

  describe('format and repository filters', () => {
    it('filters by format', async () => {
      const components = [
        makeComponent({ id: 'c1', name: 'npm-pkg', format: 'npm' }),
        makeComponent({ id: 'c2', name: 'maven-pkg', format: 'maven2' }),
      ];
      mockGet.mockResolvedValue({ items: components, continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('npm-pkg')).toBeInTheDocument();
        expect(screen.getByText('maven-pkg')).toBeInTheDocument();
      });
    });

    it('filters by repository using search text matching repository field', async () => {
      const components = [
        makeComponent({ id: 'c1', name: 'pkg-1', repository: 'repo-alpha' }),
        makeComponent({ id: 'c2', name: 'pkg-2', repository: 'repo-beta' }),
      ];
      mockGet.mockResolvedValue({ items: components, continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('pkg-1')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Search components...');
      fireEvent.change(searchInput, { target: { value: 'alpha' } });

      expect(screen.getByText('pkg-1')).toBeInTheDocument();
      expect(screen.queryByText('pkg-2')).not.toBeInTheDocument();
    });

    it('clears search filter when X button is clicked', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('my-package')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Search components...');
      fireEvent.change(searchInput, { target: { value: 'something' } });

      const clearBtn = screen.getByTestId('clear-search-btn');
      expect(clearBtn).toBeInTheDocument();
      fireEvent.click(clearBtn);
      expect(searchInput).toHaveValue('');
    });
  });

  describe('column sort clicks', () => {
    beforeEach(async () => {
      const components = [
        makeComponent({ id: 'c1', name: 'alpha', format: 'npm', version: '1.0.0', repository: 'repo-a' }),
        makeComponent({ id: 'c2', name: 'beta', format: 'maven2', version: '2.0.0', repository: 'repo-b' }),
      ];
      mockGet.mockResolvedValue({ items: components, continuationToken: null });
    });

    it('sorts by format ascending when Format column is clicked (maven2 before npm)', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('alpha')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Format'));

      // Find data rows by their button role (rows have role="button" now)
      const dataRows = screen.getAllByRole('button').filter(btn => btn.getAttribute('aria-label')?.startsWith('View '));
      expect(dataRows[0]).toHaveTextContent('maven2');
      expect(dataRows[1]).toHaveTextContent('npm');

      // Sorting must not strip row interactivity: with Search access available,
      // each sorted row stays keyboard/pointer-actionable (role="button" + tabindex).
      expect(dataRows).toHaveLength(2);
      dataRows.forEach(row => {
        expect(row).toHaveAttribute('role', 'button');
        expect(row).toHaveAttribute('tabindex', '0');
      });
    });

    it('sorts by version ascending when Version column is clicked', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('alpha')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Version'));

      const dataRows = screen.getAllByRole('button').filter(btn => btn.getAttribute('aria-label')?.startsWith('View '));
      expect(dataRows[0]).toHaveTextContent('1.0.0');
      expect(dataRows[1]).toHaveTextContent('2.0.0');
    });

    it('sorts by repository ascending when Repository column is clicked', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('alpha')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Repository'));

      const dataRows = screen.getAllByRole('button').filter(btn => btn.getAttribute('aria-label')?.startsWith('View '));
      expect(dataRows[0]).toHaveTextContent('repo-a');
      expect(dataRows[1]).toHaveTextContent('repo-b');
    });

    it('toggles sort direction to descending when same column header clicked twice', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Format')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Format'));
      fireEvent.click(screen.getByText('Format'));

      // After second click: descending → npm before maven2
      const dataRows = screen.getAllByRole('button').filter(btn => btn.getAttribute('aria-label')?.startsWith('View '));
      expect(dataRows[0]).toHaveTextContent('npm');
      expect(dataRows[1]).toHaveTextContent('maven2');
    });

    it('changes sort field when different column is clicked', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Format')).toBeInTheDocument();
      });

      // Default sort is name asc; click Format to switch sort field → format asc (maven2 before npm)
      fireEvent.click(screen.getByText('Format'));
      let dataRows = screen.getAllByRole('button').filter(btn => btn.getAttribute('aria-label')?.startsWith('View '));
      expect(dataRows[0]).toHaveTextContent('maven2');
      expect(dataRows[1]).toHaveTextContent('npm');

      // Switch to version asc (1.0.0 before 2.0.0)
      fireEvent.click(screen.getByText('Version'));
      dataRows = screen.getAllByRole('button').filter(btn => btn.getAttribute('aria-label')?.startsWith('View '));
      expect(dataRows[0]).toHaveTextContent('1.0.0');
      expect(dataRows[1]).toHaveTextContent('2.0.0');
    });
  });

  describe('view component action', () => {
    it('navigates to component detail when row is clicked', async () => {
      const component = makeComponent({
        id: 'comp-1',
        format: 'npm',
        group: 'my-group',
        name: 'my-package',
        version: '1.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('my-package')).toBeInTheDocument();
      });

      // Click the row (has role="button" with aria-label)
      const row = screen.getByRole('button', { name: 'View my-package' });
      fireEvent.click(row);

      // Navigate to component detail route with gaId passed plain (format:group:name);
      // the route's `type: 'path'` param encodes/decodes it symmetrically (NEXUS-54201)
      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'my-package',
        gaId: 'npm:my-group:my-package',
        version: '1.0.0',
      });
    });

    it('navigates with a plain gaId for Alpine format with slashes in group', async () => {
      const component = makeComponent({
        id: 'comp-alpine',
        format: 'alpine',
        group: '1.0.0/main',
        name: 'acpi',
        version: '1.7-r7',
        repository: 'alpine-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('acpi')).toBeInTheDocument();
      });

      const row = screen.getByRole('button', { name: 'View acpi' });
      fireEvent.click(row);

      // gaId = 'alpine:1.0.0/main:acpi' - passed plain, not pre-encoded
      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'acpi',
        gaId: 'alpine:1.0.0/main:acpi',
        version: '1.7-r7',
      });
    });

    it('navigates when component has no group', async () => {
      const component = makeComponent({
        id: 'comp-no-group',
        format: 'npm',
        group: null,
        name: 'ungrouped-pkg',
        version: '2.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('ungrouped-pkg')).toBeInTheDocument();
      });

      const row = screen.getByRole('button', { name: 'View ungrouped-pkg' });
      fireEvent.click(row);

      // gaId = 'npm:ungrouped-pkg' - no group segment
      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'ungrouped-pkg',
        gaId: 'npm:ungrouped-pkg',
        version: '2.0.0',
      });
    });

    it('navigates when component has no version', async () => {
      const component = makeComponent({
        id: 'comp-no-version',
        format: 'npm',
        group: 'my-group',
        name: 'no-version-pkg',
        version: null,
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('no-version-pkg')).toBeInTheDocument();
      });

      const row = screen.getByRole('button', { name: 'View no-version-pkg' });
      fireEvent.click(row);

      // No version in params when version is null
      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'no-version-pkg',
        gaId: 'npm:my-group:no-version-pkg',
      });
    });

    it('navigates when component has no assets', async () => {
      // Components without assets should still navigate (they use the component route now)
      const component = makeComponent({
        id: 'c1',
        assets: [],
        format: 'npm',
        name: 'no-assets-pkg',
        version: '1.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('no-assets-pkg')).toBeInTheDocument();
      });

      const row = screen.getByRole('button', { name: 'View no-assets-pkg' });
      fireEvent.click(row);

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'no-assets-pkg',
        gaId: 'npm:no-assets-pkg',
        version: '1.0.0',
      });
    });

    it('navigates when clicking on the row', async () => {
      const component = makeComponent({
        id: 'comp-row-click',
        format: 'npm',
        name: 'row-click-pkg',
        version: '1.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('row-click-pkg')).toBeInTheDocument();
      });

      // Find the row (has role="button")
      const row = screen.getByRole('button', { name: 'View row-click-pkg' });
      fireEvent.click(row);

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'row-click-pkg',
        gaId: 'npm:row-click-pkg',
        version: '1.0.0',
      });
    });

    it('navigates when pressing Enter on the row', async () => {
      const component = makeComponent({
        id: 'comp-enter-key',
        format: 'npm',
        name: 'enter-key-pkg',
        version: '1.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('enter-key-pkg')).toBeInTheDocument();
      });

      const row = screen.getByRole('button', { name: 'View enter-key-pkg' });
      fireEvent.keyDown(row, { key: 'Enter' });

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'enter-key-pkg',
        gaId: 'npm:enter-key-pkg',
        version: '1.0.0',
      });
    });

    it('navigates when pressing Space on the row', async () => {
      const component = makeComponent({
        id: 'comp-space-key',
        format: 'npm',
        name: 'space-key-pkg',
        version: '1.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('space-key-pkg')).toBeInTheDocument();
      });

      const row = screen.getByRole('button', { name: 'View space-key-pkg' });
      fireEvent.keyDown(row, { key: ' ' });

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'space-key-pkg',
        gaId: 'npm:space-key-pkg',
        version: '1.0.0',
      });
    });

    it('clicking view button does not trigger double navigation', async () => {
      const component = makeComponent({
        id: 'comp-double',
        format: 'npm',
        name: 'double-nav-pkg',
        version: '1.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('double-nav-pkg')).toBeInTheDocument();
      });

      // Click the row directly
      const row = screen.getByRole('button', { name: 'View double-nav-pkg' });
      fireEvent.click(row);

      // Should be called exactly once
      expect(mockGo).toHaveBeenCalledTimes(1);
    });

    it('never navigates to preview.browse.search.asset route', async () => {
      const component = makeComponent({
        id: 'comp-asset-route-check',
        format: 'npm',
        name: 'asset-check-pkg',
        version: '1.0.0',
        repository: 'my-repo',
        assets: [{ id: 'asset-1', downloadUrl: 'http://example.com', path: '/p' }],
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('asset-check-pkg')).toBeInTheDocument();
      });

      const row = screen.getByRole('button', { name: 'View asset-check-pkg' });
      fireEvent.click(row);

      // Never call the old asset route
      expect(mockGo).not.toHaveBeenCalledWith('preview.browse.search.asset', expect.anything());
      // Always call the component route
      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', expect.anything());
    });
  });

  describe('export CSV', () => {
    it('triggers CSV download when Export CSV is clicked', async () => {
      const createObjectURL = jest.fn(() => 'blob:url');
      const revokeObjectURL = jest.fn();
      global.URL.createObjectURL = createObjectURL;
      global.URL.revokeObjectURL = revokeObjectURL;

      // Intercept appendChild to prevent anchor from causing issues
      const origAppendChild = document.body.appendChild.bind(document.body);
      const appendSpy = jest.spyOn(document.body, 'appendChild');
      appendSpy.mockImplementation((node: Node) => origAppendChild(node));

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('my-package')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Export CSV'));
      expect(createObjectURL).toHaveBeenCalled();

      appendSpy.mockRestore();
    });
  });

  describe('format and repository filters', () => {
    it('filters components by format', async () => {
      const components = [
        makeComponent({ id: 'c1', name: 'pkg-npm', format: 'npm' }),
        makeComponent({ id: 'c2', name: 'pkg-maven', format: 'maven2' }),
      ];
      mockGet.mockImplementation((url: string) => {
        if ((url as string).includes('/internal/ui/tags/filtered')) {
          return Promise.resolve(mockFilteredTagsResponse);
        }
        return Promise.resolve({ items: components, continuationToken: null });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('pkg-npm')).toBeInTheDocument();
        expect(screen.getByText('pkg-maven')).toBeInTheDocument();
      });

      // Select npm format — Radix Select fires onValueChange with the value
      const formatSelect = screen.getAllByRole('combobox')[0];
      fireEvent.change(formatSelect, { target: { value: 'npm' } });

      // Trigger via the internal change (simulate select value change)
      await waitFor(() => {
        expect(screen.getByText('pkg-npm')).toBeInTheDocument();
      });
    });

    it('filters components by repository', async () => {
      const components = [
        makeComponent({ id: 'c1', name: 'pkg-a', repository: 'repo-a' }),
        makeComponent({ id: 'c2', name: 'pkg-b', repository: 'repo-b' }),
      ];
      mockGet.mockImplementation((url: string) => {
        if ((url as string).includes('/internal/ui/tags/filtered')) {
          return Promise.resolve(mockFilteredTagsResponse);
        }
        return Promise.resolve({ items: components, continuationToken: null });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('pkg-a')).toBeInTheDocument();
        expect(screen.getByText('pkg-b')).toBeInTheDocument();
      });
    });
  });

  describe('load more', () => {
    it('calls the search API again when Load More is clicked', async () => {
      mockGet.mockImplementation((url: string) => {
        if ((url as string).includes('/internal/ui/tags/filtered')) {
          return Promise.resolve(mockFilteredTagsResponse);
        }
        return Promise.resolve({ items: [makeComponent()], continuationToken: 'token-abc' });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Load More')).toBeInTheDocument();
      });

      const searchCallsBefore = mockGet.mock.calls.filter(
        ([url]) => !(url as string).includes('/internal/ui/tags/filtered')
      ).length;

      fireEvent.click(screen.getByText('Load More'));

      await waitFor(() => {
        const searchCallsAfter = mockGet.mock.calls.filter(
          ([url]) => !(url as string).includes('/internal/ui/tags/filtered')
        ).length;
        expect(searchCallsAfter).toBeGreaterThan(searchCallsBefore);
      });
    });
  });

  describe('components loading error', () => {
    it('surfaces a components fetch failure as an error banner', async () => {
      mockGet.mockImplementation((url: string) => {
        if ((url as string).includes('/internal/ui/tags/filtered')) {
          return Promise.resolve(mockFilteredTagsResponse);
        }
        return Promise.reject(new Error('Search failed'));
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Failed to load tagged components')).toBeInTheDocument();
      });
      expect(screen.getByText('Search failed')).toBeInTheDocument();
      expect(
        screen.queryByText('No components tagged with this tag yet.')
      ).not.toBeInTheDocument();
    });

    it('surfaces a Load More failure inline while keeping the loaded rows', async () => {
      let searchCall = 0;
      mockGet.mockImplementation((url: string) => {
        if ((url as string).includes('/internal/ui/tags/filtered')) {
          return Promise.resolve(mockFilteredTagsResponse);
        }
        searchCall += 1;
        if (searchCall === 1) {
          return Promise.resolve({
            items: [makeComponent({ id: 'c1', name: 'first-pkg' })],
            continuationToken: 'token-abc',
          });
        }
        return Promise.reject(new Error('Load more failed'));
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Load More')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Load More'));

      // The failure is surfaced inline (not swallowed), the specific message shows,
      // and the already-loaded rows remain visible.
      await waitFor(() => {
        expect(screen.getByText('Failed to load more components')).toBeInTheDocument();
      });
      expect(screen.getByText('Load more failed')).toBeInTheDocument();
      expect(screen.getByText('first-pkg')).toBeInTheDocument();
    });
  });

  describe('permission guard (canOpenComponent=false)', () => {
    beforeEach(() => {
      setCanOpenComponent(false);
    });

    it('rows are NOT role="button" when user lacks search permission', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('my-package')).toBeInTheDocument();
      });

      // Row should NOT have role="button"
      expect(screen.queryByRole('button', { name: 'View my-package' })).not.toBeInTheDocument();
    });

    it('rows are NOT focusable (no tabIndex) when user lacks search permission', async () => {
      const component = makeComponent({ id: 'comp-1', name: 'test-pkg' });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('test-pkg')).toBeInTheDocument();
      });

      // Find the row by text content and verify it has no tabIndex
      const rowText = screen.getByText('test-pkg');
      const row = rowText.closest('tr');
      expect(row).not.toHaveAttribute('tabindex');
    });

    it('external-link icon is not rendered when user lacks search permission', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByText('my-package')).toBeInTheDocument();
      });

      // No ExternalLink icon should be present in the actions column
      expect(screen.queryByLabelText('View Details')).not.toBeInTheDocument();
    });

    it('clicking the row does NOT navigate when user lacks search permission', async () => {
      const component = makeComponent({
        id: 'comp-1',
        format: 'npm',
        name: 'no-perm-pkg',
        version: '1.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('no-perm-pkg')).toBeInTheDocument();
      });

      // Find the row (not a button) and click it
      const rowText = screen.getByText('no-perm-pkg');
      const row = rowText.closest('tr')!;
      fireEvent.click(row);

      // Should NOT have navigated
      expect(mockGo).not.toHaveBeenCalled();
    });

    it('pressing Enter on the row does NOT navigate when user lacks search permission', async () => {
      const component = makeComponent({
        id: 'comp-1',
        format: 'npm',
        name: 'enter-no-perm',
        version: '1.0.0',
        repository: 'my-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('enter-no-perm')).toBeInTheDocument();
      });

      // Find the row and press Enter
      const rowText = screen.getByText('enter-no-perm');
      const row = rowText.closest('tr')!;
      fireEvent.keyDown(row, { key: 'Enter' });

      // Should NOT have navigated
      expect(mockGo).not.toHaveBeenCalled();
    });
  });

  // canOpenComponent stays true here (default) so this isolates the name condition:
  // a component with no name cannot build a gaId, so its row must not be interactive.
  describe('malformed component name (no name to build gaId)', () => {
    it('renders the row as non-interactive and does not navigate when name is empty', async () => {
      const component = makeComponent({
        id: 'comp-empty-name',
        format: 'npm',
        name: '',
        version: '1.0.0',
        repository: 'lonely-repo',
      });
      mockGet.mockResolvedValue({ items: [component], continuationToken: null });

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('lonely-repo')).toBeInTheDocument();
      });

      // No interactive "View ..." row button and no external-link action for this row
      expect(screen.queryByRole('button', { name: /^View / })).not.toBeInTheDocument();
      expect(screen.queryByLabelText('View Details')).not.toBeInTheDocument();

      // Clicking the row must not navigate
      const row = screen.getByText('lonely-repo').closest('tr')!;
      fireEvent.click(row);
      expect(mockGo).not.toHaveBeenCalled();
    });
  });
});
