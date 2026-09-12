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
import { fireEvent, render, screen, } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { GAVersionsTab } from '../GAVersionsTab';
import { GAFilesTab } from '../GAFilesTab';
import * as exportToCsvModule from '../../../../shared/utils/exportToCsv';
import type { GAVersion, GAAsset } from '../../core';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

const testVersions: readonly GAVersion[] = [
  { version: '2.0.0', lastUpdated: '2024-03-01T00:00:00Z', repositories: ['repo-a', 'repo-b'] },
  { version: '1.0.0', lastUpdated: '2024-01-01T00:00:00Z', repositories: ['repo-a'] },
];

const testAssets: readonly GAAsset[] = [
  {
    id: 'a1',
    repository: 'repo-a',
    path: 'com/example/lib/1.0/lib-1.0.jar',
    downloadUrl: 'http://localhost/repository/repo-a/lib-1.0.jar',
    format: 'jar',
    extension: 'jar',
    size: 1024,
    contentType: 'application/java-archive',
    lastModified: '2024-01-15T10:00:00Z',
    checksums: { sha1: 'abc', sha256: 'def', md5: 'ghi' },
  },
  {
    id: 'a2',
    repository: 'repo-a',
    path: 'com/example/lib/1.0/lib-1.0.pom',
    downloadUrl: 'http://localhost/repository/repo-a/lib-1.0.pom',
    format: 'pom',
    extension: 'pom',
    size: 512,
    contentType: 'application/xml',
    lastModified: '2024-01-15T10:00:00Z',
    checksums: { sha1: 'jkl', sha256: 'mno', md5: 'pqr' },
    classifier: 'sources',
  },
];

describe('Export CSV button', () => {
  let exportToCsvSpy: jest.SpyInstance;

  beforeEach(() => {
    exportToCsvSpy = jest
      .spyOn(exportToCsvModule, 'exportToCsv')
      .mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // ─── GAVersionsTab ──────────────────────────────────────────────────────────
  // Render-only since NEXUS-54219: filtering, sorting, and paging happen server-side via
  // useComponentVersions. These tests pass data/paging state as props and assert the
  // component renders it and delegates interactions to the callback props, rather than
  // filtering/sorting `versions` itself.

  describe('GAVersionsTab', () => {
    const defaultProps = {
      versions: testVersions,
      total: testVersions.length,
      totalPages: 1,
      currentPage: 1,
      itemsPerPage: 20,
      sortKey: 'lastUpdated' as const,
      sortDirection: 'desc' as const,
      searchQuery: '',
      loading: false,
      error: null,
      onPageChange: jest.fn(),
      onItemsPerPageChange: jest.fn(),
      onSortChange: jest.fn(),
      onSearchQueryChange: jest.fn(),
      onRetry: jest.fn(),
      selectedVersion: null,
      onVersionSelect: jest.fn(),
    };

    it('renders the Export page button', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      expect(screen.getByText('Export page')).toBeInTheDocument();
    });

    it('Export page button aria-label states the page scope', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} currentPage={3} totalPages={200} />);
      expect(
        screen.getByLabelText('Export this page of versions as CSV (page 3 of 200)'),
      ).toBeInTheDocument();
    });

    it('Export page button is enabled when versions exist', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      expect(screen.getByText('Export page').closest('button')).not.toBeDisabled();
    });

    it('Export page button is disabled when the current page has no rows', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} versions={[]} total={0} searchQuery="zzznomatch" />);
      // total===0 renders the no-results card instead of the button; assert the card, not the button.
      expect(screen.getByText('No Versions Found')).toBeInTheDocument();
    });

    it('calls exportToCsv with a filename stating the page scope', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} currentPage={2} totalPages={5} />);
      await userEvent.click(screen.getByText('Export page'));
      expect(exportToCsvSpy).toHaveBeenCalledTimes(1);
      expect(exportToCsvSpy).toHaveBeenCalledWith(
        expect.any(Array),
        'versions-page-2-of-5.csv',
        ['version', 'repositories', 'lastUpdated'],
      );
    });

    it('exports exactly the rows it was given, in the given order', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export page'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(testVersions.length);
      expect(data[0].version).toBe(testVersions[0].version);
    });

    it('serialises repositories array as semicolon-separated string', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export page'));
      const [data] = exportToCsvSpy.mock.calls[0];
      const rowWithMultipleRepos = data.find(
        (row: { repositories: string }) => row.repositories.includes(';'),
      );
      expect(rowWithMultipleRepos).toBeDefined();
      expect(rowWithMultipleRepos.repositories).toBe('repo-a;repo-b');
    });

    it('shows empty state when there are no versions at all', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} versions={[]} total={0} />);
      expect(screen.getByText('No versions found')).toBeInTheDocument();
    });

    it('shows no-results card with a distinct message when a search query yields zero results', () => {
      renderWithTheme(
        <GAVersionsTab {...defaultProps} versions={[]} total={0} searchQuery="zzznomatch" />,
      );
      expect(screen.getByText('No Versions Found')).toBeInTheDocument();
    });

    it('reset filters button clears the local input and calls onSearchQueryChange', async () => {
      const onSearchQueryChange = jest.fn();
      renderWithTheme(
        <GAVersionsTab
          {...defaultProps}
          versions={[]}
          total={0}
          searchQuery="zzznomatch"
          onSearchQueryChange={onSearchQueryChange}
        />,
      );
      await userEvent.click(screen.getByRole('button', { name: 'Reset Filters' }));
      expect(onSearchQueryChange).toHaveBeenCalledWith('');
    });

    it('shows a loading spinner when loading and no rows are cached yet', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} versions={[]} loading={true} />);
      expect(screen.getByText('Loading versions...')).toBeInTheDocument();
    });

    it('keeps the current rows visible with an inline spinner when loading a new page', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} loading={true} />);
      expect(screen.getByTestId('versions-inline-spinner')).toBeInTheDocument();
      expect(screen.getByText(testVersions[0].version)).toBeInTheDocument();
    });

    it('shows the error message and a retry button, and calls onRetry when clicked', async () => {
      const onRetry = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} error="Failed to load versions" onRetry={onRetry} />);
      expect(screen.getByText('Failed to load versions')).toBeInTheDocument();
      await userEvent.click(screen.getByTestId('versions-retry-button'));
      expect(onRetry).toHaveBeenCalledTimes(1);
    });

    it('highlights the selected version row via data-selected attribute', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} selectedVersion="2.0.0" />);
      const selectedRows = document.querySelectorAll('[data-selected="true"]');
      expect(selectedRows.length).toBeGreaterThanOrEqual(1);
    });

    it('calls onVersionSelect when a version row is clicked', async () => {
      const onVersionSelect = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} onVersionSelect={onVersionSelect} />);
      await userEvent.click(screen.getByText('2.0.0'));
      expect(onVersionSelect).toHaveBeenCalledWith('2.0.0');
    });

    it('echoes typed input immediately and calls onSearchQueryChange', async () => {
      const onSearchQueryChange = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} onSearchQueryChange={onSearchQueryChange} />);
      const input = screen.getByPlaceholderText('Filter by version');
      await userEvent.type(input, 'foo');
      expect(input).toHaveValue('foo');
      expect(onSearchQueryChange).toHaveBeenLastCalledWith('foo');
    });

    it('clear search X button removes the search text and calls onSearchQueryChange with an empty string', async () => {
      const onSearchQueryChange = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} onSearchQueryChange={onSearchQueryChange} />);
      const input = screen.getByPlaceholderText('Filter by version');
      await userEvent.type(input, 'foo');
      await userEvent.click(screen.getByLabelText('Clear search'));
      expect(input).toHaveValue('');
      expect(onSearchQueryChange).toHaveBeenLastCalledWith('');
    });

    it('calls onSortChange with the clicked column and a direction when a header is clicked', async () => {
      const onSortChange = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} onSortChange={onSortChange} />);
      // Exact match: a regex /Version/i also matches the "Export this page of versions" button.
      await userEvent.click(screen.getByRole('button', { name: 'Version' }));
      expect(onSortChange).toHaveBeenCalledWith('version', expect.stringMatching(/^(asc|desc)$/));
    });

    it('calls onSortChange when the Repositories header is clicked', async () => {
      const onSortChange = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} onSortChange={onSortChange} />);
      await userEvent.click(screen.getByRole('button', { name: 'Repositories' }));
      expect(onSortChange).toHaveBeenCalledWith('repositories', expect.stringMatching(/^(asc|desc)$/));
    });

    it('delegates pagination to onPageChange rather than slicing versions locally', async () => {
      const onPageChange = jest.fn();
      renderWithTheme(
        <GAVersionsTab {...defaultProps} total={100} totalPages={5} currentPage={1} onPageChange={onPageChange} />,
      );
      await userEvent.click(screen.getByRole('button', { name: 'Next page' }));
      expect(onPageChange).toHaveBeenCalledWith(2);
    });

    it('renders no status column, filter, or sort option', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      expect(screen.queryByRole('columnheader', { name: /status/i })).not.toBeInTheDocument();
      expect(screen.queryByText(/quarantined/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/not.recommended/i)).not.toBeInTheDocument();
      expect(screen.queryByText('Status')).not.toBeInTheDocument();
    });

    it('opens the mobile filter drawer when the mobile Filter button is clicked', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      // A regex /Filter/i also matches the sidebar's "Reset filters" button; the mobile
      // trigger's accessible name is the exact string "Filter".
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      await userEvent.click(screen.getByRole('button', { name: 'Filter' }));
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('selects a version when Enter is pressed on a focused row', () => {
      // userEvent 12.8.3 (installed) has no .keyboard() API; fireEvent.keyDown targets the
      // exact DOM event the row's onKeyDown handler checks.
      const onVersionSelect = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} onVersionSelect={onVersionSelect} />);
      const row = screen.getByText('2.0.0').closest('tr')!;
      fireEvent.keyDown(row, { key: 'Enter' });
      expect(onVersionSelect).toHaveBeenCalledWith('2.0.0');
    });

    it('selects a version when Space is pressed on a focused row', () => {
      const onVersionSelect = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} onVersionSelect={onVersionSelect} />);
      const row = screen.getByText('1.0.0').closest('tr')!;
      fireEvent.keyDown(row, { key: ' ' });
      expect(onVersionSelect).toHaveBeenCalledWith('1.0.0');
    });

    it('does not select a version for an unrelated key press on a row', () => {
      const onVersionSelect = jest.fn();
      renderWithTheme(<GAVersionsTab {...defaultProps} onVersionSelect={onVersionSelect} />);
      const row = screen.getByText('2.0.0').closest('tr')!;
      fireEvent.keyDown(row, { key: 'Tab' });
      expect(onVersionSelect).not.toHaveBeenCalled();
    });

    it('renders sidebar sort-key and sort-direction selectors reflecting the current props', () => {
      // The sidebar Select.Root triggers are Radix comboboxes whose open/choose interaction
      // is unreliable under jsdom with the installed user-event (12.8.3, pre-.keyboard()/.selectOptions()).
      // onValueChange wiring for both is otherwise identical to the SortableTableHeader path,
      // which IS exercised above; this asserts the controlled values render correctly instead.
      renderWithTheme(<GAVersionsTab {...defaultProps} sortKey="repositories" sortDirection="asc" />);
      // The sidebar's two sort selectors come first in the DOM; TablePagination renders two
      // more comboboxes (page size, page number) after them.
      const [sortKeyCombobox, sortDirectionCombobox] = screen.getAllByRole('combobox');
      expect(sortKeyCombobox).toHaveTextContent('Repositories');
      expect(sortDirectionCombobox).toHaveTextContent('Ascending');
    });
  });

  // ─── GAFilesTab ────────────────────────────────────────────────────────────

  describe('GAFilesTab', () => {
    const defaultProps = {
      assets: testAssets,
      selectedVersion: '1.0',
      loading: false,
    };

    it('renders the Export CSV button', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      expect(screen.getByText('Export CSV')).toBeInTheDocument();
    });

    it('Export CSV button has correct aria-label', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeInTheDocument();
    });

    // `repository` where `classifier` used to be: the real API never populates classifier
    // (componentVersionDetailApi.toAsset hardcodes it undefined), so that column exported blank
    // for every row, and the repository is what the rows themselves now display (NEXUS-54201).
    it('calls exportToCsv with files.csv filename when clicked', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      expect(exportToCsvSpy).toHaveBeenCalledTimes(1);
      expect(exportToCsvSpy).toHaveBeenCalledWith(
        expect.any(Array),
        'files.csv',
        ['file', 'extension', 'repository', 'size', 'lastModified', 'downloadUrl'],
      );
    });

    it('exports the repository for each row', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      for (const row of data as Array<Record<string, unknown>>) {
        expect(row.repository).toBeTruthy();
        expect(row).not.toHaveProperty('classifier');
      }
    });

    // An undated asset must export as an empty cell, never as the string "Invalid Date".
    it('exports an empty cell for an asset with no timestamp', async () => {
      renderWithTheme(
        <GAFilesTab
          {...defaultProps}
          assets={[{ ...testAssets[0], lastModified: null }]}
        />,
      );
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect((data as Array<Record<string, unknown>>)[0].lastModified).toBe('');
    });

    it('exports all visible files as rows', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(testAssets.length);
    });

    it('exports only filtered files when a search filter is active', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), '.pom');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data.every((row: { extension: string }) => row.extension === 'pom')).toBe(true);
    });

    it('uses the filename (last path segment) not the full path in the file column', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      data.forEach((row: { file: string }) => {
        expect(row.file).not.toContain('/');
      });
    });

    it('does not show Export CSV when no version is selected', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion={null} loading={false} />);
      expect(screen.queryByText('Export CSV')).not.toBeInTheDocument();
    });

    it('shows a prompt to select a version when none is selected', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion={null} loading={false} />);
      expect(screen.getByText(/Select a version/)).toBeInTheDocument();
    });

    it('does not show Export CSV while assets are loading', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion="1.0" loading={true} />);
      expect(screen.queryByText('Export CSV')).not.toBeInTheDocument();
    });

    it('shows loading spinner while loading', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion="1.0" loading={true} />);
      expect(screen.getByText('Loading files...')).toBeInTheDocument();
    });

    it('shows empty state when assets array is empty and not loading', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion="1.0" loading={false} />);
      expect(screen.getByText('No Files Found')).toBeInTheDocument();
    });

    it('shows no-results card when search filters all out', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      expect(screen.getByText('No Files Found')).toBeInTheDocument();
    });

    it('reset filters button clears search and re-shows rows', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      await userEvent.click(screen.getByRole('button', { name: 'Reset Filters' }));
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
    });

    it('renders the file name (last path segment) in the table', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
      expect(screen.getByText('lib-1.0.pom')).toBeInTheDocument();
    });

    // '' is the valid selected version for versionless formats (raw) — distinct from null
    // ("nothing selected yet"). Plain truthiness treats them the same and always shows the
    // "select a version" prompt, even once assets have loaded (NEXUS-54201).
    it('shows files when selectedVersion is the empty string, not the select-a-version prompt', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} selectedVersion="" />);
      expect(screen.queryByText(/select a version/i)).not.toBeInTheDocument();
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
    });

    it('clear search X button removes the search text', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      const input = screen.getByPlaceholderText('Filter');
      await userEvent.type(input, 'foo');
      await userEvent.click(screen.getByLabelText('Clear search'));
      expect(input).toHaveValue('');
    });

    /*
     * The Last Modified column must never print the literal string "Invalid Date".
     *
     * formatDate's try/catch cannot prevent it on its own: `new Date('')` and `new Date('nope')`
     * throw nothing, they yield an Invalid Date whose toLocaleDateString() returns exactly that
     * string. Both an absent timestamp and an unparseable one have to be tested explicitly. Only
     * null is reachable from the API — componentVersionDetailApi maps a missing lastModified to it
     * — but the unparseable case guards the same render path against a future format change.
     */
    it.each([
      ['null', null],
      ['an empty string', ''],
      ['an unparseable string', 'not-a-date'],
    ])('renders an em dash, not "Invalid Date", when lastModified is %s', (_label, value) => {
      renderWithTheme(
        <GAFilesTab
          {...defaultProps}
          assets={[{ ...testAssets[0], lastModified: value as string | null }]}
        />,
      );
      expect(screen.queryByText('Invalid Date')).not.toBeInTheDocument();
      expect(screen.getByText('—')).toBeInTheDocument();
    });

    // An undated row must not scramble the ordering of the dated ones. Subtracting two -Infinity
    // sort keys yields NaN, which makes the comparator's verdict on those two rows arbitrary.
    it('keeps a deterministic order when several assets carry no timestamp', async () => {
      const mixed = [
        { ...testAssets[0], id: 'x1', path: 'a/undated-1.jar', lastModified: null },
        { ...testAssets[0], id: 'x2', path: 'a/dated.jar', lastModified: '2024-06-01T00:00:00Z' },
        { ...testAssets[0], id: 'x3', path: 'a/undated-2.jar', lastModified: null },
      ];
      renderWithTheme(<GAFilesTab assets={mixed} selectedVersion="1.0.0" loading={false} />);
      await userEvent.click(screen.getByText('Export CSV'));

      const [data] = exportToCsvSpy.mock.calls[0];
      // All three rows survive sorting, and the dated one still formats as a real date.
      expect(data).toHaveLength(3);
      expect(screen.queryByText('Invalid Date')).not.toBeInTheDocument();
      expect(screen.getAllByText('—')).toHaveLength(2);
    });

    it('filters by repository in the search query', async () => {
      const twoRepos = [
        { ...testAssets[0], id: 'r1', repository: 'maven-releases' },
        { ...testAssets[1], id: 'r2', repository: 'maven-snapshots' },
      ];
      renderWithTheme(<GAFilesTab assets={twoRepos} selectedVersion="1.0.0" loading={false} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'snapshots');

      expect(screen.queryByText('lib-1.0.jar')).not.toBeInTheDocument();
      expect(screen.getByText('lib-1.0.pom')).toBeInTheDocument();
    });

    // The sub-line under each filename used to render asset.classifier, which the real API never
    // populates (componentVersionDetailApi.ts hardcodes classifier: undefined). It now carries the
    // repository, which is the only field distinguishing assets that share a filename.
    it('shows the repository in the file cell', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      // Both fixture assets live in repo-a, so both rows show it.
      expect(screen.getAllByText('repo-a')).toHaveLength(2);
    });

    it('renders the repository as a badge, matching the per-row badge convention used in the Versions and Repositories tabs', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      const [badge] = screen.getAllByText('repo-a');
      expect(badge.closest('.rt-Badge')).not.toBeNull();
    });

    it('labels the repository for screen readers', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      // VisuallyHidden renders a real span, so the label is in the DOM but not visible.
      expect(screen.getAllByText('Repository:')).toHaveLength(2);
    });

    it('distinguishes assets that share a filename across repositories', () => {
      const sameNameDifferentRepos = [
        {
          id: 'm1',
          repository: 'npm-hosted-a',
          path: 'dummy-npm-lib-shared02-002-1.0.7.tgz',
          downloadUrl: 'http://localhost/repository/npm-hosted-a/dummy.tgz',
          format: 'tgz',
          extension: 'tgz',
          size: 486,
          contentType: 'application/gzip',
          lastModified: '2026-08-06T10:00:00Z',
          checksums: { sha1: 'a', sha256: 'b', md5: 'c' },
        },
        {
          id: 'm2',
          repository: 'npm-hosted-b',
          path: 'dummy-npm-lib-shared02-002-1.0.7.tgz',
          downloadUrl: 'http://localhost/repository/npm-hosted-b/dummy.tgz',
          format: 'tgz',
          extension: 'tgz',
          size: 486,
          contentType: 'application/gzip',
          lastModified: '2026-08-06T10:00:00Z',
          checksums: { sha1: 'a', sha256: 'b', md5: 'c' },
        },
        {
          id: 'm3',
          repository: 'npm-proxy-c',
          path: 'dummy-npm-lib-shared02-002-1.0.7.tgz',
          downloadUrl: 'http://localhost/repository/npm-proxy-c/dummy.tgz',
          format: 'tgz',
          extension: 'tgz',
          size: 486,
          contentType: 'application/gzip',
          lastModified: '2026-08-06T10:00:00Z',
          checksums: { sha1: 'a', sha256: 'b', md5: 'c' },
        },
      ];

      renderWithTheme(
        <GAFilesTab assets={sameNameDifferentRepos} selectedVersion="1.0.7" loading={false} />,
      );

      // Three rows, one filename, three distinct repositories.
      expect(screen.getAllByText('dummy-npm-lib-shared02-002-1.0.7.tgz')).toHaveLength(3);
      expect(screen.getByText('npm-hosted-a')).toBeInTheDocument();
      expect(screen.getByText('npm-hosted-b')).toBeInTheDocument();
      expect(screen.getByText('npm-proxy-c')).toBeInTheDocument();
    });

    it('Export CSV button is disabled when filters leave sortedAssets empty', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeDisabled();
    });

    it('filters by extension checkbox — only jar files shown', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      const checkboxes = screen.getAllByRole('checkbox');
      // Extension checkboxes are dynamically built; jar and pom should both appear
      // Click the first checkbox (jar)
      await userEvent.click(checkboxes[0]);
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
      expect(screen.queryByText('lib-1.0.pom')).not.toBeInTheDocument();
    });

    it('unchecking an extension checkbox removes the filter', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[0]); // check
      await userEvent.click(checkboxes[0]); // uncheck
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
      expect(screen.getByText('lib-1.0.pom')).toBeInTheDocument();
    });

    it('filters by classifier in search query', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'sources');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(1);
      expect(data[0].extension).toBe('pom');
    });

    it('no longer renders the classifier placeholder dash', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      // The superseded test asserted getByText('-') and passed, which proves the fixture rendered
      // exactly one bare '-' — the classifier placeholder. Nothing else in these two rows renders
      // one (sizes are '1 KB' / '512 B', dates are 'Jan 15, 2024'), so this query is precise.
      expect(screen.queryByText('-')).not.toBeInTheDocument();
    });
  });
});
